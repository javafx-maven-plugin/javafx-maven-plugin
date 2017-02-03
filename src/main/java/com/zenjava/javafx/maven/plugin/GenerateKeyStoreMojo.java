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

import org.apache.maven.model.Organization;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.project.MavenProject;

/**
 * Generates a development keysstore that can be used for signing web based distribution bundles based on POM settings.
 * You only need to run this command once and then you can include the resulting keystore in your source control. There
 * is no harm in re-running the command however, it will simply overwrite the keystore with a new one.
 * <p>
 * The resulting keystore is useful for simplifying development but should not be used in a production environment. You
 * should get a legitimate certificate from a certifier and include that keystore in your codebase. Using this testing
 * keystore will result in your users seeing the ugly warning about untrusted code.
 * <p>
 * Please do not use for production.
 *
 * @goal build-keystore
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
     * Flag to turn on verbose logging. Set this to true if you are having problems and want more detailed information.
     *
     * @parameter property="jfx.verbose" default-value="false"
     */
    protected Boolean verbose;

    /**
     * All commands executed by this Maven-plugin will be done using the current available commands
     * of your maven-execution environment. It is possible to call Maven with a different version of Java,
     * so these calls might be wrong. To use the executables of the JDK used for running this maven-plugin,
     * please set this to false. You might need this in the case you installed multiple versions of Java.
     *
     * The default is to use environment relative executables.
     *
     * @parameter property="jfx.useEnvironmentRelativeExecutables" default-value="true"
     */
    protected boolean useEnvironmentRelativeExecutables;

    /**
     * Set this to true for skipping the execution.
     *
     * @parameter property="jfx.skip" default-value="false"
     */
    protected boolean skip;

    @FunctionalInterface
    private interface RequiredFieldAlternativeCallback {

        String getValue();
    }

    /**
     * Set this to true to silently overwrite the keystore. If this is set to false (the default) then if a keystore
     * already exists, this Mojo will fail with an error. This is just to stop you inadvertantly overwritting a keystore
     * you really didn't want to lose.
     *
     * @parameter property="jfx.overwriteKeyStore" default-value="false"
     */
    protected boolean overwriteKeyStore;

    /**
     * The location of the keystore. If not set, this will default to src/main/deploy/kesytore.jks which is usually fine
     * to use for most cases.
     *
     * @parameter property="jfx.keyStore" default-value="src/main/deploy/keystore.jks"
     */
    protected File keyStore;

    /**
     * The alias to use when accessing the keystore. This will default to "myalias".
     *
     * @parameter property="jfx.keyStoreAlias" default-value="myalias"
     */
    protected String keyStoreAlias;

    /**
     * The password to use when accessing the keystore. This will default to "password".
     *
     * @parameter property="jfx.keyStorePassword" default-value="password"
     */
    protected String keyStorePassword;

    /**
     * The password to use when accessing the key within the keystore. If not set, this will default to
     * keyStorePassword.
     *
     * @parameter property="jfx.keyPassword"
     */
    protected String keyPassword;

    /**
     * The 'domain' to use for the certificate. Typically this is your company's domain name.
     *
     * @parameter property="jfx.certDomain"
     */
    protected String certDomain;

    /**
     * The 'organisational unit' to use for the certificate. Your department or team name typically.
     *
     * @parameter property="jfx.certOrgUnit"
     */
    protected String certOrgUnit;

    /**
     * The 'organisation' name to use for the certificate.
     *
     * @parameter property="jfx.certOrg"
     */
    protected String certOrg;

    /**
     * The 'state' (province, etc) that your organisation is based in.
     *
     * @parameter property="jfx.certState"
     */
    protected String certState;

    /**
     * The 'country' code that your organisation is based in. This should be a proper country code, e.g. Australia is
     * 'AU'
     *
     * @parameter property="jfx.certCountry"
     */
    protected String certCountry;

    /**
     * @parameter property="jfx.additionalKeytoolParameters"
     */
    protected List<String> additionalKeytoolParameters = new ArrayList<>();

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if( skip ){
            getLog().info("Skipping execution of GenerateKeyStoreMojo MOJO.");
            return;
        }

        if( keyStore.exists() ){
            if( overwriteKeyStore ){
                if( !keyStore.delete() ){
                    throw new MojoFailureException("Unable to delete existing keystore at: " + keyStore);
                }
            } else {
                throw new MojoExecutionException("Keystore already exists (set 'overwriteKeyStore' to force) at: " + keyStore);
            }
        }

        checkKeystoreRequiredParameter(keyStoreAlias, "keyStoreAlias");
        checkKeystoreRequiredParameter(keyStorePassword, "keyStorePassword");

        if( keyPassword == null ){
            keyPassword = keyStorePassword;
        }

        List<String> distinguishedNameParts = new ArrayList<>();
        Organization projectOrganization = project.getOrganization();

        checkAndAddRequiredField(distinguishedNameParts, "certDomain", certDomain, "cn", () -> {
            if( projectOrganization != null && !StringUtils.isEmpty(projectOrganization.getUrl()) ){
                String url = projectOrganization.getUrl();
                if( url.startsWith("http://") ){
                    return url.substring("http://".length());
                }
                if( url.startsWith("https://") ){
                    return url.substring("https://".length());
                }
            }
            return null;
        });
        checkAndAddRequiredField(distinguishedNameParts, "certOrgUnit", certOrgUnit, "ou", () -> {
            return "none";
        });
        checkAndAddRequiredField(distinguishedNameParts, "certOrg", certOrg, "o", () -> {
            if( projectOrganization != null && !StringUtils.isEmpty(projectOrganization.getName()) ){
                return projectOrganization.getName();
            }
            return null;
        });
        checkAndAddRequiredField(distinguishedNameParts, "certState", certState, "st");
        checkAndAddRequiredField(distinguishedNameParts, "certCountry", certCountry, "c");

        generateKeyStore(
                keyStore, keyStoreAlias, keyStorePassword, keyPassword, String.join(", ", distinguishedNameParts)
        );
    }

    protected void generateKeyStore(File keyStore, String keyStoreAlias, String keyStorePassword, String keyPassword, String distinguishedName) throws MojoExecutionException, MojoFailureException {
        getLog().info("Generating keystore in: " + keyStore);

        try{
            // generated folder if it does not exist
            Files.createDirectories(keyStore.getParentFile().toPath());

            List<String> command = new ArrayList<>();

            command.add(getEnvironmentRelativeExecutablePath() + "keytool");
            command.add("-genkeypair");
            command.add("-keystore");
            command.add(keyStore.getPath());
            command.add("-alias");
            command.add(keyStoreAlias);
            command.add("-storepass");
            command.add(keyStorePassword);
            command.add("-keypass");
            command.add(keyPassword);
            command.add("-dname");
            command.add(distinguishedName);
            command.add("-sigalg");
            command.add("SHA256withRSA");
            command.add("-validity");
            command.add("100");
            command.add("-keyalg");
            command.add("RSA");
            command.add("-keysize");
            command.add("2048");
            Optional.ofNullable(additionalKeytoolParameters).ifPresent(additionalParameters -> {
                command.addAll(additionalParameters);
            });
            if( verbose ){
                command.add("-v");
            }

            ProcessBuilder pb = new ProcessBuilder().inheritIO().command(command);
            Process p = pb.start();
            p.waitFor();
        } catch(IOException | InterruptedException ex){
            throw new MojoExecutionException("There was an exception while generating keystore.", ex);
        }
    }

    private void checkKeystoreRequiredParameter(String value, String valueName) throws MojoExecutionException {
        if( StringUtils.isEmpty(value) ){
            throw new MojoExecutionException("The property '" + valueName + "' is required to generate a new KeyStore.");
        }
    }

    private void checkAndAddRequiredField(List<String> distinguishedNameParts, String propertyName, String value, String fieldName) throws MojoExecutionException {
        checkAndAddRequiredField(distinguishedNameParts, propertyName, value, fieldName, null);
    }

    private void checkAndAddRequiredField(List<String> distinguishedNameParts, String propertyName, String value, String fieldName, RequiredFieldAlternativeCallback alternative) throws MojoExecutionException {
        if( !StringUtils.isEmpty(value) ){
            distinguishedNameParts.add(fieldName + "=" + value);
        } else {
            if( alternative == null || StringUtils.isEmpty(alternative.getValue()) ){
                throw new MojoExecutionException("The property '" + propertyName + "' must be provided to generate a new certificate.");
            } else {
                distinguishedNameParts.add(fieldName + "=" + alternative.getValue());
            }
        }
    }

    protected String getEnvironmentRelativeExecutablePath() {
        if( useEnvironmentRelativeExecutables ){
            return "";
        }

        String jrePath = System.getProperty("java.home");
        String jdkPath = jrePath + File.separator + ".." + File.separator + "bin" + File.separator;

        return jdkPath;
    }
}
