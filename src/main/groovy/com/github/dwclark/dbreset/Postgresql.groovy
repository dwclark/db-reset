package com.github.dwclark.dbreset;

import org.gradle.api.Project;
import org.gradle.api.Task;
import java.sql.*;
import groovy.sql.*;

public class PostgresqlExtension {
    String url;
    String source;
    String target;
    Properties connectionProperties = new Properties();
}

public class Postgresql {

    public static final String DRIVER_NAME = 'org.postgresql.Driver';
    public static final String EXTENSION_NAME = 'postgresqlConfig';
    public static final String TASK_NAME = 'resetPostgres';

    public static boolean isDriverPresent() {
        try {
            Class.forName(DRIVER_NAME);
            return true;
        }
        catch(ClassNotFoundException cnfe) {
            return false;
        }
    }

    public static Task configure(Project project) {
        if(!isDriverPresent()) {
            return null;
        }

        project.extensions.create(EXTENSION_NAME, PostgresqlExtension)
        Task task = project.task(TASK_NAME);
        task.with {
            group = DbResetPlugin.GROUP;
            description = ("Reset postgresql database. Specify url, source, and target in ${EXTENSION_NAME}. " +
                           "All other connection properties can be set through connectionProperties in ${EXTENSION_NAME}.");
            doLast { new Postgresql(project).run(); } };
    }

    public Postgresql(Project project) {
        this.project = project;
    }

    Project project;

    public void run() {
        checkConfiguration();
        Sql gsql = new Sql(connection);
        try {
            gsql.execute(dropSql);
            gsql.execute(createSql);
        }
        finally {
            gsql.close();
        }
    }
    
    public void checkConfiguration() {
        List errors = [];
        PostgresqlExtension ext = project[EXTENSION_NAME];
        if(!ext.url) {
            errors += "You need to set the url property in the ${EXTENSION_NAME} task";
        }

        if(!ext.source) {
            errors += "You need to set the source database using the source property in the ${EXTENSION_NAME} task";
        }

        if(!ext.target) {
            errors += "You need to set the target database using the target property in the ${EXTENSION_NAME} task";
        }
    }

    public PostgresqlExtension getExtension() {
        return project[EXTENSION_NAME];
    }

    @Lazy Connection connection = DriverManager.getConnection(extension.url, extension.connectionProperties);

    public String getDropSql() {
        return "drop database if exists ${extension.target}".toString();
    }

    public String getCreateSql() {
        return "create database ${extension.target} template ${extension.source}".toString();
    }
}
