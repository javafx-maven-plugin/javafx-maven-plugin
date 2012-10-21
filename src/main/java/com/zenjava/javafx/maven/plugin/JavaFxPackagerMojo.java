package com.zenjava.javafx.maven.plugin;

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

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Build;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.twdata.maven.mojoexecutor.MojoExecutor.*;

/**
 * @goal package
 * @phase package
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
        unpackDependencies();


        // find the JavaFX tools library from within the JDK

        getLog().info("Java home is: " + javaHome);
        File javaHomeDir = new File(javaHome);
        File jdkHomeDir = javaHomeDir.getParentFile();
        File jfxToolsJar = new File(jdkHomeDir, "lib/ant-javafx.jar");

        if (!jfxToolsJar.exists()) {
            throw new MojoFailureException("Unable to find JavaFX tools JAR file at '"
                    + jfxToolsJar + "'. Is your JAVA_HOME set to a JDK with JavaFX installed?");
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
        jfxToolsWrapper.packageAsJar(outputFile, classesDir, mainClass);


        // build native packages (if required)

        jfxToolsWrapper.generateDeploymentPackages(javafxBuildDir, jarName, bundleType,
                project.getBuild().getFinalName(), mainClass);
    }

    protected void unpackDependencies() throws MojoExecutionException, MojoFailureException {

        String baseDir = project.getBuild().getDirectory();
        String subDir = "jfx-dependencies";

        getLog().info("Unpacking module dependendencies to '" + baseDir + "/" + subDir + "'");

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
    }

    protected void unpackDependenciesx() throws MojoExecutionException, MojoFailureException {

        List<Dependency> dependencies = project.getDependencies();
        List<Element> artifactItemsList = new ArrayList<Element>();

        for (Dependency dependency : dependencies) {
            if (!"test".equals(dependency.getScope()) && !"provided".equals(dependency.getScope())) {

                getLog().info("Including dependency '" + dependency + "' in JavaFX bundle");
                artifactItemsList.add(
                        element("artifactItem",
                                element("groupId", dependency.getGroupId()),
                                element("artifactId", dependency.getArtifactId()),
                                element("version", dependency.getVersion())
                        )
                );

            }
        }

        Element[] artifactItems = new Element[artifactItemsList.size()];
        artifactItemsList.toArray(artifactItems);

        executeMojo(
                plugin(
                        groupId("org.apache.maven.plugins"),
                        artifactId("maven-dependency-plugin"),
                        version("2.0")
                ),
                goal("unpack"),
                configuration(
                        element(name("outputDirectory"), "${project.build.directory}/jfx-dependencies"),
                        element(name("artifactItems"), artifactItems)
                ),
                executionEnvironment(
                        project,
                        session,
                        pluginManager
                )
        );
    }

}
