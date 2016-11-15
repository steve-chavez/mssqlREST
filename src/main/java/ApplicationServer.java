
import spark.*;
import com.google.gson.reflect.TypeToken;
import com.google.gson.*;

import com.univocity.parsers.common.processor.*;
import com.univocity.parsers.conversions.*;
import com.univocity.parsers.csv.*;

import com.ebay.xcelite.sheet.*;
import com.ebay.xcelite.reader.*;
import com.ebay.xcelite.writer.*;
import com.ebay.xcelite.*;

import java.util.*;
import java.util.stream.*;
import java.io.*;

import com.zaxxer.hikari.*;

import io.jsonwebtoken.*;

import fj.data.Either;

import javax.servlet.http.*;

import java.io.*;
import java.nio.file.*;

public class ApplicationServer {

    static final String COOKIE_NAME = "x-rest-jwt";
    static final Gson gson = new GsonBuilder().serializeNulls().create();

    public static void main(String[] args){

        Optional<Configurator.Config> config = Configurator.fromYaml(args[0]);

        if(!config.isPresent()){
          System.out.println("Config file doesn't exist or is an invalid YAML format");
          System.exit(0);
        }

        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl(config.get().url);
        ds.setUsername(config.get().user);
        ds.setPassword(config.get().password);

        Spark.port(config.get().port);

        QueryExecuter queryExecuter = new QueryExecuter(config.get().schema, ds, config.get().defaultRole);

        String secret = config.get().secret;

        Optional<String> optOrigin = config.get().origin;

        List<String> jwtRoutines = config.get().jwtRoutines
          .orElse(new ArrayList<String>());

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
            Optional<String> accept = Optional.ofNullable(request.headers("Accept"));

            String table = request.params(":table");
            Structure.Format format = Structure.Format.JSON;

            if(accept.isPresent()){
                 if(accept.get().equals("application/json"))
                    format = Structure.Format.JSON;
                 else if(accept.get().equals("text/csv"))
                    format = Structure.Format.CSV;
								 else if(accept.get().equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    format = Structure.Format.XLSX;
								 else if(accept.get().equals("application/octet-stream"))
                    format = Structure.Format.BINARY;
            }

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
                    else if(format == Structure.Format.XLSX){
                        response.header("Content-Disposition", "attachment");
                        response.type("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
                        response.status(200);
                        HttpServletResponse raw = response.raw();
                        raw.getOutputStream().write((byte[])result1.right().value());
                        raw.getOutputStream().flush();
                        raw.getOutputStream().close();
                        return raw;
                    }
                    else
                        response.type("application/octet-stream");
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

            Structure.Format format = Structure.Format.JSON;
            if(contentType.isPresent()){
                if(contentType.get().equals("text/csv"))
                    format = Structure.Format.CSV;
                if(contentType.get().equals("application/json"))
                    format = Structure.Format.JSON;
                if(contentType.get().equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    format = Structure.Format.XLSX;
            }

            if(format==Structure.Format.JSON){
                Map<String, String> mappedValues = jsonToMap(request.body());
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
            }else if(format==Structure.Format.CSV){
                List<Map<String, String>> mappedValues = csvToMap(request.body());
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
            }else{
                List<Map<String, String>> mappedValues = xlsxToMap(request.raw());
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

            Map<String, String> mappedValues = jsonToMap(request.body());
            Map<String, String> map = normalizeMap(request.queryMap().toMap());
            Either<Object, Object> result = queryExecuter.updateSet(request.params(":table"),
                    mappedValues, map, getRoleFromCookieOrHeader(secret, request));
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

            Map<String, String> mappedValues = jsonToMap(request.body());
            Either<Object, Map<String, Object>> result = queryExecuter.callRoutine(request.params(":routine"),
                mappedValues, getRoleFromCookieOrHeader(secret, request));
            if(result.isRight()){
                response.type("application/json");
                response.status(200);
                if(jwtRoutines.contains(request.params(":routine")))
                    return Jwts.builder()
                        .setClaims(result.right().value())
                        .signWith(SignatureAlgorithm.HS256, secret).compact();
                else
                    return gson.toJson(result.right().value());
            }else{
                response.type("application/json");
                response.status(400);
                return result.left().value().toString();
            }
        });
    }

    private static Map<String, String> normalizeMap(Map<String, String[]> map){
        return map.entrySet().stream().collect(
                Collectors.toMap(
                    Map.Entry::getKey, e -> e.getValue()[0]
        ));
    }

    private static Map<String, String> jsonToMap(String body){
        Map<String, String> emptyMap = Collections.emptyMap();
        if(!body.isEmpty())
          return gson.fromJson(body, new TypeToken<Map<String, String>>(){}.getType());
        else
          return emptyMap;
    }

    private static List<Map<String, String>> csvToMap(String body){
        CsvParser parser = new CsvParser(new CsvParserSettings());
        List<String[]> rows = parser.parseAll(new StringReader(body));
        List<Map<String, String>> mappedValues =
            new ArrayList<Map<String, String>>();
        String[] headers = rows.get(0);
        int length = headers.length;
        for(int i=1; i < rows.size(); i++){
            String[] values = rows.get(i);
            Map<String, String> attr = new LinkedHashMap<String, String>();
            for(int j=0; j<length; j++){
                attr.put(headers[j], values[j]);
            }
            mappedValues.add(attr);
        }
        return mappedValues;
    }

    private static List<Map<String, String>> xlsxToMap(HttpServletRequest hsr){
        try{
            List<Map<String, String>> mappedValues = new ArrayList();
            Xcelite xcelite = new Xcelite(hsr.getInputStream());
            XceliteSheet sheet = xcelite.getSheet("data_sheet");
            SheetReader<Collection<Object>> simpleReader = sheet.getSimpleReader();
            List<Collection<Object>> data =
                new ArrayList<Collection<Object>>(simpleReader.read());

            List<String> headers = data.get(0).stream()
               .map(object -> (object != null ? object.toString() : null))
               .collect(Collectors.toList());

            Integer headersSize = headers.size();
            Integer rowsSize = data.size();

            for (int i=1; i<rowsSize; i++){
                List<Object> values = new ArrayList(data.get(i));
                Map<String, String> value = new LinkedHashMap<String, String>();
                for(int j=0; j<headersSize; j++){
                    String header = headers.get(j);
                    if(header != null && !header.isEmpty())
                        value.put(header,
                                values.get(j)==null?null:values.get(j).toString());
                }
                mappedValues.add(value);
            }
            return mappedValues;
        }catch(IOException ioe){
            System.out.println(ioe.getMessage());
            return null;
        }
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
}

