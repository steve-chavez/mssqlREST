/*
* Class that converts the ResultSet coming from SQLServer to different formats.
*/
package mssqlrest;

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

import static mssqlrest.Structure.*;

public class ResultSetConverter {

  static final Gson gson = new GsonBuilder().disableHtmlEscaping().serializeNulls().create();

  public static Either<Object, Object> convert(ResultSet rs, Boolean singular, Format format, Optional<Routine> routine)
      throws SQLException {

      ResultSetMetaData rsmd = rs.getMetaData();
      int numColumns = rsmd.getColumnCount();

      switch(format){
        case JSON: {
          if(routine.isPresent() && routine.get().isScalar()){
            rs.next();
            return Either.right(getScalarValue(rs, toSqlType(routine.get().returnType)));
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

  public static Either<Object, Object> convert(CallableStatement cs, Routine routine, Format format)
      throws SQLException {
    if(format==Format.JSON){
      Map<String, Object> map = new HashMap<String, Object>();
      for(Map.Entry<String, Parameter> entry : routine.parameters.entrySet())
          if(entry.getValue().isOut())
              map.put(entry.getKey(), getParameterValue(cs, entry.getKey(), toSqlType(entry.getValue().dataType)));
      return Either.right(gson.toJson(map));
    }else
      //TODO: Return 406 status code
      return Either.right(Errors.messageToJson("Only JSON output is implemented for a PROCEDURE call"));
  }

  private static Object getColumnValue(ResultSet rs, String columnName, int type)
      throws SQLException{
      Object o;
      switch(type){
          case java.sql.Types.ARRAY: o = rs.getArray(columnName); break;
          case java.sql.Types.BIGINT: o = rs.getInt(columnName); break;
          case java.sql.Types.BOOLEAN: o = rs.getBoolean(columnName); break;
          case java.sql.Types.BLOB: o = rs.getBlob(columnName); break;
          case java.sql.Types.DOUBLE: o = rs.getDouble(columnName); break;
          case java.sql.Types.FLOAT: o = rs.getFloat(columnName); break;
          case java.sql.Types.INTEGER: o = rs.getInt(columnName); break;
          case java.sql.Types.NVARCHAR: o = rs.getNString(columnName); break;
          case java.sql.Types.VARCHAR: o = rs.getString(columnName); break;
          case java.sql.Types.TINYINT: o = rs.getInt(columnName); break;
          case java.sql.Types.SMALLINT: o = rs.getInt(columnName); break;
          case java.sql.Types.DATE: o = rs.getString(columnName); break;
          case java.sql.Types.TIMESTAMP: o = rs.getString(columnName); break;
          default: o = rs.getObject(columnName); break;
      }
      //Must be called in this way, after rs.get<Type>(..). According to https://docs.oracle.com/javase/7/docs/api/java/sql/ResultSet.html:
      //Note that you must first call one of the getter methods on a column to try to read its value and then call the method wasNull to see if the value read was
      //SQL NULL.
      if(rs.wasNull())
          return null;
      else
          return o;
  }

  //TODO: DRY and make this method unnecessary. Ideally cs.getResultSet() would work but it gives null.
  private static Object getParameterValue(CallableStatement cs, String name, int type)
      throws SQLException{
      Object o;
      switch(type){
          case java.sql.Types.ARRAY: o = cs.getArray(name); break;
          case java.sql.Types.BIGINT: o = cs.getInt(name); break;
          case java.sql.Types.BOOLEAN: o = cs.getBoolean(name); break;
          case java.sql.Types.BLOB: o = cs.getBlob(name); break;
          case java.sql.Types.DOUBLE: o = cs.getDouble(name); break;
          case java.sql.Types.FLOAT: o = cs.getFloat(name); break;
          case java.sql.Types.INTEGER: o = cs.getInt(name); break;
          case java.sql.Types.NVARCHAR: o = cs.getNString(name); break;
          case java.sql.Types.VARCHAR: o = cs.getString(name); break;
          case java.sql.Types.TINYINT: o = cs.getInt(name); break;
          case java.sql.Types.SMALLINT: o = cs.getInt(name); break;
          case java.sql.Types.DATE: o = cs.getString(name); break;
          case java.sql.Types.TIMESTAMP: o = cs.getString(name); break;
          default: o = cs.getObject(name); break;
      }
      if(cs.wasNull())
          return null;
      else
          return o;
  }

  private static Object getScalarValue(ResultSet rs, int type) throws SQLException{
      Object o;
      switch(type){
          case java.sql.Types.ARRAY: o = rs.getArray(1); break;
          case java.sql.Types.BIGINT: o = rs.getInt(1); break;
          case java.sql.Types.BOOLEAN: o = rs.getBoolean(1); break;
          case java.sql.Types.BLOB: o = rs.getBlob(1); break;
          case java.sql.Types.DOUBLE: o = rs.getDouble(1); break;
          case java.sql.Types.FLOAT: o = rs.getFloat(1); break;
          case java.sql.Types.INTEGER: o = rs.getInt(1); break;
          case java.sql.Types.NVARCHAR: o = rs.getNString(1); break;
          case java.sql.Types.VARCHAR: o = rs.getString(1); break;
          case java.sql.Types.TINYINT: o = rs.getInt(1); break;
          case java.sql.Types.SMALLINT: o = rs.getInt(1); break;
          case java.sql.Types.DATE: o = rs.getString(1); break;
          case java.sql.Types.TIMESTAMP: o = rs.getString(1); break;
          default: o = rs.getObject(1); break;
      }
      if(rs.wasNull())
          return null;
      else
          return o;
  }
}
