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

import com.zenjava.javafx.deploy.ApplicationProfile;
import com.zenjava.javafx.maven.plugin.util.JfxToolsWrapper;
import org.apache.maven.model.Build;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import java.io.File;

/**
 * @goal build-native
 * @phase package
 * @execute goal="build-jar"
 * @requiresDependencyResolution
 */
public class BuildNativeMojo extends AbstractBundleMojo {

    /**
     * @parameter
     */
    protected String executableJarFileName;

    /**
     * @parameter
     */
    protected File nativeOutputDir;

    /**
     * @parameter expression="${bundleType}" default-value="ALL"
     */
    private String bundleType;

    public void execute() throws MojoExecutionException, MojoFailureException {

        getLog().info("Building Native Installers");

        ApplicationProfile appProfile = getApplicationProfile();
        Build build = project.getBuild();

        File outputDir = nativeOutputDir;
        if (outputDir == null) {
            outputDir = new File(build.getDirectory());
        }
        //copyAllJarsAndUpdateProfile(outputDir, null, appProfile);

        JfxToolsWrapper jfxTools = getJfxToolsWrapper();

        String jarName = executableJarFileName;
        if (jarName == null) {
            jarName = build.getFinalName() + "-jfx.jar";
        }

        jfxTools.generateDeploymentPackages(outputDir,
                new String[] { jarName },
                bundleType,
                build.getFinalName(),
                project.getName(),
                project.getVersion(),
                project.getOrganization() != null ? project.getOrganization().getName() : "Unknown JavaFX Developer",
                mainClass);
    }

}
