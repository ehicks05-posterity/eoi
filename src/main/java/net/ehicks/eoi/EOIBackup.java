package net.ehicks.eoi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class EOIBackup
{
    private static final Logger log = LoggerFactory.getLogger(EOIBackup.class);

    public static String getBackupExtension()
    {
        if (EOI.dbBrand.equals(DbBrand.H2))
            return ".sql";
        if (EOI.dbBrand.equals(DbBrand.SQL_SERVER))
            return ".bak";
        return ".dump";
    }

    public static void backup(String backupPath)
    {
        backup(backupPath, null, null, null, null, null, null);
    }

    public static void backup(String backupPath, String exePath, String dbHost, String dbPort, String dbName, String dbUser, String dbPass)
    {
        if (EOI.dbBrand.equals(DbBrand.H2))
            EOI.executeQuery("script to '" + backupPath + "'");
        if (EOI.dbBrand.equals(DbBrand.SQL_SERVER))
            EOI.execute("BACKUP DATABASE " + dbName + " TO DISK = '" + backupPath + "' WITH FORMAT;");
        if (EOI.dbBrand.equals(DbBrand.POSTGRES))
        {
            String host = "--host=" + dbHost;
            String port = "--port=" + dbPort;
            String user = "--username=" + dbUser;
            String pass = "--password=" + dbPass;
            String file = "--file=" + backupPath;
            String format = "--format=custom";

            ProcessBuilder builder = new ProcessBuilder(exePath, host, port, user, pass, file, format, "-w", dbName);
            builder.redirectErrorStream(true);

            try
            {
                Process process = builder.start();

                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                StringBuilder stringBuilder = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null)
                {
                    stringBuilder.append(line);
                    stringBuilder.append(System.getProperty("line.separator"));
                }

                String result = stringBuilder.toString();
                if (result.length() > 0)
                    log.info(result);
            }
            catch (IOException e)
            {
                log.error(e.getMessage(), e);
            }
        }
    }
}
