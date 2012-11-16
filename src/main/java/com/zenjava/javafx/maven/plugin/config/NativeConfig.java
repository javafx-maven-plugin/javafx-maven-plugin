package com.zenjava.javafx.maven.plugin.config;

public class NativeConfig {

    private boolean buildNativeBundles;

    private String bundleType;

    public NativeConfig() {
        this.buildNativeBundles = true;
        this.bundleType = "ALL";
    }

    public boolean isBuildNativeBundles() {
        return buildNativeBundles;
    }

    public void setBuildNativeBundles(boolean buildNativeBundles) {
        this.buildNativeBundles = buildNativeBundles;
    }

    public String getBundleType() {
        return bundleType;
    }

    public void setBundleType(String bundleType) {
        this.bundleType = bundleType;
    }
}
