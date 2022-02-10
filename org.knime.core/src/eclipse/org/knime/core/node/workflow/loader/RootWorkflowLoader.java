/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ---------------------------------------------------------------------
 *
 * History
 *   9 Feb 2022 (carlwitt): created
 */
package org.knime.core.node.workflow.loader;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.config.base.ConfigBase;
import org.knime.core.node.config.base.ConfigBaseRO;
import org.knime.core.node.workflow.FileWorkflowPersistor;
import org.knime.core.node.workflow.UnsupportedWorkflowVersionException;
import org.knime.core.node.workflow.WorkflowLoadHelper.UnknownKNIMEVersionLoadPolicy;
import org.knime.core.node.workflow.def.CoreToDefUtil;
import org.knime.core.util.LoadVersion;
import org.knime.core.util.Version;
import org.knime.core.workflow.def.ConfigMapDef;
import org.knime.core.workflow.def.CredentialPlaceholderDef;
import org.knime.core.workflow.def.RootWorkflowDef;
import org.knime.core.workflow.def.WorkflowDef;
import org.knime.core.workflow.def.impl.CredentialPlaceholderDefBuilder;
import org.knime.core.workflow.def.impl.RootWorkflowDefBuilder;

/**
 * Loads a workflow project, i.e., a top-level workflow that is not contained in another workflow.
 *
 * @author Carl Witt, KNIME AG, Zurich, Switzerland
 */
public class RootWorkflowLoader {

    /** Lists element identifiers for different workflow information. */
    enum ConfigKey {
            /**
             * @see {@link RootWorkflowDef#isCreatorIsNightly()}
             * @see {@link KNIMEConstants#isNightlyBuild()}
             */
            CREATOR_IS_NIGHTLY("created_by_nightly"),
            /** @see {@link RootWorkflowDef#getCreatorVersion()} */
            CREATOR_KNIME_VERSION("created_by"),
            /** @see {@link CredentialPlaceholderDef#getLogin()} */
            CREDENTIAL_PLACEHOLDER_LOGIN("login"),
            /** @see {@link CredentialPlaceholderDef#getName()} */
            CREDENTIAL_PLACEHOLDER_NAME("name"),
            /** @see {@link RootWorkflowDef#getCredentialPlaceholders()} */
            CREDENTIAL_PLACEHOLDERS("workflow_credentials"),
            /** @see {@link RootWorkflowDef#getTableBackendSettings()} */
            TABLE_BACKEND("tableBackend"),
            /** @see {@link RootWorkflowDef#getWorkflowFormatVersion()} */
            WORKFLOW_FORMAT_VERSION("version");

        String m_key;

        ConfigKey(final String key) {
            m_key = key;
        }
    }

    private final RootWorkflowDefBuilder m_builder = new RootWorkflowDefBuilder();

    private ConfigBase m_workflowDescription = new SimpleConfig("root");

    private LoadVersion m_workflowFormatVersion;

    /**
     * Loads the project specific information (load version, etc.) and the actual workflow.
     *
     * @param directory The directory to load from
     * @throws IOException If an IO error occurred
     * @throws UnsupportedWorkflowVersionException If the workflow is of an unsupported version
     */
    public void load(final File directory) throws IOException, UnsupportedWorkflowVersionException {

        File dotKnime = FileWorkflowLoader.getKnimeFile(directory);

        try (var fis = new FileInputStream(dotKnime); var bis = new BufferedInputStream(fis)) {
            m_workflowDescription.load(bis);
        }

        m_workflowFormatVersion = parseVersion(loadWorkflowFormatVersionString(dotKnime, m_workflowDescription));
        var creatorVersion = loadCreatorVersion(m_workflowDescription);
        var creatorIsNightly = m_workflowDescription.getBoolean(ConfigKey.CREATOR_IS_NIGHTLY.m_key, false); // added in 3.5.0

        var loadPolicy = getUnknownKNIMEVersionLoadPolicy(m_workflowFormatVersion, creatorVersion, creatorIsNightly);
        var isSetDirtyAfterLoad =
            isVersionCompatible(m_workflowFormatVersion, creatorVersion, creatorIsNightly, loadPolicy);
        if (isSetDirtyAfterLoad) {
            // TODO
            //            persistor.setDirtyAfterLoad();
        }

        getBuilder()//
            .setWorkflowFormatVersion(m_workflowFormatVersion.getVersionString())//
            .setCreatorIsNightly(creatorIsNightly)//
            .setWorkflow(loadWorkflow(directory))//
            .setTableBackendSettings(loadTableBackendSettings(m_workflowDescription))//
            .setCredentialPlaceholders(loadCredentialPlaceholders(m_workflowDescription));
        Optional.ofNullable(creatorVersion).map(Version::toString).ifPresent(getBuilder()::setSavedWithVersion);
    }

    /**
     * Callback if an unknown version string is encountered in the KNIME workflow. This method gets only called if
     * either
     * <ol>
     * <li>the version of the workflow comes from the future (like loading a 5.x workflow in KNIME 3.5)</li>
     * <li>the workflow was created using a {@linkplain KNIMEConstants#isNightlyBuild() NIGHTLY build} and the current
     * instance is not a NIGHTLY build</li>
     * </ol>
     *
     * @param workflowKNIMEVersion The load version of the workflow(.knime) itself (includes table formats, workflow
     *            connection table etc), not null.
     * @param createdByKNIMEVersion The version that created the workflow, corresponds to
     *            {@link KNIMEConstants#VERSION}. This may be null in case the workflow.knime file is corrupt.
     * @param isNightlyBuild Whether the workflow was saved using a nightly build
     *            {@link KNIMEConstants#isNightlyBuild()}.
     * @return A non-null policy (this implementation returns "Abort").
     * @since 3.7
     */
    // see also org.knime.core.node.workflow.FileWorkflowPersistor.saveHeader(NodeSettings)
    public UnknownKNIMEVersionLoadPolicy getUnknownKNIMEVersionLoadPolicy(final LoadVersion workflowKNIMEVersion,
        final Version createdByKNIMEVersion, final boolean isNightlyBuild) {
        return UnknownKNIMEVersionLoadPolicy.Abort;
    }

    RootWorkflowDefBuilder getBuilder() {
        return m_builder;
    }

    RootWorkflowDef getDef() {
        return getBuilder().build();
    }

    /**
     * TODO copied from {@link FileWorkflowPersistor} Parse the version string, return {@link LoadVersion#FUTURE} if it
     * can't be parsed.
     */
    static LoadVersion parseVersion(final String versionString) {
        boolean isBeforeV2 = versionString.equals("0.9.0");
        isBeforeV2 |= versionString.equals("1.0");
        isBeforeV2 |= versionString.matches("1\\.[01234]\\.[0-9].*");
        if (isBeforeV2) {
            return LoadVersion.UNKNOWN;
        }
        return LoadVersion.get(versionString).orElse(LoadVersion.FUTURE);
    }

    /**
     * @param directory that contains the workflow
     */
    private WorkflowDef loadWorkflow(final File directory) {
        var loader = new FileWorkflowLoader(m_workflowFormatVersion);
        return loader.load(directory, m_workflowDescription);
    }

    /**
     * @param settings parsed contents of workflow.knime
     * @return the version of the KNIME instance that was used to create the workflow.
     */
    private static Version loadCreatorVersion(final ConfigBase settings) {
        Version createdWith = null;
        var createdWithString = settings.getString(ConfigKey.CREATOR_KNIME_VERSION.m_key, null);
        if (createdWithString != null) {
            try {
                createdWith = new Version(createdWithString);
            } catch (IllegalArgumentException e) {
                // ideally this goes into the 'LoadResult' but it's not instantiated yet
                // TODO
                //                LOGGER.warn(String.format("Unable to parse version string \"%s\" (file \"%s\"): %s", createdBy,
                //                    dotKNIME.getAbsolutePath(), e.getMessage()), e);
            }
        }
        return createdWith;
    }

    /**
     * Loads credentials, this method returns an empty list. Credentials added for v2.2
     *
     * @param parsed contents of workflow.knime
     * @return the credentials list
     * @throws InvalidSettingsException If this fails for any reason.
     */
    private List<CredentialPlaceholderDef> loadCredentialPlaceholders(final ConfigBaseRO settings) {
        try {
            if (m_workflowFormatVersion.isOlderThan(LoadVersion.V220)) {
                // no credentials in v2.1 and before
                return List.of();
            }

            ConfigBaseRO sub = settings.getConfigBase(ConfigKey.CREDENTIAL_PLACEHOLDERS.m_key);
            List<CredentialPlaceholderDef> placeholders = new ArrayList<>();
            Set<String> unique = new HashSet<>();
            for (String key : sub.keySet()) {
                ConfigBaseRO placeholder = sub.getConfigBase(key);
                CredentialPlaceholderDef p = loadCredentialPlaceholder(placeholder);
                if (!unique.add(p.getName())) {
                    // TODO
                    //                    getLogger().warn("Duplicate credentials variable \"" + c.getName() + "\" -- ignoring it");
                } else {
                    placeholders.add(p);
                }
            }
            return placeholders;
        } catch (InvalidSettingsException e) {
            // TODO
            //            String error = "Unable to load credentials: " + e.getMessage();
            //            getLogger().debug(error, e);
            //            setDirtyAfterLoad();
            //            loadResult.addError(error);
            return List.of();
        }
    }

    private static CredentialPlaceholderDef loadCredentialPlaceholder(final ConfigBaseRO settings)
        throws InvalidSettingsException {
        return new CredentialPlaceholderDefBuilder()//
            .setName(settings.getString(ConfigKey.CREDENTIAL_PLACEHOLDER_NAME.m_key))//
            .setLogin(settings.getString(ConfigKey.CREDENTIAL_PLACEHOLDER_LOGIN.m_key))//
            .build();
        // TODO move to persistor
        // request to initialize credentials - if available
        //           if (m_credentials != null && !m_credentials.isEmpty()) {
        //               m_credentials = m_loadHelper.loadCredentialsPrefilled(m_credentials);
        //           }
        // TODO move to persistor, then inline this method
        //            try {
        //                Credentials result = new Credentials(name);
        //                result.setLogin(login);
        //                return result;
        //            } catch (Exception e) {
        //                throw new InvalidSettingsException("Can't create credentials for "
        //                        + "name \"" + name + "\": " + e.getMessage(), e);
        //            }
    }

    /**
     * @param settings the parsed contents of the workflow.knime XML file
     * @return
     */
    private static ConfigMapDef loadTableBackendSettings(final ConfigBaseRO settings) {
        try {
            if (settings.containsKey(ConfigKey.TABLE_BACKEND.m_key)) {
                return CoreToDefUtil.toConfigMapDef(settings.getConfigBase(ConfigKey.TABLE_BACKEND.m_key));
            }
        } catch (InvalidSettingsException e) {
            // TODO
            //            var error = "Unable to load table backend: " + e.getMessage();
            //            getLogger().debug(error, e);
            //            loadResult.setDirtyAfterLoad();
            //            loadResult.setResetRequiredAfterLoad();
            //            loadResult.addError(error, true);
            // TODO error handling special / move to persistor?
            //            if (e instanceof TableBackendUnknownException) { // NOSONAR
            //                loadResult.addMissingTableFormat(((TableBackendUnknownException)e).getFormatInfo());
            //            }
        }
        // TODO or empty settings?
        return null;
    }

    /**
     * @param dotKNIME
     * @param settings
     * @return load version, see {@link LoadVersion}
     * @throws IOException
     */
    private static String loadWorkflowFormatVersionString(final File dotKNIME, final ConfigBase settings)
        throws IOException {
        // CeBIT 2006 version did not contain a version string.
        String versionString;
        if (settings.containsKey(ConfigKey.WORKFLOW_FORMAT_VERSION.m_key)) {
            try {
                versionString = settings.getString(ConfigKey.WORKFLOW_FORMAT_VERSION.m_key);
            } catch (InvalidSettingsException e) {
                throw new IOException("Can't read version number from \"" + dotKNIME.getAbsolutePath() + "\"", e);
            }
        } else {
            versionString = "0.9.0";
        }
        return versionString;
    }

    /**
     * TODO move to new persistor - this is beyond data loading logic Checks if the workflow can be loaded with the
     * running version of KNIME.
     *
     * @param version the minimum version required to load the workflow
     * @param createdWith the version with which the workflow was created
     * @param isNightly whether created by a nightly version of KNIME
     * @param loadPolicy
     * @return true if everything is ok, false if the workflow can be loaded but must be considered changed after load
     * @throws UnsupportedWorkflowVersionException if the workflow cannot be loaded
     */
    private static boolean isVersionCompatible(final LoadVersion version, final Version createdWith,
        final boolean isNightly, final UnknownKNIMEVersionLoadPolicy loadPolicy)
        throws UnsupportedWorkflowVersionException {

        boolean isRunningNightly = KNIMEConstants.isNightlyBuild();
        boolean isFutureWorkflow =
            createdWith != null && !new Version(KNIMEConstants.VERSION).isSameOrNewer(createdWith);

        if (version == LoadVersion.FUTURE || isFutureWorkflow || (!isRunningNightly && isNightly)) {
            if (loadPolicy == UnknownKNIMEVersionLoadPolicy.Abort) {
                throw new UnsupportedWorkflowVersionException(
                    unableToLoadErrorMessage(version, createdWith, isFutureWorkflow));
            } else {
                return false;
            }
        } else if (version.isOlderThan(LoadVersion.V200)) {
            // TODO
            //            LOGGER.warn("The current KNIME version (" + KNIMEConstants.VERSION + ") is different from the one that "
            //                    + "created the workflow (" + version + ") you are trying to load. In some rare cases, it  "
            //                    + "might not be possible to load all data or some nodes can't be configured. "
            //                    + "Please re-configure and/or re-execute these nodes.");
        }
        return true;
    }

    /**
     * TODO move to new persistor - this is beyond data loading logic.
     *
     * Generates an error message in case the workflow version cannot be loaded with the currently running version.
     *
     * @param version of the workflow that we want to load
     * @param createdWith version with which the workflow was created
     * @param isFutureWorkflow whether the workflow was created by a future version of KNIME
     * @return
     */
    private static String unableToLoadErrorMessage(final LoadVersion version, final Version createdWith,
        final boolean isFutureWorkflow) {
        var e = new StringBuilder("Unable to load ");
        e.append("workflow, ");
        if (version == LoadVersion.FUTURE || isFutureWorkflow) {
            e.append("it was created with a future version of KNIME (").append(createdWith).append("). ");
            e.append("You are running ").append(KNIMEConstants.VERSION).append(".");
        } else {
            e.append("it was created with a nightly build of KNIME (version ").append(createdWith).append("). ");
            e.append("You are running ").append(KNIMEConstants.VERSION).append(".");
        }
        return e.toString();
    }

}
