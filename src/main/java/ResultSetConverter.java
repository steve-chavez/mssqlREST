
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

    public static JSONObject routineResultToJson( Structure.Routine routine, ResultSet rs)
        throws SQLException, JSONException {

        JSONObject obj = new JSONObject();
        while(rs.next()) 
            if(!routine.returnType.equals("TABLE"))
                obj.put("result", getColumnValue(rs, new Integer(1), routine.returnType));
            else
                for(Map.Entry<String, String> entry : routine.returnColumns.entrySet()) 
                    obj.put(entry.getKey(), getColumnValue(rs, entry.getKey(), entry.getValue()));
        return obj;
    }

    public static JSONObject routineResultToJson( Structure.Routine routine, CallableStatement cs)
        throws SQLException, JSONException {

        JSONObject obj = new JSONObject();
        for(Map.Entry<String, Structure.Parameter> entry : routine.parameters.entrySet())
            if(entry.getValue().parameterMode.equals("INOUT") || entry.getValue().parameterMode.equals("OUT"))
                obj.put(entry.getKey(), getColumnValue(cs, entry.getKey(), entry.getValue().dataType));
        return obj;
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


    private static Object getColumnValue(ResultSet rs, String index, String type) throws SQLException{
        Object o;
        switch(type){
            case "bigint":
            case "smallint":
            case "int":
            case "tinyint":
                o = rs.getInt(index);
            case "numeric":
            case "decimal":
                o = rs.getDouble(index);
            case "float":
            case "real":
                o = rs.getFloat(index);
            case "char":
            case "varchar":
            case "text":
                o = rs.getString(index);
            case "nchar":
            case "nvarchar":
            case "ntext":
                o = rs.getNString(index);
            case "binary":
            case "varbinary":
            case "image":
                o = rs.getBlob(index);
            case "date":
            case "datetime":
            case "datetime2":
            case "time":
            case "timestamp":
                o = rs.getDate(index);
            default:
                o = rs.getObject(index);
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
            case "numeric":
            case "decimal":
                o = rs.getDouble(index);
            case "float":
            case "real":
                o = rs.getFloat(index);
            case "char":
            case "varchar":
            case "text":
                o = rs.getString(index);
            case "nchar":
            case "nvarchar":
            case "ntext":
                o = rs.getNString(index);
            case "binary":
            case "varbinary":
            case "image":
                o = rs.getBlob(index);
            case "date":
            case "datetime":
            case "datetime2":
            case "time":
            case "timestamp":
                o = rs.getDate(index);
            default:
                o = rs.getObject(index);
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
            case "numeric":
            case "decimal":
                o = cs.getDouble(index);
            case "float":
            case "real":
                o = cs.getFloat(index);
            case "char":
            case "varchar":
            case "text":
                o = cs.getString(index);
            case "nchar":
            case "nvarchar":
            case "ntext":
                o = cs.getNString(index);
            case "binary":
            case "varbinary":
            case "image":
                o = cs.getBlob(index);
            case "date":
            case "datetime":
            case "datetime2":
            case "time":
            case "timestamp":
                o = cs.getDate(index);
            default:
                o = cs.getObject(index);
        }
        if(cs.wasNull())
            return JSONObject.NULL;
        else
            return o;
    }
}
