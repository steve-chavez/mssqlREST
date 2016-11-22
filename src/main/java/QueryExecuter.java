
import java.sql.*;
import javax.sql.DataSource;

import com.google.gson.*;

import java.util.*;

import fj.data.Either;

public class QueryExecuter{

    private DataSource ds;
    private String schema;
    private String defaultRole;

    public QueryExecuter(String schema, DataSource ds, String defaultRole){
        this.schema = schema;
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
            String query =
              "SELECT CHARACTER_MAXIMUM_LENGTH AS max_length, COLUMN_DEFAULT AS [default], " +
              "CONVERT(BIT, (CASE WHEN IS_NULLABLE = 'YES' THEN 1 ELSE 0 END)) AS nullable, " +
              "COLUMN_NAME AS name, DATA_TYPE as type, " +
              "NUMERIC_PRECISION AS precision, NUMERIC_SCALE AS scale " +
              "FROM information_schema.columns WHERE table_name = ? AND table_schema = ?";
            System.out.println(query);
            Either<Object, Object> result;
            try{
                PreparedStatement statement = conn.prepareStatement(query);
                statement.setString(1, tableName);
                statement.setString(2, this.schema);
                ResultSet rs = statement.executeQuery();
                result = ResultSetConverter.convert(rs, singular, Structure.Format.JSON, Optional.empty());
            } catch (SQLException e) {
                result = Either.left(Errors.exceptionToJson(e));
            }
            conn.createStatement().execute("REVERT");
            conn.commit();
            return result;
        } catch (SQLException e) {
            //This exception is only for connection
            return Either.left(Errors.exceptionToJson(e));
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
                " FROM information_schema.table_privileges WHERE grantee = ? AND table_schema = ? GROUP BY table_schema,table_name";
            System.out.println(query);
            Either<Object, Object> result;
            try{
                PreparedStatement statement = conn.prepareStatement(query);
                statement.setString(1, actualRole);
                statement.setString(2, this.schema);
                ResultSet rs = statement.executeQuery();
                result = ResultSetConverter.convert(rs, false, Structure.Format.JSON, Optional.empty());
            } catch (SQLException e) {
                result = Either.left(Errors.exceptionToJson(e));
            }
            conn.createStatement().execute("REVERT");
            conn.commit();
            return result;
        } catch (SQLException e) {
            //This exception is only for connection
            return Either.left(Errors.exceptionToJson(e));
        }
    }

    public Either<Object, Object> selectFrom(
            String tableName, Map<String, String> queryParams,
            Optional<String> selectColumns,
            Optional<String> order,
            Boolean singular,
            Structure.Format format,
            Optional<String> role){
        try(Connection conn = this.ds.getConnection()){
            conn.setAutoCommit(false);
            if(role.isPresent())
                conn.createStatement().execute(String.format("EXEC AS USER='%s'", role.get()));
            else
                conn.createStatement().execute(String.format("EXEC AS USER='%s'", this.defaultRole));
            Optional<Structure.Table> optionalTable = Structure.getTableStructure(this.schema, tableName, conn);
            if(!optionalTable.isPresent()){
                conn.createStatement().execute("REVERT");
                conn.commit();
                return Either.left(Errors.missingError());
            }else{
                Structure.Table table = optionalTable.get();
                String query = QueryBuilder.selectQuery(table,
                        queryParams.keySet(),
                        selectColumns,
                        order);
                System.out.println(query);
                Either<Object, Object> result;
                try{
                    PreparedStatement statement = StatementBuilder.buildPreparedStatement(conn, query, table, queryParams);
                    ResultSet rs = statement.executeQuery();
                    result = ResultSetConverter.convert(rs, singular, format, Optional.empty());
                }catch(SQLException e){
                    result = Either.left(Errors.exceptionToJson(e));
                }
                conn.createStatement().execute("REVERT");
                conn.commit();
                return result;
            }
        } catch (SQLException e) {
            //This exception is only for connection
            return Either.left(Errors.exceptionToJson(e));
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
            Optional<Structure.Table> optionalTable = Structure.getTableStructure(this.schema, tableName, conn);
            if(!optionalTable.isPresent()){
                conn.createStatement().execute("REVERT");
                conn.commit();
                return Either.left(Errors.missingError());
            }else{
                Structure.Table table = optionalTable.get();
                String query = QueryBuilder.insertQuery(table, values.keySet());
                System.out.println(query);
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
                    result = Either.left(Errors.exceptionToJson(e));
                }
                conn.createStatement().execute("REVERT");
                conn.commit();
                return result;
            }
        } catch (SQLException e) {
            //This exception is only for connection
            return Either.left(Errors.exceptionToJson(e));
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
            Optional<Structure.Table> optionalTable = Structure.getTableStructure(this.schema, tableName, conn);
            if(!optionalTable.isPresent()){
                conn.createStatement().execute("REVERT");
                conn.commit();
                return Either.left(Errors.missingError());
            }else{
                Structure.Table table = optionalTable.get();
                String query = QueryBuilder.insertQuery(table, values.get(0).keySet());
                System.out.println(query);
                int[] inserts;
                Either<Object, Object> result;
                try{
                    PreparedStatement statement = StatementBuilder.buildBatchPreparedStatement(conn, query, table, values);
                    inserts = statement.executeBatch();
                    result = Either.right(inserts.length);
                } catch (SQLException e) {
                    result = Either.left(Errors.exceptionToJson(e));
                }
                conn.createStatement().execute("REVERT");
                conn.commit();
                return result;
            }
        } catch (SQLException e) {
            //This exception is only for connection
            return Either.left(Errors.exceptionToJson(e));
        }
    }

    public Either<Object, Object> updateSet(String tableName, Map<String, String> values, Map<String, String> queryParams, Optional<String> role){
        try(Connection conn = this.ds.getConnection()){
            conn.setAutoCommit(false);
            if(role.isPresent())
                conn.createStatement().execute(String.format("EXEC AS USER='%s'", role.get()));
            else
                conn.createStatement().execute(String.format("EXEC AS USER='%s'", this.defaultRole));
            Optional<Structure.Table> optionalTable = Structure.getTableStructure(this.schema, tableName, conn);
            if(!optionalTable.isPresent()){
                conn.createStatement().execute("REVERT");
                conn.commit();
                return Either.left(Errors.missingError());
            }else{
                Structure.Table table = optionalTable.get();
                String query = QueryBuilder.updateQuery(table, values.keySet(), queryParams.keySet());
                System.out.println(query);
                Either<Object, Object> result;
                try{
                    PreparedStatement statement = StatementBuilder.buildPreparedStatement(conn, query, table, values, queryParams);
                    statement.executeUpdate();
                    result = Either.right("");
                } catch (SQLException e) {
                    result = Either.left(Errors.exceptionToJson(e));
                }
                conn.createStatement().execute("REVERT");
                conn.commit();
                return result;
            }
        } catch (SQLException e) {
            //This exception is only for connection
            return Either.left(Errors.exceptionToJson(e));
        }
    }

    public Either<Object, Object> deleteFrom(String tableName, Map<String, String> queryParams, Optional<String> role){
        try(Connection conn = this.ds.getConnection()){
            conn.setAutoCommit(false);
            if(role.isPresent())
                conn.createStatement().execute(String.format("EXEC AS USER='%s'", role.get()));
            else
                conn.createStatement().execute(String.format("EXEC AS USER='%s'", this.defaultRole));
            Optional<Structure.Table> optionalTable = Structure.getTableStructure(this.schema, tableName, conn);
            if(!optionalTable.isPresent()){
                conn.createStatement().execute("REVERT");
                conn.commit();
                return Either.left(Errors.missingError());
            }else{
                Structure.Table table = optionalTable.get();
                String query = QueryBuilder.deleteQuery(table, queryParams.keySet());
                System.out.println(query);
                Either<Object, Object> result;
                try{
                    PreparedStatement statement = StatementBuilder.buildPreparedStatement(conn, query, table, queryParams);
                    statement.executeUpdate();
                    result = Either.right("");
                } catch (SQLException e) {
                    result = Either.left(Errors.exceptionToJson(e));
                }
                conn.createStatement().execute("REVERT");
                conn.commit();
                return result;
            }
        } catch (SQLException e) {
            //This exception is only for connection
            return Either.left(Errors.exceptionToJson(e));
        }
    }

    public Either<Object, Object> callRoutine(
            String funcName,
            Map<String, String> values,
            Structure.Format format,
            Boolean singular,
            Optional<String> role
        ){
        try(Connection conn = this.ds.getConnection()){
            conn.setAutoCommit(false);
            if(role.isPresent())
                conn.createStatement().execute(String.format("EXEC AS USER='%s'", role.get()));
            else
                conn.createStatement().execute(String.format("EXEC AS USER='%s'", this.defaultRole));
            Optional<Structure.Routine> optionalRoutine = Structure.getRoutineStructure(this.schema, funcName, conn);
            if(!optionalRoutine.isPresent()){
                conn.createStatement().execute("REVERT");
                conn.commit();
                return Either.left(Errors.missingError());
            }else{
                Structure.Routine routine = optionalRoutine.get();
                String query = QueryBuilder.functionQuery(routine);
                System.out.println(query);
                Either<Object, Object> result;
                try{
                    CallableStatement cs = StatementBuilder
                        .buildCallableStatement(conn, query, routine, values);
                    if(routine.isFunction()){
                      ResultSet rs = cs.executeQuery();
                      result = ResultSetConverter.convert(rs, singular, format, Optional.of(routine));
                    }else{
                      cs.execute();
                      result = ResultSetConverter.convert(cs, routine, format);
                    }
                } catch (SQLException e) {
                    result = Either.left(Errors.exceptionToJson(e));
                }
                conn.createStatement().execute("REVERT");
                conn.commit();
                return result;
            }
        } catch (SQLException e) {
            return Either.left(Errors.exceptionToJson(e));
        }
    }

}
