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

import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.model.Build;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * <p>Builds an executable JAR for the project that has all the trappings needed to run as a JavaFX app. This will
 * include Pre-Launchers and all the other weird and wonderful things that the JavaFX packaging tools allow and/or
 * require.</p>
 *
 * <p>Any runtime dependencies for this project will be included in a separate 'lib' sub-directory alongside the
 * resulting JavaFX friendly JAR. The manifest within the JAR will have a reference to these libraries using the
 * relative 'lib' path so that you can copy the JAR and the lib directory exactly as is and distribute this bundle.</p>
 *
 * <p>The JAR and the 'lib' directory built by this Mojo are used as the inputs to the other distribution bundles. The
 * native and web Mojos for example, will trigger this Mojo first and then will copy the resulting JAR into their own
 * distribution bundles.</p>
 *
 * @goal jar
 * @phase package
 * @execute phase="package"
 * @requiresDependencyResolution
 */
public class JarMojo extends AbstractJfxToolsMojo {

    /**
     * Flag to switch on and off the compiling of CSS files to the binary format. In theory this has some minor
     * performance gains, but it's debatable whether you will notice them, and some people have experienced problems
     * with the resulting compiled files. Use at your own risk. By default this is false and CSS files are left in their
     * plain text format as they are found.
     *
     * @parameter default-value=false
     */
    protected boolean css2bin;

    /**
     * A custom class that can act as a Pre-Loader for your app. The Pre-Loader is run before anything else and is
     * useful for showing splash screens or similar 'progress' style windows. For more information on Pre-Loaders, see
     * the official JavaFX packaging documentation.
     *
     * @parameter
     */
    protected String preLoader;

    /**
     * Flag to switch on updating the existing jar created with maven. The jar to be updated is taken from 
     * '${project.basedir}/target/${project.build.finalName}.jar'.
     *
     * @parameter default-value=false
     */
    protected boolean updateExistingJar;

    public void execute() throws MojoExecutionException, MojoFailureException {

        getLog().info("Building JavaFX JAR for application");

        Build build = project.getBuild();

        CreateJarParams createJarParams = new CreateJarParams();
        createJarParams.setOutdir(jfxAppOutputDir);
        createJarParams.setOutfile(jfxMainAppJarName);
        createJarParams.setApplicationClass(mainClass);
        createJarParams.setCss2bin(css2bin);
        createJarParams.setPreloader(preLoader);
        StringBuilder classpath = new StringBuilder();
        File libDir = new File(jfxAppOutputDir, "lib");
        if (!libDir.exists() && !libDir.mkdirs()) {
            throw new MojoExecutionException("Unable to create app lib dir: " + libDir);
        }

        if (updateExistingJar) {
            createJarParams.addResource(null, new File(build.getDirectory() + File.separator + build.getFinalName() + ".jar"));
        } else {
            createJarParams.addResource(new File(build.getOutputDirectory()), "");
        }

        try {
            for (Object object : project.getRuntimeClasspathElements()) {
                String path = (String) object;
                File file = new File(path);
                if (file.isFile()) {
                    getLog().debug("Including classpath element: " + path);
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
            getPackagerLib().packageAsJar(createJarParams);
        } catch (PackagerException e) {
            throw new MojoExecutionException("Unable to build JFX JAR for application", e);
        }
    }
}
