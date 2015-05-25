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
import java.util.Set;

/**
 * @goal build-native
 */
public class NativeMojo extends AbstractJfxToolsMojo {

    /**
     * Used as the 'id' of the application, and is used as the CFBundleDisplayName on Mac. See the official JavaFX
     * Packaging tools documentation for other information on this.
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
     * <p>The output directory that the native bundles are to be built into. This will be the base directory only as the
     * JavaFX packaging tools use sub-directories that can't be customised. Generally just have a rummage through the
     * sub-directories until you find what you are looking for.</p>
     *
     * <p>This defaults to 'target/jfx/native' and the interesting files are usually under 'bundles'.</p>
     *
     * @parameter default-value="${project.build.directory}/jfx/native"
     */
    protected File nativeOutputDir;

    /**
     * <p>A magic parameter used by the underlying JavaFX packaging tools to specify which types of native bundles you
     * want built. On the whole quite confusing and not overly useful as you are limited to the native installer options
     * of your OS and the tools you have installed. Furthermore the terms used as the 'bundleType' options rarely relate
     * directly back to the options you have available to you.</p>
     *
     * <p>By default this will be set to 'ALL' which is usually the easiest and the safest. You will end up with the
     * native bundles for your OS, based on whatever tools you have installed. If you want to get more fancy than that
     * then you are probably best to read the official JavaFX packaging tool documentation for more info. </p>
     *
     * @parameter property="bundleType" default-value="ALL"
     */
    private String bundleType;

    /**
     * <p>Specify the used bundler found by selected bundleType. May not be installed your OS and will fail in that case.</p>
     * 
     * <p>By default this will be set to 'ALL', depending on your installed OS following values are possible for installers: </p>
     * <ul>
     *     <li>exe <i>(Microsoft Windows EXE Installer, via InnoIDE)</i></li>
     *     <li>msi <i>(Microsoft Windows MSI Installer, via WiX)</i></li>
     *     <li>deb <i>(Linux Debian Bundle)</i></li>
     *     <li>rpm <i>(Redhat Package Manager (RPM) bundler)</i></li>
     *     <li>dmg <i>(Mac DMG Installer Bundle)</i></li>
     *     <li>pkg <i>(Mac PKG Installer Bundle)</i></li>
     *     <li>mac.appStore <i>(Creates a binary bundle ready for deployment into the Mac App Store)</i></li>
     * </ul>
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
     * JVM Flags to be passed into the JVM at invocation time.  These are the arguments to the left of the main class
     * name when launching Java on the command line.  For example:
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
     *
     * These options are user overridable for the value part of the entry via user preferences.  The key and the value
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
     * A list of bundler arguments.  The particular keys and the meaning of their values are dependent on the bundler
     * that is reading the arguments.  Any argument not recognized by a bundler is silently ignored, so that arguments
     * that are specific to a specific bundler (for example, a Mac OS X Code signing key name) can be configured and
     * ignored by bundlers that don't use the particular argument.
     *
     * If there are bundle arguments that override other fields in the configuration, then it is an execution error.
     *
     * @parameter
     */
    protected Map<String, String> bundleArguments;

    /**
     * The name of the JavaFX packaged executable to be built into the 'native/bundles' directory. By default this will
     * be the finalName as set in your project. Change this if you want something nicer.
     *
     * @parameter default-value="${project.build.finalName}"
     */
    protected String appName;

    /**
     * A description used within generated JNLP-file.
     *
     * @parameter default-value="${project.description}"
     */
    protected String description;

    /**
     * A title used within generated JNLP-file.
     *
     * @parameter default-value="${project.name}"
     */
    protected String title;
    
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

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if( jfxCallFromCLI ){
            getLog().info("call from CLI - skipping creation of Native Installers");
            return;
        }

        //noinspection deprecation
        if( "NONE".equalsIgnoreCase(bundleType) ){
            return;
        }

        getLog().info("Building Native Installers");

        try {
            Map<String, ? super Object> params = new HashMap<>();


            params.put(StandardBundlerParam.VERBOSE.getID(), verbose);

            if (identifier != null) {
                params.put(StandardBundlerParam.IDENTIFIER.getID(), identifier);
            }

            params.put(StandardBundlerParam.APP_NAME.getID(), appName);
            params.put(StandardBundlerParam.VERSION.getID(), nativeReleaseVersion);
            params.put(StandardBundlerParam.VENDOR.getID(), vendor);
            params.put(StandardBundlerParam.TITLE.getID(), title);
            params.put(StandardBundlerParam.DESCRIPTION.getID(), description);
            params.put(StandardBundlerParam.SHORTCUT_HINT.getID(), needShortcut);
            params.put(StandardBundlerParam.MENU_HINT.getID(), needMenu);
            params.put(StandardBundlerParam.MAIN_CLASS.getID(), mainClass);

            if (jvmProperties != null) {
                Map<String, String> jvmProps = new HashMap<>();
                for (String key : jvmProperties.keySet()) {
                    jvmProps.put(key, jvmProperties.get(key));
                }
                params.put(StandardBundlerParam.JVM_PROPERTIES.getID(), jvmProps);
            }

            if (jvmArgs != null) {
                List<String> jvmOptions = new ArrayList<>();
                for (String arg : jvmArgs) {
                    jvmOptions.add(arg);
                }
                params.put(StandardBundlerParam.JVM_OPTIONS.getID(), jvmOptions);
            }

            if (userJvmArgs != null) {
                Map<String, String> userJvmOptions = new HashMap<>();
                for (String key : userJvmArgs.keySet()) {
                    userJvmOptions.put(key, userJvmArgs.get(key));
                }
                params.put(StandardBundlerParam.USER_JVM_OPTIONS.getID(), userJvmOptions);
            }

            Set<File> resourceFiles = new HashSet<>();

            // bugfix for #83 (by copying additional resources to /jfx/app folder)
            if(additionalAppResources != null && additionalAppResources.exists() ){
                try {
                    Path targetFolder = jfxAppOutputDir.toPath();
                    Path sourceFolder = additionalAppResources.toPath();
                    Files.walkFileTree(additionalAppResources.toPath(), new FileVisitor<Path>() {

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
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            try {
                Files.walk(jfxAppOutputDir.toPath())
                    .forEach(p -> {
                        File f = p.toFile();
                        if (f.isFile()) {
                            getLog().info(String.format("Add %s file to application resources.", p.toFile()));
                            resourceFiles.add(p.toFile());
                        }
                    });
            } catch (IOException e) {
                e.printStackTrace();
            }

            params.put(StandardBundlerParam.APP_RESOURCES.getID(), new RelativeFileSet(jfxAppOutputDir, resourceFiles));

            if (bundleArguments == null) {
            	bundleArguments = new HashMap<>();
            }

            Collection<String> duplicateKeys = new HashSet<>(params.keySet());
            duplicateKeys.retainAll(bundleArguments.keySet());
            if (!duplicateKeys.isEmpty()) {
                throw new MojoExecutionException("The following keys in <bundleArguments> duplicate other settings, please remove one or the other: " + duplicateKeys.toString());
            }

            params.putAll(bundleArguments);

            Bundlers bundlers = Bundlers.createBundlersInstance(); // service discovery?
            boolean foundBundler = false;
            for (Bundler b : bundlers.getBundlers()) {
                try {
                    //noinspection deprecation
                    if (bundleType != null && !"ALL".equalsIgnoreCase(bundleType) && !b.getBundleType().equalsIgnoreCase(bundleType)) {
                        // not this kind of bundler
                        continue;
                    }
                    if (bundler != null && !"ALL".equalsIgnoreCase(bundler) && !bundler.equalsIgnoreCase(b.getID())){
                        // this is not the specified bundler
                        continue;
                    }
                    foundBundler = true;

                    if (b.validate(params)) {
                        b.execute(params, nativeOutputDir);
                    }
                } catch (UnsupportedPlatformException e) {
                    // quietly ignore
                } catch (ConfigException e) {
                    getLog().info("Skipping " + b.getName() + " because of configuration error " + e.getMessage() + "\nAdvice to Fix: " + e.getAdvice());
                }
            }
            if(!foundBundler){
                getLog().warn("No bundler found for given type " + bundleType + ". Please check your configuration.");
            }
        } catch (RuntimeException e) {
            throw new MojoExecutionException("An error occurred while generating native deployment bundles", e);
        }
    }
}
