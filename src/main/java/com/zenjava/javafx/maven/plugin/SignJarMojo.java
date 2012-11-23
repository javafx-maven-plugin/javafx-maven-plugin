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
 * @goal sign-jar
 * @phase package
 * @execute goal="build-jar"
 */
public class SignJarMojo extends AbstractJfxPackagingMojo {

    /**
     * @parameter
     */
    protected File keyStore;

    /**
     * @parameter
     * @required
     */
    protected String keyStoreAlias;

    /**
     * @parameter
     * @required
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


    public void execute() throws MojoExecutionException, MojoFailureException {

        File targetJarFile = getTargetJarFile();
        getLog().info("Signing JAR file '" + targetJarFile + "'");

        if (keyStore == null) {
            keyStore = new File(project.getBasedir(), "/src/main/deploy/keystore.jks");
        }
        if (!keyStore.exists()) {
            throw new MojoFailureException("No keystore file found at: " + keyStore);
        }

        if (keyPassword == null) {
            keyPassword = keyStorePassword;
        }

        JfxToolsWrapper jfxTools = getJfxToolsWrapper();
        jfxTools.signJar(targetJarFile, keyStore, keyStoreAlias, keyStorePassword, keyPassword, keyStoreType);
    }
}
