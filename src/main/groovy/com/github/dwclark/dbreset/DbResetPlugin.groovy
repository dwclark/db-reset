package com.github.dwclark.dbreset;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class DbResetPlugin implements Plugin<Project> {

    public static final String GROUP_NAME = 'Database Reset';

    public void addGlobalDependencies(Project project) {
        project.apply(plugin: 'application');
        project.apply(plugin: 'groovy');
    }

    public void apply(Project project) {
        addGlobalDependencies(project);
        
        if(Postgresql.configure(project)) {
            return;
        }
    }
}