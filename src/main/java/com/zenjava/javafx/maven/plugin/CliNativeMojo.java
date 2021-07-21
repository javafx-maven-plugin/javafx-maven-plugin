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
 * Generates native deployment bundles (MSI, EXE, DMG, RPG, etc). This Mojo simply wraps the JavaFX packaging tools
 * so it has all the problems and limitations of those tools. Most importantly, this will only generate a native bundle
 * for the platform you are building on (e.g. if you're on Windows you will get an MSI and an EXE). Additionally you
 * need to first download and install the 3rd-party tools that the JavaFX packaging tools require (e.g. Wix, Inno,
 * etc).
 * <p>
 * For detailed information on generating native packages it is best to first read through the official documentation
 * on the JavaFX packaging tools.
 *
 * @goal native
 * @execute goal="jar"
 */
public class CliNativeMojo extends NativeMojo {
    // NO-OP
}
