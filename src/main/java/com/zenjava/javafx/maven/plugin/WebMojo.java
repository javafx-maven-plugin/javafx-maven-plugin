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
import com.sun.javafx.tools.packager.PackagerLib;
import com.sun.javafx.tools.packager.SignJarParams;
import com.sun.javafx.tools.packager.bundlers.Bundler;
import org.apache.maven.model.Build;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;

/**
 * @goal web
 * @phase package
 * @execute goal="jar"
 */
public class WebMojo extends AbstractJfxToolsMojo {

    /**
     * @parameter expression="${mainClass}"
     * @required
     */
    protected String mainClass;

    /**
     * @parameter expression="${project.organization.name}"
     * @required
     */
    protected String vendor;

    /**
     * @parameter expression="${project.build.directory}/jfx/web"
     */
    protected File webOutputDir;

    /**
     * @parameter default-value=false
     */
    protected boolean needShortcut;

    /**
     * @parameter default-value=false
     */
    protected boolean needMenu;

    /**
     * @parameter
     */
    protected String preLoader;

    /**
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
     * @parameter expression="src/main/deploy/keystore.jks"
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


    public void execute(PackagerLib packagerLib) throws MojoExecutionException, MojoFailureException {

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
            deployParams.setBundleType(Bundler.BundleType.NONE);

            packagerLib.generateDeploymentPackages(deployParams);

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

                packagerLib.signJar(signJarParams);
            }

        } catch (PackagerException e) {
            throw new MojoExecutionException("An error occurred while generating web deployment bundle", e);
        }
    }
}
