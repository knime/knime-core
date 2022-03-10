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
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.knime.core.internal.ReferencedFile;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.config.base.ConfigBaseRO;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.workflow.FileSubNodeContainerPersistor;
import org.knime.core.node.workflow.FileWorkflowPersistor;
import org.knime.core.node.workflow.MetaNodeTemplateInformation;
import org.knime.core.node.workflow.MetaNodeTemplateInformation.Role;
import org.knime.core.node.workflow.TemplateNodeContainerPersistor;
import org.knime.core.node.workflow.UnsupportedWorkflowVersionException;
import org.knime.core.node.workflow.WorkflowDataRepository;
import org.knime.core.node.workflow.WorkflowLoadHelper;
import org.knime.core.node.workflow.WorkflowLoadHelper.UnknownKNIMEVersionLoadPolicy;
import org.knime.core.node.workflow.WorkflowPersistor;
import org.knime.core.node.workflow.def.CoreToDefUtil;
import org.knime.core.util.LoadVersion;
import org.knime.core.util.Version;
import org.knime.core.workflow.def.ConfigMapDef;
import org.knime.core.workflow.def.CredentialPlaceholderDef;
import org.knime.core.workflow.def.FlowVariableDef;
import org.knime.core.workflow.def.RootWorkflowDef;
import org.knime.core.workflow.def.WorkflowDef;
import org.knime.core.workflow.def.impl.CredentialPlaceholderDefBuilder;
import org.knime.core.workflow.def.impl.FlowVariableDefBuilder;
import org.knime.core.workflow.def.impl.RootWorkflowDefBuilder;
import org.knime.core.workflow.def.impl.WorkflowDefBuilder;

/**
 * Loads a standalone component (a.k.a. template), i.e., a top-level component that is not part of a workflow but can
 * for instance be dragged from the KNIME explorer into a workflow to be inserted into a workflow.
 *
 * @author Carl Witt, KNIME AG, Zurich, Switzerland
 */
public class RootComponentLoader {

    /** String constants, such as key names in the workflow configuration, file names, etc. */
    public enum Const {
        NONE_YET("");

        final String m_const;

        Const(final String key) {
            m_const = key;
        }

        /** @return the string constant. */
        public String get() {
            return m_const;
        }
    }


    /** Create persistor for a workflow or template.
     * @param directory The directory to load from
     * @param templateSourceURI URI of the link to the template (will load a template and when the template is
     * instantiated and put into the workflow a link is created)
     * @throws UnsupportedWorkflowVersionException If the workflow is of an unsupported version
     */
    public final TemplateNodeContainerPersistor createTemplateLoadPersistor(
        final File directory, final URI templateSourceURI)
            throws IOException, UnsupportedWorkflowVersionException {
        if (directory == null) {
            throw new NullPointerException("Arguments must not be null.");
        }
        String fileName = getDotKNIMEFileName();
        if (!directory.isDirectory() || !directory.canRead()) {
            throw new IOException("Can't read metanode/template directory " + directory);
        }

        // template.knime or workflow.knime
        ReferencedFile dotKNIMERef = new ReferencedFile(new ReferencedFile(directory), fileName);
        File dotKNIME = dotKNIMERef.getFile();
        if (!dotKNIME.isFile()) {
            throw new IOException("No \"" + fileName + "\" file in directory \"" + directory.getAbsolutePath() + "\"");
        }

        NodeSettingsRO settings = NodeSettings.loadFromXML(new BufferedInputStream(new FileInputStream(dotKNIME)));

        LoadVersion version = FileWorkflowPersistor.parseVersion(versionString); // might also be FUTURE
        boolean isSetDirtyAfterLoad = false;
        StringBuilder versionDetails = new StringBuilder(versionString);
        String createdBy = settings.getString(WorkflowLoadHelper.CFG_CREATED_BY, null);
        Version createdByVersion = null;
        if (createdBy != null) {
            versionDetails.append(" (created by KNIME ").append(createdBy).append(")");
            try {
                createdByVersion = new Version(createdBy);
            } catch (IllegalArgumentException e) {
                // ideally this goes into the 'LoadResult' but it's not instantiated yet
                LOGGER.warn(String.format("Unable to parse version string \"%s\" (file \"%s\"): %s", createdBy,
                    dotKNIME.getAbsolutePath(), e.getMessage()), e);
            }
        }
        boolean isNightly = settings.getBoolean(CFG_NIGHTLY, false); // added 3.5.0
        boolean isRunningNightly = KNIMEConstants.isNightlyBuild();
        boolean isFutureWorkflow = createdByVersion != null &&
                !new Version(KNIMEConstants.VERSION).isSameOrNewer(createdByVersion);
        if (version == LoadVersion.FUTURE || isFutureWorkflow || (!isRunningNightly && isNightly)) {
            switch (getUnknownKNIMEVersionLoadPolicy(version, createdByVersion, isNightly)) {
            case Abort:
                StringBuilder e = new StringBuilder("Unable to load ");
                e.append(isTemplateFlow() ? "template, " : "workflow, ");
                if (version == LoadVersion.FUTURE || isFutureWorkflow) {
                    e.append("it was created with a future version of KNIME (").append(createdBy).append("). ");
                    e.append("You are running ").append(KNIMEConstants.VERSION).append(".");
                } else {
                    e.append("it was created with a nightly build of KNIME (version ").append(createdBy).append("). ");
                    e.append("You are running ").append(KNIMEConstants.VERSION).append(".");
                }
                throw new UnsupportedWorkflowVersionException(e.toString());
            default:
                isSetDirtyAfterLoad = true;
            }
        } else if (version.isOlderThan(LoadVersion.V200)) {
            LOGGER.warn("The current KNIME version (" + KNIMEConstants.VERSION + ") is different from the one that "
                    + "created the workflow (" + version + ") you are trying to load. In some rare cases, it  "
                    + "might not be possible to load all data or some nodes can't be configured. "
                    + "Please re-configure and/or re-execute these nodes.");
        }

        final MetaNodeTemplateInformation templateInfo;
        if (isTemplateFlow() && templateSourceURI != null) {
            try {
                templateInfo = MetaNodeTemplateInformation.load(settings, version, isTemplateProject());
                CheckUtils.checkSetting(Role.Template.equals(templateInfo.getRole()),
                    "Role is not '%s' but '%s'", Role.Template, templateInfo.getRole());
            } catch (InvalidSettingsException e) {
                throw new IOException(String.format(
                    "Attempting to load template from \"%s\" but can't locate template information: %s",
                    dotKNIME.getAbsolutePath(), e.getMessage()), e);
            }
        } else if (isTemplateFlow()) {
//            LOGGER.coding("Supposed to instantiate a template but the link URI is not set");
            // metanode template from node repository
            templateInfo = null;
        } else {
            templateInfo = null;
        }

        final TemplateNodeContainerPersistor persistor;
        // TODO only create new repo if workflow is a project?
        WorkflowDataRepository workflowDataRepository = new WorkflowDataRepository();
        // ordinary workflow is loaded
        if (templateInfo == null) {
            persistor = new FileWorkflowPersistor(workflowDataRepository, dotKNIMERef, this,
                version, !isTemplateFlow());
        } else {
            // some template is loaded
            switch (templateInfo.getNodeContainerTemplateType()) {
                case MetaNode:
                    final ReferencedFile workflowDotKNIME;
                    if (version.isOlderThan(LoadVersion.V2100)) {
                        workflowDotKNIME = dotKNIMERef; // before 2.10 everything was stored in template.knime
                    } else {
                        workflowDotKNIME = new ReferencedFile(dotKNIMERef.getParent(), WorkflowPersistor.WORKFLOW_FILE);
                    }
                    persistor = new FileWorkflowPersistor(workflowDataRepository, workflowDotKNIME, this,
                        version, !isTemplateFlow());
                    break;
                case SubNode:
                    final ReferencedFile settingsDotXML = new ReferencedFile(dotKNIMERef.getParent(),
                        SingleNodeContainerPersistor.SETTINGS_FILE_NAME);
                    persistor = new FileSubNodeContainerPersistor(settingsDotXML, this, version,
                        workflowDataRepository, true);
                    break;
                default:
                    throw new IllegalStateException("Unsupported template type");
            }
        }
        if (templateInfo != null) {
            persistor.setOverwriteTemplateInformation(templateInfo.createLink(templateSourceURI, isTemplateProject()));

            if (templateSourceURI != null) {
                final String path = templateSourceURI.getPath();
                persistor.setNameOverwrite(path.substring(path.lastIndexOf('/') + 1));
            } else {
                persistor.setNameOverwrite(directory.getName());
            }
        }
        if (isSetDirtyAfterLoad) {
            persistor.setDirtyAfterLoad();
        }
        return persistor;
    }

    /**
     * Loads the workflow global information (load version, etc.) and the actual workflow.
     *
     * @param directory The directory that contains the workflow to load
     * @param handleUnknownVersion what to do if the version is unknown // TODO is this really used properly
     * @return a description of the workflow as POJOs
     * @throws IOException when the workflow settings cannot be parsed from the given directory, or the workflow format
     *             version cannot be extracted from the parsed workflow settings
     */
    public static RootWorkflowDef load(final File directory, final UnknownKNIMEVersionLoadPolicy handleUnknownVersion)
        throws IOException {

        //TODO use handleUnknownVersion

        var workflowConfig = WorkflowLoader.parseWorkflowConfig(directory);
        var workflowFormatVersion = LoadVersion.fromVersionString(loadWorkflowFormatVersionString(workflowConfig));

        return new RootWorkflowDefBuilder()//
            .setCreatorIsNightly(() -> loadCreatorIsNightly(workflowConfig), true)//
            .setCredentialPlaceholders(() -> loadCredentialPlaceholderDefs(workflowConfig, workflowFormatVersion),
                List.of())//
            .setFlowVariables(() -> loadWorkflowVariableDefs(workflowConfig, workflowFormatVersion), List.of())//
            .setSavedWithData(() -> loadSavedWithData(directory, workflowFormatVersion), true)//
            .setSavedWithVersion(() -> loadCreatorVersion(workflowConfig).toString(), LoadVersion.UNKNOWN.toString())//
            .setTableBackendSettings(() -> loadTableBackendSettingsDef(workflowConfig), null)//
            .setWorkflow(() -> WorkflowLoader.load(directory, workflowConfig, workflowFormatVersion), defaultWorkflow())//
            .setWorkflowFormatVersion(workflowFormatVersion.toString())//
            .build();

        //        var isSetDirtyAfterLoad =
        //            isVersionCompatible(workflowFormatVersion, creatorVersion, creatorIsNightly, handleUnknownVersion);
        //        if (isSetDirtyAfterLoad) {
        //            // TODO error handling
        //            //            persistor.setDirtyAfterLoad();
        //        }

        // TODO how to nest the load result?
        // or use a flat, referential form? e.g., [{problem: can't load string, wf: wf_ref}, {problem: can't load.., wf: wf2}]

    }

    private static WorkflowDef defaultWorkflow() {
        return new WorkflowDefBuilder().build();
    }

    /**
     * @param directory The directory that contains the workflow to load
     * @return a description of the workflow as POJOs
     */
    public static RootWorkflowDef load(final File directory)
        throws IOException, UnsupportedWorkflowVersionException, InvalidSettingsException {
        return load(directory, UnknownKNIMEVersionLoadPolicy.Abort);
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
    public static UnknownKNIMEVersionLoadPolicy getUnknownKNIMEVersionLoadPolicy(final LoadVersion workflowKNIMEVersion,
        final Version createdByKNIMEVersion, final boolean isNightlyBuild) {
        return UnknownKNIMEVersionLoadPolicy.Abort;
    }

    /**
     * @param workflowConfig the parsed contents of the workflow.knime XML file
     * @return the version of the KNIME instance that was used to create the workflow.
     */
    private static Version loadCreatorVersion(final ConfigBaseRO workflowConfig) {
        Version createdWith = null;
        var createdWithString = workflowConfig.getString(Const.CREATOR_KNIME_VERSION.get(), null);
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
     * @param workflowConfig the parsed contents of the workflow.knime XML file
     * @return the version of the KNIME instance that was used to create the workflow.
     * @throws InvalidSettingsException
     */
    private static boolean loadCreatorIsNightly(final ConfigBaseRO workflowConfig) throws InvalidSettingsException {
        if (!workflowConfig.containsKey(Const.CREATOR_IS_NIGHTLY.get())) {
            throw new InvalidSettingsException(
                "Workflow settings do not specify whether the workflow was created by a nightly version of KNIME ("
                    + Const.CREATOR_IS_NIGHTLY.get() + ")");
        }
        return workflowConfig.getBoolean(Const.CREATOR_IS_NIGHTLY.get());
    }

    /**
     * @param workflowConfig the parsed contents of the workflow.knime XML file
     * @return load version, see {@link LoadVersion}
     */
    private static String loadWorkflowFormatVersionString(final ConfigBaseRO workflowConfig) throws IOException {
        // CeBIT 2006 version did not contain a version string.
        String versionString;
        if (workflowConfig.containsKey(Const.WORKFLOW_FORMAT_VERSION.get())) {
            try {
                versionString = workflowConfig.getString(Const.WORKFLOW_FORMAT_VERSION.get());
            } catch (InvalidSettingsException e) {
                throw new IOException("Can't read version number from \"" + workflowConfig.getKey() + "\"", e);
            }
        } else {
            versionString = Const.DEFAULT_LOAD_VERSION_STRING.get();
        }
        return versionString;
    }

    /**
     * TODO move to new persistor - this is beyond data loading logic Checks if the workflow can be loaded with the
     * running version of KNIME.
     *
     * @param workflowFormatVersion the version of the workflow format that was used to write the workflow to load
     * @param createdWith the version with which the workflow was created
     * @param isNightly whether created by a nightly version of KNIME
     * @param loadPolicy
     * @return true if everything is ok, false if the workflow can be loaded but must be considered changed after load
     * @throws UnsupportedWorkflowVersionException if the workflow cannot be loaded
     */
    private static boolean isVersionCompatible(final LoadVersion workflowFormatVersion, final Version createdWith,
        final boolean isNightly, final UnknownKNIMEVersionLoadPolicy loadPolicy)
        throws UnsupportedWorkflowVersionException {

        // TODO get rid of KNIMEConstants - or move to knime shared
        var isFutureWorkflow = createdWith != null && !new Version(KNIMEConstants.VERSION).isSameOrNewer(createdWith);

        // TODO is running nightly doesn't make sense for general loader code -> move to core invocation
        // if (version == LoadVersion.FUTURE || isFutureWorkflow || (!isRunningNightly && isNightly)) {
        if (workflowFormatVersion == LoadVersion.FUTURE || isFutureWorkflow) {
            if (loadPolicy == UnknownKNIMEVersionLoadPolicy.Abort) {
                throw new UnsupportedWorkflowVersionException(
                    unableToLoadErrorMessage(workflowFormatVersion, createdWith, isFutureWorkflow));
            } else {
                return false;
            }
        } else if (workflowFormatVersion.isOlderThan(LoadVersion.V200)) {
            // TODO
            //            LOGGER.warn("The current KNIME version (" + KNIMEConstants.VERSION + ") is different from the one that "
            //                    + "created the workflow (" + version + ") you are trying to load. In some rare cases, it  "
            //                    + "might not be possible to load all data or some nodes can't be configured. "
            //                    + "Please re-configure and/or re-execute these nodes.");
        }
        return true;
    }

    /**
     * Determine if the workflow was saved with workflow execution results or only its description (e.g., using the
     * "export without data" option from the GUI).
     *
     * @param workflowDir contains the workflow
     * @param workflowFormatVersion the version of the workflow format that was used to write the workflow to load
     * @return true for old workflows (&lt;2.0) or if there is a {@value WorkflowPersistor#SAVED_WITH_DATA_FILE} file.
     */
    private static boolean loadSavedWithData(final File workflowDir, final LoadVersion workflowFormatVersion) {
        if (workflowFormatVersion.isOlderThan(LoadVersion.V200)) {
            return true;
        }
        return new File(workflowDir, Const.SAVED_WITH_DATA_FILE_NAME.get()).isFile();
    }

    /**
     * @param workflowFormatVersion the version of the workflow format that was used to write the workflow to load
     * @param workflowConfig parsed contents of workflow.knime
     */
    private static List<CredentialPlaceholderDef> loadCredentialPlaceholderDefs(final ConfigBaseRO workflowConfig,
        final LoadVersion workflowFormatVersion) {
        try {
            if (workflowFormatVersion.isOlderThan(LoadVersion.V220)) {
                // no credentials in v2.1 and before
                return List.of();
            }

            ConfigBaseRO sub = workflowConfig.getConfigBase(Const.CREDENTIAL_PLACEHOLDERS.get());
            List<CredentialPlaceholderDef> placeholders = new ArrayList<>();
            Set<String> unique = new HashSet<>();
            for (String key : sub.keySet()) {
                ConfigBaseRO placeholder = sub.getConfigBase(key);
                var p = loadCredentialPlaceholderDef(placeholder);
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

    /**
     * @param workflowConfig parsed contents of workflow.knime
     */
    private static CredentialPlaceholderDef loadCredentialPlaceholderDef(final ConfigBaseRO workflowConfig)
        throws InvalidSettingsException {
        return new CredentialPlaceholderDefBuilder()//
            .setName(workflowConfig.getString(Const.CREDENTIAL_PLACEHOLDER_NAME.get()))//
            .setLogin(workflowConfig.getString(Const.CREDENTIAL_PLACEHOLDER_LOGIN.get()))//
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
     * @param workflowConfig the parsed contents of the workflow.knime XML file
     */
    private static ConfigMapDef loadTableBackendSettingsDef(final ConfigBaseRO workflowConfig)
        throws InvalidSettingsException {
        if (workflowConfig.containsKey(Const.TABLE_BACKEND.get())) {
            return CoreToDefUtil.toConfigMapDef(workflowConfig.getConfigBase(Const.TABLE_BACKEND.get()));
        }
        return null;
    }

    /**
     * Load workflow variables (not available in 1.3.x flows).
     *
     * @param workflowConfig
     * @param workflowFormatVersion the version of the workflow format that was used to write the workflow to load
     * @return The variables in a list.
     * @throws InvalidSettingsException If any settings-related error occurs.
     */
    private static List<FlowVariableDef> loadWorkflowVariableDefs(final ConfigBaseRO workflowConfig,
        final LoadVersion workflowFormatVersion) throws InvalidSettingsException {
        if (workflowFormatVersion.isOlderThan(LoadVersion.V200)
            || !workflowConfig.containsKey(Const.CFG_WKF_VARIABLES.get())) {
            return List.of();
        }

        var wfmVarSub = workflowConfig.getConfigBase(Const.CFG_WKF_VARIABLES.get());
        List<FlowVariableDef> result = new ArrayList<>();
        for (String key : wfmVarSub.keySet()) {
            ConfigBaseRO sub = wfmVarSub.getConfigBase(key);
            var def = new FlowVariableDefBuilder()//
                // TODO
                //            final String name = CheckUtils.checkSettingNotNull(sub.getString("name"), "name must not be null");
                .setName(sub.getString("name"))//
                // TODO
                //            final VariableValue<?> value = VariableType.load(sub);
                .setValue(null)
                // TODO
                .setPropertyClass(null)//
                .build();
            result.add(def);
        }
        return result;
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
