JavaFX Maven Plugin
===================

The JavaFX Maven Plugin provides a way to assemble distribution bundles for JavaFX applications (2.2+) from within Maven.
 
For information about this plugin including licencing information and how to configure your POM, please refer to the main wiki page: 

* https://github.com/zonski/javafx-maven-plugin/wiki


Release Notes
================

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
