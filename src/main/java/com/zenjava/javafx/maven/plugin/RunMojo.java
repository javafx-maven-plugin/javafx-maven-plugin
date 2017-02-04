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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 *
 * @goal run
 * @execute goal="jar"
 */
public class RunMojo extends AbstractJfxToolsMojo {

    /**
     * Developing and debugging javafx applications can be difficult, so a lot of
     * tools exists, that need to be injected into the JVM via special parameter
     * (e.g. javassist). To have this being part of the command used to start the
     * application by this MOJO, just set all your parameters here.
     *
     * @parameter property="jfx.runJavaParameter"
     */
    protected String runJavaParameter = null;

    /**
     * While developing, you might need some arguments for your application passed
     * to your execution. To have them being part of the command used to start the
     * application by this MOJO, just set all your parameters here.
     *
     * This fixes issue #176.
     *
     * @see https://github.com/javafx-maven-plugin/javafx-maven-plugin/issues/176
     * @parameter property="jfx.runAppParameter"
     */
    protected String runAppParameter = null;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if( skip ){
            getLog().info("Skipping execution of RunMojo MOJO.");
            return;
        }

        getLog().info("Running JavaFX Application");

        List<String> command = new ArrayList<>();
        command.add(getEnvironmentRelativeExecutablePath() + "java");

        // might be useful for having a custom javassist or debugger integrated in this command
        Optional.ofNullable(runJavaParameter).ifPresent(parameter -> {
            if( !parameter.trim().isEmpty() ){
                command.add(parameter);
            }
        });

        command.add("-jar");
        command.add(getRelativeBinDir().resolve(jfxMainAppJarName).toString());

        // it is possible to have jfx:run pass additional parameters
        // fixes https://github.com/javafx-maven-plugin/javafx-maven-plugin/issues/176
        Optional.ofNullable(runAppParameter).ifPresent(parameter -> {
            if( !parameter.trim().isEmpty() ){
                command.add(parameter);
            }
        });

        try{
            ProcessBuilder pb = new ProcessBuilder()
                    .inheritIO()
                    .directory(jfxAppOutputDir)
                    .command(command);

            if( verbose ){
                getLog().info("Running command: " + String.join(" ", command));
            }

            Process p = pb.start();
            p.waitFor();
            if( p.exitValue() != 0 ){
                throw new MojoExecutionException("There was an exception while executing JavaFX Application. Please check build-log.");
            }
        } catch(IOException | InterruptedException ex){
            throw new MojoExecutionException("There was an exception while executing JavaFX Application.", ex);
        }
    }
}
