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

    def project;
    def plugin;

    public static Properties connectionProperties() {
        if(System.getProperty('db.connectionProperties')) {
            return Eval.me(System.getProperty('db.connectionProperties')) as Properties;
        }
        else {
            return new Properties();
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

        project = [ (PostgresqlExtension.EXTENSION_NAME): pgext ];
        plugin = new Postgresql(project);
    }

    @IgnoreIf({ !Postgresql.isDriverPresent() || !System.getProperty('db.baseurl'); })
    def 'Test Create And Copy'() {
        setup:
        Sql gsqlInit = new Sql(DriverManager.getConnection(baseUrl() + 'template1', connectionProperties()));
        gsqlInit.execute(dropDatabase);
        gsqlInit.execute(createDatabase);
        gsqlInit.close();

        Sql gsqlMakeSchema = new Sql(DriverManager.getConnection(baseUrl() + source, connectionProperties()));
        gsqlMakeSchema.execute(createTable);
        gsqlMakeSchema.execute(insertData);
        gsqlMakeSchema.close();

        plugin.run();

        Sql gsqlVerify = new Sql(DriverManager.getConnection(baseUrl() + target, connectionProperties()));
        
        expect:
        gsqlVerify.firstRow('select count(*) as c from testing')['c'] == 3;

        cleanup:
        gsqlVerify.close();
    }

    def 'Test Configuration Information'() {
        setup:
        PostgresqlExtension ext = new PostgresqlExtension();
        def project = [ (PostgresqlExtension.EXTENSION_NAME): ext ];
        Postgresql p = new Postgresql(project);

        when:
        p.checkConfiguration();

        then:
        thrown(InvalidUserDataException);

        when:
        ext.url = 'url';
        p.checkConfiguration();

        then:
        thrown(InvalidUserDataException);

        when:
        ext.source = 'source';
        p.checkConfiguration();

        then:
        thrown(InvalidUserDataException);

        when:
        ext.target = 'target';
        p.checkConfiguration();

        then:
        notThrown(InvalidUserDataException);

    }
}