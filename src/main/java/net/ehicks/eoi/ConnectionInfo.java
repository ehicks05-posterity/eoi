package net.ehicks.eoi;

public class ConnectionInfo
{
    private String dbMode = "";
    private String dbHost = "";
    private String dbPort = "";
    private String dbName = "";
    private String dbUser = "";
    private String dbPass = "";

    private String h2DbCacheKBs = "";
    private String pgDumpPath = "";
    private String sqlserverServerInstance = "";

    public enum DbMode
    {
        H2_MEM, H2_TCP, SQLSERVER, POSTGRESQL
    }

    @Override
    public String toString()
    {
        return "ConnectionInfo{" +
                "dbMode='" + dbMode + '\'' +
                ", dbHost='" + dbHost + '\'' +
                ", dbPort='" + dbPort + '\'' +
                ", dbName='" + dbName + '\'' +
                ", dbUser='" + dbUser + '\'' +
                ", dbPass='" + "****" + '\'' +
                ", h2DbCacheKBs='" + h2DbCacheKBs + '\'' +
                ", pgDumpPath='" + pgDumpPath + '\'' +
                ", sqlserverServerInstance='" + sqlserverServerInstance + '\'' +
                '}';
    }

    public ConnectionInfo()
    {
    }

    public ConnectionInfo(String dbMode, String dbHost, String dbPort, String dbName, String dbUser, String dbPass, String h2DbCacheKBs, String pgDumpPath, String sqlserverServerInstance)
    {
        this.dbMode = dbMode;
        this.dbHost = dbHost;
        this.dbPort = dbPort;
        this.dbName = dbName;
        this.dbUser = dbUser;
        this.dbPass = dbPass;
        this.h2DbCacheKBs = h2DbCacheKBs;
        this.pgDumpPath = pgDumpPath;
        this.sqlserverServerInstance = sqlserverServerInstance;
    }

    public Dialect getDialect()
    {
        if (dbMode.equals(DbMode.H2_MEM.toString()))
            return Dialect.H2;
        if (dbMode.equals(DbMode.SQLSERVER.toString()))
            return Dialect.SQL_SERVER;
        if (dbMode.equals(DbMode.POSTGRESQL.toString()))
            return Dialect.POSTGRES;
        return null;
    }

    public String getDbConnectionString(boolean showPassword)
    {
        String connectionString = "";
        String password = showPassword ? dbPass : "****";

        String h2Settings = "TRACE_LEVEL_FILE=1;DB_CLOSE_ON_EXIT=FALSE;COMPRESS=TRUE;";
        if (getDbMode().equals(DbMode.H2_MEM.name()))
        {
            connectionString += "jdbc:h2:mem:" + dbName;
        }
        if (getDbMode().equals(DbMode.H2_TCP.name()))
        {
            connectionString += "jdbc:h2:tcp://" + dbHost + ":" + dbPort + "/" + dbName + ";" + h2Settings + "CACHE_SIZE=" + h2DbCacheKBs + ";";
        }
        if (getDbMode().equals(DbMode.SQLSERVER.name()))
        {
            connectionString += "jdbc:sqlserver://" + dbHost + "\\" + sqlserverServerInstance + ":" + dbPort + ";user=" + dbUser + ";password=" + password;
        }
        if (getDbMode().equals(DbMode.POSTGRESQL.name()))
        {
            connectionString += "jdbc:postgresql://" + dbHost + ":" + dbPort + "/" + dbName + "?user=" + dbUser + "&password=" + password;
        }

        return connectionString;
    }

    public String getDbMode()
    {
        return dbMode;
    }

    public void setDbMode(String dbMode)
    {
        this.dbMode = dbMode;
    }

    public String getDbHost()
    {
        return dbHost;
    }

    public void setDbHost(String dbHost)
    {
        this.dbHost = dbHost;
    }

    public String getDbPort()
    {
        return dbPort;
    }

    public void setDbPort(String dbPort)
    {
        this.dbPort = dbPort;
    }

    public String getDbName()
    {
        return dbName;
    }

    public void setDbName(String dbName)
    {
        this.dbName = dbName;
    }

    public String getDbUser()
    {
        return dbUser;
    }

    public void setDbUser(String dbUser)
    {
        this.dbUser = dbUser;
    }

    public String getDbPass()
    {
        return dbPass;
    }

    public void setDbPass(String dbPass)
    {
        this.dbPass = dbPass;
    }

    public String getH2DbCacheKBs()
    {
        return h2DbCacheKBs;
    }

    public void setH2DbCacheKBs(String h2DbCacheKBs)
    {
        this.h2DbCacheKBs = h2DbCacheKBs;
    }

    public String getPgDumpPath()
    {
        return pgDumpPath;
    }

    public void setPgDumpPath(String pgDumpPath)
    {
        this.pgDumpPath = pgDumpPath;
    }

    public String getSqlserverServerInstance()
    {
        return sqlserverServerInstance;
    }

    public void setSqlserverServerInstance(String sqlserverServerInstance)
    {
        this.sqlserverServerInstance = sqlserverServerInstance;
    }
}
