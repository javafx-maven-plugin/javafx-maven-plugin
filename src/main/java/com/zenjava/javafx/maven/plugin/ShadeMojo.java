package com.zenjava.javafx.maven.plugin;

import static org.twdata.maven.mojoexecutor.MojoExecutor.artifactId;
import static org.twdata.maven.mojoexecutor.MojoExecutor.executeMojo;
import static org.twdata.maven.mojoexecutor.MojoExecutor.executionEnvironment;
import static org.twdata.maven.mojoexecutor.MojoExecutor.goal;
import static org.twdata.maven.mojoexecutor.MojoExecutor.groupId;
import static org.twdata.maven.mojoexecutor.MojoExecutor.plugin;
import static org.twdata.maven.mojoexecutor.MojoExecutor.version;

import java.io.File;
import java.util.List;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.util.xml.Xpp3Dom;

import com.zenjava.javafx.maven.plugin.wrapper.TransformerWrapper;

/**
 * 
 * @goal shade
 * @phase package
 * @requiresDependencyResolution
 */
public class ShadeMojo extends JarMojo {

	/**
	 * The Maven Session Object
	 * 
	 * @parameter expression="${session}"
	 * @required
	 * @readonly
	 */
	protected MavenSession session;

	/**
	 * The Maven PluginManager Object
	 * 
	 * @component
	 * @required
	 */
	protected BuildPluginManager pluginManager;

	/**
	 * @parameter
	 */
	protected List<TransformerWrapper> transformers;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		jfxMainAppJarName = "bootstrap";
		super.execute();
		executeShadePlugin();
	}

	private void executeShadePlugin() throws MojoExecutionException {
		String filePath = jfxAppOutputDir + File.separator + jfxMainAppJarName + ".jar";

		project.getArtifact().setFile(new File(filePath));

		executeMojo(
				plugin(
						groupId("org.apache.maven.plugins"),
						artifactId("maven-shade-plugin"),
						version("1.2.1")
				),
				goal("shade"),
				getConfiguration(),
				executionEnvironment(
						project,
						session,
						pluginManager
				)
		);
	}

	private Xpp3Dom getConfiguration() {
		Xpp3Dom configuration = new Xpp3Dom("configuration");
		if (transformers != null) {
			configuration.addChild(getTransformers());
		}
		return configuration;
	}

	private Xpp3Dom getTransformers() {
		Xpp3Dom transformersDom = new Xpp3Dom("transformers");
		for (TransformerWrapper transformer : transformers) {
			transformersDom.addChild(transformer.toDom());
		}
		return transformersDom;
	}

}
