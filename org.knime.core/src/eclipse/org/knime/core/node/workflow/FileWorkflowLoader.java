/*
 * ------------------------------------------------------------------------
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
 * ------------------------------------------------------------------------
 */
package org.knime.core.node.workflow;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.Stack;
import java.util.TreeMap;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.NotImplementedException;
import org.knime.core.data.TableBackend;
import org.knime.core.internal.ReferencedFile;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.NodeAndBundleInformationPersistor;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortType;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.workflow.FromFileNodeContainerPersistor.PersistorWithPortIndex;
import org.knime.core.node.workflow.MetaNodeTemplateInformation.Role;
import org.knime.core.node.workflow.WorkflowPersistor.LoadResult;
import org.knime.core.node.workflow.WorkflowPersistor.NodeFactoryUnknownException;
import org.knime.core.node.workflow.WorkflowPersistor.WorkflowPortTemplate;
import org.knime.core.node.workflow.WorkflowTableBackendSettings.TableBackendUnknownException;
import org.knime.core.node.workflow.def.CoreToDefUtil;
import org.knime.core.node.workflow.execresult.WorkflowExecutionResult;
import org.knime.core.node.workflow.loader.NodeContainerLoader;
import org.knime.core.util.FileUtil;
import org.knime.core.util.LoadVersion;
import org.knime.core.util.LockFailedException;
import org.knime.core.util.workflowalizer.AuthorInformation;
import org.knime.core.workflow.def.ConnectionDef;
import org.knime.core.workflow.def.NodeDef;
import org.knime.core.workflow.def.impl.DefaultConnectionDef;
import org.knime.core.workflow.def.impl.DefaultMetaNodeDataDef;
import org.knime.core.workflow.def.impl.DefaultMetaNodeDataDef.DefaultMetaNodeDataDefBuilder;
import org.knime.core.workflow.def.impl.DefaultWorkflowDef;
import org.knime.core.workflow.def.impl.DefaultWorkflowDef.DefaultWorkflowDefBuilder;
import org.knime.core.workflow.def.impl.DefaultWorkflowMetadataDef;
import org.knime.core.workflow.def.impl.DefaultWorkflowMetadataDef.DefaultWorkflowMetadataDefBuilder;
import org.knime.core.workflow.def.impl.DefaultWorkflowProjectDef;
import org.knime.core.workflow.def.impl.DefaultWorkflowProjectDef.DefaultWorkflowProjectDefBuilder;

/**
 * Recursively walks through the legacy directory structure, generating the workflow defs.
 *
 * TODO remove this comment: Used to implement {@link WorkflowPersistor} and {@link TemplateNodeContainerPersistor}
 *
 * TODO remove suppresswarnings('javadoc')
 *
 * @author Carl Witt, KNIME AG, Zurich, Switzerland
 */
@SuppressWarnings("javadoc")
public class FileWorkflowLoader implements NodeContainerLoader {

    /** Key for nodes. */
    public static final String KEY_NODES = "nodes";

    /** Key for connections. */
    public static final String KEY_CONNECTIONS = "connections";

    public static final String KEY_UI_INFORMATION = "extraInfoClassName";

    /** Key for this node's internal ID. */
    static final String KEY_ID = "id";

    /** Identifier for KNIME workflows when saved to disc. */
    public static final String WORKFLOW_FILE = "workflow.knime";

    /** Identifier for KNIME meta mode templates when saved to disc. */
    public static final String TEMPLATE_FILE = "template.knime";

    /**
     * Identifier for KNIME workflows SVG export when saved to disc.
     *
     * @since 2.8
     */
    public static final String SVG_WORKFLOW_FILE = "workflow.svg";

    /**
     * Identifier for KNIME templates SVG export when saved to disc.
     *
     * @since 2.8
     */
    public static final String SVG_TEMPLATE_FILE = "template.svg";

    /**
     * File used to signal that workflow was saved in usual manner. It will always be present in the workflow directory
     * unless the workflow is exported with the "exclude data" flag being set.
     */
    public static final String SAVED_WITH_DATA_FILE = ".savedWithData";

    /** Constant for the meta info file name. */
    public static final String METAINFO_FILE = "workflowset.meta";

    /** KNIME Node type: native, meta or sub node. */
    private enum NodeType {
            NativeNode("node"), MetaNode("metanode"), SubNode("wrapped node");

        private final String m_shortName;

        /** @param shortName -- toString result */
        private NodeType(final String shortName) {
            m_shortName = shortName;
        }

        /** {@inheritDoc} */
        @Override
        public String toString() {
            return m_shortName;
        }
    }

    static final LoadVersion VERSION_LATEST = LoadVersion.V4010;

    /** Format used to save author/edit infos. */
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss Z");

    private static final String CFG_UIINFO_SUB_CONFIG = "ui_settings";

    /** Key for UI info's class name. */
    private static final String CFG_UIINFO_CLASS = "ui_classname";

    /** Key for workflow variables. */
    private static final String CFG_WKF_VARIABLES = "workflow_variables";

    /** key used to store the editor specific settings (since 2.6). */
    private static final String CFG_EDITOR_INFO = "workflow_editor_settings";

    /** Key for credentials. */
    private static final String CFG_CREDENTIALS = "workflow_credentials";

    private static final String CFG_AUTHOR_INFORMATION = "authorInformation";

    private static final String CFG_EDITOR_SNAP_GRID = "workflow.editor.snapToGrid";

    private static final String CFG_EDITOR_SHOW_GRID = "workflow.editor.ShowGrid";

    private static final String CFG_EDITOR_X_GRID = "workflow.editor.gridX";

    private static final String CFG_EDITOR_Y_GRID = "workflow.editor.gridY";

    private static final String CFG_EDITOR_ZOOM = "workflow.editor.zoomLevel";

    private static final String CFG_EDITOR_CURVED_CONNECTIONS = "workflow.editor.curvedConnections";

    private static final String CFG_EDITOR_CONNECTION_WIDTH = "workflow.editor.connectionWidth";

    /**
     * The key under which the bounds to store the {@link ConnectionUIInformation} are registered. *
     *
     * @since 3.5
     */
    public static final String KEY_BENDPOINTS = "extrainfo.conn.bendpoints";

    /** The key under which the bounds are registered. * */
    private static final String KEY_BOUNDS = "extrainfo.node.bounds";

    private static final PortType FALLBACK_PORTTYPE = PortObject.TYPE;

    private static final NodeSettingsRO EMPTY_SETTINGS = new NodeSettings("<<empty>>");

    /** The node logger for this class. */
    private final NodeLogger m_logger = NodeLogger.getLogger(getClass());

    private final LoadVersion m_versionString;

    private final TreeMap<Integer, FromFileNodeContainerPersistor> m_nodeContainerLoaderMap;

    /**
     * used to check for duplicate connection information in
     * {@link #loadNodeContainer(Map, ExecutionMonitor, org.knime.core.node.workflow.WorkflowPersistor.LoadResult)}
     * {@link #getConnectionSet()}
     * TODO move to builder.addConnection
     */
    private final HashSet<ConnectionDef> m_connectionSet;

    private final FileNodeContainerMetaPersistor m_metaPersistor;

    private final WorkflowDataRepository m_workflowDataRepository;

    private WorkflowPortTemplate[] m_inPortTemplates;

    private WorkflowPortTemplate[] m_outPortTemplates;
    /// TODO this screams for inheritance?
    private final boolean m_isProject;

    /// TODO this screams for inheritance?
    private final boolean m_isComponentProject;

    /**
     * Parent persistor, used to create (nested) decryption stream for locked metanodes.
     */
    private WorkflowPersistor m_parentPersistor;
    private boolean m_mustWarnOnDataLoadError;

    private MetaNodeTemplateInformation m_templateInformation;
    private final List<ReferencedFile> m_obsoleteNodeDirectories;

    /** see {@link #setNameOverwrite(String)}. */
    private String m_nameOverwrite;

    // -- Builders --

    final DefaultMetaNodeDataDefBuilder m_metaNodeDefBuilder = DefaultMetaNodeDataDef.builder();

    final DefaultWorkflowProjectDefBuilder m_workflowProjectDefBuilder = DefaultWorkflowProjectDef.builder();

    /**
     * Holds the nodes (TODO move to builder) until build is called. Key is the node ID (suffix?)
     */
    Map<Integer, NodeDef> m_nodesToAdd = new HashMap<>();

    final DefaultWorkflowDefBuilder m_workflowDefBuilder = DefaultWorkflowDef.builder();

    final DefaultWorkflowMetadataDefBuilder m_workflowMetadataDefBuilder = DefaultWorkflowMetadataDef.builder();

    private NodeSettingsRO m_workflowSett;

    /** Parse the version string, return {@link LoadVersion#FUTURE} if it can't be parsed. */
    static LoadVersion parseVersion(final String versionString) {
        var isBeforeV2 = versionString.equals("0.9.0");
        isBeforeV2 |= versionString.equals("1.0");
        isBeforeV2 |= versionString.matches("1\\.[01234]\\.[0-9].*");
        if (isBeforeV2) {
            return LoadVersion.UNKNOWN;
        }
        return LoadVersion.get(versionString).orElse(LoadVersion.FUTURE);
    }

    /**
     * @param dotKNIMEFile Associated workflow.knime or template.knime file
     * @param loadHelper The load helper as required by meta persistor.
     * @param version of loading workflow.
     */
    FileWorkflowLoader(final WorkflowDataRepository workflowDataRepository, final ReferencedFile dotKNIMEFile,
        final WorkflowLoadHelper loadHelper, final LoadVersion version, final boolean isProject) {
        assert version != null;
        m_workflowDataRepository = workflowDataRepository;
        m_versionString = version;
        m_metaPersistor = new FileNodeContainerMetaPersistor(dotKNIMEFile, loadHelper, version);
        m_nodeContainerLoaderMap = new TreeMap<>();
        m_connectionSet = new HashSet<>();
        m_obsoleteNodeDirectories = new ArrayList<>();
        m_isProject = isProject;
        m_isComponentProject = loadHelper.isTemplateProject();
    }

    /**
     * Set only once in constructor. Originally from both {@link WorkflowPersistor} and
     * {@link TemplateNodeContainerPersistor}
     */
    public final LoadVersion getLoadVersion() {
        return m_versionString;
    }

    NodeLogger getLogger() {
        return m_logger;
    }

    /**
     * @return The load helper as set on the meta persistor. Will be passed on to loaders of contained nodes.
     */
    WorkflowLoadHelper getLoadHelper() {
        return getMetaPersistor().getLoadHelper();
    }

    /**
     * @return the workflowDir
     */
    ReferencedFile getWorkflowKNIMEFile() {
        var meta = getMetaPersistor();
        if (meta == null) {
            throw new RuntimeException("Persistor not created for loading workflow, meta persistor is null");
        }
        return meta.getNodeSettingsFile();
    }

    /** Originally from {@link TemplateNodeContainerPersistor} */
    public boolean mustWarnOnDataLoadError() {
        return m_mustWarnOnDataLoadError;
    }

    /** Originally from {@link TemplateNodeContainerPersistor} */
    public FileNodeContainerMetaPersistor getMetaPersistor() {
        return m_metaPersistor;
    }

    /**
     * Originally from {@link WorkflowPersistor}
     *
     * @since 2.6
     */
    public WorkflowDataRepository getWorkflowDataRepository() {
        return m_workflowDataRepository;
    }

    /**
     * Originally from {@link WorkflowPersistor}
     *
     * @since 2.8
     */
    public WorkflowContext getWorkflowContext() {
        return isProject() || m_isComponentProject ? getMetaPersistor().getLoadHelper().getWorkflowContext() : null;
    }


    /**
     * @since 2.6
     */
    /** Originally from {@link TemplateNodeContainerPersistor} */
    public boolean isProject() {
        return m_isProject;
    }

    /** Originally from {@link TemplateNodeContainerPersistor} */
    public boolean mustComplainIfStateDoesNotMatch() {
        return !getLoadVersion().isOlderThan(LoadVersion.V200);
    }

    @FunctionalInterface
    private interface LoaderCode<T> {
        T load() throws InvalidSettingsException;
    }

    /**
     * Generates error message "Unable to load <attributeName>: <invalid settings exception message>"
     *
     * TODO maybe pass the logger to the load result and log it from there?
     *
     * Outputs the message on debug level and adds it to the load result.
     *
     * Sets the loadResult to dirty.
     *
     * @param <T>
     * @param attributeName
     * @param fallback
     * @param r
     * @param loadResult
     * @param logTo
     * @return Fallback if the loader code throws an {@link InvalidSettingsException}. Otherwise the loaded value.
     */
    private static <T> T tryLoadWithDefaultGeneric(final Function<Throwable, String> errorMessageGen, final T fallback, final LoaderCode<T> r,
        final LoadResult loadResult, final BiConsumer<Object, Throwable> logTo) {
        try {
            return r.load();
        } catch (InvalidSettingsException e) {
            var error = errorMessageGen.apply(e);
            logTo.accept(error, e);
            loadResult.setDirtyAfterLoad();
            loadResult.addError(error);
        }
        return fallback;
    }

    // TODO I didn't pay attention during refactoring and always used tryLoadDebug, but some should emit warnings or errors
    // seems as if loadResult is always using setError (except for two cases) even if the log level is debug - probably
    // not consistent but should stay like this for now
    private <T> T tryLoadDebug(final Function<Throwable, String> errorMessageGen, final T fallback, final LoaderCode<T> r,
        final LoadResult loadResult) {
        return tryLoadWithDefaultGeneric(errorMessageGen, fallback, r, loadResult, getLogger()::debug);
    }

    private <T> T tryLoadDebug(final String attributeName, final T fallback, final LoaderCode<T> r,
        final LoadResult loadResult) {
        return tryLoadWithDefaultGeneric(e -> "Unable to load " + attributeName + ": " + e.getMessage(), fallback, r, loadResult, getLogger()::debug);
    }


    /**
     * @param workflowCipher the workflowCipher to set
     */
    void setWorkflowCipher(final WorkflowCipher workflowCipher) {
        // TODO move to other setters
//        m_workflowProjectDefBuilder.set
        // TODO schema supports only a string, not an object
    }

    /**
     * TODO this covers way too much (workflows and metanodes (and components?)) sharing is ok, but the metanode
     * specific part needs to go to somewhere else
     *
     * reduced from 400 to around 170 lines.
     *
     */
    /** Originally from {@link TemplateNodeContainerPersistor} */
    public void preLoadNodeContainer(final WorkflowPersistor parentPersistor, final NodeSettingsRO parentSettings,
        final LoadResult loadResult) throws InvalidSettingsException, IOException {

        m_parentPersistor = parentPersistor;

        final var knimeFile = getWorkflowKNIMEFile();

        if (knimeFile == null || !knimeFile.getFile().isFile()) {
            loadResult.setDirtyAfterLoad();
            var error = "Can't read workflow file \"" + knimeFile + "\"";
            throw new IOException(error);
        }

        // workflow.knime (or template.knime)
        var nodeFile = knimeFile.getFile();
        var parentRef = knimeFile.getParent();

        if (parentRef == null) {
            loadResult.setDirtyAfterLoad();
            throw new IOException("Parent directory of file \"" + knimeFile + "\" is not represented by "
                + ReferencedFile.class.getSimpleName() + " object");
        }

        /**
         * Data Provider
         */
        m_mustWarnOnDataLoadError = loadIfMustWarnOnDataLoadError(parentRef.getFile());

        NodeSettingsRO subWFSettings;
        try {
            InputStream in = new FileInputStream(nodeFile);
            /**
             * Security TODO should be different persistors for component, metanode, workflow
             */
            if (m_parentPersistor != null) { // real metanode, not a project
                // the workflow.knime (or template.knime) file is not encrypted
                // with this metanode's cipher but possibly with a parent
                // cipher
                in = m_parentPersistor.decipherInput(in);
            }
            in = new BufferedInputStream(in);
            subWFSettings = NodeSettings.loadFromXML(in);
        } catch (IOException ioe) {
            loadResult.setDirtyAfterLoad();
            throw ioe;
        }
        m_workflowSett = subWFSettings;

        setName(tryLoadDebug("workflow name", null, () -> {
            String name;
            if (m_nameOverwrite != null) {
                name = m_nameOverwrite;
            } else {
                name = loadWorkflowName(m_workflowSett);
            }
            return name;
        }, loadResult));

        setWorkflowCipher(tryLoadDebug("workflow cipher", WorkflowCipher.NULL_CIPHER,
            () -> loadWorkflowCipher(getLoadVersion(), m_workflowSett), loadResult));

        setTemplateInformation(tryLoadDebug("template information", MetaNodeTemplateInformation.NONE, () -> {
            if (m_templateInformation != null) {
                // template information was set after construction (this node is a link created from a template)
                assert m_templateInformation.getRole() == Role.Link;
            } else {
                var templateInformation = MetaNodeTemplateInformation.load(m_workflowSett, getLoadVersion());
                CheckUtils.checkSettingNotNull(m_templateInformation, "No template information");
                return templateInformation;
            }
            return m_templateInformation; // don't change (set again with identical value)
        }, loadResult));

        setAuthorInformation(tryLoadDebug("workflow author information", AuthorInformation.UNKNOWN,
            () -> loadAuthorInformation(m_workflowSett, loadResult), loadResult));

        setWorkflowVariables(tryLoadDebug("workflow variables", Collections.emptyList(),
            () -> loadWorkflowVariables(m_workflowSett), loadResult));

        setCredentials(tryLoadDebug("credentials", Collections.emptyList(), () -> {
            var credentials = loadCredentials(m_workflowSett);
            // request to initialize credentials - if available
            if (credentials != null && !credentials.isEmpty()) {
                credentials = getLoadHelper().loadCredentialsPrefilled(credentials);
            }
            return credentials;
        }, loadResult));

        WorkflowTableBackendSettings m_tableBackendSettings = null;
        try {
            m_tableBackendSettings = loadTableBackendSettings(m_workflowSett);
        } catch (InvalidSettingsException e) {
            var error = "Unable to load table backend: " + e.getMessage();
            getLogger().debug(error, e);
            loadResult.setDirtyAfterLoad();
            loadResult.setResetRequiredAfterLoad();
            loadResult.addError(error, true);
            if (e instanceof TableBackendUnknownException) { // NOSONAR
                loadResult.addMissingTableFormat(((TableBackendUnknownException)e).getFormatInfo());
            }
            m_tableBackendSettings = isProject() ? new WorkflowTableBackendSettings() : null;
        }
        setWorkflowBackendTableSettings(m_tableBackendSettings);

        setWorkflowAnnotations(tryLoadDebug("workflow annotations", Collections.emptyList(),
            () -> loadWorkflowAnnotations(m_workflowSett), loadResult));

        setWizardState(tryLoadDebug("wizard state", null, () -> loadWizardState(m_workflowSett),             loadResult));

        NodeSettingsRO metaFlowParentSettings = new NodeSettings("fake_parent_settings");
        try {
            metaFlowParentSettings = readParentSettings();
        } catch (IOException e1) {
            var error = "Errors reading settings file: " + e1.getMessage();
            getLogger().warn(error, e1);
            loadResult.setDirtyAfterLoad();
            loadResult.addError(error);
        }

        /**
         * Data loading Detect errors and signal them
         */
        var isResetRequired = m_metaPersistor.load(subWFSettings, metaFlowParentSettings, loadResult);

        if (isResetRequired) {
            loadResult.setResetRequiredAfterLoad();
        }
        if (m_metaPersistor.isDirtyAfterLoad()) {
            loadResult.setDirtyAfterLoad();
        }

        var inPortCount = readInports(loadResult);

        var outPortCount = readOutports(loadResult);

        var hasPorts = inPortCount > 0 || outPortCount > 0;
        if (hasPorts && m_isProject) {
            throw new InvalidSettingsException(
                String.format("Workflow \"%s\"" + " is not a project as it has ports (%d in, %d out)",
                    nodeFile.getAbsoluteFile(), inPortCount, outPortCount));
        }

        setInPortsBarUIInfo(tryLoadDebug("inport bar's UI information", null, () -> {
            if (!getLoadVersion().isOlderThan(LoadVersion.V200)) {
                return loadNodeUIInformation(loadInPortsSetting(m_workflowSett));
            }
            return null;
        }, loadResult));

        setOutPortsBarUIInfo(tryLoadDebug("output bar's UI information", null, () -> {
            if (!getLoadVersion().isOlderThan(LoadVersion.V200)) {
                return loadNodeUIInformation(loadOutPortsSetting(m_workflowSett));
            }
            return null;
        }, loadResult));

        setEditorUIInfo(tryLoadDebug("editor UI information", null, () -> loadEditorUIInformation(m_workflowSett),
            loadResult));
    }

    /**
     * @param m_tableBackendSettings
     * @throws InvalidSettingsException
     */
    private void setWorkflowBackendTableSettings(final WorkflowTableBackendSettings settingsObject)
        throws InvalidSettingsException {
        var container = new NodeSettings("Container for workflow table backend settings.");
        settingsObject.saveSettingsTo(container);
        m_workflowProjectDefBuilder.setTableBackendSettings(CoreToDefUtil.toConfigMapDef(container));
    }

    /**
     * @param loadResult
     * @return the number of outports
     */
    private int readOutports(final LoadResult loadResult) {
        var outPortsEnum = EMPTY_SETTINGS;
        try {
            var outPorts = loadOutPortsSetting(m_workflowSett);
            if (outPorts != null) {
                outPortsEnum = loadOutPortsSettingsEnum(outPorts);
            }
        } catch (InvalidSettingsException e) {
            var error = "Can't load workflow out ports, config not found: " + e.getMessage();
            getLogger().debug(error, e);
            loadResult.setDirtyAfterLoad();
            loadResult.addError(error);
        }
        var outPortCount = outPortsEnum.keySet().size();
        m_outPortTemplates = new WorkflowPortTemplate[outPortCount];
        for (String key : outPortsEnum.keySet()) {
            WorkflowPortTemplate p;
            try {
                var sub = outPortsEnum.getNodeSettings(key);
                p = loadOutPortTemplate(sub);
            } catch (InvalidSettingsException e) {
                var error = "Can't load workflow outport (internal ID \"" + key + "\", skipping it: " + e.getMessage();
                getLogger().debug(error, e);
                loadResult.setDirtyAfterLoad();
                loadResult.addError(error);
                loadResult.setResetRequiredAfterLoad();
                continue;
            }
            var index = p.getPortIndex();
            if (index < 0 || index >= outPortCount) {
                loadResult.setDirtyAfterLoad();
                loadResult.addError("Invalid inport index " + index);
                loadResult.setResetRequiredAfterLoad();
                continue;
            }
            if (m_outPortTemplates[index] != null) {
                loadResult.setDirtyAfterLoad();
                loadResult.addError("Duplicate outport definition for index: " + index);
            }
            m_outPortTemplates[index] = p;
        }
        for (var i = 0; i < m_outPortTemplates.length; i++) {
            if (m_outPortTemplates[i] == null) {
                loadResult.setDirtyAfterLoad();
                loadResult.addError("Assigning fallback port type for " + "missing output port " + i);
                m_outPortTemplates[i] = new WorkflowPortTemplate(i, FALLBACK_PORTTYPE);
            }
        }
        return outPortCount;
    }

    /**
     * TODO untangle data loading from dirty setting TODO looks just like
     * {@link #readOutports(org.knime.core.node.workflow.WorkflowPersistor.LoadResult)}, maybe unify
     *
     * @param loadResult
     * @return the number of inports
     */
    private int readInports(final LoadResult loadResult) {
        /* read in and outports */
        var inPortsEnum = EMPTY_SETTINGS;
        try {
            var inPorts = loadInPortsSetting(m_workflowSett);
            if (inPorts != null) {
                inPortsEnum = loadInPortsSettingsEnum(inPorts);
            }
        } catch (InvalidSettingsException e) {
            var error = "Can't load workflow ports, config not found";
            getLogger().debug(error, e);
            loadResult.setDirtyAfterLoad();
            loadResult.addError(error);
            loadResult.setResetRequiredAfterLoad();
        }
        var inPortCount = inPortsEnum.keySet().size();
        m_inPortTemplates = new WorkflowPortTemplate[inPortCount];
        for (String key : inPortsEnum.keySet()) {
            WorkflowPortTemplate p;
            try {
                var sub = inPortsEnum.getNodeSettings(key);
                p = loadInPortTemplate(sub);
            } catch (InvalidSettingsException e) {
                var error = "Can't load workflow inport (internal ID \"" + key + "\", skipping it: " + e.getMessage();
                getLogger().debug(error, e);
                loadResult.setDirtyAfterLoad();
                loadResult.addError(error);
                loadResult.setResetRequiredAfterLoad();
                continue;
            }
            var index = p.getPortIndex();
            if (index < 0 || index >= inPortCount) {
                loadResult.setDirtyAfterLoad();
                loadResult.addError("Invalid inport index " + index);
                loadResult.setResetRequiredAfterLoad();
                continue;
            }
            if (m_inPortTemplates[index] != null) {
                loadResult.setDirtyAfterLoad();
                loadResult.addError("Duplicate inport definition for index: " + index);
            }
            m_inPortTemplates[index] = p;
        }
        for (var i = 0; i < m_inPortTemplates.length; i++) {
            if (m_inPortTemplates[i] == null) {
                loadResult.setDirtyAfterLoad();
                loadResult.addError("Assigning fallback port type for " + "missing input port " + i);
                m_inPortTemplates[i] = new WorkflowPortTemplate(i, FALLBACK_PORTTYPE);
            }
        }
        return inPortCount;
    }

    /** Originally from {@link TemplateNodeContainerPersistor} */
    public void loadNodeContainer(final Map<Integer, BufferedDataTable> tblRep, final ExecutionMonitor exec,
        final LoadResult loadResult) throws CanceledExecutionException, IOException {
        var workflowKNIMEFile = getWorkflowKNIMEFile();
        if (workflowKNIMEFile == null || m_workflowSett == null) {
            loadResult.setDirtyAfterLoad();
            throw new IllegalStateException("The method preLoadNodeContainer has either not been called or failed");
        }
        /* read nodes */
        NodeSettingsRO nodes;
        try {
            nodes = loadSettingsForNodes(m_workflowSett);
        } catch (InvalidSettingsException e) {
            var error = "Can't load nodes in workflow, config not found: " + e.getMessage();
            getLogger().debug(error, e);
            loadResult.addError(error);
            loadResult.setDirtyAfterLoad();
            loadResult.setResetRequiredAfterLoad();
            // stop loading here
            return;
        }
        // ids of nodes that failed to load. Used to suppress superfluous errors when reading the connections
        Set<Integer> failingNodeIDSet = new HashSet<>();
        // ids of nodes whose factory can't be loaded (e.g. node extension not installed)
        Map<Integer, NodeFactoryUnknownException> missingNodeIDMap = new HashMap<>();

        exec.setMessage("node information");

        final var workflowDirRef = workflowKNIMEFile.getParent();

        for (String nodeKey : nodes.keySet()) {
            loadNode(workflowDirRef, nodeKey, exec, loadResult, nodes, failingNodeIDSet, missingNodeIDMap, workflowKNIMEFile);

        }

        /* read connections */
        exec.setMessage("connection information");
        loadConnections(exec, loadResult, failingNodeIDSet);

        exec.setProgress(1.0);
    }



    /**
     * @param workflowDirRef
     * @param nodeKey
     * @param exec
     * @param loadResult
     * @param nodes
     * @param failingNodeIDSet
     * @param missingNodeIDMap
     * @param workflowKNIMEFile
     *
     * @return a node describing what's behind the given node id (might be a metanode, component, or native node).
     *
     * @throws CanceledExecutionException
     */
    private Optional<NodeDef> loadNode(final ReferencedFile workflowDirRef, final String nodeKey, final ExecutionMonitor exec, final LoadResult loadResult,
        final NodeSettingsRO nodes, final Set<Integer> failingNodeIDSet,
        final Map<Integer, NodeFactoryUnknownException> missingNodeIDMap, final ReferencedFile workflowKNIMEFile)
        throws CanceledExecutionException {

        exec.checkCanceled();
        NodeSettingsRO nodeSetting;
        try {
            nodeSetting = nodes.getNodeSettings(nodeKey);
        } catch (InvalidSettingsException e) {
            var error = "Unable to load settings for node with internal " + "id \"" + nodeKey + "\": " + e.getMessage();
            getLogger().debug(error, e);
            loadResult.setDirtyAfterLoad();
            loadResult.addError(error);
            // maybe this should throw an exception instead of skipping the node
            return Optional.empty();
        }

        // TODO move to loader for old file formats see implementation in ObsoleteMetaNodeFileWorkflowPersistor
        if (shouldSkipThisNode(nodeSetting)) {
            return Optional.empty();
        }

        int nodeIDSuffix;
        try {
            nodeIDSuffix = loadNodeIDSuffix(nodeSetting);
        } catch (InvalidSettingsException e) {
            nodeIDSuffix = getRandomNodeID();
            var error = "Unable to load node ID (internal id \"" + nodeKey + "\"), trying random number " + nodeIDSuffix
                + "instead: " + e.getMessage();
            getLogger().debug(error, e);
            loadResult.setDirtyAfterLoad();
            loadResult.addError(error);
        }

        // TODO fix
        NodeType nodeType = tryLoadDebug(String.format(
            "Can't retrieve node type for contained node with id suffix %d, attempting to read ordinary (native) node: "),
            NodeType.NativeNode, () -> loadNodeType(nodeSetting), loadResult);

        NodeUIInformation nodeUIInfo = null;
        String uiInfoClassName;
        try {
            uiInfoClassName = loadUIInfoClassName(nodeSetting);
        } catch (InvalidSettingsException e) {
            var error = "Unable to load UI information class name " + "to node with ID suffix " + nodeIDSuffix
                + ", no UI information available: " + e.getMessage();
            getLogger().debug(error, e);
            loadResult.setDirtyAfterLoad();
            loadResult.addError(error);
            uiInfoClassName = null;
        }
        if (uiInfoClassName != null) {
            try {
                //load node ui info
                nodeUIInfo = loadNodeUIInformation(nodeSetting);
            } catch (InvalidSettingsException e) {
                var error = "Unable to load UI information to " + "node with ID suffix " + nodeIDSuffix
                    + ", no UI information available: " + e.getMessage();
                getLogger().debug(error, e);
                loadResult.setDirtyAfterLoad();
                loadResult.addError(error);
            }
        }

        ReferencedFile nodeFile;
        try {
            nodeFile = loadNodeFile(nodeSetting, workflowDirRef);
        } catch (InvalidSettingsException e) {
            var error = "Unable to load settings for node " + "with ID suffix " + nodeIDSuffix + ": " + e.getMessage();
            getLogger().debug(error, e);
            loadResult.setDirtyAfterLoad();
            loadResult.addError(error);
            failingNodeIDSet.add(nodeIDSuffix);
            return Optional.empty();
        }
        NodeContainerLoader persistor;
        switch (nodeType) {
            case MetaNode:
                persistor = createWorkflowLoader(nodeFile);
                break;
            case NativeNode:
                persistor = createNativeNodeContainerPersistorLoad(nodeFile);
                break;
            case SubNode:
                persistor = createSubNodeContainerPersistorLoad(nodeFile);
                break;
            default:
                throw new IllegalStateException("Unknown node type: " + nodeType);
        }

        try {
            var childResult = new LoadResult(nodeType.toString() + " with ID suffix " + nodeIDSuffix);

            /**
             * Recurse
             */
            var childDef = persistor.getLoadResult(this, nodeSetting, childResult);
            m_nodesToAdd.put(nodeIDSuffix, childDef);

            loadResult.addChildError(childResult);
        } catch (Throwable e) {
            var error =
                "Unable to load node with ID suffix " + nodeIDSuffix + " into workflow, skipping it: " + e.getMessage();
            String loadErrorString;
            if (e instanceof NodeFactoryUnknownException) {
                loadErrorString = e.getMessage();
            } else {
                loadErrorString = error;
            }
            if (e instanceof InvalidSettingsException || e instanceof IOException
                || e instanceof NodeFactoryUnknownException) {
                getLogger().debug(error, e);
            } else {
                getLogger().error(error, e);
            }
            loadResult.addError(loadErrorString);
            if (e instanceof NodeFactoryUnknownException) {
                missingNodeIDMap.put(nodeIDSuffix, (NodeFactoryUnknownException)e);
                // don't set dirty
            } else {
                loadResult.setDirtyAfterLoad();
                failingNodeIDSet.add(nodeIDSuffix);
                // node directory is the parent of the settings.xml
                m_obsoleteNodeDirectories.add(nodeFile.getParent());
                return Optional.empty();
            }
        }
        // NodeContainerMetaPersistor

//        var meta = persistor.getMetaPersistor();
//        if (m_nodeContainerLoaderMap.containsKey(nodeIDSuffix)) {
//            var randomID = getRandomNodeID();
//            loadResult.setDirtyAfterLoad();
//            loadResult.addError("Duplicate id encountered in workflow: " + nodeIDSuffix + ", uniquifying to random id "
//                + randomID + ", this possibly screws the connections");
//            nodeIDSuffix = randomID;
//        }
//        meta.setNodeIDSuffix(nodeIDSuffix);
//        meta.setUIInfo(nodeUIInfo);
//        if (persistor.isDirtyAfterLoad()) {
//            loadResult.setDirtyAfterLoad();
//        }
//        m_nodeContainerLoaderMap.put(nodeIDSuffix, persistor);
        // TODO
        return null;
    }

    /**
     * @param nodeIDSuffix
     * @param childDef
     */
    private void addNode(final int nodeIDSuffix, final NodeDef childDef) {
        // TODO Auto-generated method stub

    }

    /**
     * @param exec
     * @param loadResult
     * @param failingNodeIDSet
     * @throws CanceledExecutionException
     */
    private void loadConnections(final ExecutionMonitor exec, final LoadResult loadResult,
        final Set<Integer> failingNodeIDSet) throws CanceledExecutionException {
        NodeSettingsRO connections;
        try {
            connections = loadSettingsForConnections(m_workflowSett);
            if (connections == null) {
                connections = EMPTY_SETTINGS;
            }
        } catch (InvalidSettingsException e) {
            var error = "Can't load workflow connections, config not found: " + e.getMessage();
            getLogger().debug(error, e);
            loadResult.setDirtyAfterLoad();
            loadResult.addError(error);
            connections = EMPTY_SETTINGS;
        }
        for (String connectionKey : connections.keySet()) {
            exec.checkCanceled();
            ConnectionDef c;
            try {
                c = loadConnection(connections.getNodeSettings(connectionKey));
            } catch (InvalidSettingsException e) {
                var error = "Can't load connection with internal ID \"" + connectionKey + "\": " + e.getMessage();
                getLogger().debug(error, e);
                loadResult.setDirtyAfterLoad();
                loadResult.addError(error);
                continue;
            }
            int sourceIDSuffix = c.getSourceID();
            NodeContainerPersistor sourceNodePersistor = m_nodeContainerLoaderMap.get(sourceIDSuffix);
            if (sourceNodePersistor == null && sourceIDSuffix != -1) {
                loadResult.setDirtyAfterLoad();
                if (!failingNodeIDSet.contains(sourceIDSuffix)) {
                    loadResult.addError("Unable to load node connection " + c + ", source node does not exist");
                }
                continue;
            }
            // TODO rewrite to accept def
            //            FileWorkflowPersistor.fixSourcePortIfNecessary(sourceNodePersistor, c, getLoadVersion());

            int destIDSuffix = c.getDestID();
            NodeContainerPersistor destNodePersistor = m_nodeContainerLoaderMap.get(destIDSuffix);
            if (destNodePersistor == null && destIDSuffix != -1) {
                loadResult.setDirtyAfterLoad();
                if (!failingNodeIDSet.contains(destIDSuffix)) {
                    loadResult.addError("Unable to load node connection " + c + ", destination node does not exist");
                }
                continue;
            }
            // TODO rewrite to accept def
            //            FileWorkflowPersistor.fixDestPortIfNecessary(destNodePersistor, c, getLoadVersion());

            if (!m_connectionSet.add(c)) {
                loadResult.setDirtyAfterLoad();
                loadResult.addError("Duplicate connection information: " + c);
            }
        }
    }

    // TODO return def
    private NodeUIInformation loadNodeUIInformation(final NodeSettingsRO portSettings) throws InvalidSettingsException {

        // in previous releases, the settings were directly written to the
        // top-most node settings object; since 2.0 they are put into a
        // separate sub-settings object
        var subSettings = getLoadVersion().isOlderThan(LoadVersion.V200) ? portSettings
            : portSettings.getNodeSettings(CFG_UIINFO_SUB_CONFIG);
        final var loadOrdinal = getLoadVersion().ordinal();
        var bounds = subSettings.getIntArray(KEY_BOUNDS);
        var symbolRelative = loadOrdinal >= LoadVersion.V230.ordinal();
        return NodeUIInformation.builder().setNodeLocation(bounds[0], bounds[1], bounds[2], bounds[3])
            .setIsSymbolRelative(symbolRelative).build();
    }

    /** Fills the list with null so that list.get(index) doesn't throw an exception. */
    private static void ensureArrayListIndexValid(final ArrayList<?> list, final int index) {
        for (var i = list.size(); i <= index; i++) {
            list.add(null);
        }
    }

    NodeSettingsRO readParentSettings() throws IOException {
        if (getLoadVersion().isOlderThan(LoadVersion.V200)) {
            var result = new NodeSettings("generated_wf_settings");
            result.addBoolean("isExecuted", false);
            result.addBoolean("isConfigured", false);
            return result;
        }
        return null; // only used in 1.3.x
    }

    /**
     * This is overridden by the metanode loader (1.x.x) and returns true for the "special" nodes.
     *
     * @param settings node sub-element
     * @return true if to skip (though in 99.9% false)
     */
    boolean shouldSkipThisNode(final NodeSettingsRO settings) {
        return false;
    }

    int loadNodeIDSuffix(final NodeSettingsRO settings) throws InvalidSettingsException {
        return settings.getInt(KEY_ID);
    }

    String loadUIInfoClassName(final NodeSettingsRO settings) throws InvalidSettingsException {
        if (getLoadVersion().isOlderThan(LoadVersion.V200)) {
            if (settings.containsKey(KEY_UI_INFORMATION)) {
                return settings.getString(KEY_UI_INFORMATION);
            }
        } else {
            if (settings.containsKey(CFG_UIINFO_CLASS)) {
                return settings.getString(CFG_UIINFO_CLASS);
            }
        }
        return null;
    }

    String fixUIInfoClassName(final String name) {
        if (("org.knime.workbench.editor2.extrainfo." + "ModellingNodeExtraInfo").equals(name)) {
            return "org.knime.core.node.workflow.NodeUIInformation";
        } else if (("org.knime.workbench.editor2.extrainfo." + "ModellingConnectionExtraInfo").equals(name)) {
            return "org.knime.core.node.workflow.ConnectionUIInformation";
        }
        return name;
    }

    /**
     * Creates the <code>UIInformaion</code> from given settings, describing whatever additional information was stored
     * (graphical layout?).
     *
     * @param className The name of the class to be loaded.
     * @return new <code>UIInformation</code> object or null
     */
    UIInformation loadUIInfoInstance(final String className) {
        if (className == null) {
            return null;
        }
        var fixedName = fixUIInfoClassName(className);
        try {
            // avoid NoClassDefFoundErrors by using magic class loader
            return (UIInformation)(Class.forName(fixedName).newInstance());
        } catch (Exception e) {
            var b = new StringBuilder();
            b.append("UIInfo class \"");
            b.append(className);
            b.append("\"");
            if (!className.equals(fixedName)) {
                b.append(" programmatically changed to \"");
                b.append(fixedName).append("\"");
            }
            b.append(" could not be loaded: ");
            b.append(e.getMessage());
            var error = b.toString();
            getLogger().warn(error, e);
            return null;
        }
    }

    void loadUIInfoSettings(final UIInformation uiInfo, final NodeSettingsRO settings) throws InvalidSettingsException {
        // in previous releases, the settings were directly written to the
        // top-most node settings object; since 2.0 they are put into a
        // separate sub-settings object
        var subSettings =
            getLoadVersion().isOlderThan(LoadVersion.V200) ? settings : settings.getNodeSettings(CFG_UIINFO_SUB_CONFIG);
        uiInfo.load(subSettings, getLoadVersion());
    }

    /**
     * Load editor information (grid settings &amp; zoom level).
     *
     * @param settings ...
     * @return null
     * @since 2.6
     * @throws InvalidSettingsException ...
     */
    EditorUIInformation loadEditorUIInformation(final NodeSettingsRO settings) throws InvalidSettingsException {
        final var loadVersion = getLoadVersion();
        if (loadVersion.isOlderThan(LoadVersion.V260) || !settings.containsKey(CFG_EDITOR_INFO)) {
            return EditorUIInformation.builder().build();
        }
        var editorCfg = settings.getNodeSettings(CFG_EDITOR_INFO);
        var builder = EditorUIInformation.builder();
        builder.setSnapToGrid(editorCfg.getBoolean(CFG_EDITOR_SNAP_GRID));
        builder.setShowGrid(editorCfg.getBoolean(CFG_EDITOR_SHOW_GRID));
        builder.setGridX(editorCfg.getInt(CFG_EDITOR_X_GRID));
        builder.setGridY(editorCfg.getInt(CFG_EDITOR_Y_GRID));
        builder.setZoomLevel(editorCfg.getDouble(CFG_EDITOR_ZOOM));
        if (editorCfg.containsKey(CFG_EDITOR_CURVED_CONNECTIONS)) {
            builder.setHasCurvedConnections(editorCfg.getBoolean(CFG_EDITOR_CURVED_CONNECTIONS));
        }
        if (editorCfg.containsKey(CFG_EDITOR_CONNECTION_WIDTH)) {
            builder.setConnectionLineWidth(editorCfg.getInt(CFG_EDITOR_CONNECTION_WIDTH));
        }
        return builder.build();
    }

    ReferencedFile loadNodeFile(final NodeSettingsRO settings, final ReferencedFile workflowDirRef)
        throws InvalidSettingsException {
        var fileString = settings.getString("node_settings_file");
        if (fileString == null) {
            throw new InvalidSettingsException("Unable to read settings " + "file for node " + settings.getKey());
        }
        var workflowDir = workflowDirRef.getFile();
        // fileString is something like "File Reader(#1)/settings.xml", thus
        // it contains two levels of the hierarchy. We leave it here to the
        // java.io.File implementation to resolve these levels
        var fullFile = new File(workflowDir, fileString);
        if (!fullFile.isFile() || !fullFile.canRead()) {
            throw new InvalidSettingsException("Unable to read settings " + "file " + fullFile.getAbsolutePath());
        }
        var children = new Stack<String>();
        var workflowDirAbsolute = workflowDir.getAbsoluteFile();
        while (!fullFile.getAbsoluteFile().equals(workflowDirAbsolute)) {
            children.push(fullFile.getName());
            fullFile = fullFile.getParentFile();
        }
        // create a ReferencedFile hierarchy for the settings file
        var result = workflowDirRef;
        while (!children.empty()) {
            result = new ReferencedFile(result, children.pop());
        }
        return result;
    }

    NodeType loadNodeType(final NodeSettingsRO settings) throws InvalidSettingsException {
        if (getLoadVersion().isOlderThan(LoadVersion.V200)) {
            var factory = settings.getString("factory");
            if (ObsoleteMetaNodeFileWorkflowPersistor.OLD_META_NODES.contains(factory)) {
                return NodeType.MetaNode;
            } else {
                return NodeType.NativeNode;
            }
        } else if (getLoadVersion().isOlderThan(LoadVersion.V2100Pre)) {
            return settings.getBoolean("node_is_meta") ? NodeType.MetaNode : NodeType.NativeNode;
        } else {
            final var nodeTypeString = settings.getString("node_type");
            CheckUtils.checkSettingNotNull(nodeTypeString, "node type must not be null");
            try {
                return NodeType.valueOf(nodeTypeString);
            } catch (IllegalArgumentException iae) {
                throw new InvalidSettingsException("Can't parse node type: " + nodeTypeString);
            }
        }
    }

    NodeSettingsRO loadSettingsForConnections(final NodeSettingsRO set) throws InvalidSettingsException {
        return set.getNodeSettings(KEY_CONNECTIONS);
    }

    ConnectionDef loadConnection(final NodeSettingsRO settings) throws InvalidSettingsException {
        var sourceID = settings.getInt("sourceID");
        var destID = loadConnectionDestID(settings);
        var sourcePort = loadConnectionSourcePort(settings);
        var destPort = loadConnectionDestPort(settings);
        // this attribute is in most cases not present (not saved)
        var isDeletable = settings.getBoolean("isDeletable", true);
        if (sourceID != -1 && sourceID == destID) {
            throw new InvalidSettingsException("Source and Destination must " + "not be equal, id is " + sourceID);
        }
        ConnectionUIInformation connUIInfo = null;
        try {
            var uiInfoClass = loadUIInfoClassName(settings);
            if (uiInfoClass != null) {
                if (!uiInfoClass.equals(ConnectionUIInformation.class.getName())) {
                    getLogger().debug("Could not load UI information for " + "connection between nodes " + sourceID
                        + " and " + destID + ": expected " + ConnectionUIInformation.class.getName() + " but got "
                        + uiInfoClass.getClass().getName());
                } else {
                    var builder = ConnectionUIInformation.builder();
                    // in previous releases, the settings were directly written to the
                    // top-most node settings object; since 2.0 they are put into a
                    // separate sub-settings object
                    var subSettings = getLoadVersion().isOlderThan(LoadVersion.V200) ? settings
                        : settings.getNodeSettings(CFG_UIINFO_SUB_CONFIG);
                    var size = subSettings.getInt(KEY_BENDPOINTS + "_size");
                    for (var i = 0; i < size; i++) {
                        var tmp = subSettings.getIntArray(KEY_BENDPOINTS + "_" + i);
                        //TODO add bendpoint directly as int array
                        builder.addBendpoint(tmp[0], tmp[1], i);
                    }
                    connUIInfo = builder.build();
                }
            }
        } catch (InvalidSettingsException ise) {
            getLogger().debug(
                "Could not load UI information for connection " + "between nodes " + sourceID + " and " + destID);
        } catch (Throwable t) {
            getLogger().warn(
                "Exception while loading connection UI " + "information between nodes " + sourceID + " and " + destID,
                t);
        }
        return DefaultConnectionDef.builder()//
            .setSourceID(sourceID)//
            .setSourcePort(sourcePort)//
            .setDestID(destID)//
            .setDestPort(destPort)//
            .setDeletable(isDeletable)//
            .setUiSettings(CoreToDefUtil.toConnectionUISettingsDef(connUIInfo))// TODO maybe inline
            .build();
    }

    int loadConnectionDestID(final NodeSettingsRO settings) throws InvalidSettingsException {
        if (getLoadVersion().isOlderThan(LoadVersion.V200)) {
            return settings.getInt("targetID");
        } else {
            return settings.getInt("destID");
        }
    }

    int loadConnectionDestPort(final NodeSettingsRO settings) throws InvalidSettingsException {
        if (getLoadVersion().isOlderThan(LoadVersion.V200)) {
            return settings.getInt("targetPort");
        } else {
            // possibly port index correction in fixDestPort method
            return settings.getInt("destPort");
        }
    }

    int loadConnectionSourcePort(final NodeSettingsRO settings) throws InvalidSettingsException {
        return settings.getInt("sourcePort");
    }

    NodeSettingsRO loadSettingsForNodes(final NodeSettingsRO set) throws InvalidSettingsException {
        return set.getNodeSettings(KEY_NODES);
    }

    /**
     * Sub class hook o read workflow name.
     *
     * @param set Ignored.
     * @return "Workflow Manager"
     * @throws InvalidSettingsException Not actually thrown here.
     */
    String loadWorkflowName(final NodeSettingsRO set) throws InvalidSettingsException {
        if (getLoadVersion().isOlderThan(LoadVersion.V200)) {
            return "Workflow Manager";
        } else {
            return set.getString("name");
        }
    }

    WorkflowCipher loadWorkflowCipher(final LoadVersion loadVersion, final NodeSettingsRO settings)
        throws InvalidSettingsException {
        // added in v2.5 - no check necessary
        if ((getLoadVersion().ordinal() < LoadVersion.V250.ordinal()) || !settings.containsKey("cipher")) {
            return WorkflowCipher.NULL_CIPHER;
        }
        var cipherSettings = settings.getNodeSettings("cipher");
        return WorkflowCipher.load(loadVersion, cipherSettings);
    }

    /**
     * Synchronized call to DATE_FORMAT.parse(String).
     *
     * @param s To parse, not null.
     * @return The date.
     * @throws DateTimeParseException if the text cannot be parsed
     */
    private static OffsetDateTime parseDate(final String s) {
        synchronized (DATE_FORMAT) {
            return OffsetDateTime.parse(s, DATE_FORMAT);
        }
    }

    private AuthorInformation loadAuthorInformation(final NodeSettingsRO settings, final LoadResult loadResult)
        throws InvalidSettingsException {
        if (getLoadVersion().ordinal() >= LoadVersion.V280.ordinal() && settings.containsKey(CFG_AUTHOR_INFORMATION)) {
            final var sub = settings.getNodeSettings(CFG_AUTHOR_INFORMATION);
            final var author = sub.getString("authored-by");
            final var authorDateS = sub.getString("authored-when");
            OffsetDateTime authorDate;
            if (authorDateS == null) {
                authorDate = null;
            } else {
                try {
                    authorDate = parseDate(authorDateS);
                } catch (DateTimeParseException e) {
                    authorDate = OffsetDateTime.ofInstant(Instant.EPOCH, ZoneOffset.UTC);
                    loadResult.addWarning(String.format("Can't parse authored-when-date \"%s\". Replaced with \"%s\".",
                        authorDateS, authorDate.toString()));
                }
            }
            final var editor = sub.getString("lastEdited-by");
            final var editDateS = sub.getString("lastEdited-when");
            OffsetDateTime editDate;
            if (editDateS == null) {
                editDate = null;
            } else {
                try {
                    editDate = parseDate(editDateS);
                } catch (DateTimeParseException e) {
                    editDate = OffsetDateTime.ofInstant(Instant.EPOCH, ZoneOffset.UTC);
                    loadResult.addWarning(String.format("Can't parse lastEdit-when-date \"%s\". Replaced with \"%s\".",
                        editDateS, editDate.toString()));
                }
            }
            return new AuthorInformation(author, authorDate, editor, editDate);
        } else {
            return AuthorInformation.UNKNOWN;
        }
    }

    /**
     * Load workflow variables (not available in 1.3.x flows).
     *
     * @param settings To load from.
     * @return The variables in a list.
     * @throws InvalidSettingsException If any settings-related error occurs.
     */
    List<FlowVariable> loadWorkflowVariables(final NodeSettingsRO settings) throws InvalidSettingsException {
        if (getLoadVersion().isOlderThan(LoadVersion.V200)) {
            return Collections.emptyList();
        } else {
            if (!settings.containsKey(CFG_WKF_VARIABLES)) {
                return Collections.emptyList();
            }
            var wfmVarSub = settings.getNodeSettings(CFG_WKF_VARIABLES);
            List<FlowVariable> result = new ArrayList<>();
            for (String key : wfmVarSub.keySet()) {
                result.add(FlowVariable.load(wfmVarSub.getNodeSettings(key)));
            }
            return result;
        }
    }

    /**
     * Loads credentials, this method returns an empty list. Credentials added for v2.2
     *
     * @param settings to load from.
     * @return the credentials list
     * @throws InvalidSettingsException If this fails for any reason.
     */
    List<Credentials> loadCredentials(final NodeSettingsRO settings) throws InvalidSettingsException {
        if (getLoadVersion().isOlderThan(LoadVersion.V220)) {
            // no credentials in v2.1 and before
            return Collections.emptyList();
        } else {
            var sub = settings.getNodeSettings(CFG_CREDENTIALS);
            List<Credentials> r = new ArrayList<>();
            Set<String> credsNameSet = new HashSet<>();
            for (String key : sub.keySet()) {
                var child = sub.getNodeSettings(key);
                var c = Credentials.load(child);
                if (!credsNameSet.add(c.getName())) {
                    getLogger().warn("Duplicate credentials variable \"" + c.getName() + "\" -- ignoring it");
                } else {
                    r.add(c);
                }
            }
            return r;
        }
    }

    /**
     * Loads table backend settings (only for workflow projects). Might throw {@link TableBackendUnknownException}.
     */
    WorkflowTableBackendSettings loadTableBackendSettings(final NodeSettingsRO settings)
        throws InvalidSettingsException {
        if (!isProject()) {
            throw new IllegalStateException("Cannot load table backend settings for a workflow that is not a project.");
        } else {
            // added in 4.2.2
            return WorkflowTableBackendSettings.loadSettingsInModel(settings);
        }
    }

    /**
     * Load annotations (added in v2.3).
     *
     * @param settings to load from
     * @return non-null list.
     * @throws InvalidSettingsException If this fails for any reason.
     */
    List<WorkflowAnnotation> loadWorkflowAnnotations(final NodeSettingsRO settings) throws InvalidSettingsException {
        if (getLoadVersion().isOlderThan(LoadVersion.V230)) {
            // no credentials in v2.2 and before
            return Collections.emptyList();
        } else {
            if (!settings.containsKey("annotations")) {
                return Collections.emptyList();
            }
            var annoSettings = settings.getNodeSettings("annotations");
            List<WorkflowAnnotation> result = new ArrayList<>();
            for (String key : annoSettings.keySet()) {
                var child = annoSettings.getNodeSettings(key);
                var anno = new WorkflowAnnotation();
                anno.load(child, getLoadVersion());
                result.add(anno);
            }
            return result;
        }
    }

    /**
     * @return the wizard state saved in the file or null (often null).
     * @param settings ...
     * @throws InvalidSettingsException ...
     */
    NodeSettingsRO loadWizardState(final NodeSettingsRO settings) throws InvalidSettingsException {
        return settings.containsKey("wizard") ? settings.getNodeSettings("wizard") : null;
    }

    /**
     * Sub class hook o read port settings.
     *
     * @param settings Ignored.
     * @return null
     * @throws InvalidSettingsException Not actually thrown here.
     */
    NodeSettingsRO loadInPortsSetting(final NodeSettingsRO settings) throws InvalidSettingsException {
        if (getLoadVersion().isOlderThan(LoadVersion.V200)) {
            return null;
        }
        if (settings.containsKey("meta_in_ports")) {
            return settings.getNodeSettings("meta_in_ports");
        }
        return null;
    }

    /**
     * Sub class hook o read port settings.
     *
     * @param settings Ignored.
     * @return null
     * @throws InvalidSettingsException Not actually thrown here.
     */
    NodeSettingsRO loadInPortsSettingsEnum(final NodeSettingsRO settings) throws InvalidSettingsException {
        if (getLoadVersion().isOlderThan(LoadVersion.V200)) {
            return null;
        }
        return settings.getNodeSettings("port_enum");
    }

    /**
     * Sub class hook o read port settings.
     *
     * @param settings Ignored.
     * @return null
     * @throws InvalidSettingsException Not actually thrown here.
     */
    WorkflowPortTemplate loadInPortTemplate(final NodeSettingsRO settings) throws InvalidSettingsException {
        if (getLoadVersion().isOlderThan(LoadVersion.V200)) {
            throw new InvalidSettingsException("No ports for metanodes in version 1.x.x");
        }
        var index = settings.getInt("index");
        var name = settings.getString("name");
        var portTypeSettings = settings.getNodeSettings("type");
        var type = PortType.load(portTypeSettings);
        var result = new WorkflowPortTemplate(index, type);
        result.setPortName(name);
        return result;
    }

    /**
     * Sub class hook o read port settings.
     *
     * @param settings Ignored.
     * @return null
     * @throws InvalidSettingsException Not actually thrown here.
     */
    NodeSettingsRO loadOutPortsSetting(final NodeSettingsRO settings) throws InvalidSettingsException {
        if (getLoadVersion().isOlderThan(LoadVersion.V200)) {
            return null;
        }
        if (settings.containsKey("meta_out_ports")) {
            return settings.getNodeSettings("meta_out_ports");
        }
        return null;
    }

    /**
     * Sub class hook o read port settings.
     *
     * @param settings Ignored.
     * @return null
     * @throws InvalidSettingsException Not actually thrown here.
     */
    NodeSettingsRO loadOutPortsSettingsEnum(final NodeSettingsRO settings) throws InvalidSettingsException {
        if (getLoadVersion().isOlderThan(LoadVersion.V200)) {
            return null;
        }
        return settings.getNodeSettings("port_enum");
    }

    /**
     * Sub class hook o read port settings.
     *
     * @param settings Ignored.
     * @return null
     * @throws InvalidSettingsException Not actually thrown here.
     */
    WorkflowPortTemplate loadOutPortTemplate(final NodeSettingsRO settings) throws InvalidSettingsException {
        if (getLoadVersion().isOlderThan(LoadVersion.V200)) {
            throw new InvalidSettingsException("No ports for metanodes in version 1.x.x");
        } else {
            var index = settings.getInt("index");
            var name = settings.getString("name");
            var portTypeSettings = settings.getNodeSettings("type");
            var type = PortType.load(portTypeSettings);
            var result = new WorkflowPortTemplate(index, type);
            result.setPortName(name);
            return result;
        }
    }

    /**
     * check whether there is a "loaded with no data" file.
     *
     * @param workflowDir ...
     * @return true for old workflows (&lt;2.0) or if there is a {@value WorkflowPersistor#SAVED_WITH_DATA_FILE} file.
     */
    boolean loadIfMustWarnOnDataLoadError(final File workflowDir) {
        if (getLoadVersion().isOlderThan(LoadVersion.V200)) {
            return true;
        }
        return new File(workflowDir, SAVED_WITH_DATA_FILE).isFile();
    }

    NodeContainerLoader createNativeNodeContainerPersistorLoad(final ReferencedFile nodeFile) {
        return null;
//        return new FileNativeNodeContainerLoader(nodeFile, getLoadHelper(), getLoadVersion(),
//            getWorkflowDataRepository(), mustWarnOnDataLoadError());
    }

    NodeContainerLoader createSubNodeContainerPersistorLoad(final ReferencedFile nodeFile) {
        return null;
//        return new FileSubNodeContainerLoader(nodeFile, getLoadHelper(), getLoadVersion(),
//            getWorkflowDataRepository(), mustWarnOnDataLoadError());
    }

    NodeContainerLoader createWorkflowLoader(final ReferencedFile wfmFile) {
        // TODO handle old versions
        //        if (getLoadVersion().isOlderThan(LoadVersion.V200)) {
        //            return new ObsoleteMetaNodeFileWorkflowPersistor(getWorkflowDataRepository(), wfmFile, getLoadHelper(),
        //                getLoadVersion());
        //        } else {
        return new FileWorkflowLoader(getWorkflowDataRepository(), wfmFile, getLoadHelper(), getLoadVersion(), false);
        //        }
    }

    private int getRandomNodeID() {
        // some number between 10k and 20k, hopefully unique.
        // TODO we can do better
        var nodeIDSuffix = new Random().nextInt(10_000) + 10_000;
        while (m_nodeContainerLoaderMap.containsKey(nodeIDSuffix)) {
            nodeIDSuffix += 1;
        }
        return nodeIDSuffix;
    }

    Optional<WorkflowExecutionResult> getExecutionResult() {
        return Optional.empty();
    }

    /**
     * @since 2.7
     */
    /** Originally from {@link TemplateNodeContainerPersistor} */
    public void guessPortTypesFromConnectedNodes(final NodeAndBundleInformationPersistor nodeInfo,
        final NodeSettingsRO additionalFactorySettings, final ArrayList<PersistorWithPortIndex> upstreamNodes,
        final List<List<PersistorWithPortIndex>> downstreamNodes) {
        // not applicable for metanodes
    }

    /**
     * @since 2.7
     */
    /** Originally from {@link TemplateNodeContainerPersistor} */
    public PortType getDownstreamPortType(final int index) {
        throw new NotImplementedException("downstream port type");
        // TODO remove
//        if (m_outPortTemplates != null && index < m_outPortTemplates.length) {
//            return m_outPortTemplates[index].getPortType();
//        }
//        return null;
    }

    /**
     * @since 2.7
     */
    /** Originally from {@link TemplateNodeContainerPersistor} */
    public PortType getUpstreamPortType(final int index) {
        // TODO remove
        throw new NotImplementedException("upstream port type");
//        if (m_inPortTemplates != null && index < m_inPortTemplates.length) {
//            return m_inPortTemplates[index].getPortType();
//        }
//        return null;
    }

    /**
     * @return version that is saved
     * @since 3.7
     */
    protected static LoadVersion getSaveVersion() {
        return VERSION_LATEST;
    }

    /**
     * Synchronized call to DATE_FORMAT.format(Date).
     *
     * @param date ... not null.
     * @return The string.
     */
    static String formatDate(final OffsetDateTime date) {
        synchronized (DATE_FORMAT) {
            return DATE_FORMAT.format(date);
        }
    }

    protected static void saveUIInfoClassName(final NodeSettingsWO settings, final UIInformation info) {
        settings.addString(CFG_UIINFO_CLASS, info != null ? info.getClass().getName() : null);
    }

    protected static void saveUIInfoSettings(final NodeSettingsWO settings, final UIInformation uiInfo) {
        if (uiInfo == null) {
            return;
        }
        // nest into separate sub config
        var subConfig = settings.addNodeSettings(CFG_UIINFO_SUB_CONFIG);
        uiInfo.save(subConfig);
    }

    public static String save(final WorkflowManager wm, final ReferencedFile rawWorkflowDirRef,
        final ExecutionMonitor execMon, final WorkflowSaveHelper saveHelper)
        throws IOException, CanceledExecutionException, LockFailedException {
        final var name = wm.getDirectNCParent().getCipherFileName(WORKFLOW_FILE);
        var preFilledSettings = new NodeSettings(name);
        saveHeader(preFilledSettings);
        wm.getTemplateInformation().save(preFilledSettings);
        saveWizardState(wm, preFilledSettings, saveHelper);
        saveContent(wm, preFilledSettings, rawWorkflowDirRef, execMon, saveHelper);
        return name;
    }

    public static String saveAsTemplate(final WorkflowManager wm, final ReferencedFile rawWorkflowDirRef,
        final ExecutionMonitor execMon, final WorkflowSaveHelper saveHelper)
        throws IOException, CanceledExecutionException, LockFailedException {
        var tI = wm.getTemplateInformation();
        if (!Role.Template.equals(tI.getRole())) {
            throw new IllegalStateException("Cannot save workflow as template (role " + tI.getRole() + ")");
        }
        // as per 2.10 template workflows are also saved under workflow.knime (previously it was all contained in
        // template.knime). The new template.knime file is written elsewhere.
        final var name = wm.getDirectNCParent().getCipherFileName(WORKFLOW_FILE);
        var preFilledSettings = new NodeSettings(name);
        saveContent(wm, preFilledSettings, rawWorkflowDirRef, execMon, saveHelper);
        return name;
    }

    /**
     * @param wm The WFM to save.
     * @param preFilledSettings The settings eventually written to workflow.knime (or workflow.knime.encrypted). For
     *            workflows it contains the version number, cipher, template infos etc. The name of the setting defines
     *            the output file name (so it's important!)
     * @param rawWorkflowDirRef To save to.
     * @param execMon ...
     * @param saveHelper ...
     * @throws IOException ...
     * @throws CanceledExecutionException ...
     * @throws LockFailedException ...
     */
    private static void saveContent(final WorkflowManager wm, final NodeSettings preFilledSettings,
        final ReferencedFile rawWorkflowDirRef, final ExecutionMonitor execMon, final WorkflowSaveHelper saveHelper)
        throws IOException, CanceledExecutionException, LockFailedException {
        var workflowDirRef = rawWorkflowDirRef;
        var r = wm.getTemplateInformation().getRole();
        final var fName = preFilledSettings.getKey();
        if (!workflowDirRef.fileLockRootForVM()) {
            throw new LockFailedException(
                "Can't write workflow to \"" + workflowDirRef + "\" because the directory can't be locked");
        }
        try {
            final var nodeContainerDirectory = wm.getNodeContainerDirectory();
            final var autoSaveDirectory = wm.getAutoSaveDirectory();
            if (!saveHelper.isAutoSave() && workflowDirRef.equals(nodeContainerDirectory)) {
                if (!nodeContainerDirectory.isDirty()) {
                    return;
                } else {
                    workflowDirRef = nodeContainerDirectory; // update variable assignment to do changes on member
                    // delete "old" node directories if not saving to the working
                    // directory -- do this before saving the nodes (dirs newly created)
                    WorkflowManager.deleteObsoleteNodeDirs(nodeContainerDirectory.getDeletedNodesFileLocations());
                }
            }
            if (saveHelper.isAutoSave() && workflowDirRef.equals(autoSaveDirectory)) {
                if (!autoSaveDirectory.isDirty()) {
                    return;
                } else {
                    workflowDirRef = autoSaveDirectory;
                    WorkflowManager.deleteObsoleteNodeDirs(autoSaveDirectory.getDeletedNodesFileLocations());
                }
            }
            var workflowDir = workflowDirRef.getFile();
            workflowDir.mkdirs();
            if (!workflowDir.isDirectory()) {
                throw new IOException("Unable to create or write directory \": " + workflowDir + "\"");
            }
            saveWorkflowName(preFilledSettings, wm.getNameField());
            saveAuthorInformation(wm.getAuthorInformation(), preFilledSettings);
            saveWorkflowCipher(preFilledSettings, wm.getWorkflowCipher());
            FileNodeContainerMetaPersistor.save(preFilledSettings, wm, workflowDirRef);
            saveWorkflowVariables(wm, preFilledSettings);
            saveCredentials(wm, preFilledSettings);
            saveTableBackend(wm, preFilledSettings);
            saveWorkflowAnnotations(wm, preFilledSettings);

            var nodesSettings = saveSettingsForNodes(preFilledSettings);
            var nodes = wm.getNodeContainers();
            var progRatio = 1.0 / (nodes.size() + 1);

            for (NodeContainer nextNode : nodes) {
                var id = nextNode.getID().getIndex();
                var subExec = execMon.createSubProgress(progRatio);
                execMon.setMessage(nextNode.getNameWithID());
                var sub = nodesSettings.addNodeSettings("node_" + id);
                NodeContext.pushContext(nextNode);
                try {
                    saveNodeContainer(sub, workflowDirRef, nextNode, subExec, saveHelper);
                } finally {
                    NodeContext.removeLastContext();
                }
                subExec.setProgress(1.0);
            }

            execMon.setMessage("connection information");
            var connSettings = saveSettingsForConnections(preFilledSettings);
            var connectionNumber = 0;
            for (ConnectionContainer cc : wm.getConnectionContainers()) {
                var nextConnectionConfig = connSettings.addNodeSettings("connection_" + connectionNumber);
                saveConnection(nextConnectionConfig, cc);
                connectionNumber += 1;
            }
            var inCount = wm.getNrInPorts();
            var inPortsSetts = inCount > 0 ? saveInPortsSetting(preFilledSettings) : null;
            NodeSettingsWO inPortsSettsEnum = null;
            if (inPortsSetts != null) {
                //TODO actually not neccessary to save the class name
                saveInportsBarUIInfoClassName(inPortsSetts, wm.getInPortsBarUIInfo());
                saveInportsBarUIInfoSettings(inPortsSetts, wm.getInPortsBarUIInfo());
                inPortsSettsEnum = saveInPortsEnumSetting(inPortsSetts);
            }
            for (var i = 0; i < inCount; i++) {
                var sPort = saveInPortSetting(inPortsSettsEnum, i);
                saveInPort(sPort, wm, i);
            }
            var outCount = wm.getNrOutPorts();
            var outPortsSetts = outCount > 0 ? saveOutPortsSetting(preFilledSettings) : null;
            NodeSettingsWO outPortsSettsEnum = null;
            if (outPortsSetts != null) {
                saveOutportsBarUIInfoClassName(outPortsSetts, wm.getOutPortsBarUIInfo());
                saveOutportsBarUIInfoSettings(outPortsSetts, wm.getOutPortsBarUIInfo());
                outPortsSettsEnum = saveOutPortsEnumSetting(outPortsSetts);
            }
            for (var i = 0; i < outCount; i++) {
                var singlePort = saveOutPortSetting(outPortsSettsEnum, i);
                saveOutPort(singlePort, wm, i);
            }
            saveEditorUIInformation(wm, preFilledSettings);

            var workflowFile = new File(workflowDir, fName);
            var toBeDeletedFileName = Role.Template.equals(r) ? TEMPLATE_FILE : WORKFLOW_FILE;
            new File(workflowDir, toBeDeletedFileName).delete();
            new File(workflowDir, WorkflowCipher.getCipherFileName(toBeDeletedFileName)).delete();

            OutputStream os = new FileOutputStream(workflowFile);
            os = wm.getDirectNCParent().cipherOutput(os);
            preFilledSettings.saveToXML(os);
            if (saveHelper.isSaveData()) {
                var saveWithDataFile = new File(workflowDir, SAVED_WITH_DATA_FILE);
                var o = new BufferedWriter(new FileWriter(saveWithDataFile));
                o.write("Do not delete this file!");
                o.newLine();
                o.write("This file serves to indicate that the workflow was written as part of the usual save "
                    + "routine (not exported).");
                o.newLine();
                o.newLine();
                o.write("Workflow was last saved by user ");
                o.write(System.getProperty("user.name"));
                o.write(" on " + new Date());
                o.close();
            }
            if (saveHelper.isAutoSave() && autoSaveDirectory == null) {
                wm.setAutoSaveDirectory(workflowDirRef);
            }
            if (!saveHelper.isAutoSave() && nodeContainerDirectory == null) {
                wm.setNodeContainerDirectory(workflowDirRef);
            }
            var wmState = wm.getNodeContainerState();
            // non remote executions
            var isExecutingLocally = wmState.isExecutionInProgress() && !wmState.isExecutingRemotely();
            if (workflowDirRef.equals(nodeContainerDirectory) && !isExecutingLocally) {
                wm.unsetDirty();
            }
            workflowDirRef.setDirty(isExecutingLocally);
            execMon.setProgress(1.0);
        } finally {
            workflowDirRef.fileUnlockRootForVM();
        }
    }

    /** Add version field. */
    static void saveHeader(final NodeSettings settings) {
        settings.addString(WorkflowLoadHelper.CFG_CREATED_BY, KNIMEConstants.VERSION);
        settings.addBoolean(WorkflowLoadHelper.CFG_NIGHTLY, KNIMEConstants.isNightlyBuild());
        settings.addString(WorkflowLoadHelper.CFG_VERSION, getSaveVersion().getVersionString());
    }

    /**
     * Saves the status of the wizard if set so in the save-helper.
     *
     * @param wm ...
     * @param preFilledSettings ...
     * @param saveHelper ...
     */
    private static void saveWizardState(final WorkflowManager wm, final NodeSettings preFilledSettings,
        final WorkflowSaveHelper saveHelper) {
        //don't save the wizard state if
        //(1) simply not desired
        //(2) the workflow is or is part of a metanode
        //(3) hasn't been started in wizard execution mode (i.e. not from the web portal)
        if (!saveHelper.isSaveWizardController() || !wm.isProject() || !wm.isInWizardExecution()) {
            return;
        }
        var wizardSettings = preFilledSettings.addNodeSettings("wizard");
        final var wizardController = wm.getWizardExecutionController();
        assert wizardController != null;
        wizardController.save(wizardSettings);
    }

    protected static void saveWorkflowName(final NodeSettingsWO settings, final String name) {
        settings.addString("name", name);
    }

    /**
     * Metanode locking information.
     *
     * @param settings
     * @param workflowCipher
     */
    protected static void saveWorkflowCipher(final NodeSettings settings, final WorkflowCipher workflowCipher) {
        if (!workflowCipher.isNullCipher()) {
            var cipherSettings = settings.addNodeSettings("cipher");
            workflowCipher.save(cipherSettings);
        }
    }

    /** @since 3.7 */
    protected static void saveAuthorInformation(final AuthorInformation aI, final NodeSettingsWO settings) {
        if (aI != null) {
            final var sub = settings.addNodeSettings(CFG_AUTHOR_INFORMATION);
            sub.addString("authored-by", aI.getAuthor());
            var authorWhen = aI.getAuthoredDate() == null ? null : formatDate(aI.getAuthoredDate());
            sub.addString("authored-when", authorWhen);
            sub.addString("lastEdited-by", aI.getLastEditor().orElse(null));
            var lastEditWhen = aI.getLastEditDate() == null ? null
                : aI.getLastEditDate().isPresent() ? formatDate(aI.getLastEditDate().get()) : null;
            sub.addString("lastEdited-when", lastEditWhen);
        }
    }

    /**
     * @param settings
     * @since 2.6
     */
    static void saveEditorUIInformation(final WorkflowManager wfm, final NodeSettings settings) {
        var editorInfo = wfm.getEditorUIInformation();
        if (editorInfo != null) {
            var editorConfig = settings.addNodeSettings(CFG_EDITOR_INFO);
            editorConfig.addBoolean(CFG_EDITOR_SNAP_GRID, editorInfo.getSnapToGrid());
            editorConfig.addBoolean(CFG_EDITOR_SHOW_GRID, editorInfo.getShowGrid());
            editorConfig.addInt(CFG_EDITOR_X_GRID, editorInfo.getGridX());
            editorConfig.addInt(CFG_EDITOR_Y_GRID, editorInfo.getGridY());
            editorConfig.addDouble(CFG_EDITOR_ZOOM, editorInfo.getZoomLevel());
            editorConfig.addBoolean(CFG_EDITOR_CURVED_CONNECTIONS, editorInfo.getHasCurvedConnections());
            editorConfig.addInt(CFG_EDITOR_CONNECTION_WIDTH, editorInfo.getConnectionLineWidth());
        }
    }

    protected static void saveWorkflowVariables(final WorkflowManager wfm, final NodeSettingsWO settings) {
        var vars = wfm.getWorkflowVariables();
        if (!vars.isEmpty()) {
            var wfmVarSub = settings.addNodeSettings(CFG_WKF_VARIABLES);
            var i = 0;
            for (FlowVariable v : vars) {
                v.save(wfmVarSub.addNodeSettings("Var_" + (i++)));
            }
        }
    }

    protected static void saveCredentials(final WorkflowManager wfm, final NodeSettingsWO settings) {
        var credentialsStore = wfm.getCredentialsStore();
        var sub = settings.addNodeSettings(CFG_CREDENTIALS);
        synchronized (credentialsStore) {
            for (Credentials c : credentialsStore.getCredentials()) {
                var s = sub.addNodeSettings(c.getName());
                c.save(s);
            }
        }
    }

    /**
     * Save the {@link TableBackend} set on workflow projects.
     *
     * @since 4.3
     */
    protected static void saveTableBackend(final WorkflowManager wfm, final NodeSettingsWO settings) {
        wfm.getTableBackendSettings().ifPresent(backendSettings -> backendSettings.saveSettingsTo(settings));
    }

    protected static void saveWorkflowAnnotations(final WorkflowManager manager, final NodeSettingsWO settings) {
        var annotations = manager.getWorkflowAnnotations();
        if (annotations.isEmpty()) {
            return;
        }
        var annoSettings = settings.addNodeSettings("annotations");
        var i = 0;
        for (Annotation a : annotations) {
            var t = annoSettings.addNodeSettings("annotation_" + i);
            a.save(t);
            i += 1;
        }
    }

    /**
     * Save nodes in an own sub-config object as a series of configs.
     *
     * @param settings To save to.
     * @return The sub config where subsequent writing takes place.
     */
    protected static NodeSettingsWO saveSettingsForNodes(final NodeSettingsWO settings) {
        return settings.addNodeSettings(KEY_NODES);
    }

    /**
     * Save connections in an own sub-config object.
     *
     * @param settings To save to.
     * @return The sub config where subsequent writing takes place.
     */
    protected static NodeSettingsWO saveSettingsForConnections(final NodeSettingsWO settings) {
        return settings.addNodeSettings(KEY_CONNECTIONS);
    }

    /**
     * Saves recursively to a directory
     *
     * @param settings
     * @param workflowDirRef
     * @param container
     * @param exec
     * @param saveHelper
     * @throws CanceledExecutionException
     * @throws IOException
     * @throws LockFailedException
     */
    protected static void saveNodeContainer(final NodeSettingsWO settings, final ReferencedFile workflowDirRef,
        final NodeContainer container, final ExecutionMonitor exec, final WorkflowSaveHelper saveHelper)
        throws CanceledExecutionException, IOException, LockFailedException {
        var parent = container.getParent();
        var workingDir = parent.getNodeContainerDirectory();
        var isWorkingDir = workflowDirRef.equals(workingDir);

        saveNodeIDSuffix(settings, container);
        var idSuffix = container.getID().getIndex();

        // name of sub-directory container node/sub-workflow settings
        // all chars which are not letter or number are replaced by '_'
        final var containerName = container.getName();
        var nodeDirID = FileUtil.getValidFileName(containerName,
            container instanceof WorkflowManager || container instanceof SubNodeContainer ? 12 : -1);
        nodeDirID = nodeDirID.concat(" (#" + idSuffix + ")");

        // try to re-use previous node dir (might be different from calculated
        // one above in case node was renamed between releases)
        if (isWorkingDir && container.getNodeContainerDirectory() != null) {
            var ncDirectory = container.getNodeContainerDirectory();
            nodeDirID = ncDirectory.getFile().getName();
        }

        var nodeDirectoryRef = new ReferencedFile(workflowDirRef, nodeDirID);
        String fileName;
        if (container instanceof WorkflowManager) {
            fileName = FileWorkflowLoader.save((WorkflowManager)container, nodeDirectoryRef, exec, saveHelper);
        } else {
            fileName = FileSingleNodeContainerPersistor.save((SingleNodeContainer)container, nodeDirectoryRef, exec,
                saveHelper);
        }
        saveFileLocation(settings, nodeDirID + "/" + fileName);
        saveNodeType(settings, container);

        //save node UI info
        saveNodeUIInformation(settings, container.getUIInformation());
    }

    /**
     * Helper to save a {@link NodeUIInformation} object.
     */
    private static void saveNodeUIInformation(final NodeSettingsWO settings, final NodeUIInformation nodeUIInfo) {
        //save UI info class name (TODO: for historical reasons, probably not needed anymore)
        settings.addString(CFG_UIINFO_CLASS, nodeUIInfo != null ? nodeUIInfo.getClass().getName() : null);
        //save UI info settings
        //nest into separate sub config
        if (nodeUIInfo != null) {
            var subConfig = settings.addNodeSettings(CFG_UIINFO_SUB_CONFIG);
            subConfig.addIntArray(KEY_BOUNDS, nodeUIInfo.getBounds());
        }
    }

    protected static void saveNodeIDSuffix(final NodeSettingsWO settings, final NodeContainer nc) {
        settings.addInt(KEY_ID, nc.getID().getIndex());
    }

    protected static void saveFileLocation(final NodeSettingsWO settings, final String location) {
        settings.addString("node_settings_file", location);
    }

    protected static void saveNodeType(final NodeSettingsWO settings, final NodeContainer nc) {
        // obsolote since LoadVersion.V2100 - written to help old knime installs to read new workflows
        // treat sub and metanodes the same
        settings.addBoolean("node_is_meta", !(nc instanceof NativeNodeContainer));
        NodeType nodeType;
        if (nc instanceof NativeNodeContainer) {
            nodeType = NodeType.NativeNode;
        } else if (nc instanceof WorkflowManager) {
            nodeType = NodeType.MetaNode;
        } else if (nc instanceof SubNodeContainer) {
            nodeType = NodeType.SubNode;
        } else {
            throw new IllegalArgumentException(
                "Unsupported node container class: " + nc == null ? "<null>" : nc.getClass().getName());
        }
        settings.addString("node_type", nodeType.name()); // added for 2.10Pre
    }

    protected static NodeSettingsWO saveInPortsSetting(final NodeSettingsWO settings) {
        return settings.addNodeSettings("meta_in_ports");
    }

    protected static NodeSettingsWO saveInPortsEnumSetting(final NodeSettingsWO settings) {
        return settings.addNodeSettings("port_enum");
    }

    protected static NodeSettingsWO saveInPortSetting(final NodeSettingsWO settings, final int portIndex) {
        return settings.addNodeSettings("inport_" + portIndex);
    }

    /**
     * @since 3.5
     */
    protected static void saveInportsBarUIInfoClassName(final NodeSettingsWO settings, final NodeUIInformation info) {
        settings.addString(CFG_UIINFO_CLASS, info != null ? info.getClass().getName() : null);
    }

    /**
     * @since 3.5
     */
    protected static void saveInportsBarUIInfoSettings(final NodeSettingsWO settings, final NodeUIInformation uiInfo) {
        saveNodeUIInformation(settings, uiInfo);
    }

    protected static void saveInPort(final NodeSettingsWO settings, final WorkflowManager wm, final int portIndex) {
        var inport = wm.getInPort(portIndex);
        settings.addInt("index", portIndex);
        settings.addString("name", inport.getPortName());
        var portTypeSettings = settings.addNodeSettings("type");
        inport.getPortType().save(portTypeSettings);
    }

    protected static NodeSettingsWO saveOutPortsSetting(final NodeSettingsWO settings) {
        return settings.addNodeSettings("meta_out_ports");
    }

    protected static NodeSettingsWO saveOutPortsEnumSetting(final NodeSettingsWO settings) {
        return settings.addNodeSettings("port_enum");
    }

    /**
     * @since 3.5
     */
    protected static void saveOutportsBarUIInfoClassName(final NodeSettingsWO settings, final NodeUIInformation info) {
        settings.addString(CFG_UIINFO_CLASS, info != null ? info.getClass().getName() : null);
    }

    /**
     * @since 3.5
     */
    protected static void saveOutportsBarUIInfoSettings(final NodeSettingsWO settings, final NodeUIInformation uiInfo) {
        saveNodeUIInformation(settings, uiInfo);
    }

    protected static NodeSettingsWO saveOutPortSetting(final NodeSettingsWO settings, final int portIndex) {
        return settings.addNodeSettings("outport_" + portIndex);
    }

    protected static void saveOutPort(final NodeSettingsWO settings, final WorkflowManager wm, final int portIndex) {
        var outport = wm.getOutPort(portIndex);
        settings.addInt("index", portIndex);
        settings.addString("name", outport.getPortName());
        var portTypeSettings = settings.addNodeSettings("type");
        outport.getPortType().save(portTypeSettings);
    }

    protected static void saveConnection(final NodeSettingsWO settings, final ConnectionContainer connection) {
        var sourceID = connection.getSource().getIndex();
        var destID = connection.getDest().getIndex();
        switch (connection.getType()) {
            case WFMIN:
                sourceID = -1;
                break;
            case WFMOUT:
                destID = -1;
                break;
            case WFMTHROUGH:
                sourceID = -1;
                destID = -1;
                break;
            default:
                // all handled above
        }
        settings.addInt("sourceID", sourceID);
        settings.addInt("destID", destID);
        var sourcePort = connection.getSourcePort();
        settings.addInt("sourcePort", sourcePort);
        var targetPort = connection.getDestPort();
        settings.addInt("destPort", targetPort);
        var uiInfo = connection.getUIInfo();
        if (uiInfo != null) {
            //TODO there is actually no need to store the class name - just keep it for now for backwards compatibility
            settings.addString(CFG_UIINFO_CLASS, uiInfo.getClass().getName());
            // nest into separate sub config
            var subConfig = settings.addNodeSettings(CFG_UIINFO_SUB_CONFIG);
            var allBendpoints = uiInfo.getAllBendpoints();
            subConfig.addInt(KEY_BENDPOINTS + "_size", allBendpoints.length);
            for (var i = 0; i < allBendpoints.length; i++) {
                subConfig.addIntArray(KEY_BENDPOINTS + "_" + i, allBendpoints[i]);
            }
        }
        if (!connection.isDeletable()) {
            settings.addBoolean("isDeletable", false);
        }
    }

    /**
     * @param editorUIInfo the editorUIInfo to set
     */
    void setEditorUIInfo(final EditorUIInformation editorUIInfo) {
        //TODO add to builder
    }

    /**
     * @param credentials the credentials to set
     */
    void setCredentials(final List<Credentials> credentials) {
        m_workflowDefBuilder.setWorkflowCredentials(CoreToDefUtil.toWorkflowCredentialsDef(credentials));
    }

    /**
     * @param wizardState the wizardState to set
     */
    void setWizardState(final NodeSettingsRO wizardState) {
        // TODO add to builder
//        m_wizardState = wizardState;
    }

    /**
     * @param workflowAnnotations the workflowAnnotations to set
     */
    void setWorkflowAnnotations(final List<WorkflowAnnotation> workflowAnnotations) {
        var annotations =
            workflowAnnotations.stream().map(CoreToDefUtil::toAnnotationDataDef).collect(Collectors.toList());
        m_workflowDefBuilder.setAnnotations(annotations);
    }

    /**
     * @param authorInformation the authorInformation to set
     */
    void setAuthorInformation(final AuthorInformation authorInformation) {
        m_workflowMetadataDefBuilder.setAuthorInformation(CoreToDefUtil.toAuthorInformationDef(authorInformation));
    }

    /**
     * @param workflowVariables the workflowVariables to set
     */
    void setWorkflowVariables(final List<FlowVariable> workflowVariables) {
        // TODO add to builder
    }

    /**
     * @param name the name to set
     */
    void setName(final String name) {
        // TODO is this the right place for the data
        m_workflowMetadataDefBuilder.setName(name);
    }

    /**
     * @param templateInformation the templateInformation to set
     */
    void setTemplateInformation(final MetaNodeTemplateInformation templateInformation) {
        //TODO add to builder
    }

    /**
     * @param inPortsBarUIInfo the inPortsBarUIInfo to set
     */
    void setInPortsBarUIInfo(final NodeUIInformation inPortsBarUIInfo) {
        m_metaNodeDefBuilder.setInPortsBarUIInfo(CoreToDefUtil.toNodeUIInfoDef(inPortsBarUIInfo));
    }

    /**
     * TODO
     * @param outPortsBarUIInfo the outPortsBarUIInfo to set
     */
    void setOutPortsBarUIInfo(final NodeUIInformation outPortsBarUIInfo) {
        m_metaNodeDefBuilder.setOutPortsBarUIInfo(CoreToDefUtil.toNodeUIInfoDef(outPortsBarUIInfo));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeDef getLoadResult(final NodeContainerLoader parentPersistor, final NodeSettingsRO parentSettings,
        final LoadResult loadResult) throws InvalidSettingsException, IOException {
        return m_workflowDefBuilder//

//                .setNodes(m_nodesToAdd)
                .build();
    }

}
