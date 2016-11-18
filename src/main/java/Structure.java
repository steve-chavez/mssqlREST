
import java.sql.*;
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

    public static Optional<Table> getTableStructure(String schema, String tableName, Connection conn) throws SQLException{
        String query = "SELECT column_name, data_type FROM information_schema.columns WHERE table_name = ? AND table_schema = ?";
        System.out.println(query);
        PreparedStatement statement = conn.prepareStatement(query);
        statement.setString(1, tableName);
        statement.setString(2, schema);
        ResultSet rs = statement.executeQuery();
        if(rs.isBeforeFirst()){
            Table table = new Table();
            table.name = tableName;
            table.schema = schema;
            while(rs.next()){
                table.columns.put(rs.getString("column_name"), rs.getString("data_type"));
            }
            return Optional.of(table);
        }
        else
            return Optional.empty();
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

    public static Optional<Routine> getRoutineStructure(String schema, String routineName, Connection conn) throws SQLException{
        Routine routine = new Routine();
        String query1 = "SELECT routine_name, routine_schema, routine_type, data_type AS return_type FROM information_schema.routines WHERE routine_name = ? AND routine_schema = ?";
        System.out.println(query1);
        PreparedStatement statement1 = conn.prepareStatement(query1);
        statement1.setString(1, routineName);
        statement1.setString(2, schema);
        ResultSet rs1 = statement1.executeQuery();
        if(rs1.isBeforeFirst()){
            while(rs1.next()){
                routine.schema = rs1.getString("routine_schema");
                routine.name = rs1.getString("routine_name");
                routine.type = rs1.getString("routine_type");
                routine.returnType = rs1.getString("return_type");
            }
            String query2 = "SELECT parameter_name, ordinal_position, data_type, parameter_mode FROM information_schema.parameters WHERE specific_name = ? AND specific_schema = ?";
            System.out.println(query2);
            PreparedStatement statement2 = conn.prepareStatement(query2);
            statement2.setString(1, routineName);
            statement2.setString(2, schema);
            ResultSet rs2 = statement2.executeQuery();
            while(rs2.next()){
                //SQL Server gets a parameter with ordinal_position of 0 to indicate return type
                //this is redundant since it was previously obtained
                if(rs2.getInt("ordinal_position") > 0){
                    Structure.Parameter parameter = new Structure.Parameter();
                    parameter.name = rs2.getString("parameter_name").substring(1);
                    parameter.dataType = rs2.getString("data_type");
                    parameter.ordinalPosition = rs2.getInt("ordinal_position");
                    parameter.parameterMode = rs2.getString("parameter_mode");
                    routine.parameters.put(parameter.name, parameter);
                }
            }
            //Get the RETURNS TABLE structure
            String query3 = "SELECT name, type_name(user_type_id) AS data_type FROM sys.all_columns WHERE object_id = object_id(?)";
            System.out.println(query3);
            if(routine.returnType != null && routine.returnType.equals("TABLE")){
                PreparedStatement statement3 = conn.prepareStatement(query3);
                statement3.setString(1, QueryBuilder.quoteName(routine.schema) + "." + QueryBuilder.quoteName(routine.name));
                ResultSet rs3 = statement3.executeQuery();
                while(rs3.next())
                    routine.returnColumns.put(rs3.getString("name"), rs3.getString("data_type"));
            }
            return Optional.of(routine);
        }else
            return Optional.empty();
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
