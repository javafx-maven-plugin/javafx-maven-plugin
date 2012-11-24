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

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.archiver.manager.NoSuchArchiverException;
import org.sonatype.aether.RepositorySystem;
import org.sonatype.aether.RepositorySystemSession;
import org.sonatype.aether.repository.RemoteRepository;
import org.sonatype.aether.resolution.ArtifactRequest;
import org.sonatype.aether.resolution.ArtifactResolutionException;
import org.sonatype.aether.resolution.ArtifactResult;
import org.sonatype.aether.util.artifact.DefaultArtifact;

import java.io.File;
import java.util.List;

/**
 * @goal setup-wix
 * @requiresProject false
 */
public class BuildWinInstallerMojo extends AbstractMojo {

    /**
     * The entry point to Aether, i.e. the component doing all the work.
     *
     * @component
     */
    private RepositorySystem repoSystem;

    /**
     * The current repository/network configuration of Maven.
     *
     * @parameter default-value="${repositorySystemSession}"
     * @readonly
     */
    private RepositorySystemSession repoSession;

    /**
     * The project's remote repositories to use for the resolution of plugins and their dependencies.
     *
     * @parameter default-value="${project.remotePluginRepositories}"
     * @readonly
     */
    private List<RemoteRepository> remoteRepos;

    /**
     * To look up Archiver/UnArchiver implementations
     *
     * @component
     */
    protected ArchiverManager archiverManager;

    public void execute() throws MojoExecutionException, MojoFailureException {


   }

    protected File getWixToolset() throws MojoExecutionException {

        ArtifactRequest request = new ArtifactRequest();
        DefaultArtifact artifact = new DefaultArtifact("org.apache.maven:maven-model:3.0");
        request.setArtifact(artifact);
        request.setRepositories(remoteRepos);

        getLog().info("Resolving artifact " + artifact + " from " + remoteRepos);

        ArtifactResult result;
        try {
            result = repoSystem.resolveArtifact(repoSession, request);
        } catch (ArtifactResolutionException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }


        getLog().info("Resolved artifact " + artifact + " to " + result.getArtifact().getFile()
                + " from " + result.getRepository());

        File zipFile = result.getArtifact().getFile();

        File wixDir = new File(zipFile.getParent(), "wix-unzipped");
        if (!wixDir.exists()) {

            getLog().info("Extracting WiX binaries to: " + wixDir);
            if (!wixDir.mkdirs()) {
                throw new MojoExecutionException("Unable to create base directory to unzip WiX into: " + wixDir);
            }

            try {
                UnArchiver unArchiver = archiverManager.getUnArchiver(zipFile);
                unArchiver.setSourceFile(zipFile);
                unArchiver.setDestDirectory(wixDir);
                unArchiver.extract();
            } catch (NoSuchArchiverException e) {
                throw new MojoExecutionException("Unable to unzip WiX binaries from " + zipFile + " to " + wixDir, e);
            }

            getLog().debug("WiX binaries extracted to: " + wixDir);

        } else {

            getLog().info("Using existing extracted WiX binaries in: " + wixDir);

        }

        return wixDir;
    }
}
