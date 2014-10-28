package com.github.dwclark.dbreset;

import java.sql.*;
import groovy.sql.*;
import spock.lang.*;

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
    def connectionProperties = [ user: 'ciuser', password: 'ciuser' ] as Properties;
    def source = 'postgresqltest';
    def target = 'postgresqlcopy';

    def setup() {
        println("In setup()");
        def pgext = new PostgresqlExtension(url: 'jdbc:postgresql:template1', source: source,
                                            target: target, connectionProperties: connectionProperties);

        project = [ (Postgresql.EXTENSION_NAME): pgext ];
        plugin = new Postgresql(project);
    }

    def 'Test Create And Copy'() {
        setup:
        Sql gsqlInit = new Sql(DriverManager.getConnection('jdbc:postgresql:template1', connectionProperties));
        gsqlInit.execute(dropDatabase);
        gsqlInit.execute(createDatabase);
        gsqlInit.close();

        Sql gsqlMakeSchema = new Sql(DriverManager.getConnection('jdbc:postgresql:' + source, connectionProperties));
        gsqlMakeSchema.execute(createTable);
        gsqlMakeSchema.execute(insertData);
        gsqlMakeSchema.close();

        plugin.run();

        Sql gsqlVerify = new Sql(DriverManager.getConnection('jdbc:postgresql:' + target, connectionProperties));
        
        expect:
        gsqlVerify.firstRow('select count(*) as c from testing')['c'] == 3;

        cleanup:
        gsqlVerify.close();
    }
}