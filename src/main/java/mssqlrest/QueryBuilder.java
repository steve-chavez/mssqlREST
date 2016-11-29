package mssqlrest;

import java.util.*;
import java.util.stream.*;

import static mssqlrest.Structure.*;

public class QueryBuilder{

    public static String selectQuery(
            Table table,
            Map<String, OperatorVal> filters,
            List<String> select,
            List<Order> order
        ){
        StringBuilder builder = new StringBuilder("SELECT ");

        if(select.isEmpty()){
            builder.append("*");
        }
        else
            builder.append(select.stream().map(s -> quoteName(s)).collect(Collectors.joining(", ")));

        builder.append(" FROM " + quoteName(table.schema) + "." + quoteName(table.name) + " ");

        builder.append(whereFragment(filters));

        if(!order.isEmpty()){
            builder.append(" ORDER BY ");
            builder.append(order.stream().map(o -> quoteName(o.identifier) + o.dir.map(d -> " " + d).orElse("")).collect(Collectors.joining(", ")));
        }

        return builder.toString();
    }

    public static String insertQuery(Table table, Set<String> columns){
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
            Table table,
            Set<String> vals,
            Map<String, OperatorVal> filters
    ){
        StringBuilder builder = new StringBuilder("UPDATE ");
        builder.append(quoteName(table.schema) + "." + quoteName(table.name) + " ");
        builder.append("SET ");
        builder.append(vals.stream().map(v -> quoteName(v) + " = ?").collect(Collectors.joining(", ")));
        builder.append(whereFragment(filters));

        return builder.toString();
    }

    public static String deleteQuery(
            Table table,
            Map<String, OperatorVal> filters
    ){
        StringBuilder builder = new StringBuilder("DELETE FROM ");
        builder.append(quoteName(table.schema) + "." + quoteName(table.name) + " ");
        builder.append(whereFragment(filters));

        return builder.toString();
    }

    public static String functionQuery(Routine routine){
        StringBuilder builder;
        if(routine.isFunction()){
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

    private static String whereFragment(Map<String, OperatorVal> filters){
      if(!filters.isEmpty())
        return " WHERE " + filters.entrySet().stream().map(e ->
            quoteName(e.getKey()) + " " + e.getValue().op.literal + " ?").collect(Collectors.joining(" AND "));
      else
        return "";
    }

    //Does the same as `select quotename('something')`
    //If 'something' contains ']', quotename replaces the ']' by ']]'. '[' is left as is.
    //select quotename('some[thing]else')
    //[some[thing]]else]
    public static String quoteName(String s){
      return "[" + s.replaceAll("]", "]]") + "]";
    }

}
