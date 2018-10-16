package net.ehicks.eoi;

import net.ehicks.eoi.diagnostic.Project;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;

class Diagnostic
{
    private static final Logger log = LoggerFactory.getLogger(Diagnostic.class);

    @BeforeAll
    static void init() throws IOException
    {
        ConnectionInfo connectionInfo;
        if (false)
            connectionInfo = new ConnectionInfo(ConnectionInfo.DbMode.POSTGRESQL.toString(), "localhost", "5432", "eoi",
                "postgres", "password", "", "", "");
        else
            connectionInfo = new ConnectionInfo(ConnectionInfo.DbMode.H2_MEM.toString(), "", "", "test",
                    "", "", "2097152", "", "");
        
        EOI.init(connectionInfo);
        EOI.setSlowQueryThreshold(1);

        DBMap.loadDbMaps(new File("src/main/java/net/ehicks/eoi/diagnostic").getCanonicalPath(), "net.ehicks.eoi.diagnostic");
        dropTables();
        createTables();
    }

    @AfterAll
    static void destroy()
    {
        EOI.destroy();
    }

    @Test
    void mainDiagnostic() throws IOException
    {
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

        List<Project> projects = Project.getAll();
        if (projects.size() == 0)
        {
            Project project = new Project();
            project.setName("Genesis");
            project.setPrefix("GS");
            long newId = EOI.insert(project, auditUser);

            project = Project.getById(newId);
            log.info("Added a project: {}", projectToString(project));

            project = new Project();
            project.setName("SchoolFI");
            project.setPrefix("SF");
            project.setInceptDate(new Date());
            newId = EOI.insert(project, auditUser);

            project = Project.getById(newId);
            log.info("Added a project: {}", projectToString(project));
        }

        for (Project project : Project.getAll())
        {
            if (project.getId() == 1)
            {
                project.setName("World Peace");
                project.setPrefix("WP");
                project.setInceptDate(new Date());
                EOI.update(project, auditUser);
                log.info("Updated a project: {}", projectToString(project));
            }
            if (project.getId() == 2)
            {
                project.setName("Write a Book");
                project.setPrefix("WB");
                project.setInceptDate(null);
                EOI.update(project, auditUser);
                log.info("Updated a project: {}", projectToString(project));
            }
        }

        for (Project project : Project.getAll())
        {
            EOI.executeDelete(project, auditUser);
            log.info("Deleted a project: {}", projectToString(project));
        }

        EOI.executeQuery("select * from projects where id in (?,?,?,?,?)", Arrays.asList(1,2,3,4,5));

        log.info("Test migrator:");
        DBMap projectDbMap = DBMap.dbMaps.get(1);
        projectDbMap.tableName = projectDbMap.tableName + "_TEST";
        SQLMigrator.migrate(Collections.singletonList(projectDbMap));

        log.info("Add some rows:");
        projects = Project.getAll();
        if (projects.size() == 0)
        {
            Project project = new Project();
            project.setName("Genesis");
            project.setPrefix("GS");
            EOI.insert(project, auditUser);
            log.info("We just added a project: {}", projectToString(project));
        }
        for (Object result : EOI.executeQuery("select * from projects_test"))
            log.info("result: {}", result);
    }

    @Test
    void metricsDiagnostic()
    {
        Map<String, String> metrics = Metrics.getMetrics();

        for (String key : metrics.keySet())
            log.info(key + " -> " + metrics.get(key));
    }

    private static String projectToString(Project project)
    {
        return project.getId() + " " + project.getName() + " " + project.getPrefix() + " " + project.getInceptDate() + " " + project.getLastUpdatedOn();
    }

    private static void createTables()
    {
        long subTaskStart;
        subTaskStart = System.currentTimeMillis();
        for (DBMap dbMap : DBMap.dbMaps)
            if (!EOI.isTableExists(dbMap.tableName))
            {
                log.info("Creating table " + dbMap.tableName + "...");
                String createTableStatement = SQLGenerator.getCreateTableStatement(dbMap);
                log.info(createTableStatement);
                EOI.executeUpdate(createTableStatement);
            }
        log.info("Made sure all tables exist (creating if necessary) in " + (System.currentTimeMillis() - subTaskStart) + "ms");
    }

    private static void dropTables()
    {
        long subTaskStart;
        subTaskStart = System.currentTimeMillis();
        for (DBMap dbMap : DBMap.dbMaps)
        {
            if (EOI.isTableExists(dbMap.tableName))
            {
                log.info("Dropping " + dbMap.tableName + "...");
                EOI.executeUpdate("drop table " + dbMap.tableName);
            }
        }
        log.info("Dropped existing tables in " + (System.currentTimeMillis() - subTaskStart) + "ms");
    }
}
