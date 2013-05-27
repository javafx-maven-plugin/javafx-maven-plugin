package com.zenjava.javafx.maven.plugin;

import com.sun.javafx.tools.packager.Log;
import com.sun.javafx.tools.packager.PackagerLib;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.project.MavenProject;

import java.io.File;

public abstract class AbstractJfxToolsMojo extends AbstractMojo {

    /**
     * The Maven Project Object
     *
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    protected MavenProject project;

    /**
     * @parameter expression="${verbose}" default-value="false"
     */
    protected Boolean verbose;

    /**
     * @parameter expression="${project.build.directory}/jfx/app"
     */
    protected File jfxAppOutputDir;

    /**
     * @parameter expression="${project.build.finalName}-jfx.jar"
     */
    protected String jfxMainAppJarName;


    private PackagerLib packagerLib;


    public PackagerLib getPackagerLib() {
        if (packagerLib == null) {
            Log.Logger logger = new Log.Logger(verbose);
            Log.setLogger(logger);
            this.packagerLib = new PackagerLib();
        }
        return this.packagerLib;
    }
}
