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
import com.sun.javafx.tools.packager.SignJarParams;
import com.sun.javafx.tools.packager.bundlers.Bundler;
import org.apache.maven.model.Build;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;

/**
 * <p>Generates web deployment bundles (applet and webstart). This Mojo simply wraps the JavaFX packaging tools
 * so it has all the problems and limitations of those tools. Currently you get both the webstart and applet outputs
 * whether you want both or not.</p>
 *
 * <p>This Mojo will automatically try and sign all JARs included in the deployment bundle if 'all-permissions' are
 * requested. If permissions are not requested, no signing will take place. The keystore parameters of this Mojo are
 * only used in the case where signing is needed, and in that case some are required.</p>
 *
 * <p>As a general comment, these web deployment techniques have been pretty error prone in the newer releases of Java.
 * They are also not ideal if the user doesn't have Java already installed as the JRE installation process is very user
 * unfriendly. Additionally, these web deployment methods are the root of all the security problems that have been
 * giving Java a bad name recently. For all these reasons and more, I'd highly recommend moving away from these
 * deployment approaches in favour of native deployment bundles or just plain old JARs.</p>
 *
 * <p>For detailed information on generating web bundles it is best to first read through the official documentation
 * on the JavaFX packaging tools.</p>
 *
 * @goal web
 * @phase package
 * @execute goal="jar"
 */
public class WebMojo extends AbstractJfxToolsMojo {

    /**
     * The vendor (i.e. you) to include in the deployment information.
     *
     * @parameter property="project.organization.name"
     * @required
     */
    protected String vendor;

    /**
     * <p>The output directory that the web bundle is to be built into. Both the webstart and applet bundle are
     * generated into the same output directory and share the same JNLP and JAR files.</p>
     *
     * @parameter default-value="${project.build.directory}/jfx/web"
     */
    protected File webOutputDir;

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
     */
    protected String preLoader;

    /**
     * <p>Set this to true if your app needs to break out of the standard web sandbox and do more powerful functions.</p>
     *
     * <p>By setting this value, you are implicitly saying that your app needs to be signed. As such, this Mojo will
     * automatically attempt to sign your JARs if this is set, and in this case the various keyStore parameters need to
     * be set correctly and a keyStore must be present. Use the generate-key-store Mojo to generate a local keyStore for
     * testing.</p>
     *
     * <p>If you are using FXML you will need to set this value to true.</p>
     *
     * @parameter default-value=false
     */
    protected boolean allPermissions;

    /**
     * @parameter default-value=800
     */
    protected int width;

    /**
     * @parameter default-value=600
     */
    protected int height;

    /**
     * @parameter
     */
    protected String embeddedWidth;

    /**
     * @parameter
     */
    protected String embeddedHeight;


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


    public void execute() throws MojoExecutionException, MojoFailureException {

        getLog().info("Building Web deployment bundles");

        try {
            Build build = project.getBuild();

            DeployParams deployParams = new DeployParams();
            deployParams.setVerbose(verbose);

            deployParams.setAppName(build.getFinalName());
            deployParams.setVersion(project.getVersion());
            deployParams.setVendor(vendor);

            deployParams.setApplicationClass(mainClass);
            deployParams.setOutdir(webOutputDir);
            deployParams.setOutfile(build.getFinalName());

            deployParams.setNeedMenu(needMenu);
            deployParams.setNeedShortcut(needShortcut);

            deployParams.addResource(jfxAppOutputDir, jfxMainAppJarName);
            deployParams.addResource(jfxAppOutputDir, "lib");

            deployParams.setAllPermissions(allPermissions);

            deployParams.setPreloader(preLoader);

            deployParams.setWidth(width);
            deployParams.setHeight(height);

            String embeddedWidth = this.embeddedWidth != null ? this.embeddedWidth : String.valueOf(width);
            String embeddedHeight = this.embeddedHeight != null ? this.embeddedHeight : String.valueOf(height);
            deployParams.setEmbeddedDimensions(embeddedWidth, embeddedHeight);

            // turn off native bundles for this web build
            //noinspection deprecation
            deployParams.setBundleType(Bundler.BundleType.NONE);

            getPackagerLib().generateDeploymentPackages(deployParams);

            // if permissions have been requested then we need to sign the JAR file
            if (allPermissions) {

                getLog().info("Permissions requested, signing JAR files for webstart bundle");

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

                SignJarParams signJarParams = new SignJarParams();
                signJarParams.setVerbose(verbose);
                signJarParams.setKeyStore(keyStore);
                signJarParams.setAlias(keyStoreAlias);
                signJarParams.setStorePass(keyStorePassword);
                signJarParams.setKeyPass(keyPassword);
                signJarParams.setStoreType(keyStoreType);

                signJarParams.addResource(webOutputDir, jfxMainAppJarName);
                signJarParams.addResource(webOutputDir, "lib");

                getPackagerLib().signJar(signJarParams);
            }

        } catch (PackagerException e) {
            throw new MojoExecutionException("An error occurred while generating web deployment bundle", e);
        }
    }
}
