package net.ehicks.eoi;

public class EOIBackup
{
    public static String getBackupExtension()
    {
        if (EOI.dbBrand.equals(DbBrand.H2))
            return ".sql";
        if (EOI.dbBrand.equals(DbBrand.SQL_SERVER))
            return ".bak";
        return ".dmp";
    }

    public static void backup(String backupPath)
    {
        if (EOI.dbBrand.equals(DbBrand.H2))
            EOI.executeQuery("script to '" + backupPath + "'");
        if (EOI.dbBrand.equals(DbBrand.SQL_SERVER))
        {
            String dbName = "master";
            EOI.execute("BACKUP DATABASE " + dbName + " TO DISK = '" + backupPath + "' WITH FORMAT;");
        }
    }
}
