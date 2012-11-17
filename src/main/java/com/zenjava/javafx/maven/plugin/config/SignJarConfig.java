package com.zenjava.javafx.maven.plugin.config;

import java.io.File;

public class SignJarConfig {

    private boolean signJar;

    private File keyStore;

    /**
     * @required
     */
    private String alias;

    private String storePassword;

    private String keyPassword;

    private String storeType;

    public SignJarConfig() {
        this.signJar = true;
    }

    public boolean isSignJar() {
        return signJar;
    }

    public void setSignJar(boolean signJar) {
        this.signJar = signJar;
    }

    public File getKeyStore() {
        return keyStore;
    }

    public void setKeyStore(File keyStore) {
        this.keyStore = keyStore;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getStorePassword() {
        return storePassword;
    }

    public void setStorePassword(String storePassword) {
        this.storePassword = storePassword;
    }

    public String getKeyPassword() {
        return keyPassword;
    }

    public void setKeyPassword(String keyPassword) {
        this.keyPassword = keyPassword;
    }

    public String getStoreType() {
        return storeType;
    }

    public void setStoreType(String storeType) {
        this.storeType = storeType;
    }
}
