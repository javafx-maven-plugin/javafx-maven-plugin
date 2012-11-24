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
import com.zenjava.javafx.deploy.ApplicationTemplate;
import com.zenjava.javafx.deploy.ApplicationTemplateProcessor;
import com.zenjava.javafx.maven.plugin.util.MavenLog;
import org.apache.maven.model.Build;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import java.io.File;

/**
 * @goal build-win-bat-bundle
 * @phase package
 * @execute phase="package"
 * @requiresDependencyResolution
 */
public class BuildWinBatBundleMojo extends AbstractBundleMojo {

    /**
     * @parameter
     */
    protected File winBatOutputDir;

    /**
     * @parameter
     */
    protected File winBatFile;

    /**
     * @parameter
     */
    protected File winBatFileTemplate;

    /**
     * @parameter default-value="lib"
     */
    protected String winBatLibDir;

    /**
     * @parameter default-value="run.bat"
     */
    protected String winBatFileName;


    public void execute() throws MojoExecutionException, MojoFailureException {

        Build build = project.getBuild();
        ApplicationProfile appProfile = getApplicationProfile();

        File outputDir = winBatOutputDir;
        if (outputDir == null) {
            outputDir = new File(build.getDirectory(), "win-bat");
        }
        if (!outputDir.exists() && !outputDir.mkdirs()) {
            throw new MojoExecutionException("Unable to create output directory for windows batch bundle: " + winBatOutputDir);
        }
        getLog().info("Assembling JavaFX Windows batch file distribution bundle in " + outputDir);

        copyAllJarsAndUpdateProfile(outputDir, winBatLibDir, appProfile);

        ApplicationTemplateProcessor templateProcessor = new ApplicationTemplateProcessor(new MavenLog(getLog()));

        ApplicationTemplate batFileTemplate;
        if (this.winBatFileTemplate == null) {
            File file = new File(project.getBasedir(), "src/main/deploy/win-bat-template.vm");
            if (file.exists()) {
                batFileTemplate = new ApplicationTemplate(file.getPath());
            } else {
                batFileTemplate = ApplicationTemplate.DEFAULT_WIN_BATCH_SCRIPT_TEMPLATE;
            }
        } else {
            if (this.winBatFileTemplate.exists()) {
                batFileTemplate = new ApplicationTemplate(this.winBatFileTemplate.getPath());
            } else {
                throw new MojoFailureException("The specified windows 'bat file' template does not exist: " + this.winBatFileTemplate);
            }
        }
        templateProcessor.processTemplate(batFileTemplate, appProfile, new File(outputDir, winBatFileName));
    }
}
