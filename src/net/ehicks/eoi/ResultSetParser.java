package net.ehicks.eoi;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Blob;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ResultSetParser
{
    public static <T> List<T> parseResultSet(String queryString, ResultSet resultSet, boolean bypassCache) throws Exception
    {
        List<T> results = new ArrayList<>();

        SQLQuery sqlQuery = SQLQuery.parseSQL(queryString);
        DBMap dbMap = sqlQuery.dbMap;

        if (dbMap == null)
        {
            int i = 1;
            while (resultSet.next())
            {
                Object object = resultSet.getObject(i);
                results.add((T) object);
                i++;
            }
            return results;
        }

        if (queryString.startsWith("select *"))
        {
            while (resultSet.next())
            {
                Object object = getEOIObjectFromResultSet(resultSet, dbMap, bypassCache);
                results.add((T) object);
            }
        }
        else
        {
            List<ProjectionColumn> projectionColumns = ProjectionColumn.getProjectionColumns(queryString, dbMap);
            while (resultSet.next())
            {
                List<Object> list = new ArrayList<>();
                for (ProjectionColumn projectionColumn : projectionColumns)
                {
                    if (projectionColumn.type.equals("STRING"))
                        list.add(resultSet.getString(projectionColumn.columnLabel));
                    if (projectionColumn.type.equals("INTEGER"))
                        list.add(resultSet.getInt(projectionColumn.columnLabel));
                    if (projectionColumn.type.equals("LONG"))
                        list.add(resultSet.getLong(projectionColumn.columnLabel));
                    if (projectionColumn.type.equals("DECIMAL"))
                        list.add(resultSet.getBigDecimal(projectionColumn.columnLabel));
                    if (projectionColumn.type.equals("TIMESTAMP"))
                        list.add(resultSet.getTimestamp(projectionColumn.columnLabel));
                    if (projectionColumn.type.equals("BLOB"))
                    {
                        Blob blob = resultSet.getBlob(projectionColumn.columnLabel);
                        list.add(blob.getBytes(0, (int) blob.length()));
                    }
                    if (projectionColumn.type.equals("BOOLEAN"))
                        list.add(resultSet.getBoolean(projectionColumn.columnLabel));
                }
                results.add((T) list);
            }
        }
        return results;
    }

    private static Object getEOIObjectFromResultSet(ResultSet resultSet, DBMap dbMap, boolean bypassCache) throws Exception
    {
        Object object = dbMap.constructor.newInstance();

        for (DBMapField field : dbMap.getPKFields())
            invokeSetter(resultSet, object, field);

        // we've filled the PK, can we find it in cache?
        if (!bypassCache)
        {
            Object fromCache = EOICache.get(object.toString());
            if (fromCache != null)
                return fromCache;
        }

        for (DBMapField field : dbMap.getNonPKFields())
            invokeSetter(resultSet, object, field);

        EOICache.set(object);

        return object;
    }

    private static void invokeSetter(ResultSet resultSet, Object object, DBMapField field) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, SQLException
    {
        Method method = field.getSetter();

        if (field.type.equals("STRING"))
            method.invoke(object, resultSet.getString(field.columnName));
        if (field.type.equals("INTEGER"))
            method.invoke(object, resultSet.getInt(field.columnName));
        if (field.type.equals("LONG"))
            method.invoke(object, resultSet.getLong(field.columnName));
        if (field.type.equals("DECIMAL"))
            method.invoke(object, resultSet.getBigDecimal(field.columnName));
        if (field.type.equals("TIMESTAMP"))
            method.invoke(object, resultSet.getTimestamp(field.columnName));
        if (field.type.equals("BLOB"))
        {
            Blob blob = resultSet.getBlob(field.columnName);
            byte[] bytes = blob.getBytes(0, (int) blob.length());
            method.invoke(object, (Object) bytes);
        }
        if (field.type.equals("BOOLEAN"))
            method.invoke(object, resultSet.getBoolean(field.columnName));
    }
}
