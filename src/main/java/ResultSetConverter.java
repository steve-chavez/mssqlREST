
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

  static final Gson gson = new GsonBuilder().disableHtmlEscaping().serializeNulls().create();

  public static Either<Object, Object> convert(ResultSet rs, Boolean singular, Structure.Format format, Optional<Structure.Routine> routine)
      throws SQLException {

      ResultSetMetaData rsmd = rs.getMetaData();
      int numColumns = rsmd.getColumnCount();

      switch(format){
        case JSON: {
          if(routine.isPresent() && routine.get().isScalar()){
            rs.next();
            return Either.right(getScalarValue(rs, Structure.toSqlType(routine.get().returnType)));
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
        }
        case CSV: {
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
        }
        case XLSX:{
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
        }
        case BINARY:{
          if(numColumns == 1){
              StringJoiner joiner = new StringJoiner("\n");
              while(rs.next()) {
                joiner.add(getColumnValue(rs, rsmd.getColumnName(1), rsmd.getColumnType(1)).toString());
              }
              return Either.right(joiner);
          } else {
              return Either.left(Errors.messageToJson("To use application/octet-stream the query must contain only one column"));
          }
        }
        default: //OTHER: Should be unreachable because of rejection at the ApplicationServer level.
          return Either.left(Errors.messageToJson("Not implemented"));
      }
  }

  public static Either<Object, Object> convert(CallableStatement cs, Structure.Routine routine, Structure.Format format)
      throws SQLException {
    if(format==Structure.Format.JSON){
      Map<String, Object> map = new HashMap<String, Object>();
      for(Map.Entry<String, Structure.Parameter> entry : routine.parameters.entrySet())
          if(entry.getValue().isOut())
              map.put(entry.getKey(), getParameterValue(cs, entry.getKey(), Structure.toSqlType(entry.getValue().dataType)));
      return Either.right(gson.toJson(map));
    }else
      //TODO: Return 406 status code
      return Either.right(Errors.messageToJson("Only JSON output is implemented for a PROCEDURE call"));
  }

  private static Object getColumnValue(ResultSet rs, String columnName, int type) throws SQLException{
      if(rs.wasNull())
        return null;
      else switch(type){
        case java.sql.Types.BIGINT: return rs.getInt(columnName);
        case java.sql.Types.INTEGER: return rs.getInt(columnName);
        case java.sql.Types.TINYINT: return rs.getInt(columnName);
        case java.sql.Types.SMALLINT: return rs.getInt(columnName);
        case java.sql.Types.BOOLEAN: return rs.getBoolean(columnName);
        case java.sql.Types.BLOB: return rs.getBlob(columnName);
        case java.sql.Types.DOUBLE: return rs.getDouble(columnName);
        case java.sql.Types.FLOAT: return rs.getFloat(columnName);
        case java.sql.Types.NVARCHAR: return rs.getNString(columnName);
        case java.sql.Types.VARCHAR: return rs.getString(columnName);
        case java.sql.Types.DATE: return rs.getString(columnName);
        case java.sql.Types.TIMESTAMP: return rs.getString(columnName);
        case java.sql.Types.ARRAY: return rs.getArray(columnName);
        default: return rs.getObject(columnName);
      }
  }

  //TODO: DRY and make this method unnecessary. Ideally cs.getResultSet() would work but it gives null.
  private static Object getParameterValue(CallableStatement cs, String name, int type)
      throws SQLException{
      if(cs.wasNull())
        return null;
      else switch(type){
        case java.sql.Types.BIGINT: return cs.getInt(name);
        case java.sql.Types.INTEGER: return cs.getInt(name);
        case java.sql.Types.TINYINT: return cs.getInt(name);
        case java.sql.Types.SMALLINT: return cs.getInt(name);
        case java.sql.Types.BOOLEAN: return cs.getBoolean(name);
        case java.sql.Types.BLOB: return cs.getBlob(name);
        case java.sql.Types.DOUBLE: return cs.getDouble(name);
        case java.sql.Types.FLOAT: return cs.getFloat(name);
        case java.sql.Types.NVARCHAR: return cs.getNString(name);
        case java.sql.Types.VARCHAR: return cs.getString(name);
        case java.sql.Types.DATE: return cs.getString(name);
        case java.sql.Types.TIMESTAMP: return cs.getString(name);
        case java.sql.Types.ARRAY: return cs.getArray(name);
        default: return cs.getObject(name);
      }
  }

  private static Object getScalarValue(ResultSet rs, int type) throws SQLException{
      if(rs.wasNull())
        return null;
      else switch(type){
        case java.sql.Types.BIGINT: return rs.getInt(1);
        case java.sql.Types.INTEGER: return rs.getInt(1);
        case java.sql.Types.TINYINT: return rs.getInt(1);
        case java.sql.Types.SMALLINT: return rs.getInt(1);
        case java.sql.Types.BOOLEAN: return rs.getBoolean(1);
        case java.sql.Types.BLOB: return rs.getBlob(1);
        case java.sql.Types.DOUBLE: return rs.getDouble(1);
        case java.sql.Types.FLOAT: return rs.getFloat(1);
        case java.sql.Types.NVARCHAR: return rs.getNString(1);
        case java.sql.Types.VARCHAR: return rs.getString(1);
        case java.sql.Types.DATE: return rs.getString(1);
        case java.sql.Types.TIMESTAMP: return rs.getString(1);
        case java.sql.Types.ARRAY: return rs.getArray(1);
        default: return rs.getObject(1);
      }
  }
}
