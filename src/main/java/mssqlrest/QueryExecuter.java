/*
* Class that executes the SQL queries
*
* All of the queries are wrapped in a user impersonation context(EXEC AS USER='')
*/
package mssqlrest;

import java.sql.*;
import javax.sql.DataSource;

import com.google.gson.*;

import java.util.*;

import fj.data.Either;

import org.slf4j.LoggerFactory;

import static mssqlrest.Structure.*;

public class QueryExecuter{

    private static org.slf4j.Logger LOGGER = LoggerFactory.getLogger(QueryExecuter.class);

    private DataSource ds;
    private String schema;

    public QueryExecuter(String schema, DataSource ds){
        this.schema = schema;
        this.ds = ds;
    }

    public Either<Object, Object> selectTableMetaData(String tableName, String role){
        try(Connection conn = this.ds.getConnection()){
            conn.createStatement().execute(String.format("EXEC AS USER='%s'", role));
            String query = QueryBuilder.tableMetaDataQuery();
            LOGGER.info(query);
            Either<Object, Object> result;
            try{
                PreparedStatement statement = conn.prepareStatement(query);
                statement.setString(1, tableName);
                statement.setString(2, this.schema);
                ResultSet rs = statement.executeQuery();
                result = ResultSetConverter.convert(rs, false, Format.JSON, Optional.empty());
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
            String query = QueryBuilder.allPrivilegedTablesQuery();
            LOGGER.info(query);
            Either<Object, Object> result;
            try{
                PreparedStatement statement = conn.prepareStatement(query);
                statement.setString(1, role);
                statement.setString(2, this.schema);
                ResultSet rs = statement.executeQuery();
                result = ResultSetConverter.convert(rs, false, Format.JSON, Optional.empty());
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
            String tableName, Map<String, OperatorVal> filters,
            List<String> select,
            List<Order> order,
            Boolean singular,
            Format format,
            String role){
        try(Connection conn = this.ds.getConnection()){
            conn.setAutoCommit(false);
            conn.createStatement().execute(String.format("EXEC AS USER='%s'", role));
            Optional<Table> optionalTable = getTableStructure(this.schema, tableName, conn);
            if(!optionalTable.isPresent()){
                conn.createStatement().execute("REVERT");
                conn.commit();
                return Either.left(Errors.missingError());
            }else{
                Table table = optionalTable.get();
                String query = QueryBuilder.selectQuery(table,
                        filters,
                        select,
                        order);
                LOGGER.info(query);
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
            Optional<Table> optionalTable = getTableStructure(this.schema, tableName, conn);
            if(!optionalTable.isPresent()){
                conn.createStatement().execute("REVERT");
                conn.commit();
                return Either.left(Errors.missingError());
            }else{
                Table table = optionalTable.get();
                String query = QueryBuilder.insertQuery(table, values.keySet());
                LOGGER.info(query);
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
            Optional<Table> optionalTable = getTableStructure(this.schema, tableName, conn);
            if(!optionalTable.isPresent()){
                conn.createStatement().execute("REVERT");
                conn.commit();
                return Either.left(Errors.missingError());
            }else{
                Table table = optionalTable.get();
                String query = QueryBuilder.insertQuery(table, values.get(0).keySet());
                LOGGER.info(query);
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

    public Either<Object, Object> updateSet(String tableName, Map<String, String> values, Map<String, OperatorVal> filters, String role){
        try(Connection conn = this.ds.getConnection()){
            conn.setAutoCommit(false);
            conn.createStatement().execute(String.format("EXEC AS USER='%s'", role));
            Optional<Table> optionalTable = getTableStructure(this.schema, tableName, conn);
            if(!optionalTable.isPresent()){
                conn.createStatement().execute("REVERT");
                conn.commit();
                return Either.left(Errors.missingError());
            }else{
                Table table = optionalTable.get();
                String query = QueryBuilder.updateQuery(table, values.keySet(), filters);
                LOGGER.info(query);
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

    public Either<Object, Object> deleteFrom(String tableName, Map<String, OperatorVal> filters, String role){
        try(Connection conn = this.ds.getConnection()){
            conn.setAutoCommit(false);
            conn.createStatement().execute(String.format("EXEC AS USER='%s'", role));
            Optional<Table> optionalTable = getTableStructure(this.schema, tableName, conn);
            if(!optionalTable.isPresent()){
                conn.createStatement().execute("REVERT");
                conn.commit();
                return Either.left(Errors.missingError());
            }else{
                Table table = optionalTable.get();
                String query = QueryBuilder.deleteQuery(table, filters);
                LOGGER.info(query);
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
            Format format,
            Boolean singular,
            String role
        ){
        try(Connection conn = this.ds.getConnection()){
            conn.setAutoCommit(false);
            conn.createStatement().execute(String.format("EXEC AS USER='%s'", role));
            Optional<Routine> optionalRoutine = getRoutineStructure(this.schema, funcName, conn);
            if(!optionalRoutine.isPresent()){
                conn.createStatement().execute("REVERT");
                conn.commit();
                return Either.left(Errors.missingError());
            }else{
                Routine routine = optionalRoutine.get();
                String query = QueryBuilder.functionQuery(routine);
                LOGGER.info(query);
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


    private static Optional<Table> getTableStructure(String schema, String tableName, Connection conn) throws SQLException{
        String query = "SELECT column_name, data_type FROM information_schema.columns WHERE table_name = ? AND table_schema = ?";
        LOGGER.info(query);
        PreparedStatement statement = conn.prepareStatement(query);
        statement.setString(1, tableName);
        statement.setString(2, schema);
        ResultSet rs = statement.executeQuery();
        if(rs.isBeforeFirst()){
            Table table = new Table();
            table.name = tableName;
            table.schema = schema;
            while(rs.next()){
                table.columns.put(rs.getString("column_name"), rs.getString("data_type"));
            }
            return Optional.of(table);
        }
        else
            return Optional.empty();
    }

    private static Optional<Routine> getRoutineStructure(String schema, String routineName, Connection conn) throws SQLException{
        Routine routine = new Routine();
        String query1 = "SELECT routine_name, routine_schema, routine_type, data_type AS return_type FROM information_schema.routines WHERE routine_name = ? AND routine_schema = ?";
        LOGGER.info(query1);
        PreparedStatement statement1 = conn.prepareStatement(query1);
        statement1.setString(1, routineName);
        statement1.setString(2, schema);
        ResultSet rs1 = statement1.executeQuery();
        if(rs1.isBeforeFirst()){
            while(rs1.next()){
                routine.schema = rs1.getString("routine_schema");
                routine.name = rs1.getString("routine_name");
                routine.type = rs1.getString("routine_type");
                routine.returnType = rs1.getString("return_type");
            }
            String query2 = "SELECT parameter_name, ordinal_position, data_type, parameter_mode FROM information_schema.parameters WHERE specific_name = ? AND specific_schema = ?";
            LOGGER.info(query2);
            PreparedStatement statement2 = conn.prepareStatement(query2);
            statement2.setString(1, routineName);
            statement2.setString(2, schema);
            ResultSet rs2 = statement2.executeQuery();
            while(rs2.next()){
                //SQL Server gets a parameter with ordinal_position of 0 to indicate return type
                //this is redundant since it was previously obtained
                if(rs2.getInt("ordinal_position") > 0){
                    Parameter parameter = new Parameter();
                    parameter.name = rs2.getString("parameter_name").substring(1);
                    parameter.dataType = rs2.getString("data_type");
                    parameter.ordinalPosition = rs2.getInt("ordinal_position");
                    parameter.parameterMode = rs2.getString("parameter_mode");
                    routine.parameters.put(parameter.name, parameter);
                }
            }
            //Get the RETURNS TABLE structure
            String query3 = "SELECT name, type_name(user_type_id) AS data_type FROM sys.all_columns WHERE object_id = object_id(?)";
            LOGGER.info(query3);
            if(routine.returnType != null && routine.returnType.equals("TABLE")){
                PreparedStatement statement3 = conn.prepareStatement(query3);
                statement3.setString(1, QueryBuilder.quoteName(routine.schema) + "." + QueryBuilder.quoteName(routine.name));
                ResultSet rs3 = statement3.executeQuery();
                while(rs3.next())
                    routine.returnColumns.put(rs3.getString("name"), rs3.getString("data_type"));
            }
            return Optional.of(routine);
        }else
            return Optional.empty();
    }
}
