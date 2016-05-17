
import java.util.*;
import java.util.stream.*;

public class QueryBuilder{

    public static String selectQuery(
            Structure.Table table,
            String[] params
        ){
        StringBuilder builder = new StringBuilder("SELECT * FROM ");
        builder.append(table.name);

        List<String> questionParams = new ArrayList<String>();


        for (String param : params) {
            questionParams.add(param + " = ?");
        }

        if(questionParams.size()>0){
            builder.append(" WHERE ");
            builder.append(questionParams.stream().collect(Collectors.joining(" AND ")));
        }

        return builder.toString();
    }

    public static String insertQuery(Structure.Table table, List<String> columns){
        StringBuilder builder = new StringBuilder("INSERT INTO ");
        builder.append(table.name);
        builder.append("(");
        builder.append(columns.stream().collect(Collectors.joining(",")));
        builder.append(") VALUES(");
        builder.append(columns.stream().map( s -> "?" ).collect(Collectors.joining(",")));
        builder.append(")");

        return builder.toString();
    }

    public static String updateQuery(
            Structure.Table table, 
            String[] vals,
            String[] params
    ){
        StringBuilder builder = new StringBuilder("UPDATE ");
        builder.append(table.name);
        builder.append(" SET ");

        List<String> questionValues = new ArrayList<String>();

        for (String val : vals) {
            questionValues.add(val + " = ?");
        }

        builder.append(questionValues.stream().collect(Collectors.joining(", ")));


        List<String> questionParams = new ArrayList<String>();

        for (String param : params) {
            questionParams.add(param + " = ?");
        }

        if(questionParams.size() > 0){
            builder.append(" WHERE ");
            builder.append(questionParams.stream().collect(Collectors.joining(" AND ")));
        }

        return builder.toString();
    }

    public static String deleteQuery(
            Structure.Table table, 
            String[] params
    ){
        StringBuilder builder = new StringBuilder("DELETE FROM ");
        builder.append(table.name);

        List<String> questionParams = new ArrayList<String>();

        for (String param : params) {
            questionParams.add(param + " = ?");
        }

        if(questionParams.size() > 0){
            builder.append(" WHERE ");
            builder.append(questionParams.stream().collect(Collectors.joining(" AND ")));
        }

        return builder.toString();
    }

    public static String functionQuery(Structure.Routine routine){
        StringBuilder builder;
        if(routine.type.equals("FUNCTION")){
            if(!routine.returnType.equals("TABLE"))
                builder = new StringBuilder("SELECT dbo.");
            else
                builder = new StringBuilder("SELECT * FROM dbo.");
            builder.append(routine.name);
            builder.append("(");
            List<String> questionParams =
                Collections.nCopies(routine.parameters.size(), "?");
            builder.append(questionParams.stream().collect(Collectors.joining(", ")));
            builder.append(")");
        }else{
            builder = new StringBuilder("{call ");
            builder.append(routine.name);
            builder.append("(");
            List<String> questionParams =
                Collections.nCopies(routine.parameters.size(), "?");
            builder.append(questionParams.stream().collect(Collectors.joining(", ")));
            builder.append(")}");
        }
        return builder.toString();
    }
}
