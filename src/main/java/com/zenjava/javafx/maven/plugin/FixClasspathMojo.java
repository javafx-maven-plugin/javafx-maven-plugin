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

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * @goal fix-classpath
 * @phase validate
 * @requiresProject false
 */
public class FixClasspathMojo extends AbstractMojo {

    /**
     * @parameter expression="${java.home}"
     */
    private String javaHome;

    /**
     * @parameter expression="${silentJfxFix}"
     */
    private boolean silentJfxFix;

    public void execute() throws MojoExecutionException, MojoFailureException {

        if (!silentJfxFix && !confirm(javaHome)) {
            getLog().info("NOT updating JRE classpath (user chose not to continue)");
            return;
        }

        getLog().warn("Fixing JRE bootclasspath to include JavaFX runtime and native files");
        getLog().warn("All applications using JRE will be affected: "+ javaHome);

        File jreHomeDir = new File(javaHome);

        // find runtime JAR
        File jfxRuntimeJar = new File(jreHomeDir, "lib/jfxrt.jar");
        if (!jfxRuntimeJar.exists()) {
            throw new MojoExecutionException("Unable to find JavaFX Runtime JAR file at '" + jfxRuntimeJar
                    + "'. Is your JAVA_HOME set to a JDK with JavaFX installed (must be Java 1.7.0 update 9 or higher)?");
        }

        // find extensions directory
        File jreExtDir = new File(jreHomeDir, "lib/ext");
        if (!jreExtDir.exists()) {
            throw new MojoExecutionException("Unable to find JRE extensions directory at '" + jreExtDir
                    + "'. This plugin has only been tested on windows, if you are running on another OS please"
                    + " raise an issue at https://github.com/zonski/javafx-maven-plugin");
        }

        File targetFile = new File(jreExtDir, jfxRuntimeJar.getName());
        if (!targetFile.exists()) {
            try {
                getLog().info("Copying JFX Runtime JAR '" + jfxRuntimeJar + "' to '" + jreExtDir +"'");
                Files.copy(jfxRuntimeJar.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                throw new MojoExecutionException("Error while copying JFX Runtime JAR '" + jfxRuntimeJar
                        + "' to '" + jreExtDir +"': " + e, e);
            }
        } else {
            getLog().info("JFX Runtime JAR already exists in the JRE extensions directory, no action was taken (" + targetFile + ")");
        }
    }

    private boolean confirm(String javaHome) throws MojoExecutionException {

        getLog().warn("\n\n"
                + "\nThis action will change the runtime classpath for your JRE installed at '" + javaHome
                + "' to include JavaFX. All Java applications (not just this one) using this JRE will have"
                + " the JavaFX runtime include on their path. Generally this is pretty safe but if you are"
                + " unsure how your applications may be affected you should not do this."
                + "\n\nIf you want to run this command without seeing this warning use -DsilentJfxFix=true"
                + "\n\nAre you sure you want to continue? (y/n)");

        BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
        try {
            String choice = input.readLine();
            return "y".equalsIgnoreCase(choice);
        } catch (IOException e) {
            throw new MojoExecutionException("Error confirming JRE classpath update with user", e);
        }
    }
}
