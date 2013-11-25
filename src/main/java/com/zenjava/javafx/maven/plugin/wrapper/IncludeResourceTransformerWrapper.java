package com.zenjava.javafx.maven.plugin.wrapper;

import java.io.File;

import org.codehaus.plexus.util.xml.Xpp3Dom;

public class IncludeResourceTransformerWrapper extends TransformerWrapper {

	private final String IMPLEMENTATION = "org.apache.maven.plugins.shade.resource.IncludeResourceTransformer";

	File file;

	String resource;

	@Override
	public String getImplementation() {
		return IMPLEMENTATION;
	}

	@Override
	public Xpp3Dom toDom() {
		Xpp3Dom transformer = getTransformerDom();
		transformer.addChild(getResourceDom());
		transformer.addChild(getFileDom());
		return transformer;
	}

	private Xpp3Dom getResourceDom() {
		Xpp3Dom resourceDom = new Xpp3Dom("resource");
		resourceDom.setValue(resource);
		return resourceDom;
	}

	private Xpp3Dom getFileDom() {
		Xpp3Dom fileDom = new Xpp3Dom("file");
		fileDom.setValue(file.getPath());
		return fileDom;
	}

}
