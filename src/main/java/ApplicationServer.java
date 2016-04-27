
import spark.*;
import com.google.gson.reflect.TypeToken;
import com.google.gson.Gson;

import java.util.*;
import java.util.stream.*;
import java.io.*;
import org.yaml.snakeyaml.*;

import com.zaxxer.hikari.*;

public class ApplicationServer {

    public static void main(String[] args) throws FileNotFoundException{

		Yaml yaml = new Yaml();
	
		Map<String, Object> vals = (Map<String, Object>) yaml
				.load(new FileInputStream(new File("config.yml")));

        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl(vals.get("url").toString());
        ds.setUsername(vals.get("user").toString());
        ds.setPassword(vals.get("password").toString());

        TableDAO tableDAO = new TableDAO(ds, vals.get("defaultRole").toString());

        Spark.port((Integer)vals.get("port")); 

        //CORS
        Spark.before(new Filter() {
            @Override
            public void handle(Request request, Response response) {
                response.header("Access-Control-Allow-Origin", "*");
                response.header("Access-Control-Allow-Methods", "GET,POST,PUT,PATCH,DELETE,OPTIONS");
                response.header("Access-Control-Allow-Headers", "Content-Type, Authorization, X-Requested-With");
            }
        });

        Spark.options("/:table", (request, response) -> {
            System.out.println(request.requestMethod() + " : " + request.url());
            response.status(200);
            return "OK";
        });

        Spark.get("/:table", (request, response) -> {
            System.out.println(request.requestMethod() + " : " + request.url());
            response.status(200);
            response.type("application/json");
            return tableDAO.selectFrom(request.params(":table")).toString();
        });

        Spark.post("/:table", (request, response) -> {
            System.out.println(request.requestMethod() + " : " + request.url());
            Gson gson = new Gson();
            Map<String, String> values = gson.fromJson(request.body(), new TypeToken<Map<String, String>>(){}.getType());
            response.status(200);
            response.type("application/json");
            return tableDAO.insertInto(request.params(":table"), values);
        });

        Spark.patch("/:table", (request, response) -> {
            System.out.println(request.requestMethod() + " : " + request.url());
            Gson gson = new Gson();
            Map<String, String> values = gson.fromJson(request.body(), new TypeToken<Map<String, String>>(){}.getType());
            response.status(200);
            response.type("application/json");
            return tableDAO.updateSet(request.params(":table"), values, request.queryMap().toMap());
        });

        Spark.delete("/:table", (request, response) -> {
            System.out.println(request.requestMethod() + " : " + request.url());
            response.status(200);
            response.type("application/json");
            Map<String, String> convertedQueryMap = request.queryMap().toMap().entrySet().stream().collect(
                Collectors.toMap(
                    Map.Entry::getKey, e -> e.getValue()[0]
            ));
            return tableDAO.deleteFrom(request.params(":table"), convertedQueryMap);
        });

        Spark.post("/rpc/:routine", (request, response) -> {
            System.out.println(request.requestMethod() + " : " + request.url());
            Gson gson = new Gson();
            Map<String, String> values = gson.fromJson(request.body(), new TypeToken<Map<String, String>>(){}.getType());
            response.status(200);
            response.type("application/json");
            return tableDAO.callRoutine(request.params(":routine"), values).toString();
        });
    }
}

