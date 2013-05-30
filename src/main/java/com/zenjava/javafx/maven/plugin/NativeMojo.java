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

import com.sun.javafx.tools.packager.DeployParams;
import com.sun.javafx.tools.packager.PackagerException;
import com.sun.javafx.tools.packager.bundlers.Bundler;
import org.apache.maven.model.Build;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import java.io.File;
import java.util.List;
import java.util.Map;

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
     * @parameter expression="${project.organization.name}"
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
     * @parameter expression="${project.build.directory}/jfx/native"
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
     * @parameter expression="${bundleType}" default-value="ALL"
     */
    private Bundler.BundleType bundleType;

    /**
     * Properties passed to the Java Virtual Machine when the application is started (i.e. these properties are system
     * properties of the JVM bundled in the native distribution and used to run the application once installed).
     *
     * @parameter
     */
    private Map<String, String> jvmProperties;

    /**
     * Optional command line arguments passed to the application when it is started. These will be included in the
     * native bundle that is generated and will be accessible via the main(String[] args) method on the main class that
     * is launched at runtime.
     *
     * @parameter
     */
    private List<String> jvmArgs;

    /**
     * The release version as passed to the native installer. It would be nice to just use the project's version number
     * but this must be a fairly traditional version string (like '1.34.5') with only numeric characters and dot
     * separators, otherwise the JFX packaging tools bomb out. We default to 1.0 in case you can't be bothered to set
     * a version and don't really care.
     *
     * @parameter expression="1.0"
     */
    private String nativeReleaseVersion;

    /**
     * A custom class that can act as a Pre-Loader for your app. The Pre-Loader is run before anything else and is
     * useful for showing splash screens or similar 'progress' style windows. For more information on Pre-Loaders, see
     * the official JavaFX packaging documentation.
     *
     * @parameter
     */
    protected String preLoader;

    public void execute() throws MojoExecutionException, MojoFailureException {

        getLog().info("Building Native Installers");

        try {
            Build build = project.getBuild();

            DeployParams deployParams = new DeployParams();
            deployParams.setVerbose(verbose);

            if (identifier != null) {
                deployParams.setId(identifier);
            }

            deployParams.setBundleType(bundleType);
            deployParams.setAppName(build.getFinalName());
            deployParams.setVersion(nativeReleaseVersion);
            deployParams.setVendor(vendor);

            deployParams.setApplicationClass(mainClass);

            if (jvmProperties != null) {
                for (String key : jvmProperties.keySet()) {
                    deployParams.addJvmProperty(key, jvmProperties.get(key));
                }
            }

            if (jvmArgs != null) {
                for (String arg : jvmArgs) {
                    deployParams.addJvmArg(arg);
                }
            }

            deployParams.setOutdir(nativeOutputDir);
            deployParams.setOutfile(build.getFinalName());
            deployParams.setPreloader(preLoader);
            deployParams.addResource(jfxAppOutputDir, jfxMainAppJarName);
            deployParams.addResource(jfxAppOutputDir, "lib");

            getPackagerLib().generateDeploymentPackages(deployParams);

            // delete the JNLP and webstart generated files as we didn't ask for them
            new File(nativeOutputDir, build.getFinalName() + ".html").delete();
            new File(nativeOutputDir, build.getFinalName() + ".jnlp").delete();

        } catch (PackagerException e) {
            throw new MojoExecutionException("An error occurred while generating native deployment bundles", e);
        }
    }
}
