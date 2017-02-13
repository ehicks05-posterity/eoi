package net.ehicks.eoi;

public class EOIBackup
{
    public static String getBackupExtension()
    {
        if (EOI.databaseBrand.equals("h2"))
            return ".sql";
        if (EOI.databaseBrand.equals("sqlserver"))
            return ".bak";
        return ".dmp";
    }

    public static void backup(String backupPath)
    {
        if (EOI.databaseBrand.equals("h2"))
            EOI.executeQuery("script to '" + backupPath + "'");
        if (EOI.databaseBrand.equals("sqlserver"))
        {
            String dbName = "master";
            EOI.execute("BACKUP DATABASE " + dbName + " TO DISK = '" + backupPath + "' WITH FORMAT;");
        }
    }
}
