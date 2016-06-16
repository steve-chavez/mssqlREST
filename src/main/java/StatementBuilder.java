

import java.sql.*;
import java.util.*;
import java.io.*;
import java.nio.charset.*;

// **** IMPORTANT ****
// The method "statement.setFloat" gets weird results don't know why, example:
// When Float.parseFloat(entry.getValue()) gives 0.0169
// statementSetFloat enters the value 0.016899999231100082 in the database
public class StatementBuilder{

    public static PreparedStatement buildPreparedStatement(
            Connection conn, 
            String query, 
            Structure.Routine routine,
            Map<String, String> values
    ) throws SQLException{
        PreparedStatement statement = conn.prepareStatement(query);
        for (Map.Entry<String, String> entry : values.entrySet()) {
            Structure.Parameter parameter = routine.parameters.get(entry.getKey());
            //if(parameter.parameterMode.equals("IN"))
            if(entry.getValue()==null)
                statement.setObject(parameter.ordinalPosition, entry.getValue());
            else switch(parameter.dataType){
                case "bigint":
                case "smallint":
                case "int":
                case "tinyint":
                    statement.setInt(parameter.ordinalPosition, Integer.parseInt(entry.getValue()));
                    break;
                case "numeric":
                case "decimal":
                    statement.setDouble(parameter.ordinalPosition, Double.parseDouble(entry.getValue()));
                    break;
                case "float":
                case "real":
                    //statement.setFloat(parameter.ordinalPosition, Float.parseFloat(entry.getValue()));
                    statement.setDouble(parameter.ordinalPosition, Double.parseDouble(entry.getValue()));
                    break;
                case "char":
                case "varchar":
                case "text":
                    statement.setString(parameter.ordinalPosition, entry.getValue());
                    break;
                case "nchar":
                case "nvarchar":
                case "ntext":
                    statement.setNString(parameter.ordinalPosition, entry.getValue());
                    break;
                case "date":
                case "datetime":
                case "datetime2":
                case "time":
                case "timestamp":
                    statement.setString(parameter.ordinalPosition, entry.getValue());
                    break;
                case "binary":
                case "varbinary":
                case "image":
                    statement.setBlob(parameter.ordinalPosition, new ByteArrayInputStream(entry.getValue().getBytes(StandardCharsets.UTF_8)));
                    break;
                default:
                    statement.setObject(parameter.ordinalPosition, entry.getValue());
                    break;
            }
        }
        return statement;
    }

    public static PreparedStatement buildPreparedStatement(
            Connection conn, 
            String query, 
            Structure.Table table,
            Map<String, String> values
    ) throws SQLException{
        PreparedStatement statement = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
        Integer i = 1;
        for (Map.Entry<String, String> entry : values.entrySet()) {
            if(entry.getValue()==null)
                statement.setObject(i, entry.getValue());
            else switch(table.columns.get(entry.getKey())){
                case "bigint":
                case "smallint":
                case "int":
                case "tinyint":
                    statement.setInt(i, Integer.parseInt(entry.getValue()));
                    break;
                case "numeric":
                case "decimal":
                    statement.setDouble(i, Double.parseDouble(entry.getValue()));
                    break;
                case "float":
                case "real":
                    //statement.setFloat(i, Float.parseFloat(entry.getValue())); 
                    statement.setDouble(i, Double.parseDouble(entry.getValue()));
                    break;
                case "char":
                case "varchar":
                case "text":
                    statement.setString(i, entry.getValue());
                    break;
                case "nchar":
                case "nvarchar":
                case "ntext":
                    statement.setNString(i, entry.getValue());
                    break;
                case "date":
                case "datetime":
                case "datetime2":
                case "time":
                case "timestamp":
                    statement.setString(i, entry.getValue());
                    break;
                case "binary":
                case "varbinary":
                case "image":
                    statement.setBlob(i, new ByteArrayInputStream(entry.getValue().getBytes(StandardCharsets.UTF_8)));
                    break;
                default:
                    statement.setObject(i, entry.getValue());
                    break;
            }
            i++;
        }
        return statement;
    }

    public static PreparedStatement buildBatchPreparedStatement(
            Connection conn, 
            String query, 
            Structure.Table table,
            List<Map<String, String>> valuesList
    ) throws SQLException{
        PreparedStatement statement = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
        for(Map<String, String> values:valuesList){
            Integer i = 1;
            for (Map.Entry<String, String> entry : values.entrySet()) {
                if(entry.getValue()==null)
                    statement.setObject(i, entry.getValue());
                else switch(table.columns.get(entry.getKey())){
                    case "bigint":
                    case "smallint":
                    case "int":
                    case "tinyint":
                        statement.setInt(i, Integer.parseInt(entry.getValue()));
                        break;
                    case "numeric":
                    case "decimal":
                        statement.setDouble(i, Double.parseDouble(entry.getValue()));
                        break;
                    case "float":
                    case "real":
                        //statement.setFloat(i, Float.parseFloat(entry.getValue())); 
                        statement.setDouble(i, Double.parseDouble(entry.getValue()));
                        break;
                    case "char":
                    case "varchar":
                    case "text":
                        statement.setString(i, entry.getValue());
                        break;
                    case "nchar":
                    case "nvarchar":
                    case "ntext":
                        statement.setNString(i, entry.getValue());
                        break;
                    case "date":
                    case "datetime":
                    case "datetime2":
                    case "time":
                    case "timestamp":
                        statement.setString(i, entry.getValue());
                        break;
                    case "binary":
                    case "varbinary":
                    case "image":
                        statement.setBlob(i, new ByteArrayInputStream(entry.getValue().getBytes(StandardCharsets.UTF_8)));
                        break;
                    default:
                        statement.setObject(i, entry.getValue());
                        break;
                }
                i++;
            }
            statement.addBatch();
        }
        return statement;
    }

    public static PreparedStatement buildPreparedStatement(
            Connection conn, 
            String query, 
            Structure.Table table,
            Map<String, String> values,
            Map<String, String> queryParams
    ) throws SQLException{
        PreparedStatement statement = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
        Integer i = 1;
        for (Map.Entry<String, String> entry : values.entrySet()) {
            if(entry.getValue()==null)
                statement.setObject(i, entry.getValue());
            else switch(table.columns.get(entry.getKey())){
                case "bigint":
                case "smallint":
                case "int":
                case "tinyint":
                    statement.setInt(i, Integer.parseInt(entry.getValue()));
                    break;
                case "numeric":
                case "decimal":
                    statement.setDouble(i, Double.parseDouble(entry.getValue()));
                    break;
                case "float":
                case "real":
                    //statement.setFloat(i, Float.parseFloat(entry.getValue()));
                    statement.setDouble(i, Double.parseDouble(entry.getValue()));
                    break;
                case "char":
                case "varchar":
                case "text":
                    statement.setString(i, entry.getValue());
                    break;
                case "nchar":
                case "nvarchar":
                case "ntext":
                    statement.setNString(i, entry.getValue());
                    break;
                case "date":
                case "datetime":
                case "datetime2":
                case "time":
                case "timestamp":
                    statement.setString(i, entry.getValue());
                    break;
                case "binary":
                case "varbinary":
                case "image":
                    statement.setBlob(i, new ByteArrayInputStream(entry.getValue().getBytes(StandardCharsets.UTF_8)));
                    break;
                default:
                    statement.setObject(i, entry.getValue());
                    break;
            }
            i++;
        }
        for (Map.Entry<String, String> entry : queryParams.entrySet()) {
            if(entry.getValue()==null)
                statement.setObject(i, entry.getValue());
            else switch(table.columns.get(entry.getKey())){
                case "bigint":
                case "smallint":
                case "int":
                case "tinyint":
                    statement.setInt(i, Integer.parseInt(entry.getValue()));
                    break;
                case "numeric":
                case "decimal":
                    statement.setDouble(i, Double.parseDouble(entry.getValue()));
                    break;
                case "float":
                case "real":
                    //statement.setFloat(i, Float.parseFloat(entry.getValue()));
                    statement.setDouble(i, Double.parseDouble(entry.getValue()));
                    break;
                case "char":
                case "varchar":
                case "text":
                    statement.setString(i, entry.getValue());
                    break;
                case "nchar":
                case "nvarchar":
                case "ntext":
                    statement.setNString(i, entry.getValue());
                    break;
                case "date":
                case "datetime":
                case "datetime2":
                case "time":
                case "timestamp":
                    statement.setString(i, entry.getValue());
                    break;
                case "binary":
                case "varbinary":
                case "image":
                    statement.setBlob(i, new ByteArrayInputStream(entry.getValue().getBytes(StandardCharsets.UTF_8)));
                    break;
                default:
                    statement.setObject(i, entry.getValue());
                    break;
            }
            i++;
        }
        return statement;
    }

    public static CallableStatement buildCallableStatement(
            Connection conn, 
            String query, 
            Structure.Routine routine,
            Map<String, String> values
    ) throws SQLException{
        CallableStatement callableStatement = conn.prepareCall(query);
        for (Map.Entry<String, String> entry : values.entrySet()) {
            Structure.Parameter parameter = routine.parameters.get(entry.getKey());
            if(entry.getValue()==null)
                callableStatement.setObject(parameter.ordinalPosition, null);
            switch(parameter.dataType){
                case "bigint":
                case "smallint":
                case "int":
                case "tinyint":
                    callableStatement.setInt(parameter.ordinalPosition, Integer.parseInt(entry.getValue()));
                    break;
                case "numeric":
                case "decimal":
                    callableStatement.setDouble(parameter.ordinalPosition, Double.parseDouble(entry.getValue()));
                    break;
                case "float":
                case "real":
                    //callableStatement.setFloat(parameter.ordinalPosition, Float.parseFloat(entry.getValue()));
                    callableStatement.setDouble(parameter.ordinalPosition, Double.parseDouble(entry.getValue()));
                    break;
                case "char":
                case "varchar":
                case "text":
                    callableStatement.setString(parameter.ordinalPosition, entry.getValue());
                    break;
                case "nchar":
                case "nvarchar":
                case "ntext":
                    callableStatement.setNString(parameter.ordinalPosition, entry.getValue());
                    break;
                case "date":
                case "datetime":
                case "datetime2":
                case "time":
                case "timestamp":
                    callableStatement.setString(parameter.ordinalPosition, entry.getValue());
                    break;
                case "binary":
                case "varbinary":
                case "image":
                    callableStatement.setBlob(parameter.ordinalPosition,
                        new ByteArrayInputStream(entry.getValue().getBytes(StandardCharsets.UTF_8)));
                    break;
                default:
                    callableStatement.setObject(parameter.ordinalPosition, entry.getValue());
                    break;
            }
        }
        for (Map.Entry<String, Structure.Parameter> entry : routine.parameters.entrySet()) {
            Structure.Parameter parameter = entry.getValue();
            if(parameter.parameterMode.equals("INOUT")){
                switch(parameter.dataType){
                    case "bigint":
                    case "smallint":
                    case "int":
                    case "tinyint":
                        callableStatement.registerOutParameter(parameter.ordinalPosition, java.sql.Types.INTEGER);
                        break;
                    case "numeric":
                    case "decimal":
                        callableStatement.registerOutParameter(parameter.ordinalPosition, java.sql.Types.DOUBLE);
                        break;
                    case "float":
                    case "real":
                        callableStatement.registerOutParameter(parameter.ordinalPosition, java.sql.Types.FLOAT);
                        break;
                    case "char":
                    case "varchar":
                    case "text":
                        callableStatement.registerOutParameter(parameter.ordinalPosition, java.sql.Types.VARCHAR);
                        break;
                    case "nchar":
                    case "nvarchar":
                    case "ntext":
                        callableStatement.registerOutParameter(parameter.ordinalPosition, java.sql.Types.NVARCHAR);
                        break;
                    case "date":
                    case "datetime":
                    case "datetime2":
                    case "time":
                    case "timestamp":
                        callableStatement.registerOutParameter(parameter.ordinalPosition, java.sql.Types.DATE);
                        break;
                    case "binary":
                    case "varbinary":
                    case "image":
                        callableStatement.registerOutParameter(parameter.ordinalPosition, java.sql.Types.BLOB);
                        break;
                    default:
                        callableStatement.registerOutParameter(parameter.ordinalPosition, java.sql.Types.JAVA_OBJECT);
                        break;
                }
            }
        }
        return callableStatement;
    }
}
