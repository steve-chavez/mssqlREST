
import com.healthmarketscience.sqlbuilder.dbspec.basic.*;
import com.healthmarketscience.sqlbuilder.*;

import java.util.*;
import model.*;

public class QueryBuilder{

    public static String selectQuery(Table table){
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

    public static String insertQuery(Table table, Map<String, String> values){
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
            Table table, 
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
            Table table, 
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

}
