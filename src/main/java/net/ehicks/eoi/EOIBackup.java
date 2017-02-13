package net.ehicks.eoi;

public class EOIBackup
{
    public static void backup(String backupPath)
    {
        EOI.executeQuery("script to '" + backupPath + "'");
    }
}
