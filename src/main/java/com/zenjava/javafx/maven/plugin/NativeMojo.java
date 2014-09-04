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
import org.apache.maven.model.Build;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * <p>Generates native deployment bundles (MSI, EXE, DMG, RPG, etc). This Mojo simply wraps the JavaFX packaging tools
 * so it has all the problems and limitations of those tools. Most importantly, this will only generate a native bundle
 * for the platform you are building on (e.g. if you're on Windows you will get an MSI and an EXE). Additionally you
 * need to first download and install the 3rd-party tools that the JavaFX packaging tools require (e.g. Wix, Inno,
 * etc).</p>
 *
 * <p>For detailed information on generating native packages it is best to first read through the official documentation
 * on the JavaFX packaging tools.</p>
 *
 * @goal native
 * @phase package
 * @execute goal="jar"
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
     *     <jvmArgs>
     *         <jvmArg>-Xmx8G</jvmArg>
     *     </jvmArgs>
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
     * A custom class that can act as a Pre-Loader for your app. The Pre-Loader is run before anything else and is
     * useful for showing splash screens or similar 'progress' style windows. For more information on Pre-Loaders, see
     * the official JavaFX packaging documentation.
     *
     * @parameter
     * @deprecated
     */
    protected String preLoader;

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



    public void execute() throws MojoExecutionException, MojoFailureException {

        //noinspection deprecation
        if ("NONE".equals(bundleType)) return;

        getLog().info("Building Native Installers");

        try {
            Build build = project.getBuild();

            Map<String, ? super Object> params = new HashMap<>();


            params.put(StandardBundlerParam.VERBOSE.getID(), verbose);

            if (identifier != null) {
                params.put(StandardBundlerParam.IDENTIFIER.getID(), identifier);
            }

            params.put(StandardBundlerParam.APP_NAME.getID(), appName);
            params.put(StandardBundlerParam.VERSION.getID(), nativeReleaseVersion);
            params.put(StandardBundlerParam.VENDOR.getID(), vendor);
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
                for (String key : jvmProperties.keySet()) {
                    userJvmOptions.put(key, jvmProperties.get(key));
                }
                params.put(StandardBundlerParam.USER_JVM_OPTIONS.getID(), userJvmOptions);
            }

            Set<File> resourceFiles = new HashSet<>();

            resourceFiles.add(new File(jfxAppOutputDir, jfxMainAppJarName));

            File libDir = new File(jfxAppOutputDir, "lib");
            if (libDir.exists() && libDir.list().length > 0) {
                try {
                    Files.walk(libDir.toPath())
                        .forEach(p -> {
                            File f = p.toFile();
                            System.out.println(p.toFile());
                            if (f.isFile()) {
                                resourceFiles.add(p.toFile());
                            }
                        });
                } catch (IOException e) {
                    e.printStackTrace();
                }
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
            for (Bundler b : bundlers.getBundlers()) {
                Map<String, ? super Object> localParams = new HashMap<>(params);
                try {
                    //noinspection deprecation
                    if (bundleType != null && !"ALL".equals(bundleType) && !b.getBundleType().equals(bundleType)) {
                        // not this kind of bundler
                        return;
                    }

                    if (b.validate(params)) {
                        b.execute(params, nativeOutputDir);
                    }
                } catch (UnsupportedPlatformException e) {
                    // quietly ignore
                } catch (ConfigException e) {
                    getLog().info("Skipping " + b.getName() + " because of configuration error " + e.getMessage() + "\nAdvice to Fix: " + e.getAdvice());
                }
            }
        } catch (RuntimeException e) {
            throw new MojoExecutionException("An error occurred while generating native deployment bundles", e);
        }
    }
}
