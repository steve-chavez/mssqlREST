
import javax.ws.rs.*;
import javax.ws.rs.core.*;
 
@Path("/")
public class TableResource{
 
    private TableDAO tableDAO = new TableDAO();

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{table}")
    public Response getTrabajadores(@PathParam("table") String table) {
        return Response.ok(tableDAO.selectFrom(table).toString()).build();
    }
}


