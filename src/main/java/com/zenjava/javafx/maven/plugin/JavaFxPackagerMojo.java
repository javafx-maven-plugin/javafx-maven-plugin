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
import org.apache.maven.model.Build;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;

import java.io.File;

import static org.twdata.maven.mojoexecutor.MojoExecutor.*;

/**
 * @goal package
 * @phase package
 * @requiresDependencyResolution
 */
public class JavaFxPackagerMojo extends AbstractMojo {

    /**
     * @parameter expression="${java.home}"
     */
    private String javaHome;

    /**
     * @parameter expression="${mainClass}"
     * @required
     */
    private String mainClass;

    /**
     * @parameter expression="${bundleType}" default-value="NONE"
     */
    private String bundleType;

    /**
     * @parameter expression="${verbose}" default-value="false"
     */
    private Boolean verbose;


    /**
     * The Maven Project Object
     *
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    protected MavenProject project;

    /**
     * The Maven Session Object
     *
     * @parameter expression="${session}"
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


    public void execute() throws MojoExecutionException, MojoFailureException {

        Build build = project.getBuild();
        File javafxBuildDir = new File(build.getDirectory(), "javafx");

        getLog().info("Assembling JavaFX distributable to '" + javafxBuildDir + "'");

        // unpack project dependencies into a directory the JavaFX tools can use

        File dependenciesDir = unpackDependencies();


        // find the JavaFX tools library from within the JDK

        getLog().info("Java home is: " + javaHome);
        File javaHomeDir = new File(javaHome);
        File jdkHomeDir = javaHomeDir.getParentFile();
        File jfxToolsJar = new File(jdkHomeDir, "lib/ant-javafx.jar");

        if (!jfxToolsJar.exists()) {
            throw new MojoFailureException("Unable to find JavaFX tools JAR file at '"
                    + jfxToolsJar + "'. Is your JAVA_HOME set to a JDK with JavaFX installed (must be Java 1.7.0 update 9 or higher)?");
        }

        JfxToolsWrapper jfxToolsWrapper = new JfxToolsWrapper(jfxToolsJar, verbose);


        // build the JavaFX executable JAR

        File classesDir = new File(build.getOutputDirectory());
        if (!classesDir.exists()) {
            throw new MojoFailureException("Build directory '" + classesDir + "' does not exist. You need to run the 'compile' phase first.");
        }

        String jarName = build.getFinalName() + "-jfx.jar";
        File outputFile = new File(javafxBuildDir, jarName);
        getLog().info("Packaging to JavaFX JAR file: " + outputFile);
        getLog().info("Using main class '" + mainClass + "'");
        jfxToolsWrapper.packageAsJar(outputFile, classesDir, dependenciesDir, mainClass);


        // build native packages (if required)

        jfxToolsWrapper.generateDeploymentPackages(javafxBuildDir, jarName, bundleType,
                project.getBuild().getFinalName(),
                project.getName(),
                project.getVersion(),
                project.getOrganization() != null ? project.getOrganization().getName() : "Unknown JavaFX Developer",
                mainClass);
    }


    protected File unpackDependencies() throws MojoExecutionException, MojoFailureException {

        String baseDir = project.getBuild().getDirectory();
        String subDir = "jfx-dependencies";
        File targetDir = new File(baseDir, subDir);

        getLog().info("Unpacking module dependendencies to: " + targetDir);

        executeMojo(
                plugin(
                        groupId("org.apache.maven.plugins"),
                        artifactId("maven-dependency-plugin"),
                        version("2.0")
                ),
                goal("unpack-dependencies"),
                configuration(
                        element(name("outputDirectory"), "${project.build.directory}/" + subDir)
                ),
                executionEnvironment(
                        project,
                        session,
                        pluginManager
                )
        );

        return targetDir;
    }
}
