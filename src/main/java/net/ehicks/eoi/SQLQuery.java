package net.ehicks.eoi;

public class SQLQuery
{
    public String queryString = "";
    public DBMap dbMap;
    public boolean isCreate = false;
    public boolean isSelect = false;
    public boolean isUpdate = false;
    public boolean isDelete = false;

    public SQLQuery(String queryString, DBMap dbMap, boolean isCreate, boolean isSelect, boolean isUpdate, boolean isDelete)
    {
        this.queryString = queryString;
        this.dbMap = dbMap;
        this.isCreate = isCreate;
        this.isSelect = isSelect;
        this.isUpdate = isUpdate;
        this.isDelete = isDelete;
    }

    public static SQLQuery parseSQL(String sql)
    {
        boolean isCreate = false;
        boolean isSelect = false;
        boolean isUpdate = false;
        boolean isDelete = false;

        if (sql.startsWith("create")) isCreate = true;
        if (sql.startsWith("select")) isSelect = true;
        if (sql.startsWith("update")) isUpdate = true;
        if (sql.startsWith("delete")) isDelete = true;

        String[] tokens = sql.split(" ");

        String table = "";
        for (int i = 0; i < tokens.length; i++)
            if (tokens[i].equals("from"))
            {
                table = tokens[i + 1];
                break;
            }

        if (table.endsWith(";"))
            table = table.substring(0, table.length() - 1);

        DBMap dbMap = DBMap.getDBMapByTableName(table);
        return new SQLQuery(sql, dbMap, isCreate, isSelect, isUpdate, isDelete);
    }
}
