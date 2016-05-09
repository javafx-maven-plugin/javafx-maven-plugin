package com.zenjava.test.customBundlers;

import com.oracle.tools.packager.Bundler;
import com.oracle.tools.packager.BundlerParamInfo;
import com.oracle.tools.packager.ConfigException;
import com.oracle.tools.packager.UnsupportedPlatformException;
import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 *
 * @author Danny Althoff
 */
public class DummyBundler implements Bundler {

    @Override
    public String getName() {
        return "DummyBundler";
    }

    @Override
    public String getDescription() {
        return "DummyBundler - Example custom bundler of the javafx-maven-plugin";
    }

    @Override
    public String getID() {
        return "DummyBundler";
    }

    @Override
    public String getBundleType() {
        return "IMAGE";
    }

    @Override
    public Collection<BundlerParamInfo<?>> getBundleParameters() {
        return Collections.emptyList();
    }

    @Override
    public boolean validate(Map<String, ? super Object> map) throws UnsupportedPlatformException, ConfigException {
        System.out.println("DummyBundler > VALIDATING");
        return true;
    }

    @Override
    public File execute(Map<String, ? super Object> map, File file) {
        System.out.println("DummyBundler > EXECUTING");
        return null;
    }

}
