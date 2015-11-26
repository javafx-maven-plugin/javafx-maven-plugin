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
    <version>8.2.0</version>
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
    <version>8.2.0</version>
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


Last Release Notes
==================

**Version 8.2.0 (??-Nov-2015)**

Bugfixes:
* bugfix #159 added support for openjdk/openjfx (version-checker failed with NumberFormatException due to wrong expectations)
* added workaround for bug #167 regarding native windows launcher configuration-file (cfg-file), bug is inside Oracle JDK since 1.8.0 Update 60 (to work around this, this plugin tries to enforce property-file-format, which does not contain the problem)

New:
* added new property to disable workaround `<skipNativeLauncherWorkaround167>true</skipNativeLauncherWorkaround167>`
* added new mojo: calling `mvn jfx:list-bundlers` shows currently available bundlers with ID, name and descriptions, including their specific arguments able to be passed via `<bundleArguments>`-configuration
* added possibility for "secondary launchers", makes it possible to have more than one native launcher
* added possibility to filter dependencies while putting required JAR-files into the generated lib-folder
* added property to disable transitive filtering

Improvements:
* added some IT-projects and updated others
* updated to more Java 8 syntax
* added [Windows-CI via AppVeyor](http://www.appveyor.com/) (including status badge)


(Not yet) Release(d) Notes
==================

upcoming Version 8.2.1 (??-2015)
* nothing yet changed
