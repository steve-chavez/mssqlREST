
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

public class ApplicationServer {

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

    private static Optional<String> obtainRole(String secret, Optional<String> authorization){
        if(authorization.isPresent()){
            Optional<String> bearer = obtainBearer(authorization.get());
            if(bearer.isPresent()){
                try{
                    Claims claims = Jwts.parser().setSigningKey(secret).parseClaimsJws(bearer.get()).getBody();
                    return Optional.of(claims.get("role").toString());
                }catch(Exception e){
                    return Optional.empty();
                }
            }
            else
                return Optional.empty();
        }
        else
            return Optional.empty();
    }

    public static void main(String[] args) throws FileNotFoundException{

		Yaml yaml = new Yaml();
	
		Map<String, Object> vals = (Map<String, Object>) yaml
				.load(new FileInputStream(new File("config-cloud.yml")));

        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl(vals.get("url").toString());
        ds.setUsername(vals.get("user").toString());
        ds.setPassword(vals.get("password").toString());

        Optional<Object> optionalJwtRoutines = Optional.ofNullable(vals.get("jwtRoutines"));
        List<String> jwtRoutines;
        if(optionalJwtRoutines.isPresent())
            jwtRoutines = (ArrayList<String>)optionalJwtRoutines.get();
        else
            jwtRoutines = new ArrayList<String>();

        String secret = vals.get("secret").toString(); 

        QueryExecuter queryExecuter = new QueryExecuter(ds, vals.get("defaultRole").toString());

        Spark.port((Integer)vals.get("port")); 

        // Headers:
        // Plurality: singular, plural
        // Resource : definition, data
        Spark.before(new Filter() {
            @Override
            public void handle(Request request, Response response) {
                response.header("Access-Control-Allow-Origin", "*");
                response.header("Access-Control-Allow-Methods", "GET,POST,PUT,PATCH,DELETE,OPTIONS");
                response.header("Access-Control-Allow-Headers", "Content-Type, Authorization, X-Requested-With, Plurality, Resource");
            }
        });

        Spark.get("/test/jwt", (request, response) -> {
            String s = Jwts.builder()
                .claim("email", "ctm@gmail.com")
                .claim("role", "controlador")
                .signWith(SignatureAlgorithm.HS256, secret).compact();
            return s;
        });

        //---------Pre flight---------
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
            Optional<String> authorization = Optional.ofNullable(request.headers("Authorization"));
            Either<Object, Object> result = queryExecuter.selectAllPrivilegedTables(obtainRole(secret, authorization));
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
            Map<String, String> convertedQueryMap = request.queryMap().toMap().entrySet().stream().collect(
                Collectors.toMap(
                    Map.Entry::getKey, e -> e.getValue()[0]
            ));
            Optional<String> authorization = Optional.ofNullable(request.headers("Authorization"));
            Optional<String> resource = Optional.ofNullable(request.headers("Resource"));
            Optional<String> plurality = Optional.ofNullable(request.headers("Plurality"));
            Optional<String> accept = Optional.ofNullable(request.headers("Accept"));

            ResultSetConverter.Format format;
            if(accept.isPresent())
                format = accept.get().equals("text/csv")?ResultSetConverter.Format.CSV:ResultSetConverter.Format.JSON;
            else
                format = ResultSetConverter.Format.JSON;

            Boolean singular = plurality.isPresent() && plurality.get().equals("singular");

            if(!resource.isPresent()){
                Either<Object, Object> result1 = queryExecuter.selectFrom(request.params(":table"), 
                        convertedQueryMap, singular, format, obtainRole(secret, authorization));
                if(result1.isRight()){
                    if(format == ResultSetConverter.Format.CSV)
                        response.type("text/csv");
                    else
                        response.type("application/json");
                    response.status(200);
                    return result1.right().value().toString();
                }else{
                    response.type("application/json");
                    response.status(400);
                    return result1.left().value().toString();
                }
            }else{
                Either<Object, Object> result2 = queryExecuter.selectTableMetaData(request.params(":table"), 
                        singular, obtainRole(secret, authorization));
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
            Optional<String> authorization = Optional.ofNullable(request.headers("Authorization"));

            ResultSetConverter.Format format;
            if(contentType.isPresent())
                format = contentType.get().equals("text/csv")?ResultSetConverter.Format.CSV:ResultSetConverter.Format.JSON;
            else
                format = ResultSetConverter.Format.JSON;

            if(format==ResultSetConverter.Format.JSON){
                Gson gson = new Gson();
                Map<String, String> values = gson.fromJson(request.body(), new TypeToken<Map<String, String>>(){}.getType());
                Either<Object, Object> result = queryExecuter.insertInto(request.params(":table"), 
                        values, obtainRole(secret, authorization));
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
                        mappedValues, obtainRole(secret, authorization));
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
            Optional<String> authorization = Optional.ofNullable(request.headers("Authorization"));
            Gson gson = new Gson();

            Map<String, String> values = gson.fromJson(request.body(), new TypeToken<Map<String, String>>(){}.getType());
            Map<String, String> convertedQueryMap = request.queryMap().toMap().entrySet().stream().collect(
                Collectors.toMap(
                    Map.Entry::getKey, e -> e.getValue()[0]
            ));
            Either<Object, Object> result = queryExecuter.updateSet(request.params(":table"), 
                    values, convertedQueryMap, obtainRole(secret, authorization));
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
            Optional<String> authorization = Optional.ofNullable(request.headers("Authorization"));

            Map<String, String> convertedQueryMap = request.queryMap().toMap().entrySet().stream().collect(
                Collectors.toMap(
                    Map.Entry::getKey, e -> e.getValue()[0]
            ));
            Either<Object, Object> result = queryExecuter.deleteFrom(request.params(":table"), 
                    convertedQueryMap, obtainRole(secret, authorization));
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
            Optional<String> authorization = Optional.ofNullable(request.headers("Authorization"));
            Gson gson = new Gson();

            Map<String, String> values = gson.fromJson(request.body(), new TypeToken<Map<String, String>>(){}.getType());
            Either<Map<String, Object>, Map<String, Object>> result = queryExecuter.callRoutine(request.params(":routine"), 
                values, obtainRole(secret, authorization));
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

