JavaFX Maven Plugin
===================

The JavaFX Maven Plugin provides a way to to assemble distributable bundles for JavaFX applications from within Maven.
It provides a wrapper around the JavaFX packaging tools which are provided as part of the JavaFX installation (i.e. the
ANT tasks that come with JavaFX).

Currently this plugin can build self-contained, executable JARs and also native installers (MSI, EXE, RPM, DMG). It
does not currently build Applets or JNLP bundles.

Additionally the JavaFX Maven Plugin provides a command to "fix" the JRE to include JavaFX on the JDK classpath of your 
development environment. This a work around for the problems around JFX not being on the classpath by default. 


Setting Up
============

You must have a valid JDK installed with a valid JavaFX installation included in it (i.e. *Java 1.7.0 update 9* or 
higher). This plugin uses the tools within the JDK installation (found via the JAVA_HOME setting).

To build native installers with this plugin, you need to also install the relevant native installer library used by
JavaFX for your OS (e.g. on Windows, install WiX). See the JavaFX installation steps for info on this: https://blogs.oracle.com/talkingjavadeployment/entry/native_packaging_for_javafx


Example Project
===============

To make getting started easier I have created a small, but complete example project with a fully configured POM:

https://github.com/zonski/hello-javafx-maven-example

You can download this and use it as a kick-starter template, or you can follow the steps below. 


Fixing the JRE Classpath
============================

There is a (big) flaw in the current approach used by Oracle to co-bundle JavaFX in with the JDK. When you install 
the JDK it now also installs JavaFX, however (for reasons of politics and red tape), JavaFX is NOT added to the 
classpath.

Oracle plans to fix this (hopefully by Java 8) but until then it is necessary to manually update the JRE classpath of
your local JDK so that JavaFX is available to your application. 

To "fix" your classpath, create your Maven project as per the *Usage* section below and then use the command line to run the
following command: 

```
   mvn jfx:fix-classpath
```

This will copy the JavaFX runtime JAR (jfxrt.jar) into the extensions directory of the JRE.

Note: the obvious idea of adding JavaFX as a standard Maven dependency does not work due to the native file loading 
approach used by JFX. Search the OTN forums for more information on this. Oracle has no plans to fix this loading issue
and the co-bundling fix for Java 8 will make this unnecesary anyway (you won't want, or need a Maven dependency for 
JavaFX since it will be on the classpath by default, much like Swing and the Java Collections API).  



Usage
=============

The Plugin is currently available in the Central Maven repository. 

By default, the plugin will build an executable JAR (i.e. one you can double click to launch, assuming you already have
a JRE installed on your system) for your JavaFX application that contains all runtime dependencies within it.

To make this happen, add the following to your pom.xml:

``` xml
    <plugin>
        <groupId>com.zenjava</groupId>
        <artifactId>javafx-maven-plugin</artifactId>
        <version>1.1</version>
        <executions>
            <execution>
                <phase>package</phase>
                <goals>
                    <goal>package</goal>
                </goals>
            </execution>
        </executions>
        <configuration>
            <mainClass>[put your application main class here]</mainClass>
        </configuration>
    </plugin>
```

Additionally, the plugin can be used to build native distributions:

``` xml
    <plugin>
        <groupId>com.zenjava</groupId>
        <artifactId>javafx-maven-plugin</artifactId>
        <version>1.1</version>
        <executions>
            <execution>
                <phase>package</phase>
                <goals>
                    <goal>package</goal>
                </goals>
            </execution>
        </executions>
        <configuration>
            <mainClass>[put your application main class here]</mainClass>

            <!-- 
            These mysterious parameters are from the JFX core packaging library. 
            Using a bundle type of ALL seems to be the best for producing native installers 
            -->  
            <bundleType>ALL|IMAGE|INSTALLER|NONE</bundleType>

        </configuration>
    </plugin>
```


Support
=======

This plugin is currently basic. It works for creating standard executable JARs and native distributables but provides only the basic
configuration for these. It does not support JNLP or Applet deployment.

Currently I am waiting for the JFX tools to be open sourced before developing this any further. I make no guarantees
that I will do any further work on this. If the core packaging tools are open sourced in a way to make them usable
I will likely enhance the native packaging but I am unlikely to improve Applet or JNLP support. If anyone would like
to contribute functionality for these, please contact me. 

Likewise, I am not intending to provide on-going support for this plugin. It is available as-is. If future releases of
JavaFX change the way the packaging tools work then this plugin is likely to break. I may, or may not, fix these issues, 
it will largely depend on whether I am working with JavaFX or not at the time. Again, I am happy to help contributors 
get started should anyone want to do this work in the future.


License
=======

Copyright 2012 Daniel Zwolenski.

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.


Credit
======

This plugin makes use of Tim Moore's excellent mojo-executor project: https://github.com/TimMoore/mojo-executor

