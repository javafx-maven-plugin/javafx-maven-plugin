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

import com.zenjava.javafx.deploy.webstart.WebstartBundleConfig;
import com.zenjava.javafx.deploy.webstart.WebstartBundler;
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
 * @execute goal="build-jar"
 */
public class BuildWebstartMojo extends AbstractJfxPackagingMojo {

    /**
     * @parameter
     */
    private File webstartOutputDir;

    /**
     * @parameter default-value="launch.jnlp"
     */
    private String jnlpFileName;

    /**
     * @parameter
     */
    private File jnlpTemplate;

    /**
     * @parameter expression="${project.name}"
     * @required
     */
    private String title;

    /**
     * @parameter expression="${project.organization.name}"
     * @required
     */
    private String vendor;

    /**
     * @parameter expression="${project.description}"
     */
    private String description;

    /**
     * @parameter expression="${project.organization.url}"
     */
    private String homepage;

    /**
     * @parameter
     */
    private File icon;

    /**
     * @parameter
     */
    private File splashImage;

    /**
     * @parameter
     */
    private boolean offlineAllowed;

    /**
     * @parameter
     */
    private String jreVersion;

    /**
     * @parameter
     */
    private String jreArgs;

    /**
     * @parameter
     */
    private String jfxVersion;

    /**
     * @parameter default-value="true"
     */
    private boolean buildHtmlFile;

    /**
     * @parameter
     */
    private File htmlTemplate;

    /**
     * @parameter default-value="index.html"
     */
    private String htmlFileName;


    public void execute() throws MojoExecutionException, MojoFailureException {

        Build build = project.getBuild();
        File targetJarFile = getTargetJarFile();

        WebstartBundleConfig config = new WebstartBundleConfig();

        File outputDir = webstartOutputDir;
        if (outputDir == null) {
            outputDir = new File(build.getDirectory(), "webstart");
        }
        config.setOutputDir(outputDir);

        config.setJnlpFileName(jnlpFileName);

        File jnlpTemplate = this.jnlpTemplate;
        if (jnlpTemplate == null) {
            File file = new File(project.getBasedir(), "src/main/deploy/jnlp-template.vm");
            if (file.exists()) {
                jnlpTemplate = file;
            }
        }
        config.setJnlpTemplate(jnlpTemplate);

        config.setTitle(title);
        config.setVendor(vendor);
        config.setDescription(description);
        config.setMainClass(mainClass);
        config.setHomepage(homepage);

        File icon = this.icon;
        if (icon == null) {
            File file = new File(project.getBasedir(), "src/main/deploy/icon.png");
            if (file.exists()) {
                icon = file;
            }
        }
        if (icon != null) {
            getLog().debug("Using icon file at '" + icon + "'");
            config.setIcon(icon.getName());
        }

        File splashImage = this.splashImage;
        if (splashImage == null) {
            File file = new File(project.getBasedir(), "src/main/deploy/splash.jpg");
            if (file.exists()) {
                splashImage = file;
            }
        }
        if (splashImage != null) {
            getLog().debug("Using splash image file at '" + splashImage + "'");
            config.setSplashImage(splashImage.getName());
        }

        config.setOfflineAllowed(offlineAllowed);

        if (jreVersion != null) {
            config.setJreVersion(jreVersion);
        }

        if (jreArgs != null) {
            config.setJreArgs(jreArgs);
        }

        if (jfxVersion != null) {
            config.setJreVersion(jfxVersion);
        }

        String jarFileName = this.jarFileName;
        if (jarFileName == null) {
            jarFileName = targetJarFile.getName();
        }
        config.setJarResources(jarFileName);


        if (buildHtmlFile) {
            config.setBuildHtmlFile(true);
            config.setHtmlFileName(htmlFileName);

            File htmlTemplate = this.htmlTemplate;
            if (htmlTemplate == null) {
                File file = new File(project.getBasedir(), "src/main/deploy/webstart-html-template.vm");
                if (file.exists()) {
                    htmlTemplate = file;
                }
            }
            config.setHtmlTemplate(htmlTemplate);
        }

        bundle(config);

        try {
            FileUtils.copyFileIfModified(targetJarFile, new File(outputDir, jarFileName));
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to copy JAR file into webstart directory", e);
        }

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

    protected void bundle(WebstartBundleConfig config) {

        WebstartBundler bundler = new WebstartBundler(new MavenLog(getLog()));
        bundler.bundle(config);
    }
}
