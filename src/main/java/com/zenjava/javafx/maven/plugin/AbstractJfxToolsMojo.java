package com.zenjava.javafx.maven.plugin;

import com.sun.javafx.tools.packager.Log;
import com.sun.javafx.tools.packager.PackagerLib;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

public abstract class AbstractJfxToolsMojo extends AbstractMojo {

    /**
     * The Maven Project Object
     *
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    protected MavenProject project;

    /**
     * @parameter expression="${java.home}"
     */
    protected String javaHome;

    /**
     * @parameter expression="${verbose}" default-value="false"
     */
    protected Boolean verbose;

    /**
     * @parameter expression="${project.build.directory}/jfx/app"
     */
    protected File jfxAppOutputDir;

    /**
     * @parameter expression="${project.build.finalName}-jfx.jar"
     */
    protected String jfxMainAppJarName;


    @Override
    public final void execute() throws MojoExecutionException, MojoFailureException {

        getLog().info("Java home is: " + javaHome);

        // find ant-javafx.jar in JDK install

        File javaHomeDir = new File(javaHome);
        File jdkHomeDir = javaHomeDir.getParentFile();
        File jfxToolsJar = new File(jdkHomeDir, "lib/ant-javafx.jar");
        if (!jfxToolsJar.exists()) {
            throw new MojoFailureException("Unable to find JavaFX tools JAR file at '"
                    + jfxToolsJar + "'. Is your JAVA_HOME set to a JDK with JavaFX installed (must be Java 1.7.0 update 9 or higher)?");
        }

        // add ant-javafx.jar to system classpath

        URLClassLoader sysloader = (URLClassLoader) ClassLoader.getSystemClassLoader();
        Class sysclass = URLClassLoader.class;

        try {
            Method method = sysclass.getDeclaredMethod("addURL", URL.class);
            method.setAccessible(true);
            method.invoke(sysloader, jfxToolsJar.toURI().toURL());
        } catch (Throwable t) {
            t.printStackTrace();
            throw new MojoExecutionException("Error, could not add URL to system classloader");
        }

        // set the JFX Packager log class

        Log.Logger logger = new Log.Logger(verbose);
        Log.setLogger(logger);

        // run the actual Mojo class now with ant-javafx.jar on the classpath

        PackagerLib packagerLib = new PackagerLib();
        execute(packagerLib);
    }

    public abstract void execute(PackagerLib packagerLib) throws MojoExecutionException, MojoFailureException;
}
