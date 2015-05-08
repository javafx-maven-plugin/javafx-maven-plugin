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
import java.nio.file.Paths;
import java.util.List;

/**
 * @goal build-web
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
     * Width of the program on execution.
     * 
     * @parameter default-value=800
     */
    protected int width;

    /**
     * Height of the program on execution.
     * 
     * @parameter default-value=600
     */
    protected int height;

    /**
     * Width of the program on execution.
     * 
     * @parameter
     */
    protected String embeddedWidth;

    /**
     * Height of the program on execution.
     * 
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

    /**
     * The list of filenames of user-defined HTML templates.
     * If no files are specified, then the default template will be used.
     * 
     * @parameter
     */
    protected List<File> templates;

    /**
     * The name of the JavaFX packaged executable to be built into the 'web' directory. By default this will
     * be the finalName as set in your project. Change this if you want something nicer.
     *
     * @parameter default-value="${project.build.finalName}"
     */
    protected String appName;

    /**
    * A description of the JavaFX packaged executable. By default this will be the name of your project.
    *
    * @parameter default-value="${project.name}"
    */
   protected String description;

   /**
   * A parameter for specification of the neccessary JRE version. It defaults to '1.8+'.
   *
   * @parameter default-value="1.8+"
   */
  protected String jre;


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
            
            // bugfix for issue #46 "FileNotFoundException: ...\target\jfx\web\lib"
            File libFolder = new File(jfxAppOutputDir, "lib");
            if(libFolder.exists() && libFolder.list().length > 0){
                deployParams.addResource(jfxAppOutputDir, "lib");
            }

            deployParams.setAllPermissions(allPermissions);

            deployParams.setPreloader(preLoader);

            deployParams.setWidth(width);
            deployParams.setHeight(height);

            String embeddedWidth = this.embeddedWidth != null ? this.embeddedWidth : String.valueOf(width);
            String embeddedHeight = this.embeddedHeight != null ? this.embeddedHeight : String.valueOf(height);
            deployParams.setEmbeddedDimensions(embeddedWidth, embeddedHeight);
            
            if (templates != null)
                for (File template : templates) {
//                  final File in = project.getBasedir().toPath().resolve(Paths.get("src", "main", "deploy", "templates", template)).toFile();
//                  final File in = new File(project.getBasedir(), "src/main/deploy/templates/" + template);
                  final File out = new File(webOutputDir, template.getName());
                  getLog().info("Using template: " + template);
                  deployParams.addTemplate(template, out);
                }
              else
                getLog().info("Using default template.");

            // turn off native bundles for this web build
            //noinspection deprecation
            deployParams.setBundleType(Bundler.BundleType.NONE);
            deployParams.setTitle(appName);
            deployParams.setDescription(description);
            deployParams.setJRE(jre);

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
                
                // bugfix for issue #46 "FileNotFoundException: ...\target\jfx\web\lib"
                if(libFolder.exists() && libFolder.list().length > 0){
                    signJarParams.addResource(webOutputDir, "lib");
                }

                getPackagerLib().signJar(signJarParams);
            }

        } catch (PackagerException e) {
            throw new MojoExecutionException("An error occurred while generating web deployment bundle", e);
        }
    }
}
