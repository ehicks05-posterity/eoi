package net.ehicks.eoi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

public class SQLMigrator
{
    private static final Logger log = LoggerFactory.getLogger(SQLMigrator.class);
    public static void migrate(List<DBMap> dbMaps)
    {
        long subTaskStart = System.currentTimeMillis();
        int tablesCreated = 0;
        int columnsCreated = 0;
        int columnsAltered = 0;

        for (DBMap dbMap : dbMaps)
        {
            // create table
            if (!EOI.isTableExists(dbMap.tableName))
            {
                String createTableStatement = SQLGenerator.getCreateTableStatement(dbMap);
                EOI.executeUpdate(createTableStatement);
                tablesCreated++;
                continue;
            }

            for (DBMapField dbMapField : dbMap.fields)
            {
                // create column
                if (!EOI.isColumnExists(dbMapField.dbMap.tableName, dbMapField.columnName))
                {
                    String alterTableStatement = getAlterTableStatement(dbMap, Arrays.asList(dbMapField), "ADD");
                    EOI.executeUpdate(alterTableStatement);
                    columnsCreated++;
                    continue;
                }

                // alter columns
            }
        }

        log.info("tables created: {}", tablesCreated);
        log.info("columns created: {}", columnsCreated);
        log.info("columns altered: {}", columnsAltered);
        log.info("SQLMigrator took {} ms", (System.currentTimeMillis() - subTaskStart));
    }

    public static String getAlterTableStatement(DBMap dbMap, List<DBMapField> dbMapFields, String verb)
    {
        String alterStatement = "alter table " + dbMap.tableName + " ";
        String columns = "";
        for (DBMapField dbMapField : dbMapFields)
        {
            if (columns.length() > 0)
                columns += ", ";
            columns += dbMapField.columnName + " " + dbMapField.getColumnDefinition();
        }
        alterStatement += " " + verb + " (" + columns + ");";

        return alterStatement;
    }
}
