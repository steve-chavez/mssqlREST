
import java.util.*;
import java.util.stream.*;

public class QueryBuilder{

    public static String selectQuery(
            Structure.Table table,
            Set<String> params,
            Optional<String> selectColumns,
            Optional<String> order
        ){
        StringBuilder builder = new StringBuilder("SELECT ");

        if(selectColumns.isPresent()){
            builder.append(selectColumns.get());
        }
        else
            builder.append("*");

        builder.append(" FROM " + quoteName(table.schema) + "." + quoteName(table.name) + " ");

        if(!params.isEmpty()){
            builder.append(" WHERE ");
            builder.append(params.stream().map( p -> quoteName(p) + " = ?").collect(Collectors.joining(" AND ")));
        }

        if(order.isPresent())
            builder.append(" ORDER BY " + order.get());

        return builder.toString();
    }

    public static String insertQuery(Structure.Table table, Set<String> columns){
        StringBuilder builder = new StringBuilder("INSERT INTO ");
        builder.append(quoteName(table.schema) + "." + quoteName(table.name) + " ");
        if(!columns.isEmpty()){
          builder.append("(");
          builder.append(columns.stream().map( s -> quoteName(s) ).collect(Collectors.joining(",")));
          builder.append(") VALUES (");
          builder.append(columns.stream().map( s -> "?" ).collect(Collectors.joining(",")));
          builder.append(")");
        }
        else
          builder.append(" DEFAULT VALUES");

        return builder.toString();
    }

    public static String updateQuery(
            Structure.Table table,
            Set<String> vals,
            Set<String> params
    ){
        StringBuilder builder = new StringBuilder("UPDATE ");
        builder.append(quoteName(table.schema) + "." + quoteName(table.name) + " ");
        builder.append("SET ");
        builder.append(vals.stream().map(v -> quoteName(v) + " = ?").collect(Collectors.joining(", ")));

        if(!params.isEmpty()){
            builder.append(" WHERE ");
            builder.append(params.stream().map(p -> quoteName(p) + " = ?").collect(Collectors.joining(" AND ")));
        }

        return builder.toString();
    }

    public static String deleteQuery(
            Structure.Table table,
            Set<String> params
    ){
        StringBuilder builder = new StringBuilder("DELETE FROM ");
        builder.append(quoteName(table.schema) + "." + quoteName(table.name) + " ");

        if(!params.isEmpty()){
            builder.append(" WHERE ");
            builder.append(params.stream().map( p -> quoteName(p) + " = ?").collect(Collectors.joining(" AND ")));
        }

        return builder.toString();
    }

    public static String functionQuery(Structure.Routine routine){
        StringBuilder builder;
        if(routine.type.equals("FUNCTION")){
            if(!routine.returnType.equals("TABLE"))
                builder = new StringBuilder("SELECT ");
            else
                builder = new StringBuilder("SELECT * FROM ");
            builder.append(quoteName(routine.schema) + "." + quoteName(routine.name) + " ");
            builder.append("(");
            List<String> questionParams =
                Collections.nCopies(routine.parameters.size(), "?");
            builder.append(questionParams.stream().collect(Collectors.joining(", ")));
            builder.append(")");
        }else{
            builder = new StringBuilder("{call ");
            builder.append(quoteName(routine.schema) + "." + quoteName(routine.name) + " ");
            builder.append("(");
            List<String> questionParams =
                Collections.nCopies(routine.parameters.size(), "?");
            builder.append(questionParams.stream().collect(Collectors.joining(", ")));
            builder.append(")}");
        }
        return builder.toString();
    }

    //Does the same as `select quotename('something')`
    //If 'something' contains ']', quotename replaces the ']' by ']]'. '[' is left as is.
    //select quotename('some[thing]else')
    //[some[thing]]else]
    public static String quoteName(String s){
      return "[" + s.replaceAll("]", "]]") + "]";
    }

}
