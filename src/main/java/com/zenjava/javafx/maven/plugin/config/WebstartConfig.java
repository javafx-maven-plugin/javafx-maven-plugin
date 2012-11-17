package com.zenjava.javafx.maven.plugin.config;

import java.io.File;

public class WebstartConfig {

    private boolean buildWebstartBundle;

    private String outputDir;
    private String jnlpFileName;
    private File jnlpTemplate;
    private String title;
    private String vendor;
    private String description;
    private String homepage;
    private String icon;
    private String splashImage;
    private boolean offlineAllowed;
    private String jreVersion;
    private String jreArgs;
    private String jfxVersion;
    private String jarFileName;
    private boolean requiresAllPermissions;
    private String mainClass;

    private boolean buildHtmlFile;
    private String htmlTemplate;
    private String htmlFileName;

    public WebstartConfig() {
        this.buildWebstartBundle = true;
        this.buildHtmlFile = true;
    }

    public boolean isBuildWebstartBundle() {
        return buildWebstartBundle;
    }

    public void setBuildWebstartBundle(boolean buildWebstartBundle) {
        this.buildWebstartBundle = buildWebstartBundle;
    }

    public String getOutputDir() {
        return outputDir;
    }

    public void setOutputDir(String outputDir) {
        this.outputDir = outputDir;
    }

    public String getJnlpFileName() {
        return jnlpFileName;
    }

    public void setJnlpFileName(String jnlpFileName) {
        this.jnlpFileName = jnlpFileName;
    }

    public File getJnlpTemplate() {
        return jnlpTemplate;
    }

    public void setJnlpTemplate(File jnlpTemplate) {
        this.jnlpTemplate = jnlpTemplate;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getVendor() {
        return vendor;
    }

    public void setVendor(String vendor) {
        this.vendor = vendor;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getHomepage() {
        return homepage;
    }

    public void setHomepage(String homepage) {
        this.homepage = homepage;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getSplashImage() {
        return splashImage;
    }

    public void setSplashImage(String splashImage) {
        this.splashImage = splashImage;
    }

    public boolean isOfflineAllowed() {
        return offlineAllowed;
    }

    public void setOfflineAllowed(boolean offlineAllowed) {
        this.offlineAllowed = offlineAllowed;
    }

    public String getJreVersion() {
        return jreVersion;
    }

    public void setJreVersion(String jreVersion) {
        this.jreVersion = jreVersion;
    }

    public String getJreArgs() {
        return jreArgs;
    }

    public void setJreArgs(String jreArgs) {
        this.jreArgs = jreArgs;
    }

    public String getJfxVersion() {
        return jfxVersion;
    }

    public void setJfxVersion(String jfxVersion) {
        this.jfxVersion = jfxVersion;
    }

    public String getJarFileName() {
        return jarFileName;
    }

    public void setJarFileName(String jarFileName) {
        this.jarFileName = jarFileName;
    }

    public String getMainClass() {
        return mainClass;
    }

    public void setMainClass(String mainClass) {
        this.mainClass = mainClass;
    }

    public boolean isBuildHtmlFile() {
        return buildHtmlFile;
    }

    public void setBuildHtmlFile(boolean buildHtmlFile) {
        this.buildHtmlFile = buildHtmlFile;
    }

    public String getHtmlTemplate() {
        return htmlTemplate;
    }

    public void setHtmlTemplate(String htmlTemplate) {
        this.htmlTemplate = htmlTemplate;
    }

    public String getHtmlFileName() {
        return htmlFileName;
    }

    public void setHtmlFileName(String htmlFileName) {
        this.htmlFileName = htmlFileName;
    }

    public boolean isRequiresAllPermissions() {
        return requiresAllPermissions;
    }

    public void setRequiresAllPermissions(boolean requiresAllPermissions) {
        this.requiresAllPermissions = requiresAllPermissions;
    }
}
