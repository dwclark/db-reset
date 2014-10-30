package com.github.dwclark.dbreset;

import org.gradle.api.Project;
import java.sql.*;
import groovy.sql.*;
import org.gradle.api.tasks.JavaExec;

public class PostgresqlExtension extends MapStringExtension {
    public static final String DRIVER_NAME = 'org.postgresql.Driver';
    public static final String EXTENSION_NAME = 'resetPostgresqlConfig';
    public static final String TASK_NAME = 'resetPostgresql';
    
    String url;
    String source;
    String target;
    Map connectionProperties = [:];

    public String toMapString() {
        Map map = [:];
        if(url) map.url = url;
        if(source) map.source = source;
        if(target) map.target = target;
        if(connectionProperties) map.connectionProperties = connectionProperties;
        return toMapString(map);
    }
}

public class PostgresqlConfigureTask {

    public static void addTask(Project project) {
        project.extensions.create(PostgresqlExtension.EXTENSION_NAME, PostgresqlExtension)
        project.task(PostgresqlExtension.TASK_NAME).with {
            group = DbResetPlugin.GROUP_NAME;
            description = ("Reset postgresql database. Specify url, source, and target in ${PostgresqlExtension.EXTENSION_NAME}. " +
                           "All other connection properties can be set through connectionProperties in ${PostgresqlExtension.EXTENSION_NAME}.");
            doLast {
                project.javaexec {
                    main = PostgresqlReset.name;
                    classpath(project.configurations.dbReset);
                    args project[PostgresqlExtension.EXTENSION_NAME].toMapString();
                }
            }
        }
    }
}

public class PostgresqlReset {

    PostgresqlExtension extension;
    List errors;

    public PostgresqlReset(PostgresqlExtension extension) {
        this.extension = extension;
    }

    public static Class loadDriverClass() {
        try {
            return Class.forName(PostgresqlExtension.DRIVER_NAME);
        }
        catch(ClassNotFoundException cnfe) {
            return null;
        }
    }

    public Driver getDriver() {
        Class c = loadDriverClass();
        if(c) {
            return c.newInstance();
        }
        else {
            return null;
        }
    }

    public void reset() {
        try {
            gsql.execute(dropSql);
            gsql.execute(createSql);
        }
        finally {
            gsql.close();
        }
    }
    
    public void checkConfiguration() {
        errors = [];
        if(!driver) {
            errors += "You need to add the postgresql driver to the dbReset dependency set";
        }

        if(!extension.url) {
            errors += "You need to set the url property in the ${PostgresqlExtension.EXTENSION_NAME} task";
        }

        if(!extension.source) {
            errors += "You need to set the source database using the source property in the ${PostgresqlExtension.EXTENSION_NAME} task";
        }

        if(!extension.target) {
            errors += "You need to set the target database using the target property in the ${PostgresqlExtension.EXTENSION_NAME} task";
        }
    }

    @Lazy Connection connection = driver.connect(extension.url, extension.connectionProperties as Properties);
    @Lazy Sql gsql = new Sql(connection);

    public String getDropSql() {
        return "drop database if exists ${extension.target}".toString();
    }

    public String getCreateSql() {
        return "create database ${extension.target} template ${extension.source}".toString();
    }

    public static void main(String[] args) {
        try {
            PostgresqlExtension ext = new PostgresqlExtension();
            ext.fromMapString(args[0]);
            PostgresqlReset p = new PostgresqlReset(ext);
            p.checkConfiguration();
            if(p.errors) {
                p.errors.each { String error -> System.err.println(error); };
                System.exit(-1);
            }
            else {
                p.reset();
            }
        }
        catch(Throwable t) {
            t.printStackTrace();
            throw t;
        }
    }
}
