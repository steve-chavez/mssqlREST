
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.servlet.*;

public class ApplicationServer{

    public static void main(String[] args) throws Exception{
        Server server = new Server(8080);
        ServletContextHandler handler = new ServletContextHandler(server, "/");
        ServletHolder jerseyServlet = handler.addServlet(org.glassfish.jersey.servlet.ServletContainer.class, "/api/*");
        jerseyServlet.setInitParameter("jersey.config.server.provider.classnames", TableResource.class.getCanonicalName());
        server.start();
    }
}

