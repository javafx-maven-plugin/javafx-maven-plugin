[![Travis Build Status](https://travis-ci.org/javafx-maven-plugin/javafx-maven-plugin.svg?branch=master)](https://travis-ci.org/javafx-maven-plugin/javafx-maven-plugin)
[![AppVeyor Build status](https://ci.appveyor.com/api/projects/status/64700ul3m9y88agi/branch/master?svg=true)](https://ci.appveyor.com/project/FibreFoX/javafx-maven-plugin/branch/master)
[![Maven Central](https://img.shields.io/maven-central/v/com.zenjava/javafx-maven-plugin.svg)](https://maven-badges.herokuapp.com/maven-central/com.zenjava/javafx-maven-plugin)
[![Dependency Status](https://www.versioneye.com/java/com.zenjava:javafx-maven-plugin/8.7.0/badge.svg)](https://www.versioneye.com/java/com.zenjava:javafx-maven-plugin/8.7.0)



JavaFX Maven Plugin
===================

The JavaFX Maven Plugin provides a way to assemble distribution bundles for JavaFX applications (8+) from within Maven.
 
For easy configuration please use our new website (which needs to get updated/reworked again):
**[http://javafx-maven-plugin.github.io](http://javafx-maven-plugin.github.io)**

For (outdated) documentation/examples, your can look at archived website:
**[https://web.archive.org/web/20141009064442/http://zenjava.com/javafx/maven/](https://web.archive.org/web/20141009064442/http://zenjava.com/javafx/maven/)**



Quickstart for JavaFX JAR
=========================

Add this to your pom.xml within to your build-plugin:

```xml
<plugin>
    <groupId>com.zenjava</groupId>
    <artifactId>javafx-maven-plugin</artifactId>
    <version>8.7.0</version>
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
    <version>8.7.0</version>
    <configuration>
        <vendor>YourCompany</vendor>
        <mainClass>your.package.with.Launcher</mainClass>
    </configuration>
</plugin>
```

To create your executable file with JavaFX-magic and some installers (please see official oracle-documentation which applications are required for this), call `mvn jfx:native`. The native launchers or installers will be placed at `target/jfx/native`.



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
**This is currently heavily outdated**



Last Release Notes
==================

**Version 8.7.0 (09-Sept-2016)**

New:
* added `additionalBundlerResources` for being able to have additional files available to the used bundler
* added feature for copying additionalAppResources to `target/jfx/app` when calling `jfx:jar` and `jfx:run`, making it possible to have all that files available (like native files being required to not reside in the jar-files) by setting `<copyAdditionalAppResourcesToJar>true</copyAdditionalAppResourcesToJar>`

Bugfixes:
* fixed possible file-handler leak (unreported)

Improvements:
* refactored a bit to have cleaner code



(Not yet) Release(d) Notes
==========================

upcoming Version 8.8.0 (???-feb-2017)

New:
* added detection of missing main class, wrong configuration now gets detected a bit earlier
* `nativeReleaseVersion` will now get sanitized, anything than numbers and dots are removed, this ensures compatibility with the used bundler toolsets
* signing jars using `jarsigner` was introduced some time ago, but it was lacking some custom parameters, this is now fixed by having the new `additionalJarsignerParameters`-list while using native-mojo (fixes issue #260)
* added ability to fail the build on errors while bundling, just set `<failOnError>true</failOnError>`
* when having not specified any bundler, it now is possible to remove that JNLP-warning regarding "No OutFile Specificed", which makes that bundler being skipped, just set `<skipJNLP>true</skipJNLP>` inside the `<configuration>`-block
* added property to skip `nativeReleaseVersion` rewriting, just set `<skipNativeVersionNumberSanitizing>true</skipNativeVersionNumberSanitizing>` inside the `<configuration>`-block

Improvements:
* added warning when no classes were generated for `-jfx.jar`-generation, fixes issue #233 (no real FIX, as this is no real BUG ... IMHO)
* added warning about slow performance (even on SSD) when having ext4/btrfs filesystems using "deb"-bundler (fixes issue #41)
* added warning about missing "jnlp.outfile"-property inside bundleArguments when using JNLP-bundler (from issue #42)

Changes:
* reimplemented `<additionalBundlerResources>`, now searching for folders with the name of the used bundler, makes it possible to adjust nearly all bundlers now
