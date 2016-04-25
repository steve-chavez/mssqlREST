
import org.json.*;

import java.sql.*;
import javax.sql.DataSource;

import com.google.gson.*;

import java.util.*;

public class TableDAO{

    private DataSource ds;

    public TableDAO(DataSource ds){
        this.ds = ds;
    }
    
    public JSONArray selectFrom(String tableName){
        Structure.Table table = this.getTableMetaData(tableName);
        String query = QueryBuilder.selectQuery(table);
        System.out.println(query);
        JSONArray json = new JSONArray();
        try(Connection conn = this.ds.getConnection()){
            PreparedStatement statement = conn.prepareStatement(query);
            ResultSet rs = statement.executeQuery();
            json = ResultSetJsoner.convert(rs);
        } catch (SQLException e) {
            JSONObject obj = new JSONObject();
            obj.put("error", e.getMessage());
            json.put(obj);
        }
        return json;
    }

    public Integer insertInto(String tableName, Map<String, String> values){
        Structure.Table table = this.getTableMetaData(tableName);
        String query = QueryBuilder.insertQuery(table, values);
        System.out.println(query);
        Integer id = 0;
        try(Connection conn = this.ds.getConnection()){
            Statement statement = conn.createStatement();
            statement.executeUpdate(query, Statement.RETURN_GENERATED_KEYS);
            ResultSet rs = statement.getGeneratedKeys();
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
        Integer id = 0;
        try(Connection conn = this.ds.getConnection()){
            Statement statement = conn.createStatement();
            statement.executeUpdate(query, Statement.RETURN_GENERATED_KEYS);
            ResultSet rs = statement.getGeneratedKeys();
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
        Integer id = 0;
        try(Connection conn = this.ds.getConnection()){
            Statement statement = conn.createStatement();
            statement.executeUpdate(query, Statement.RETURN_GENERATED_KEYS);
            ResultSet rs = statement.getGeneratedKeys();
            if(rs.next())
                id = rs.getInt(1);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return id;
    }

    public JSONObject callRoutine(String funcName, Map<String, String> values){
        JSONObject obj = null;
        Structure.Routine routine = this.getRoutineMetaData(funcName);
        String query = QueryBuilder.functionQuery(routine); 
        System.out.println(query);
        try(Connection conn = this.ds.getConnection()){
            CallableStatement cs = StatementBuilder.buildCallableStatement(conn, query, routine, values);
            if(routine.type.equals("FUNCTION")){
                ResultSet rs = cs.executeQuery();
                obj = ResultSetJsoner.routineResultToJson(routine, rs);
            }
            else{
                cs.execute();
                obj = ResultSetJsoner.routineResultToJson(routine, cs);
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            obj.put("error", e.getMessage());
        }
        return obj;
    }

    public Structure.Table getTableMetaData(String tableName){
        Structure.Table table = new Structure.Table();
        table.name = tableName; 
        String query = "SELECT column_name, data_type FROM information_schema.columns WHERE table_name = ?";
        try(Connection conn = this.ds.getConnection()){
            PreparedStatement statement = conn.prepareStatement(query);
            statement.setString(1, tableName);
            ResultSet rs = statement.executeQuery();
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
        String query1 = "SELECT routine_name, routine_type, data_type AS return_type FROM information_schema.routines WHERE routine_name = ?";
        String query2 = "SELECT parameter_name, ordinal_position, data_type, parameter_mode FROM information_schema.parameters WHERE specific_name = ?";
        String query3 = "SELECT name, type_name(user_type_id) AS data_type FROM sys.all_columns WHERE object_id = object_id(?)";
        try(Connection conn = this.ds.getConnection()){
            conn.setAutoCommit(false);
            PreparedStatement statement1 = conn.prepareStatement(query1);
            statement1.setString(1, routineName);
            ResultSet rs1 = statement1.executeQuery();
            while(rs1.next()){
                routine.name = rs1.getString("routine_name");
                routine.type = rs1.getString("routine_type");
                routine.returnType = rs1.getString("return_type");
            }
            PreparedStatement statement2 = conn.prepareStatement(query2);
            statement2.setString(1, routineName);
            ResultSet rs2 = statement2.executeQuery();
            while(rs2.next()){
                //SQL Server gets a parameter with ordinal_position of 0 to indicate return type
                //this is redundant since it was previously obtained
                if(rs2.getInt("ordinal_position") > 0){
                    Structure.Parameter parameter = new Structure.Parameter();
                    parameter.name = rs2.getString("parameter_name").substring(1);
                    parameter.dataType = rs2.getString("data_type");
                    parameter.ordinalPosition = rs2.getInt("ordinal_position");
                    parameter.parameterMode = rs2.getString("parameter_mode");
                    routine.parameters.put(parameter.name, parameter); 
                }
            }
            if(routine.returnType != null && routine.returnType.equals("TABLE")){
                PreparedStatement statement3 = conn.prepareStatement(query3);
                statement3.setString(1, routineName);
                ResultSet rs3 = statement3.executeQuery();
                while(rs3.next())
                    routine.returnColumns.put(rs3.getString("name"), rs3.getString("data_type")); 
            }
            conn.commit();
        } catch (SQLException e) {
            System.out.println(e.toString());
            //conn.rollback();
        }
        return routine;
    }
}
