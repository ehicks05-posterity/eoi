package net.ehicks.eoi;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

public class SQLGenerator
{
    public static String getCreateTableStatement(DBMap dbMap)
    {
        String createStatement = "create table " + dbMap.tableName + " ";
        String columns = "";
        for (DBMapField dbMapField : dbMap.fields)
        {
            if (columns.length() > 0)
                columns += ", ";
            columns += dbMapField.columnName + " " + dbMapField.getColumnDefinition();
        }
        createStatement += "(" + columns + ");";

        return createStatement;
    }

    public static String getInsertStatement(Object object)
    {
        DBMap dbMap = DBMap.getDBMapByClass(object.getClass());
        String columnNames = "";
        String columnValues = "";
        for (DBMapField dbMapField : dbMap.fields)
        {
            if (dbMapField.autoIncrement)
                continue;

            if (columnNames.length() > 0)
                columnNames += ",";
            columnNames += dbMapField.columnName;

            if (columnValues.length() > 0)
                columnValues += ",";
            columnValues += "?";
        }
        return "insert into " + dbMap.tableName + " (" + columnNames + ") values (" + columnValues + ");";
    }

    /*
    * 
    * */
    public static <T> PSIngredients getUpdateStatement(T object)
    {
        DBMap dbMap = DBMap.getDBMapByClass(object.getClass());
        PSIngredients whereClause = getWhereClause(object);
        T existing = EOI.executeQueryOneResult("select * from " + dbMap.tableName + whereClause.query, whereClause.args, true);
        if (existing == null)
            return null;

        String setClause = " set ";
        List<Object> setClauseArgs = new ArrayList<>();
        for (DBMapField dbMapField : dbMap.fields)
        {
            try
            {
                Object valueInDb = dbMapField.getGetter().invoke(existing);
                Object newValue = dbMapField.getGetter().invoke(object);

                boolean bothNull = newValue == null && valueInDb == null;
                boolean bothExist = newValue != null && valueInDb != null;
                boolean equal = bothNull || (bothExist && newValue.equals(valueInDb));
                if (equal)
                    continue;

                if (!setClause.endsWith(" set "))
                    setClause += ",";

                setClause += dbMapField.columnName + "=?";

                setClauseArgs.add(newValue);
            }
            catch (IllegalAccessException | InvocationTargetException e)
            {
                e.printStackTrace();
            }
        }

        if (setClauseArgs.size() == 0)
            return null;

        List<Object> args = new ArrayList<>(setClauseArgs);
        args.addAll(whereClause.args);
        return new PSIngredients("update " + dbMap.tableName + setClause + whereClause.query + ";", args);
    }

    public static <T> PSIngredients getDeleteStatement(T object)
    {
        DBMap dbMap = DBMap.getDBMapByClass(object.getClass());
        PSIngredients whereClause = getWhereClause(object);
        List<T> existing = EOI.executeQuery("select * from " + dbMap.tableName + whereClause.query, whereClause.args, true);

        if (existing == null)
            return null;
        if (existing.size() > 1)
            return null;

        return new PSIngredients("delete from " + dbMap.tableName + whereClause.query + ";", whereClause.args);
    }

    public static PSIngredients getWhereClause(Object object)
    {
        String where = " where ";
        List<Object> args = new ArrayList<>();

        DBMap dbMap = DBMap.getDBMapByClass(object.getClass());
        for (DBMapField pkField : dbMap.getPKFields())
        {
            String columnName = pkField.columnName;
            Object columnValue = null;
            try
            {
                columnValue = pkField.getGetter().invoke(object, null);
            }
            catch (IllegalAccessException | InvocationTargetException e)
            {
                e.printStackTrace();
            }

            if (columnValue != null)
            {
                if (where.length() > 7)
                    where += " AND ";

                where += columnName + "=?";
                args.add(columnValue);
            }
        }

        return new PSIngredients(where, args);
    }

    public static String getCountVersionOfQuery(String query)
    {
        int indexOfFrom = query.indexOf("from");
        query = query.substring(indexOfFrom);

        int indexOfOrderBy = query.indexOf("order by");
        if (indexOfOrderBy != -1)
            query = query.substring(0, indexOfOrderBy);

        int indexOfGroupBy = query.indexOf("group by");
        if (indexOfGroupBy != -1)
            query = query.substring(0, indexOfGroupBy);

        int indexOfHaving = query.indexOf(" having ");
        if (indexOfHaving != -1)
            query = query.substring(0, indexOfHaving);

        return "select count(*) " + query;
    }
}
