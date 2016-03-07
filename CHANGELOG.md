Release Notes
=============

Version 8.3.0 (07-Mar-2015)

Bugfixes:
* bugfix #182 replace backslash with normal slash within JNLP-files
* bugfix #185 add signing-feature for bundler with ID "jnlp" (by setting `<jnlp.allPermisions>true</jnlp.allPermisions>` inside bundleArguments)
* fixed size-attributes within JNLP-files when using bundler with ID "jnlp" and requesting to sign the jars

New:
* added possibility for adding file associations
* added new property to disable backslash-fix `<skipNativeLauncherWorkaround182>true</skipNativeLauncherWorkaround182>`
* added new property to disable signing referenced jar-files `<skipSigningJarFilesJNLP185>true</skipSigningJarFilesJNLP185>`
* added new property to skip size-recalculation for jar-files inside generated JNLP-files `<skipSizeRecalculationForJNLP185>true</skipSizeRecalculationForJNLP185>`


Version 8.2.0 (26-Nov-2015)
Bugfixes:
* bugfix #159 added support for openjdk/openjfx (version-checker failed with NumberFormatException due to wrong expectations)
* added workaround for bug #167 regarding native windows launcher configuration-file (cfg-file), bug is inside Oracle JDK since 1.8.0 Update 60 (to work around this, this plugin tries to enforce property-file-format, which does not contain the problem)

New:
* added new property to disable workaround `<skipNativeLauncherWorkaround167>true</skipNativeLauncherWorkaround167>`
* added new mojo: calling `mvn jfx:list-bundlers` shows currently available bundlers with ID, name and descriptions, including their specific arguments able to be passed via `<bundleArguments>`-configuration
* added possibility for "secondary launchers", makes it possible to have more than one native launcher
* added possibility to filter dependencies while putting required JAR-files into the generated lib-folder
* added property to disable transitive filtering
* added CONTRIBUTING-file (fixes #125)

Improvements:
* added some IT-projects and updated others
* updated to more Java 8 syntax
* added [Windows-CI via AppVeyor](http://www.appveyor.com/) (including status badge)


Version 8.1.5 (24-Sep-2015)
* added workaround for bug #124 regarding native launcher, bug is inside Oracle JDK since 1.8.0 Update 40 (thanks to Jens Deters and Stefan Helfrich for testing/reporting helping information)
* added new property to disable workaround `<skipNativeLauncherWorkaround124>true</skipNativeLauncherWorkaround124>`


Version 8.1.4 (12-Sep-2015)
* add `packager.jar` from system-scoped dependencies to generated lib-folder, enables the usage of UserJvmOptionsService-class, which requires java 1.8.40 (see https://docs.oracle.com/javase/8/docs/technotes/guides/deploy/jvm_options_api.html)
* introduced `.editorconfig`-file (more about this feature can be found [on their website](http://editorconfig.org/)) for better pull-requests (this is just an experiment, maybe it helps)
* added missing runtime-dependency for running `mvn jfx:generate-key-store`
* exluded JRE from native-bundles on IT-projects (reduces build-time and avoids unneeded file-generation, nicer for SSDs)
* new goal available for generating keystore using maven-lifecycle
* fixed some typos
* new website online


Version 8.1.3 (24-Jul-2015)

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
* check "jfxMainAppJarName" ending with JAR


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
