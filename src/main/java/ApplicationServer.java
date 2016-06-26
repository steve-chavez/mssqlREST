
import spark.*;
import com.google.gson.reflect.TypeToken;
import com.google.gson.Gson;

import com.univocity.parsers.common.processor.*;
import com.univocity.parsers.conversions.*;
import com.univocity.parsers.csv.*;

import java.util.*;
import java.util.stream.*;
import java.io.*;
import org.yaml.snakeyaml.*;

import com.zaxxer.hikari.*;

import io.jsonwebtoken.*;

import fj.data.Either;

import org.json.*;

import javax.servlet.http.*;

public class ApplicationServer {
 
    static final String COOKIE_NAME = "x-rest-jwt";

    private static Map<String, String> normalizeMap(Map<String, String[]> map){
        return map.entrySet().stream().collect(
                Collectors.toMap(
                    Map.Entry::getKey, e -> e.getValue()[0]
        ));
    }

    private static List<Map<String, String>> convertCSV(List<String[]> rows){
        List<Map<String, String>> mappedValues = 
            new ArrayList<Map<String, String>>();
        String[] headers = rows.get(0);
        int length = headers.length;
        for(int i=1; i < rows.size(); i++){
            String[] values = rows.get(i);
            Map<String, String> attr = new HashMap<String, String>();
            for(int j=0; j<length; j++){
                attr.put(headers[j], values[j]);
            }
            mappedValues.add(attr);
        }
        return mappedValues;
    }

    private static Optional<String> obtainBearer(String s){
        if(s.isEmpty())
            return Optional.empty();
        else {
            String[] arr = s.split(" ");
            if(!arr[0].equalsIgnoreCase("Bearer"))
                return Optional.empty();
            else{
                if(arr.length == 1)
                    return Optional.empty();
                else
                    return Optional.of(arr[1]);
            }
        }
    }

    private static Optional<String> decodeGetRole(String secret, String jwt){
        try{
            Claims claims = Jwts.parser().setSigningKey(secret).parseClaimsJws(jwt).getBody();
            return Optional.of(claims.get("role").toString());
        }catch(Exception e){
            return Optional.empty();
        }
    }

    private static Optional<String> getRoleFromCookieOrHeader(String secret, Request request){
        Optional<String> authorization = Optional.ofNullable(request.headers("Authorization"));
        Optional<String> cookie = Optional.ofNullable(request.cookie(COOKIE_NAME));
        if(cookie.isPresent()){
            return decodeGetRole(secret, cookie.get());
        }
        else if(authorization.isPresent()){
            Optional<String> bearer = obtainBearer(authorization.get());
            return decodeGetRole(secret, bearer.get());
        }
        else return Optional.empty();
    }

    private static Optional<Map<String, String>> getTableAndDot(String table){
        String[] parts = table.split("\\.");
        Map<String, String> map = new HashMap();
        if(parts.length <= 1)
            return Optional.empty();
        else{
            map.put("table", parts[0]);
            map.put("dot", parts[1]);
            return Optional.of(map);
        }
    }

    public static void main(String[] args){

    	Map<String, Object> vals = null;
        try{
            vals = (Map<String, Object>) new Yaml()
                    .load(new FileInputStream(new File(args[0])));
        }catch(Exception e){
            System.out.println("Config file doesn't exit or is an invalid YAML format");
            System.exit(0);
        }

        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl(vals.get("url").toString());
        ds.setUsername(vals.get("user").toString());
        ds.setPassword(vals.get("password").toString());

        Spark.port((Integer)vals.get("port")); 

        Optional<Object> optionalJwtRoutines = Optional.ofNullable(vals.get("jwtRoutines"));
        List<String> jwtRoutines;
        if(optionalJwtRoutines.isPresent())
            jwtRoutines = (ArrayList<String>)optionalJwtRoutines.get();
        else
            jwtRoutines = new ArrayList<String>();

        String secret = vals.get("secret").toString(); 

        QueryExecuter queryExecuter = new QueryExecuter(ds, vals.get("defaultRole").toString());

        Optional<Object> optOrigin = Optional.ofNullable(vals.get("origin"));

        // Headers:
        // Plurality: singular, plural
        // Resource : definition, data
        Spark.before(new Filter() {
            @Override
            public void handle(Request request, Response response) {
                if(optOrigin.isPresent()){
                    response.header("Access-Control-Allow-Origin", optOrigin.get().toString());
                    response.header("Access-Control-Allow-Credentials", "true");
                }else
                    response.header("Access-Control-Allow-Origin", "*");
                response.header("Access-Control-Allow-Methods", "GET,POST,PUT,PATCH,DELETE,OPTIONS");
                response.header("Access-Control-Allow-Headers", "Content-Type, Authorization, X-Requested-With, Plurality, Resource");
            }
        });

        //---------Pre flight---------
        Spark.options("/", (request, response) -> {
            System.out.println(request.requestMethod() + " : " + request.url());
            response.status(200);
            return "";
        });

        Spark.options("/:table", (request, response) -> {
            System.out.println(request.requestMethod() + " : " + request.url());
            response.status(200);
            return "";
        });

        Spark.options("/rpc/:routine", (request, response) -> {
            System.out.println(request.requestMethod() + " : " + request.url());
            response.status(200);
            return "";
        });
        //----------------------------
        //
        Spark.get("/", (request, response) -> {
            System.out.println(request.requestMethod() + " : " + request.url());
            Either<Object, Object> result = queryExecuter.selectAllPrivilegedTables(
                    getRoleFromCookieOrHeader(secret, request));
            if(result.isRight()){
                response.type("application/json");
                response.status(200);
                return result.right().value().toString();
            }else{
                response.status(400);
                return result.left().value().toString();
            }
        });

        Spark.get("/:table", (request, response) -> {
            System.out.println(request.requestMethod() + " : " + request.url());

            Map<String, String> map = normalizeMap(request.queryMap().toMap());

            //order query param
            String optOrder = map.entrySet().stream()
                .filter( x -> x.getKey().equalsIgnoreCase("order"))
                .map( x -> x.getValue())
                .collect(Collectors.joining());
            Optional<String> order = optOrder.isEmpty()?
                Optional.empty():Optional.of(optOrder);

            //select query param
            String optSelect = map.entrySet().stream()
                .filter( x -> x.getKey().equalsIgnoreCase("select"))
                .map( x -> x.getValue())
                .collect(Collectors.joining());
            Optional<String> selectColumns = optSelect.isEmpty()?
                Optional.empty():Optional.of(optSelect);

            //Other query params
            Map<String, String> mapWithout = map.entrySet().stream()
                .filter( x -> !x.getKey().equalsIgnoreCase("order")&&
                              !x.getKey().equalsIgnoreCase("select"))
                .collect(Collectors.toMap( x -> x.getKey(), x -> x.getValue()));

            Optional<String> resource = Optional.ofNullable(request.headers("Resource"));
            Optional<String> plurality = Optional.ofNullable(request.headers("Plurality"));

            Optional<Map<String,String>> tableDot = getTableAndDot(request.params(":table"));
            String table, dot;

            Structure.Format format = Structure.Format.JSON;

            if(tableDot.isPresent()){
                table = tableDot.get().get("table");
                dot = tableDot.get().get("dot");
                if(dot.equals("csv"))
                    format = Structure.Format.CSV;
                else if(dot.equals("xls"))
                    format = Structure.Format.XLSX;
                else if(dot.equals("json"))
                    format = Structure.Format.JSON;
            }else table = request.params(":table");

            Boolean singular = plurality.isPresent() && plurality.get().equals("singular");

            if(!resource.isPresent()){
                Either<Object, Object> result1 = queryExecuter.selectFrom(table, 
                        mapWithout, selectColumns, order, singular, format, 
                        getRoleFromCookieOrHeader(secret, request));
                if(result1.isRight()){
                    if(format == Structure.Format.JSON)
                        response.type("application/json");
                    else if(format == Structure.Format.CSV)
                        response.type("text/csv");
                    else{
                        response.type("application/vnd.ms-excel");
                        response.status(200);
                        HttpServletResponse raw = response.raw();
                        raw.getOutputStream().write((byte[])result1.right().value());
                        raw.getOutputStream().flush();
                        raw.getOutputStream().close();
                        return raw;
                    }
                    response.status(200);
                    return result1.right().value().toString();
                }else{
                    response.type("application/json");
                    response.status(400);
                    return result1.left().value().toString();
                }
            }else{
                Either<Object, Object> result2 = queryExecuter.selectTableMetaData(table, 
                        singular, getRoleFromCookieOrHeader(secret, request));
                if(result2.isRight()){
                    response.type("application/json");
                    response.status(200);
                    return result2.right().value().toString();
                }else{
                    response.type("application/json");
                    response.status(400);
                    return result2.left().value().toString();
                }
            }
        });

        Spark.post("/:table", (request, response) -> {
            System.out.println(request.requestMethod() + " : " + request.url());

            Optional<String> contentType = Optional.ofNullable(request.headers("Content-Type"));
            Optional<String> resource = Optional.ofNullable(request.headers("Resource"));

            Structure.Format format;
            if(contentType.isPresent())
                format = contentType.get().equals("text/csv")?Structure.Format.CSV:Structure.Format.JSON;
            else
                format = Structure.Format.JSON;

            if(format==Structure.Format.JSON){
                Gson gson = new Gson();
                Map<String, String> values = gson.fromJson(request.body(), new TypeToken<Map<String, String>>(){}.getType());
                Either<Object, Object> result = queryExecuter.insertInto(request.params(":table"), 
                        values, getRoleFromCookieOrHeader(secret, request));
                if(result.isRight()){
                    response.type("application/json");
                    response.status(200);
                    return result.right().value().toString();
                }else{
                    response.type("application/json");
                    response.status(400);
                    return result.left().value().toString();
                }
            }else{
                CsvParser parser = new CsvParser(new CsvParserSettings());
                List<Map<String, String>> mappedValues = convertCSV(parser.parseAll(new StringReader(request.body())));
                Either<Object, Object> result = queryExecuter.insertInto(request.params(":table"), 
                        mappedValues, getRoleFromCookieOrHeader(secret, request));
                if(result.isRight()){
                    response.type("application/json");
                    response.status(200);
                    return result.right().value().toString();
                }else{
                    response.type("application/json");
                    response.status(400);
                    return result.left().value().toString();
                }
            }
        });

        Spark.patch("/:table", (request, response) -> {
            System.out.println(request.requestMethod() + " : " + request.url());
            Optional<String> resource = Optional.ofNullable(request.headers("Resource"));
            Gson gson = new Gson();

            Map<String, String> values = gson.fromJson(request.body(), new TypeToken<Map<String, String>>(){}.getType());
            Map<String, String> map = normalizeMap(request.queryMap().toMap());
            Either<Object, Object> result = queryExecuter.updateSet(request.params(":table"), 
                    values, map, getRoleFromCookieOrHeader(secret, request));
            if(result.isRight()){
                response.type("application/json");
                response.status(200);
                return result.right().value().toString();
            }else{
                response.type("application/json");
                response.status(400);
                return result.left().value().toString();
            }
        });

        Spark.delete("/:table", (request, response) -> {
            System.out.println(request.requestMethod() + " : " + request.url());
            Optional<String> resource = Optional.ofNullable(request.headers("Resource"));

            Map<String, String> map = normalizeMap(request.queryMap().toMap());
            Either<Object, Object> result = queryExecuter.deleteFrom(request.params(":table"), 
                    map, getRoleFromCookieOrHeader(secret, request));
            if(result.isRight()){
                response.type("application/json");
                response.status(200);
                return result.right().value().toString();
            }else{
                response.type("application/json");
                response.status(400);
                return result.left().value().toString();
            }
        });

        Spark.post("/rpc/:routine", (request, response) -> {
            System.out.println(request.requestMethod() + " : " + request.url());
            Optional<String> resource = Optional.ofNullable(request.headers("Resource"));
            Gson gson = new Gson();

            Map<String, String> values = gson.fromJson(request.body(), new TypeToken<Map<String, String>>(){}.getType());
            Either<Map<String, Object>, Map<String, Object>> result = queryExecuter.callRoutine(request.params(":routine"), 
                values, getRoleFromCookieOrHeader(secret, request));
            if(result.isRight()){
                response.type("application/json");
                response.status(200);
                if(jwtRoutines.contains(request.params(":routine")))
                    return Jwts.builder()
                        .setClaims(result.right().value())
                        .signWith(SignatureAlgorithm.HS256, secret).compact();
                else
                    return new JSONObject(result.right().value()).toString();
            }else{
                response.type("application/json");
                response.status(400);
                return new JSONObject(result.left().value()).toString();
            }
        });
    }
}

