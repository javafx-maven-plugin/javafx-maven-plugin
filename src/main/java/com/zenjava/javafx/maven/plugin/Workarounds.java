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

import com.oracle.tools.packager.Bundler;
import com.oracle.tools.packager.IOUtils;
import com.oracle.tools.packager.RelativeFileSet;
import com.oracle.tools.packager.StandardBundlerParam;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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

    private static final String CONFIG_FILE_EXTENSION = ".cfg";

    private Log logger;

    private File nativeOutputDir;

    public Workarounds(File nativeOutputDir, Log logger) {
        this.logger = logger;
        this.nativeOutputDir = nativeOutputDir;
    }

    public Log getLog() {
        return logger;
    }

    public boolean isWorkaroundForBug124Needed() {
        return JavaDetectionTools.IS_JAVA_8 && JavaDetectionTools.isAtLeastOracleJavaUpdateVersion(40) || JavaDetectionTools.IS_JAVA_9_AND_BEYOND;
    }

    public boolean isWorkaroundForBug167Needed() {
        // this has been fixed and made available since 1.8.0u92:
        // http://www.oracle.com/technetwork/java/javase/2col/8u92-bugfixes-2949473.html
        return JavaDetectionTools.IS_JAVA_8 && JavaDetectionTools.isAtLeastOracleJavaUpdateVersion(60) && !JavaDetectionTools.isAtLeastOracleJavaUpdateVersion(92);
    }

    public boolean isWorkaroundForBug182Needed() {
        // jnlp-bundler uses RelativeFileSet, and generates system-dependent dividers (\ on windows, / on others)
        return File.separator.equals("\\");
    }

    public boolean isWorkaroundForBug185Needed(Map<String, Object> params) {
        return params.containsKey("jnlp.allPermisions") && Boolean.parseBoolean(String.valueOf(params.get("jnlp.allPermisions")));
    }

    public boolean isWorkaroundForBug205Needed() {
        return (JavaDetectionTools.IS_JAVA_8 && JavaDetectionTools.isAtLeastOracleJavaUpdateVersion(40)) || JavaDetectionTools.IS_JAVA_9_AND_BEYOND;
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
        Path oldConfigFile = appPath.resolve(appName + CONFIG_FILE_EXTENSION);
        try{
            Files.move(oldConfigFile, appPath.resolve(newConfigFileName + CONFIG_FILE_EXTENSION), StandardCopyOption.ATOMIC_MOVE);
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

    /**
     * Get generated, fixed cfg-files and push them to app-resources-list.
     *
     *
     * @param appName
     * @param secondaryLaunchers
     * @param params
     */
    public void applyWorkaround205(String appName, List<NativeLauncher> secondaryLaunchers, Map<String, Object> params) {
        // to workaround, we are gathering the fixed versions of the previous executed "app-bundler"
        // and assume they all are existing
        Set<File> filenameFixedConfigFiles = new HashSet<>();

        // get cfg-file of main native launcher
        Path appPath = nativeOutputDir.toPath().resolve(appName).resolve("app").toAbsolutePath();
        if( appName.contains(".") ){
            String newConfigFileName = appName.substring(0, appName.lastIndexOf("."));
            File mainAppNameCfgFile = appPath.resolve(newConfigFileName + CONFIG_FILE_EXTENSION).toFile();
            if( mainAppNameCfgFile.exists() ){
                getLog().info("Found main native application configuration file (" + mainAppNameCfgFile.toString() + ").");
            }
            filenameFixedConfigFiles.add(mainAppNameCfgFile);
        }

        // when having secondary native launchers, we need their cfg-files too
        Optional.ofNullable(secondaryLaunchers).ifPresent(launchers -> {
            launchers.stream().map(launcher -> {
                return launcher.getAppName();
            }).forEach(secondaryLauncherAppName -> {
                if( secondaryLauncherAppName.contains(".") ){
                    String newSecondaryLauncherConfigFileName = secondaryLauncherAppName.substring(0, secondaryLauncherAppName.lastIndexOf("."));
                    filenameFixedConfigFiles.add(appPath.resolve(newSecondaryLauncherConfigFileName + CONFIG_FILE_EXTENSION).toFile());
                }
            });
        });

        if( filenameFixedConfigFiles.isEmpty() ){
            // it wasn't required to apply this workaround
            getLog().info("No workaround for native launcher issue 205 needed. Continuing.");
            return;
        }
        getLog().info("Applying workaround for native launcher issue 205 by modifying application resources.");

        // since 1.8.0_60 there exists some APP_RESOURCES_LIST, which contains multiple RelativeFileSet-instances
        // this is the more easy way ;)
        List<RelativeFileSet> appResourcesList = new ArrayList<>();
        RelativeFileSet appResources = StandardBundlerParam.APP_RESOURCES.fetchFrom(params);
        // original application resources
        appResourcesList.add(appResources);
        // additional filename-fixed cfg-files
        appResourcesList.add(new RelativeFileSet(appPath.toFile(), filenameFixedConfigFiles));

        // special workaround when having some jdk before update 60
        if( JavaDetectionTools.IS_JAVA_8 && !JavaDetectionTools.isAtLeastOracleJavaUpdateVersion(60) ){
            try{
                // pre-update60 did not contain any list of RelativeFileSets, which requires to rework APP_RESOURCES :/
                Path tempResourcesDirectory = Files.createTempDirectory("jfxmp-workaround205-").toAbsolutePath();
                File tempResourcesDirAsFile = tempResourcesDirectory.toFile();
                getLog().info("Modifying application resources for native launcher issue 205 by copying into temporary folder (" + tempResourcesDirAsFile.toString() + ").");
                for( RelativeFileSet sources : appResourcesList ){
                    File baseDir = sources.getBaseDirectory();
                    for( String fname : appResources.getIncludedFiles() ){
                        IOUtils.copyFile(new File(baseDir, fname), new File(tempResourcesDirAsFile, fname));
                    }
                }

                // might not work for gradle, but maven does not hold up any JVM ;)
                // might rework this later into cleanup-phase
                tempResourcesDirAsFile.deleteOnExit();

                // generate new RelativeFileSet with fixed cfg-file
                Set<File> fixedResourceFiles = new HashSet<>();
                try(Stream<Path> walkstream = Files.walk(tempResourcesDirectory)){
                    walkstream.
                            map(p -> p.toFile())
                            .filter(File::isFile)
                            .filter(File::canRead)
                            .forEach(f -> {
                                getLog().info(String.format("Add %s file to application resources.", f));
                                fixedResourceFiles.add(f);
                            });
                } catch(IOException ignored){
                    // NO-OP
                }
                params.put(StandardBundlerParam.APP_RESOURCES.getID(), new RelativeFileSet(tempResourcesDirAsFile, fixedResourceFiles));
            } catch(IOException ex){
                getLog().warn(ex);
            }
            return;
        }
        /*
         * Backward-compatibility note:
         * When using JDK 1.8.0u51 on travis-ci it would results into "cannot find symbol: variable APP_RESOURCES_LIST"!
         *
         * To solve this, we are using some hard-coded map-key :/ (please no hacky workaround via reflections .. urgh)
         */
        params.put(StandardBundlerParam.APP_RESOURCES.getID() + "List", appResourcesList);
    }

    public boolean isWorkaroundForNativeMacBundlerNeeded(File additionalBundlerResources) {
        boolean isMac = System.getProperty("os.name").toLowerCase().contains("os x");
        boolean hasBundlerResources = additionalBundlerResources != null && additionalBundlerResources.isDirectory() && additionalBundlerResources.exists();

        return isMac && hasBundlerResources;
    }

    public Bundler applyWorkaroundForNativeMacBundler(final Bundler b, String currentRunningBundlerID, Map<String, Object> params, File additionalBundlerResources) {
        getLog().info("Workaround for native mac bundler not present in this version (due to JDK9 compatibility conflicts).");
        getLog().info("Please use some older version of this plugin.");
        return b;
    }

}
