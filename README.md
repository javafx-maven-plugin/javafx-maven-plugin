JavaFX Maven Plugin
===================

The JavaFX Maven Plugin provides a way to to assemble distributable bundles for JavaFX applications from within Maven.
It provides a wrapper around the JavaFX packaging tools which are provided as part of the JavaFX installation (i.e. the
ANT tasks that come with JavaFX).


Setting Up
============

You must have a valid JDK installed with a valid JavaFX installation included in it. Due to legal reasons (i.e. Oracle
licensing restrictions) it was not possible to include the tools directly within this plugin so the plugin instead
searches for the tools within the JDK installation (found via the JAVA_HOME setting).

Additionally you must manually setup JavaFX to be on your system's Java path. There are several options, all of them
ugly. See http://www.zenjava.com/firstcontact/architecture/setup/install-javafx/ for one option. This one is due to the
fact that JavaFX loads it's DLLs in a way Maven can't handle. Co-bundling of JavaFX within the JRE (when it finally
happens) will remove this need eventually.


Usage
=============

The Plugin is currently available via the Zen Java Maven repository. To use it, add this to your POM:

``` xml
    <pluginRepositories>
        <pluginRepository>
            <id>zen-java</id>
            <name>Zen Java Maven Repo</name>
            <url>http://zenjava.com/maven-repo/</url>
        </pluginRepository>
    </pluginRepositories>
```

By default, the plugin will build an executable JAR (i.e. one you can double click to launch, assuming you already have
a JRE installed on your system) for your JavaFX application that contains all runtime dependencies within it.

To make this happen, add the following to your pom.xml:

``` xml
    <plugin>
        <groupId>com.zenjava</groupId>
        <artifactId>javafx-maven-plugin</artifactId>
        <version>1.0-SNAPSHOT</version>
        <executions>
            <execution>
                <phase>package</phase>
                <goals>
                    <goal>javafx</goal>
                </goals>
            </execution>
        </executions>
        <configuration>
            <mainClass>[put your application main class here]</mainClass>
        </configuration>
    </plugin>
```

Additionally, the plugin can be used to build native distributions:

``` xml
    <plugin>
        <groupId>com.zenjava</groupId>
        <artifactId>javafx-maven-plugin</artifactId>
        <version>1.0-SNAPSHOT</version>
        <executions>
            <execution>
                <phase>package</phase>
                <goals>
                    <goal>javafx</goal>
                </goals>
            </execution>
        </executions>
        <configuration>
            <mainClass>[put your application main class here]</mainClass>
            <bundleType>ALL|IMAGE|INSTALLER|NONE</bundleType>
        </configuration>
    </plugin>
```

The plugin uses several settings from your POM to determine attributes of the final bundle, in particular:

- name: is used to determine the display name used for the installation package
- organization: is used to determine the display name used for the installation package
- finalName: is used to determine the name of the bundle produced

e.g. for best results you should add some config like this to the your POM:

``` xml
    <name>JFX and JEE Sample 4</name>
    <organization>
        <name>Zen Java</name>
    </organization>
```


Support
=======

This plugin is currently very basic. It works for creating standard JAR and native distributables but it does not handle
any complex configuration for these, or support JNLP or Applet deployment.

The core framework is there for this and it would not be difficult to extend the plugin to cover these cases, however
I have no intention of doing this. If you have a need for such features you should get in contact with me to find out
how you can contribute.

Likewise, I am not intending to provide on-going support for this plugin. It is available as-is. If future releases of
JavaFX change the way the packaging tools work then this plugin is likely to break. Again, I am happy to help
contributors get started should anyone want to do this work in the future.


License
=======

Copyright 2012 Daniel Zwolenski.

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.

