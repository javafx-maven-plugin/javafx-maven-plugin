package com.zenjava.javafx.maven.plugin.wrapper;

import org.codehaus.plexus.util.xml.Xpp3Dom;

public abstract class TransformerWrapper {

	
	protected final Xpp3Dom getTransformerDom() {
		Xpp3Dom transformer = new Xpp3Dom("transformer");
		transformer.setAttribute("implementation", getImplementation());
		return transformer;
	}
	
	public abstract String getImplementation();
	
	public abstract Xpp3Dom toDom();

}
