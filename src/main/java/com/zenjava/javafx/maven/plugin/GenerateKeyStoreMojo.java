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

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;

import static org.twdata.maven.mojoexecutor.MojoExecutor.*;

/**
 * Generates a development keysstore that can be used for signing web based distribution bundles based on POM settings.
 * You only need to run this command once and then you can include the resulting keystore in your source control. There
 * is no harm in re-running the command however, it will simply overwrite the keystore with a new one.
 *
 * The resulting keystore is useful for simplifying development but should not be used in a production environment. You
 * should get a legitimate certificate from a certifier and include that keystore in your codebase. Using this testing
 * keystore will result in your users seeing the ugly warning about untrusted code.
 *
 * @goal generate-key-store
 * @phase validate
 * @requiresDependencyResolution
 */
public class GenerateKeyStoreMojo extends AbstractMojo {

    /**
     * The Maven Project Object
     *
     * @parameter property="project"
     * @required
     * @readonly
     */
    protected MavenProject project;

    /**
     * The Maven Session Object
     *
     * @parameter property="session"
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
     * Set this to true to sliently overwrite the keystore. If this is set to false (the default) then if a keystore
     * already exists, this Mojo will fail with an error. This is just to stop you inadvertantly overwritting a keystore
     * you really didn't want to lose.
     *
     * @parameter default-value="false" property="overwriteKeyStore"
     */
    protected boolean overwriteKeyStore;

    /**
     * The location of the keystore. If not set, this will default to src/main/deploy/kesytore.jks which is usually fine
     * to use for most cases.
     *
     * @parameter default-value="src/main/deploy/keystore.jks"
     */
    protected File keyStore;

    /**
     * The alias to use when accessing the keystore. This will default to "myalias".
     *
     * @parameter default-value="myalias"
     */
    protected String keyStoreAlias;

    /**
     * The password to use when accessing the keystore. This will default to "password".
     *
     * @parameter default-value="password"
     */
    protected String keyStorePassword;

    /**
     * The password to use when accessing the key within the keystore. If not set, this will default to
     * keyStorePassword.
     *
     * @parameter
     */
    protected String keyPassword;

    /**
     * The 'domain' to use for the certificate. Typically this is your company's domain name.
     *
     * @parameter property="certDomain"
     */
    protected String certDomain;

    /**
     * The 'organisational unit' to use for the certificate. Your department or team name typically.
     *
     * @parameter property="certOrgUnit"
     */
    protected String certOrgUnit;

    /**
     * The 'organisation' name to use for the certificate.
     *
     * @parameter property="certOrg"
     */
    protected String certOrg;

    /**
     * The 'state' (province, etc) that your organisation is based in.
     *
     * @parameter property="certState"
     */
    protected String certState;

    /**
     * The 'country' code that your organisation is based in. This should be a proper country code, e.g. Australia is
     * 'AU'
     *
     * @parameter property="certCountry"
     */
    protected String certCountry;


    public void execute() throws MojoExecutionException, MojoFailureException {

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
