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
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
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
import java.util.stream.Collectors;

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
     * A magic parameter used by the underlying JavaFX packaging tools to specify which types of native bundles you
     * want built. On the whole quite confusing and not overly useful as you are limited to the native installer options
     * of your OS and the tools you have installed. Furthermore the terms used as the 'bundleType' options rarely relate
     * directly back to the options you have available to you.
     * <p>
     * By default this will be set to 'ALL' which is usually the easiest and the safest. You will end up with the
     * native bundles for your OS, based on whatever tools you have installed. If you want to get more fancy than that
     * then you are probably best to read the official JavaFX packaging tool documentation for more info.
     *
     * @parameter property="bundleType" default-value="ALL"
     * @deprecated will be removed soon, as this is replaced by 'bundlers'-parameter
     *
     */
    @Deprecated
    private String bundleType;

    /**
     * Specify the used bundler found by selected bundleType. May not be installed your OS and will fail in that case.
     *
     * <p>
     * By default this will be set to 'ALL', depending on your installed OS following values are possible for installers:
     * <p>
     * <ul>
     * <li>exe <i>(Microsoft Windows EXE Installer, via InnoIDE)</i></li>
     * <li>msi <i>(Microsoft Windows MSI Installer, via WiX)</i></li>
     * <li>deb <i>(Linux Debian Bundle)</i></li>
     * <li>rpm <i>(Redhat Package Manager (RPM) bundler)</i></li>
     * <li>dmg <i>(Mac DMG Installer Bundle)</i></li>
     * <li>pkg <i>(Mac PKG Installer Bundle)</i></li>
     * <li>mac.appStore <i>(Creates a binary bundle ready for deployment into the Mac App Store)</i></li>
     * </ul>
     *
     * <p>
     * For a full list of available bundlers on your system, call 'mvn jfx:list-bundler' inside your project
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

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if( jfxCallFromCLI ){
            getLog().info("call from CLI - skipping creation of Native Installers");
            return;
        }

        if( "NONE".equalsIgnoreCase(bundleType) ){
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
                        getLog().debug("Adding secondary launcher: " + launcher.getAppName());
                        Map<String, Object> secondaryLauncher = new HashMap<>();
                        addToMapWhenNotNull(launcher.getAppName(), StandardBundlerParam.APP_NAME.getID(), secondaryLauncher);
                        addToMapWhenNotNull(launcher.getMainClass(), StandardBundlerParam.MAIN_CLASS.getID(), secondaryLauncher);
                        addToMapWhenNotNull(launcher.getJfxMainAppJarName(), StandardBundlerParam.MAIN_JAR.getID(), secondaryLauncher);
                        // TODO implement secondary launcher mapping
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

            // bugfix for "bundler not being able to produce native bundle without JRE on windows"
            // https://github.com/javafx-maven-plugin/javafx-maven-plugin/issues/167
            if( isJavaVersion(8) && isAtLeastOracleJavaUpdateVersion(60) ){
                if( !skipNativeLauncherWorkaround167 ){
                    if( params.containsKey("runtime") ){
                        getLog().info("Applying workaround for oracle-jdk-bug since 1.8.0u60");
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
                    //noinspection deprecation
                    if( bundleType != null && !"ALL".equalsIgnoreCase(bundleType) && !b.getBundleType().equalsIgnoreCase(bundleType) ){
                        // not this kind of bundler
                        continue;
                    }
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
                                getLog().info("Applying workaround for oracle-jdk-bug since 1.8.0u40");
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
                    }
                } catch(UnsupportedPlatformException e){
                    // quietly ignored
                } catch(ConfigException e){
                    getLog().info("Skipping " + b.getName() + " because of configuration error " + e.getMessage() + "\nAdvice to Fix: " + e.getAdvice());
                }
            }
            if( !foundBundler ){
                getLog().warn("No bundler found for given type " + bundleType + ". Please check your configuration.");
            }
        } catch(RuntimeException e){
            throw new MojoExecutionException("An error occurred while generating native deployment bundles", e);
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
}
