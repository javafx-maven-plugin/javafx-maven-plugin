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

import org.apache.maven.model.Build;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.maven.artifact.Artifact;

/**
 * @goal build-jar
 * @phase package
 * @requiresDependencyResolution
 */
public class JarMojo extends AbstractJfxToolsMojo {
    private static final String LIB_DIR_NAME = "lib";
    private static final String JDK_LIB_DIR_NAME = "lib";

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
     * Set this to true if your app needs to break out of the standard web sandbox and do more powerful functions.
     * <p>
     * If you are using FXML you will need to set this value to true.
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

    /**
     * For being able to use &lt;userJvmArgs&gt;, we have to copy some dependency when being used. To disable this feature an not having packager.jar
     * in your project, set this to false.
     * <p>
     * To get more information about, please check the documentation here: https://docs.oracle.com/javase/8/docs/technotes/guides/deploy/jvm_options_api.html.
     *
     * @parameter default-value=true
     * @since 8.1.4
     */
    protected boolean addPackagerJar;

    /**
     * In the case you don't want some dependency landing in the generated lib-folder (e.g. complex maven-dependencies),
     * you now can manually exclude that dependency by added it's coordinates here.
     *
     * @parameter
     * @since 8.2.0
     */
    protected List<Dependency> classpathExcludes = new ArrayList<>();

    /**
     * Per default all listed classpath excludes are ment to be transivie, that means when any direct declared dependency
     * requires another dependency.
     * <p>
     * When having &lt;classpathExcludes&gt; contains any dependency, that dependency including all transitive dependencies
     * are filtered while processing lib-files, it's the default behaviour. In the rare case you have some very special setup,
     * and just want to exlude these dependency, but want to preserve all transitive dependencies going into the lib-folder,
     * this can be set via this property.
     * <p>
     * Set this to false when you want to have the direct declared dependency excluded from lib-file-processing.
     *
     * @parameter default-value=true
     * @since 8.2.0
     */
    protected boolean classpathExcludesTransient;

    /**
     * When you need to add additional files to generated app-folder (e.g. README, license, third-party-tools, ...),
     * you can specify the source-folder here. All files will be copied recursively.
     *
     * @parameter
     */
    protected File additionalAppResources;

    /**
     * It is possible to copy all files specified by additionalAppResources into the created app-folder containing
     * your jfx-jar. This makes it possible to have external files (like native binaries) being available while
     * developing using the run-mojo.
     *
     * @parameter default-value="false"
     */
    private boolean copyAdditionalAppResourcesToJar = false;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if( jfxCallFromCLI ){
            getLog().info("call from CLI - skipping creation of JavaFX JAR for application");
            return;
        }

        if( skip ){
            getLog().info("Skipping execution of JarMojo MOJO.");
            return;
        }

        getLog().info("Building JavaFX JAR for application");

        Build build = project.getBuild();

        CreateJarParams createJarParams = new CreateJarParams();
        createJarParams.setOutdir(jfxAppOutputDir);

        // check if we got some filename ending with ".jar" (found this while checking issue 128)
        if( !jfxMainAppJarName.toLowerCase().endsWith(".jar") ){
            getLog().error("Please provide a proper value for <jfxMainAppJarName>! It has to end with \".jar\".");
            return;
        }

        createJarParams.setOutfile(jfxMainAppJarName);
        createJarParams.setApplicationClass(mainClass);
        createJarParams.setCss2bin(css2bin);
        createJarParams.setPreloader(preLoader);

        if( manifestAttributes == null ){
            manifestAttributes = new HashMap<>();
        }
        createJarParams.setManifestAttrs(manifestAttributes);

        StringBuilder classpath = new StringBuilder();
        File libDir = new File(jfxAppOutputDir, LIB_DIR_NAME);
        if( !libDir.exists() && !libDir.mkdirs() ){
            throw new MojoExecutionException("Unable to create app lib dir: " + libDir);
        }

        if( updateExistingJar ){
            File potentialExistingFile = new File(build.getDirectory() + File.separator + build.getFinalName() + ".jar");
            if( !potentialExistingFile.exists() ){
                throw new MojoExecutionException("Could not update existing jar-file, because it does not exist. Please make sure this file gets created or exists, or set updateExistingJar to false.");
            }
            createJarParams.addResource(null, potentialExistingFile);
        } else {
            File potentialExistingGeneratedClasses = new File(build.getOutputDirectory());
            // make sure folder exists, it is possible to have just some bootstraping "-jfx.jar"
            if( !potentialExistingGeneratedClasses.exists() ){
                getLog().warn("There were no classes build, this might be a problem of your project, if its not, just ignore this message. Continuing creating JavaFX JAR...");
                potentialExistingGeneratedClasses.mkdirs();
            }
            createJarParams.addResource(potentialExistingGeneratedClasses, "");
        }

        try{
            if( checkIfJavaIsHavingPackagerJar() ){
                getLog().debug("Check if packager.jar needs to be added");
                if( addPackagerJar ){
                    getLog().debug("Searching for packager.jar ...");
                    String targetPackagerJarPath = JDK_LIB_DIR_NAME + File.separator + "packager.jar";
                    for( Dependency dependency : project.getDependencies() ){
                        // check only system-scoped
                        if( "system".equalsIgnoreCase(dependency.getScope()) ){
                            File packagerJarFile = new File(dependency.getSystemPath());
                            String packagerJarFilePathString = packagerJarFile.toPath().normalize().toString();
                            if( packagerJarFile.exists() && packagerJarFilePathString.endsWith(targetPackagerJarPath) ){
                                getLog().debug(String.format("Including packager.jar from system-scope: %s", packagerJarFilePathString));
                                File dest = new File(libDir, packagerJarFile.getName());
                                if( !dest.exists() ){
                                    Files.copy(packagerJarFile.toPath(), dest.toPath());
                                }
                                appendClasspath(classpath, packagerJarFile);
                            }
                        }
                    }
                } else {
                    getLog().debug("No packager.jar will be added");
                }
            } else {
                if( addPackagerJar ){
                    getLog().warn("Skipped checking for packager.jar. Please install at least Java 1.8u40 for using this feature.");
                }
            }

            copyDependenciesToLibDir(libDir, classpath);

        } catch (IOException e) {
            throw new MojoExecutionException("Error copying dependency for application", e);
        }
        createJarParams.setClasspath(classpath.toString());

        // https://docs.oracle.com/javase/8/docs/technotes/guides/deploy/manifest.html#JSDPG896
        if( allPermissions ){
            manifestAttributes.put("Permissions", "all-permissions");
        }

        try{
            getPackagerLib().packageAsJar(createJarParams);
        } catch(PackagerException e){
            throw new MojoExecutionException("Unable to build JFX JAR for application", e);
        }

        if( copyAdditionalAppResourcesToJar ){
            Optional.ofNullable(additionalAppResources)
                    .filter(File::exists)
                    .ifPresent(appResources -> {
                        getLog().info("Copying additional app ressources...");

                        try{
                            Path targetFolder = jfxAppOutputDir.toPath();
                            Path sourceFolder = appResources.toPath();
                            copyRecursive(sourceFolder, targetFolder);
                        } catch(IOException e){
                            getLog().warn("Couldn't copy additional application resource-file(s).", e);
                        }
                    });
        }

        // cleanup
        if( libDir.list().length == 0 ){
            // remove lib-folder, when nothing ended up there
            libDir.delete();
        }
    }

    private void copyDependenciesToLibDir(File libDir, StringBuilder classpath) throws MojoExecutionException {
        List<String> brokenArtifacts = new ArrayList<>();
        project.getArtifacts().stream().filter(artifact -> {
            // filter all unreadable, non-file artifacts
            File artifactFile = artifact.getFile();
            return artifactFile.isFile() && artifactFile.canRead();
        }).filter(artifact -> {
            if (classpathExcludes.isEmpty()) {
                return true;
            }
            boolean isListedInList = isListedInExclusionList(artifact);
            return !isListedInList;
        }).forEach(artifact -> {
            File artifactFile = artifact.getFile();
            getLog().debug(String.format("Including classpath element: %s", artifactFile.getAbsolutePath()));
            File dest = new File(libDir, artifactFile.getName());
            if (!dest.exists()) {
                try {
                    Files.copy(artifactFile.toPath(), dest.toPath());
                } catch (IOException ex) {
                    getLog().warn(String.format("Couldn't read from file %s", artifactFile.getAbsolutePath()));
                    getLog().debug(ex);
                    brokenArtifacts.add(artifactFile.getAbsolutePath());
                }
            }
            appendClasspath(classpath, artifactFile);
        });
        if (!brokenArtifacts.isEmpty()) {
            throw new MojoExecutionException("Error copying dependencies for application");
        }
    }

    private void appendClasspath(StringBuilder classpath, File artifactFile) {
        classpath.append(LIB_DIR_NAME).append("/").append(artifactFile.getName()).append(" ");
    }

    private boolean checkIfJavaIsHavingPackagerJar() {
        if( JavaDetectionTools.IS_JAVA_8 && JavaDetectionTools.isAtLeastOracleJavaUpdateVersion(40) ){
            return true;
        }
        if( JavaDetectionTools.IS_JAVA_9 ){ // NOSONAR
            return true;
        }
        return false;
    }

    private boolean isListedInExclusionList(Artifact artifact) {
        return classpathExcludes.stream().filter(dependency -> {
            // we are checking for "groupID:artifactId:" because we don't care about versions nor types (jar, war, source, ...)
            String dependencyTrailIdentifier = dependency.getGroupId() + ":" + dependency.getArtifactId() + ":";

            // when not transitive, look at the artifact information
            if( !classpathExcludesTransient ){
                return dependencyTrailIdentifier.startsWith(artifact.getGroupId() + ":" + artifact.getArtifactId() + ":");
            }

            // when transitive, look at the trail
            return artifact.getDependencyTrail().stream().anyMatch((dependencyTrail) -> (dependencyTrail.startsWith(dependencyTrailIdentifier)));
        }).count() > 0;
    }
}
