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

import org.apache.maven.model.Build;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.util.FileUtils;

import java.io.*;

import static org.twdata.maven.mojoexecutor.MojoExecutor.*;

/**
 * @goal build-win-batch-script
 * @phase package
 * @execute phase="package"
 * @requiresDependencyResolution
 */
public class BuildWinBatchScriptMojo extends AbstractJfxPackagingMojo {

    /**
     * @parameter
     */
    private File winBatOutputDir;

    /**
     * @parameter
     */
    private File batchFile;


    public void execute() throws MojoExecutionException, MojoFailureException {

        getLog().info("Assembling JavaFX Windows batch file distribution bundle");
        Build build = project.getBuild();

        File outputDir = winBatOutputDir;
        if (outputDir == null) {
            outputDir = new File(build.getDirectory(), "win-bat");
        }
        if (!outputDir.exists() && !outputDir.mkdirs()) {
            throw new MojoExecutionException("Unable to create output directory for windows batch bundle: " + winBatOutputDir);
        }

        copyDependencies(outputDir);

        File jarFile = new File(build.getDirectory(), build.getFinalName() + ".jar");
        try {
            getLog().info("Copying module jar file to output directory: '" + jarFile + "' => '" + outputDir + "'");
            FileUtils.copyFileToDirectoryIfModified(jarFile, outputDir);
        } catch (IOException e) {
            throw new MojoExecutionException("Unable to copy module jar file to output directory: '"
                    + jarFile + "' => '" + outputDir + "'");
        }


        File batchFile = this.batchFile;
        if (batchFile == null) {
            batchFile = new File(outputDir, "run.bat");
        }
        writeBatchFile(batchFile, jarFile);
    }

    protected void copyDependencies(File targetDir) throws MojoExecutionException, MojoFailureException {

        executeMojo(
                plugin(
                        groupId("org.apache.maven.plugins"),
                        artifactId("maven-dependency-plugin"),
                        version("2.5.1")
                ),
                goal("copy-dependencies"),
                configuration(
                        element(name("outputDirectory"), targetDir.getPath())
                ),
                executionEnvironment(
                        project,
                        session,
                        pluginManager
                )
        );
    }

    protected void writeBatchFile(File batchFile, File jarFile) throws MojoExecutionException {

        getLog().info("Writing windows batch file to: " + batchFile);

        try {
            Writer out = new BufferedWriter(new FileWriter(batchFile));
            try {
                out.write("java.exe -cp \"" + jarFile.getName() + ";*\" " + mainClass);
            }
            finally {
                try {
                    out.close();
                } catch (IOException e) {
                    getLog().warn("Unable to close the stream to the batch file", e);
                }
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Unable to write batch file to: " + batchFile, e);
        }
    }
}
