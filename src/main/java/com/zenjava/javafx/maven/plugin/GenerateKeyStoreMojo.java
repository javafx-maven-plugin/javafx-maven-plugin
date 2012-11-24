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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;

import static org.twdata.maven.mojoexecutor.MojoExecutor.*;

/**
 * @goal generate-key-store
 * @phase validate
 * @requiresDependencyResolution
 */
public class GenerateKeyStoreMojo extends AbstractBundleMojo {

    /**
     * @parameter default-value="false" expression="${overwriteKeyStore}"
     */
    protected boolean overwriteKeyStore;

    /**
     * @parameter expression="${certDomain}"
     */
    protected String certDomain;

    /**
     * @parameter expression="${certOrgUnit}"
     */
    protected String certOrgUnit;

    /**
     * @parameter expression="${certOrg}"
     */
    protected String certOrg;

    /**
     * @parameter expression="${certState}"
     */
    protected String certState;

    /**
     * @parameter expression="${certCountry}"
     */
    protected String certCountry;


    public void execute() throws MojoExecutionException, MojoFailureException {

        File keyStore = this.keyStore;
        if (keyStore == null) {
            keyStore = new File(project.getBasedir(), "/src/main/deploy/keystore.jks");
        }
        if (keyStore.exists()) {
            if (overwriteKeyStore) {
                if (!keyStore.delete()) {
                    throw new MojoFailureException("Unable to delete existing keystore at: " + keyStore);
                }
            } else {
                throw new MojoExecutionException("Keystore already exists (set 'overwriteKeyStore' to force) at: " + keyStore);
            }
        }

        if (StringUtils.isEmpty(keyStoreAlias)) {
            throw new MojoExecutionException("A 'keyStoreAlias' is required to generate a new KeyStore");
        }

        if (StringUtils.isEmpty(keyStorePassword)) {
            throw new MojoExecutionException("A 'keyStorePassword' is required to generate a new KeyStore");
        }

        if (keyPassword == null) {
            keyPassword = keyStorePassword;
        }

        StringBuilder domainName = new StringBuilder();

        if (certDomain != null) {
            domainName.append("cn=").append(certDomain);
        } else if (project.getOrganization() != null && project.getOrganization().getUrl() != null) {
            String url = project.getOrganization().getUrl();
            if (url.startsWith("http://")) {
                url = url.substring("http://".length());
            }
            domainName.append("cn=").append(url);
        } else {
            throw new MojoExecutionException("A 'certDomain' must be provided to generate a KeyStore");
        }

        domainName.append("ou=").append(certOrgUnit != null ? certOrgUnit : "none");

        if (certOrg != null) {
            domainName.append("o=").append(certOrg);
        } else if (project.getOrganization() != null && project.getOrganization().getName() != null) {
            domainName.append("o=").append(project.getOrganization().getName());
        } else {
            throw new MojoExecutionException("A 'certOrg' must be provided to generate a KeyStore");
        }

        if (certState != null) {
            domainName.append("st=").append(certState);
        } else {
            throw new MojoExecutionException("A 'certState' must be provided to generate a KeyStore");
        }

        if (certCountry != null) {
            domainName.append("c=").append(certState);
        } else {
            throw new MojoExecutionException("A 'certCountry' must be provided to generate a KeyStore");
        }

        generateKeyStore(
            keyStore, keyStoreAlias, keyStorePassword, keyPassword, domainName.toString()
        );
    }

    protected void generateKeyStore(File keyStore,
                                    String keyStoreAlias,
                                    String keyStorePassword,
                                    String keyPassword,
                                    String domainName)

            throws MojoExecutionException,
                   MojoFailureException {

        getLog().info("Generating keystore in: " + keyStore);

        executeMojo(
                plugin(
                        groupId("org.codehaus.mojo"),
                        artifactId("keytool-maven-plugin"),
                        version("1.2")
                ),
                goal("generateKeyPair"),
                configuration(
                        element(name("keystore"), keyStore.getPath()),
                        element(name("alias"), keyStoreAlias),
                        element(name("storepass"), keyStorePassword),
                        element(name("keypass"), keyPassword),
                        element(name("dname"), domainName),

                        element(name("sigalg"), "SHA1withDSA"),
                        element(name("ext"), ""),
                        element(name("validity"), "100"),
                        element(name("keyalg"), "DSA"),
                        element(name("keysize"), "1024")
                ),
                executionEnvironment(
                        project,
                        session,
                        pluginManager
                )
        );
    }

}
