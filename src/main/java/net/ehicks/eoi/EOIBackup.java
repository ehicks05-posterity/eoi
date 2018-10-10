package net.ehicks.eoi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;

public class EOIBackup
{
    private static final Logger log = LoggerFactory.getLogger(EOIBackup.class);

    public static String getBackupExtension()
    {
        if (EOI.dialect.equals(Dialect.H2))
            return ".sql";
        if (EOI.dialect.equals(Dialect.SQL_SERVER))
            return ".bak";
        return ".dump";
    }

    public static void backup(String backupPath)
    {
        backup(backupPath, null);
    }

    public static void backup(String backupPath, ConnectionInfo connectionInfo)
    {
        if (EOI.dialect.equals(Dialect.H2))
            EOI.executeQuery("script to '" + backupPath + "'");
        if (EOI.dialect.equals(Dialect.SQL_SERVER))
            EOI.execute("BACKUP DATABASE " + connectionInfo.getDbName() + " TO DISK = '" + backupPath + "' WITH FORMAT;");
        if (EOI.dialect.equals(Dialect.POSTGRES))
        {
            String host = "--host="     + connectionInfo.getDbHost();
            String port = "--port="     + connectionInfo.getDbPort();
            String user = "--username=" + connectionInfo.getDbUser();
            String pass = "--password=" + connectionInfo.getDbPass();
            String file = "--file=" + backupPath;
            String format = "--format=custom";

            ProcessBuilder builder = new ProcessBuilder(connectionInfo.getPgDumpPath(), host, port, user, pass, file,
                    format, "--verbose", "-w", connectionInfo.getDbName());
            builder.redirectErrorStream(true);

            Map<String, String> env = builder.environment();
            env.put("PGPASSWORD", connectionInfo.getDbPass());

            try
            {
                Process process = builder.start();

                new Thread(() -> {
                    try
                    {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                        String line;
                        while ((line = reader.readLine()) != null)
                            log.info(line);
                    }
                    catch (IOException e)
                    {
                        log.error(e.getMessage(), e);
                    }
                }).start();

                process.waitFor();
            }
            catch (Exception e)
            {
                log.error(e.getMessage(), e);
            }
        }
    }
}
