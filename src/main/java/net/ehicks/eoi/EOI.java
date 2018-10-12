package net.ehicks.eoi;

import com.zaxxer.hikari.HikariDataSource;
import org.h2.tools.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.*;
import java.util.*;
import java.util.Date;

public class EOI
{
    private static final Logger log = LoggerFactory.getLogger(EOI.class);
    private static Server h2Server;

    public static HikariDataSource cp;
    public static ConnectionInfo connectionInfo;
    public static Dialect dialect;
    public static boolean enableCache = false;
    public static String poolName = "Primary Pool";
    public static ThreadLocal<Connection> conn = new ThreadLocal<>();
    public static int slowQueryThreshold = 100;

    public static void init(ConnectionInfo connectionInfo)
    {
        EOI.connectionInfo = connectionInfo;
        try
        {
            dialect = connectionInfo.getDialect();
            if (dialect.equals(Dialect.H2))
                h2Server = Server.createTcpServer("-tcpAllowOthers").start();

            log.info("EOI is connecting to {}", connectionInfo.getDbConnectionString(false));

            cp = new HikariDataSource();

            if (dialect.equals(Dialect.H2))
                cp.setDriverClassName("org.h2.Driver");
            if (dialect.equals(Dialect.SQL_SERVER))
                cp.setDriverClassName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
            if (dialect.equals(Dialect.POSTGRES))
                cp.setDriverClassName("org.postgresql.Driver");

            cp.setPoolName(poolName);
            cp.setMetricRegistry(Metrics.getMetricRegistry());
            cp.setJdbcUrl(connectionInfo.getDbConnectionString(true));
        }
        catch (Exception e)
        {
            log.error(e.getMessage(), e);
        }
    }

    public static void destroy()
    {
        cp.close();
        if (connectionInfo.getDbMode().equals(ConnectionInfo.DbMode.H2_TCP.toString()))
            h2Server.stop();
    }

    public static int getSlowQueryThreshold()
    {
        return slowQueryThreshold;
    }

    public static void setSlowQueryThreshold(int slowQueryThreshold)
    {
        EOI.slowQueryThreshold = slowQueryThreshold;
    }

    private static Connection getConnection()
    {
        return getConnection(true);
    }

    private static Connection getConnection(boolean autoCommit)
    {
        Connection connection = conn.get();
        try
        {
            // check for existing connection (indicates a transaction was already started??)
            if (connection == null)
            {
                connection = cp.getConnection();
                conn.set(connection);

                if (!autoCommit)
                    connection.setAutoCommit(false);
            }

            return connection;
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return null;
    }

    public static void startTransaction()
    {
        getConnection(false);
    }

    public static void commit()
    {
        try
        {
            Connection connection = conn.get();
            if (connection != null)
            {
                connection.commit();
                closeConnection(true);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public static void rollback()
    {
        try
        {
            Connection connection = conn.get();
            if (connection != null)
            {
                if (!connection.getAutoCommit())
                    connection.rollback();
                closeConnection(true);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private static void closeConnection(boolean hardClose)
    {
        try
        {
            Connection connection = conn.get();
            if (connection != null)
            {
                if (!connection.getAutoCommit() && !hardClose)
                    return;

                connection.setAutoCommit(true);
                connection.close();
                conn.remove();
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    // -------- String-Based Methods -------- //

    // for INSERT, UPDATE, or DELETE, or DDL statements
    public static int executeUpdate(String queryString)
    {
        log.debug("executeUpdate(), Query: {}", queryString);

        Connection connection = getConnection();
        try
        {
            try (Statement statement = connection.createStatement();)
            {
                return statement.executeUpdate(queryString);
            }
            catch (Exception e)
            {
                rollback();
                e.printStackTrace();
            }
        }
        catch (Exception e)
        {
            log.error(e.getMessage(), e);
        }
        finally
        {
            closeConnection(false);
        }

        return 0;
    }

    // for INSERT, UPDATE, or DELETE, or DDL statements
    public static int executePreparedUpdate(String queryString, List<Object> args)
    {
        log.debug("executePreparedUpdate(), Query: {}, Args: {}", queryString, args);

        Connection connection = getConnection();
        try (PreparedStatement preparedStatement = connection.prepareStatement(queryString);)
        {
            int argIndex = 1;
            for (Object arg : args)
                setPreparedStatementParameter(preparedStatement, argIndex++, arg);

            return preparedStatement.executeUpdate();
        }
        catch (Exception e)
        {
            rollback();
            e.printStackTrace();
        }
        finally
        {
            closeConnection(false);
        }

        return 0;
    }

    public static void execute(String queryString)
    {
        log.debug("execute(), Query: {}", queryString);

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

    public static long insert(Object object, AuditUser auditUser)
    {
        if (object instanceof List)
            return _insertFromList((List) object, auditUser);
        else
            return _insert(object, auditUser);
    }

    private static long _insertFromList(List<?> objects, AuditUser auditUser)
    {
        int success = 0;
        int fail = 0;
        for (Object object : objects)
        {
            long result = _insert(object, auditUser);
            if (result > 0)
                success++;
            else
                fail++;
        }
        log.info("Finished mass create: {} succeeded, {} failed", success, fail);
        return success;
    }

    private static long _insert(Object object, AuditUser auditUser)
    {
        String insertStatement = SQLGenerator.getInsertStatement(object);

        Connection connection = getConnection();
        try (PreparedStatement preparedStatement = connection.prepareStatement(insertStatement, Statement.RETURN_GENERATED_KEYS);)
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
            if (generatedKeysResultSet == null || !generatedKeysResultSet.next())
                return 0;

            long generatedKey = generatedKeysResultSet.getLong(1);
            log.debug("_insert(), Object Class: {}, Generated Key: {}", object.getClass().toString(), generatedKey);

            // prepare audit
            createAudit(auditUser, "INSERT", dbMap, generatedKey);

            return generatedKey;
        }
        catch (Exception e)
        {
            rollback();
            e.printStackTrace();
        }
        finally
        {
            closeConnection(false);
        }
        return 0;
    }

    public static long batchInsert(List<?> objects)
    {
        log.debug("batchInsert(), Class: {}", objects.getClass().toString());

        String insertStatement = SQLGenerator.getInsertStatement(objects.get(0));

        Connection connection = getConnection();
        try (PreparedStatement preparedStatement = connection.prepareStatement(insertStatement))
        {
            DBMap dbMap = DBMap.getDBMapByClass(objects.get(0).getClass());

            for (Object object : objects)
            {
                int argIndex = 1;
                for (DBMapField dbMapField : dbMap.fields)
                {
                    if (dbMapField.autoIncrement)
                        continue;

                    Object value = dbMapField.getValue(object);

                    setPreparedStatementParameter(preparedStatement, argIndex++, value);
                }

                preparedStatement.addBatch();
            }
            preparedStatement.executeBatch();
        }
        catch (Exception e)
        {
            rollback();
            e.printStackTrace();
        }
        finally
        {
            closeConnection(false);
        }
        return 0;
    }

    private static void createAudit(AuditUser auditUser, String eventType, DBMap dbMap, long objectId)
    {
        createAudit(auditUser, eventType, dbMap, objectId, null, null, null, null);
    }

    private static void createAudit(AuditUser auditUser, String eventType, DBMap dbMap, long objectId, Object object, String fieldName, String oldValue, String newValue)
    {
        if (!dbMap.className.contains("Audit"))
        {
            String objectKey = "";
            if (object != null)
                objectKey = object.toString();
            else
                if (objectId != 0)
                    objectKey = dbMap.className + ":" + objectId;

            String queryString = "insert into audits (object_key, user_id, user_ip, event_time, event_type, field_name, old_value, new_value) values (?,?,?,?,?,?,?,?);";
            List<Object> args = Arrays.asList(objectKey, auditUser.getId(), auditUser.getIpAddress(), new Date(), eventType, fieldName, oldValue, newValue);
            EOI.executePreparedUpdate(queryString, args);
        }
    }

    public static void update(Object object, AuditUser auditUser)
    {
        if (object instanceof List)
            _updateFromList((List) object, auditUser);
        else
            _update(object, auditUser);
    }

    private static void _updateFromList(List<?> objects, AuditUser auditUser)
    {
        int success = 0;
        int fail = 0;
        for (Object object : objects)
        {
            int result = _update(object, auditUser);
            if (result == 1)
                success++;
            else
                fail++;
        }
        log.info("Finished mass update: {} succeeded, {} failed", success, fail);
    }

    private static int _update(Object object, AuditUser auditUser)
    {
        log.debug("_update(), Object: {}", object);
        PSIngredients psIngredients = SQLGenerator.getUpdateStatement(object);
        if (psIngredients == null)
            return 0;

        Connection connection = getConnection();
        try (PreparedStatement preparedStatement = connection.prepareStatement(psIngredients.query);)
        {
            int argIndex = 1;
            for (Object arg : psIngredients.args)
                setPreparedStatementParameter(preparedStatement, argIndex++, arg);

            int result = preparedStatement.executeUpdate();
            if (result == 1)
            {
                DBMap dbMap = DBMap.getDBMapByClass(object.getClass());
                for (PSIngredients.UpdatedField updatedField : psIngredients.updatedFields)
                {
                    String oldValue = updatedField.oldValue == null ? "<NULL>" : updatedField.oldValue.toString();
                    String newValue = updatedField.newValue == null ? "<NULL>" : updatedField.newValue.toString();
                    createAudit(auditUser, "UPDATE", dbMap, 0, object, updatedField.fieldName, oldValue, newValue);
                }

                if (enableCache)
                    EOICache.set(object);
                return result;
            }
        }
        catch (Exception e)
        {
            rollback();
            e.printStackTrace();
        }
        finally
        {
            closeConnection(false);
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
        log.debug("executeQuery(), Query: {}, Args: {}", queryString, args);

        Connection connection = getConnection();
        try (PreparedStatement preparedStatement = connection.prepareStatement(queryString))
        {
            int argIndex = 1;
            for (Object arg : args)
                setPreparedStatementParameter(preparedStatement, argIndex++, arg);

            long start = System.currentTimeMillis();
            ResultSet resultSet = preparedStatement.executeQuery();
            long end = System.currentTimeMillis();
            if (end - start >= slowQueryThreshold)
            {
                String message = "EOI QUERY took {} ms: {}. Args: {}";
                log.info(message, (end - start), queryString, args);
            }

            return ResultSetParser.parseResultSet(queryString, resultSet, bypassCache);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        finally
        {
            closeConnection(false);
        }

        return null;
    }

    public static Map<String, List<Object>> getPrintableResult(String queryString) throws SQLException
    {
        return getPrintableResult(queryString, Collections.emptyList());
    }
    
    public static Map<String, List<Object>> getPrintableResult(String queryString, List<Object> args) throws SQLException
    {
        log.debug("getPrintableResult(), Query: {}, Args: {}", queryString, args);

        Connection connection = getConnection();
        try (PreparedStatement preparedStatement = connection.prepareStatement(queryString))
        {
            int argIndex = 1;
            for (Object arg : args)
                setPreparedStatementParameter(preparedStatement, argIndex++, arg);

            long start = System.currentTimeMillis();
            ResultSet resultSet = preparedStatement.executeQuery();
            long end = System.currentTimeMillis();
            if (end - start >= slowQueryThreshold)
            {
                String message = "EOI QUERY took {} ms: {}. Args: {}";
                log.info(message, (end - start), queryString, args);
            }

            ResultSetMetaData metaData = resultSet.getMetaData();
            int columns = metaData.getColumnCount();
            List<Object> columnLabels = new ArrayList<>();
            for (int i = 0; i < columns; i++)
            {
                String columnLabel = metaData.getColumnLabel(i + 1);
                columnLabels.add(columnLabel);
            }

            List<Object> resultRows = new ArrayList<>();
            while (resultSet.next())
            {
                Object[] row = new Object[columns];
                for (int i = 0; i < columns; i++)
                    row[i] = resultSet.getObject(i + 1);
                resultRows.add(row);
            }

            Map<String, List<Object>> printableResults = new HashMap<>();
            printableResults.put("columnLabels", columnLabels);
            printableResults.put("resultRows", resultRows);

            return printableResults;
        }
        catch (Exception e)
        {
            e.printStackTrace();
            throw e;
        }
        finally
        {
            closeConnection(false);
        }
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

    public static int executeDelete(Object object, AuditUser auditUser)
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
                // prepare audit
                DBMap dbMap = DBMap.getDBMapByClass(object.getClass());
                createAudit(auditUser, "DELETE", dbMap, (Long) dbMap.getPKFields().get(0).getGetter().invoke(object));

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
        if (obj == null) ps.setNull(argIndex, Types.NULL);
    }

    /**
     * Checks tableNamePattern, tableNamePattern.toUpper, and tableNamePattern.toLower
     * <br>Uses DatabaseMetaData.getTables internally.
     */
    public static boolean isTableExists(String tableNamePattern)
    {
        return _isTableExists(tableNamePattern) || _isTableExists(tableNamePattern.toUpperCase()) || _isTableExists(tableNamePattern.toLowerCase());
    }

    private static boolean _isTableExists(String tableNamePattern)
    {
        Connection connection = getConnection(true);
        try
        {
            ResultSet resultSet = connection.getMetaData().getTables(null, null, tableNamePattern, new String[] {"TABLE"});

            return resultSet.next();
        }
        catch (SQLException e)
        {
            log.error(e.getMessage(), e);
        }
        finally
        {
            closeConnection(true);
        }
        return false;
    }

    /**
     * Checks tableNamePattern, tableNamePattern.toUpper, and tableNamePattern.toLower
     * <br>Uses DatabaseMetaData.getTables internally.
     */
    public static boolean isColumnExists(String tableNamePattern, String columnNamePattern)
    {
        return _isColumnExists(tableNamePattern, columnNamePattern)
                || _isColumnExists(tableNamePattern.toUpperCase(), columnNamePattern.toUpperCase())
                || _isColumnExists(tableNamePattern.toLowerCase(), columnNamePattern.toLowerCase());
    }

    private static boolean _isColumnExists(String tableNamePattern, String columnNamePattern)
    {
        Connection connection = getConnection(true);
        try
        {
            ResultSet resultSet = connection.getMetaData().getColumns(null, null, tableNamePattern, columnNamePattern);

            return resultSet.next();
        }
        catch (SQLException e)
        {
            log.error(e.getMessage(), e);
        }
        finally
        {
            closeConnection(true);
        }
        return false;
    }
}
