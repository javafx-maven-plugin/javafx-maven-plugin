package com.zenjava.javafx.maven.plugin;

import com.zenjava.javafx.deploy.ApplicationProfile;
import com.zenjava.javafx.maven.plugin.util.JfxToolsWrapper;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Build;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import static org.twdata.maven.mojoexecutor.MojoExecutor.*;

public abstract class AbstractBundleMojo extends AbstractMojo {

    /**
     * @parameter expression="${project.name}"
     * @required
     */
    protected String title;

    /**
     * @parameter expression="${project.organization.name}"
     * @required
     */
    protected String vendor;

    /**
     * @parameter expression="${project.description}"
     */
    protected String description;

    /**
     * @parameter expression="${project.organization.url}"
     */
    protected String homepage;

    /**
     * @parameter
     */
    protected File icon;

    /**
     * @parameter
     */
    protected File splashImage;

    /**
     * @parameter default-value="launch.jnlp"
     */
    protected String jnlpFileName;

    /**
     * @parameter
     */
    protected boolean offlineAllowed;

    /**
     * @parameter
     */
    protected String[] permissions;

    /**
     * @parameter
     */
    protected String jreVersion;

    /**
     * @parameter
     */
    protected String jreArgs;

    /**
     * @parameter
     */
    protected String jfxVersion;

    /**
     * @parameter expression="${mainClass}"
     * @required
     */
    protected String mainClass;

    /**
     * @parameter expression="${java.home}"
     */
    protected String javaHome;

    /**
     * @parameter expression="${verbose}" default-value="false"
     */
    protected Boolean verbose;


    /**
     * @parameter
     */
    protected File keyStore;

    /**
     * @parameter
     */
    protected String keyStoreAlias;

    /**
     * @parameter
     */
    protected String keyStorePassword;

    /**
     * @parameter
     */
    protected String keyPassword;

    /**
     * @parameter default-value="jks"
     */
    protected String keyStoreType;


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



    protected ApplicationProfile getApplicationProfile() throws MojoExecutionException {

        ApplicationProfile appProfile = new ApplicationProfile();
        appProfile.setTitle(title);
        appProfile.setVendor(vendor);
        appProfile.setDescription(description);
        appProfile.setMainClass(mainClass);
        appProfile.setHomepage(homepage);

        File icon = this.icon;
        if (icon == null) {
            File file = new File(project.getBasedir(), "src/main/deploy/icon.png");
            if (file.exists()) {
                icon = file;
            }
        }
        if (icon != null) {
            getLog().debug("Using icon file at '" + icon + "'");
            appProfile.setIcon(icon.getName());
        }

        File splashImage = this.splashImage;
        if (splashImage == null) {
            File file = new File(project.getBasedir(), "src/main/deploy/splash.jpg");
            if (file.exists()) {
                splashImage = file;
            }
        }
        if (splashImage != null) {
            getLog().debug("Using splash image file at '" + splashImage + "'");
            appProfile.setSplashImage(splashImage.getName());
        }

        appProfile.setJnlpFileName(jnlpFileName);
        appProfile.setOfflineAllowed(offlineAllowed);

        if (jreVersion != null) {
            appProfile.setJreVersion(jreVersion);
        }

        if (jreArgs != null) {
            appProfile.setJreArgs(jreArgs);
        }

        if (jfxVersion != null) {
            appProfile.setJreVersion(jfxVersion);
        }

        if (permissions != null) {
            appProfile.setPermissions(permissions);
        }

        return appProfile;
    }

    protected void copyAllJarsAndUpdateProfile(File baseDir,
                                               String jarSubDir,
                                               ApplicationProfile appProfile)
            throws MojoExecutionException, MojoFailureException {

        File jarDir = baseDir;
        if (!StringUtils.isEmpty(jarSubDir)) {
            jarDir = new File(baseDir, jarSubDir);
        }
        copyAllJars(jarDir);
        updateApplicationProfileDependencies(baseDir, appProfile);
    }

    protected void copyAllJars(File targetDir) throws MojoExecutionException, MojoFailureException {

        getLog().debug("Building exploded bundle for JavaFX distributable to '" + targetDir + "'");

        getLog().info("Copying module dependencies to: '" + targetDir + "'");
        copyModuleDependencies(targetDir);

        Build build = project.getBuild();
        File jarFile = new File(build.getDirectory(), build.getFinalName() + ".jar");
        try {
            getLog().info("Copying module jar file to output directory: '" + jarFile + "' => '" + targetDir + "'");
            FileUtils.copyFileToDirectoryIfModified(jarFile, targetDir);
        } catch (IOException e) {
            throw new MojoExecutionException("Unable to copy module jar file to output directory: '"
                    + jarFile + "' => '" + targetDir + "'");
        }
    }

    protected File[] findJars(File baseDirectory) {

        Collection jars =  org.apache.commons.io.FileUtils.listFiles(baseDirectory, new String[] { "jar" }, true);
        return org.apache.commons.io.FileUtils.convertFileCollectionToFileArray(jars);
    }

    protected String[] findJarsRelative(File baseDirectory) throws MojoExecutionException {

        File[] jars = findJars(baseDirectory);
        String[] relativeJars = new String[jars.length];
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < jars.length; i++) {
            File jar = jars[i];
            builder.setLength(0);
            while (!jar.equals(baseDirectory)) {
                if (builder.length() > 0) {
                    builder.insert(0, "/");
                }
                builder.insert(0, jar.getName());
                jar = jar.getParentFile();
            }
            relativeJars[i] = builder.toString();
        }
        return relativeJars;
    }

    protected void updateApplicationProfileDependencies(File bundleDir, ApplicationProfile appProfile)
            throws MojoExecutionException {

        String[] jars = findJarsRelative(bundleDir);
        appProfile.setJarResources(jars);
    }

    protected void copyModuleDependencies(File targetDir) throws MojoExecutionException, MojoFailureException {

        executeMojo(
                plugin(
                        groupId("org.apache.maven.plugins"),
                        artifactId("maven-dependency-plugin"),
                        version("2.5.1")
                ),
                goal("copy-dependencies"),
                configuration(
                        element(name("outputDirectory"), targetDir.getPath())
                ),
                executionEnvironment(
                        project,
                        session,
                        pluginManager
                )
        );
    }

    public void signJarFiles(File directory) throws MojoExecutionException, MojoFailureException {

        getLog().info("Signing all JAR files in '" + directory + "'");

        if (keyStore == null) {
            keyStore = new File(project.getBasedir(), "/src/main/deploy/keystore.jks");
        }
        if (!keyStore.exists()) {
            throw new MojoFailureException("Keystore does not exist, use 'jfx:generate-key-store' command to make one (expected at: " + keyStore + ")");
        }

        if (StringUtils.isEmpty(keyStoreAlias)) {
            throw new MojoFailureException("A 'keyStoreAlias' is required for signing JARs");
        }

        if (StringUtils.isEmpty(keyStorePassword)) {
            throw new MojoFailureException("A 'keyStorePassword' is required for signing JARs");
        }

        if (keyPassword == null) {
            keyPassword = keyStorePassword;
        }

        JfxToolsWrapper jfxTools = getJfxToolsWrapper();
        File[] jarFiles = findJars(directory);
        jfxTools.signJarFiles(keyStore, keyStoreAlias, keyStorePassword, keyPassword, keyStoreType, jarFiles);
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

        File deployDir = new File(project.getBasedir(), "src/main/deploy");

        return new JfxToolsWrapper(jfxToolsJar, deployDir.exists() ? deployDir : null, verbose);
    }
}
