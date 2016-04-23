
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

import java.sql.SQLException;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;

public class ResultSetJsoner {

    public static JSONArray convert( ResultSet rs )
        throws SQLException, JSONException {

        JSONArray json = new JSONArray();
        ResultSetMetaData rsmd = rs.getMetaData();
        int numColumns = rsmd.getColumnCount();

        while(rs.next()) {
            JSONObject obj = new JSONObject();

            for (int i=1; i<numColumns+1; i++) {
                String columnName = rsmd.getColumnName(i);

                switch(rsmd.getColumnType(i)){
                    case java.sql.Types.ARRAY:
                        obj.put(columnName, rs.getArray(columnName));
                        break;
                    case java.sql.Types.BIGINT:
                        obj.put(columnName, rs.getInt(columnName));
                        break;
                    case java.sql.Types.BOOLEAN:
                        obj.put(columnName, rs.getBoolean(columnName));
                        break;
                    case java.sql.Types.BLOB:
                        obj.put(columnName, rs.getBlob(columnName));
                        break;
                    case java.sql.Types.DOUBLE:
                        obj.put(columnName, rs.getDouble(columnName));
                        break;
                    case java.sql.Types.FLOAT:
                        obj.put(columnName, rs.getFloat(columnName));
                        break;
                    case java.sql.Types.INTEGER:
                        obj.put(columnName, rs.getInt(columnName));
                        break;
                    case java.sql.Types.NVARCHAR:
                        obj.put(columnName, rs.getNString(columnName));
                        break;
                    case java.sql.Types.VARCHAR:
                        obj.put(columnName, rs.getString(columnName));
                        break;
                    case java.sql.Types.TINYINT:
                        obj.put(columnName, rs.getInt(columnName));
                        break;
                    case java.sql.Types.SMALLINT:
                        obj.put(columnName, rs.getInt(columnName));
                        break;
                    case java.sql.Types.DATE:
                        obj.put(columnName, rs.getDate(columnName));
                        break;
                    case java.sql.Types.TIMESTAMP:
                        obj.put(columnName, rs.getDate(columnName));
                        break;
                    default:
                        obj.put(columnName, rs.getObject(columnName));
                        break;
                }
            }

            json.put(obj);

        }

        return json;
    }

    public static JSONObject funcResultToJson( Structure.Routine routine, ResultSet rs )
        throws SQLException, JSONException {

        JSONObject obj = new JSONObject();

        while(rs.next()) {
            switch(routine.dataType){
                case "bigint":
                case "smallint":
                case "int":
                case "tinyint":
                    obj.put("result", rs.getInt(1));
                    break;
                case "numeric":
                case "decimal":
                    obj.put("result", rs.getDouble(1));
                    break;
                case "float":
                case "real":
                    obj.put("result", rs.getFloat(1));
                    break;
                case "char":
                case "varchar":
                case "text":
                    obj.put("result", rs.getString(1));
                    break;
                case "nchar":
                case "nvarchar":
                case "ntext":
                    obj.put("result", rs.getNString(1));
                    break;
                case "binary":
                case "varbinary":
                case "image":
                    obj.put("result", rs.getBlob(1));
                    break;
                case "date":
                case "datetime":
                case "datetime2":
                case "time":
                case "timestamp":
                    obj.put("result", rs.getDate(1));
                    break;
                default:
                    obj.put("result", rs.getObject(1));
                    break;
            }
        }
        return obj;
    }

}
