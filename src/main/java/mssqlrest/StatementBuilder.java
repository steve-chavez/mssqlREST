/*
* Class that builds prepared statements.
*
* It maps the http parsed body and query params to placeholders(?) with their proper type
*/
package mssqlrest;

import java.sql.*;
import java.util.*;
import java.io.*;
import java.nio.charset.*;

import static mssqlrest.Structure.*;

public class StatementBuilder{

    public static PreparedStatement buildSelectPreparedStatement(
            Connection conn,
            String query,
            Table table,
            Map<String, OperatorVal> filters
    ) throws SQLException{
        PreparedStatement statement = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
        Integer i = 1;
        for (Map.Entry<String, OperatorVal> entry : filters.entrySet()) {
          setValue(statement, i, toSqlType(table.columns.get(entry.getKey())), entry.getValue().val);
          i++;
        }
        return statement;
    }

    public static PreparedStatement buildInsertPreparedStatement(
            Connection conn,
            String query,
            Table table,
            Map<String, String> values
    ) throws SQLException{
        PreparedStatement statement = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
        Integer i = 1;
        for (Map.Entry<String, String> entry : values.entrySet()) {
          setValue(statement, i, toSqlType(table.columns.get(entry.getKey())), entry.getValue());
          i++;
        }
        return statement;
    }

    public static PreparedStatement buildBatchPreparedStatement(
            Connection conn,
            String query,
            Table table,
            List<Map<String, String>> valuesList
    ) throws SQLException{
        PreparedStatement statement = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
        for(Map<String, String> values:valuesList){
            Integer i = 1;
            for (Map.Entry<String, String> entry : values.entrySet()) {
              setValue(statement, i, toSqlType(table.columns.get(entry.getKey())), entry.getValue());
              i++;
            }
            statement.addBatch();
        }
        return statement;
    }

    public static PreparedStatement buildUpdatePreparedStatement(
            Connection conn,
            String query,
            Table table,
            Map<String, String> values,
            Map<String, OperatorVal> filters
    ) throws SQLException{
        PreparedStatement statement = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
        Integer i = 1;
        for (Map.Entry<String, String> entry : values.entrySet()) {
            setValue(statement, i, toSqlType(table.columns.get(entry.getKey())), entry.getValue());
            i++;
        }
        for (Map.Entry<String, OperatorVal> entry : filters.entrySet()) {
            setValue(statement, i, toSqlType(entry.getKey()), entry.getValue().val);
            i++;
        }
        return statement;
    }

    public static CallableStatement buildCallableStatement(
            Connection conn,
            String query,
            Routine routine,
            Map<String, String> values
    ) throws SQLException{
        CallableStatement callableStatement = conn.prepareCall(query);
        for (Map.Entry<String, String> entry : values.entrySet()) {
            Parameter parameter = routine.parameters.get(entry.getKey());
            setValue(callableStatement, parameter.ordinalPosition, toSqlType(parameter.dataType), entry.getValue());
        }
        for (Map.Entry<String, Parameter> entry : routine.parameters.entrySet()) {
            Parameter parameter = entry.getValue();
            if(parameter.isOut()){
              try{
                callableStatement.registerOutParameter(parameter.ordinalPosition, toSqlType(parameter.dataType));
              }catch(NumberFormatException nfe){
                callableStatement.registerOutParameter(parameter.ordinalPosition, java.sql.Types.VARCHAR);
              }
            }
        }
        return callableStatement;
    }

    private static void setValue(PreparedStatement stmt, int index, int type, String value) throws SQLException{
      try{
        if(value == null)
          stmt.setObject(index, null);
        else switch(type){
          case java.sql.Types.BIGINT:
                                        stmt.setInt(index, Integer.parseInt(value));
                                        break;
          case java.sql.Types.SMALLINT:
                                        stmt.setInt(index, Integer.parseInt(value));
                                        break;
          case java.sql.Types.INTEGER:
                                        stmt.setInt(index, Integer.parseInt(value));
                                        break;
          case java.sql.Types.TINYINT:
                                        stmt.setInt(index, Integer.parseInt(value));
                                        break;
          case java.sql.Types.DOUBLE:
                                        stmt.setDouble(index, Double.parseDouble(value));
                                        break;
          case java.sql.Types.FLOAT:
                                        // The method "statement.setFloat" gives weird results. Example:
                                        // When Float.parseFloat(entry.getValue()) gives 0.0169
                                        // statementSetFloat enters the value 0.016899999231100082 in the database
                                        // stmt.setFloat(index, Float.parseFloat(value));
                                        stmt.setDouble(index, Double.parseDouble(value));
                                        break;
          case java.sql.Types.VARCHAR:
                                        stmt.setString(index, value);
                                        break;
          case java.sql.Types.NVARCHAR:
                                        stmt.setNString(index, value);
                                        break;
          case java.sql.Types.BLOB:
                                        stmt.setBlob(index, new ByteArrayInputStream(value.getBytes(StandardCharsets.UTF_8)));
                                        break;
          default:
                                        stmt.setObject(index, value);
                                        break;
        }
      } catch(NumberFormatException nfe){
        stmt.setString(index, value);
      }
    }

}
