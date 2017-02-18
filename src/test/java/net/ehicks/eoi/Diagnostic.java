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
            runDiagnostic();
        }
        catch (Exception e)
        {
            System.out.println(e.getMessage());
        }
    }

    private static void runDiagnostic() throws IOException
    {
//        EOI.init("jdbc:h2:mem:test;CACHE_SIZE=2097152;DB_CLOSE_ON_EXIT=FALSE");
        EOI.init("jdbc:sqlserver://localhost\\SQLEXPRESS:1433;***REMOVED***");

        DBMap.loadDbMaps(new File("src/main/java/net/ehicks/eoi/diagnostic").getCanonicalPath(), "net.ehicks.eoi.diagnostic");

        dropTables();

        createTables();

        List<Project> projects = Project.getAll();
        if (projects.size() == 0)
        {
            Project project = new Project();
            project.setName("Genesis");
            project.setPrefix("GS");
            long newId = EOI.insert(project, null);

            project = Project.getById(newId);
            System.out.println("We just created: " + project);

            project = new Project();
            project.setName("SchoolFI");
            project.setPrefix("SF");
            project.setInceptDate(new Date());
            newId = EOI.insert(project, null);

            project = Project.getById(newId);
            System.out.println("We just created: " + project);
        }

        for (Project project : Project.getAll())
        {
            if (project.getId() == 1)
            {
                project.setName("World Peace");
                project.setPrefix("WP");
                project.setInceptDate(new Date());
                EOI.update(project, null);
            }
            if (project.getId() == 2)
            {
                project.setName("Write a Book");
                project.setPrefix("WB");
                project.setInceptDate(null);
                EOI.update(project, null);
            }

            System.out.println("We just updated: " + project);
        }

        for (Project project : Project.getAll())
        {
            for (int i = 0; i < 5; i++)
            {
                if (i == 0) System.out.print(String.format("%-30s", project.getId()));
                if (i == 1) System.out.print(String.format("%-30s", project.getName()));
                if (i == 2) System.out.print(String.format("%-30s", project.getPrefix()));
                if (i == 3) System.out.print(String.format("%-30s", project.getInceptDate()));
                if (i == 4) System.out.print(String.format("%-30s", project.getLastUpdatedOn()));
            }
            System.out.println();
        }

        for (Project project : Project.getAll())
        {
            EOI.executeDelete(project, null);
            System.out.println("We just deleted: " + project);
        }

        EOI.executeQuery("select * from projects where id in (?,?,?,?,?)", Arrays.asList(1,2,3,4,5));

        System.out.println("done");
        EOI.destroy();
    }

    private static void createTables()
    {
        long subTaskStart;
        subTaskStart = System.currentTimeMillis();
        for (DBMap dbMap : DBMap.dbMaps)
            if (!EOI.isTableExists(dbMap))
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
            System.out.print("Dropping " + dbMap.tableName + "...");
            System.out.println(EOI.executeUpdate("drop table " + dbMap.tableName));
        }
        System.out.println("Dropped existing tables in " + (System.currentTimeMillis() - subTaskStart) + "ms");
    }
}
