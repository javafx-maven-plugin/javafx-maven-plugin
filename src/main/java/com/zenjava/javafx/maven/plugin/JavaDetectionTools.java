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
 * @author Danny Althoff
 */
public class JavaDetectionTools {

    public static final boolean IS_JAVA_8 = isJavaVersion(8);

    public static final boolean IS_JAVA_9_AND_BEYOND = !IS_JAVA_8 && (isJavaVersion(9) || isJavaVersion(9, true) || isJavaVersion(10) || isJavaVersion(10, true) || isJavaVersion(11) || isJavaVersion(11, true));

    private JavaDetectionTools() {
        // utility class
    }

    public static boolean isJavaVersion(int oracleJavaVersion, boolean noVersionOne) {
        String javaVersion = System.getProperty("java.version");
        if( noVersionOne ){
            return javaVersion.startsWith(String.valueOf(oracleJavaVersion));
        }
        return javaVersion.startsWith("1." + oracleJavaVersion);
    }

    public static boolean isJavaVersion(int oracleJavaVersion) {
        return isJavaVersion(oracleJavaVersion, false);
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
