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

/**
 * Builds an executable JAR for the project that has all the trappings needed to run as a JavaFX app. This will
 * include Pre-Launchers and all the other weird and wonderful things that the JavaFX packaging tools allow and/or
 * require.
 * <p>
 * Any runtime dependencies for this project will be included in a separate 'lib' sub-directory alongside the
 * resulting JavaFX friendly JAR. The manifest within the JAR will have a reference to these libraries using the
 * relative 'lib' path so that you can copy the JAR and the lib directory exactly as is and distribute this bundle.
 * <p>
 * The JAR and the 'lib' directory built by this Mojo are used as the inputs to the other distribution bundles. The
 * native and web Mojos for example, will trigger this Mojo first and then will copy the resulting JAR into their own
 * distribution bundles.
 *
 * @goal jar
 * @execute lifecycle="jfxjar" phase="package"
 * @requiresDependencyResolution
 */
public class CliJarMojo extends JarMojo {
    // NO-OP
}
