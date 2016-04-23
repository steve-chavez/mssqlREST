
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

    public static String insertQuery(Structure.Table table, Map<String, String> values){
        DbSpec builderSpec = new DbSpec();
        DbSchema builderSchema = builderSpec.addDefaultSchema();
        DbTable builderTable = builderSchema.addTable(table.name);

        for (Map.Entry<String, String> entry : table.columns.entrySet()) {
            builderTable.addColumn(entry.getKey(), entry.getValue(), null);
        }

        InsertQuery query = new InsertQuery(builderTable);

        for (Map.Entry<String, String> entry : values.entrySet()) {
            query.addColumn( builderTable.findColumn(entry.getKey()), entry.getValue());
        }

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
            Map<String, String[]> queryParams
    ){
        DbSpec builderSpec = new DbSpec();
        DbSchema builderSchema = builderSpec.addDefaultSchema();
        DbTable builderTable = builderSchema.addTable(table.name);

        for (Map.Entry<String, String> entry : table.columns.entrySet()) {
            builderTable.addColumn(entry.getKey(), entry.getValue(), null);
        }

        DeleteQuery query = new DeleteQuery(builderTable);

        for (Map.Entry<String, String[]> entry : queryParams.entrySet()) {
            query.addCondition( 
                BinaryCondition.equalTo(builderTable.findColumn(entry.getKey()),
                    entry.getValue()[0])
            );
        }

        return query.validate().toString();
    }

    public static String functionQuery(Structure.Routine routine){
        StringBuilder builder = new StringBuilder("SELECT dbo.");
        builder.append(routine.name);
        builder.append("(");
        List<String> questionParams =
            Collections.nCopies(routine.parameters.size(), "?");
        builder.append(questionParams.stream().collect(Collectors.joining(", ")));
        builder.append(")");
        return builder.toString();
    }
}
