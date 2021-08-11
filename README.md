[![Maven Central](https://img.shields.io/maven-central/v/com.zenjava/javafx-maven-plugin.svg)](https://maven-badges.herokuapp.com/maven-central/com.zenjava/javafx-maven-plugin)



JavaFX Maven Plugin
===================

The JavaFX Maven Plugin provides a way to assemble distribution bundles for JavaFX applications (8+) from within Maven.

This plugin is essentially a Maven wrapper for the packaging tool that comes with JavaFX, it's called [javapackager](https://docs.oracle.com/javase/9/tools/javapackager.htm).
 
For easy configuration please use the old configurator:
**[https://zenjava.net/javafx-maven-plugin/](https://zenjava.net/javafx-maven-plugin/)**



Requirements
============
* Maven 3.5 (older versions might work too)
* Java Developer Kit 8 with at least Update 40 (does **NOT** support JKD9 or later yet)



OS-specific requirements
========================
* (Windows) EXE installers: Inno Setup
* (Windows) MSI installers: WiX (at least version 3.7)
* (Linux) DEB installers: dpkg-deb
* (Linux) RPM installers: rpmbuild
* (Mac) DMG installers: hdiutil
* (Mac) PKG installers: pkgbuild



Quickstart for JavaFX JAR
=========================

Add this to your pom.xml within to your build-plugin:

```xml
<plugin>
    <groupId>com.zenjava</groupId>
    <artifactId>javafx-maven-plugin</artifactId>
    <version>8.8.3</version>
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
    <version>8.8.3</version>
    <configuration>
        <vendor>YourCompany</vendor>
        <mainClass>your.package.with.Launcher</mainClass>
    </configuration>
</plugin>
```

To create your executable file with JavaFX-magic and some installers (please see official oracle-documentation which applications are required for this), call `mvn jfx:native`. The native launchers or installers will be placed at `target/jfx/native`.


Using `SNAPSHOT`-versions
=========================
When you report a bug and this got worked around, you might be able to have access to some -SNAPSHOT-version, please adjust your `pom.xml`:

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


Last Release Notes
==================

**Version 8.8.3 (09-feb-2017)**

Bugfixes:
* fixed `<launcherArguments>` of secondary launchers not being set correctly ([reported at the javafx-gradle-plugin](https://github.com/FibreFoX/javafx-gradle-plugin/issues/55))


(Not yet) Release(d) Notes
==========================

upcoming Version 8.10.0 (???-???-2021)

New:
* added a way to have PKCS11 signing by setting `<skipKeypassWhileSigning>true</skipKeypassWhileSigning>` and `<skipKeyStoreChecking>true</skipKeyStoreChecking>`, makes it possible to have hardware tokens
* added ability to prefix dependencies with their `groupId` by setting `<prefixWithGroupIdForClasspathDependencies>true</prefixWithGroupIdForClasspathDependencies>`, should work around the edge-case where dependencies have the same artifactId and would overwrite otherwise

Enhancement:
* ~~JDK 9 compatibility~~ (got broken with Jigsaw)
* TravisCI: use newer build machines

Documentation:
* clarified that this plugin is a wrapper, thanks to @TurekBot
