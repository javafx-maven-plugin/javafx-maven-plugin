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

import com.zenjava.javafx.maven.plugin.util.JfxToolsWrapper;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import java.io.File;

/**
 * @goal build-native
 * @phase package
 * @execute goal="build-jar"
 */
public class BuildNativeMojo extends AbstractJfxPackagingMojo {

    /**
     * @parameter expression="${jarFileName}" default-value="ALL"
     */
    private String bundleType;

    public void execute() throws MojoExecutionException, MojoFailureException {

        getLog().info("Building Native Installers");

        File targetJarFile = getTargetJarFile();

        JfxToolsWrapper jfxTools = getJfxToolsWrapper();
        jfxTools.generateDeploymentPackages(targetJarFile.getParentFile(), targetJarFile.getName(), bundleType,
                project.getBuild().getFinalName(),
                project.getName(),
                project.getVersion(),
                project.getOrganization() != null ? project.getOrganization().getName() : "Unknown JavaFX Developer",
                mainClass);
    }

}
