JavaFX Maven Plugin
===================

The JavaFX Maven Plugin provides a way to assemble distribution bundles for JavaFX applications (2.2+) from within Maven.
 
For information about this plugin including licencing information and how to configure your POM, please refer to the main wiki page: 

* [http://zenjava.com/javafx/maven/](http://zenjava.com/javafx/maven/)


Release Notes
================

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
