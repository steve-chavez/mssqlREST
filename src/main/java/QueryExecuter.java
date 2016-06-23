
import org.json.*;

import java.sql.*;
import javax.sql.DataSource;

import com.google.gson.*;

import java.util.*;

import fj.data.Either;

public class QueryExecuter{

    private DataSource ds;
    private String defaultRole;
    private final String MISSING = "The resource doesn't exist or permission was denied"; 

    public QueryExecuter(DataSource ds, String defaultRole){
        this.ds = ds;
        this.defaultRole = defaultRole;
    }

    public Either<Object, Object> selectTableMetaData(String tableName, Boolean singular, Optional<String> role){
        try(Connection conn = this.ds.getConnection()){
            conn.setAutoCommit(false);
            if(role.isPresent())
                conn.createStatement().execute(String.format("EXEC AS USER='%s'", role.get()));
            else
                conn.createStatement().execute(String.format("EXEC AS USER='%s'", this.defaultRole));
            String query = "SELECT CHARACTER_MAXIMUM_LENGTH AS max_length, COLUMN_DEFAULT AS [default],"+
                " COLUMN_NAME AS name, DATA_TYPE as type,"+
                " CONVERT(BIT, (CASE WHEN IS_NULLABLE = 'YES' THEN 1 ELSE 0 END)) AS nullable,"+
                " NUMERIC_PRECISION AS precision, NUMERIC_SCALE AS scale"+
                " FROM information_schema.columns WHERE table_name = ? ";
            System.out.println(query);
            Either<Object, Object> result;
            try{
                PreparedStatement statement = conn.prepareStatement(query);
                statement.setString(1, tableName);
                ResultSet rs = statement.executeQuery();
                Object json = ResultSetConverter.convert(rs, singular, ResultSetConverter.Format.JSON);
                result = Either.right(json);
            } catch (SQLException e) {
                JSONObject obj = new JSONObject();
                obj.put("message", e.getMessage());
                obj.put("code", e.getErrorCode());
                result = Either.left(obj);
            }
            conn.createStatement().execute("REVERT");
            conn.commit();
            return result;
        } catch (SQLException e) {
            //This exception is only for connection
            JSONObject obj = new JSONObject();
            obj.put("message", e.getMessage());
            obj.put("code", e.getErrorCode());
            return Either.left(obj);
        }
    }

    public Either<Object, Object> selectAllPrivilegedTables(Optional<String> role){
        try(Connection conn = this.ds.getConnection()){
            conn.setAutoCommit(false);
            String actualRole = this.defaultRole;
            if(role.isPresent()){
                actualRole = role.get();
                conn.createStatement().execute(String.format("EXEC AS USER='%s'", actualRole));
            } else
                conn.createStatement().execute(String.format("EXEC AS USER='%s'", actualRole));
            String query = "SELECT table_schema AS [schema], table_name AS name, " +
                "CONVERT(BIT, MAX(CASE WHEN privilege_type = 'SELECT' THEN 1 ELSE 0 END )) AS selectable,"+
                " CONVERT(BIT, MAX(CASE WHEN privilege_type = 'INSERT' THEN 1 ELSE 0 END )) AS insertable,"+
                " CONVERT(BIT, MAX(CASE WHEN privilege_type = 'UPDATE' THEN 1 ELSE 0 END )) AS updateable,"+
                " CONVERT(BIT, MAX(CASE WHEN privilege_type = 'DELETE' THEN 1 ELSE 0 END )) AS deletable"+
                " FROM information_schema.table_privileges WHERE grantee = ? GROUP BY table_schema,table_name";
            System.out.println(query);
            Either<Object, Object> result;
            try{
                PreparedStatement statement = conn.prepareStatement(query);
                statement.setString(1, actualRole);
                ResultSet rs = statement.executeQuery();
                Object json = ResultSetConverter.convert(rs, false, ResultSetConverter.Format.JSON);
                result = Either.right(json);
            } catch (SQLException e) {
                JSONObject obj = new JSONObject();
                obj.put("message", e.getMessage());
                obj.put("code", e.getErrorCode());
                result = Either.left(obj);
            }
            conn.createStatement().execute("REVERT");
            conn.commit();
            return result;
        } catch (SQLException e) {
            //This exception is only for connection
            JSONObject obj = new JSONObject();
            obj.put("message", e.getMessage());
            obj.put("code", e.getErrorCode());
            return Either.left(obj);
        }
    }

    public Either<Object, Object> selectFrom(
            String tableName, Map<String, String> queryParams, 
            Optional<String> order, Boolean singular, 
            ResultSetConverter.Format format,
            Optional<String> role){
        try(Connection conn = this.ds.getConnection()){
            conn.setAutoCommit(false);
            if(role.isPresent())
                conn.createStatement().execute(String.format("EXEC AS USER='%s'", role.get()));
            else
                conn.createStatement().execute(String.format("EXEC AS USER='%s'", this.defaultRole));
            Optional<Structure.Table> optionalTable = this.getTableStructure(tableName, conn);
            if(!optionalTable.isPresent()){
                JSONObject obj = new JSONObject();
                obj.put("message", MISSING);
                conn.createStatement().execute("REVERT");
                conn.commit();
                return Either.left(obj);
            }else{
                Structure.Table table = optionalTable.get();
                String query = QueryBuilder.selectQuery(table, 
                        queryParams.keySet().toArray(new String[queryParams.size()]), 
                        order);
                System.out.println(query);
                Either<Object, Object> result;
                try{
                    PreparedStatement statement = StatementBuilder.buildPreparedStatement(conn, query, table, queryParams);
                    ResultSet rs = statement.executeQuery();
                    Object json = ResultSetConverter.convert(rs, singular, format);
                    result = Either.right(json);
                }catch(SQLException e){
                    JSONObject obj = new JSONObject();
                    obj.put("message", e.getMessage());
                    obj.put("code", e.getErrorCode());
                    result = Either.left(obj);
                }
                conn.createStatement().execute("REVERT");
                conn.commit();
                return result;
            }
        } catch (SQLException e) {
            //This exception is only for connection
            JSONObject obj = new JSONObject();
            obj.put("message", e.getMessage());
            obj.put("code", e.getErrorCode());
            return Either.left(obj);
        }
    }

    //Single
    public Either<Object, Object> insertInto(String tableName, Map<String, String> values, Optional<String> role){
        try(Connection conn = this.ds.getConnection()){
            conn.setAutoCommit(false);
            if(role.isPresent())
                conn.createStatement().execute(String.format("EXEC AS USER='%s'", role.get()));
            else
                conn.createStatement().execute(String.format("EXEC AS USER='%s'", this.defaultRole));
            Optional<Structure.Table> optionalTable = this.getTableStructure(tableName, conn);
            if(!optionalTable.isPresent()){
                JSONObject obj = new JSONObject();
                obj.put("message", MISSING);
                conn.createStatement().execute("REVERT");
                conn.commit();
                return Either.left(obj);
            }else{
                Structure.Table table = optionalTable.get();
                String query = QueryBuilder.insertQuery(table, new ArrayList<String>(values.keySet()));
                Either<Object, Object> result;
                try{
                    PreparedStatement statement = StatementBuilder.buildPreparedStatement(conn, query, table, values);
                    statement.executeUpdate();
                    ResultSet rs = statement.getGeneratedKeys();
                    Integer id = 0;
                    if(rs.next())
                        id = rs.getInt(1);
                    result = Either.right(id);
                }catch(SQLException e){
                    JSONObject obj = new JSONObject();
                    obj.put("message", e.getMessage());
                    obj.put("code", e.getErrorCode());
                    result = Either.left(obj);
                }
                conn.createStatement().execute("REVERT");
                conn.commit();
                return result;
            }
        } catch (SQLException e) {
            //This exception is only for connection
            JSONObject obj = new JSONObject();
            obj.put("message", e.getMessage());
            obj.put("code", e.getErrorCode());
            return Either.left(obj);
        }
    }

    //Batch
    public Either<Object, Object> insertInto(String tableName, List<Map<String, String>> values, Optional<String> role){
        try(Connection conn = this.ds.getConnection()){
            conn.setAutoCommit(false);
            if(role.isPresent())
                conn.createStatement().execute(String.format("EXEC AS USER='%s'", role.get()));
            else
                conn.createStatement().execute(String.format("EXEC AS USER='%s'", this.defaultRole));
            Optional<Structure.Table> optionalTable = this.getTableStructure(tableName, conn);
            if(!optionalTable.isPresent()){
                JSONObject obj = new JSONObject();
                obj.put("message", MISSING);
                conn.createStatement().execute("REVERT");
                conn.commit();
                return Either.left(obj);
            }else{
                Structure.Table table = optionalTable.get();
                String query = QueryBuilder.insertQuery(table, new ArrayList<String>(values.get(0).keySet()));
                System.out.println(query);
                int[] inserts;
                Either<Object, Object> result;
                try{
                    PreparedStatement statement = StatementBuilder.buildBatchPreparedStatement(conn, query, table, values);
                    inserts = statement.executeBatch();
                    result = Either.right(inserts);
                } catch (SQLException e) {
                    JSONObject obj = new JSONObject();
                    obj.put("message", e.getMessage());
                    obj.put("code", e.getErrorCode());
                    result =  Either.left(obj);
                }
                conn.createStatement().execute("REVERT");
                conn.commit();
                return result;
            }
        } catch (SQLException e) {
            //This exception is only for connection
            JSONObject obj = new JSONObject();
            obj.put("message", e.getMessage());
            obj.put("code", e.getErrorCode());
            return Either.left(obj);
        }
    }


    public Either<Object, Object> updateSet(String tableName, Map<String, String> values, Map<String, String> queryParams, Optional<String> role){
        try(Connection conn = this.ds.getConnection()){
            conn.setAutoCommit(false);
            if(role.isPresent())
                conn.createStatement().execute(String.format("EXEC AS USER='%s'", role.get()));
            else
                conn.createStatement().execute(String.format("EXEC AS USER='%s'", this.defaultRole));
            Optional<Structure.Table> optionalTable = this.getTableStructure(tableName, conn);
            if(!optionalTable.isPresent()){
                JSONObject obj = new JSONObject();
                obj.put("message", MISSING);
                conn.createStatement().execute("REVERT");
                conn.commit();
                return Either.left(obj);
            }else{
                Structure.Table table = optionalTable.get();
                String query = QueryBuilder.updateQuery(
                        table, values.keySet().toArray(new String[values.size()]), 
                        queryParams.keySet().toArray(new String[queryParams.size()])
                );
                System.out.println(query);
                Either<Object, Object> result;
                try{
                    PreparedStatement statement = StatementBuilder.buildPreparedStatement(conn, query, table, values, queryParams);
                    statement.executeUpdate();
                    result = Either.right("");
                } catch (SQLException e) {
                    JSONObject obj = new JSONObject();
                    obj.put("message", e.getMessage());
                    obj.put("code", e.getErrorCode());
                    result =  Either.left(obj);
                }
                conn.createStatement().execute("REVERT");
                conn.commit();
                return result;
            }
        } catch (SQLException e) {
            //This exception is only for connection
            JSONObject obj = new JSONObject();
            obj.put("message", e.getMessage());
            obj.put("code", e.getErrorCode());
            return Either.left(obj);
        }
    }

    public Either<Object, Object> deleteFrom(String tableName, Map<String, String> queryParams, Optional<String> role){
        try(Connection conn = this.ds.getConnection()){
            conn.setAutoCommit(false);
            if(role.isPresent())
                conn.createStatement().execute(String.format("EXEC AS USER='%s'", role.get()));
            else
                conn.createStatement().execute(String.format("EXEC AS USER='%s'", this.defaultRole));
            Optional<Structure.Table> optionalTable = this.getTableStructure(tableName, conn);
            if(!optionalTable.isPresent()){
                JSONObject obj = new JSONObject();
                obj.put("message", MISSING);
                conn.createStatement().execute("REVERT");
                conn.commit();
                return Either.left(obj);
            }else{
                Structure.Table table = optionalTable.get();
                String query = QueryBuilder.deleteQuery(table, queryParams.keySet().toArray(new String[queryParams.size()]));
                System.out.println(query);
                Either<Object, Object> result;
                try{
                    PreparedStatement statement = StatementBuilder.buildPreparedStatement(conn, query, table, queryParams);
                    statement.executeUpdate();
                    result = Either.right("");
                } catch (SQLException e) {
                    JSONObject obj = new JSONObject();
                    obj.put("message", e.getMessage());
                    obj.put("code", e.getErrorCode());
                    result =  Either.left(obj);
                }
                conn.createStatement().execute("REVERT");
                conn.commit();
                return result;
            }
        } catch (SQLException e) {
            //This exception is only for connection
            JSONObject obj = new JSONObject();
            obj.put("message", e.getMessage());
            obj.put("code", e.getErrorCode());
            return Either.left(obj);
        }
    }

    public Either<Map<String, Object>, Map<String, Object>> callRoutine(
            String funcName, Map<String, String> values, 
            Optional<String> role
        ){
            try(Connection conn = this.ds.getConnection()){
            conn.setAutoCommit(false);
            if(role.isPresent())
                conn.createStatement().execute(String.format("EXEC AS USER='%s'", role.get()));
            else
                conn.createStatement().execute(String.format("EXEC AS USER='%s'", this.defaultRole));
            Optional<Structure.Routine> optionalRoutine = this.getRoutineStructure(funcName, conn);
            if(!optionalRoutine.isPresent()){
                Map<String, Object> map = new HashMap<String, Object>();
                map.put("message", MISSING);
                conn.createStatement().execute("REVERT");
                conn.commit();
                return Either.left(map);
            }else{
                Structure.Routine routine = optionalRoutine.get();
                String query = QueryBuilder.functionQuery(routine); 
                System.out.println(query);
                Either<Map<String, Object>, Map<String, Object>> result;
                try{
                    Map<String, Object> map = new HashMap<String, Object>();
                    CallableStatement cs = StatementBuilder
                        .buildCallableStatement(conn, query, routine, values);
                    if(routine.type.equals("FUNCTION")){
                        ResultSet rs = cs.executeQuery();
                        map = ResultSetConverter.routineResultToMap(routine, rs);
                    }
                    else{
                        cs.execute();
                        map = ResultSetConverter.routineResultToMap(routine, cs);
                    }
                    result = Either.right(map);
                } catch (SQLException e) {
                    Map<String, Object> map = new HashMap<String, Object>();
                    map.put("message", e.getMessage());
                    map.put("code", e.getErrorCode());
                    result =  Either.left(map);
                }
                conn.createStatement().execute("REVERT");
                conn.commit();
                return result;
            }
        } catch (SQLException e) {
            //This exception is only for connection
            Map<String, Object> map = new HashMap<String, Object>();
            map.put("message", e.getMessage());
            map.put("code", e.getErrorCode());
            return Either.left(map);
        }
    }

    public Optional<Structure.Table> getTableStructure(String tableName, Connection conn) throws SQLException{
        String query = "SELECT column_name, data_type FROM information_schema.columns WHERE table_name = ?";
        PreparedStatement statement = conn.prepareStatement(query);
        statement.setString(1, tableName);
        ResultSet rs = statement.executeQuery();
        if(rs.isBeforeFirst()){
            Structure.Table table = new Structure.Table();
            table.name = tableName; 
            while(rs.next()){
                table.columns.put(rs.getString("column_name"), rs.getString("data_type"));
            }
            return Optional.of(table);
        }
        else
            return Optional.empty();
    }

    public Optional<Structure.Routine> getRoutineStructure(String routineName, Connection conn) throws SQLException{
        Structure.Routine routine = new Structure.Routine();
        String query1 = "SELECT routine_name, routine_type, data_type AS return_type FROM information_schema.routines WHERE routine_name = ?";
        PreparedStatement statement1 = conn.prepareStatement(query1);
        statement1.setString(1, routineName);
        ResultSet rs1 = statement1.executeQuery();
        if(rs1.isBeforeFirst()){
            while(rs1.next()){
                routine.name = rs1.getString("routine_name");
                routine.type = rs1.getString("routine_type");
                routine.returnType = rs1.getString("return_type");
            }
            String query2 = "SELECT parameter_name, ordinal_position, data_type, parameter_mode FROM information_schema.parameters WHERE specific_name = ?";
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
            String query3 = "SELECT name, type_name(user_type_id) AS data_type FROM sys.all_columns WHERE object_id = object_id(?)";
            if(routine.returnType != null && routine.returnType.equals("TABLE")){
                PreparedStatement statement3 = conn.prepareStatement(query3);
                statement3.setString(1, routineName);
                ResultSet rs3 = statement3.executeQuery();
                while(rs3.next())
                    routine.returnColumns.put(rs3.getString("name"), rs3.getString("data_type")); 
            }
            return Optional.of(routine);
        }else
            return Optional.empty();
    }

}
