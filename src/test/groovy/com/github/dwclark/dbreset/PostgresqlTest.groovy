package com.github.dwclark.dbreset;

import java.sql.*;
import groovy.sql.*;
import spock.lang.*;
import org.gradle.api.InvalidUserDataException;

public class PostgresqlTest extends Specification {

    String dropDatabase = 'drop database if exists postgresqltest';
    String createDatabase = 'create database postgresqltest';
    String createTable = """create sequence testing_seq;

create table testing (
testing_id integer not null primary key default nextval('testing_seq'),
description varchar(100) not null
);""";
    String insertData = """insert into testing (description) values ('one');
insert into testing (description) values ('two');
insert into testing (description) values ('three');""";

    def postgresqlReset;

    public static Map connectionProperties() {
        println("Connection Properties: " + System.getProperty('db.connectionProperties'));
        if(System.getProperty('db.connectionProperties')) {
            return Eval.me(System.getProperty('db.connectionProperties'));
        }
        else {
            return [:];
        }
    }

    public static String baseUrl() {
        return System.getProperty('db.baseurl');
    }

    def source = 'postgresqltest';
    def target = 'postgresqlcopy';

    def setup() {
        def pgext = new PostgresqlExtension(url: baseUrl() + 'template1', source: source,
                                            target: target, connectionProperties: connectionProperties());

        postgresqlReset = new PostgresqlReset(pgext);
    }

    @IgnoreIf({ PostgresqlReset.loadDriverClass() == null || !System.getProperty('db.baseurl'); })
    def 'Test Create And Copy'() {
        setup:
        println("Trying to connect to template1");
        Sql gsqlInit = new Sql(DriverManager.getConnection(baseUrl() + 'template1', connectionProperties() as Properties));
        println("Connected");
        gsqlInit.execute(dropDatabase);
        println("Dropped Database");
        gsqlInit.execute(createDatabase);
        println("Created Database");
        gsqlInit.close();

        Sql gsqlMakeSchema = new Sql(DriverManager.getConnection(baseUrl() + source, connectionProperties() as Properties));
        gsqlMakeSchema.execute(createTable);
        gsqlMakeSchema.execute(insertData);
        gsqlMakeSchema.close();

        postgresqlReset.reset();

        Sql gsqlVerify = new Sql(DriverManager.getConnection(baseUrl() + target, connectionProperties() as Properties));
        
        expect:
        gsqlVerify.firstRow('select count(*) as c from testing')['c'] == 3;

        cleanup:
        gsqlVerify.close();
    }

    def 'Test Configuration Information'() {
        setup:
        PostgresqlExtension ext = new PostgresqlExtension();
        PostgresqlReset p = new PostgresqlReset(ext);

        when:
        p.checkConfiguration();

        then:
        p.errors;

        when:
        ext.url = 'url';
        p.checkConfiguration();

        then:
        p.errors;

        when:
        ext.source = 'source';
        p.checkConfiguration();

        then:
        p.errors;

        when:
        ext.target = 'target';
        p.checkConfiguration();

        then:
        !p.errors;

    }
}