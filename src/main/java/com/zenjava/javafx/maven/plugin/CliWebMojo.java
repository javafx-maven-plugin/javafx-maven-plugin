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
 * Generates web deployment bundles (applet and webstart). This Mojo simply wraps the JavaFX packaging tools
 * so it has all the problems and limitations of those tools. Currently you get both the webstart and applet outputs
 * whether you want both or not.
 * <p>
 * This Mojo will automatically try and sign all JARs included in the deployment bundle if 'all-permissions' are
 * requested. If permissions are not requested, no signing will take place. The keystore parameters of this Mojo are
 * only used in the case where signing is needed, and in that case some are required.
 * <p>
 * As a general comment, these web deployment techniques have been pretty error prone in the newer releases of Java.
 * They are also not ideal if the user doesn't have Java already installed as the JRE installation process is very user
 * unfriendly. Additionally, these web deployment methods are the root of all the security problems that have been
 * giving Java a bad name recently. For all these reasons and more, I'd highly recommend moving away from these
 * deployment approaches in favour of native deployment bundles or just plain old JARs.
 * <p>
 * For detailed information on generating web bundles it is best to first read through the official documentation
 * on the JavaFX packaging tools.
 * <p>
 * Note: this will be removed in some time, because Oracle created a bundler with id 'jnlp'.
 *
 * @goal web
 * @execute goal="jar"
 */
@SuppressWarnings("deprecation")
public class CliWebMojo extends WebMojo {
    // NO-OP
}
