
import com.healthmarketscience.sqlbuilder.dbspec.basic.*;
import com.healthmarketscience.sqlbuilder.*;

import java.util.*;
import java.util.stream.*;

public class QueryBuilder{

    public static String selectQuery(Structure.Table table){
        DbSpec builderSpec = new DbSpec();
        DbSchema builderSchema = builderSpec.addDefaultSchema();
        DbTable builderTable = builderSchema.addTable(table.name);

        for (Map.Entry<String, String> entry : table.columns.entrySet()) {
            builderTable.addColumn(entry.getKey(), entry.getValue(), null);
        }
        return new SelectQuery()
            .addAllTableColumns(builderTable)
            .validate().toString();
    }

    public static String insertQuery(Structure.Table table, String[] keys){
        DbSpec builderSpec = new DbSpec();
        DbSchema builderSchema = builderSpec.addDefaultSchema();
        DbTable builderTable = builderSchema.addTable(table.name);

        for (Map.Entry<String, String> entry : table.columns.entrySet()) {
            builderTable.addColumn(entry.getKey(), entry.getValue(), null);
        }

        InsertQuery query = new InsertQuery(builderTable);

        query.addPreparedColumns(builderTable.findColumns(keys));

        return query.validate().toString();
    }

    public static String updateQuery(
            Structure.Table table, 
            Map<String, String> values,
            Map<String, String[]> queryParams
    ){
        DbSpec builderSpec = new DbSpec();
        DbSchema builderSchema = builderSpec.addDefaultSchema();
        DbTable builderTable = builderSchema.addTable(table.name);

        for (Map.Entry<String, String> entry : table.columns.entrySet()) {
            builderTable.addColumn(entry.getKey(), entry.getValue(), null);
        }

        UpdateQuery query = new UpdateQuery(builderTable);

        for (Map.Entry<String, String> entry : values.entrySet()) {
            query.addSetClause( builderTable.findColumn(entry.getKey()), entry.getValue());
        }

        for (Map.Entry<String, String[]> entry : queryParams.entrySet()) {
            query.addCondition( 
                BinaryCondition.equalTo(builderTable.findColumn(entry.getKey()),
                    entry.getValue()[0])
            );
        }

        return query.validate().toString();
    }

    public static String deleteQuery(
            Structure.Table table, 
            String[] params
    ){
        StringBuilder builder = new StringBuilder("DELETE FROM ");
        builder.append(table.name);
        builder.append(" WHERE ");

        List<String> questionParams = new ArrayList<String>();

        for (String param : params) {
            questionParams.add(param + " = ?");
        }

        builder.append(questionParams.stream().collect(Collectors.joining(" AND ")));

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
