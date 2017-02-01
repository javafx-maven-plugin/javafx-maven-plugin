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

import com.oracle.tools.packager.Log;
import com.sun.javafx.tools.packager.PackagerLib;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * Base Mojo that any other Mojo wanting to access the JavaFX Packager tools should extend from. This provides convenience methods for accessing the JavaFX Packager tools in a standard and simple way.
 */
public abstract class AbstractJfxToolsMojo extends AbstractMojo {

    /**
     * The Maven Project Object
     *
     * @parameter property="project"
     * @required
     * @readonly
     */
    protected MavenProject project;

    /**
     * Flag to turn on verbose logging. Set this to true if you are having problems and want more detailed information.
     *
     * @parameter property="verbose" default-value="false"
     */
    protected Boolean verbose;

    /**
     * The main JavaFX application class that acts as the entry point to the JavaFX application.
     *
     * @parameter property="mainClass"
     * @required
     */
    protected String mainClass;

    /**
     * The 'app' output directory. This is where the base executable JavaFX jar is built into, along with any dependent libraries (place in the 'lib' sub-directory). The resulting JAR in this
     * directory will be ready for distribution, including Pre-Loaders, signing, etc. This JAR will also be the one bundled into the other distribution bundles (i.e. web or native) if you run the
     * relevant commands for that.
     * <p>
     * This defaults to 'target/jfx/app' and in most cases there is no real need to change this.
     *
     * @parameter property="jfxAppOutputDir" default-value="${project.build.directory}/jfx/app"
     */
    protected File jfxAppOutputDir;

    /**
     * The 'bin' directory. This is a relative path to the jfxAppOutputDir property. This is the directory the executable JavaFX jar is built into and is the parent of the lib directory.
     * <p>
     * This defaults to '' so the executable JavaFX jar and the lib directory end up at jfxAppOutputDir.
     *
     * @parameter property="jfxBinDir" default-value=""
     * @since 8.8.0
     */
    private String jfxBinDir;

    /**
     * The 'lib' directory. This is a relative path to the jfxBinDir property. This is the directory the executable JavaFX jar is built into and is the parent of the lib directory.
     * <p>
     * This defaults to 'lib'. If jfxAppOutputDir and jfxBinDir keep their defaults the resolved default lib path would become 'target/jfx/app/lib'. If e.g. jfxBinDir is set to 'bin' then the resolved
     * lib path would become 'target/jfx/app/bin/lib'.
     * <p>
     * All JAR-files in the lib directory will be added to the Manifest Class-Path.
     *
     * @parameter property="jfxLibDir" default-value="lib"
     * @since 8.8.0
     */
    private String jfxLibDir;

    /**
     * The name of the JavaFX packaged JAR to be built into the 'app' directory. By default this will be the finalName as set in your project with a '-jfx' suffix. Change this if you want something
     * nicer. Note, that changing this value does not affect the regular old, non-JFX modified JAR (built in the 'target' directory).
     *
     * @parameter property="jfxMainAppJarName" default-value="${project.build.finalName}-jfx.jar"
     */
    protected String jfxMainAppJarName;

    /**
     * The directory contain deployment specific files, such as icons and splash screen images. This directory is added to the classpath of the Mojo when it runs, so that any files within this
     * directory are accessible to the JavaFX packaging tools.
     * <p>
     * This defaults to src/main/deploy and typically this is good enough. Just put your deployment specific files in this directory and they will be automatically picked up.
     * <p>
     * The most common usage for this is to provide platform specific icons for native bundles. In this case you need to follow the convention of the JavaFX packaging tools to ensure your icons get
     * picked up.
     *
     * <ul>
     * <li>for <b>windows</b> put an icon at src/main/deploy/package/windows/your-app-name.ico</li>
     * <li>for <b>mac</b> put an icon at src/main/deploy/package/macosx/your-app-name.icns</li>
     * </ul>
     *
     * @parameter default-value="${project.basedir}/src/main/deploy"
     */
    protected String deployDir;

    /**
     * All commands executed by this Maven-plugin will be done using the current available commands of your maven-execution environment. It is possible to call Maven with a different version of Java,
     * so these calls might be wrong. To use the executables of the JDK used for running this maven-plugin, please set this to false. You might need this in the case you installed multiple versions of
     * Java.
     *
     * The default is to use environment relative executables.
     *
     * @parameter property="useEnvironmentRelativeExecutables" default-value="true"
     */
    protected boolean useEnvironmentRelativeExecutables;

    /**
     * Set this to true for skipping the execution.
     *
     * @parameter default-value="false"
     */
    protected boolean skip;

    private PackagerLib packagerLib;

    public PackagerLib getPackagerLib() throws MojoExecutionException {
        // lazy-initialization of packagerLib
        if (packagerLib == null) {
            // add deployDir to system classpath
            if (deployDir != null) {
                getLog().info("Adding 'deploy' directory to Mojo classpath: " + deployDir);
                URLClassLoader sysloader = (URLClassLoader) Thread.currentThread().getContextClassLoader();
                Class<URLClassLoader> sysclass = URLClassLoader.class;
                try {
                    Method method = sysclass.getDeclaredMethod("addURL", URL.class);
                    method.setAccessible(true);
                    method.invoke(sysloader, new File(deployDir).toURI().toURL());
                } catch (NoSuchMethodException | SecurityException | MalformedURLException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                    throw new MojoExecutionException("Error, could not add URL to system classloader", ex);
                }
            }

            Log.setLogger(new Log.Logger(verbose));
            this.packagerLib = new PackagerLib();
        }
        return this.packagerLib;
    }

    protected String getEnvironmentRelativeExecutablePath() {
        if (useEnvironmentRelativeExecutables) {
            return "";
        }

        String jrePath = System.getProperty("java.home");
        String jdkPath = jrePath + File.separator + ".." + File.separator + "bin" + File.separator;

        return jdkPath;
    }

    protected void copyRecursive(Path sourceFolder, Path targetFolder) throws IOException {
        Files.walkFileTree(sourceFolder, new SimpleFileVisitor<Path>() {

            @Override
            public FileVisitResult preVisitDirectory(Path subfolder, BasicFileAttributes attrs) throws IOException {
                // do create subfolder (if needed)
                Files.createDirectories(targetFolder.resolve(sourceFolder.relativize(subfolder)));
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path sourceFile, BasicFileAttributes attrs) throws IOException {
                // do copy, and replace, as the resource might already be existing
                Files.copy(sourceFile, targetFolder.resolve(sourceFolder.relativize(sourceFile)), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
                return FileVisitResult.CONTINUE;
            }

        });
    }

    protected Path getRelativeLibDir() {
        return jfxLibDir != null ? Paths.get(jfxLibDir) : Paths.get("");
    }

    protected Path getAbsoluteAppLibDir() {
        return getAbsoluteAppBinDir().resolve(getRelativeLibDir());
    }

    protected Path getRelativeBinDir() {
        return jfxBinDir != null ? Paths.get(jfxBinDir) : Paths.get("");
    }

    protected Path getAbsoluteAppBinDir() {
        return jfxAppOutputDir.toPath().resolve(getRelativeBinDir());
    }
}
