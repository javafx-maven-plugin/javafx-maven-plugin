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
import java.util.HashMap;
import java.util.Map;

/**
 * @goal build-jar
 * @phase package
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

    /**
     * <p>Set this to true if your app needs to break out of the standard web sandbox and do more powerful functions.</p>
     *
     * <p>If you are using FXML you will need to set this value to true.</p>
     *
     * @parameter default-value=false
     */
    protected boolean allPermissions;
    
    /**
     * Will be set when having goal "build-jar" within package-phase and calling "jfx:jar" or "jfx:native" from CLI. Internal usage only.
     * 
     * @parameter default-value=false
     */
    protected boolean jfxCallFromCLI;

    /**
     * To add custom manifest-entries, just add each entry/value-pair here.
     * 
     * @parameter
     */
    protected Map<String, String> manifestAttributes;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if( jfxCallFromCLI ){
            getLog().info("call from CLI - skipping creation of JavaFX JAR for application");
            return;
        }

        getLog().info("Building JavaFX JAR for application");

        Build build = project.getBuild();

        CreateJarParams createJarParams = new CreateJarParams();
        createJarParams.setOutdir(jfxAppOutputDir);
        createJarParams.setOutfile(jfxMainAppJarName);
        createJarParams.setApplicationClass(mainClass);
        createJarParams.setCss2bin(css2bin);
        createJarParams.setPreloader(preLoader);

        if( manifestAttributes == null ) {
            manifestAttributes = new HashMap<>();
        }
        createJarParams.setManifestAttrs(manifestAttributes);

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
        
        // https://docs.oracle.com/javase/8/docs/technotes/guides/deploy/manifest.html#JSDPG896
        if( allPermissions ){
            manifestAttributes.put("Permissions", "all-permissions");
        }

        try {
            getPackagerLib().packageAsJar(createJarParams);
        } catch (PackagerException e) {
            throw new MojoExecutionException("Unable to build JFX JAR for application", e);
        }
    }
}
