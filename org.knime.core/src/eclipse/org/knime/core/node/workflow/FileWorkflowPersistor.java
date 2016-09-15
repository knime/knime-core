/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
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
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
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
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.TreeMap;

import org.knime.core.api.node.workflow.AnnotationData;
import org.knime.core.api.node.workflow.AnnotationData.StyleRange;
import org.knime.core.api.node.workflow.AnnotationData.TextAlignment;
import org.knime.core.api.node.workflow.ConnectionUIInformation;
import org.knime.core.api.node.workflow.NodeUIInformation;
import org.knime.core.data.container.ContainerTable;
import org.knime.core.data.filestore.internal.WorkflowFileStoreHandlerRepository;
import org.knime.core.internal.ReferencedFile;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.Node;
import org.knime.core.node.NodeAndBundleInformation;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortType;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.workflow.MetaNodeTemplateInformation.Role;
import org.knime.core.node.workflow.WorkflowManager.AuthorInformation;
import org.knime.core.util.FileUtil;
import org.knime.core.util.LockFailedException;

/**
 *
 * @author wiswedel, University of Konstanz
 */
public class FileWorkflowPersistor implements WorkflowPersistor, TemplateNodeContainerPersistor {

    /**
     * A Version representing a specific workflow format. This enum covers only the version that this specific class can
     * read (or write). Ordinal numbering is important.
     */
    public static enum LoadVersion {
        // Don't modify order, ordinal number are important.
            /** Pre v2.0. */
            UNKNOWN("<unknown>"),
            /** Version 2.0.0 - 2.0.x. */
            V200("2.0.0"),
            /**
             * Trunk version when 2.0.x was out, covers cluster and server prototypes. Obsolete since 2009-08-12.
             */
            V210_Pre("2.0.1"),
            /** Version 2.1.x. */
            V210("2.1.0"),
            /**
             * Version 2.2.x, introduces optional inputs, flow variable input credentials, node local drop directory.
             */
            V220("2.2.0"),
            /** Version 2.3.x, introduces workflow annotations &amp; switches. */
            V230("2.3.0"),
            /** Version 2.4.x, introduces metanode templates. */
            V240("2.4.0"),
            /** Version 2.5.x, lockable metanodes, node-relative annotations. */
            V250("2.5.0"),
            /** Version 2.6.x, file store objects, grid information, node vendor &amp; plugin information.
             * @since 2.6 */
            V260("2.6.0"),
            /** node.xml and settings.xml are one file, settings in SNC, meta data in workflow.knime.
             * @since 2.8 */
            V280("2.8.0"),
            /** basic subnode support, never released (trunk code between 2.9 and 2.10).
             * @since 2.10 */
            V2100Pre("2.10.0Pre"),
            /** better subnode support, PortObjectHolder, FileStorePortObject w/ array file stores.
             * @since 2.10 */
            V2100("2.10.0"),
            /** Subnode outputs as port object holder, "VoidTable".
             * @since 3.1 */
            V3010("3.1.0"),
            /** Try to be forward compatible.
             * @since 2.8 */
            FUTURE("<future>");

        private final String m_versionString;

        private LoadVersion(final String str) {
            m_versionString = str;
        }

        /** @return The String representing the LoadVersion (workflow.knime). */
        public String getVersionString() {
            return m_versionString;
        }

        /** Is this' ordinal smaller than this ordinal of the argument? For instance
         * getLoadVersion().isOlderThan(LoadVersion.V200) means we are loading a workflow
         * older than 2.0
         * @param version compare version
         * @return  that property */
        public boolean isOlderThan(final LoadVersion version) {
            return ordinal() < version.ordinal();
        }

        /**
         * Get the load version for the version string or null if unknown.
         *
         * @param string Version string (as in workflow.knime).
         * @return The LoadVersion or null.
         */
        static LoadVersion get(final String string) {
            for (LoadVersion e : values()) {
                if (e.m_versionString.equals(string)) {
                    return e;
                }
            }
            return null;
        }

    }

    /** KNIME Node type: native, meta or sub node.*/
    private enum NodeType {
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

    static final LoadVersion VERSION_LATEST = LoadVersion.V3010;

    /** Format used to save author/edit infos. */
    static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z");

    private static final String CFG_UIINFO_SUB_CONFIG = "ui_settings";

    /** Key for UI info's class name. */
    private static final String CFG_UIINFO_CLASS = "ui_classname";

    /** Key for workflow variables. */
    private static final String CFG_WKF_VARIABLES = "workflow_variables";

    /** key used to store the editor specific settings (since 2.6). */
    private static final String CFG_EDITOR_INFO = "workflow_editor_settings";

    /** Key for workflow template information. */
    private static final String CFG_TEMPLATE_INFO = "workflow_template_information";

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

    /** The key under which the bounds to store the {@link ConnectionUIInformation} are registered. * */
    public static final String KEY_BENDPOINTS = "extrainfo.conn.bendpoints";

    /** The key under which the bounds are registered. * */
    private static final String KEY_BOUNDS = "extrainfo.node.bounds";

    private static final PortType FALLBACK_PORTTYPE = PortObject.TYPE;

    private static final NodeSettingsRO EMPTY_SETTINGS = new NodeSettings("<<empty>>");

    /** The node logger for this class. */
    private final NodeLogger m_logger = NodeLogger.getLogger(getClass());

    private final LoadVersion m_versionString;

    private final TreeMap<Integer, FromFileNodeContainerPersistor> m_nodeContainerLoaderMap;

    private final HashSet<ConnectionContainerTemplate> m_connectionSet;

    private final FileNodeContainerMetaPersistor m_metaPersistor;

    private final HashMap<Integer, ContainerTable> m_globalTableRepository;

    private final WorkflowFileStoreHandlerRepository m_fileStoreRepository;

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

    private List<Credentials> m_credentials;

    private List<WorkflowAnnotation> m_workflowAnnotations;

    private NodeSettingsRO m_wizardState;

    private boolean m_needsResetAfterLoad;

    private boolean m_isDirtyAfterLoad;

    private boolean m_mustWarnOnDataLoadError;

    private final boolean m_isProject;

    private NodeSettingsRO m_workflowSett;

    private final List<ReferencedFile> m_obsoleteNodeDirectories;

    static LoadVersion parseVersion(final String versionString) {
        boolean isBeforeV2 = versionString.equals("0.9.0");
        isBeforeV2 |= versionString.equals("1.0");
        isBeforeV2 |= versionString.matches("1\\.[01234]\\.[0-9].*");
        if (isBeforeV2) {
            return LoadVersion.UNKNOWN;
        }
        return LoadVersion.get(versionString);
    }

    /**
     * Create persistor for load.
     *
     * @param tableRep Table map
     * @param dotKNIMEFile Associated workflow.knime or template.knime file
     * @param loadHelper The load helper as required by meta persistor.
     * @param version of loading workflow.
     */
    FileWorkflowPersistor(final HashMap<Integer, ContainerTable> tableRep,
        final WorkflowFileStoreHandlerRepository fileStoreHandlerRepository, final ReferencedFile dotKNIMEFile,
        final WorkflowLoadHelper loadHelper, final LoadVersion version, final boolean isProject) {
        assert version != null;
        m_globalTableRepository = tableRep;
        m_fileStoreRepository = fileStoreHandlerRepository;
        m_versionString = version;
        m_metaPersistor = new FileNodeContainerMetaPersistor(dotKNIMEFile, loadHelper, version);
        m_nodeContainerLoaderMap = new TreeMap<Integer, FromFileNodeContainerPersistor>();
        m_connectionSet = new HashSet<ConnectionContainerTemplate>();
        m_obsoleteNodeDirectories = new ArrayList<ReferencedFile>();
        m_isProject = isProject;
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
            throw new RuntimeException("Persistor not created for loading " + "workflow, meta persistor is null");
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

    /** {@inheritDoc} */
    @Override
    public HashMap<Integer, ContainerTable> getGlobalTableRepository() {
        return m_globalTableRepository;
    }

    /**
     * {@inheritDoc}
     *
     * @since 2.6
     */
    @Override
    public WorkflowFileStoreHandlerRepository getFileStoreHandlerRepository() {
        return m_fileStoreRepository;
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

    /** {@inheritDoc} */
    @Override
    public List<Credentials> getCredentials() {
        return m_credentials;
    }

    /** {@inheritDoc}
     * @since 2.8
     */
    @Override
    public WorkflowContext getWorkflowContext() {
        return isProject() ? getMetaPersistor().getLoadHelper().getWorkflowContext() : null;
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

    /** {@inheritDoc} */
    @Override
    public NodeUIInformation getInPortsBarUIInfo() {
        return m_inPortsBarUIInfo;
    }

    /** {@inheritDoc} */
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
        final ReferencedFile knimeFile = getWorkflowKNIMEFile();
        if (knimeFile == null || !knimeFile.getFile().isFile()) {
            setDirtyAfterLoad();
            String error = "Can't read workflow file \"" + knimeFile + "\"";
            throw new IOException(error);
        }
        // workflow.knime (or template.knime)
        File nodeFile = knimeFile.getFile();
        ReferencedFile parentRef = knimeFile.getParent();
        if (parentRef == null) {
            setDirtyAfterLoad();
            throw new IOException("Parent directory of file \"" + knimeFile + "\" is not represented by "
                + ReferencedFile.class.getSimpleName() + " object");
        }
        m_mustWarnOnDataLoadError = loadIfMustWarnOnDataLoadError(parentRef.getFile());
        NodeSettingsRO subWFSettings;
        try {
            InputStream in = new FileInputStream(nodeFile);
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
                assert m_templateInformation.getRole() == Role.Link;
            } else {
                m_templateInformation = MetaNodeTemplateInformation.load(m_workflowSett, getLoadVersion());
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
            m_authorInformation = loadAuthorInformation(m_workflowSett);
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
            m_credentials = loadCredentials(m_workflowSett);
            // request to initialize credentials - if available
            if (m_credentials != null && !m_credentials.isEmpty()) {
                m_credentials = getLoadHelper().loadCredentials(m_credentials);
            }
        } catch (InvalidSettingsException e) {
            String error = "Unable to load credentials: " + e.getMessage();
            getLogger().debug(error, e);
            setDirtyAfterLoad();
            loadResult.addError(error);
            m_credentials = Collections.emptyList();
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
        boolean isResetRequired = m_metaPersistor.load(subWFSettings, metaFlowParentSettings, loadResult);
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
            fixSourcePortIfNecessary(sourceNodePersistor, c);

            int destIDSuffix = c.getDestSuffix();
            NodeContainerPersistor destNodePersistor = m_nodeContainerLoaderMap.get(destIDSuffix);
            if (destNodePersistor == null && destIDSuffix != -1) {
                setDirtyAfterLoad();
                if (!failingNodeIDSet.contains(destIDSuffix)) {
                    loadResult.addError("Unable to load node connection " + c + ", destination node does not exist");
                }
                continue;
            }
            fixDestPortIfNecessary(destNodePersistor, c);

            if (!m_connectionSet.add(c)) {
                setDirtyAfterLoad();
                loadResult.addError("Duplicate connection information: " + c);
            }
        }

        for (Map.Entry<Integer, NodeFactoryUnknownException> missingNode : missingNodeIDMap.entrySet()) {
            exec.checkCanceled();
            int missingNodeSuffix = missingNode.getKey();
            NodeAndBundleInformation nodeInfo = missingNode.getValue().getNodeAndBundleInformation();
            loadResult.addMissingNode(nodeInfo);
            NodeSettingsRO additionalFactorySettings = missingNode.getValue().getAdditionalFactorySettings();
            ArrayList<PersistorWithPortIndex> upstreamNodes = new ArrayList<PersistorWithPortIndex>();
            ArrayList<List<PersistorWithPortIndex>> downstreamNodes = new ArrayList<List<PersistorWithPortIndex>>();
            for (ConnectionContainerTemplate t : m_connectionSet) {
                // check upstream nodes
                int sourceSuffix = t.getSourceSuffix();
                int destSuffix = t.getDestSuffix();
                int sourcePort = t.getSourcePort();
                int destPort = t.getDestPort();
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
                    List<PersistorWithPortIndex> downstreamNodesAtPort = downstreamNodes.get(sourcePort);
                    if (downstreamNodesAtPort == null) {
                        downstreamNodesAtPort = new ArrayList<PersistorWithPortIndex>();
                        downstreamNodes.set(sourcePort, downstreamNodesAtPort);
                    }
                    downstreamNodesAtPort.add(new PersistorWithPortIndex(persistor, destPort));
                }
            }
            FromFileNodeContainerPersistor failingNodePersistor = m_nodeContainerLoaderMap.get(missingNodeSuffix);
            failingNodePersistor.guessPortTypesFromConnectedNodes(nodeInfo, additionalFactorySettings, upstreamNodes,
                downstreamNodes);
        }
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
        boolean symbolRelative = loadOrdinal >= FileWorkflowPersistor.LoadVersion.V230.ordinal();
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
     * @param sourcePersistor The persistor of the source node.
     * @param c The connection template to be fixed.
     */
    void fixSourcePortIfNecessary(final NodeContainerPersistor sourcePersistor, final ConnectionContainerTemplate c) {
        // v2.1 and before did not have flow variable ports (index 0)
        if (getLoadVersion().isOlderThan(LoadVersion.V220)) {
            if (sourcePersistor instanceof FileSingleNodeContainerPersistor) {
                // correct port index only for ordinary nodes (no new flow
                // variable ports on metanodes)
                int index = c.getSourcePort();
                c.setSourcePort(index + 1);
            }
        }
    }

    /**
     * Fixes destination port index if necessary. For v1.x flows, e.g., the indices of model and data ports were
     * swapped. Subclasses will overwrite this method (e.g. to enable loading flows, which did not have the mandatory
     * flow variable port object).
     *
     * @param destPersistor The persistor of the destination node.
     * @param c The connection template to be fixed.
     */
    void fixDestPortIfNecessary(final NodeContainerPersistor destPersistor, final ConnectionContainerTemplate c) {
        if (getLoadVersion().isOlderThan(LoadVersion.V220)) {
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
        } else if (getLoadVersion().isOlderThan(LoadVersion.V220)) {
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
     * @param uiInfo Ignored.
     * @param settings Ignored.
     * @throws InvalidSettingsException Not actually thrown
     */
    void loadInPortsBarUIInfo(final UIInformation uiInfo, final NodeSettingsRO settings)
        throws InvalidSettingsException {
        if (!getLoadVersion().isOlderThan(LoadVersion.V200)) {
            loadUIInfoSettings(uiInfo, settings);
        }
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
     * Load output port bars. This implementation does nothing, sub-classes override this method.
     *
     * @param uiInfo Ignored here.
     * @param settings Ignored here.
     * @throws InvalidSettingsException Not actually thrown here.
     */
    void loadOutPortsBarUIInfo(final UIInformation uiInfo, final NodeSettingsRO settings)
        throws InvalidSettingsException {
        if (!getLoadVersion().isOlderThan(LoadVersion.V200)) {
            loadUIInfoSettings(uiInfo, settings);
        }
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
        EditorUIInformation.Builder builder = new EditorUIInformation.Builder();
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
        return set.getNodeSettings(KEY_CONNECTIONS);
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
     * @throws ParseException ...
     */
    static Date parseDate(final String s) throws ParseException {
        synchronized (DATE_FORMAT) {
            return DATE_FORMAT.parse(s);
        }
    }

    AuthorInformation loadAuthorInformation(final NodeSettingsRO settings) throws InvalidSettingsException {
        if (getLoadVersion().ordinal() >= LoadVersion.V280.ordinal() && settings.containsKey(CFG_AUTHOR_INFORMATION)) {
            final NodeSettingsRO sub = settings.getNodeSettings(CFG_AUTHOR_INFORMATION);
            final String author = sub.getString("authored-by");
            final String authorDateS = sub.getString("authored-when");
            final Date authorDate;
            if (authorDateS == null) {
                authorDate = null;
            } else {
                try {
                    authorDate = parseDate(authorDateS);
                } catch (ParseException e) {
                    throw new InvalidSettingsException("Can't parse authored-when \"" + authorDateS
                        + "\": " + e.getMessage(), e);
                }
            }
            final String editor = sub.getString("lastEdited-by");
            final String editDateS = sub.getString("lastEdited-when");
            final Date editDate;
            if (editDateS == null) {
                editDate = null;
            } else {
                try {
                    editDate = parseDate(editDateS);
                } catch (ParseException e) {
                    throw new InvalidSettingsException("Can't parse lastEdit-when \"" + editDateS
                        + "\": " + e.getMessage(), e);
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
            NodeSettingsRO sub = settings.getNodeSettings(CFG_CREDENTIALS);
            List<Credentials> r = new ArrayList<Credentials>();
            Set<String> credsNameSet = new HashSet<String>();
            for (String key : sub.keySet()) {
                NodeSettingsRO child = sub.getNodeSettings(key);
                Credentials c = Credentials.load(child);
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
                AnnotationData annotationData = loadAnnotationData(child, getLoadVersion());
                WorkflowAnnotation anno = new WorkflowAnnotation(annotationData);
                anno.fireChangeEvent();
                result.add(anno);
            }
            return result;
        }
    }

    /**
     * Loads the annotation data from the given settings object.
     *
     * @param settings
     * @param loadVersion
     * @return a new {@link AnnotationData} object
     * @throws InvalidSettingsException
     */
    static AnnotationData loadAnnotationData(final NodeSettingsRO settings, final LoadVersion loadVersion)
        throws InvalidSettingsException {
        AnnotationData.Builder builder = AnnotationData.builder();
        builder.setText(settings.getString("text"));
        builder.setBgColor(settings.getInt("bgcolor"));
        int x = settings.getInt("x-coordinate");
        int y = settings.getInt("y-coordinate");
        int width = settings.getInt("width");
        int height = settings.getInt("height");
        int borderSize = settings.getInt("borderSize", 0); // default to 0 for backward compatibility
        int borderColor = settings.getInt("borderColor", 0); // default for backward compatibility
        int defFontSize = settings.getInt("defFontSize", -1); // default for backward compatibility
        int version = settings.getInt("annotation-version", AnnotationData.VERSION_OLD); // added in 3.0
        TextAlignment alignment = TextAlignment.LEFT;
        if (loadVersion.ordinal() >= FileWorkflowPersistor.LoadVersion.V250.ordinal()) {
            String alignmentS = settings.getString("alignment");
            try {
                alignment = TextAlignment.valueOf(alignmentS);
            } catch (Exception e) {
                throw new InvalidSettingsException("Invalid alignment: " + alignmentS, e);
            }
        }
        builder.setDimension(x, y, width, height);
        builder.setAlignment(alignment);
        builder.setBorderSize(borderSize);
        builder.setBorderColor(borderColor);
        builder.setDefaultFontSize(defFontSize);
        NodeSettingsRO styleConfigs = settings.getNodeSettings("styles");
        StyleRange[] styles = new StyleRange[styleConfigs.getChildCount()];
        int i = 0;
        for (String key : styleConfigs.keySet()) {
            NodeSettingsRO cur = styleConfigs.getNodeSettings(key);
            styles[i++] = loadStyleRange(cur);
        }
        builder.setStyleRanges(styles);
        return builder.build();
    }

    /**
     * Helper method to load a style range from a settings object.
     *
     * @param settings
     * @throws InvalidSettingsException
     */
    private static StyleRange loadStyleRange(final NodeSettingsRO settings) throws InvalidSettingsException {
        StyleRange.Builder result = StyleRange.builder();
        result.setStart(settings.getInt("start"));
        result.setLength(settings.getInt("length"));
        result.setFontName(settings.getString("fontname"));
        result.setFontStyle(settings.getInt("fontstyle"));
        result.setFontSize(settings.getInt("fontsize"));
        result.setFgColor(settings.getInt("fgcolor"));
        return result.build();
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
        return new File(workflowDir, SAVED_WITH_DATA_FILE).isFile();
    }


    /** {@inheritDoc} */
    @Override
    public void postLoad(final WorkflowManager wfm, final LoadResult loadResult) {
    }

    FileSingleNodeContainerPersistor createNativeNodeContainerPersistorLoad(final ReferencedFile nodeFile) {
        return new FileNativeNodeContainerPersistor(nodeFile, getLoadHelper(), getLoadVersion(),
            getGlobalTableRepository(), getFileStoreHandlerRepository(), mustWarnOnDataLoadError());
    }

    FileSubNodeContainerPersistor createSubNodeContainerPersistorLoad(final ReferencedFile nodeFile) {
        return new FileSubNodeContainerPersistor(nodeFile, getLoadHelper(), getLoadVersion(),
            getGlobalTableRepository(), getFileStoreHandlerRepository(), mustWarnOnDataLoadError());
    }

    FileWorkflowPersistor createWorkflowPersistorLoad(final ReferencedFile wfmFile) {
        if (getLoadVersion().isOlderThan(LoadVersion.V200)) {
            return new ObsoleteMetaNodeFileWorkflowPersistor(getGlobalTableRepository(),
                getFileStoreHandlerRepository(), wfmFile, getLoadHelper(), getLoadVersion());
        } else {
            return new FileWorkflowPersistor(getGlobalTableRepository(), getFileStoreHandlerRepository(),
                wfmFile, getLoadHelper(), getLoadVersion(), false);
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

    /**
     * {@inheritDoc}
     *
     * @since 2.7
     */
    @Override
    public void guessPortTypesFromConnectedNodes(final NodeAndBundleInformation nodeInfo,
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

    /** @return version that is saved */
    protected static LoadVersion getSaveVersion() {
        return VERSION_LATEST;
    }


    /** Synchronized call to DATE_FORMAT.format(Date).
     * @param date ... not null.
     * @return The string.
     */
    static String formatDate(final Date date) {
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
        NodeSettingsWO subConfig = settings.addNodeSettings(CFG_UIINFO_SUB_CONFIG);
        uiInfo.save(subConfig);
    }

    public static String save(final WorkflowManager wm, final ReferencedFile rawWorkflowDirRef,
        final ExecutionMonitor execMon, final WorkflowSaveHelper saveHelper)
                throws IOException, CanceledExecutionException, LockFailedException {
        final String name = wm.getDirectNCParent().getCipherFileName(WORKFLOW_FILE);
        NodeSettings preFilledSettings = new NodeSettings(name);
        saveHeader(preFilledSettings);
        wm.getTemplateInformation().save(preFilledSettings);
        saveWizardState(wm, preFilledSettings, saveHelper);
        saveContent(wm, preFilledSettings, rawWorkflowDirRef, execMon, saveHelper);
        return name;
    }

    public static String saveAsTemplate(final WorkflowManager wm, final ReferencedFile rawWorkflowDirRef,
        final ExecutionMonitor execMon, final WorkflowSaveHelper saveHelper)
                throws IOException, CanceledExecutionException, LockFailedException {
        MetaNodeTemplateInformation tI = wm.getTemplateInformation();
        if (!Role.Template.equals(tI.getRole())) {
            throw new IllegalStateException("Cannot save workflow as template (role " + tI.getRole() + ")");
        }
        // as per 2.10 template workflows are also saved under workflow.knime (previously it was all contained in
        // template.knime). The new template.knime file is written elsewhere.
        final String name = wm.getDirectNCParent().getCipherFileName(WORKFLOW_FILE);
        NodeSettings preFilledSettings = new NodeSettings(name);
        saveContent(wm, preFilledSettings, rawWorkflowDirRef, execMon, saveHelper);
        return name;
    }

    /**
     * @param wm The WFM to save.
     * @param preFilledSettings The settings eventually written to workflow.knime (or workflow.knime.encrypted).
     * For workflows it contains the version number, cipher, template infos etc. The name of the setting defines the
     * output file name (so it's important!)
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
        ReferencedFile workflowDirRef = rawWorkflowDirRef;
        Role r = wm.getTemplateInformation().getRole();
        final String fName = preFilledSettings.getKey();
        if (!workflowDirRef.fileLockRootForVM()) {
            throw new LockFailedException("Can't write workflow to \"" + workflowDirRef
                + "\" because the directory can't be locked");
        }
        try {
            final ReferencedFile nodeContainerDirectory = wm.getNodeContainerDirectory();
            final ReferencedFile autoSaveDirectory = wm.getAutoSaveDirectory();
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
            File workflowDir = workflowDirRef.getFile();
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
            saveWorkflowAnnotations(wm, preFilledSettings);

            NodeSettingsWO nodesSettings = saveSettingsForNodes(preFilledSettings);
            Collection<NodeContainer> nodes = wm.getNodeContainers();
            double progRatio = 1.0 / (nodes.size() + 1);

            for (NodeContainer nextNode : nodes) {
                int id = nextNode.getID().getIndex();
                ExecutionMonitor subExec = execMon.createSubProgress(progRatio);
                execMon.setMessage(nextNode.getNameWithID());
                NodeSettingsWO sub = nodesSettings.addNodeSettings("node_" + id);
                saveNodeContainer(sub, workflowDirRef, nextNode, subExec, saveHelper);
                subExec.setProgress(1.0);
            }

            execMon.setMessage("connection information");
            NodeSettingsWO connSettings = saveSettingsForConnections(preFilledSettings);
            int connectionNumber = 0;
            for (ConnectionContainer cc : wm.getConnectionContainers()) {
                NodeSettingsWO nextConnectionConfig = connSettings.addNodeSettings("connection_" + connectionNumber);
                saveConnection(nextConnectionConfig, cc);
                connectionNumber += 1;
            }
            int inCount = wm.getNrInPorts();
            NodeSettingsWO inPortsSetts = inCount > 0 ? saveInPortsSetting(preFilledSettings) : null;
            NodeSettingsWO inPortsSettsEnum = null;
            if (inPortsSetts != null) {
                //TODO actually not neccessary to save the class name
                saveInportsBarUIInfoClassName(inPortsSetts, wm.getInPortsBarUIInfo());
                saveInportsBarUIInfoSettings(inPortsSetts, wm.getInPortsBarUIInfo());
                inPortsSettsEnum = saveInPortsEnumSetting(inPortsSetts);
            }
            for (int i = 0; i < inCount; i++) {
                NodeSettingsWO sPort = saveInPortSetting(inPortsSettsEnum, i);
                saveInPort(sPort, wm, i);
            }
            int outCount = wm.getNrOutPorts();
            NodeSettingsWO outPortsSetts = outCount > 0 ? saveOutPortsSetting(preFilledSettings) : null;
            NodeSettingsWO outPortsSettsEnum = null;
            if (outPortsSetts != null) {
                saveOutportsBarUIInfoClassName(outPortsSetts, wm.getOutPortsBarUIInfo());
                saveOutportsBarUIInfoSettings(outPortsSetts, wm.getOutPortsBarUIInfo());
                outPortsSettsEnum = saveOutPortsEnumSetting(outPortsSetts);
            }
            for (int i = 0; i < outCount; i++) {
                NodeSettingsWO singlePort = saveOutPortSetting(outPortsSettsEnum, i);
                saveOutPort(singlePort, wm, i);
            }
            saveEditorUIInformation(wm, preFilledSettings);

            File workflowFile = new File(workflowDir, fName);
            String toBeDeletedFileName = Role.Template.equals(r) ? TEMPLATE_FILE : WORKFLOW_FILE;
            new File(workflowDir, toBeDeletedFileName).delete();
            new File(workflowDir, WorkflowCipher.getCipherFileName(toBeDeletedFileName)).delete();

            OutputStream os = new FileOutputStream(workflowFile);
            os = wm.getDirectNCParent().cipherOutput(os);
            preFilledSettings.saveToXML(os);
            if (saveHelper.isSaveData()) {
                File saveWithDataFile = new File(workflowDir, SAVED_WITH_DATA_FILE);
                BufferedWriter o = new BufferedWriter(new FileWriter(saveWithDataFile));
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
            NodeContainerState wmState = wm.getNodeContainerState();
            // non remote executions
            boolean isExecutingLocally = wmState.isExecutionInProgress() && !wmState.isExecutingRemotely();
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
        settings.addString(WorkflowManager.CFG_CREATED_BY, KNIMEConstants.VERSION);
        settings.addString(WorkflowManager.CFG_VERSION, getSaveVersion().getVersionString());
    }

    /** Saves the status of the wizard if set so in the save-helper.
     * @param wm ...
     * @param preFilledSettings ...
     * @param saveHelper ...
     */
    private static void saveWizardState(final WorkflowManager wm, final NodeSettings preFilledSettings,
        final WorkflowSaveHelper saveHelper) {
        if (!saveHelper.isSaveWizardController() || !wm.isProject()) {
            return;
        }
        NodeSettingsWO wizardSettings = preFilledSettings.addNodeSettings("wizard");
        final WizardExecutionController wizardController = wm.getWizardExecutionController();
        if (wizardController != null) {
            wizardController.save(wizardSettings);
        }
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
            NodeSettingsWO cipherSettings = settings.addNodeSettings("cipher");
            workflowCipher.save(cipherSettings);
        }
    }

    /** @since 2.8 */
    protected static void saveAuthorInformation(final AuthorInformation aI, final NodeSettingsWO settings) {
        if (aI != null) {
            final NodeSettingsWO sub = settings.addNodeSettings(CFG_AUTHOR_INFORMATION);
            sub.addString("authored-by", aI.getAuthor());
            String authorWhen = aI.getAuthoredDate() == null ? null : formatDate(aI.getAuthoredDate());
            sub.addString("authored-when", authorWhen);
            sub.addString("lastEdited-by", aI.getLastEditor());
            String lastEditWhen = aI.getLastEditDate() == null ? null : formatDate(aI.getLastEditDate());
            sub.addString("lastEdited-when", lastEditWhen);
        }
    }

    /**
     * @param settings
     * @since 2.6
     */
    static void saveEditorUIInformation(final WorkflowManager wfm, final NodeSettings settings) {
        EditorUIInformation editorInfo = wfm.getEditorUIInformation();
        if (editorInfo != null) {
            NodeSettingsWO editorConfig = settings.addNodeSettings(CFG_EDITOR_INFO);
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
        List<FlowVariable> vars = wfm.getWorkflowVariables();
        if (!vars.isEmpty()) {
            NodeSettingsWO wfmVarSub = settings.addNodeSettings(CFG_WKF_VARIABLES);
            int i = 0;
            for (FlowVariable v : vars) {
                v.save(wfmVarSub.addNodeSettings("Var_" + (i++)));
            }
        }
    }

    protected static void saveCredentials(final WorkflowManager wfm, final NodeSettingsWO settings) {
        CredentialsStore credentialsStore = wfm.getCredentialsStore();
        NodeSettingsWO sub = settings.addNodeSettings(CFG_CREDENTIALS);
        synchronized (credentialsStore) {
            for (Credentials c : credentialsStore.getCredentials()) {
                NodeSettingsWO s = sub.addNodeSettings(c.getName());
                c.save(s);
            }
        }
    }

    protected static void saveWorkflowAnnotations(final WorkflowManager manager, final NodeSettingsWO settings) {
        Collection<WorkflowAnnotation> annotations = manager.getWorkflowAnnotations();
        if (annotations.size() == 0) {
            return;
        }
        NodeSettingsWO annoSettings = settings.addNodeSettings("annotations");
        int i = 0;
        for (Annotation a : annotations) {
            NodeSettingsWO t = annoSettings.addNodeSettings("annotation_" + i);
            saveAnnotationData(annoSettings, a.getData());
            i += 1;
        }
    }

    /**
     * Stores the given annotation data to the settings object
     *
     * @param settings
     * @param annotationData
     */
    static void saveAnnotationData(final NodeSettingsWO settings, final AnnotationData annotationData) {
        settings.addString("text", annotationData.getText());
        settings.addInt("bgcolor", annotationData.getBgColor());
        settings.addInt("x-coordinate", annotationData.getX());
        settings.addInt("y-coordinate", annotationData.getY());
        settings.addInt("width", annotationData.getWidth());
        settings.addInt("height", annotationData.getHeight());
        settings.addString("alignment", annotationData.getAlignment().toString());
        settings.addInt("borderSize", annotationData.getBorderSize());
        settings.addInt("borderColor", annotationData.getBorderColor());
        settings.addInt("defFontSize", annotationData.getDefaultFontSize());
        settings.addInt("annotation-version", annotationData.getVersion());
        NodeSettingsWO styleConfigs = settings.addNodeSettings("styles");
        int i = 0;
        for (StyleRange sr : annotationData.getStyleRanges()) {
            NodeSettingsWO cur = styleConfigs.addNodeSettings("style_" + (i++));
            saveStyleRange(cur, sr);
        }
    }

    /**
     * Stores the given style range object to the settings object.
     */
    private static void saveStyleRange(final NodeSettingsWO settings, final StyleRange styleRange) {
        settings.addInt("start", styleRange.getStart());
        settings.addInt("length", styleRange.getLength());
        settings.addString("fontname", styleRange.getFontName());
        settings.addInt("fontstyle", styleRange.getFontStyle());
        settings.addInt("fontsize", styleRange.getFontSize());
        settings.addInt("fgcolor", styleRange.getFgColor());
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

    protected static void saveNodeContainer(final NodeSettingsWO settings, final ReferencedFile workflowDirRef,
        final NodeContainer container, final ExecutionMonitor exec, final WorkflowSaveHelper saveHelper)
        throws CanceledExecutionException, IOException, LockFailedException {
        WorkflowManager parent = container.getParent();
        ReferencedFile workingDir = parent.getNodeContainerDirectory();
        boolean isWorkingDir = workflowDirRef.equals(workingDir);

        saveNodeIDSuffix(settings, container);
        int idSuffix = container.getID().getIndex();

        // name of sub-directory container node/sub-workflow settings
        // all chars which are not letter or number are replaced by '_'
        final String containerName = container.getName();
        String nodeDirID =
            FileUtil.getValidFileName(containerName, container instanceof WorkflowManager
                || container instanceof SubNodeContainer ? 12 : -1);
        nodeDirID = nodeDirID.concat(" (#" + idSuffix + ")");

        // try to re-use previous node dir (might be different from calculated
        // one above in case node was renamed between releases)
        if (isWorkingDir && container.getNodeContainerDirectory() != null) {
            ReferencedFile ncDirectory = container.getNodeContainerDirectory();
            nodeDirID = ncDirectory.getFile().getName();
        }

        ReferencedFile nodeDirectoryRef = new ReferencedFile(workflowDirRef, nodeDirID);
        String fileName;
        if (container instanceof WorkflowManager) {
            fileName = FileWorkflowPersistor.save((WorkflowManager)container, nodeDirectoryRef, exec, saveHelper);
        } else {
            fileName =  FileSingleNodeContainerPersistor.save(
                (SingleNodeContainer)container, nodeDirectoryRef, exec, saveHelper);
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
            NodeSettingsWO subConfig = settings.addNodeSettings(CFG_UIINFO_SUB_CONFIG);
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

    protected static void saveInportsBarUIInfoClassName(final NodeSettingsWO settings, final NodeUIInformation info) {
        settings.addString(CFG_UIINFO_CLASS, info != null ? info.getClass().getName() : null);
    }

    protected static void saveInportsBarUIInfoSettings(final NodeSettingsWO settings, final NodeUIInformation uiInfo) {
        saveNodeUIInformation(settings, uiInfo);
    }

    protected static void saveInPort(final NodeSettingsWO settings, final WorkflowManager wm, final int portIndex) {
        WorkflowInPort inport = wm.getInPort(portIndex);
        settings.addInt("index", portIndex);
        settings.addString("name", inport.getPortName());
        NodeSettingsWO portTypeSettings = settings.addNodeSettings("type");
        inport.getPortType().save(portTypeSettings);
    }

    protected static NodeSettingsWO saveOutPortsSetting(final NodeSettingsWO settings) {
        return settings.addNodeSettings("meta_out_ports");
    }

    protected static NodeSettingsWO saveOutPortsEnumSetting(final NodeSettingsWO settings) {
        return settings.addNodeSettings("port_enum");
    }

    protected static void saveOutportsBarUIInfoClassName(final NodeSettingsWO settings, final NodeUIInformation info) {
        settings.addString(CFG_UIINFO_CLASS, info != null ? info.getClass().getName() : null);
    }

    protected static void saveOutportsBarUIInfoSettings(final NodeSettingsWO settings, final NodeUIInformation uiInfo) {
        saveNodeUIInformation(settings, uiInfo);
    }

    protected static NodeSettingsWO saveOutPortSetting(final NodeSettingsWO settings, final int portIndex) {
        return settings.addNodeSettings("outport_" + portIndex);
    }

    protected static void saveOutPort(final NodeSettingsWO settings, final WorkflowManager wm, final int portIndex) {
        WorkflowOutPort outport = wm.getOutPort(portIndex);
        settings.addInt("index", portIndex);
        settings.addString("name", outport.getPortName());
        NodeSettingsWO portTypeSettings = settings.addNodeSettings("type");
        outport.getPortType().save(portTypeSettings);
    }

    protected static void saveConnection(final NodeSettingsWO settings, final ConnectionContainer connection) {
        int sourceID = connection.getSource().getIndex();
        int destID = connection.getDest().getIndex();
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
        int sourcePort = connection.getSourcePort();
        settings.addInt("sourcePort", sourcePort);
        int targetPort = connection.getDestPort();
        settings.addInt("destPort", targetPort);
        ConnectionUIInformation uiInfo = connection.getUIInfo();
        if (uiInfo != null) {
            //TODO there is actually no need to store the class name - just keep it for now for backwards compatibility
            settings.addString(CFG_UIINFO_CLASS, uiInfo.getClass().getName());
            // nest into separate sub config
            NodeSettingsWO subConfig = settings.addNodeSettings(CFG_UIINFO_SUB_CONFIG);
            int[][] allBendpoints = uiInfo.getAllBendpoints();
            subConfig.addInt(KEY_BENDPOINTS + "_size", allBendpoints.length);
            for (int i = 0; i < allBendpoints.length; i++) {
                subConfig.addIntArray(KEY_BENDPOINTS + "_" + i, allBendpoints[i]);
            }
        }
        if (!connection.isDeletable()) {
            settings.addBoolean("isDeletable", false);
        }
    }


}
