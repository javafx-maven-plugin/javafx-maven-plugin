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

import com.zenjava.javafx.deploy.webstart.WebstartBundleConfig;
import com.zenjava.javafx.deploy.webstart.WebstartBundler;
import com.zenjava.javafx.maven.plugin.config.WebstartConfig;
import com.zenjava.javafx.maven.plugin.util.JfxToolsWrapper;
import com.zenjava.javafx.maven.plugin.util.MavenLog;
import org.apache.commons.io.FileUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Build;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.IOException;

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
     * @parameter expression="${webstart}"
     */
    private WebstartConfig webstartConfig;

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
        File outputDir = new File(build.getDirectory());

        getLog().info("Assembling JavaFX distributable to '" + outputDir + "'");

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
        JfxToolsWrapper jfxTools = new JfxToolsWrapper(jfxToolsJar, verbose);

        File jarFile = buildExecutableJar(outputDir, dependenciesDir, jfxTools);
        buildWebstartBundle(jarFile);
        buildNativeBundles(outputDir, jarFile, jfxTools);
    }

    protected File buildExecutableJar(File workingDir, File dependenciesDir, JfxToolsWrapper jfxTools)
            throws MojoFailureException, MojoExecutionException {

        Build build = project.getBuild();
        File classesDir = new File(build.getOutputDirectory());
        if (!classesDir.exists()) {
            throw new MojoFailureException("Build directory '" + classesDir + "' does not exist. You need to run the 'compile' phase first.");
        }

        String jarName = build.getFinalName() + "-jfx.jar";
        File outputFile = new File(workingDir, jarName);
        getLog().info("Packaging to JavaFX JAR file: " + outputFile);
        getLog().info("Using main class '" + mainClass + "'");
        jfxTools.packageAsJar(outputFile, classesDir, dependenciesDir, mainClass);

        return outputFile;
    }

    protected void buildWebstartBundle(File jarFile) throws MojoFailureException, MojoExecutionException {

        if (webstartConfig != null && webstartConfig.isBuildWebstartBundle()) {

            Build build = project.getBuild();

            WebstartBundleConfig config = new WebstartBundleConfig();

            String dirName = webstartConfig.getOutputDir() != null ? webstartConfig.getOutputDir() : "webstart";
            File outputDir = new File(build.getDirectory(), dirName);
            config.setOutputDir(outputDir);

            config.setJnlpFileName(webstartConfig.getJnlpFileName() != null ? webstartConfig.getJnlpFileName() : "launch.jnlp");

            config.setJnlpTemplate(webstartConfig.getJnlpTemplate());

            String title = webstartConfig.getTitle() != null ? webstartConfig.getTitle() : project.getName();
            if (title != null) {
                config.setTitle(title);
            } else {
                throw new MojoFailureException("A 'title' must be set to generate a webstart bundle");
            }

            String vendor = webstartConfig.getVendor();
            if (vendor == null) {
                if (project.getOrganization() != null && project.getOrganization().getName() != null) {
                    vendor = project.getOrganization().getName();
                } else {
                    throw new MojoFailureException("A 'vendor' must be set to generate a webstart bundle");
                }
            }
            config.setVendor(vendor);

            config.setDescription(webstartConfig.getDescription() != null ? webstartConfig.getDescription() : project.getDescription());

            String mainClass = webstartConfig.getMainClass();
            if (mainClass == null) {
                if (this.mainClass != null) {
                    mainClass = this.mainClass;
                } else {
                    throw new MojoFailureException("A 'mainClass' must be set to generate a webstart bundle");
                }
            }
            config.setMainClass(mainClass);

            config.setJarFile(jarFile.getName());

            if (webstartConfig.isBuildHtmlFile()) {
                config.setBuildHtmlFile(true);
                config.setHtmlFileName(webstartConfig.getHtmlFileName() != null ? webstartConfig.getHtmlFileName() : "index.html");
                config.setHtmlTemplate(webstartConfig.getHtmlTemplate());
            }

            WebstartBundler bundler = new WebstartBundler(new MavenLog(getLog()));
            bundler.bundle(config);

            try {
                FileUtils.copyFileToDirectory(jarFile, outputDir);
            } catch (IOException e) {
                throw new MojoExecutionException("Failed to copy JAR file into webstart directory", e);
            }
        }
    }

    protected void buildNativeBundles(File outputDir, File jarFile, JfxToolsWrapper jfxTools) throws MojoExecutionException {

        jfxTools.generateDeploymentPackages(outputDir, jarFile.getName(), bundleType,
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
