
import org.json.*;

import java.sql.*;

import model.*;

import com.google.gson.*;

import java.util.*;

public class TableDAO{

    private String url = "jdbc:sqlserver://rrhh.database.windows.net:1433;database=RRHH;encrypt=true;trustServerCertificate=false;hostNameInCertificate=eastus1-a.control.database.windows.net;loginTimeout=30;";
    private String user = "stevebash";
    private String password = "1qaz\"WSX";
    
    public JSONArray selectFrom(String tableName){
        Table table = this.getMetaData(tableName);
        String query = QueryBuilder.selectQuery(table);
        System.out.println(query);
        Connection conn = null;  
        PreparedStatement statement;
        ResultSet rs;
        JSONArray json = new JSONArray();
        try {
            conn = DriverManager.getConnection(this.url, this.user, this.password);
            statement = conn.prepareStatement(query);
            rs = statement.executeQuery();
            json = ResultSetJsoner.convert(rs);
        } catch (SQLException e) {
            //e.printStackTrace();
            System.out.println(e.getMessage());
        }
        return json;
    }

    public Integer insertInto(String tableName, Map<String, String> values){
        Table table = this.getMetaData(tableName);
        String query = QueryBuilder.insertQuery(table, values);
        System.out.println(query);
        Connection conn = null;  
        Statement statement;
        ResultSet rs;
        Integer id = 0;
        try {
            conn = DriverManager.getConnection(this.url, this.user, this.password);
            statement = conn.createStatement();
            statement.executeUpdate(query, Statement.RETURN_GENERATED_KEYS);
            rs = statement.getGeneratedKeys();
            if(rs.next())
                id = rs.getInt(1);
        } catch (SQLException e) {
            //e.printStackTrace();
            System.out.println(e.getMessage());
        }
        return id;
    }

    public Table getMetaData(String tableName){
        Connection conn = null;  
        PreparedStatement statement;
        ResultSet rs;
        Table table = new Table();
        table.name = tableName; 
        String query = "SELECT column_name, data_type FROM INFORMATION_SCHEMA.columns WHERE table_name=?";
        try {
            conn = DriverManager.getConnection(this.url, this.user, this.password);
            statement = conn.prepareStatement(query);
            statement.setString(1, tableName);
            rs = statement.executeQuery();
            while(rs.next()){
                table.columns.put(rs.getString("column_name"), rs.getString("data_type"));
            }
            //System.out.println(table);
        } catch (SQLException e) {
            //e.printStackTrace();
            System.out.println(e.toString());
        }
        return table;
    }

}
