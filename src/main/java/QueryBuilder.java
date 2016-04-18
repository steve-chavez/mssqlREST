
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

}
