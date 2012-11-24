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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import java.io.File;

import static org.twdata.maven.mojoexecutor.MojoExecutor.*;

/**
 * @goal run
 * @phase package
 * @execute phase="compile"
 * @requiresDependencyResolution
 */
public class RunMojo extends AbstractBundleMojo {

    public void execute() throws MojoExecutionException, MojoFailureException {

        String baseDir = project.getBuild().getDirectory();
        String subDir = "jfx-dependencies";
        File targetDir = new File(baseDir, subDir);

        getLog().info("Unpacking module dependendencies to: " + targetDir);

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
