/*
* Class that builds the String SQL queries.
*
* All of the identifiers coming from the client are properly quoted with quoteName.
*/
package mssqlrest;

import java.util.*;
import java.util.stream.*;

import static mssqlrest.Structure.*;

public class QueryBuilder{

    //Does the same as `select quotename('something')`
    //If 'something' contains ']', quotename replaces the ']' by ']]'. '[' is left as is.
    //select quotename('some[thing]else')
    //[some[thing]]else]
    public static String quoteName(String s){
      return "[" + s.replaceAll("]", "]]") + "]";
    }

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

    public static String tableMetaDataQuery(){
      return
        "SELECT CHARACTER_MAXIMUM_LENGTH AS max_length, COLUMN_DEFAULT AS [default], " +
        "CONVERT(BIT, (CASE WHEN IS_NULLABLE = 'YES' THEN 1 ELSE 0 END)) AS nullable, " +
        "COLUMN_NAME AS name, DATA_TYPE as type, " +
        "NUMERIC_PRECISION AS precision, NUMERIC_SCALE AS scale " +
        "FROM information_schema.columns WHERE table_name = ? AND table_schema = ?";
    }

    public static String allPrivilegedTablesQuery(){
      return
        "SELECT table_name AS [name], " +
        "CONVERT(BIT, MAX(CASE WHEN privilege_type = 'SELECT' THEN 1 ELSE 0 END )) AS selectable, "+
        "CONVERT(BIT, MAX(CASE WHEN privilege_type = 'INSERT' THEN 1 ELSE 0 END )) AS insertable, "+
        "CONVERT(BIT, MAX(CASE WHEN privilege_type = 'UPDATE' THEN 1 ELSE 0 END )) AS updateable, "+
        "CONVERT(BIT, MAX(CASE WHEN privilege_type = 'DELETE' THEN 1 ELSE 0 END )) AS deletable "+
        "FROM information_schema.table_privileges WHERE grantee = ? AND table_schema = ? GROUP BY table_schema,table_name";

    }

}
