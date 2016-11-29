
import java.sql.*;
import javax.sql.DataSource;

import com.google.gson.*;

import java.util.*;

import fj.data.Either;

public class QueryExecuter{

    private DataSource ds;
    private String schema;

    public QueryExecuter(String schema, DataSource ds){
        this.schema = schema;
        this.ds = ds;
    }

    public Either<Object, Object> selectTableMetaData(String tableName, String role){
        try(Connection conn = this.ds.getConnection()){
            conn.createStatement().execute(String.format("EXEC AS USER='%s'", role));
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
                result = ResultSetConverter.convert(rs, false, Structure.Format.JSON, Optional.empty());
            } catch (SQLException e) {
                result = Either.left(Errors.exceptionToJson(e));
            }
            conn.createStatement().execute("REVERT");
            conn.commit();
            return result;
        } catch (SQLException e) {
            //This exception is only for connection. Not using REVERT.
            return Either.left(Errors.exceptionToJson(e));
        }
    }

    public Either<Object, Object> selectAllPrivilegedTables(String role){
        try(Connection conn = this.ds.getConnection()){
            conn.setAutoCommit(false);
            conn.createStatement().execute(String.format("EXEC AS USER='%s'", role));
            String query = "SELECT table_name AS [name], " +
                "CONVERT(BIT, MAX(CASE WHEN privilege_type = 'SELECT' THEN 1 ELSE 0 END )) AS selectable,"+
                " CONVERT(BIT, MAX(CASE WHEN privilege_type = 'INSERT' THEN 1 ELSE 0 END )) AS insertable,"+
                " CONVERT(BIT, MAX(CASE WHEN privilege_type = 'UPDATE' THEN 1 ELSE 0 END )) AS updateable,"+
                " CONVERT(BIT, MAX(CASE WHEN privilege_type = 'DELETE' THEN 1 ELSE 0 END )) AS deletable"+
                " FROM information_schema.table_privileges WHERE grantee = ? AND table_schema = ? GROUP BY table_schema,table_name";
            System.out.println(query);
            Either<Object, Object> result;
            try{
                PreparedStatement statement = conn.prepareStatement(query);
                statement.setString(1, role);
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
            return Either.left(Errors.exceptionToJson(e));
        }
    }

    public Either<Object, Object> selectFrom(
            String tableName, Map<String, Structure.OperatorVal> filters,
            List<String> select,
            List<Structure.Order> order,
            Boolean singular,
            Structure.Format format,
            String role){
        try(Connection conn = this.ds.getConnection()){
            conn.setAutoCommit(false);
            conn.createStatement().execute(String.format("EXEC AS USER='%s'", role));
            Optional<Structure.Table> optionalTable = Structure.getTableStructure(this.schema, tableName, conn);
            if(!optionalTable.isPresent()){
                conn.createStatement().execute("REVERT");
                conn.commit();
                return Either.left(Errors.missingError());
            }else{
                Structure.Table table = optionalTable.get();
                String query = QueryBuilder.selectQuery(table,
                        filters,
                        select,
                        order);
                System.out.println(query);
                Either<Object, Object> result;
                try{
                    PreparedStatement statement = StatementBuilder.buildSelectPreparedStatement(conn, query, table, filters);
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
            return Either.left(Errors.exceptionToJson(e));
        }
    }

    public Either<Object, Object> insertInto(String tableName, Map<String, String> values, String role){
        try(Connection conn = this.ds.getConnection()){
            conn.setAutoCommit(false);
            conn.createStatement().execute(String.format("EXEC AS USER='%s'", role));
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
                    PreparedStatement statement = StatementBuilder.buildInsertPreparedStatement(conn, query, table, values);
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
            return Either.left(Errors.exceptionToJson(e));
        }
    }

    public Either<Object, Object> batchInsertInto(String tableName, List<Map<String, String>> values, String role){
        try(Connection conn = this.ds.getConnection()){
            conn.setAutoCommit(false);
            conn.createStatement().execute(String.format("EXEC AS USER='%s'", role));
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
            return Either.left(Errors.exceptionToJson(e));
        }
    }

    public Either<Object, Object> updateSet(String tableName, Map<String, String> values, Map<String, Structure.OperatorVal> filters, String role){
        try(Connection conn = this.ds.getConnection()){
            conn.setAutoCommit(false);
            conn.createStatement().execute(String.format("EXEC AS USER='%s'", role));
            Optional<Structure.Table> optionalTable = Structure.getTableStructure(this.schema, tableName, conn);
            if(!optionalTable.isPresent()){
                conn.createStatement().execute("REVERT");
                conn.commit();
                return Either.left(Errors.missingError());
            }else{
                Structure.Table table = optionalTable.get();
                String query = QueryBuilder.updateQuery(table, values.keySet(), filters);
                System.out.println(query);
                Either<Object, Object> result;
                try{
                    PreparedStatement statement = StatementBuilder.buildUpdatePreparedStatement(conn, query, table, values, filters);
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
            return Either.left(Errors.exceptionToJson(e));
        }
    }

    public Either<Object, Object> deleteFrom(String tableName, Map<String, Structure.OperatorVal> filters, String role){
        try(Connection conn = this.ds.getConnection()){
            conn.setAutoCommit(false);
            conn.createStatement().execute(String.format("EXEC AS USER='%s'", role));
            Optional<Structure.Table> optionalTable = Structure.getTableStructure(this.schema, tableName, conn);
            if(!optionalTable.isPresent()){
                conn.createStatement().execute("REVERT");
                conn.commit();
                return Either.left(Errors.missingError());
            }else{
                Structure.Table table = optionalTable.get();
                String query = QueryBuilder.deleteQuery(table, filters);
                System.out.println(query);
                Either<Object, Object> result;
                try{
                    PreparedStatement statement = StatementBuilder.buildSelectPreparedStatement(conn, query, table, filters);
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
            return Either.left(Errors.exceptionToJson(e));
        }
    }

    public Either<Object, Object> callRoutine(
            String funcName,
            Map<String, String> values,
            Structure.Format format,
            Boolean singular,
            String role
        ){
        try(Connection conn = this.ds.getConnection()){
            conn.setAutoCommit(false);
            conn.createStatement().execute(String.format("EXEC AS USER='%s'", role));
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
