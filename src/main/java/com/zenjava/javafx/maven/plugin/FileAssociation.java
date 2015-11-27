package com.zenjava.javafx.maven.plugin;

import java.io.File;

/**
 *
 * @author Danny Althoff
 */
public class FileAssociation {

    /**
     * @parameter
     */
    private String description = null;
    /**
     * @parameter
     */
    private String extensions = null;
    /**
     * @parameter
     */
    private String contentType = null;
    /**
     * @parameter
     */
    private File icon = null;

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getExtensions() {
        return extensions;
    }

    public void setExtensions(String extensions) {
        this.extensions = extensions;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public File getIcon() {
        return icon;
    }

    public void setIcon(File icon) {
        this.icon = icon;
    }

}
