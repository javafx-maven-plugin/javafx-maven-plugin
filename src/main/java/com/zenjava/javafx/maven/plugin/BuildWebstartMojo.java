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
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.IOException;

/**
 * @goal build-webstart
 * @phase package
 * @execute phase="package"
 * @requiresDependencyResolution
 */
public class BuildWebstartMojo extends AbstractBundleMojo {

    /**
     * @parameter
     */
    private File webstartOutputDir;

    /**
     * @parameter default-value="launch.jnlp"
     */
    private String jnlpFileName;

    /**
     * @parameter default-value="lib"
     */
    private String webstartLibDir;

    /**
     * @parameter
     */
    private File jnlpTemplate;


    /**
     * @parameter default-value="true" expression="${buildWebstartHtmlFile}"
     */
    private boolean buildWebstartHtmlFile;

    /**
     * @parameter
     */
    private File webstartHtmlTemplate;

    /**
     * @parameter default-value="index.html"
     */
    private String webstartHtmlFileName;


    public void execute() throws MojoExecutionException, MojoFailureException {

        Build build = project.getBuild();
        ApplicationProfile appProfile = getApplicationProfile();
        ApplicationTemplateProcessor templateProcessor = new ApplicationTemplateProcessor(new MavenLog(getLog()));

        File outputDir = webstartOutputDir;
        if (outputDir == null) {
            outputDir = new File(build.getDirectory(), "webstart");
        }

        // copy runtime dependencies and HTML resources into target directory

        copyAllJarsAndUpdateProfile(outputDir, webstartLibDir, appProfile);

        // if permissions have been requested then we need to sign the JAR file
        if (permissions != null && permissions.length > 0) {

            getLog().info("Permissions requested, signing JAR files for webstart bundle");
            signJarFiles(outputDir);
        }

        // create the JNLP file

        ApplicationTemplate jnlpTemplate;
        if (this.jnlpTemplate == null) {
            File file = new File(project.getBasedir(), "src/main/deploy/jnlp-template.vm");
            if (file.exists()) {
                jnlpTemplate = new ApplicationTemplate(file.getPath());
            } else {
                jnlpTemplate = ApplicationTemplate.DEFAULT_JNLP_TEMPLATE;
            }
        } else {
            if (this.jnlpTemplate.exists()) {
                jnlpTemplate = new ApplicationTemplate(this.jnlpTemplate.getPath());
            } else {
                throw new MojoFailureException("The specific JNLP template does not exist: " + this.jnlpTemplate);
            }
        }
        templateProcessor.processTemplate(jnlpTemplate, appProfile, new File(outputDir, jnlpFileName));


        // create the HTML file

        if (buildWebstartHtmlFile) {

            ApplicationTemplate htmlTemplate;
            if (this.webstartHtmlTemplate == null) {
                File file = new File(project.getBasedir(), "src/main/deploy/webstart-html-template.vm");
                if (file.exists()) {
                    htmlTemplate = new ApplicationTemplate(file.getPath());
                } else {
                    htmlTemplate = ApplicationTemplate.DEFAULT_WEBSTART_HTML_TEMPLATE;
                }
            } else {
                if (this.webstartHtmlTemplate.exists()) {
                    htmlTemplate = new ApplicationTemplate(this.webstartHtmlTemplate.getPath());
                } else {
                    throw new MojoFailureException("The specific webstart HTML template does not exist: " + this.webstartHtmlTemplate);
                }
            }
            templateProcessor.processTemplate(htmlTemplate, appProfile, new File(outputDir, webstartHtmlFileName));
        }


        // copy any additional HTML resources into target directory

        if (icon != null && icon.exists()) {
            try {
                FileUtils.copyFileIfModified(icon, new File(outputDir, icon.getName()));
            } catch (IOException e) {
                throw new MojoExecutionException("Failed to copy application icon file into webstart directory", e);
            }
        }

        if (splashImage != null && splashImage.exists()) {
            try {
                FileUtils.copyFileIfModified(splashImage, new File(outputDir, splashImage.getName()));
            } catch (IOException e) {
                throw new MojoExecutionException("Failed to copy splash screen image file into webstart directory", e);
            }
        }
    }
}
