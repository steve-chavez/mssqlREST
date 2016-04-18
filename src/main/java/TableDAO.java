
import org.json.*;

import java.sql.*;

//import com.microsoft.sqlserver.jdbc.SQLServerException;

import com.healthmarketscience.sqlbuilder.dbspec.basic.*;
import com.healthmarketscience.sqlbuilder.*;

public class TableDAO{

    private String url = "your_url";
    private String user = "your_user";
    private String password = "your_password";
    
    public JSONArray queryTable(String table){
        String query = String.format("SELECT * FROM %s", table);
        Connection conn = null;  
        PreparedStatement statement;
        ResultSet rs;
        JSONArray json = null;
        try {
            conn = DriverManager.getConnection(this.url, this.user, this.password);
            statement = conn.prepareStatement(query);
            rs = statement.executeQuery();
            json = ResultSetJsoner.convert(rs);
        } catch (SQLException e) {
            //e.printStackTrace();
            System.out.println(e.toString());
            System.out.println("DB error");
        }
        return json;
    }

}
