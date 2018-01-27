package net.ehicks.eoi;

import net.ehicks.eoi.diagnostic.Project;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class Diagnostic
{
    @Test
    public void main() throws IOException, ClassNotFoundException
    {
        try
        {
            System.out.println("running diagnostic");
            runDiagnostic();
        }
        catch (Exception e)
        {
            System.out.println(e.getMessage());
        }
    }

    private static void runDiagnostic() throws IOException
    {
        ConnectionInfo connectionInfo = new ConnectionInfo(ConnectionInfo.DbMode.H2_MEM.toString(), "", "", "test", "",
                "", "2097152", "", "");

        EOI.init(connectionInfo);

        System.out.println(connectionInfo);

        AuditUser auditUser = new AuditUser()
        {
            @Override
            public String getId()
            {
                return "Diagnostic";
            }

            @Override
            public String getIpAddress()
            {
                return "";
            }
        };

        DBMap.loadDbMaps(new File("src/main/java/net/ehicks/eoi/diagnostic").getCanonicalPath(), "net.ehicks.eoi.diagnostic");

        System.out.println("\r\nDrop tables:");
        dropTables();

        System.out.println("\r\nCreate tables:");
        createTables();

        System.out.println("\r\nAdd some rows:");
        List<Project> projects = Project.getAll();
        if (projects.size() == 0)
        {
            Project project = new Project();
            project.setName("Genesis");
            project.setPrefix("GS");
            long newId = EOI.insert(project, auditUser);

            project = Project.getById(newId);
            System.out.println("We just created: " + project);

            project = new Project();
            project.setName("SchoolFI");
            project.setPrefix("SF");
            project.setInceptDate(new Date());
            newId = EOI.insert(project, auditUser);

            project = Project.getById(newId);
            System.out.println("We just created: " + project);
        }

        printAllProjects();

        System.out.println("\r\nUpdate some rows:");
        for (Project project : Project.getAll())
        {
            if (project.getId() == 1)
            {
                project.setName("World Peace");
                project.setPrefix("WP");
                project.setInceptDate(new Date());
                EOI.update(project, auditUser);
            }
            if (project.getId() == 2)
            {
                project.setName("Write a Book");
                project.setPrefix("WB");
                project.setInceptDate(null);
                EOI.update(project, auditUser);
            }

            System.out.println("We just updated: " + project);
        }

        printAllProjects();

        for (Project project : Project.getAll())
        {
            EOI.executeDelete(project, auditUser);
            System.out.println("We just deleted: " + project);
        }

        EOI.executeQuery("select * from projects where id in (?,?,?,?,?)", Arrays.asList(1,2,3,4,5));

        System.out.println("\r\nTest Metrics:");
        for (String key : Metrics.getMetrics().keySet())
        {
            System.out.println(key + " -> " + Metrics.getMetrics().get(key));
        }


        System.out.println("\r\nTest migrator:");
        DBMap projectDbMap = DBMap.dbMaps.get(1);
        projectDbMap.tableName = projectDbMap.tableName + "_TEST";
        SQLMigrator.migrate(Arrays.asList(projectDbMap));

        System.out.println("\r\nAdd some rows:");
        projects = Project.getAll();
        if (projects.size() == 0)
        {
            Project project = new Project();
            project.setName("Genesis");
            project.setPrefix("GS");
            long newId = EOI.insert(project, auditUser);
        }
        for (Object result : EOI.executeQuery("select * from projects_test"))
            System.out.println(result);

        printAllProjects();

        System.out.println("done");
        EOI.destroy();
    }

    private static void printAllProjects()
    {
        for (Project project : Project.getAll())
        {
            for (int i = 0; i < 5; i++)
            {
                if (i == 0) System.out.print(String.format("%-5s", project.getId()));
                if (i == 1) System.out.print(String.format("%-20s", project.getName()));
                if (i == 2) System.out.print(String.format("%-5s", project.getPrefix()));
                if (i == 3) System.out.print(String.format("%-30s", project.getInceptDate()));
                if (i == 4) System.out.print(String.format("%-30s", project.getLastUpdatedOn()));
            }
            System.out.println();
        }
    }

    private static void createTables()
    {
        long subTaskStart;
        subTaskStart = System.currentTimeMillis();
        for (DBMap dbMap : DBMap.dbMaps)
            if (!EOI.isTableExists(dbMap.tableName))
            {
                System.out.println("Creating table " + dbMap.tableName + "...");
                String createTableStatement = SQLGenerator.getCreateTableStatement(dbMap);
                System.out.println(createTableStatement);
                EOI.executeUpdate(createTableStatement);
            }
        System.out.println("Made sure all tables exist (creating if necessary) in " + (System.currentTimeMillis() - subTaskStart) + "ms");
    }

    private static void dropTables()
    {
        long subTaskStart;
        subTaskStart = System.currentTimeMillis();
        for (DBMap dbMap : DBMap.dbMaps)
        {
            if (EOI.isTableExists(dbMap.tableName))
            {
                System.out.println("Dropping " + dbMap.tableName + "...");
                EOI.executeUpdate("drop table " + dbMap.tableName);
            }
        }
        System.out.println("Dropped existing tables in " + (System.currentTimeMillis() - subTaskStart) + "ms");
    }
}
