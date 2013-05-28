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
 * @goal native
 * @phase package
 * @execute goal="jar"
 */
public class NativeMojo extends AbstractJfxToolsMojo {

    /**
     * @parameter expression="${mainClass}"
     * @required
     */
    protected String mainClass;
    
    /**
     * @parameter
     */
    protected String identifier;

    /**
     * @parameter expression="${project.organization.name}"
     * @required
     */
    protected String vendor;

    /**
     * @parameter expression="${project.build.directory}/jfx/native"
     */
    protected File nativeOutputDir;

    /**
     * @parameter expression="${bundleType}" default-value="ALL"
     */
    private String bundleType;

    /**
     * @parameter
     */
    private List<String> jvmArgs;
    
    /**
     * @parameter
     */
    private Map<String, String> jvmProperties;

    /**
     * This must be a fairly traditional version string (like '1.34.5') with only numeric
     * characters and dot separators, otherwise the JFX packaging tools bomb out.
     *
     * @parameter expression="1.0"
     */
    private String nativeReleaseVersion;

    /**
     * @parameter
     */
    protected String preLoader;

    public void execute() throws MojoExecutionException, MojoFailureException {

        getLog().info("Building Native Installers");

        try {
            Build build = project.getBuild();

            DeployParams deployParams = new DeployParams();
            deployParams.setVerbose(verbose);

            deployParams.setBundleType(Bundler.BundleType.valueOf(bundleType));
            deployParams.setAppName(build.getFinalName());
            deployParams.setVersion(nativeReleaseVersion);
            deployParams.setVendor(vendor);
            if (identifier != null) {
                deployParams.setId(identifier);
            }

            deployParams.setApplicationClass(mainClass);
            if (jvmArgs != null) {
                for (String jvmArg : jvmArgs) {
                    deployParams.addJvmArg(jvmArg);
                }
            }
            if (jvmProperties != null) {
                for (String key : jvmProperties.keySet()) {
                    deployParams.addJvmProperty(key, jvmProperties.get(key));
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
