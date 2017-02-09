Release Notes
=============

Version 8.8.3 (09-feb-2017)

Bugfixes:
* fixed `<launcherArguments>` of secondary launchers not being set correctly ([reported at the javafx-gradle-plugin](https://github.com/FibreFoX/javafx-gradle-plugin/issues/55))


Version 8.8.2 (09-feb-2017)

Bugfixes:
* fixed `<nativeReleaseVersion>` missing it's default-value (issue #275)


Version 8.8.1 (06-feb-2017)

Bugfixes:
* the new option `<useLibFolderContentForManifestClasspath>` did not calculate the paths correctly (issue #271)


Version 8.8.0 (05-feb-2017)

New:
* added detection of missing main class, wrong configuration now gets detected a bit earlier, to disable scanning, just set `<skipMainClassScanning>true</skipMainClassScanning>` (might cause the build-time to increase when enabled)
* `nativeReleaseVersion` will now get sanitized, anything than numbers and dots are removed, this ensures compatibility with the used bundler toolsets
* signing jars using `jarsigner` was introduced some time ago, but it was lacking some custom parameters, this is now fixed by having the new `additionalJarsignerParameters`-list while using native MOJO (fixes issue #260)
* generating a keystore has some hardcoded parameters (like keysize or used algorithm), but was missing support for additional parameters, this is now fixed by having the new `additionalKeytoolParameters`-list while using generate-key-store MOJO
* added ability to fail the build on errors while bundling, just set `<failOnError>true</failOnError>`
* when having not specified any bundler, it now is possible to remove that JNLP-warning regarding "No OutFile Specificed", which makes that bundler being skipped, just set `<skipJNLP>true</skipJNLP>` inside the `<configuration>`-block
* added property to skip `nativeReleaseVersion` rewriting, just set `<skipNativeVersionNumberSanitizing>true</skipNativeVersionNumberSanitizing>` inside the `<configuration>`-block
* added `skipCopyingDependencies` to make it possible to NOT copying dependencies, but they are added to the classpath inside the manifest like normal
* added `<fixedManifestClasspath>` for setting the classpath-entry inside the generated manifest-file in the main jfx-jar, this is already possible for secondary launchers by setting `<classpath>` within the configuration-block of the secondary launcher
* added `<useLibFolderContentForManifestClasspath>` for creating the manifest-entriy for the classpath, depending on the content of the lib-folder, makes it possible to have files not being inside dependencies being present there (which got copied beforehand)

Improvements:
* added warning when no classes were generated for `-jfx.jar`-generation, fixes issue #233 (no real FIX, as this is no real BUG ... IMHO)
* added warning about slow performance (even on SSD) when having ext4/btrfs filesystems using "deb"-bundler (fixes issue #41)
* added warning about missing "jnlp.outfile"-property inside bundleArguments when using JNLP-bundler (from issue #42)
* added ability to change name of the lib-folder by setting `libFolderName`

Changes:
* reimplemented `<additionalBundlerResources>`, now searching for folders with the name of the used bundler, makes it possible to adjust nearly all bundlers now
* all parameters are now accessible via `jfx.`-prefixed properties, please adjust your properties accordingly (I hope this does not break much for you)


Version 8.7.0 (09-Sept-2016)

New:
* added `additionalBundlerResources` for being able to have additional files available to the used bundler
* added feature for copying additionalAppResources to `target/jfx/app` when calling `jfx:jar` and `jfx:run`, making it possible to have all that files available (like native files being required to not reside in the jar-files) by setting `<copyAdditionalAppResourcesToJar>true</copyAdditionalAppResourcesToJar>`

Bugfixes:
* fixed possible file-handler leak (unreported)

Improvements:
* refactored a bit to have cleaner code


Version 8.6.0 (01-Sept-2016)

New:
* added new property `useEnvironmentRelativeExecutables` to make sure having the correct executables used, required when having multiple installations of java, just set this to false for using the JDK used for executing maven  (this got migrated from the [javafx-gradle-plugin](https://github.com/FibreFoX/javafx-gradle-plugin))
* added new property `runAppParameter` for specifying application parameters passed to the execution call `java -jar` while developing your application (this fixes #176, because that issue got valid as the `mvn jfx:run` goal is valid again after the removal of the `exec-maven-plugin`)
* added new property `runJavaParameter` for having additional settings passed to the execution call used for running your javafx-application, makes it possible to specify javassist-parameters now (and much more)

Bugfixes:
* fixed tests not running on MacOSX due to different paths exceptations (thanks @sa-wilson)

Improvements:
* cleanup of some unused parameters
* fixed missing "s" inside description about `jfx:list-bundlers`-mojo


Version 8.5.0 (30-May-2016)

Bugfixes:
* updated workaround-detection for creating native bundles without JRE, because [it got fixed by latest Oracle JDK 1.8.0u92](http://www.oracle.com/technetwork/java/javase/2col/8u92-bugfixes-2949473.html)
* added workaround for native linux launcher inside native linux installer bundle (DEB and RPM) not working, see issue [#205](https://github.com/javafx-maven-plugin/javafx-maven-plugin/issues/205) for more details on this (it's a come-back of the [issue 124](https://github.com/javafx-maven-plugin/javafx-maven-plugin/issues/124))

New:
* added ability to write and use custom bundlers! This makes it possible to customize the work which is required for your bundling-process.
* added new property to disable "native linux launcher inside native linux installer"-fix `<skipNativeLauncherWorkaround205>true</skipNativeLauncherWorkaround205>`

Improvements:
* added IT-project "23-simple-custom-bundler"
* added IT-project "24-simple-custom-bundler-failed", which fails to use custom bundler, but does not fail (normal behaviour)
* added IT-projects regarding workaround for issue 205 (currenty they do nothing, I still need to write some verify-beanshell files)
* moved workarounds and workaround-detection into its own class (makes it a bit easier to concentrate on the main work inside NativeMojo)


Version 8.4.0 (11-Mar-2015)

New:
* when creating JNLP-files, your can now choose between Blob Signing (which was introduced since JavaFX but seems has never worked, and will be removed from Java 9) or normal signing done by `jarsigner` by providing the new proverty `<noBlobSigning>true</noBlobSigning>`
* the `run` goal got its deprecation removed, you can call `mvn jfx:run` now again to start your application like you would start a normal executable-jar (no more calling `java -jar yourProject-jfx.jar`)

Improvements:
* removed `org.twdata.maven:mojo-executor`-dependency
* fixed maven-plugin dependencies
* generating keystore now directly uses `keytool`
* changed appveyor to build/test using x86 and x64


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
