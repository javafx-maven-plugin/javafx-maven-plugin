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
package com.zenjava.javafx.maven.plugin.util;

import org.apache.maven.plugin.MojoExecutionException;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * This class uses a custom classloader, and general trickery to load the JFX tools JAR from the JAVA_HOME for the
 * Maven execution. It assumes the JAVA_HOME is set to a valid JDK installation with JavaFX installed within it.
 * <p/>
 * This would not be necessary if the JavaFX tools JAR file was available in a public repository. Currently however
 * there are legal restrictions against non-Oracle developers redistributing any official JavaFX code. This reflection
 * based solution avoids this by using the officially distributed tools JAR that comes with the JDK.
 */
public class JfxToolsWrapper {

    private Class packagerLibClass;
    private Class createJarParamsClass;
    private Class signJarParamsClass;
    private Class deployParamsClass;
    private Class bundleTypeClass;
    private Object packagerLib;
    private boolean verbose;

    public JfxToolsWrapper(File jfxToolsJar, boolean verbose) throws MojoExecutionException {

        ClassLoader jfxToolsClassloader = loadClassLoader(jfxToolsJar);
        packagerLibClass = loadClass(jfxToolsClassloader, "PackagerLib");
        createJarParamsClass = loadClass(jfxToolsClassloader, "CreateJarParams");
        signJarParamsClass = loadClass(jfxToolsClassloader, "SignJarParams");
        deployParamsClass = loadClass(jfxToolsClassloader, "DeployParams");
        bundleTypeClass = loadClass(jfxToolsClassloader, "bundlers.Bundler$BundleType");
        packagerLib = newInstance(packagerLibClass);

        Class logClass = loadClass(jfxToolsClassloader, "Log");
        Class loggerClass = loadClass(jfxToolsClassloader, "Log$Logger");
        this.verbose = verbose;
        Object logger = newInstance(loggerClass, new Class[]{Boolean.TYPE}, verbose);
        invokeStatic(logClass, "setLogger", logger);
    }

    public void packageAsJar(File outputFile, File classesDir, File dependenciesDir, String mainClass, boolean css2bin) throws MojoExecutionException {

        Object params = newInstance(createJarParamsClass);
        invoke(params, "setOutdir", outputFile.getParentFile());
        invoke(params, "setOutfile", outputFile.getName());
        invoke(params, "addResource", classesDir, "");
        if( dependenciesDir.exists() ) {
            invoke( params, "addResource", dependenciesDir, "" );
        }
        invoke(params, "setApplicationClass", mainClass);
        invoke(params, "setCss2bin", css2bin);

        invoke(packagerLib, "packageAsJar", params);
    }

    public void signJarFiles(File keyStore, String alias, String storePass, String keyPass, String storeType, File... jarFiles)
            throws MojoExecutionException {

        Object params = newInstance(signJarParamsClass);
        invoke(params, "setKeyStore", keyStore);
        invoke(params, "setAlias", alias);
        invoke(params, "setStorePass", storePass);
        invoke(params, "setKeyPass", keyPass);
        invoke(params, "setStoreType", storeType);

        for (File jarFile : jarFiles) {
            invoke(params, "addResource", jarFile.getParentFile(), jarFile.getName());
        }

        invoke(packagerLib, "signJar", params);

    }

    public void generateDeploymentPackages(File outputDir, String[] jarResources, String bundleType,
                                           String appDistributionName, String appName, String version, String vendor,
                                           String mainClass)
            throws MojoExecutionException {

        Object deployParams = newInstance(deployParamsClass);
        invoke(deployParams, "setOutdir", outputDir);
        invoke(deployParams, "setOutfile", appDistributionName);
        invoke(deployParams, "setApplicationClass", mainClass);
        invoke(deployParams, "setVerbose", new Class[] { Boolean.TYPE }, verbose);

        if (jarResources != null && jarResources.length > 0) {
            for (String jarResource : jarResources) {
                invoke(deployParams, "addResource", outputDir, jarResource);
            }
        }

        Object bundleTypeEnum = invokeStatic(bundleTypeClass, "valueOf", bundleType);
        invoke(deployParams, "setBundleType", bundleTypeEnum);

        // Installer seems to crash with this stuff on ...
        //Object bundleParams = invoke(deployParams, "getBundleParams");
        //invoke(bundleParams, "setName", appDistributionName);
        //invoke(bundleParams, "setApplicationVersion", version);
        //invoke(bundleParams, "setDisplayName", appName);
        //invoke(bundleParams, "setVendor", vendor);

        invoke(packagerLib, "generateDeploymentPackages", deployParams);

        // delete the JNLP and webstart generated files as we didn't ask for them
        new File(outputDir, appDistributionName + ".html").delete();
        new File(outputDir, appDistributionName + ".jnlp").delete();
    }



    //-------------------------------------------------------------------------

    protected ClassLoader loadClassLoader(File jfxToolsJar) throws MojoExecutionException {

        try {
            return new URLClassLoader(new URL[] {jfxToolsJar.toURI().toURL()});
        } catch (MalformedURLException e) {
            throw new MojoExecutionException("JAVA_HOME did not resolve to a valid URL: " + jfxToolsJar, e);
        }
    }

    protected Class loadClass(ClassLoader classLoader, String name) throws MojoExecutionException {

        try {
            return Class.forName("com.sun.javafx.tools.packager." + name, true, classLoader);

        } catch (ClassNotFoundException e) {
            throw new MojoExecutionException("Unable to find the JavaFX '"
                    + name + "' class. It's possible this plugin is not compatible with the version of JavaFX you are using.", e);
        }
    }

    protected Object newInstance(Class jfxPackagerClass, Class[] paramTypes, Object... params) throws MojoExecutionException {

        try {
            Constructor constructor = jfxPackagerClass.getConstructor(paramTypes);
            return constructor.newInstance(params);

        } catch (InstantiationException e) {
            throw new MojoExecutionException("Unable to instantiate an instance of the JavaFX '"
                    + jfxPackagerClass.getName() + "' class. It's possible this plugin is not compatible with the version of JavaFX you are using.", e);

        } catch (IllegalAccessException e) {
            throw new MojoExecutionException("Unable to access the no-arg constructor for the JavaFX '"
                    + jfxPackagerClass.getName() + "' class. It's possible this plugin is not compatible with the version of JavaFX you are using.", e);

        } catch (NoSuchMethodException e) {
            throw new MojoExecutionException("Unable to find matching constructor for JavaFX '"
                    + jfxPackagerClass.getName() + "' class. It's possible this plugin is not compatible with the version of JavaFX you are using.", e);

        } catch (InvocationTargetException e) {
            throw new MojoExecutionException("Error while instantiating an instance of the JavaFX '"
                    + jfxPackagerClass.getName() + "' class. It's possible this plugin is not compatible with the version of JavaFX you are using.", e);

        }
    }

    protected Object newInstance(Class jfxPackagerClass) throws MojoExecutionException {

        try {
            return jfxPackagerClass.newInstance();

        } catch (InstantiationException e) {
            throw new MojoExecutionException("Unable to instantiate an instance of the JavaFX '"
                    + jfxPackagerClass.getName() + "' class. It's possible this plugin is not compatible with the version of JavaFX you are using.", e);

        } catch (IllegalAccessException e) {
            throw new MojoExecutionException("Unable to access the no-arg constructor for the JavaFX '"
                    + jfxPackagerClass.getName() + "' class. It's possible this plugin is not compatible with the version of JavaFX you are using.", e);

        }
    }

    protected Method loadMethod(Class ownerClass, String name, Class... paramTypes) throws MojoExecutionException {

        try {

            return ownerClass.getMethod(name, paramTypes);

        } catch (NoSuchMethodException e) {
            throw new MojoExecutionException("Unable to find JavaFX method '" + name + "' on '" + ownerClass.getName()
                    + "'. It's possible this plugin is not compatible with the version of JavaFX you are using.", e);

        }
    }

    protected Object invoke(Object target, String methodName, Object... params) throws MojoExecutionException {

        Class[] paramTypes = new Class[params.length];
        for (int i = 0; i < params.length; i++) {
            paramTypes[i] = params[i].getClass();
        }

        return invoke(target, methodName, paramTypes, params);
    }

    protected Object invoke(Object target, String methodName, Class[] paramTypes, Object... params) throws MojoExecutionException {

        Method method = loadMethod(target.getClass(), methodName, paramTypes);
        return invoke(method, target, params);
    }

    protected Object invokeStatic(Class targetClass, String methodName, Object... params) throws MojoExecutionException {

        Class[] paramTypes = new Class[params.length];
        for (int i = 0; i < params.length; i++) {
            paramTypes[i] = params[i].getClass();
        }

        Method method = loadMethod(targetClass, methodName, paramTypes);
        return invoke(method, (Object)null, params);
    }

    protected Object invoke(Method method, Object target, Object... params) throws MojoExecutionException {

        try {
            return method.invoke(target, params);
        } catch (SecurityException e) {
            throw new MojoExecutionException("Security violation accessing JavaFX method '" + method.getName()
                    + "'. It's possible this plugin is not compatible with the version of JavaFX you are using.", e);
        } catch (IllegalAccessException e) {
            throw new MojoExecutionException("Unable to access method '" + method.getName()
                    + "'. It's possible this plugin is not compatible with the version of JavaFX you are using.", e);
        } catch (IllegalArgumentException e) {
            throw new MojoExecutionException("Signature on JavaFX method '" + method.getName()
                    + "' did not match expectations. It's possible this plugin is not compatible with the version of JavaFX you are using.", e);
        } catch (InvocationTargetException e) {
            throw new MojoExecutionException("Invocation of JavaFX method '" + method.getName()
                    + "' failed with an error. It's possible this plugin is not compatible with the version of JavaFX you are using.", e);
        }
    }

    protected Object invoke(Object target, String methodName, boolean param) throws MojoExecutionException {

        Class[] paramTypes = { boolean.class };

        Method method = loadMethod(target.getClass(), methodName, paramTypes);

        try {
            return method.invoke(target, param);
        } catch (SecurityException e) {
            throw new MojoExecutionException("Security violation accessing JavaFX method '" + method.getName()
                    + "'. It's possible this plugin is not compatible with the version of JavaFX you are using.", e);
        } catch (IllegalAccessException e) {
            throw new MojoExecutionException("Unable to access method '" + method.getName()
                    + "'. It's possible this plugin is not compatible with the version of JavaFX you are using.", e);
        } catch (IllegalArgumentException e) {
            throw new MojoExecutionException("Signature on JavaFX method '" + method.getName()
                    + "' did not match expectations. It's possible this plugin is not compatible with the version of JavaFX you are using.", e);
        } catch (InvocationTargetException e) {
            throw new MojoExecutionException("Invocation of JavaFX method '" + method.getName()
                    + "' failed with an error. It's possible this plugin is not compatible with the version of JavaFX you are using.", e);
        }
    }
}
