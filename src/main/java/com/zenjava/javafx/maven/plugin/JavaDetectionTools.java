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
 *
 * @author Danny Althoff
 */
public class JavaDetectionTools {

    public static boolean isJavaVersion(int oracleJavaVersion) {
        String javaVersion = System.getProperty("java.version");
        return javaVersion.startsWith("1." + oracleJavaVersion);
    }

    public static boolean isAtLeastOracleJavaUpdateVersion(int updateNumber) {
        String javaVersion = System.getProperty("java.version");
        String[] javaVersionSplitted = javaVersion.split("_");
        if( javaVersionSplitted.length <= 1 ){
            return false;
        }
        String javaUpdateVersionRaw = javaVersionSplitted[1];
        // issue #159 NumberFormatException on openjdk (the reported Java version is "1.8.0_45-internal")
        String javaUpdateVersion = javaUpdateVersionRaw.replaceAll("[^\\d]", "");
        return Integer.parseInt(javaUpdateVersion, 10) >= updateNumber;
    }
}
