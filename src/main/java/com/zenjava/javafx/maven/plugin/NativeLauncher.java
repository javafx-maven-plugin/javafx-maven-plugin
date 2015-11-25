/*
 * Copyright 2012 Daniel Zwolenski.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.zenjava.javafx.maven.plugin;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * Data transfer object for configuring secondary native launchers.
 *
 * @author Danny Althoff
 */
public class NativeLauncher {

    /**
     * This has to be different than original appname, as all existing parameter are copied and this would be overwritten.
     *
     * @parameter
     * @required
     */
    private String appName = null;

    /**
     * @parameter
     */
    private String mainClass = null;

    /**
     * @parameter
     */
    private File jfxMainAppJarName = null;

    /**
     * @parameter
     */
    private Map<String, String> jvmProperties = null;

    /**
     * @parameter
     */
    private List<String> jvmArgs = null;

    /**
     * @parameter
     */
    private Map<String, String> userJvmArgs = null;

    /**
     * @parameter default-value="1.0"
     */
    private String nativeReleaseVersion;

    /**
     * @parameter default-value=false
     */
    private boolean needShortcut;

    /**
     * @parameter default-value=false
     */
    private boolean needMenu;

    public String getMainClass() {
        return mainClass;
    }

    public File getJfxMainAppJarName() {
        return jfxMainAppJarName;
    }

    public String getAppName() {
        return appName;
    }

    public Map<String, String> getJvmProperties() {
        return jvmProperties;
    }

    public List<String> getJvmArgs() {
        return jvmArgs;
    }

    public Map<String, String> getUserJvmArgs() {
        return userJvmArgs;
    }

    public String getNativeReleaseVersion() {
        return nativeReleaseVersion;
    }

    public boolean isNeedShortcut() {
        return needShortcut;
    }

    public boolean isNeedMenu() {
        return needMenu;
    }

}
