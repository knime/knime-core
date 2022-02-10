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
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Stack;
import java.util.TreeMap;

import org.knime.core.internal.ReferencedFile;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.Node;
import org.knime.core.node.NodeAndBundleInformationPersistor;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortType;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.workflow.MetaNodeTemplateInformation.Role;
import org.knime.core.node.workflow.WorkflowTableBackendSettings.TableBackendUnknownException;
import org.knime.core.node.workflow.execresult.WorkflowExecutionResult;
import org.knime.core.util.LoadVersion;
import org.knime.core.util.workflowalizer.AuthorInformation;

/**
 *
 * @author wiswedel, University of Konstanz
 */
@SuppressWarnings("javadoc")
public class FileWorkflowPersistor implements WorkflowPersistor, TemplateNodeContainerPersistor {

    /** KNIME Node type: native, meta or sub node.*/
    enum NodeType {
        NativeNode("node"),
        MetaNode("metanode"),
        SubNode("wrapped node");

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

    static final String CFG_UIINFO_SUB_CONFIG = "ui_settings";

    /** Key for UI info's class name. */
    static final String CFG_UIINFO_CLASS = "ui_classname";

    /** Key for workflow variables. */
    private static final String CFG_WKF_VARIABLES = "workflow_variables";

    /** key used to store the editor specific settings (since 2.6). */
    private static final String CFG_EDITOR_INFO = "workflow_editor_settings";


    private static final String CFG_AUTHOR_INFORMATION = "authorInformation";

    private static final String CFG_EDITOR_SNAP_GRID = "workflow.editor.snapToGrid";

    private static final String CFG_EDITOR_SHOW_GRID = "workflow.editor.ShowGrid";

    private static final String CFG_EDITOR_X_GRID = "workflow.editor.gridX";

    private static final String CFG_EDITOR_Y_GRID = "workflow.editor.gridY";

    private static final String CFG_EDITOR_ZOOM = "workflow.editor.zoomLevel";

    private static final String CFG_EDITOR_CURVED_CONNECTIONS = "workflow.editor.curvedConnections";

    private static final String CFG_EDITOR_CONNECTION_WIDTH = "workflow.editor.connectionWidth";

    /** The key under which the bounds to store the {@link ConnectionUIInformation} are registered. *
     * @since 3.5*/
    public static final String KEY_BENDPOINTS = "extrainfo.conn.bendpoints";

    /** The key under which the bounds are registered. * */
    static final String KEY_BOUNDS = "extrainfo.node.bounds";

    private static final PortType FALLBACK_PORTTYPE = PortObject.TYPE;

    private static final NodeSettingsRO EMPTY_SETTINGS = new NodeSettings("<<empty>>");

    /** The node logger for this class. */
    private final NodeLogger m_logger = NodeLogger.getLogger(getClass());

    private final LoadVersion m_versionString;

    private final TreeMap<Integer, FromFileNodeContainerPersistor> m_nodeContainerLoaderMap;

    private final HashSet<ConnectionContainerTemplate> m_connectionSet;

    private final FileNodeContainerMetaPersistor m_metaPersistor;

    private final WorkflowDataRepository m_workflowDataRepository;

    private WorkflowPortTemplate[] m_inPortTemplates;

    private WorkflowPortTemplate[] m_outPortTemplates;

    private NodeUIInformation m_inPortsBarUIInfo;

    private NodeUIInformation m_outPortsBarUIInfo;

    private EditorUIInformation m_editorUIInfo;

    /**
     * Parent persistor, used to create (nested) decryption stream for locked metanodes.
     */
    private WorkflowPersistor m_parentPersistor;

    private String m_name;

    private WorkflowCipher m_workflowCipher;

    private MetaNodeTemplateInformation m_templateInformation;

    private AuthorInformation m_authorInformation;

    /** see {@link #setNameOverwrite(String)}. */
    private String m_nameOverwrite;

    private List<FlowVariable> m_workflowVariables;

    private WorkflowTableBackendSettings m_tableBackendSettings;

    private List<WorkflowAnnotation> m_workflowAnnotations;

    private NodeSettingsRO m_wizardState;

    private boolean m_needsResetAfterLoad;

    private boolean m_isDirtyAfterLoad;

    private boolean m_mustWarnOnDataLoadError;

    private final boolean m_isProject;

    private final boolean m_isComponentProject;

    private NodeSettingsRO m_workflowSett;

    private final List<ReferencedFile> m_obsoleteNodeDirectories;

    /** Parse the version string, return {@link LoadVersion#FUTURE} if it can't be parsed. */
    // TODO was moved to RootWorkflowLoader
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
     * Create persistor for load.
     * @param dotKNIMEFile Associated workflow.knime or template.knime file
     * @param loadHelper The load helper as required by meta persistor.
     * @param version of loading workflow.
     */
    FileWorkflowPersistor(final WorkflowDataRepository workflowDataRepository,
        final ReferencedFile dotKNIMEFile, final WorkflowLoadHelper loadHelper,
        final LoadVersion version, final boolean isProject) {
        assert version != null;
        m_workflowDataRepository = workflowDataRepository;
        m_versionString = version;
        m_metaPersistor = new FileNodeContainerMetaPersistor(dotKNIMEFile, loadHelper, version);
        m_nodeContainerLoaderMap = new TreeMap<Integer, FromFileNodeContainerPersistor>();
        m_connectionSet = new HashSet<ConnectionContainerTemplate>();
        m_obsoleteNodeDirectories = new ArrayList<ReferencedFile>();
        m_isProject = isProject;
        m_isComponentProject = loadHelper.isTemplateProject();
    }

    /** {@inheritDoc} */
    @Override
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
        FileNodeContainerMetaPersistor meta = getMetaPersistor();
        if (meta == null) {
            throw new RuntimeException("Persistor not created for loading workflow, meta persistor is null");
        }
        return meta.getNodeSettingsFile();
    }

    /** {@inheritDoc} */
    @Override
    public Set<ConnectionContainerTemplate> getConnectionSet() {
        return m_connectionSet;
    }

    /** {@inheritDoc} */
    @Override
    public Set<ConnectionContainerTemplate> getAdditionalConnectionSet() {
        return Collections.emptySet();
    }

    /** {@inheritDoc} */
    @Override
    public Map<Integer, ? extends NodeContainerPersistor> getNodeLoaderMap() {
        return m_nodeContainerLoaderMap;
    }

    /** {@inheritDoc} */
    @Override
    public boolean mustWarnOnDataLoadError() {
        return m_mustWarnOnDataLoadError;
    }

    /** {@inheritDoc} */
    @Override
    public FileNodeContainerMetaPersistor getMetaPersistor() {
        return m_metaPersistor;
    }

    /**
     * {@inheritDoc}
     *
     * @since 2.6
     */
    @Override
    public WorkflowDataRepository getWorkflowDataRepository() {
        return m_workflowDataRepository;
    }

    /** {@inheritDoc} */
    @Override
    public NodeContainer getNodeContainer(final WorkflowManager parent, final NodeID id) {
        return parent.createSubWorkflow(this, id);
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return m_name;
    }

    /** {@inheritDoc} */
    @Override
    public WorkflowCipher getWorkflowCipher() {
        return m_workflowCipher;
    }

    /** {@inheritDoc} */
    @Override
    public void setOverwriteTemplateInformation(final MetaNodeTemplateInformation templateInfo) {
        m_templateInformation = templateInfo;
    }

    /** {@inheritDoc} */
    @Override
    public void setNameOverwrite(final String nameOverwrite) {
        m_nameOverwrite = nameOverwrite;
    }

    /** {@inheritDoc} */
    @Override
    public MetaNodeTemplateInformation getTemplateInformation() {
        return m_templateInformation;
    }

    /** {@inheritDoc}
     * @since 2.8*/
    @Override
    public AuthorInformation getAuthorInformation() {
        return m_authorInformation;
    }

    /**
     * {@inheritDoc}
     *
     * @throws IOException
     */
    @Override
    public InputStream decipherInput(final InputStream input) throws IOException {
        InputStream myInput = m_workflowCipher.decipherInput(input);
        if (m_parentPersistor != null) {
            return m_parentPersistor.decipherInput(myInput);
        }
        // top most persistor, i.e. for workflow project
        return myInput;
    }

    /** {@inheritDoc} */
    @Override
    public List<FlowVariable> getWorkflowVariables() {
        return m_workflowVariables;
    }

    @Override
    public WorkflowTableBackendSettings getWorkflowTableBackendSettings() {
        return m_tableBackendSettings;
    }

    /** {@inheritDoc}
     * @since 2.8
     */
    @Override
    public WorkflowContext getWorkflowContext() {
        return isProject() || m_isComponentProject ? getMetaPersistor().getLoadHelper().getWorkflowContext() : null;
    }

    /** {@inheritDoc} */
    @Override
    public List<WorkflowAnnotation> getWorkflowAnnotations() {
        return m_workflowAnnotations;
    }

    /** {@inheritDoc} */
    @Override
    public NodeSettingsRO getWizardExecutionControllerState() {
        return m_wizardState;
    }

    /** {@inheritDoc} */
    @Override
    public List<ReferencedFile> getObsoleteNodeDirectories() {
        return m_obsoleteNodeDirectories;
    }

    /** {@inheritDoc} */
    @Override
    public WorkflowPortTemplate[] getInPortTemplates() {
        return m_inPortTemplates;
    }

    /** {@inheritDoc} */
    @Override
    public WorkflowPortTemplate[] getOutPortTemplates() {
        return m_outPortTemplates;
    }

    /** {@inheritDoc}
     * @since 3.5*/
    @Override
    public NodeUIInformation getInPortsBarUIInfo() {
        return m_inPortsBarUIInfo;
    }

    /** {@inheritDoc}
     * @since 3.5*/
    @Override
    public NodeUIInformation getOutPortsBarUIInfo() {
        return m_outPortsBarUIInfo;
    }

    /**
     * {@inheritDoc}
     *
     * @since 2.6
     */
    @Override
    public EditorUIInformation getEditorUIInformation() {
        return m_editorUIInfo;
    }

    /**
     * {@inheritDoc}
     *
     * @since 2.6
     */
    @Override
    public boolean isProject() {
        return m_isProject;
    }

    /** {@inheritDoc} */
    @Override
    public boolean needsResetAfterLoad() {
        return m_needsResetAfterLoad;
    }

    /**
     * Indicate that this node should better be reset after load. (Due to loading problems).
     */
    public void setNeedsResetAfterLoad() {
        m_needsResetAfterLoad = true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isDirtyAfterLoad() {
        return m_isDirtyAfterLoad;
    }

    /** {@inheritDoc} */
    @Override
    public boolean mustComplainIfStateDoesNotMatch() {
        return !getLoadVersion().isOlderThan(LoadVersion.V200);
    }

    /** {@inheritDoc} */
    @Override
    public void setDirtyAfterLoad() {
        m_isDirtyAfterLoad = true;
    }

    /** {@inheritDoc} */
    @Override
    public void preLoadNodeContainer(final WorkflowPersistor parentPersistor, final NodeSettingsRO parentSettings,
        final LoadResult loadResult) throws InvalidSettingsException, IOException {

        m_parentPersistor = parentPersistor;

        /**
         * Data Provider
         *
         * extract information from workflow.knime file
         */
        final ReferencedFile knimeFile = getWorkflowKNIMEFile();

        /**
         * Error handling
         */
        if (knimeFile == null || !knimeFile.getFile().isFile()) {
            setDirtyAfterLoad();
            String error = "Can't read workflow file \"" + knimeFile + "\"";
            throw new IOException(error);
        }

        // workflow.knime (or template.knime)
        File nodeFile = knimeFile.getFile();
        ReferencedFile parentRef = knimeFile.getParent();

        /**
         * Error handling
         */
        if (parentRef == null) {
            setDirtyAfterLoad();
            throw new IOException("Parent directory of file \"" + knimeFile + "\" is not represented by "
                + ReferencedFile.class.getSimpleName() + " object");
        }

        /**
         * Data Provider
         */
        m_mustWarnOnDataLoadError = loadIfMustWarnOnDataLoadError(parentRef.getFile());

        /**
         * Data Loading
         */
        NodeSettingsRO subWFSettings;
        try {
            InputStream in = new FileInputStream(nodeFile);
            /**
             * Security
             * TODO should be different persistors for component, metanode, workflow
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
            setDirtyAfterLoad();
            throw ioe;
        }
        m_workflowSett = subWFSettings;

        /**
         * Manipulation
         */
        try {
            if (m_nameOverwrite != null) {
                m_name = m_nameOverwrite;
            } else {
                m_name = loadWorkflowName(m_workflowSett);
            }
        } catch (InvalidSettingsException e) {
            String error = "Unable to load workflow name: " + e.getMessage();
            getLogger().debug(error, e);
            setDirtyAfterLoad();
            loadResult.addError(error);
            m_name = null;
        }

        try {
            /**
             * Data loading
             * Conversion
             * Version management
             * Security
             */
            m_workflowCipher = loadWorkflowCipher(getLoadVersion(), m_workflowSett);
        } catch (InvalidSettingsException e) {
            String error = "Unable to load workflow cipher: " + e.getMessage();
            getLogger().debug(error, e);
            setDirtyAfterLoad();
            loadResult.addError(error);
            m_workflowCipher = WorkflowCipher.NULL_CIPHER;
        }

        try {
            if (m_templateInformation != null) {
                // template information was set after construction (this node is a link created from a template)
                /**
                 * Sanity checking
                 */
                assert m_templateInformation.getRole() == Role.Link;
            } else {
                /**
                 * Data Loading
                 */
                m_templateInformation = MetaNodeTemplateInformation.load(m_workflowSett, getLoadVersion());
                /**
                 * Sanity checking: fail if not set
                 */
                CheckUtils.checkSettingNotNull(m_templateInformation, "No template information");
            }
        } catch (InvalidSettingsException e) {
            String error = "Unable to load workflow template information: " + e.getMessage();
            getLogger().debug(error, e);
            setDirtyAfterLoad();
            loadResult.addError(error);
            m_templateInformation = MetaNodeTemplateInformation.NONE;
        }

        try {
            m_authorInformation = loadAuthorInformation(m_workflowSett, loadResult);
        } catch (InvalidSettingsException e) {
            String error = "Unable to load workflow author information: " + e.getMessage();
            getLogger().debug(error, e);
            setDirtyAfterLoad();
            loadResult.addError(error);
            m_authorInformation = AuthorInformation.UNKNOWN;
        }
        try {
            m_workflowVariables = loadWorkflowVariables(m_workflowSett);
        } catch (InvalidSettingsException e) {
            String error = "Unable to load workflow variables: " + e.getMessage();
            getLogger().debug(error, e);
            setDirtyAfterLoad();
            loadResult.addError(error);
            m_workflowVariables = Collections.emptyList();
        }



        try {
            m_tableBackendSettings = loadTableBackendSettings(m_workflowSett);
        } catch (InvalidSettingsException e) {
            String error = "Unable to table backend: " + e.getMessage();
            getLogger().debug(error, e);
            setNeedsResetAfterLoad();
            setDirtyAfterLoad();
            loadResult.addError(error, true);
            if (e instanceof TableBackendUnknownException) { // NOSONAR
                loadResult.addMissingTableFormat(((TableBackendUnknownException)e).getFormatInfo());
            }
            m_tableBackendSettings = isProject() ? new WorkflowTableBackendSettings() : null;
        }

        try {
            m_workflowAnnotations = loadWorkflowAnnotations(m_workflowSett);
        } catch (InvalidSettingsException e) {
            String error = "Unable to load workflow annotations: " + e.getMessage();
            getLogger().debug(error, e);
            setDirtyAfterLoad();
            loadResult.addError(error);
            m_workflowAnnotations = Collections.emptyList();
        }

        try {
            m_wizardState = loadWizardState(m_workflowSett);
        } catch (InvalidSettingsException e) {
            String error = "Unable to load wizard state: " + e.getMessage();
            getLogger().debug(error, e);
            setDirtyAfterLoad();
            loadResult.addError(error);
            m_wizardState = null;
        }

        NodeSettingsRO metaFlowParentSettings = new NodeSettings("fake_parent_settings");
        try {
            metaFlowParentSettings = readParentSettings();
        } catch (IOException e1) {
            String error = "Errors reading settings file: " + e1.getMessage();
            getLogger().warn(error, e1);
            setDirtyAfterLoad();
            loadResult.addError(error);
        }

        /**
         * Data loading
         * Detect errors and signal them
         */
        boolean isResetRequired = m_metaPersistor.load(subWFSettings, metaFlowParentSettings, loadResult);
        // TODO idea: if reset required use a different implementation of the template step

        if (isResetRequired) {
            setNeedsResetAfterLoad();
        }
        if (m_metaPersistor.isDirtyAfterLoad()) {
            setDirtyAfterLoad();
        }

        /* read in and outports */
        NodeSettingsRO inPortsEnum = EMPTY_SETTINGS;
        try {
            NodeSettingsRO inPorts = loadInPortsSetting(m_workflowSett);
            if (inPorts != null) {
                inPortsEnum = loadInPortsSettingsEnum(inPorts);
            }
        } catch (InvalidSettingsException e) {
            String error = "Can't load workflow ports, config not found";
            getLogger().debug(error, e);
            setDirtyAfterLoad();
            loadResult.addError(error);
            setNeedsResetAfterLoad();
        }
        int inPortCount = inPortsEnum.keySet().size();
        m_inPortTemplates = new WorkflowPortTemplate[inPortCount];
        for (String key : inPortsEnum.keySet()) {
            WorkflowPortTemplate p;
            try {
                NodeSettingsRO sub = inPortsEnum.getNodeSettings(key);
                p = loadInPortTemplate(sub);
            } catch (InvalidSettingsException e) {
                String error =
                    "Can't load workflow inport (internal ID \"" + key + "\", skipping it: " + e.getMessage();
                getLogger().debug(error, e);
                setDirtyAfterLoad();
                loadResult.addError(error);
                setNeedsResetAfterLoad();
                continue;
            }
            int index = p.getPortIndex();
            if (index < 0 || index >= inPortCount) {
                setDirtyAfterLoad();
                loadResult.addError("Invalid inport index " + index);
                setNeedsResetAfterLoad();
                continue;
            }
            if (m_inPortTemplates[index] != null) {
                setDirtyAfterLoad();
                loadResult.addError("Duplicate inport definition for index: " + index);
            }
            m_inPortTemplates[index] = p;
        }
        for (int i = 0; i < m_inPortTemplates.length; i++) {
            if (m_inPortTemplates[i] == null) {
                setDirtyAfterLoad();
                loadResult.addError("Assigning fallback port type for " + "missing input port " + i);
                m_inPortTemplates[i] = new WorkflowPortTemplate(i, FALLBACK_PORTTYPE);
            }
        }

        NodeSettingsRO outPortsEnum = EMPTY_SETTINGS;
        try {
            NodeSettingsRO outPorts = loadOutPortsSetting(m_workflowSett);
            if (outPorts != null) {
                outPortsEnum = loadOutPortsSettingsEnum(outPorts);
            }
        } catch (InvalidSettingsException e) {
            String error = "Can't load workflow out ports, config not found: " + e.getMessage();
            getLogger().debug(error, e);
            setDirtyAfterLoad();
            loadResult.addError(error);
        }
        int outPortCount = outPortsEnum.keySet().size();
        m_outPortTemplates = new WorkflowPortTemplate[outPortCount];
        for (String key : outPortsEnum.keySet()) {
            WorkflowPortTemplate p;
            try {
                NodeSettingsRO sub = outPortsEnum.getNodeSettings(key);
                p = loadOutPortTemplate(sub);
            } catch (InvalidSettingsException e) {
                String error =
                    "Can't load workflow outport (internal ID \"" + key + "\", skipping it: " + e.getMessage();
                getLogger().debug(error, e);
                setDirtyAfterLoad();
                loadResult.addError(error);
                setNeedsResetAfterLoad();
                continue;
            }
            int index = p.getPortIndex();
            if (index < 0 || index >= outPortCount) {
                setDirtyAfterLoad();
                loadResult.addError("Invalid inport index " + index);
                setNeedsResetAfterLoad();
                continue;
            }
            if (m_outPortTemplates[index] != null) {
                setDirtyAfterLoad();
                loadResult.addError("Duplicate outport definition for index: " + index);
            }
            m_outPortTemplates[index] = p;
        }
        for (int i = 0; i < m_outPortTemplates.length; i++) {
            if (m_outPortTemplates[i] == null) {
                setDirtyAfterLoad();
                loadResult.addError("Assigning fallback port type for " + "missing output port " + i);
                m_outPortTemplates[i] = new WorkflowPortTemplate(i, FALLBACK_PORTTYPE);
            }
        }

        boolean hasPorts = inPortCount > 0 || outPortCount > 0;
        if (hasPorts && m_isProject) {
            throw new InvalidSettingsException(String.format("Workflow \"%s\""
                + " is not a project as it has ports (%d in, %d out)", nodeFile.getAbsoluteFile(), inPortCount,
                outPortCount));
        }

        NodeSettingsRO inPorts = EMPTY_SETTINGS;
        NodeUIInformation inPortsBarUIInfo = null;
        String uiInfoClassName = null;
        try {
            inPorts = loadInPortsSetting(m_workflowSett);
            if (inPorts != null) {
                uiInfoClassName = loadInPortsBarUIInfoClassName(inPorts);
            }
        } catch (InvalidSettingsException e) {
            String error = "Unable to load class name for inport bar's " + "UI information: " + e.getMessage();
            getLogger().debug(error, e);
            setDirtyAfterLoad();
            loadResult.addError(error);
        }
        if (uiInfoClassName != null) {
            try {
                if (!getLoadVersion().isOlderThan(LoadVersion.V200)) {
                    inPortsBarUIInfo = loadNodeUIInformation(inPorts);
                }
            } catch (InvalidSettingsException e) {
                String error = "Unable to load inport bar's UI information: " + e.getMessage();
                getLogger().debug(error, e);
                setDirtyAfterLoad();
                loadResult.addError(error);
                inPortsBarUIInfo = null;
            }
        }

        /**
         * Input ports bar user interface information
         */
        NodeSettingsRO outPorts = null;
        m_inPortsBarUIInfo = inPortsBarUIInfo;
        NodeUIInformation outPortsBarUIInfo = null;
        uiInfoClassName = null;
        try {
            //TODO probably not necessary anymore to store the ui information class name (it's node ui information anyway)
            outPorts = loadOutPortsSetting(m_workflowSett);
            if (outPorts != null) {
                uiInfoClassName = loadOutPortsBarUIInfoClassName(outPorts);
            }
        } catch (InvalidSettingsException e) {
            String error =
                "Unable to load class name for outport bar's UI information" + ", no UI information available: "
                    + e.getMessage();
            setDirtyAfterLoad();
            getLogger().debug(error, e);
            loadResult.addError(error);
        }

        /**
         * Output ports bar user interface information
         */
        if (uiInfoClassName != null) {
            try {
                if (!getLoadVersion().isOlderThan(LoadVersion.V200)) {
                    outPortsBarUIInfo = loadNodeUIInformation(outPorts);
                }
            } catch (InvalidSettingsException e) {
                String error = "Unable to load outport bar's UI information: " + e.getMessage();
                getLogger().debug(error, e);
                setDirtyAfterLoad();
                loadResult.addError(error);
                outPortsBarUIInfo = null;
            }
        }
        m_outPortsBarUIInfo = outPortsBarUIInfo;

        try {
            m_editorUIInfo = loadEditorUIInformation(m_workflowSett);
        } catch (InvalidSettingsException e) {
            String error = "Unable to load editor UI information: " + e.getMessage();
            getLogger().debug(error, e);
            setDirtyAfterLoad();
            loadResult.addError(error);
            m_editorUIInfo = null;
        }
    }

    /** {@inheritDoc} */
    @Override
    public void loadNodeContainer(final Map<Integer, BufferedDataTable> tblRep, final ExecutionMonitor exec,
        final LoadResult loadResult) throws CanceledExecutionException, IOException {
        ReferencedFile workflowKNIMEFile = getWorkflowKNIMEFile();
        if (workflowKNIMEFile == null || m_workflowSett == null) {
            setDirtyAfterLoad();
            throw new IllegalStateException("The method preLoadNodeContainer has either not been called or failed");
        }
        /* read nodes */
        NodeSettingsRO nodes;
        try {
            nodes = loadSettingsForNodes(m_workflowSett);
        } catch (InvalidSettingsException e) {
            String error = "Can't load nodes in workflow, config not found: " + e.getMessage();
            getLogger().debug(error, e);
            loadResult.addError(error);
            setDirtyAfterLoad();
            setNeedsResetAfterLoad();
            // stop loading here
            return;
        }
        // ids of nodes that failed to load. Used to suppress superfluous errors when reading the connections
        Set<Integer> failingNodeIDSet = new HashSet<Integer>();
        // ids of nodes whose factory can't be loaded (e.g. node extension not installed)
        Map<Integer, NodeFactoryUnknownException> missingNodeIDMap =
            new HashMap<Integer, NodeFactoryUnknownException>();
        exec.setMessage("node information");
        final ReferencedFile workflowDirRef = workflowKNIMEFile.getParent();
        /* Load nodes */
        for (String nodeKey : nodes.keySet()) {
            exec.checkCanceled();
            NodeSettingsRO nodeSetting;
            try {
                nodeSetting = nodes.getNodeSettings(nodeKey);
            } catch (InvalidSettingsException e) {
                String error =
                    "Unable to load settings for node with internal " + "id \"" + nodeKey + "\": " + e.getMessage();
                getLogger().debug(error, e);
                setDirtyAfterLoad();
                loadResult.addError(error);
                continue;
            }
            if (shouldSkipThisNode(nodeSetting)) {
                continue;
            }
            int nodeIDSuffix;
            try {
                nodeIDSuffix = loadNodeIDSuffix(nodeSetting);
            } catch (InvalidSettingsException e) {
                nodeIDSuffix = getRandomNodeID();
                String error =
                    "Unable to load node ID (internal id \"" + nodeKey + "\"), trying random number " + nodeIDSuffix
                        + "instead: " + e.getMessage();
                getLogger().debug(error, e);
                setDirtyAfterLoad();
                loadResult.addError(error);
            }
            NodeType nodeType;
            try {
                nodeType = loadNodeType(nodeSetting);
            } catch (InvalidSettingsException e) {
                String error =
                    "Can't retrieve node type for contained node with id suffix " + nodeIDSuffix
                        + ", attempting to read ordinary (native) node: " + e.getMessage();
                getLogger().debug(error, e);
                setDirtyAfterLoad();
                loadResult.addError(error);
                nodeType = NodeType.NativeNode;
            }
            NodeUIInformation nodeUIInfo = null;
            String uiInfoClassName;
            try {
                uiInfoClassName = loadUIInfoClassName(nodeSetting);
            } catch (InvalidSettingsException e) {
                String error =
                    "Unable to load UI information class name " + "to node with ID suffix " + nodeIDSuffix
                        + ", no UI information available: " + e.getMessage();
                getLogger().debug(error, e);
                setDirtyAfterLoad();
                loadResult.addError(error);
                uiInfoClassName = null;
            }
            if (uiInfoClassName != null) {
                try {
                    //load node ui info
                    nodeUIInfo = loadNodeUIInformation(nodeSetting);
                } catch (InvalidSettingsException e) {
                    String error = "Unable to load UI information to " + "node with ID suffix " + nodeIDSuffix
                        + ", no UI information available: " + e.getMessage();
                    getLogger().debug(error, e);
                    setDirtyAfterLoad();
                    loadResult.addError(error);
                }
            }

            ReferencedFile nodeFile;
            try {
                nodeFile = loadNodeFile(nodeSetting, workflowDirRef);
            } catch (InvalidSettingsException e) {
                String error =
                    "Unable to load settings for node " + "with ID suffix " + nodeIDSuffix + ": " + e.getMessage();
                getLogger().debug(error, e);
                setDirtyAfterLoad();
                loadResult.addError(error);
                failingNodeIDSet.add(nodeIDSuffix);
                continue;
            }
            FromFileNodeContainerPersistor persistor;
            switch (nodeType) {
                case MetaNode:
                    persistor = createWorkflowPersistorLoad(nodeFile); break;
                case NativeNode:
                    persistor = createNativeNodeContainerPersistorLoad(nodeFile); break;
                case SubNode:
                    persistor = createSubNodeContainerPersistorLoad(nodeFile); break;
                default:
                    throw new IllegalStateException("Unknown node type: " + nodeType);
            }
            try {
                LoadResult childResult = new LoadResult(nodeType.toString() + " with ID suffix " + nodeIDSuffix);

                /**
                 * Recurse
                 */
                persistor.preLoadNodeContainer(this, nodeSetting, childResult);

                loadResult.addChildError(childResult);
            } catch (Throwable e) {
                String error =
                    "Unable to load node with ID suffix " + nodeIDSuffix + " into workflow, skipping it: "
                        + e.getMessage();
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
                    setDirtyAfterLoad();
                    failingNodeIDSet.add(nodeIDSuffix);
                    // node directory is the parent of the settings.xml
                    m_obsoleteNodeDirectories.add(nodeFile.getParent());
                    continue;
                }
            }
            NodeContainerMetaPersistor meta = persistor.getMetaPersistor();
            if (m_nodeContainerLoaderMap.containsKey(nodeIDSuffix)) {
                int randomID = getRandomNodeID();
                setDirtyAfterLoad();
                loadResult.addError("Duplicate id encountered in workflow: " + nodeIDSuffix
                    + ", uniquifying to random id " + randomID + ", this possibly screws the connections");
                nodeIDSuffix = randomID;
            }
            meta.setNodeIDSuffix(nodeIDSuffix);
            meta.setUIInfo(nodeUIInfo);
            if (persistor.isDirtyAfterLoad()) {
                setDirtyAfterLoad();
            }
            m_nodeContainerLoaderMap.put(nodeIDSuffix, persistor);
        }

        /* read connections */
        exec.setMessage("connection information");
        NodeSettingsRO connections;
        try {
            connections = loadSettingsForConnections(m_workflowSett);
            if (connections == null) {
                connections = EMPTY_SETTINGS;
            }
        } catch (InvalidSettingsException e) {
            String error = "Can't load workflow connections, config not found: " + e.getMessage();
            getLogger().debug(error, e);
            setDirtyAfterLoad();
            loadResult.addError(error);
            connections = EMPTY_SETTINGS;
        }
        for (String connectionKey : connections.keySet()) {
            exec.checkCanceled();
            ConnectionContainerTemplate c;
            try {
                c = loadConnection(connections.getNodeSettings(connectionKey));
            } catch (InvalidSettingsException e) {
                String error = "Can't load connection with internal ID \"" + connectionKey + "\": " + e.getMessage();
                getLogger().debug(error, e);
                setDirtyAfterLoad();
                loadResult.addError(error);
                continue;
            }
            int sourceIDSuffix = c.getSourceSuffix();
            NodeContainerPersistor sourceNodePersistor = m_nodeContainerLoaderMap.get(sourceIDSuffix);
            if (sourceNodePersistor == null && sourceIDSuffix != -1) {
                setDirtyAfterLoad();
                if (!failingNodeIDSet.contains(sourceIDSuffix)) {
                    loadResult.addError("Unable to load node connection " + c + ", source node does not exist");
                }
                continue;
            }
            fixSourcePortIfNecessary(sourceNodePersistor, c, getLoadVersion());

            int destIDSuffix = c.getDestSuffix();
            NodeContainerPersistor destNodePersistor = m_nodeContainerLoaderMap.get(destIDSuffix);
            if (destNodePersistor == null && destIDSuffix != -1) {
                setDirtyAfterLoad();
                if (!failingNodeIDSet.contains(destIDSuffix)) {
                    loadResult.addError("Unable to load node connection " + c + ", destination node does not exist");
                }
                continue;
            }
            fixDestPortIfNecessary(destNodePersistor, c, getLoadVersion());

            if (!m_connectionSet.add(c)) {
                setDirtyAfterLoad();
                loadResult.addError("Duplicate connection information: " + c);
            }
        }

        handleMissingNodes(exec, loadResult, missingNodeIDMap);

        exec.setProgress(1.0);
    }

    private NodeUIInformation loadNodeUIInformation(final NodeSettingsRO nodeSetting) throws InvalidSettingsException {
        // in previous releases, the settings were directly written to the
        // top-most node settings object; since 2.0 they are put into a
        // separate sub-settings object
        NodeSettingsRO subSettings = getLoadVersion().isOlderThan(LoadVersion.V200) ? nodeSetting
            : nodeSetting.getNodeSettings(CFG_UIINFO_SUB_CONFIG);
        final int loadOrdinal = getLoadVersion().ordinal();
        int[] bounds = subSettings.getIntArray(KEY_BOUNDS);
        boolean symbolRelative = loadOrdinal >= LoadVersion.V230.ordinal();
        NodeUIInformation nodeUIInfo = NodeUIInformation.builder()
            .setNodeLocation(bounds[0], bounds[1], bounds[2], bounds[3])
            .setIsSymbolRelative(symbolRelative).build();
        return nodeUIInfo;
    }

    /** Fills the list with null so that list.get(index) doesn't throw an exception. */
    private static void ensureArrayListIndexValid(final ArrayList<?> list, final int index) {
        for (int i = list.size(); i <= index; i++) {
            list.add(null);
        }
    }

    /**
     * Fixes source port index if necessary. Fixes the mandatory flow variable port object.
     *
     * TODO move somewhere else
     *
     * @param sourcePersistor The persistor of the source node.
     * @param c The connection template to be fixed.
     * @param loadVersion TODO
     *
     */
    static void fixSourcePortIfNecessary(final NodeContainerPersistor sourcePersistor,
        final ConnectionContainerTemplate c, final LoadVersion loadVersion) {
        // v2.1 and before did not have flow variable ports (index 0)
        if (loadVersion.isOlderThan(LoadVersion.V220) && sourcePersistor instanceof FileSingleNodeContainerPersistor) {
            // correct port index only for ordinary nodes (no new flow
            // variable ports on metanodes)
            int index = c.getSourcePort();
            c.setSourcePort(index + 1);
        }
    }

    /**
     * Fixes destination port index if necessary. For v1.x flows, e.g., the indices of model and data ports were
     * swapped. Subclasses will overwrite this method (e.g. to enable loading flows, which did not have the mandatory
     * flow variable port object).
     *
     * TODO move somewhere else
     *
     * @param destPersistor The persistor of the destination node.
     * @param c The connection template to be fixed.
     * @param loadVersion TODO
     */
    static void fixDestPortIfNecessary(final NodeContainerPersistor destPersistor, final ConnectionContainerTemplate c,
        final LoadVersion loadVersion) {
        if (loadVersion.isOlderThan(LoadVersion.V220)) {
            if (destPersistor instanceof FileNativeNodeContainerPersistor) {
                FileNativeNodeContainerPersistor pers = (FileNativeNodeContainerPersistor)destPersistor;
                /* workflows saved with 1.x.x have misleading port indices for
                 * incoming ports. Data ports precede the model ports (in their
                 * index), although the GUI and the true ordering is the other
                 * way around. */
                // only test if the node is not missing (missing node placeholder)
                if (pers.getNode() != null && pers.shouldFixModelPortOrder()) {
                    Node node = pers.getNode();
                    int modelPortCount = 0;
                    // first port is flow variable input port
                    for (int i = 1; i < node.getNrInPorts(); i++) {
                        PortType portType = node.getInputType(i);
                        if (!portType.getPortObjectClass().isAssignableFrom(BufferedDataTable.class)
                        // with v2.4 we added model ports to the
                        // PMML learner nodes that don't count as model
                        // ports in legacy workflows (not connected anyway)
                            && !portType.isOptional()) {
                            modelPortCount += 1;
                        }
                    }
                    if (modelPortCount == node.getNrInPorts()) {
                        return;
                    }
                    int destPort = c.getDestPort();
                    if (destPort < modelPortCount) { // c represent data connection
                        c.setDestPort(destPort + modelPortCount);
                    } else { // c represents model connection
                        c.setDestPort(destPort - modelPortCount);
                    }
                }
                // correct port index only for ordinary nodes (no new flow
                // variable ports on metanodes)
                int index = c.getDestPort();
                c.setDestPort(index + 1);
            }
        } else if (loadVersion.isOlderThan(LoadVersion.V220)) {
            // v2.1 and before did not have flow variable ports (index 0)
            if (destPersistor instanceof FileSingleNodeContainerPersistor) {
                // correct port index only for ordinary nodes (no new flow
                // variable ports on metanodes)
                int index = c.getDestPort();
                c.setDestPort(index + 1);
            }
        }
    }

    NodeSettingsRO readParentSettings() throws IOException {
        if (getLoadVersion().isOlderThan(LoadVersion.V200)) {
            NodeSettings result = new NodeSettings("generated_wf_settings");
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
        return settings.getInt(WorkflowPersistor.KEY_ID);
    }

    String loadUIInfoClassName(final NodeSettingsRO settings) throws InvalidSettingsException {
        if (getLoadVersion().isOlderThan(LoadVersion.V200)) {
            if (settings.containsKey(WorkflowPersistor.KEY_UI_INFORMATION)) {
                return settings.getString(WorkflowPersistor.KEY_UI_INFORMATION);
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
        String fixedName = fixUIInfoClassName(className);
        try {
            // avoid NoClassDefFoundErrors by using magic class loader
            return (UIInformation)(Class.forName(fixedName).newInstance());
        } catch (Exception e) {
            StringBuilder b = new StringBuilder();
            b.append("UIInfo class \"");
            b.append(className);
            b.append("\"");
            if (!className.equals(fixedName)) {
                b.append(" programmatically changed to \"");
                b.append(fixedName).append("\"");
            }
            b.append(" could not be loaded: ");
            b.append(e.getMessage());
            String error = b.toString();
            getLogger().warn(error, e);
            return null;
        }
    }

    void loadUIInfoSettings(final UIInformation uiInfo, final NodeSettingsRO settings) throws InvalidSettingsException {
        // in previous releases, the settings were directly written to the
        // top-most node settings object; since 2.0 they are put into a
        // separate sub-settings object
        NodeSettingsRO subSettings = getLoadVersion().isOlderThan(LoadVersion.V200)
                ? settings : settings.getNodeSettings(CFG_UIINFO_SUB_CONFIG);
        uiInfo.load(subSettings, getLoadVersion());
    }

    /**
     * Sub class hook o read port bar info.
     *
     * @param settings Ignored.
     * @return null
     * @throws InvalidSettingsException Not actually thrown here.
     */
    String loadInPortsBarUIInfoClassName(final NodeSettingsRO settings) throws InvalidSettingsException {
        if (getLoadVersion().isOlderThan(LoadVersion.V200)) {
            return null;
        }
        return loadUIInfoClassName(settings);
    }

    /**
     * Sub-class hook to load port bar info.
     *
     * @param settings Ignored.
     * @return null
     * @throws InvalidSettingsException Not actually thrown
     */
    String loadOutPortsBarUIInfoClassName(final NodeSettingsRO settings) throws InvalidSettingsException {
        if (!getLoadVersion().isOlderThan(LoadVersion.V200)) {
            return loadUIInfoClassName(settings);
        }
        return null;
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
        final LoadVersion loadVersion = getLoadVersion();
        if (loadVersion.isOlderThan(LoadVersion.V260) || !settings.containsKey(CFG_EDITOR_INFO)) {
            return EditorUIInformation.builder().build();
        }
        NodeSettingsRO editorCfg = settings.getNodeSettings(CFG_EDITOR_INFO);
        EditorUIInformation.Builder builder = EditorUIInformation.builder();
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
        String fileString = settings.getString("node_settings_file");
        if (fileString == null) {
            throw new InvalidSettingsException("Unable to read settings " + "file for node " + settings.getKey());
        }
        File workflowDir = workflowDirRef.getFile();
        // fileString is something like "File Reader(#1)/settings.xml", thus
        // it contains two levels of the hierarchy. We leave it here to the
        // java.io.File implementation to resolve these levels
        File fullFile = new File(workflowDir, fileString);
        if (!fullFile.isFile() || !fullFile.canRead()) {
            throw new InvalidSettingsException("Unable to read settings " + "file " + fullFile.getAbsolutePath());
        }
        Stack<String> children = new Stack<String>();
        File workflowDirAbsolute = workflowDir.getAbsoluteFile();
        while (!fullFile.getAbsoluteFile().equals(workflowDirAbsolute)) {
            children.push(fullFile.getName());
            fullFile = fullFile.getParentFile();
        }
        // create a ReferencedFile hierarchy for the settings file
        ReferencedFile result = workflowDirRef;
        while (!children.empty()) {
            result = new ReferencedFile(result, children.pop());
        }
        return result;
    }

    NodeType loadNodeType(final NodeSettingsRO settings) throws InvalidSettingsException {
        if (getLoadVersion().isOlderThan(LoadVersion.V200)) {
            String factory = settings.getString("factory");
            if (ObsoleteMetaNodeFileWorkflowPersistor.OLD_META_NODES.contains(factory)) {
                return NodeType.MetaNode;
            } else {
                return NodeType.NativeNode;
            }
        } else if (getLoadVersion().isOlderThan(LoadVersion.V2100Pre)) {
            return settings.getBoolean("node_is_meta") ? NodeType.MetaNode : NodeType.NativeNode;
        } else {
            final String nodeTypeString = settings.getString("node_type");
            CheckUtils.checkSettingNotNull(nodeTypeString, "node type must not be null");
            try {
                return NodeType.valueOf(nodeTypeString);
            } catch (IllegalArgumentException iae) {
                throw new InvalidSettingsException("Can't parse node type: " + nodeTypeString);
            }
        }
    }

    NodeSettingsRO loadSettingsForConnections(final NodeSettingsRO set) throws InvalidSettingsException {
        return set.getNodeSettings(WorkflowPersistor.KEY_CONNECTIONS);
    }

    ConnectionContainerTemplate loadConnection(final NodeSettingsRO settings) throws InvalidSettingsException {
        int sourceID = settings.getInt("sourceID");
        int destID = loadConnectionDestID(settings);
        int sourcePort = loadConnectionSourcePort(settings);
        int destPort = loadConnectionDestPort(settings);
        // this attribute is in most cases not present (not saved)
        boolean isDeletable = settings.getBoolean("isDeletable", true);
        if (sourceID != -1 && sourceID == destID) {
            throw new InvalidSettingsException("Source and Destination must " + "not be equal, id is " + sourceID);
        }
        ConnectionUIInformation connUIInfo = null;
        try {
            String uiInfoClass = loadUIInfoClassName(settings);
            if (uiInfoClass != null) {
                if (!uiInfoClass.equals(ConnectionUIInformation.class.getName())) {
                    getLogger().debug("Could not load UI information for " + "connection between nodes " + sourceID
                        + " and " + destID + ": expected " + ConnectionUIInformation.class.getName() + " but got "
                        + uiInfoClass.getClass().getName());
                } else {
                    ConnectionUIInformation.Builder builder = ConnectionUIInformation.builder();
                    // in previous releases, the settings were directly written to the
                    // top-most node settings object; since 2.0 they are put into a
                    // separate sub-settings object
                    NodeSettingsRO subSettings = getLoadVersion().isOlderThan(LoadVersion.V200) ? settings
                        : settings.getNodeSettings(CFG_UIINFO_SUB_CONFIG);
                    int size = subSettings.getInt(KEY_BENDPOINTS + "_size");
                    for (int i = 0; i < size; i++) {
                        int[] tmp = subSettings.getIntArray(KEY_BENDPOINTS + "_" + i);
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
        return new ConnectionContainerTemplate(sourceID, sourcePort, destID, destPort, isDeletable, connUIInfo);
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
        return set.getNodeSettings(WorkflowPersistor.KEY_NODES);
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
        if (getLoadVersion().ordinal() < LoadVersion.V250.ordinal()) {
            return WorkflowCipher.NULL_CIPHER;
        }
        if (!settings.containsKey("cipher")) {
            return WorkflowCipher.NULL_CIPHER;
        }
        NodeSettingsRO cipherSettings = settings.getNodeSettings("cipher");
        return WorkflowCipher.load(loadVersion, cipherSettings);
    }

    /** Synchronized call to DATE_FORMAT.parse(String).
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
            final NodeSettingsRO sub = settings.getNodeSettings(CFG_AUTHOR_INFORMATION);
            final String author = sub.getString("authored-by");
            final String authorDateS = sub.getString("authored-when");
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
            final String editor = sub.getString("lastEdited-by");
            final String editDateS = sub.getString("lastEdited-when");
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
            NodeSettingsRO wfmVarSub = settings.getNodeSettings(CFG_WKF_VARIABLES);
            List<FlowVariable> result = new ArrayList<FlowVariable>();
            for (String key : wfmVarSub.keySet()) {
                result.add(FlowVariable.load(wfmVarSub.getNodeSettings(key)));
            }
            return result;
        }
    }

    /**
     * Loads table backend settings (only for workflow projects). Might throw {@link TableBackendUnknownException}.
     */
    WorkflowTableBackendSettings loadTableBackendSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        if (!isProject()) {
            return null;
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
            NodeSettingsRO annoSettings = settings.getNodeSettings("annotations");
            List<WorkflowAnnotation> result = new ArrayList<WorkflowAnnotation>();
            for (String key : annoSettings.keySet()) {
                NodeSettingsRO child = annoSettings.getNodeSettings(key);
                WorkflowAnnotation anno = new WorkflowAnnotation();
                anno.load(child, getLoadVersion());
                result.add(anno);
            }
            return result;
        }
    }

    /** @return the wizard state saved in the file or null (often null).
     * @param settings ...
     * @throws InvalidSettingsException ... */
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
        int index = settings.getInt("index");
        String name = settings.getString("name");
        NodeSettingsRO portTypeSettings = settings.getNodeSettings("type");
        PortType type = PortType.load(portTypeSettings);
        WorkflowPortTemplate result = new WorkflowPortTemplate(index, type);
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
            int index = settings.getInt("index");
            String name = settings.getString("name");
            NodeSettingsRO portTypeSettings = settings.getNodeSettings("type");
            PortType type = PortType.load(portTypeSettings);
            WorkflowPortTemplate result = new WorkflowPortTemplate(index, type);
            result.setPortName(name);
            return result;
        }
    }

    /**
     * check whether there is a "loaded with no data" file.
     * @param workflowDir ...
     * @return true for old workflows (&lt;2.0) or if there is a {@value WorkflowPersistor#SAVED_WITH_DATA_FILE} file.
     */
    boolean loadIfMustWarnOnDataLoadError(final File workflowDir) {
        if (getLoadVersion().isOlderThan(LoadVersion.V200)) {
            return true;
        }
        return new File(workflowDir, WorkflowPersistor.SAVED_WITH_DATA_FILE).isFile();
    }


    /** {@inheritDoc} */
    @Override
    public void postLoad(final WorkflowManager wfm, final LoadResult loadResult) {
    }

    FileSingleNodeContainerPersistor createNativeNodeContainerPersistorLoad(final ReferencedFile nodeFile) {
        return new FileNativeNodeContainerPersistor(nodeFile, getLoadHelper(), getLoadVersion(),
            getWorkflowDataRepository(), mustWarnOnDataLoadError());
    }

    FileSubNodeContainerPersistor createSubNodeContainerPersistorLoad(final ReferencedFile nodeFile) {
        return new FileSubNodeContainerPersistor(nodeFile, getLoadHelper(), getLoadVersion(),
            getWorkflowDataRepository(), mustWarnOnDataLoadError());
    }

    FileWorkflowPersistor createWorkflowPersistorLoad(final ReferencedFile wfmFile) {
        if (getLoadVersion().isOlderThan(LoadVersion.V200)) {
            return new ObsoleteMetaNodeFileWorkflowPersistor(getWorkflowDataRepository(),
                wfmFile, getLoadHelper(), getLoadVersion());
        } else {
            return new FileWorkflowPersistor(getWorkflowDataRepository(), wfmFile,
                getLoadHelper(), getLoadVersion(), false);
        }
    }

    private int getRandomNodeID() {
        // some number between 10k and 20k, hopefully unique.
        int nodeIDSuffix = 10000 + (int)(Math.random() * 10000);
        while (m_nodeContainerLoaderMap.containsKey(nodeIDSuffix)) {
            nodeIDSuffix += 1;
        }
        return nodeIDSuffix;
    }

    Optional<WorkflowExecutionResult> getExecutionResult() {
        return Optional.empty();
    }

    /**
     * {@inheritDoc}
     *
     * @since 2.7
     */
    @Override
    public void guessPortTypesFromConnectedNodes(final NodeAndBundleInformationPersistor nodeInfo,
        final NodeSettingsRO additionalFactorySettings, final ArrayList<PersistorWithPortIndex> upstreamNodes,
        final ArrayList<List<PersistorWithPortIndex>> downstreamNodes) {
        // not applicable for metanodes
    }

    /**
     * {@inheritDoc}
     *
     * @since 2.7
     */
    @Override
    public PortType getDownstreamPortType(final int index) {
        if (m_outPortTemplates != null && index < m_outPortTemplates.length) {
            return m_outPortTemplates[index].getPortType();
        }
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * @since 2.7
     */
    @Override
    public PortType getUpstreamPortType(final int index) {
        if (m_inPortTemplates != null && index < m_inPortTemplates.length) {
            return m_inPortTemplates[index].getPortType();
        }
        return null;
    }

    /** @return version that is saved
     * @since 3.7*/
    protected static LoadVersion getSaveVersion() {
        return VERSION_LATEST;
    }


    /** Synchronized call to DATE_FORMAT.format(Date).
     * @param date ... not null.
     * @return The string.
     */
    static String formatDate(final OffsetDateTime date) {
        synchronized (DATE_FORMAT) {
            return DATE_FORMAT.format(date);
        }
    }

    /**
     * TODO
     *
     * Iterates
     *
     * @param exec
     * @param loadResult
     * @param missingNodeIDMap
     * @throws CanceledExecutionException
     */
    private void handleMissingNodes(final ExecutionMonitor exec, final LoadResult loadResult,
        final Map<Integer, NodeFactoryUnknownException> missingNodeIDMap) throws CanceledExecutionException {

        for (Map.Entry<Integer, NodeFactoryUnknownException> missingNode : missingNodeIDMap.entrySet()) {
            exec.checkCanceled();
            int missingNodeSuffix = missingNode.getKey();
            var nodeInfo = missingNode.getValue().getNodeAndBundleInformation();

            loadResult.addMissingNode(nodeInfo);

            var additionalFactorySettings = missingNode.getValue().getAdditionalFactorySettings();
            var upstreamNodes = new ArrayList<PersistorWithPortIndex>();
            var downstreamNodes = new ArrayList<List<PersistorWithPortIndex>>();
            for (ConnectionContainerTemplate t : m_connectionSet) {
                // check upstream nodes
                var sourceSuffix = t.getSourceSuffix();
                var destSuffix = t.getDestSuffix();
                var sourcePort = t.getSourcePort();
                var destPort = t.getDestPort();
                if (destSuffix == missingNodeSuffix) {
                    FromFileNodeContainerPersistor persistor;
                    if (sourceSuffix == -1) { // connected to this metanode's input port bar
                        persistor = this;
                    } else {
                        persistor = m_nodeContainerLoaderMap.get(sourceSuffix);
                    }
                    ensureArrayListIndexValid(upstreamNodes, destPort);
                    upstreamNodes.set(destPort, new PersistorWithPortIndex(persistor, sourcePort));
                }
                // check downstream nodes
                if (sourceSuffix == missingNodeSuffix) {
                    FromFileNodeContainerPersistor persistor;
                    if (destSuffix == -1) { // connect to this metanode's output port bar
                        persistor = this;
                    } else {
                        persistor = m_nodeContainerLoaderMap.get(destSuffix);
                    }
                    ensureArrayListIndexValid(downstreamNodes, sourcePort);
                    var downstreamNodesAtPort = downstreamNodes.get(sourcePort);
                    if (downstreamNodesAtPort == null) {
                        downstreamNodesAtPort = new ArrayList<>();
                        downstreamNodes.set(sourcePort, downstreamNodesAtPort);
                    }
                    downstreamNodesAtPort.add(new PersistorWithPortIndex(persistor, destPort));
                }
            }
            var failingNodePersistor = m_nodeContainerLoaderMap.get(missingNodeSuffix);
            failingNodePersistor.guessPortTypesFromConnectedNodes(nodeInfo, additionalFactorySettings, upstreamNodes,
                downstreamNodes);
        }
    }


}
