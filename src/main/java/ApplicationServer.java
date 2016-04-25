
import spark.*;
import com.google.gson.reflect.TypeToken;
import com.google.gson.Gson;

import java.util.*;
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

        Spark.get("/:table", (request, response) -> {
            response.status(200);
            response.type("application/json");
            return tableDAO.selectFrom(request.params(":table")).toString();
        });

        Spark.post("/:table", (request, response) -> {
            Gson gson = new Gson();
            Map<String, String> values = gson.fromJson(request.body(), new TypeToken<Map<String, String>>(){}.getType());
            response.status(200);
            response.type("application/json");
            return tableDAO.insertInto(request.params(":table"), values);
        });

        Spark.patch("/:table", (request, response) -> {
            Gson gson = new Gson();
            Map<String, String> values = gson.fromJson(request.body(), new TypeToken<Map<String, String>>(){}.getType());
            response.status(200);
            response.type("application/json");
            return tableDAO.updateSet(request.params(":table"), values, request.queryMap().toMap());
        });

        Spark.delete("/:table", (request, response) -> {
            response.status(200);
            response.type("application/json");
            return tableDAO.deleteFrom(request.params(":table"), request.queryMap().toMap());
        });

        Spark.post("/rpc/:routine", (request, response) -> {
            Gson gson = new Gson();
            Map<String, String> values = gson.fromJson(request.body(), new TypeToken<Map<String, String>>(){}.getType());
            response.status(200);
            response.type("application/json");
            return tableDAO.callRoutine(request.params(":routine"), values).toString();
        });
    }
}

