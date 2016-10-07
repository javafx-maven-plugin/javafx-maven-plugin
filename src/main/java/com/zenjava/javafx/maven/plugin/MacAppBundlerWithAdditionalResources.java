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

import com.oracle.tools.packager.BundlerParamInfo;
import com.oracle.tools.packager.IOUtils;
import com.oracle.tools.packager.Log;
import com.oracle.tools.packager.StandardBundlerParam;
import static com.oracle.tools.packager.StandardBundlerParam.APP_NAME;
import static com.oracle.tools.packager.StandardBundlerParam.BUILD_ROOT;
import static com.oracle.tools.packager.StandardBundlerParam.VERBOSE;
import com.oracle.tools.packager.mac.MacAppBundler;
import com.oracle.tools.packager.mac.MacBaseInstallerBundler;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicReference;

/**
 * This bundler is the workaround for mac bundlers not having the option to provide additional files
 * being used while bundling.
 *
 * If there should be any legal issue about this, please dear lawyers, contant me first.
 * I am expecting this to be allowed 'cause of the classpath-exception of the used GPLv2!
 *
 * @author Danny Althoff
 */
public class MacAppBundlerWithAdditionalResources extends MacAppBundler {

    private static final ResourceBundle I18N = ResourceBundle.getBundle(MacAppBundler.class.getName());
    private static final String LIBRARY_NAME = "libpackager.dylib";
    private static final Class<MacAppBundler> ORIGINAL_MAC_APP_BUNDLER_CLASS = MacAppBundler.class;
    public static final BundlerParamInfo<File> ADDITIONAL_BUNDLER_RESOURCES = new StandardBundlerParam<>(
            "additional bundler resources",
            "Field for providing additional resources that will be put into generation-folder.",
            "mac.app.additionalBundlerResources",
            File.class,
            params -> null,
            (s, p) -> new File(s));

    /**
     * Code mostly used from the existing bundler, but not the same ;)
     *
     * @param p
     * @param outputDirectory
     * @param dependentTask
     *
     * @return
     */
    @Override
    public File doBundle(Map<String, ? super Object> p, File outputDirectory, boolean dependentTask) {
        // use original stuff as much as possible
        File additionalBundlerResources = ADDITIONAL_BUNDLER_RESOURCES.fetchFrom(p);
        if( additionalBundlerResources == null ){
            return super.doBundle(p, outputDirectory, dependentTask);
        }

        // if special additional bundler resources are provided, we need to do magic here !
        Map<String, ? super Object> originalParams = new HashMap<>(p);
        doOutputFolderChecks(outputDirectory);

        File rootDirectory = null;
        try{
            final File predefinedImage = MacBaseInstallerBundler.getPredefinedImage(p);
            if( predefinedImage != null ){
                return predefinedImage;
            }

            BUILD_ROOT.fetchFrom(p);
            prepareConfigFiles(p);
            rootDirectory = new File(outputDirectory, APP_NAME.fetchFrom(p) + ".app");
            // this is the root of evil, because we can't just "pre-copy" all additional files needed
            IOUtils.deleteRecursive(rootDirectory);
            // recreate
            rootDirectory.mkdirs();

            // this mac app bundler gets called by other mac installer bundlers
            if( !dependentTask ){
                Log.info(MessageFormat.format(I18N.getString("message.creating-app-bundle"), rootDirectory.getAbsolutePath()));
            }

            File contentsDirectory = new File(rootDirectory, "Contents");
            contentsDirectory.mkdirs();
            File macOSDirectory = new File(contentsDirectory, "MacOS");
            macOSDirectory.mkdirs();
            File javaDirectory = new File(contentsDirectory, "Java");
            javaDirectory.mkdirs();
            File plugInsDirectory = new File(contentsDirectory, "PlugIns");
            File resourcesDirectory = new File(contentsDirectory, "Resources");
            resourcesDirectory.mkdirs();

            File pkgInfoFile = new File(contentsDirectory, "PkgInfo");
            pkgInfoFile.createNewFile();
            writePkgInfo(pkgInfoFile);

            File executableFile = new File(macOSDirectory, getLauncherName(p));
            IOUtils.copyFromURL(RAW_EXECUTABLE_URL.fetchFrom(p), executableFile);

            if( JavaDetectionTools.IS_JAVA_8 && JavaDetectionTools.isAtLeastOracleJavaUpdateVersion(40) ){
                // use FQN for not having incompatible import
                IOUtils.copyFromURL(com.oracle.tools.packager.mac.MacResources.class.getResource(LIBRARY_NAME), new File(macOSDirectory, LIBRARY_NAME));

                if( JavaDetectionTools.isAtLeastOracleJavaUpdateVersion(60) ){
                    if( !MAC_CONFIGURE_LAUNCHER_IN_PLIST.fetchFrom(p) ){
                        if( LAUNCHER_CFG_FORMAT.fetchFrom(p).equals(CFG_FORMAT_PROPERTIES) ){
                            writeCfgFile(p, rootDirectory);
                        } else {
                            writeCfgFile(p, new File(rootDirectory, getLauncherCfgName(p)), getRuntimeLocation(p));
                        }
                    }
                }
            }

            executableFile.setExecutable(true, false);

            copyRuntime(plugInsDirectory, p);
            copyClassPathEntries(javaDirectory, p);
            IOUtils.copyFile(getConfig_Icon(p), new File(resourcesDirectory, getConfig_Icon(p).getName()));

            if( JavaDetectionTools.IS_JAVA_8 && JavaDetectionTools.isAtLeastOracleJavaUpdateVersion(60) ){
                for( Map<String, ? super Object> fa : com.oracle.tools.packager.StandardBundlerParam.FILE_ASSOCIATIONS.fetchFrom(p) ){
                    File f = com.oracle.tools.packager.StandardBundlerParam.FA_ICON.fetchFrom(fa);
                    if( f != null && f.exists() ){
                        IOUtils.copyFile(f, new File(resourcesDirectory, f.getName()));
                    }
                }
            }

            IOUtils.copyFile(getConfig_InfoPlist(p), new File(contentsDirectory, "Info.plist"));

            if( JavaDetectionTools.IS_JAVA_8 && JavaDetectionTools.isAtLeastOracleJavaUpdateVersion(60) ){
                for( Map<String, ? super Object> entryPoint : StandardBundlerParam.SECONDARY_LAUNCHERS.fetchFrom(p) ){
                    Map<String, ? super Object> tmp = new HashMap<>(originalParams);
                    tmp.putAll(entryPoint);
                    createLauncherForEntryPoint(tmp, rootDirectory);
                }
            }
            Path sourceFolder = additionalBundlerResources.toPath();
            Path targetFolder = rootDirectory.toPath();
            AtomicReference<IOException> copyingException = new AtomicReference<>(null);

            Files.walkFileTree(sourceFolder, new FileVisitor<Path>() {

                @Override
                public FileVisitResult preVisitDirectory(Path subfolder, BasicFileAttributes attrs) throws IOException {
                    // do create subfolder (if needed)
                    Files.createDirectories(targetFolder.resolve(sourceFolder.relativize(subfolder)));
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path sourceFile, BasicFileAttributes attrs) throws IOException {
                    // do copy, and replace, as the resource might already be existing
                    Files.copy(sourceFile, targetFolder.resolve(sourceFolder.relativize(sourceFile)), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path source, IOException ioe) throws IOException {
                    // don't fail, just inform user
                    copyingException.set(ioe);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path source, IOException ioe) throws IOException {
                    // nothing to do here
                    return FileVisitResult.CONTINUE;
                }
            });

            if( copyingException.get() != null ){
                throw new RuntimeException("Got exception while copying additional bundler resources", copyingException.get());
            }

            String signingIdentity = DEVELOPER_ID_APP_SIGNING_KEY.fetchFrom(p);
            if( signingIdentity != null ){
                if( JavaDetectionTools.IS_JAVA_8 && JavaDetectionTools.isAtLeastOracleJavaUpdateVersion(40) ){
                    // update 40 seems to have made this optional ;)
                    // use FQN for not having incompatible import
                    if( Optional.ofNullable(com.oracle.tools.packager.StandardBundlerParam.SIGN_BUNDLE.fetchFrom(p)).orElse(Boolean.TRUE) ){
                        MacBaseInstallerBundler.signAppBundle(p, rootDirectory, signingIdentity, BUNDLE_ID_SIGNING_PREFIX.fetchFrom(p));
                    }
                } else {
                    MacBaseInstallerBundler.signAppBundle(p, rootDirectory, signingIdentity, BUNDLE_ID_SIGNING_PREFIX.fetchFrom(p));
                }
            }
        } catch(IOException ex){
            Log.info(ex.toString());
            Log.verbose(ex);
            return null;
        } finally{
            if( !VERBOSE.fetchFrom(p) ){
                cleanupConfigFiles(p);
            } else {
                Log.info(MessageFormat.format(I18N.getString("message.config-save-location"), CONFIG_ROOT.fetchFrom(p).getAbsolutePath()));
            }
        }
        return rootDirectory;
    }

    private void doOutputFolderChecks(File outputDirectory) {
        if( !outputDirectory.isDirectory() && !outputDirectory.mkdirs() ){
            throw new RuntimeException(MessageFormat.format(I18N.getString("error.cannot-create-output-dir"), outputDirectory.getAbsolutePath()));
        }
        if( !outputDirectory.canWrite() ){
            throw new RuntimeException(MessageFormat.format(I18N.getString("error.cannot-write-to-output-dir"), outputDirectory.getAbsolutePath()));
        }
    }

    private void prepareConfigFiles(Map<String, ? super Object> params) throws IOException {
        // call using reflection, because this method is "private"
        try{
            Method method = ORIGINAL_MAC_APP_BUNDLER_CLASS.getDeclaredMethod("prepareConfigFiles", Map.class);
            method.setAccessible(true);
            method.invoke(this, params);
        } catch(NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex){
            // if this does not work, explode
            if( ex instanceof InvocationTargetException ){
                Throwable cause = ((InvocationTargetException) ex).getCause();
                if( cause instanceof IOException ){
                    throw (IOException) cause;
                }
                if( cause instanceof RuntimeException ){
                    throw (RuntimeException) cause;
                }
            }
        }
    }

    private void writeCfgFile(Map<String, ? super Object> params, File rootDir) throws FileNotFoundException {
        // call using reflection, because this method is "private"
        try{
            Method method = ORIGINAL_MAC_APP_BUNDLER_CLASS.getDeclaredMethod("writeCfgFile", Map.class, File.class);
            method.setAccessible(true);
            method.invoke(this, params, rootDir);
        } catch(NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex){
            // if this does not work, explode
            if( ex instanceof InvocationTargetException ){
                Throwable cause = ((InvocationTargetException) ex).getCause();
                if( cause instanceof FileNotFoundException ){
                    throw (FileNotFoundException) cause;
                }
                if( cause instanceof RuntimeException ){
                    throw (RuntimeException) cause;
                }
            }
        }
    }

    private void writePkgInfo(File file) throws IOException {
        // call using reflection, because this method is "private"
        try{
            Method method = ORIGINAL_MAC_APP_BUNDLER_CLASS.getDeclaredMethod("writePkgInfo", File.class);
            method.setAccessible(true);
            method.invoke(this, file);
        } catch(NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex){
            // if this does not work, explode
            if( ex instanceof InvocationTargetException ){
                Throwable cause = ((InvocationTargetException) ex).getCause();
                if( cause instanceof IOException ){
                    throw (IOException) cause;
                }
                if( cause instanceof RuntimeException ){
                    throw (RuntimeException) cause;
                }
            }
        }
    }

    private void copyRuntime(File plugInsDirectory, Map<String, ? super Object> params) throws IOException {
        // call using reflection, because this method is "private"
        try{
            Method method = ORIGINAL_MAC_APP_BUNDLER_CLASS.getDeclaredMethod("copyRuntime", File.class, Map.class);
            method.setAccessible(true);
            method.invoke(this, plugInsDirectory, params);
        } catch(NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex){
            // if this does not work, explode
            if( ex instanceof InvocationTargetException ){
                Throwable cause = ((InvocationTargetException) ex).getCause();
                if( cause instanceof IOException ){
                    throw (IOException) cause;
                }
                if( cause instanceof RuntimeException ){
                    throw (RuntimeException) cause;
                }
            }
        }
    }

    private void copyClassPathEntries(File javaDirectory, Map<String, ? super Object> params) throws IOException {
        // call using reflection, because this method is "private"
        try{
            Method method = ORIGINAL_MAC_APP_BUNDLER_CLASS.getDeclaredMethod("copyClassPathEntries", File.class, Map.class);
            method.setAccessible(true);
            method.invoke(this, javaDirectory, params);
        } catch(NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex){
            // if this does not work, explode
            if( ex instanceof InvocationTargetException ){
                Throwable cause = ((InvocationTargetException) ex).getCause();
                if( cause instanceof IOException ){
                    throw (IOException) cause;
                }
                if( cause instanceof RuntimeException ){
                    throw (RuntimeException) cause;
                }
            }
        }
    }

    private File getConfig_Icon(Map<String, ? super Object> params) {
        // call using reflection, because this method is "private"
        try{
            Method method = ORIGINAL_MAC_APP_BUNDLER_CLASS.getDeclaredMethod("getConfig_Icon", Map.class);
            method.setAccessible(true);
            Object invokationResult = method.invoke(this, params);
            if( invokationResult instanceof File ){
                return (File) invokationResult;
            }
        } catch(NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex){
            if( ex instanceof RuntimeException ){
                throw (RuntimeException) ex;
            }
        }
        return null;
    }

    private String getRuntimeLocation(Map<String, ? super Object> params) {
        // call using reflection, because this method is "private"
        try{
            Method method = ORIGINAL_MAC_APP_BUNDLER_CLASS.getDeclaredMethod("getRuntimeLocation", Map.class);
            method.setAccessible(true);
            Object invokationResult = method.invoke(this, params);
            if( invokationResult instanceof String ){
                return (String) invokationResult;
            }
        } catch(NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex){
            if( ex instanceof RuntimeException ){
                throw (RuntimeException) ex;
            }
        }
        return null;
    }

    private File getConfig_InfoPlist(Map<String, ? super Object> params) {
        // call using reflection, because this method is "private"
        try{
            Method method = ORIGINAL_MAC_APP_BUNDLER_CLASS.getDeclaredMethod("getConfig_InfoPlist", Map.class);
            method.setAccessible(true);
            Object invokationResult = method.invoke(this, params);
            if( invokationResult instanceof File ){
                return (File) invokationResult;
            }
        } catch(NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex){
            if( ex instanceof RuntimeException ){
                throw (RuntimeException) ex;
            }
        }
        return null;
    }

    private void createLauncherForEntryPoint(Map<String, ? super Object> p, File rootDirectory) throws IOException {
        // call using reflection, because this method is "private"
        try{
            Method method = ORIGINAL_MAC_APP_BUNDLER_CLASS.getDeclaredMethod("createLauncherForEntryPoint", Map.class, File.class);
            method.setAccessible(true);
            method.invoke(this, p, rootDirectory);
        } catch(NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex){
            // if this does not work, explode
            if( ex instanceof InvocationTargetException ){
                Throwable cause = ((InvocationTargetException) ex).getCause();
                if( cause instanceof IOException ){
                    throw (IOException) cause;
                }
                if( cause instanceof RuntimeException ){
                    throw (RuntimeException) cause;
                }
            }
        }
    }

    private String getLauncherName(Map<String, ? super Object> params) {
        // call using reflection, because this method is "private"
        try{
            Method method = ORIGINAL_MAC_APP_BUNDLER_CLASS.getDeclaredMethod("getLauncherName", Map.class);
            method.setAccessible(true);
            Object invokationResult = method.invoke(this, params);
            if( invokationResult instanceof String ){
                return (String) invokationResult;
            }
        } catch(NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex){
            if( ex instanceof RuntimeException ){
                throw (RuntimeException) ex;
            }
        }
        return null;
    }
}
