package com.github.dwclark.dbreset;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class DbResetPlugin implements Plugin<Project> {

    public static final String GROUP_NAME = 'Database Reset';

    public void apply(Project project) {
        project.configurations {
            dbReset
        }

        project.dependencies {
            dbReset(localGroovy())
            dbReset(group: 'com.github.dwclark', name: 'db-reset', version: '1.0.0');
        }

        PostgresqlConfigureTask.addTask(project);
    }
}