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

import static com.oracle.tools.packager.AbstractBundler.IMAGES_ROOT;
import com.oracle.tools.packager.BundlerParamInfo;
import com.oracle.tools.packager.ConfigException;
import com.oracle.tools.packager.StandardBundlerParam;
import static com.oracle.tools.packager.StandardBundlerParam.VERSION;
import java.io.File;
import java.util.regex.Pattern;

/**
 * Using BundlerParamInfo fields from bundlers directly is not possible since JDK 9, therefor
 * this class contains them for usage inside the MOJOs.
 */
public class ParameterMapEntries {

    // windows
    public static final BundlerParamInfo<File> EXE_IMAGE_DIR = new StandardBundlerParam<>(
            "", // name, not required for us
            "", // description, not required for us
            "win.exe.imageDir", // string-key inside map
            File.class, // type
            params -> { // default value
                File imagesRoot = IMAGES_ROOT.fetchFrom(params);
                if( !imagesRoot.exists() ){
                    imagesRoot.mkdirs();
                }
                return new File(imagesRoot, "win-exe.image");
            },
            (s, p) -> null); // string to non-string-conversion

    public static final BundlerParamInfo<File> MSI_IMAGE_DIR = new StandardBundlerParam<>(
            "", // name, not required for us
            "", // description, not required for us
            "win.msi.imageDir", // string-key inside map
            File.class, // type
            params -> { // default value
                File imagesRoot = IMAGES_ROOT.fetchFrom(params);
                if( !imagesRoot.exists() ){
                    imagesRoot.mkdirs();
                }
                return new File(imagesRoot, "win-msi.image");
            },
            (s, p) -> null); // string to non-string-conversion
    // linux

    private static final Pattern RPM_BUNDLE_NAME_PATTERN = Pattern.compile("[a-z\\d\\+\\-\\.\\_]+", Pattern.CASE_INSENSITIVE);

    public static final BundlerParamInfo<String> RPM_BUNDLE_NAME = new StandardBundlerParam<>(
            "", // name, not required for us
            "", // description, not required for us
            "linux.bundleName", // string-key inside map
            String.class, // type
            params -> { // default value
                String nm = StandardBundlerParam.APP_NAME.fetchFrom(params);
                if( nm == null ){
                    return null;
                }
                return nm.toLowerCase().replaceAll("[ ]", "-");
            },
            (s, p) -> { // string to non-string-conversion
                if( !RPM_BUNDLE_NAME_PATTERN.matcher(s).matches() ){
                    throw new IllegalArgumentException(
                            new ConfigException(new Exception("Bundle-Name was not compliant"))
                    );
                }

                return null;
            }
    );

    public static final BundlerParamInfo<File> RPM_IMAGE_DIR = new StandardBundlerParam<>(
            "", // name, not required for us
            "", // description, not required for us
            "linux.rpm.imageDir", // string-key inside map
            File.class, // type
            params -> { // default value
                File imagesRoot = IMAGES_ROOT.fetchFrom(params);
                if( !imagesRoot.exists() ){
                    imagesRoot.mkdirs();
                }
                return new File(imagesRoot, "linux-rpm.image");
            },
            (s, p) -> new File(s)); // string to non-string-conversion

    public static final BundlerParamInfo<String> DEB_FULL_PACKAGE_NAME = new StandardBundlerParam<>(
            "", // name, not required for us
            "", // description, not required for us
            "linux.deb.fullPackageName", // string-key inside map
            String.class, // type
            params -> RPM_BUNDLE_NAME.fetchFrom(params) + "-" + VERSION.fetchFrom(params), // default value
            (s, p) -> s); // string to non-string-conversion

    public static final BundlerParamInfo<File> DEB_IMAGE_DIR = new StandardBundlerParam<>(
            "", // name, not required for us
            "", // description, not required for us
            "linux.deb.imageDir", // string-key inside map
            File.class, // type
            params -> { // default value
                File imagesRoot = IMAGES_ROOT.fetchFrom(params);
                if( !imagesRoot.exists() ){
                    imagesRoot.mkdirs();
                }
                return new File(new File(imagesRoot, "linux-deb.image"), DEB_FULL_PACKAGE_NAME.fetchFrom(params));
            },
            (s, p) -> new File(s)); // string to non-string-conversion

    private ParameterMapEntries() {
        // utility class
    }

}
