
import spark.*;
import com.google.gson.reflect.TypeToken;
import com.google.gson.Gson;

import java.util.*;
import java.util.stream.*;
import java.io.*;
import org.yaml.snakeyaml.*;

import com.zaxxer.hikari.*;

import fj.data.Either;

public class ApplicationServer {

    public static void main(String[] args) throws FileNotFoundException{

		Yaml yaml = new Yaml();
	
		Map<String, Object> vals = (Map<String, Object>) yaml
				.load(new FileInputStream(new File("config-cloud.yml")));

        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl(vals.get("url").toString());
        ds.setUsername(vals.get("user").toString());
        ds.setPassword(vals.get("password").toString());

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
            Either<Object, Object> result = queryExecuter.selectAllPrivilegedTables();
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

        Spark.get("/:table", (request, response) -> {
            System.out.println(request.requestMethod() + " : " + request.url());
            Map<String, String> convertedQueryMap = request.queryMap().toMap().entrySet().stream().collect(
                Collectors.toMap(
                    Map.Entry::getKey, e -> e.getValue()[0]
            ));
            Optional<String> resource = Optional.ofNullable(request.headers("Resource"));
            Optional<String> plurality = Optional.ofNullable(request.headers("Plurality"));
            Boolean singular = plurality.isPresent() && plurality.get().equals("singular");
            if(!resource.isPresent()){
                Either<Object, Object> result1 = queryExecuter.selectFrom(request.params(":table"), convertedQueryMap, singular);
                if(result1.isRight()){
                    response.type("application/json");
                    response.status(200);
                    return result1.right().value().toString();
                }else{
                    response.type("application/json");
                    response.status(400);
                    return result1.left().value().toString();
                }
            }else{
                Either<Object, Object> result2 = queryExecuter.selectTableMetaData(request.params(":table"), singular);
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
            Gson gson = new Gson();
            Map<String, String> values = gson.fromJson(request.body(), new TypeToken<Map<String, String>>(){}.getType());
            Either<Object, Object> result = queryExecuter.insertInto(request.params(":table"), values);
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

        Spark.patch("/:table", (request, response) -> {
            System.out.println(request.requestMethod() + " : " + request.url());
            Gson gson = new Gson();
            Map<String, String> values = gson.fromJson(request.body(), new TypeToken<Map<String, String>>(){}.getType());
            Map<String, String> convertedQueryMap = request.queryMap().toMap().entrySet().stream().collect(
                Collectors.toMap(
                    Map.Entry::getKey, e -> e.getValue()[0]
            ));
            Either<Object, Object> result = queryExecuter.updateSet(request.params(":table"), values, convertedQueryMap);
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
            Map<String, String> convertedQueryMap = request.queryMap().toMap().entrySet().stream().collect(
                Collectors.toMap(
                    Map.Entry::getKey, e -> e.getValue()[0]
            ));
            Either<Object, Object> result = queryExecuter.deleteFrom(request.params(":table"), convertedQueryMap);
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
            Gson gson = new Gson();
            Map<String, String> values = gson.fromJson(request.body(), new TypeToken<Map<String, String>>(){}.getType());
            Either<Object, Object> result = queryExecuter.callRoutine(request.params(":routine"), values);
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
    }
}

