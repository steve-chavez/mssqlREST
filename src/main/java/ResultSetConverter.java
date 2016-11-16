
import com.univocity.parsers.common.processor.*;
import com.univocity.parsers.conversions.*;
import com.univocity.parsers.csv.*;

import com.ebay.xcelite.sheet.*;
import com.ebay.xcelite.reader.*;
import com.ebay.xcelite.writer.*;
import com.ebay.xcelite.*;

import java.sql.SQLException;
import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;

import com.google.gson.*;

import java.util.*;
import java.io.*;

import fj.data.Either;

public class ResultSetConverter {

    public static Either<Object, Object> convert(ResultSet rs, Boolean singular, Structure.Format format, Optional<Structure.Routine> routine)
        throws SQLException {

        ResultSetMetaData rsmd = rs.getMetaData();
        int numColumns = rsmd.getColumnCount();

        if(format==Structure.Format.JSON){
            Gson gson = new GsonBuilder().serializeNulls().create();
            if(routine.isPresent() && routine.get().isScalar()){
              rs.next();
              return Either.right(getColumnValue(rs, new Integer(1), routine.get().returnType));
            }
            else if(singular){
                Map<String, Object> map = new HashMap();
                while(rs.next()) {
                    for (int i=1; i<numColumns+1; i++) {
                        String columnName = rsmd.getColumnName(i);
                        map.put(columnName, getColumnValue(rs, columnName, rsmd.getColumnType(i)));
                    }
                }
                return Either.right(gson.toJson(map));
            }else{
                List<Map<String, Object>> maps = new ArrayList();
                while(rs.next()) {
                    Map<String, Object> map = new HashMap();
                    for (int i=1; i<numColumns+1; i++) {
                        String columnName = rsmd.getColumnName(i);
                        map.put(columnName, getColumnValue(rs, columnName, rsmd.getColumnType(i)));
                    }
                    maps.add(map);
                }
                return Either.right(gson.toJson(maps));
            }
        }else if(format==Structure.Format.CSV){
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
                for (int i=1; i<numColumns+1; i++) {
                    String columnName = rsmd.getColumnName(i);
                    row.add(getColumnValue(rs, columnName, rsmd.getColumnType(i)));
                }
                writer.writeRow(row);
            }

            writer.close();
            return Either.right(csvResult.toString());
        }else if(format==Structure.Format.XLSX){
            Xcelite xcelite = new Xcelite();
            XceliteSheet sheet = xcelite.createSheet("data_sheet");
            SheetWriter<Collection<Object>> simpleWriter = sheet.getSimpleWriter();
            List<Collection<Object>> data = new ArrayList<Collection<Object>>();

            List<Object> headers = new ArrayList<Object>();
            for (int i=1; i<numColumns+1; i++){
                headers.add(rsmd.getColumnName(i));
            }
            data.add(headers);
            while(rs.next()){
                List<Object> row = new ArrayList<Object>();
                for (int i=1; i<numColumns+1; i++) {
                    String columnName = rsmd.getColumnName(i);
                    row.add(getColumnValue(rs, columnName, rsmd.getColumnType(i)));
                }
                data.add(row);
            }
            simpleWriter.write(data);
            return Either.right(xcelite.getBytes());
        }else{ //Structure.Format.BINARY
            if(numColumns == 1){
                StringJoiner joiner = new StringJoiner("\n");
                while(rs.next()) {
                  joiner.add(getColumnValue(rs, rsmd.getColumnName(1), rsmd.getColumnType(1)).toString());
                }
                return Either.right(joiner);
            } else {
                Gson gson = new GsonBuilder().serializeNulls().create();
                Map<String, String> map = new HashMap<String, String>();
                map.put("message", "To use application/octet-stream the query must contain only one column");
                return Either.left(gson.toJson(map));
            }
        }
    }

    public static Either<Object, Object> convert(CallableStatement cs, Structure.Routine routine)
        throws SQLException {
      Gson gson = new GsonBuilder().serializeNulls().create();
      Map<String, Object> map = new HashMap<String, Object>();
      for(Map.Entry<String, Structure.Parameter> entry : routine.parameters.entrySet())
          if(entry.getValue().parameterMode.equals("INOUT"))
              map.put(entry.getKey(), getColumnValue(cs, entry.getKey(), entry.getValue().dataType));
      return Either.right(gson.toJson(map));
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
                o = rs.getString(columnName);
                break;
            case java.sql.Types.TIMESTAMP:
                o = rs.getString(columnName);
                break;
            default:
                o = rs.getObject(columnName);
                break;
        }
        if(rs.wasNull())
            return null;
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
                o = rs.getString(columnName);
                break;
            default:
                o = rs.getObject(columnName);
                break;
        }
        if(rs.wasNull())
            return null;
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
                o = rs.getString(index);
                break;
            default:
                o = rs.getObject(index);
                break;
        }
        if(rs.wasNull())
            return null;
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
                o = cs.getString(index);
                break;
            default:
                o = cs.getObject(index);
                break;
        }
        if(cs.wasNull())
            return null;
        else
            return o;
    }
}
