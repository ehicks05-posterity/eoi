package net.ehicks.eoi;

import net.ehicks.eoi.beans.Project;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class Diagnostic
{
    public static void main(String[] args) throws IOException
    {
        EOI.init("jdbc:h2:mem:test");

        DBMap.loadDbMaps(new File("src/net/ehicks/eoi/beans").getCanonicalPath(), "net.ehicks.eoi.beans");

        dropTables();

        createTables();

        List<Project> projects = Project.getAll();
        if (projects.size() == 0)
        {
            Project project = new Project();
            project.setName("Genesis");
            project.setPrefix("GS");
            long newId = EOI.insert(project);

            project = Project.getById(newId);
            System.out.println("We just created: " + project);

            project = new Project();
            project.setName("SchoolFI");
            project.setPrefix("SF");
            newId = EOI.insert(project);

            project = Project.getById(newId);
            System.out.println("We just created: " + project);
        }
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
