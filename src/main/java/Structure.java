
import java.sql.*;
import java.util.*;

public class Structure{

    public static class Table{
        public String schema;
        public String name;
        public Map<String, String> columns = new HashMap<String, String>();

        @Override
        public String toString(){
            return schema + "." + name + columns.toString();
        }

    }

    public static class Routine{
        public String schema;
        public String name;
        public String type;
        public String returnType;
        public Map<String, Parameter> parameters = new HashMap<String, Parameter>();
        public Map<String, String> returnColumns = new HashMap<String, String>();

        @Override
        public String toString(){
            return schema + "." + name + " : " +
                String.join(",", Arrays.asList(type, returnType)) +
                "; parameters : " + parameters.toString() +
                "; returnColumns : " + returnColumns.toString();
        }

        public boolean isScalar(){
            return !returnType.equals("TABLE");
        }

        public boolean isFunction(){
            return type.equals("FUNCTION");
        }
    }

    public static class Parameter{
        public String name;
        public Integer ordinalPosition;
        public String dataType;
        public String parameterMode;

        @Override
        public String toString(){
            return name + " : " +
                String.join(",", Arrays.asList(dataType,
                    ordinalPosition.toString(), parameterMode));
        }

        public boolean isOut(){
            return parameterMode.equals("INOUT") || parameterMode.equals("OUT");
        }
    }

    public static int toSqlType(String type){
      switch(type){
        case "bigint":    return java.sql.Types.BIGINT;
        case "smallint":  return java.sql.Types.SMALLINT;
        case "int":       return java.sql.Types.INTEGER;
        case "tinyint":   return java.sql.Types.TINYINT;

        case "numeric":
        case "decimal":   return java.sql.Types.DOUBLE;

        case "float":
        case "real":      return java.sql.Types.FLOAT;

        case "char":
        case "varchar":
        case "text":      return java.sql.Types.VARCHAR;

        case "nchar":
        case "nvarchar":
        case "ntext":     return java.sql.Types.NVARCHAR;

        case "binary":
        case "varbinary":
        case "image":     return java.sql.Types.BLOB;

        case "date":
        case "datetime":
        case "datetime2":
        case "time":
        case "timestamp": return java.sql.Types.VARCHAR;

        default:          return java.sql.Types.OTHER;
      }
    }

    public enum Format{
        JSON, CSV, XLSX, BINARY;
    }

    public static Format toFormat(String val){
      switch(val){
        case "application/json":
          return Format.JSON;
        case "text/csv":
          return Format.CSV;
        case "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet":
          return Format.XLSX;
        case "application/octet-stream":
          return Format.BINARY;
        default: //assume JSON for all other types. TODO correct this
          return Format.JSON;
      }
    }

    public static String toMediaType(Format format){
      switch(format){
        case JSON:
          return "application/json";
        case CSV:
          return "text/csv";
        case XLSX:
          return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        case BINARY:
          return "application/octet-stream";
        default:
          return "application/json";
      }
    }

    public enum OrderDirection{
      ASC, DESC;
    }

    public static class Order{

      public String identifier;
      public Optional<OrderDirection> dir;

      public Order(String identifier, Optional<OrderDirection> dir){
        this.identifier = identifier;
        this.dir = dir;
      }
    }

    public enum Operator{
      EQ("="), NEQ("<>"), GT(">"), GTE(">="), LT("<"), LTE("<="), LIKE("LIKE");

      public final String literal;

      private Operator(String literal){
        this.literal = literal;
      }
    }

    public static class OperatorVal{
      public Operator op;
      public String val;

      public OperatorVal(Operator op, String val){
        this.op = op;
        this.val = val;
      }
    }
}
