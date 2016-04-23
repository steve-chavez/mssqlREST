
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

import java.sql.SQLException;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;

import java.util.*;

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

        while(rs.next()) 
            if(!routine.returnType.equals("TABLE"))
                obj.put("result", getType(routine.returnType, rs, new Integer(1)));
            else
                for(Map.Entry<String, String> entry : routine.returnColumns.entrySet()) 
                    obj.put(entry.getKey(), getType(entry.getValue(), rs, entry.getKey()));
        return obj;
    }

    public static Object getType(String type, ResultSet rs, int index)
        throws SQLException{

        switch(type){
            case "bigint":
            case "smallint":
            case "int":
            case "tinyint":
                return rs.getInt(index);
            case "numeric":
            case "decimal":
                return rs.getDouble(index);
            case "float":
            case "real":
                return rs.getFloat(index);
            case "char":
            case "varchar":
            case "text":
                return rs.getString(index);
            case "nchar":
            case "nvarchar":
            case "ntext":
                return rs.getNString(index);
            case "binary":
            case "varbinary":
            case "image":
                return rs.getBlob(index);
            case "date":
            case "datetime":
            case "datetime2":
            case "time":
            case "timestamp":
                return rs.getDate(index);
            default:
                return rs.getObject(index);
        }
    }

    public static Object getType(String type, ResultSet rs, String index)
        throws SQLException{

        switch(type){
            case "bigint":
            case "smallint":
            case "int":
            case "tinyint":
                return rs.getInt(index);
            case "numeric":
            case "decimal":
                return rs.getDouble(index);
            case "float":
            case "real":
                return rs.getFloat(index);
            case "char":
            case "varchar":
            case "text":
                return rs.getString(index);
            case "nchar":
            case "nvarchar":
            case "ntext":
                return rs.getNString(index);
            case "binary":
            case "varbinary":
            case "image":
                return rs.getBlob(index);
            case "date":
            case "datetime":
            case "datetime2":
            case "time":
            case "timestamp":
                return rs.getDate(index);
            default:
                return rs.getObject(index);
        }
    }
}
