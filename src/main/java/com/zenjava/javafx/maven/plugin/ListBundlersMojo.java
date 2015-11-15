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

import com.oracle.tools.packager.Bundlers;
import com.oracle.tools.packager.ConfigException;
import com.oracle.tools.packager.UnsupportedPlatformException;
import java.util.HashMap;
import java.util.Map;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 *
 * @author Danny Althoff
 * @goal list-bundlers
 */
public class ListBundlersMojo extends AbstractMojo {

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        Bundlers bundlers = Bundlers.createBundlersInstance();

        getLog().info("Available bundlers:");
        getLog().info("-------------------");
        Map<String, ? super Object> dummyParams = new HashMap<>();
        bundlers.getBundlers().stream().forEach((bundler) -> {
            try{
                bundler.validate(dummyParams);
            } catch(UnsupportedPlatformException ex){
                return;
            } catch(ConfigException ex){
                // NO-OP
            }

            getLog().info("ID: " + bundler.getID());
            getLog().info("Name: " + bundler.getName());
            getLog().info("Description: " + bundler.getDescription());
            getLog().info("");
        });
    }

}
