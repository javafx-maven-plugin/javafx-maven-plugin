package com.zenjava.javafx.maven.plugin;

import com.oracle.tools.packager.Log;
import com.sun.javafx.tools.packager.PackagerLib;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * Base Mojo that any other Mojo wanting to access the JavaFX Packager tools should extend from. This provides
 * convenience methods for accessing the JavaFX Packager tools in a standard and simple way.
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
     * Flag to turn on verbose logging. Set this to true if you are having problems and what more detailed information.
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
     * The 'app' output directory. This is where the base executable JavaFX jar is built into, along with any dependent
     * libraries (place in the 'lib' sub-directory). The resulting JAR in this directory will be ready for distribution,
     * including Pre-Loaders, signing, etc. This JAR will also be the one bundled into the other distribution bundles
     * (i.e. web or native) if you run the relevant commands for that.
     * <p/>
     * This defaults to 'target/jfx/app' and in most cases there is no real need to change this.
     *
     * @parameter default-value="${project.build.directory}/jfx/app"
     */
    protected File jfxAppOutputDir;

    /**
     * The name of the JavaFX packaged JAR to be built into the 'app' directory. By default this will be the finalName
     * as set in your project with a '-jfx' suffix. Change this if you want something nicer. Note, that changing this
     * value does not affect the regular old, non-JFX modified JAR (built in the 'target' directory).
     *
     * @parameter default-value="${project.build.finalName}-jfx.jar"
     */
    protected String jfxMainAppJarName;

    /**
     * <p>The directory contain deployment specific files, such as icons and splash screen images. This directory is added
     * to the classpath of the Mojo when it runs, so that any files within this directory are accessible to the
     * JavaFX packaging tools.</p>
     *
     * <p>This defaults to src/main/deploy and typically this is good enough. Just put your deployment specific files in
     * this directory and they will be automatically picked up.</p>
     *
     * <p>The most common usage for this is to provide platform specific icons for native bundles. In this case you need
     * to follow the convention of the JavaFX packaging tools to ensure your icons get picked up.</p>
     *
     * <ul>
     *     <li>for <b>windows</b> put an icon at src/main/deploy/package/windows/your-app-name.ico</li>
     *     <li>for <b>mac</b> put an icon at src/main/deploy/package/macosx/your-app-name.icns</li>
     * </ul>
     *
     * @parameter default-value="${project.basedir}/src/main/deploy"
     */
    protected String deployDir;


    private PackagerLib packagerLib;


    public PackagerLib getPackagerLib() throws MojoExecutionException {

        if (packagerLib == null) {

            // add deployDir to system classpath
            if (deployDir != null) {
                getLog().info("Adding 'deploy' directory to Mojo classpath: " + deployDir);
                URLClassLoader sysloader = (URLClassLoader) ClassLoader.getSystemClassLoader();
                Class<URLClassLoader> sysclass = URLClassLoader.class;
                try {
                    Method method = sysclass.getDeclaredMethod("addURL", URL.class);
                    method.setAccessible(true);
                    method.invoke(sysloader, new File(deployDir).toURI().toURL());
                } catch (Throwable t) {
                    t.printStackTrace();
                    throw new MojoExecutionException("Error, could not add URL to system classloader");
                }
            }

            Log.Logger logger = new Log.Logger(verbose);
            Log.setLogger(logger);
            this.packagerLib = new PackagerLib();
        }
        return this.packagerLib;
    }
}
