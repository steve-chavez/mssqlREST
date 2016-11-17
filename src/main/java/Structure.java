
import java.util.*;

public class Structure{

    public enum Format{
        JSON, CSV, XLSX, BINARY;
    }

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
}
