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

import com.oracle.tools.packager.Bundler;
import com.oracle.tools.packager.Bundlers;
import com.oracle.tools.packager.ConfigException;
import com.oracle.tools.packager.RelativeFileSet;
import com.oracle.tools.packager.StandardBundlerParam;
import com.oracle.tools.packager.UnsupportedPlatformException;
import com.sun.javafx.tools.packager.PackagerException;
import com.sun.javafx.tools.packager.SignJarParams;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @goal build-native
 */
public class NativeMojo extends AbstractJfxToolsMojo {

    /**
     * Used as the 'id' of the application, and is used as the CFBundleDisplayName on Mac. See the official JavaFX
     * Packaging tools documentation for other information on this. Will be used as GUID on some installers too.
     *
     * @parameter
     */
    protected String identifier;

    /**
     * The vendor of the application (i.e. you). This is required for some of the installation bundles and it's
     * recommended just to set it from the get-go to avoid problems. This will default to the project.organization.name
     * element in you POM if you have one.
     *
     * @parameter property="project.organization.name"
     * @required
     */
    protected String vendor;

    /**
     *
     * The output directory that the native bundles are to be built into. This will be the base directory only as the
     * JavaFX packaging tools use sub-directories that can't be customised. Generally just have a rummage through the
     * sub-directories until you find what you are looking for.
     * <p>
     * This defaults to 'target/jfx/native' and the interesting files are usually under 'bundles'.
     *
     * @parameter default-value="${project.build.directory}/jfx/native"
     */
    protected File nativeOutputDir;

    /**
     * Specify the used bundler found by selected bundleType. May not be installed your OS and will fail in that case.
     *
     * <p>
     * By default this will be set to 'ALL', depending on your installed OS following values are possible for installers:
     * <p>
     * <ul>
     * <li>windows.app <i>(Creates only Windows Executable, does not bundle into Installer)</i></li>
     * <li>linux.app <i>(Creates only Linux Executable, does not bundle into Installer)</i></li>
     * <li>mac.app <i>(Creates only Mac Executable, does not bundle into Installer)</i></li>
     * <li>mac.appStore <i>(Creates a binary bundle ready for deployment into the Mac App Store)</i></li>
     * <li>exe <i>(Microsoft Windows EXE Installer, via InnoIDE)</i></li>
     * <li>msi <i>(Microsoft Windows MSI Installer, via WiX)</i></li>
     * <li>deb <i>(Linux Debian Bundle)</i></li>
     * <li>rpm <i>(Redhat Package Manager (RPM) bundler)</i></li>
     * <li>dmg <i>(Mac DMG Installer Bundle)</i></li>
     * <li>pkg <i>(Mac PKG Installer Bundle)</i></li>
     * </ul>
     *
     * <p>
     * For a full list of available bundlers on your system, call 'mvn jfx:list-bundler' inside your project.
     *
     * @parameter property="bundler" default-value="ALL"
     */
    private String bundler;

    /**
     * Properties passed to the Java Virtual Machine when the application is started (i.e. these properties are system
     * properties of the JVM bundled in the native distribution and used to run the application once installed).
     *
     * @parameter
     */
    private Map<String, String> jvmProperties;

    /**
     * JVM Flags to be passed into the JVM at invocation time. These are the arguments to the left of the main class
     * name when launching Java on the command line. For example:
     * <pre>
     *     &lt;jvmArgs&gt;
     *         &lt;jvmArg&gt;-Xmx8G&lt;/jvmArg&gt;
     *     &lt;/jvmArgs&gt;
     * </pre>
     *
     * @parameter
     */
    private List<String> jvmArgs;

    /**
     * Optional command line arguments passed to the application when it is started. These will be included in the
     * native bundle that is generated and will be accessible via the main(String[] args) method on the main class that
     * is launched at runtime.
     * <p>
     * These options are user overridable for the value part of the entry via user preferences. The key and the value
     * are concated without a joining character when invoking the JVM.
     *
     * @parameter
     */
    private Map<String, String> userJvmArgs;

    /**
     * You can specify arguments that gonna be passed when calling your application.
     *
     * @parameter
     */
    private List<String> launcherArguments;

    /**
     * The release version as passed to the native installer. It would be nice to just use the project's version number
     * but this must be a fairly traditional version string (like '1.34.5') with only numeric characters and dot
     * separators, otherwise the JFX packaging tools bomb out. We default to 1.0 in case you can't be bothered to set
     * a version and don't really care.
     *
     * @parameter default-value="1.0"
     */
    private String nativeReleaseVersion;

    /**
     * Set this to true if you would like your application to have a shortcut on the users desktop (or platform
     * equivalent) when it is installed.
     *
     * @parameter default-value=false
     */
    protected boolean needShortcut;

    /**
     * Set this to true if you would like your application to have a link in the main system menu (or platform
     * equivalent) when it is installed.
     *
     * @parameter default-value=false
     */
    protected boolean needMenu;

    /**
     * A list of bundler arguments. The particular keys and the meaning of their values are dependent on the bundler
     * that is reading the arguments. Any argument not recognized by a bundler is silently ignored, so that arguments
     * that are specific to a specific bundler (for example, a Mac OS X Code signing key name) can be configured and
     * ignored by bundlers that don't use the particular argument.
     * <p>
     * To disable creating native bundles with JRE in it, just add "&lt;runtime /&gt;" to bundleArguments.
     * <p>
     * If there are bundle arguments that override other fields in the configuration, then it is an execution error.
     *
     * @parameter
     */
    protected Map<String, String> bundleArguments;

    /**
     * The name of the JavaFX packaged executable to be built into the 'native/bundles' directory. By default this will
     * be the finalName as set in your project. Change this if you want something nicer. This also has effect on the
     * filename of icon-files, e.g. having 'NiceApp' as appName means you have to place that icon
     * at 'src/main/deploy/package/[os]/NiceApp.[icon-extension]' for having it picked up by the bundler.
     *
     * @parameter default-value="${project.build.finalName}"
     */
    protected String appName;

    /**
     * Will be set when having goal "build-native" within package-phase and calling "jfx:native" from CLI. Internal usage only.
     *
     * @parameter default-value=false
     */
    protected boolean jfxCallFromCLI;

    /**
     * When you need to add additional files to generated app-folder (e.g. README, license, third-party-tools, ...),
     * you can specify the source-folder here. All files will be copied recursively.
     *
     * @parameter
     */
    protected File additionalAppResources;

    /**
     * Since Java version 1.8.0 Update 40 the native launcher for linux was changed and includes a bug
     * while searching for the generated configfile. This results in wrong ouput like this:
     * <pre>
     * client-1.1 No main class specified
     * client-1.1 Failed to launch JVM
     * </pre>
     * <p>
     * Scenario (which would work on windows):
     * <p>
     * <ul>
     * <li>generated launcher: i-am.working.1.2.0-SNAPSHOT</li>
     * <li>launcher-algorithm extracts the "extension" (a concept not known in linux-space for executables) and now searches for i-am.working.1.2.cfg</li>
     * </ul>
     * <p>
     * Change this to "true" when you don't want this workaround.
     *
     * @see https://github.com/javafx-maven-plugin/javafx-maven-plugin/issues/124
     *
     * @parameter default-value=false
     */
    protected boolean skipNativeLauncherWorkaround124;

    /**
     * @parameter
     */
    protected List<NativeLauncher> secondaryLaunchers;

    /**
     * Since Java version 1.8.0 Update 60 the native launcher configuration for windows was changed
     * and includes a bug: the file-format before was "property-file", now it's "INI-file" per default,
     * but the runtime-configuration isn't honored like in property-files.
     * This workaround enforces the property-file-format.
     * <p>
     * Change this to "true" when you don't want this workaround.
     *
     * @see https://github.com/javafx-maven-plugin/javafx-maven-plugin/issues/167
     * @parameter default-value=false
     */
    protected boolean skipNativeLauncherWorkaround167;

    /**
     * It is possible to create file associations when using native installers. When specified,
     * all file associations are bound to the main native launcher. There is no support for bunding
     * them to second launchers.
     * <p>
     * For more informatione, please see official information source: https://docs.oracle.com/javase/8/docs/technotes/guides/deploy/javafx_ant_task_reference.html#CIAIDHBJ
     *
     * @parameter
     */
    private List<FileAssociation> fileAssociations;
    
    /**
     * Since Java version 1.8.0 Update 60 a new bundler for generating JNLP-files was presented and includes
     * a bug while generating relative file-references when building on windows.
     * <p>
     * Change this to "true" when you don't want this workaround.
     */
    protected boolean skipJNLPRessourcePathWorkaround182;

    /**
     * The location of the keystore. If not set, this will default to src/main/deploy/kesytore.jks which is usually fine
     * to use for most cases.
     *
     * @parameter default-value="src/main/deploy/keystore.jks"
     */
    protected File keyStore;

    /**
     * The alias to use when accessing the keystore. This will default to "myalias".
     *
     * @parameter default-value="myalias"
     */
    protected String keyStoreAlias;

    /**
     * The password to use when accessing the keystore. This will default to "password".
     *
     * @parameter default-value="password"
     */
    protected String keyStorePassword;

    /**
     * The password to use when accessing the key within the keystore. If not set, this will default to
     * keyStorePassword.
     *
     * @parameter
     */
    protected String keyPassword;

    /**
     * The type of KeyStore being used. This defaults to "jks", which is the normal one.
     *
     * @parameter default-value="jks"
     */
    protected String keyStoreType;

    /**
     * Since Java version 1.8.0 Update 60 a new bundler for generating JNLP-files was introduced,
     * but lacks the ability to sign jar-files by passing some flag. We are signing the files in the
     * case of having "jnlp" as bundler. The MOJO with the goal "build-web" was deprecated in favor
     * of that new bundler (mostly because the old one does not fit the bundler-list strategy).
     * <p>
     * Change this to "true" when you don't want signing jar-files.
     *
     * @parameter default-value=false
     */
    protected boolean skipSigningJarFilesJNLP185;

    /**
     * After signing is done, the sizes inside generated JNLP-files still point to unsigned jar-file sizes,
     * so we have to fix these sizes to be correct. This sizes-fix even lacks in the old web-MOJO.
     * <p>
     * Change this to "true" when you don't want to recalculate sizes of jar-files.
     *
     * @parameter default-value=false
     */
    protected boolean skipSizeRecalculationForJNLP185;
    
    /**
     * JavaFX introduced a new way for signing jar-files, which was called "BLOB signing".
     * <p>
     * The tool "jarsigner" is not able to verify that signature and webstart doesn't
     * accept that format either. For not having to call jarsigner yourself, set this to "true"
     * for having your jar-files getting signed when generating JNLP-files.
     *
     * @see https://github.com/javafx-maven-plugin/javafx-maven-plugin/issues/190
     * 
     * @parameter default-value=false
     */
    protected boolean noBlobSigning;

    private static final String JNLP_JAR_PATTERN = "(.*)href=(\".*?\")(.*)size=(\".*?\")(.*)";

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if( jfxCallFromCLI ){
            getLog().info("call from CLI - skipping creation of Native Installers");
            return;
        }

        getLog().info("Building Native Installers");

        try{
            Map<String, ? super Object> params = new HashMap<>();

            params.put(StandardBundlerParam.VERBOSE.getID(), verbose);

            Optional.ofNullable(identifier).ifPresent(id -> {
                params.put(StandardBundlerParam.IDENTIFIER.getID(), id);
            });

            params.put(StandardBundlerParam.APP_NAME.getID(), appName);
            params.put(StandardBundlerParam.VERSION.getID(), nativeReleaseVersion);
            params.put(StandardBundlerParam.VENDOR.getID(), vendor);
            params.put(StandardBundlerParam.SHORTCUT_HINT.getID(), needShortcut);
            params.put(StandardBundlerParam.MENU_HINT.getID(), needMenu);
            params.put(StandardBundlerParam.MAIN_CLASS.getID(), mainClass);

            Optional.ofNullable(jvmProperties).ifPresent(jvmProps -> {
                params.put(StandardBundlerParam.JVM_PROPERTIES.getID(), new HashMap<>(jvmProps));
            });
            Optional.ofNullable(jvmArgs).ifPresent(jvmOptions -> {
                params.put(StandardBundlerParam.JVM_OPTIONS.getID(), new ArrayList<>(jvmOptions));
            });
            Optional.ofNullable(userJvmArgs).ifPresent(userJvmOptions -> {
                params.put(StandardBundlerParam.USER_JVM_OPTIONS.getID(), new HashMap<>(userJvmOptions));
            });
            Optional.ofNullable(launcherArguments).ifPresent(arguments -> {
                params.put(StandardBundlerParam.ARGUMENTS.getID(), new ArrayList<>(arguments));
            });

            // bugfix for #83 (by copying additional resources to /jfx/app folder)
            Optional.ofNullable(additionalAppResources).filter(File::exists).ifPresent(appResources -> {
                try{
                    Path targetFolder = jfxAppOutputDir.toPath();
                    Path sourceFolder = appResources.toPath();
                    Files.walkFileTree(appResources.toPath(), new FileVisitor<Path>() {

                        @Override
                        public FileVisitResult preVisitDirectory(Path subfolder, BasicFileAttributes attrs) throws IOException {
                            // do create subfolder (if needed)
                            Files.createDirectories(targetFolder.resolve(sourceFolder.relativize(subfolder)));
                            return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public FileVisitResult visitFile(Path sourceFile, BasicFileAttributes attrs) throws IOException {
                            // do copy
                            Files.copy(sourceFile, targetFolder.resolve(sourceFolder.relativize(sourceFile)), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
                            return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public FileVisitResult visitFileFailed(Path source, IOException ioe) throws IOException {
                            // don't fail, just inform user
                            getLog().warn(String.format("Couldn't copy additional app resource %s with reason %s", source.toString(), ioe.getLocalizedMessage()));
                            return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public FileVisitResult postVisitDirectory(Path source, IOException ioe) throws IOException {
                            // nothing to do here
                            return FileVisitResult.CONTINUE;
                        }
                    });
                } catch(IOException e){
                    getLog().warn(e);
                }
            });

            Set<File> resourceFiles = new HashSet<>();
            try{
                Files.walk(jfxAppOutputDir.toPath())
                        .map(p -> p.toFile())
                        .filter(File::isFile)
                        .filter(File::canRead)
                        .forEach(f -> {
                            getLog().info(String.format("Add %s file to application resources.", f));
                            resourceFiles.add(f);
                        });
            } catch(IOException e){
                getLog().warn(e);
            }
            params.put(StandardBundlerParam.APP_RESOURCES.getID(), new RelativeFileSet(jfxAppOutputDir, resourceFiles));

            Collection<String> duplicateKeys = new HashSet<>();
            Optional.ofNullable(bundleArguments).ifPresent(bArguments -> {
                duplicateKeys.addAll(params.keySet());
                duplicateKeys.retainAll(bArguments.keySet());
                params.putAll(bArguments);
            });

            if( !duplicateKeys.isEmpty() ){
                throw new MojoExecutionException("The following keys in <bundleArguments> duplicate other settings, please remove one or the other: " + duplicateKeys.toString());
            }

            // check for misconfiguration, requires to be different as this would overwrite primary launche
            Collection<String> launcherNames = new ArrayList<>();
            launcherNames.add(appName);
            final AtomicBoolean nullLauncherNameFound = new AtomicBoolean(false);
            // check "no launcher names" and gather all names
            Optional.ofNullable(secondaryLaunchers).filter(list -> !list.isEmpty()).ifPresent(launchers -> {
                getLog().info("Adding configuration for secondary native launcher");
                nullLauncherNameFound.set(launchers.stream().anyMatch(launcher -> launcher.getAppName() == null));
                if( !nullLauncherNameFound.get() ){
                    launcherNames.addAll(launchers.stream().map(launcher -> launcher.getAppName()).collect(Collectors.toList()));

                    // assume we have valid entry here
                    params.put(StandardBundlerParam.SECONDARY_LAUNCHERS.getID(), launchers.stream().map(launcher -> {
                        getLog().info("Adding secondary launcher: " + launcher.getAppName());
                        Map<String, Object> secondaryLauncher = new HashMap<>();
                        addToMapWhenNotNull(launcher.getAppName(), StandardBundlerParam.APP_NAME.getID(), secondaryLauncher);
                        addToMapWhenNotNull(launcher.getMainClass(), StandardBundlerParam.MAIN_CLASS.getID(), secondaryLauncher);
                        addToMapWhenNotNull(launcher.getJfxMainAppJarName(), StandardBundlerParam.MAIN_JAR.getID(), secondaryLauncher);
                        addToMapWhenNotNull(launcher.getNativeReleaseVersion(), StandardBundlerParam.VERSION.getID(), secondaryLauncher);
                        addToMapWhenNotNull(launcher.getVendor(), StandardBundlerParam.VENDOR.getID(), secondaryLauncher);
                        addToMapWhenNotNull(launcher.getIdentifier(), StandardBundlerParam.IDENTIFIER.getID(), secondaryLauncher);

                        addToMapWhenNotNull(launcher.isNeedMenu(), StandardBundlerParam.MENU_HINT.getID(), secondaryLauncher);
                        addToMapWhenNotNull(launcher.isNeedShortcut(), StandardBundlerParam.SHORTCUT_HINT.getID(), secondaryLauncher);

                        // as we can set another JAR-file, this might be completly different
                        addToMapWhenNotNull(launcher.getClasspath(), StandardBundlerParam.CLASSPATH.getID(), secondaryLauncher);

                        Optional.ofNullable(launcher.getJvmArgs()).ifPresent(jvmOptions -> {
                            secondaryLauncher.put(StandardBundlerParam.JVM_OPTIONS.getID(), new ArrayList<>(jvmOptions));
                        });
                        Optional.ofNullable(launcher.getJvmProperties()).ifPresent(jvmProps -> {
                            secondaryLauncher.put(StandardBundlerParam.JVM_PROPERTIES.getID(), new HashMap<>(jvmProps));
                        });
                        Optional.ofNullable(launcher.getUserJvmArgs()).ifPresent(userJvmOptions -> {
                            secondaryLauncher.put(StandardBundlerParam.USER_JVM_OPTIONS.getID(), new HashMap<>(userJvmOptions));
                        });
                        Optional.ofNullable(launcher.getLauncherArguments()).ifPresent(arguments -> {
                            params.put(StandardBundlerParam.ARGUMENTS.getID(), new ArrayList<>(arguments));
                        });
                        return secondaryLauncher;
                    }).collect(Collectors.toList()));
                }
            });

            // check "no launcher names"
            if( nullLauncherNameFound.get() ){
                throw new MojoExecutionException("Not all secondary launchers have been configured properly.");
            }
            // check "duplicate launcher names"
            Set<String> duplicateLauncherNamesCheckSet = new HashSet<>();
            launcherNames.stream().forEach(launcherName -> duplicateLauncherNamesCheckSet.add(launcherName));
            if( duplicateLauncherNamesCheckSet.size() != launcherNames.size() ){
                throw new MojoExecutionException("Secondary launcher needs to have different name, please adjust appName inside your configuration.");
            }

            Optional.ofNullable(fileAssociations).ifPresent(associations -> {
                final List<Map<String, ? super Object>> allAssociations = new ArrayList<>();
                associations.stream().forEach(association -> {
                    Map<String, ? super Object> settings = new HashMap<>();
                    settings.put(StandardBundlerParam.FA_DESCRIPTION.getID(), association.getDescription());
                    settings.put(StandardBundlerParam.FA_ICON.getID(), association.getIcon());
                    settings.put(StandardBundlerParam.FA_EXTENSIONS.getID(), association.getExtensions());
                    settings.put(StandardBundlerParam.FA_CONTENT_TYPE.getID(), association.getContentType());
                    allAssociations.add(settings);
                });
                params.put(StandardBundlerParam.FILE_ASSOCIATIONS.getID(), allAssociations);
            });

            // bugfix for "bundler not being able to produce native bundle without JRE on windows"
            // https://github.com/javafx-maven-plugin/javafx-maven-plugin/issues/167
            // this has been fixed and made available since 1.8.0u92:
            // http://www.oracle.com/technetwork/java/javase/2col/8u92-bugfixes-2949473.html
            if( isJavaVersion(8) && isAtLeastOracleJavaUpdateVersion(60) && !isAtLeastOracleJavaUpdateVersion(92)){
                if( !skipNativeLauncherWorkaround167 ){
                    if( params.containsKey("runtime") ){
                        getLog().info("Applying workaround for oracle-jdk-bug since 1.8.0u60 regarding cfg-file-format");
                        // the problem is com.oracle.tools.packager.windows.WinAppBundler within createLauncherForEntryPoint-Method
                        // it does NOT respect runtime-setting while calling "writeCfgFile"-method of com.oracle.tools.packager.AbstractImageBundler
                        // since newer java versions (they added possability to have INI-file-format of generated cfg-file, since 1.8.0_60).
                        // Because we want to have backward-compatibility within java 8, we will use parameter-name as hardcoded string!
                        // Our workaround: use prop-file-format
                        params.put("launcher-cfg-format", "prop");
                    }
                } else {
                    getLog().info("Skipped workaround for native launcher regarding cfg-file-format.");
                }
            }

            Bundlers bundlers = Bundlers.createBundlersInstance(); // service discovery?
            boolean foundBundler = false;
            for( Bundler b : bundlers.getBundlers() ){
                try{
                    if( bundler != null && !"ALL".equalsIgnoreCase(bundler) && !bundler.equalsIgnoreCase(b.getID()) ){
                        // this is not the specified bundler
                        continue;
                    }
                    foundBundler = true;

                    Map<String, ? super Object> paramsToBundleWith = new HashMap<>(params);
                    if( b.validate(paramsToBundleWith) ){
                        b.execute(paramsToBundleWith, nativeOutputDir);

                        // Workaround for "Native package for Ubuntu doesn't work"
                        // https://github.com/javafx-maven-plugin/javafx-maven-plugin/issues/124
                        // real bug: linux-launcher from oracle-jdk starting from 1.8.0u40 logic to determine .cfg-filename
                        if( isJavaVersion(8) && isAtLeastOracleJavaUpdateVersion(40) ){
                            if( "linux.app".equals(b.getID()) ){
                                getLog().info("Applying workaround for oracle-jdk-bug since 1.8.0u40 regarding native linux launcher(s).");
                                if( !skipNativeLauncherWorkaround124 ){
                                    // apply on main launcher
                                    applyNativeLauncherWorkaround(appName);

                                    // check on secondary launchers too
                                    if( secondaryLaunchers != null && !secondaryLaunchers.isEmpty() ){
                                        secondaryLaunchers.stream().map(launcher -> {
                                            return launcher.getAppName();
                                        }).filter(launcherAppName -> {
                                            // check appName containing any dots (which is the bug)
                                            return launcherAppName.contains(".");
                                        }).forEach(launcherAppname -> {
                                            applyNativeLauncherWorkaround(launcherAppname);
                                        });
                                    }
                                } else {
                                    getLog().info("Skipped workaround for native linux launcher(s).");
                                }
                            }
                        }

                        if( "jnlp".equals(b.getID()) ){
                            if( File.separator.equals("\\") ){
                                // Workaround for "JNLP-generation: path for dependency-lib on windows with backslash"
                                // https://github.com/javafx-maven-plugin/javafx-maven-plugin/issues/182
                                // jnlp-bundler uses RelativeFileSet, and generates system-dependent dividers (\ on windows, / on others)
                                getLog().info("Applying workaround for oracle-jdk-bug since 1.8.0u60 regarding jar-path inside generated JNLP-files.");
                                if( !skipJNLPRessourcePathWorkaround182 ){
                                    fixPathsInsideJNLPFiles();
                                } else {
                                    getLog().info("Skipped workaround for jar-paths jar-path inside generated JNLP-files.");
                                }
                            }

                            // Do sign generated jar-files by calling the packager (this might change in the future,
                            // hopefully when oracle reworked the process inside the JNLP-bundler.
                            // https://github.com/javafx-maven-plugin/javafx-maven-plugin/issues/185
                            if( params.containsKey("jnlp.allPermisions") && Boolean.parseBoolean(String.valueOf(params.get("jnlp.allPermisions"))) ){
                                getLog().info("Signing jar-files referenced inside generated JNLP-files.");
                                if( !skipSigningJarFilesJNLP185 ){
                                    // JavaFX signing using BLOB method will get dropped on JDK 9: "blob signing is going away in JDK9. "
                                    // https://bugs.openjdk.java.net/browse/JDK-8088866?focusedCommentId=13889898#comment-13889898
                                    if( !noBlobSigning ){
                                        getLog().info("Signing jar-files using BLOB method.");
                                        signJarFilesUsingBlobSigning();
                                    } else {
                                        getLog().info("Signing jar-files using jarsigner.");
                                        signJarFiles();
                                    }
                                    if( !skipSizeRecalculationForJNLP185 ){
                                        getLog().info("Fixing sizes of JAR files within JNLP-files");
                                        fixFileSizesWithinGeneratedJNLPFiles();
                                    } else {
                                        getLog().info("Skipped fixing sizes of JAR files within JNLP-files");
                                    }
                                } else {
                                    getLog().info("Skipped signing jar-files referenced inside JNLP-files.");
                                }
                            }
                        }
                    }
                } catch(UnsupportedPlatformException e){
                    // quietly ignored
                } catch(ConfigException e){
                    getLog().info("Skipping " + b.getName() + " because of configuration error " + e.getMessage() + "\nAdvice to Fix: " + e.getAdvice());
                }
            }
            if( !foundBundler ){
                getLog().warn("No bundler found for given id " + bundler + ". Please check your configuration.");
            }
        } catch(RuntimeException e){
            throw new MojoExecutionException("An error occurred while generating native deployment bundles", e);
        } catch(PackagerException ex){
            throw new MojoExecutionException("An error occurred while generating native deployment bundles", ex);
        }
    }

    private void addToMapWhenNotNull(Object value, String key, Map<String, Object> map) {
        if( value == null ){
            return;
        }
        map.put(key, value);
    }

    private void applyNativeLauncherWorkaround(String appName) {
        // check appName containing any dots
        boolean needsWorkaround = appName.contains(".");
        if( !needsWorkaround ){
            return;
        }
        // rename .cfg-file (makes it able to create running applications again, even within installer)
        String newConfigFileName = appName.substring(0, appName.lastIndexOf("."));
        Path appPath = nativeOutputDir.toPath().resolve(appName).resolve("app");
        String configfileExtension = ".cfg";
        Path oldConfigFile = appPath.resolve(appName + configfileExtension);
        try{
            Files.move(oldConfigFile, appPath.resolve(newConfigFileName + configfileExtension), StandardCopyOption.ATOMIC_MOVE);
        } catch(IOException ex){
            getLog().warn("Couldn't rename configfile. Please see issue #124 of the javafx-maven-plugin for further details.", ex);
        }
    }

    private void signJarFilesUsingBlobSigning() throws MojoFailureException, PackagerException, MojoExecutionException {
        checkSigningConfiguration();

        SignJarParams signJarParams = new SignJarParams();
        signJarParams.setVerbose(verbose);
        signJarParams.setKeyStore(keyStore);
        signJarParams.setAlias(keyStoreAlias);
        signJarParams.setStorePass(keyStorePassword);
        signJarParams.setKeyPass(keyPassword);
        signJarParams.setStoreType(keyStoreType);

        signJarParams.addResource(nativeOutputDir, jfxMainAppJarName);

        // add all gathered jar-files as resources so be signed
        getJARFilesFromJNLPFiles().forEach(jarFile -> signJarParams.addResource(nativeOutputDir, jarFile));

        getLog().info("Signing JAR files for webstart bundle");
        getPackagerLib().signJar(signJarParams);
    }

    private void signJarFiles() throws MojoFailureException, PackagerException, MojoExecutionException {
        checkSigningConfiguration();

        AtomicReference<MojoExecutionException> exception = new AtomicReference<>();
        getJARFilesFromJNLPFiles().stream().map(relativeJarFilePath -> new File(nativeOutputDir, relativeJarFilePath)).forEach(jarFile -> {
            try{
                // only sign when there wasn't already some problem
                if( exception.get() == null ){
                    signJar(jarFile.getAbsoluteFile());
                }
            } catch(MojoExecutionException ex){
                // rethrow later (same trick is done inside apache-tomee project ;D)
                exception.set(ex);
            }
        });
        if( exception.get() != null ){
            throw exception.get();
        }
    }

    private void checkSigningConfiguration() throws MojoFailureException {
        if( !keyStore.exists() ){
            throw new MojoFailureException("Keystore does not exist, use 'jfx:generate-key-store' command to make one (expected at: " + keyStore + ")");
        }

        if( keyStoreAlias == null || keyStoreAlias.isEmpty() ){
            throw new MojoFailureException("A 'keyStoreAlias' is required for signing JARs");
        }

        if( keyStorePassword == null || keyStorePassword.isEmpty() ){
            throw new MojoFailureException("A 'keyStorePassword' is required for signing JARs");
        }

        if( keyPassword == null ){
            keyPassword = keyStorePassword;
        }
    }

    private List<File> getGeneratedJNLPFiles() {
        List<File> generatedFiles = new ArrayList<>();

        // try-ressource, because walking on files is lazy, resulting in file-handler left open otherwise
        try(Stream<Path> walkstream = Files.walk(nativeOutputDir.toPath())){
            walkstream.forEach(fileEntry -> {
                File possibleJNLPFile = fileEntry.toFile();
                String fileName = possibleJNLPFile.getName();
                if( fileName.endsWith(".jnlp") ){
                    generatedFiles.add(possibleJNLPFile);
                }
            });
        } catch(IOException ignored){
            // NO-OP
        }

        return generatedFiles;
    }

    private List<String> getJARFilesFromJNLPFiles() {
        List<String> jarFiles = new ArrayList<>();
        getGeneratedJNLPFiles().stream().map(jnlpFile -> jnlpFile.toPath()).forEach(jnlpPath -> {
            try{
                List<String> allLines = Files.readAllLines(jnlpPath);
                allLines.stream().filter(line -> line.trim().startsWith("<jar href=")).forEach(line -> {
                    String jarFile = line.replaceAll(JNLP_JAR_PATTERN, "$2");
                    jarFiles.add(jarFile.substring(1, jarFile.length() - 1));
                });
            } catch(IOException ignored){
                // NO-OP
            }
        });
        return jarFiles;
    }

    private Map<String, Long> getFileSizes(List<String> files) {
        final Map<String, Long> fileSizes = new HashMap<>();
        files.stream().forEach(relativeFilePath -> {
            File file = new File(nativeOutputDir, relativeFilePath);
            // add the size for each file
            fileSizes.put(relativeFilePath, file.length());
        });
        return fileSizes;
    }

    private void fixPathsInsideJNLPFiles() {
        List<File> generatedJNLPFiles = getGeneratedJNLPFiles();
        Pattern pattern = Pattern.compile(JNLP_JAR_PATTERN);
        generatedJNLPFiles.forEach(file -> {
            try{
                List<String> allLines = Files.readAllLines(file.toPath());
                List<String> newLines = new ArrayList<>();
                allLines.stream().forEach(line -> {
                    if( line.matches(JNLP_JAR_PATTERN) ){
                        // get jar-file
                        Matcher matcher = pattern.matcher(line);
                        matcher.find();
                        String rawJarName = matcher.group(2);
                        // replace \ with /
                        newLines.add(line.replace(rawJarName, rawJarName.replaceAll("\\\\", "\\/")));
                    } else {
                        newLines.add(line);
                    }
                });
                Files.write(file.toPath(), newLines, StandardOpenOption.TRUNCATE_EXISTING);
            } catch(IOException ignored){
                // NO-OP
            }
        });
    }

    private void fixFileSizesWithinGeneratedJNLPFiles() {
        // after signing, we have to adjust sizes, because they have changed (since they are modified with the signature)
        List<String> jarFiles = getJARFilesFromJNLPFiles();
        Map<String, Long> newFileSizes = getFileSizes(jarFiles);
        List<File> generatedJNLPFiles = getGeneratedJNLPFiles();
        Pattern pattern = Pattern.compile(JNLP_JAR_PATTERN);
        generatedJNLPFiles.forEach(file -> {
            try{
                List<String> allLines = Files.readAllLines(file.toPath());
                List<String> newLines = new ArrayList<>();
                allLines.stream().forEach(line -> {
                    if( line.matches(JNLP_JAR_PATTERN) ){
                        // get jar-file
                        Matcher matcher = pattern.matcher(line);
                        matcher.find();
                        String rawJarName = matcher.group(2);
                        String jarName = rawJarName.substring(1, rawJarName.length() - 1);
                        if( newFileSizes.containsKey(jarName) ){
                            // replace old size with new one
                            newLines.add(line.replace(matcher.group(4), "\"" + newFileSizes.get(jarName) + "\""));
                        } else {
                            newLines.add(line);
                        }
                    } else {
                        newLines.add(line);
                    }
                });
                Files.write(file.toPath(), newLines, StandardOpenOption.TRUNCATE_EXISTING);
            } catch(IOException ignored){
                // NO-OP
            }
        });
    }

    private void signJar(File jarFile) throws MojoExecutionException {
        List<String> command = new ArrayList<>();
        command.add("jarsigner");
        command.add("-strict");
        command.add("-keystore");
        command.add(keyStore.getAbsolutePath());
        command.add("-storepass");
        command.add(keyStorePassword);
        command.add("-keypass");
        command.add(keyPassword);
        command.add(jarFile.getAbsolutePath());
        command.add(keyStoreAlias);
        if( verbose ){
            command.add("-verbose");
        }

        try{
            ProcessBuilder pb = new ProcessBuilder()
                    .inheritIO()
                    .directory(project.getBasedir())
                    .command(command);
            Process p = pb.start();
            p.waitFor();
            if( p.exitValue() != 0 ){
                throw new MojoExecutionException("Signing jar using jarsigner wasn't successful! Please check build-log.");
            }
        } catch(IOException | InterruptedException ex){
            throw new MojoExecutionException("There was an exception while signing jar-file: " + jarFile.getAbsolutePath(), ex);
        }
    }
}
