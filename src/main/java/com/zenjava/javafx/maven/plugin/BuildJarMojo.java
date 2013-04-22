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

import com.zenjava.javafx.maven.plugin.util.JfxToolsWrapper;
import org.apache.maven.model.Build;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import java.io.File;

import static org.twdata.maven.mojoexecutor.MojoExecutor.*;

/**
 * @goal build-jar
 * @phase package
 * @execute phase="compile"
 * @requiresDependencyResolution
 */
public class BuildJarMojo extends AbstractBundleMojo {

    /**
     * @parameter
     */
    protected String executableJarFileName;
    
    /**
     * Exclude files when unpacking module dependencies
     * 
     * @parameter default-value=""
     */
    protected String excludes;

    /**
     * Compile CSS to binary format
     *
     * @parameter default-value="true"
     */
    protected Boolean css2bin;

    public void execute() throws MojoExecutionException, MojoFailureException {

        Build build = project.getBuild();

        String jarName = executableJarFileName;
        if (jarName == null) {
            jarName = build.getFinalName() + "-jfx.jar";
        }
        File targetJarFile = new File(build.getDirectory(), jarName);
        getLog().info("Assembling JavaFX executable JAR to '" + targetJarFile + "'");

        File dependenciesDir = unpackDependencies();

        JfxToolsWrapper jfxTools = getJfxToolsWrapper();
        getLog().debug("Using main class '" + mainClass + "'");
        File classesDir = new File(build.getOutputDirectory());
        jfxTools.packageAsJar(targetJarFile, classesDir, dependenciesDir, mainClass, css2bin);
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
                        version("2.5.1")
                ),
                goal("unpack-dependencies"),
                configuration(
                        element(name("outputDirectory"), "${project.build.directory}/" + subDir),
                        element(name("excludes"), excludes)
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
