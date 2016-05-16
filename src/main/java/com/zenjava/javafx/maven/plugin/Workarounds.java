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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.apache.maven.plugin.logging.Log;

/**
 *
 * @author Danny Althoff
 */
public class Workarounds {

    private static final String JNLP_JAR_PATTERN = "(.*)href=(\".*?\")(.*)size=(\".*?\")(.*)";

    private Log logger;
    private File nativeOutputDir;

    public Workarounds(File nativeOutputDir, Log logger) {
        this.logger = logger;
        this.nativeOutputDir = nativeOutputDir;
    }

    public Log getLog() {
        return logger;
    }

    protected void applyNativeLauncherWorkaround(String appName) {
        // check appName containing any dots
        boolean needsWorkaround = appName.contains(".");
        if( !needsWorkaround ){
            return;
        }
        // rename .cfg-file (makes it able to create running applications again, even within installer)
        String newConfigFileName = appName.substring(0, appName.lastIndexOf("."));
        Path appPath = nativeOutputDir.toPath().resolve(appName).resolve("app");
        String configfileExtension = ".cfg";
        Path oldConfigFile = appPath.resolve(appName + configfileExtension);
        try{
            Files.move(oldConfigFile, appPath.resolve(newConfigFileName + configfileExtension), StandardCopyOption.ATOMIC_MOVE);
        } catch(IOException ex){
            getLog().warn("Couldn't rename configfile. Please see issue #124 of the javafx-maven-plugin for further details.", ex);
        }
    }

    protected Map<String, Long> getFileSizes(List<String> files) {
        final Map<String, Long> fileSizes = new HashMap<>();
        files.stream().forEach(relativeFilePath -> {
            File file = new File(nativeOutputDir, relativeFilePath);
            // add the size for each file
            fileSizes.put(relativeFilePath, file.length());
        });
        return fileSizes;
    }

    public void fixFileSizesWithinGeneratedJNLPFiles() {
        // after signing, we have to adjust sizes, because they have changed (since they are modified with the signature)
        List<String> jarFiles = getJARFilesFromJNLPFiles();
        Map<String, Long> newFileSizes = getFileSizes(jarFiles);
        List<File> generatedJNLPFiles = getGeneratedJNLPFiles();
        Pattern pattern = Pattern.compile(JNLP_JAR_PATTERN);
        generatedJNLPFiles.forEach(file -> {
            try{
                List<String> allLines = Files.readAllLines(file.toPath());
                List<String> newLines = new ArrayList<>();
                allLines.stream().forEach(line -> {
                    if( line.matches(JNLP_JAR_PATTERN) ){
                        // get jar-file
                        Matcher matcher = pattern.matcher(line);
                        matcher.find();
                        String rawJarName = matcher.group(2);
                        String jarName = rawJarName.substring(1, rawJarName.length() - 1);
                        if( newFileSizes.containsKey(jarName) ){
                            // replace old size with new one
                            newLines.add(line.replace(matcher.group(4), "\"" + newFileSizes.get(jarName) + "\""));
                        } else {
                            newLines.add(line);
                        }
                    } else {
                        newLines.add(line);
                    }
                });
                Files.write(file.toPath(), newLines, StandardOpenOption.TRUNCATE_EXISTING);
            } catch(IOException ignored){
                // NO-OP
            }
        });
    }

    public List<File> getGeneratedJNLPFiles() {
        List<File> generatedFiles = new ArrayList<>();

        // try-ressource, because walking on files is lazy, resulting in file-handler left open otherwise
        try(Stream<Path> walkstream = Files.walk(nativeOutputDir.toPath())){
            walkstream.forEach(fileEntry -> {
                File possibleJNLPFile = fileEntry.toFile();
                String fileName = possibleJNLPFile.getName();
                if( fileName.endsWith(".jnlp") ){
                    generatedFiles.add(possibleJNLPFile);
                }
            });
        } catch(IOException ignored){
            // NO-OP
        }

        return generatedFiles;
    }

    public List<String> getJARFilesFromJNLPFiles() {
        List<String> jarFiles = new ArrayList<>();
        getGeneratedJNLPFiles().stream().map(jnlpFile -> jnlpFile.toPath()).forEach(jnlpPath -> {
            try{
                List<String> allLines = Files.readAllLines(jnlpPath);
                allLines.stream().filter(line -> line.trim().startsWith("<jar href=")).forEach(line -> {
                    String jarFile = line.replaceAll(JNLP_JAR_PATTERN, "$2");
                    jarFiles.add(jarFile.substring(1, jarFile.length() - 1));
                });
            } catch(IOException ignored){
                // NO-OP
            }
        });
        return jarFiles;
    }

    public void fixPathsInsideJNLPFiles() {
        List<File> generatedJNLPFiles = getGeneratedJNLPFiles();
        Pattern pattern = Pattern.compile(JNLP_JAR_PATTERN);
        generatedJNLPFiles.forEach(file -> {
            try{
                List<String> allLines = Files.readAllLines(file.toPath());
                List<String> newLines = new ArrayList<>();
                allLines.stream().forEach(line -> {
                    if( line.matches(JNLP_JAR_PATTERN) ){
                        // get jar-file
                        Matcher matcher = pattern.matcher(line);
                        matcher.find();
                        String rawJarName = matcher.group(2);
                        // replace \ with /
                        newLines.add(line.replace(rawJarName, rawJarName.replaceAll("\\\\", "\\/")));
                    } else {
                        newLines.add(line);
                    }
                });
                Files.write(file.toPath(), newLines, StandardOpenOption.TRUNCATE_EXISTING);
            } catch(IOException ignored){
                // NO-OP
            }
        });
    }

    public void applyWorkaround205(Map<String, ? super Object> paramsToBundleWith) {
        if( "prop".equals(paramsToBundleWith.get("launcher-cfg-format")) ){
            // property-file format
            // LinuxAppBundler
            // writeCfgFile(p, rootDir);
        } else {
            // INI-file format
            // AbstractImageBundler
            // writeCfgFile(p, new File(rootDir, LinuxAppBundler.getLauncherCfgName(p)), LinuxAppBundler.getRuntimeLocation(p));
        }
        /*
                            private String getRuntimeLocation(Map<String, ? super Object> params) {
                                if (LINUX_RUNTIME.fetchFrom(params) == null) {
                                    return "";
                                } else {
                                    return "$APPDIR/runtime";
                                }
                            }

                            public static String getLauncherCfgName(Map<String, ? super Object> p) {
                                return "app/" + APP_FS_NAME.fetchFrom(p) +".cfg";
                            }
         */
    }

    public void applyWorkaround124(String appName, List<NativeLauncher> secondaryLaunchers) {
        // apply on main launcher
        applyNativeLauncherWorkaround(appName);

        // check on secondary launchers too
        if( secondaryLaunchers != null && !secondaryLaunchers.isEmpty() ){
            secondaryLaunchers.stream().map(launcher -> {
                return launcher.getAppName();
            }).filter(launcherAppName -> {
                // check appName containing any dots (which is the bug)
                return launcherAppName.contains(".");
            }).forEach(launcherAppname -> {
                applyNativeLauncherWorkaround(launcherAppname);
            });
        }
    }

    public void applyWorkaround185(boolean skipSizeRecalculationForJNLP185) {
        if( !skipSizeRecalculationForJNLP185 ){
            getLog().info("Fixing sizes of JAR files within JNLP-files");
            fixFileSizesWithinGeneratedJNLPFiles();
        } else {
            getLog().info("Skipped fixing sizes of JAR files within JNLP-files");
        }
    }

    public void applyWorkaround167(Map<String, Object> params) {
        if( params.containsKey("runtime") ){
            getLog().info("Applying workaround for oracle-jdk-bug since 1.8.0u60 regarding cfg-file-format");
            // the problem is com.oracle.tools.packager.windows.WinAppBundler within createLauncherForEntryPoint-Method
            // it does NOT respect runtime-setting while calling "writeCfgFile"-method of com.oracle.tools.packager.AbstractImageBundler
            // since newer java versions (they added possability to have INI-file-format of generated cfg-file, since 1.8.0_60).
            // Because we want to have backward-compatibility within java 8, we will use parameter-name as hardcoded string!
            // Our workaround: use prop-file-format
            params.put("launcher-cfg-format", "prop");
        }
    }
}
