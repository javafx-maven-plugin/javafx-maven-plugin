[![Travis Build Status](https://travis-ci.org/javafx-maven-plugin/javafx-maven-plugin.svg?branch=master)](https://travis-ci.org/javafx-maven-plugin/javafx-maven-plugin)
[![AppVeyor Build status](https://ci.appveyor.com/api/projects/status/64700ul3m9y88agi/branch/master?svg=true)](https://ci.appveyor.com/project/FibreFoX/javafx-maven-plugin/branch/master)
[![Maven Central](https://img.shields.io/maven-central/v/com.zenjava/javafx-maven-plugin.svg)](https://maven-badges.herokuapp.com/maven-central/com.zenjava/javafx-maven-plugin)
[![Dependency Status](https://www.versioneye.com/java/com.zenjava:javafx-maven-plugin/8.4.0/badge.svg)](https://www.versioneye.com/java/com.zenjava:javafx-maven-plugin/8.4.0)


JavaFX Maven Plugin
===================

The JavaFX Maven Plugin provides a way to assemble distribution bundles for JavaFX applications (8+) from within Maven.
 
For easy configuration please use our new website:
**[http://javafx-maven-plugin.github.io](http://javafx-maven-plugin.github.io)**

For (outdated) documentation/examples, your can look at archived website:
**[https://web.archive.org/web/20141009064442/http://zenjava.com/javafx/maven/](https://web.archive.org/web/20141009064442/http://zenjava.com/javafx/maven/)**

A new website will be provided soon and recieves it's final polish, so please be patient.


Quickstart for JavaFX JAR
=========================

Add this to your pom.xml within to your build-plugin:

```xml
<plugin>
    <groupId>com.zenjava</groupId>
    <artifactId>javafx-maven-plugin</artifactId>
    <version>8.4.0</version>
    <configuration>
        <mainClass>your.package.with.Launcher</mainClass>
    </configuration>
</plugin>
```

To create your executable file with JavaFX-magic, call `mvn jfx:jar`. The jar-file will be placed at `target/jfx/app`.


Quickstart for JavaFX native bundle
===================================

Add this to your pom.xml within to your build-plugin:

```xml
<plugin>
    <groupId>com.zenjava</groupId>
    <artifactId>javafx-maven-plugin</artifactId>
    <version>8.4.0</version>
    <configuration>
        <vendor>YourCompany</vendor>
        <mainClass>your.package.with.Launcher</mainClass>
    </configuration>
</plugin>
```

To create your executable file with JavaFX-magic, call `mvn jfx:native`. The jar-file will be placed at `target/jfx/native`.



Prepared for Java 9
===================

Add repository in your `pom.xml` for snapshot-versions of this plugin:

```xml
<pluginRepositories>
    <pluginRepository>
        <id>oss-sonatype-snapshots</id>
        <url>https://oss.sonatype.org/content/groups/public/</url>
        <snapshots>
            <enabled>true</enabled>
        </snapshots>
    </pluginRepository>
</pluginRepositories>
```

Set version to new SNAPSHOT-version:

```xml
<plugin>
    <groupId>com.zenjava</groupId>
    <artifactId>javafx-maven-plugin</artifactId>
    <version>9.0.0-SNAPSHOT</version>
    <configuration>
        <!-- your configuration -->
    </configuration>
</plugin>
```

*Some notes: as this isn't the main branch, a lot of features aren't present in that branch yet, deployment of new "-SNAPSHOT"-version are on-demand*


Last Release Notes
==================

**Version 8.4.0 (11-Mar-2015)**

New:
* when creating JNLP-files, your can now choose between Blob Signing (which was introduced since JavaFX but seems has never worked, and will be removed from Java 9) or normal signing done by `jarsigner` by providing the new proverty `<noBlobSigning>true</noBlobSigning>`
* the `run` goal got its deprecation removed, you can call `mvn jfx:run` now again to start your application like you would start a normal executable-jar (no more calling `java -jar yourProject-jfx.jar`)

Improvements:
* removed `org.twdata.maven:mojo-executor`-dependency
* fixed maven-plugin dependencies
* generating keystore now directly uses `keytool`
* changed appveyor to build/test using x86 and x64


(Not yet) Release(d) Notes
==========================

upcoming Version 8.4.1 (???-2016)

* nothing changed yet
