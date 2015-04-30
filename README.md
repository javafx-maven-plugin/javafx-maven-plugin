[![Build Status](https://travis-ci.org/javafx-maven-plugin/javafx-maven-plugin.svg?branch=master)](https://travis-ci.org/javafx-maven-plugin/javafx-maven-plugin)
[![Maven Central](https://img.shields.io/maven-central/v/com.zenjava/javafx-maven-plugin.svg)](https://maven-badges.herokuapp.com/maven-central/com.zenjava/javafx-maven-plugin)
[![Dependency Status](https://www.versioneye.com/java/com.zenjava:javafx-maven-plugin/8.1.2/badge.svg)](https://www.versioneye.com/java/com.zenjava:javafx-maven-plugin/8.1.2)


JavaFX Maven Plugin
===================

The JavaFX Maven Plugin provides a way to assemble distribution bundles for JavaFX applications (2.2+) from within Maven.
 
For information about this plugin including licencing information and how to configure your POM, please refer to the main wiki page: 

* [http://zenjava.com/javafx/maven/(Outdated)](http://zenjava.com/javafx/maven/)
* [javafx-maven-plugin-documentation](http://pgkaila.github.io/javafx-maven-plugin/)

##How To

To use JavaFX Maven Plugin in your javafx project you need to include following maven plugin

        <plugin>
            <groupId>com.zenjava</groupId>
            <artifactId>javafx-maven-plugin</artifactId>
            <version>8.1.2</version>
            <configuration>
                <mainClass>${app.main.class}</mainClass>
                <vendor>Vendor Name</vendor>
            </configuration>
        </plugin>
        
To build native installer execute `mvn clean jfx:native`

Release Notes
================


upcoming Version (?-2015)

* Add all files from jfxAppOutputDir to application resources
* fix for bundler-lookup (return instead of continue)
* add option to select bundler (avoids creation of unwanted native packages)
* fixed compilation-warning for jvmArgs/jvmArg
* fixed keystore-generation: certCountry instead of certState
* added support for travis-ci.org
* fixed #66: updated version of used keytool-maven-plugin (changed from DSA to RSA)
* updated URL to repository/scm
* fixed #92: Fixed copy-paste problem in code related to handling userJvmArgs argument
* fixed #46: dont put lib-folder to resources when not needed/existing
* added support for checkstyle

Version 8.1.2 (4-Sep-2014)

* Fix NPE when using jfx:native goal in 8.1.1

Version 8.1.0/8.1.1 (24-Aug-2014)

* Added the parametermap Manifest Entries.
* Increase version of mojo-executor (fix issue #32) 
* desktop and menu shortcut
* port existing options over to new 8u20 API
* remove fix classpath mojo, no longer needed in JavaFX 8 
* add bundleArguments configuration option
* Fix #54 - make app name simply configurable 
* update to latest mojo-executor
* update documentation 
* Add flag to allow update of an existing jar
* upgraded to maven 3.3 syntax

Version 2.0 (1-Jul-2013)

* Complete rework of the Maven plugin from the ground up
* Changed the loading of JFX tools so as to not need the ugly reflection used previously
* Dropped the use of the separate javafx-deploy-lib module - all code is now internal to this project
* Dropped the 'uber-jar' support and now use a 'lib' directory to avoid a lot of problems people were having
* Renamed the goals to be more inline with Maven standards
* Added full site documentation now deployed as part of the Maven release process

Version 1.5 (20-Feb-2012)

* Fixed 'app name' issue that was causing native bundle builds to fail on Mac
* Added /src/main/deploy to the build classpath so you can now add things like custom native bundle icons, etc

Version 1.4 (16-Feb-2012)

* Merged in contributions from the community to fix some webstart issues and make ccs2bin optional

Version 1.3 (25-Nov-2012)

* Split bundlers into separate Mojos giving developers more control
* Webstart bundle now includes all JARs instead of working of the uber-JAR
* Added BAT file bundle generation 
* Added 'keystore' generation Mojo
* Added 'run' Mojo to launch app via Maven

Version 1.2 (19-Nov-2012)

* JNLP bundle generation including templating (via javafx-deploy-lib)
* Basic JAR signing (via Oracle JFX packaging lib)
* General enhancements and improvements to plugin configuration options

Version 1.1 (12-Nov-2012)

* First release to central Maven repository
* Basic executable JAR support
* Basic native bundling (wrapping the default JFX packaging lib)
