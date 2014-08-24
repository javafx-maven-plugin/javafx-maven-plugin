/*
 * Copyright 2012 Daniel Zwolenski.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.zenjava.javafx.maven.plugin;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;

import static org.twdata.maven.mojoexecutor.MojoExecutor.*;

/**
 * A convenience class for running the JavaFX application defined by the POM. This is really just a wrapper around the
 * standard Maven exec command, however it pulls the mainClass from the JavaFX plugin configuration so you don't have to
 * respecify it.
 *
 * @goal run
 * @phase package
 * @execute phase="compile"
 * @requiresDependencyResolution
 */
public class RunMojo extends AbstractMojo {

    /**
     * The Maven Project Object
     *
     * @parameter property="project"
     * @required
     * @readonly
     */
    protected MavenProject project;

    /**
     * The Maven Session Object
     *
     * @parameter property="session"
     * @required
     * @readonly
     */
    protected MavenSession session;

    /**
     * The Maven PluginManager Object
     *
     * @component
     * @required
     */
    protected BuildPluginManager pluginManager;

    /**
     * The main JavaFX application class that acts as the entry point to the JavaFX application.
     *
     * @parameter property="mainClass"
     * @required
     */
    protected String mainClass;

    public void execute() throws MojoExecutionException, MojoFailureException {

        getLog().info("Running JavaFX Application");

        executeMojo(
                plugin(
                        groupId("org.codehaus.mojo"),
                        artifactId("exec-maven-plugin"),
                        version("1.2.1")
                ),
                goal("java"),
                configuration(
                        element(name("mainClass"), mainClass)
                ),
                executionEnvironment(
                        project,
                        session,
                        pluginManager
                )
        );
    }
}
