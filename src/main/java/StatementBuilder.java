

import java.sql.*;
import java.util.*;
import java.io.*;
import java.nio.charset.*;

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
            switch(parameter.dataType){
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
                    statement.setFloat(parameter.ordinalPosition, Float.parseFloat(entry.getValue()));
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

    public static CallableStatement buildCallableStatement(
            Connection conn, 
            String query, 
            Structure.Routine routine,
            Map<String, String> values
    ) throws SQLException{
        CallableStatement callableStatement = conn.prepareCall(query);
        for (Map.Entry<String, String> entry : values.entrySet()) {
            Structure.Parameter parameter = routine.parameters.get(entry.getKey());
            switch(parameter.dataType){
                case "bigint":
                case "smallint":
                case "int":
                case "tinyint":
                    if(parameter.parameterMode.equals("OUT") || parameter.parameterMode.equals("INOUT"))
                        callableStatement.registerOutParameter(parameter.ordinalPosition, java.sql.Types.INTEGER);
                    callableStatement.setInt(parameter.ordinalPosition, Integer.parseInt(entry.getValue()));
                    break;
                case "numeric":
                case "decimal":
                    if(parameter.parameterMode.equals("OUT") || parameter.parameterMode.equals("INOUT"))
                        callableStatement.registerOutParameter(parameter.ordinalPosition, java.sql.Types.DOUBLE);
                    callableStatement.setDouble(parameter.ordinalPosition, Double.parseDouble(entry.getValue()));
                    break;
                case "float":
                case "real":
                    if(parameter.parameterMode.equals("OUT") || parameter.parameterMode.equals("INOUT"))
                        callableStatement.registerOutParameter(parameter.ordinalPosition, java.sql.Types.FLOAT);
                    callableStatement.setFloat(parameter.ordinalPosition, Float.parseFloat(entry.getValue()));
                    break;
                case "char":
                case "varchar":
                case "text":
                    if(parameter.parameterMode.equals("OUT") || parameter.parameterMode.equals("INOUT"))
                        callableStatement.registerOutParameter(parameter.ordinalPosition, java.sql.Types.VARCHAR);
                    callableStatement.setString(parameter.ordinalPosition, entry.getValue());
                    break;
                case "nchar":
                case "nvarchar":
                case "ntext":
                    if(parameter.parameterMode.equals("OUT") || parameter.parameterMode.equals("INOUT"))
                        callableStatement.registerOutParameter(parameter.ordinalPosition, java.sql.Types.NVARCHAR);
                    callableStatement.setNString(parameter.ordinalPosition, entry.getValue());
                    break;
                case "date":
                case "datetime":
                case "datetime2":
                case "time":
                case "timestamp":
                    if(parameter.parameterMode.equals("OUT") || parameter.parameterMode.equals("INOUT"))
                        callableStatement.registerOutParameter(parameter.ordinalPosition, java.sql.Types.DATE);
                    callableStatement.setString(parameter.ordinalPosition, entry.getValue());
                    break;
                case "binary":
                case "varbinary":
                case "image":
                    if(parameter.parameterMode.equals("OUT") || parameter.parameterMode.equals("INOUT"))
                        callableStatement.registerOutParameter(parameter.ordinalPosition, java.sql.Types.BLOB);
                    callableStatement.setBlob(parameter.ordinalPosition,
                        new ByteArrayInputStream(entry.getValue().getBytes(StandardCharsets.UTF_8)));
                    break;
                default:
                    if(parameter.parameterMode.equals("OUT") || parameter.parameterMode.equals("INOUT"))
                        callableStatement.registerOutParameter(parameter.ordinalPosition, java.sql.Types.JAVA_OBJECT);
                    callableStatement.setObject(parameter.ordinalPosition, entry.getValue());
                    break;
            }
        }
        return callableStatement;
    }
}
