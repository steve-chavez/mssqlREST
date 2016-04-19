
import spark.*;

public class ApplicationServer {

    public static void main(String[] args) {
        TableDAO tableDAO = new TableDAO();
        Spark.port(8080); 
        Spark.get("/:table", (request, response) -> {
            response.status(200);
            response.type("application/json");
            return tableDAO.selectFrom(request.params(":table")).toString();
        });
    }
}

