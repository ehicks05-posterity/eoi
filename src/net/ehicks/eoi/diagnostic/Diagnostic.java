package net.ehicks.eoi.diagnostic;

import net.ehicks.eoi.DBMap;
import net.ehicks.eoi.EOI;
import net.ehicks.eoi.SQLGenerator;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;

public class Diagnostic
{
    public static void main(String[] args) throws IOException
    {
        EOI.init("jdbc:h2:mem:test");

        DBMap.loadDbMaps(new File("src/net/ehicks/eoi/beans").getCanonicalPath(), "net.ehicks.eoi.beans");

//        dropTables();

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
            project.setInceptDate(new Date());
            newId = EOI.insert(project);

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
                EOI.update(project);
            }
            if (project.getId() == 2)
            {
                project.setName("Write a Book");
                project.setPrefix("WB");
                project.setInceptDate(null);
                EOI.update(project);
            }

            System.out.println("We just updated: " + project);
        }

        for (Project project : Project.getAll())
        {
            EOI.executeDelete(project);
            System.out.println("We just deleted: " + project);
        }

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
