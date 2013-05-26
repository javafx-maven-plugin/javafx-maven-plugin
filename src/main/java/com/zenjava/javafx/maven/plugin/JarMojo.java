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

import com.sun.javafx.tools.packager.CreateJarParams;
import com.sun.javafx.tools.packager.PackagerException;
import com.sun.javafx.tools.packager.PackagerLib;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.model.Build;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * @goal jar
 * @phase package
 * @execute phase="package"
 * @requiresDependencyResolution
 */
public class JarMojo extends AbstractJfxToolsMojo {

    /**
     * @parameter expression="${mainClass}"
     * @required
     */
    protected String mainClass;

    /**
     * @parameter default-value=false
     */
    protected boolean css2bin;


    public void execute(PackagerLib packagerLib) throws MojoExecutionException, MojoFailureException {

        getLog().info("Building JavaFX JAR for application");

        Build build = project.getBuild();

        CreateJarParams createJarParams = new CreateJarParams();
        createJarParams.setOutdir(jfxAppOutputDir);
        createJarParams.setOutfile(jfxMainAppJarName);
        createJarParams.setApplicationClass(mainClass);
        createJarParams.setCss2bin(css2bin);
        createJarParams.addResource(new File(build.getOutputDirectory()), "");

        StringBuilder classpath = new StringBuilder();
        File libDir = new File(jfxAppOutputDir, "lib");
        if (!libDir.exists() && !libDir.mkdirs()) {
            throw new MojoExecutionException("Unable to create app lib dir: " + libDir);
        }

        try {
            for (Object object : project.getRuntimeClasspathElements()) {
                String path = (String) object;
                File file = new File(path);
                if (file.isFile()) {
                    getLog().info("Including classpath element: " + path);
                    File dest = new File(libDir, file.getName());
                    if (!dest.exists()) {
                        Files.copy(file.toPath(), dest.toPath());
                    }
                    classpath.append("lib/").append(file.getName()).append(" ");
                }
            }
        } catch (DependencyResolutionRequiredException e) {
            throw new MojoExecutionException("Error resolving application classpath to use for application", e);
        } catch (IOException e) {
            throw new MojoExecutionException("Error copying dependency for application", e);
        }
        createJarParams.setClasspath(classpath.toString());

        try {
            packagerLib.packageAsJar(createJarParams);
        } catch (PackagerException e) {
            throw new MojoExecutionException("Unable to build JFX JAR for application", e);
        }
    }
}
