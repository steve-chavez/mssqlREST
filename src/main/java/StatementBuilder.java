

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
}
