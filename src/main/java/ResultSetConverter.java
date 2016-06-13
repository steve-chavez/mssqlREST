
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

import com.univocity.parsers.common.processor.*;
import com.univocity.parsers.conversions.*;
import com.univocity.parsers.csv.*;

import java.sql.SQLException;
import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;

import java.util.*;
import java.io.*;

public class ResultSetConverter {

    public enum Format{
        JSON, CSV;
    }

    public static Object convert( ResultSet rs, Boolean singular, Format format)
        throws SQLException, JSONException {

        ResultSetMetaData rsmd = rs.getMetaData();
        int numColumns = rsmd.getColumnCount();

        if(format==Format.JSON){
            if(singular){
                JSONObject obj = new JSONObject();

                while(rs.next()) {
                    for (int i=1; i<numColumns+1; i++) {
                        String columnName = rsmd.getColumnName(i);
                        obj.put(columnName, getColumnValue(rs, columnName, rsmd.getColumnType(i)));
                    }
                }
                return obj;
            }else{
                JSONArray jsonArr = new JSONArray();
                while(rs.next()) {
                    JSONObject obj = new JSONObject();
                    for (int i=1; i<numColumns+1; i++) {
                        String columnName = rsmd.getColumnName(i);
                        obj.put(columnName, getColumnValue(rs, columnName, rsmd.getColumnType(i)));
                    }
                    jsonArr.put(obj);
                }
                return jsonArr;
            }
        }else{

            ByteArrayOutputStream csvResult = new ByteArrayOutputStream();
            Writer outputWriter = new OutputStreamWriter(csvResult);
            CsvWriter writer = new CsvWriter(outputWriter, new CsvWriterSettings());

            List<String> headers = new ArrayList<String>();

            for (int i=1; i<numColumns+1; i++){
                headers.add(rsmd.getColumnName(i));
            }

            writer.writeHeaders(headers);

            while(rs.next()){
                List<Object> row = new ArrayList<Object>();
                JSONObject obj = new JSONObject();
                for (int i=1; i<numColumns+1; i++) {
                    String columnName = rsmd.getColumnName(i);
                    row.add(getColumnValue(rs, columnName, rsmd.getColumnType(i)));
                }
                writer.writeRow(row);
            }

            writer.close();
            return csvResult.toString();
        }
    }

    public static Map<String, Object> routineResultToMap( Structure.Routine routine, ResultSet rs)
        throws SQLException, JSONException {

        Map<String, Object> map = new HashMap<String, Object>();
        while(rs.next()) 
            if(!routine.returnType.equals("TABLE"))
                map.put("result", getColumnValue(rs, new Integer(1), routine.returnType));
            else
                for(Map.Entry<String, String> entry : routine.returnColumns.entrySet()) {
                    map.put(entry.getKey(), getColumnValue(rs, entry.getKey(), entry.getValue()));
                }
        return map;
    }

    public static Map<String, Object> routineResultToMap( Structure.Routine routine, CallableStatement cs)
        throws SQLException, JSONException {

        Map<String, Object> map = new HashMap<String, Object>();
        for(Map.Entry<String, Structure.Parameter> entry : routine.parameters.entrySet())
            if(entry.getValue().parameterMode.equals("INOUT") || entry.getValue().parameterMode.equals("OUT"))
                map.put(entry.getKey(), getColumnValue(cs, entry.getKey(), entry.getValue().dataType));
        return map;
    }

    private static Object getColumnValue(ResultSet rs, String columnName, int type) throws SQLException{
        Object o;
        switch(type){
            case java.sql.Types.ARRAY:
                o = rs.getArray(columnName);
                break;
            case java.sql.Types.BIGINT:
                o = rs.getInt(columnName);
                break;
            case java.sql.Types.BOOLEAN:
                o = rs.getBoolean(columnName);
                break;
            case java.sql.Types.BLOB:
                o = rs.getBlob(columnName);
                break;
            case java.sql.Types.DOUBLE:
                o = rs.getDouble(columnName);
                break;
            case java.sql.Types.FLOAT:
                o = rs.getFloat(columnName);
                break;
            case java.sql.Types.INTEGER:
                o = rs.getInt(columnName);
                break;
            case java.sql.Types.NVARCHAR:
                o = rs.getNString(columnName);
                break;
            case java.sql.Types.VARCHAR:
                o = rs.getString(columnName);
                break;
            case java.sql.Types.TINYINT:
                o = rs.getInt(columnName);
                break;
            case java.sql.Types.SMALLINT:
                o = rs.getInt(columnName);
                break;
            case java.sql.Types.DATE:
                o = rs.getDate(columnName);
                break;
            case java.sql.Types.TIMESTAMP:
                o = rs.getDate(columnName);
                break;
            default:
                o = rs.getObject(columnName);
                break;
        }
        if(rs.wasNull())
            return JSONObject.NULL;
        else
            return o;
    }


    private static Object getColumnValue(ResultSet rs, String columnName, String type) throws SQLException{
        Object o;
        switch(type){
            case "bigint":
            case "smallint":
            case "int":
            case "tinyint":
                o = rs.getInt(columnName);
                break;
            case "numeric":
            case "decimal":
                o = rs.getDouble(columnName);
                break;
            case "float":
            case "real":
                o = rs.getFloat(columnName);
                break;
            case "char":
            case "varchar":
            case "text":
                o = rs.getString(columnName);
                break;
            case "nchar":
            case "nvarchar":
            case "ntext":
                o = rs.getNString(columnName);
                break;
            case "binary":
            case "varbinary":
            case "image":
                o = rs.getBlob(columnName);
                break;
            case "date":
            case "datetime":
            case "datetime2":
            case "time":
            case "timestamp":
                o = rs.getDate(columnName);
                break;
            default:
                o = rs.getObject(columnName);
                break;
        }
        if(rs.wasNull())
            return JSONObject.NULL;
        else
            return o;
    }

    private static Object getColumnValue(ResultSet rs, int index, String type) throws SQLException{
        Object o;
        switch(type){
            case "bigint":
            case "smallint":
            case "int":
            case "tinyint":
                o = rs.getInt(index);
                break;
            case "numeric":
            case "decimal":
                o = rs.getDouble(index);
                break;
            case "float":
            case "real":
                o = rs.getFloat(index);
                break;
            case "char":
            case "varchar":
            case "text":
                o = rs.getString(index);
                break;
            case "nchar":
            case "nvarchar":
            case "ntext":
                o = rs.getNString(index);
                break;
            case "binary":
            case "varbinary":
            case "image":
                o = rs.getBlob(index);
                break;
            case "date":
            case "datetime":
            case "datetime2":
            case "time":
            case "timestamp":
                o = rs.getDate(index);
                break;
            default:
                o = rs.getObject(index);
                break;
        }
        if(rs.wasNull())
            return JSONObject.NULL;
        else
            return o;
    }

    private static Object getColumnValue(CallableStatement cs, String index, String type)
        throws SQLException{
        Object o;
        switch(type){
            case "bigint":
            case "smallint":
            case "int":
            case "tinyint":
                o = cs.getInt(index);
                break;
            case "numeric":
            case "decimal":
                o = cs.getDouble(index);
                break;
            case "float":
            case "real":
                o = cs.getFloat(index);
                break;
            case "char":
            case "varchar":
            case "text":
                o = cs.getString(index);
                break;
            case "nchar":
            case "nvarchar":
            case "ntext":
                o = cs.getNString(index);
                break;
            case "binary":
            case "varbinary":
            case "image":
                o = cs.getBlob(index);
                break;
            case "date":
            case "datetime":
            case "datetime2":
            case "time":
            case "timestamp":
                o = cs.getDate(index);
                break;
            default:
                o = cs.getObject(index);
                break;
        }
        if(cs.wasNull())
            return JSONObject.NULL;
        else
            return o;
    }
}
