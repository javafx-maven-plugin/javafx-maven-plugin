[![Travis Build Status](https://travis-ci.org/javafx-maven-plugin/javafx-maven-plugin.svg?branch=master)](https://travis-ci.org/javafx-maven-plugin/javafx-maven-plugin)
[![AppVeyor Build status](https://ci.appveyor.com/api/projects/status/64700ul3m9y88agi/branch/master?svg=true)](https://ci.appveyor.com/project/FibreFoX/javafx-maven-plugin/branch/master)
[![Maven Central](https://img.shields.io/maven-central/v/com.zenjava/javafx-maven-plugin.svg)](https://maven-badges.herokuapp.com/maven-central/com.zenjava/javafx-maven-plugin)
[![Dependency Status](https://www.versioneye.com/java/com.zenjava:javafx-maven-plugin/8.2.0/badge.svg)](https://www.versioneye.com/java/com.zenjava:javafx-maven-plugin/8.2.0)


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
    <version>8.3.0</version>
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
    <version>8.3.0</version>
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

**Version 8.3.0 (07-Mar-2015)**

Bugfixes:
* bugfix #182 replace backslash with normal slash within JNLP-files
* bugfix #185 add signing-feature for bundler with ID "jnlp" (by setting `<jnlp.allPermisions>true</jnlp.allPermisions>` inside bundleArguments)
* fixed size-attributes within JNLP-files when using bundler with ID "jnlp" and requesting to sign the jars

New:
* added possibility for adding file associations
* added new property to disable backslash-fix `<skipNativeLauncherWorkaround182>true</skipNativeLauncherWorkaround182>`
* added new property to disable signing referenced jar-files `<skipSigningJarFilesJNLP185>true</skipSigningJarFilesJNLP185>`
* added new property to skip size-recalculation for jar-files inside generated JNLP-files `<skipSizeRecalculationForJNLP185>true</skipSizeRecalculationForJNLP185>`


(Not yet) Release(d) Notes
==================

upcoming Version 8.3.1 (???-2016)

- nothing changed yet