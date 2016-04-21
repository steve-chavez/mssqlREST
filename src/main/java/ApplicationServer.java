
import spark.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.core.type.*;
import com.fasterxml.jackson.core.*;

import com.google.gson.reflect.TypeToken;
import com.google.gson.Gson;

import java.util.*;

public class ApplicationServer {

    public static void main(String[] args) {

        TableDAO tableDAO = new TableDAO();

        Spark.port(8080); 

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

    }
}

