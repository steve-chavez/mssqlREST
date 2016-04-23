
import org.json.*;

import java.sql.*;

import com.google.gson.*;

import java.util.*;

public class TableDAO{

    private String url;
    private String user;
    private String password;

    public TableDAO(String url, String user, String password){
        this.url = url;
        this.user = user;
        this.password = password;
    }
    
    public JSONArray selectFrom(String tableName){
        Structure.Table table = this.getTableMetaData(tableName);
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
            System.out.println(e.getMessage());
        }
        return json;
    }

    public Integer insertInto(String tableName, Map<String, String> values){
        Structure.Table table = this.getTableMetaData(tableName);
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
            System.out.println(e.getMessage());
        }
        return id;
    }

    public Integer updateSet(String tableName, Map<String, String> values, Map<String, String[]> queryParams){
        Structure.Table table = this.getTableMetaData(tableName);
        String query = QueryBuilder.updateQuery(table, values, queryParams);
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
            System.out.println(e.getMessage());
        }
        return id;
    }

    public Integer deleteFrom(String tableName, Map<String, String[]> queryParams){
        Structure.Table table = this.getTableMetaData(tableName);
        String query = QueryBuilder.deleteQuery(table, queryParams);
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
            System.out.println(e.getMessage());
        }
        return id;
    }

    public Integer execProc(String procName){
        Connection conn = null;  
        Statement statement;
        ResultSet rs;
        Integer id = 0;
        String query = String.format("EXEC %s 1", procName);
        System.out.println(query);
        try {
            conn = DriverManager.getConnection(this.url, this.user, this.password);
            statement = conn.prepareStatement(query);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return id;
    }

    public JSONObject selectFunc(String funcName, Map<String, String> values){
        JSONObject obj = null;
        Structure.Routine routine = this.getRoutineMetaData(funcName);
        String query = QueryBuilder.functionQuery(routine); 
        System.out.println(query);
        try {
            Connection conn = DriverManager.getConnection(this.url, this.user, this.password);
            PreparedStatement statement = StatementBuilder.buildPreparedStatement(conn, query, routine, values);
            ResultSet rs = statement.executeQuery();
            obj = ResultSetJsoner.funcResultToJson(routine, rs);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            obj.put("error", e.getMessage());
        }
        return obj;
    }

    public Structure.Table getTableMetaData(String tableName){
        Connection conn = null;  
        PreparedStatement statement;
        ResultSet rs;
        Structure.Table table = new Structure.Table();
        table.name = tableName; 
        String query = "SELECT column_name, data_type FROM information_schema.columns WHERE table_name = ?";
        try {
            conn = DriverManager.getConnection(this.url, this.user, this.password);
            statement = conn.prepareStatement(query);
            statement.setString(1, tableName);
            rs = statement.executeQuery();
            while(rs.next()){
                table.columns.put(rs.getString("column_name"), rs.getString("data_type"));
            }
        } catch (SQLException e) {
            System.out.println(e.toString());
        }
        return table;
    }

    public Structure.Routine getRoutineMetaData(String routineName){
        Structure.Routine routine = new Structure.Routine();
        String query1 = "SELECT routine_name, routine_type, data_type FROM information_schema.routines WHERE routine_name = ?";
        String query2 = "SELECT parameter_name, ordinal_position, data_type, parameter_mode FROM information_schema.parameters WHERE specific_name = ?";
        try {
            Connection conn = DriverManager.getConnection(this.url, this.user, this.password);
            conn.setAutoCommit(false);
            PreparedStatement statement1 = conn.prepareStatement(query1);
            statement1.setString(1, routineName);
            ResultSet rs1 = statement1.executeQuery();
            while(rs1.next()){
                routine.name = rs1.getString("routine_name");
                routine.routineType = rs1.getString("routine_type");
                routine.dataType = rs1.getString("data_type");
            }
            PreparedStatement statement2 = conn.prepareStatement(query2);
            statement2.setString(1, routineName);
            ResultSet rs2 = statement2.executeQuery();
            while(rs2.next()){
                if(rs2.getInt("ordinal_position") > 0){
                    Structure.Parameter parameter = new Structure.Parameter();
                    parameter.name = rs2.getString("parameter_name").substring(1);
                    parameter.dataType = rs2.getString("data_type");
                    parameter.ordinalPosition = rs2.getInt("ordinal_position");
                    parameter.parameterMode = rs2.getString("parameter_mode");
                    routine.parameters.put(parameter.name, parameter); 
                }
            }
            conn.commit();
        } catch (SQLException e) {
            System.out.println(e.toString());
            //conn.rollback();
        }
        return routine;
    }
}
