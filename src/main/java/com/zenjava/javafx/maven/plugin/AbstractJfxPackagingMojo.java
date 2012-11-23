package com.zenjava.javafx.maven.plugin;

import com.zenjava.javafx.maven.plugin.util.JfxToolsWrapper;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Build;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;

import java.io.File;

public abstract class AbstractJfxPackagingMojo extends AbstractMojo {

    /**
     * @parameter expression="${java.home}"
     */
    protected String javaHome;

    /**
     * @parameter expression="${verbose}" default-value="false"
     */
    protected Boolean verbose;

    /**
     * @parameter expression="${mainClass}"
     * @required
     */
    protected String mainClass;

    /**
     * @parameter expression="${jarFileName}"
     */
    protected String jarFileName;


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


    protected File getTargetJarFile() {

        Build build = project.getBuild();
        File outputDir = new File(build.getDirectory());

        String jarName = this.jarFileName;
        if (jarName == null) {
            jarName = build.getFinalName() + "-jfx.jar";
        }
        return new File(outputDir, jarName);
    }


    protected JfxToolsWrapper getJfxToolsWrapper() throws MojoFailureException, MojoExecutionException {

        getLog().info("Java home is: " + javaHome);
        File javaHomeDir = new File(javaHome);
        File jdkHomeDir = javaHomeDir.getParentFile();
        File jfxToolsJar = new File(jdkHomeDir, "lib/ant-javafx.jar");
        if (!jfxToolsJar.exists()) {
            throw new MojoFailureException("Unable to find JavaFX tools JAR file at '"
                    + jfxToolsJar + "'. Is your JAVA_HOME set to a JDK with JavaFX installed (must be Java 1.7.0 update 9 or higher)?");
        }
        return new JfxToolsWrapper(jfxToolsJar, verbose);
    }
}
