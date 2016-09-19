package net.ehicks.eoi;

import org.h2.jdbcx.JdbcConnectionPool;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class EOI
{
    private static JdbcConnectionPool cp;

    // example connectionString: jdbc:h2:~/test;TRACE_LEVEL_FILE=1;CACHE_SIZE=131072;SCHEMA=CINEMANG
    public static void init(String connectionString)
    {
        cp = JdbcConnectionPool.create(connectionString, "", "");
    }

    public static void destroy()
    {
        executeUpdate("shutdown compact");
        cp.dispose();
    }

    private static Connection getConnection()
    {
        try
        {
            return cp.getConnection();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return null;
    }

    // -------- String-Based Methods -------- //

    // for INSERT, UPDATE, or DELETE, or DDL statements
    public static int executeUpdate(String queryString)
    {
        try (Connection connection = getConnection();
             Statement statement = connection.createStatement();)
        {
            return statement.executeUpdate(queryString);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return 0;
    }

    public static void execute(String queryString)
    {
        try (Connection connection = getConnection();
             Statement statement = connection.createStatement();)
        {
            statement.execute(queryString);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    // -------- Object-Based Methods -------- //

    public static long insert(Object object)
    {
        if (object instanceof List)
            return _insertFromList((List) object);
        else
            return _insert(object);
    }

    private static long _insertFromList(List<?> objects)
    {
        int success = 0;
        int fail = 0;
        for (Object object : objects)
        {
            long result = _insert(object);
            if (result > 0)
                success++;
            else
                fail++;
        }
        System.out.println("Finished mass create: " + success + " succeeded, " + fail + " failed");
        return success;
    }

    private static long _insert(Object object)
    {
        String insertStatement = SQLGenerator.getInsertStatement(object);

        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(insertStatement, Statement.RETURN_GENERATED_KEYS);)
        {
            DBMap dbMap = DBMap.getDBMapByClass(object.getClass());

            int argIndex = 1;
            for (DBMapField dbMapField : dbMap.fields)
            {
                if (dbMapField.autoIncrement)
                    continue;

                Object value = dbMapField.getValue(object);
                if (value == null)
                {
                    preparedStatement.setNull(argIndex++, Types.NULL);
                    continue;
                }

                setPreparedStatementParameter(preparedStatement, argIndex++, value);
            }

            int queryResult = preparedStatement.executeUpdate();

            ResultSet generatedKeysResultSet = preparedStatement.getGeneratedKeys();
            if (generatedKeysResultSet == null)
                return 0;

            if (generatedKeysResultSet.next())
                return generatedKeysResultSet.getLong(1);
            else
                return 0;
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return 0;
    }

    public static void update(Object object)
    {
        if (object instanceof List)
            _updateFromList((List) object);
        else
            _update(object);
    }

    private static void _updateFromList(List<?> objects)
    {
        int success = 0;
        int fail = 0;
        for (Object object : objects)
        {
            int result = _update(object);
            if (result == 1)
                success++;
            else
                fail++;
        }
        System.out.println("Finished mass update: " + success + " succeeded, " + fail + " failed");
    }

    private static int _update(Object object)
    {
        PSIngredients psIngredients = SQLGenerator.getUpdateStatement(object);
        if (psIngredients == null)
            return 0;

        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(psIngredients.query);)
        {
            int argIndex = 1;
            for (Object arg : psIngredients.args)
                setPreparedStatementParameter(preparedStatement, argIndex++, arg);

            int result = preparedStatement.executeUpdate();
            if (result == 1)
            {
                EOICache.set(object);
                return result;
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return 0;
    }

    public static <T> T executeQueryOneResult(String queryString)
    {
        return executeQueryOneResult(queryString, new ArrayList<>(), false);
    }

    public static <T> T executeQueryOneResult(String queryString, List<Object> args)
    {
        return executeQueryOneResult(queryString, args, false);
    }

    public static <T> T executeQueryOneResult(String queryString, List<Object> args, boolean bypassCache)
    {
        List<T> results = executeQuery(queryString, args, bypassCache);
        if (results != null && results.size() > 0)
            return results.get(0);
        return null;
    }

    public static <T> List<T> executeQuery(String queryString)
    {
        return executeQuery(queryString, new ArrayList<>(), false);
    }

    public static <T> List<T> executeQuery(String queryString, List<Object> args)
    {
        return executeQuery(queryString, args, false);
    }

    public static <T> List<T> executeQuery(String queryString, List<Object> args, boolean bypassCache)
    {
        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(queryString))
        {
            int argIndex = 1;
            for (Object arg : args)
                setPreparedStatementParameter(preparedStatement, argIndex++, arg);

            ResultSet resultSet = preparedStatement.executeQuery();

            return ResultSetParser.parseResultSet(queryString, resultSet, bypassCache);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return null;
    }

    public static <T> List<T> executeQueryWithoutPS(String queryString, boolean bypassCache)
    {
        try (Connection connection = getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(queryString);)
        {
            return ResultSetParser.parseResultSet(queryString, resultSet, bypassCache);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return null;
    }

    public static int executeDelete(Object object)
    {
        PSIngredients psIngredients = SQLGenerator.getDeleteStatement(object);
        if (psIngredients == null)
            return 0;

        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(psIngredients.query);)
        {
            int argIndex = 1;
            for (Object arg : psIngredients.args)
                setPreparedStatementParameter(preparedStatement, argIndex++, arg);

            int result = preparedStatement.executeUpdate();
            if (result == 1)
            {
                EOICache.unset(object);
                return result;
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return 0;
    }

    private static void setPreparedStatementParameter(PreparedStatement ps, int argIndex, Object obj) throws SQLException
    {
        if (obj instanceof String) ps.setString(argIndex, (String) obj);
        if (obj instanceof Integer) ps.setInt(argIndex, (Integer) obj);
        if (obj instanceof Long) ps.setLong(argIndex, (Long) obj);
        if (obj instanceof BigDecimal) ps.setBigDecimal(argIndex, (BigDecimal) obj);
        if (obj instanceof Date) ps.setTimestamp(argIndex, new Timestamp(((Date) obj).getTime()));
        if (obj instanceof byte[]) ps.setBytes(argIndex, (byte[]) obj);
        if (obj instanceof Boolean) ps.setBoolean(argIndex, (Boolean) obj);
    }

    public static boolean isTableExists(DBMap dbMap)
    {
        try (Connection connection = getConnection();)
        {
            DatabaseMetaData databaseMetaData = connection.getMetaData();
            try (ResultSet resultSet = databaseMetaData.getTables(connection.getCatalog(), null, null, null);)
            {
                List<String> tableNamesFromDb = new ArrayList<>();
                while (resultSet.next())
                {
                    String tableName = resultSet.getString("TABLE_NAME").toUpperCase();
                    tableNamesFromDb.add(tableName);
                    if (tableName.equals(dbMap.tableName.toUpperCase()))
                        return true;
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return false;
    }

    public static String getCurrentSchema()
    {
        try (Connection connection = getConnection();)
        {
            DatabaseMetaData databaseMetaData = connection.getMetaData();
            try (ResultSet resultSet = databaseMetaData.getSchemas(connection.getCatalog(), "");)
            {
                List<String> schemaNames = new ArrayList<>();
                while (resultSet.next())
                {
                    String schemaName = resultSet.getString("TABLE_SCHEM").toUpperCase();
                    System.out.println(schemaName);
                    schemaNames.add(schemaName);
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return null;
    }

    public static boolean setSchema(String schemaName)
    {
        try (Connection connection = getConnection();)
        {
            connection.setSchema(schemaName);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return false;
    }
}
