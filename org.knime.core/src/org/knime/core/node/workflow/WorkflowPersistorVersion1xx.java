/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2010
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
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
 * --------------------------------------------------------------------- *
 *
 * History
 *   Sep 11, 2007 (wiswedel): created
 */
package org.knime.core.node.workflow;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.TreeMap;

import org.knime.core.data.container.ContainerTable;
import org.knime.core.eclipseUtil.GlobalClassCreator;
import org.knime.core.internal.ReferencedFile;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.Node;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortType;
import org.knime.core.node.workflow.WorkflowPersistorVersion200.LoadVersion;

/**
 *
 * @author wiswedel, University of Konstanz
 */
class WorkflowPersistorVersion1xx implements WorkflowPersistor {

    /** The node logger for this class. */
    private final NodeLogger m_logger = NodeLogger.getLogger(getClass());

    private static final PortType FALLBACK_PORTTYPE =
        new PortType(PortObject.class);

    private static final NodeSettingsRO EMPTY_SETTINGS =
        new NodeSettings("<<empty>>");

    private final String m_versionString;
    private final TreeMap<Integer, NodeContainerPersistor>
        m_nodeContainerLoaderMap;

    private final HashSet<ConnectionContainerTemplate> m_connectionSet;

    private NodeContainerMetaPersistor m_metaPersistor;

    private final HashMap<Integer, ContainerTable> m_globalTableRepository;

    private WorkflowPortTemplate[] m_inPortTemplates;
    private WorkflowPortTemplate[] m_outPortTemplates;
    private UIInformation m_inPortsBarUIInfo;
    private UIInformation m_outPortsBarUIInfo;

    private String m_name;
    private List<FlowVariable> m_workflowVariables;
    private List<Credentials> m_credentials;
    private List<WorkflowAnnotation> m_workflowAnnotations;

    private boolean m_needsResetAfterLoad;
    private boolean m_isDirtyAfterLoad;
    private boolean m_mustWarnOnDataLoadError;

    private NodeSettingsRO m_workflowSett;
    private ReferencedFile m_workflowDir;
    private List<ReferencedFile> m_obsoleteNodeDirectories;

    static boolean canReadVersion(final String versionString) {
        boolean result = versionString.equals("0.9.0");
        result |= versionString.equals("1.0");
        result |= versionString.matches("1\\.[01234]\\.[0-9].*");
        return result;
    }

    WorkflowPersistorVersion1xx(final HashMap<Integer, ContainerTable> tableRep,
            final String versionString) {
        m_globalTableRepository = tableRep;
        m_versionString = versionString;
        m_nodeContainerLoaderMap =
            new TreeMap<Integer, NodeContainerPersistor>();
        m_connectionSet = new HashSet<ConnectionContainerTemplate>();
        m_obsoleteNodeDirectories = new ArrayList<ReferencedFile>();
    }

    protected final String getVersionString() {
        return m_versionString;
    }

    protected NodeLogger getLogger() {
        return m_logger;
    }

    /** {@inheritDoc} */
    @Override
    public String getLoadVersionString() {
        return getVersionString();
    }

    /** @return load version, never null. */
    LoadVersion getLoadVersion() {
        // returns non-null version (asserted in constructor)
        return LoadVersion.get(getVersionString());
    }
    /**
     * @return the workflowDir
     */
    protected ReferencedFile getWorkflowDir() {
        return m_workflowDir;
    }

    /** {@inheritDoc} */
    @Override
    public Set<ConnectionContainerTemplate> getConnectionSet() {
        return m_connectionSet;
    }

    /** {@inheritDoc} */
    @Override
    public Map<Integer, NodeContainerPersistor> getNodeLoaderMap() {
        return m_nodeContainerLoaderMap;
    }

    /** {@inheritDoc} */
    @Override
    public boolean mustWarnOnDataLoadError() {
        return m_mustWarnOnDataLoadError;
    }

    /** {@inheritDoc} */
    @Override
    public NodeContainerMetaPersistor getMetaPersistor() {
        return m_metaPersistor;
    }

    /** {@inheritDoc} */
    @Override
    public HashMap<Integer, ContainerTable> getGlobalTableRepository() {
        return m_globalTableRepository;
    }

    /** {@inheritDoc} */
    @Override
    public NodeContainer getNodeContainer(final WorkflowManager parent,
            final NodeID id) {
        return parent.createSubWorkflow(this, id);
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return m_name;
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

    /** {@inheritDoc} */
    @Override
    public List<WorkflowAnnotation> getWorkflowAnnotations() {
        return m_workflowAnnotations;
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
    public UIInformation getInPortsBarUIInfo() {
        return m_inPortsBarUIInfo;
    }

    /** {@inheritDoc} */
    @Override
    public UIInformation getOutPortsBarUIInfo() {
        return m_outPortsBarUIInfo;
    }

    /** {@inheritDoc} */
    @Override
    public boolean needsResetAfterLoad() {
        return m_needsResetAfterLoad;
    }

    /** Indicate that this node should better be reset after load.
     * (Due to loading problems).
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
        return false;
    }

    /** Mark node as dirty. */
    protected void setDirtyAfterLoad() {
        m_isDirtyAfterLoad = true;
    }

    /** {@inheritDoc} */
    @Override
    public void preLoadNodeContainer(final ReferencedFile nodeFileRef,
            final NodeSettingsRO parentSettings, final LoadResult loadResult,
            final WorkflowLoadHelper loadHelper)
    throws InvalidSettingsException, IOException {
        if (nodeFileRef == null || !nodeFileRef.getFile().isFile()) {
            setDirtyAfterLoad();
            String error = "Can't read workflow file \"" + nodeFileRef + "\"";
            throw new IOException(error);
        }
        File nodeFile = nodeFileRef.getFile();
        ReferencedFile parentRef = nodeFileRef.getParent();
        if (parentRef == null) {
            setDirtyAfterLoad();
            throw new IOException("Parent directory of file \"" + nodeFileRef
                    + "\" is not represented by "
                    + ReferencedFile.class.getSimpleName() + " object");
        }
        m_mustWarnOnDataLoadError =
            loadIfMustWarnOnDataLoadError(parentRef.getFile());
        m_metaPersistor = createNodeContainerMetaPersistor(parentRef);
        NodeSettingsRO subWFSettings;
        try {
            InputStream in = new BufferedInputStream(
                    new FileInputStream(nodeFile));
            subWFSettings = NodeSettings.loadFromXML(in);
        } catch (IOException ioe) {
            setDirtyAfterLoad();
            throw ioe;
        }
        m_workflowSett = subWFSettings;
        m_workflowDir = parentRef;

        try {
            m_name = loadWorkflowName(m_workflowSett);
        } catch (InvalidSettingsException e) {
            String error = "Unable to load workflow name: " + e.getMessage();
            getLogger().debug(error, e);
            setDirtyAfterLoad();
            loadResult.addError(error);
            m_name = null;
        }

        try {
            m_workflowVariables = loadWorkflowVariables(m_workflowSett);
        } catch (InvalidSettingsException e) {
            String error =
                "Unable to load workflow variables: " + e.getMessage();
            getLogger().debug(error, e);
            setDirtyAfterLoad();
            loadResult.addError(error);
            m_workflowVariables = Collections.emptyList();
        }

        try {
            m_credentials = loadCredentials(m_workflowSett);
            // request to initialize credentials - if available
            if (m_credentials != null && !m_credentials.isEmpty()) {
                m_credentials = loadHelper.loadCredentials(m_credentials);
            }
        } catch (InvalidSettingsException e) {
            String error =
                "Unable to load credentials: " + e.getMessage();
            getLogger().debug(error, e);
            setDirtyAfterLoad();
            loadResult.addError(error);
            m_credentials = Collections.emptyList();
        }

        try {
            m_workflowAnnotations = loadWorkflowAnnotations(m_workflowSett);
        } catch (InvalidSettingsException e) {
            String error =
                "Unable to load workflow annotations: " + e.getMessage();
            getLogger().debug(error, e);
            setDirtyAfterLoad();
            loadResult.addError(error);
            m_workflowAnnotations = Collections.emptyList();
        }

        NodeSettingsRO metaFlowParentSettings =
            new NodeSettings("fake_parent_settings");
        try {
            metaFlowParentSettings = readParentSettings();
        } catch (IOException e1) {
            String error = "Errors reading settings file: " + e1.getMessage();
            getLogger().warn(error, e1);
            setDirtyAfterLoad();
            loadResult.addError(error);
        }
        boolean isResetRequired = m_metaPersistor.load(
                subWFSettings, metaFlowParentSettings, loadResult);
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
                String error = "Can't load workflow inport (internal ID \""
                    + key + "\", skipping it: " + e.getMessage();
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
                loadResult.addError(
                        "Duplicate inport definition for index: " + index);
            }
            m_inPortTemplates[index] = p;
        }
        for (int i = 0; i < m_inPortTemplates.length; i++) {
            if (m_inPortTemplates[i] == null) {
                setDirtyAfterLoad();
                loadResult.addError("Assigning fallback port type for "
                        + "missing input port " + i);
                m_inPortTemplates[i] =
                    new WorkflowPortTemplate(i, FALLBACK_PORTTYPE);
            }
        }

        NodeSettingsRO outPortsEnum = EMPTY_SETTINGS;
        try {
            NodeSettingsRO outPorts = loadOutPortsSetting(m_workflowSett);
            if (outPorts != null) {
                outPortsEnum = loadOutPortsSettingsEnum(outPorts);
            }
        } catch (InvalidSettingsException e) {
            String error = "Can't load workflow out ports, config not found: "
                + e.getMessage();
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
                String error = "Can't load workflow outport (internal ID \""
                    + key + "\", skipping it: " + e.getMessage();
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
                loadResult.addError(
                        "Duplicate outport definition for index: " + index);
            }
            m_outPortTemplates[index] = p;
        }
        for (int i = 0; i < m_outPortTemplates.length; i++) {
            if (m_outPortTemplates[i] == null) {
                setDirtyAfterLoad();
                loadResult.addError("Assigning fallback port type for "
                        + "missing output port " + i);
                m_outPortTemplates[i] =
                    new WorkflowPortTemplate(i, FALLBACK_PORTTYPE);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public void loadNodeContainer(
            final Map<Integer, BufferedDataTable> tblRep,
            final ExecutionMonitor exec, final LoadResult loadResult)
            throws CanceledExecutionException, IOException {
        if (m_workflowDir == null || m_workflowSett == null) {
            setDirtyAfterLoad();
            throw new IllegalStateException("The method preLoadNodeContainer "
                    + "has either not been called or failed");
        }
        /* read nodes */
        NodeSettingsRO nodes;
        try {
            nodes = loadSettingsForNodes(m_workflowSett);
        } catch (InvalidSettingsException e) {
            String error = "Can't load nodes in workflow, config not found: "
                + e.getMessage();
            getLogger().debug(error, e);
            loadResult.addError(error);
            setDirtyAfterLoad();
            setNeedsResetAfterLoad();
            // stop loading here
            return;
        }
        exec.setMessage("node information");
        /* Load nodes */
        for (String nodeKey : nodes.keySet()) {
            NodeSettingsRO nodeSetting;
            try {
                nodeSetting = nodes.getNodeSettings(nodeKey);
            } catch (InvalidSettingsException e) {
                String error = "Unable to load settings for node with internal "
                    + "id \"" + nodeKey + "\": " + e.getMessage();
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
                String error = "Unable to load node ID (internal id \""
                    + nodeKey + "\"), trying random number " + nodeIDSuffix
                    + "instead: " + e.getMessage();
                getLogger().debug(error, e);
                setDirtyAfterLoad();
                loadResult.addError(error);
            }
            boolean isMeta;
            try {
                isMeta = loadIsMetaNode(nodeSetting);
            } catch (InvalidSettingsException e) {
                String error = "Can't retrieve meta flag for contained node "
                    + "with id suffix " + nodeIDSuffix + ", attempting to read"
                    + "ordinary (not-meta) node: " + e.getMessage();
                getLogger().debug(error, e);
                setDirtyAfterLoad();
                loadResult.addError(error);
                isMeta = false;
            }
            NodeContainerPersistor persistor;
            if (isMeta) {
                persistor = createWorkflowPersistor();
            } else {
                persistor = createSingleNodeContainerPersistor();
            }
            UIInformation uiInfo = null;
            String uiInfoClassName;
            try {
                uiInfoClassName = loadUIInfoClassName(nodeSetting);
            } catch (InvalidSettingsException e) {
                String error = "Unable to load UI information class name "
                    + "to node with ID suffix " + nodeIDSuffix
                    + ", no UI information available: " + e.getMessage();
                getLogger().debug(error, e);
                setDirtyAfterLoad();
                loadResult.addError(error);
                uiInfoClassName = null;
            }
            if (uiInfoClassName != null) {
                uiInfo = loadUIInfoInstance(uiInfoClassName);
                if (uiInfo != null) {
                    try {
                        loadUIInfoSettings(uiInfo, nodeSetting);
                    } catch (InvalidSettingsException e) {
                        String error = "Unable to load UI information to "
                            + "node with ID suffix " + nodeIDSuffix
                            + ", no UI information available: "
                            + e.getMessage();
                        getLogger().debug(error, e);
                        setDirtyAfterLoad();
                        loadResult.addError(error);
                        uiInfo = null;
                    }
                }
            }
            ReferencedFile nodeFile;
            try {
                nodeFile = loadNodeFile(nodeSetting, m_workflowDir);
            } catch (InvalidSettingsException e) {
                String error = "Unable to load settings for node "
                    + "with ID suffix " + nodeIDSuffix + ": " + e.getMessage();
                getLogger().debug(error, e);
                setDirtyAfterLoad();
                loadResult.addError(error);
                continue;
            }
            try {
                LoadResult childResult = new LoadResult((isMeta ? "meta " : "")
                        + "node with ID suffix " + nodeIDSuffix);
                persistor.preLoadNodeContainer(
                        nodeFile, nodeSetting, childResult, null);
                loadResult.addChildError(childResult);
            } catch (Throwable e) {
                String error = "Unable to load node with ID suffix "
                        + nodeIDSuffix + " into workflow, skipping it: "
                        + e.getMessage();
                if (e instanceof InvalidSettingsException
                        || e instanceof IOException) {
                    getLogger().debug(error, e);
                } else {
                    getLogger().error(error, e);
                }
                // node directory is the parent of the settings.xml
                m_obsoleteNodeDirectories.add(nodeFile.getParent());
                setDirtyAfterLoad();
                loadResult.addError(error);
                continue;
            }
            NodeContainerMetaPersistor meta = persistor.getMetaPersistor();
            if (m_nodeContainerLoaderMap.containsKey(nodeIDSuffix)) {
                int randomID = getRandomNodeID();
                setDirtyAfterLoad();
                loadResult.addError("Duplicate id encountered in workflow: "
                        + nodeIDSuffix + ", uniquifying to random id "
                        + randomID + ", this possibly screws the connections");
                nodeIDSuffix = randomID;
            }
            meta.setNodeIDSuffix(nodeIDSuffix);
            meta.setUIInfo(uiInfo);
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
            String error = "Can't load workflow connections, config not found: "
                + e.getMessage();
            getLogger().debug(error, e);
            setDirtyAfterLoad();
            loadResult.addError(error);
            connections = EMPTY_SETTINGS;
        }
        for (String connectionKey : connections.keySet()) {
            ConnectionContainerTemplate c;
            try {
                c = loadConnection(connections.getNodeSettings(connectionKey));
            } catch (InvalidSettingsException e) {
                String error = "Can't load connection with internal ID \""
                    + connectionKey + "\": " + e.getMessage();
                getLogger().debug(error, e);
                setDirtyAfterLoad();
                loadResult.addError(error);
                continue;
            }
            int sourceIDSuffix = c.getSourceSuffix();
            NodeContainerPersistor sourceNodePersistor =
                m_nodeContainerLoaderMap.get(sourceIDSuffix);
            if (sourceNodePersistor == null && sourceIDSuffix != -1) {
                setDirtyAfterLoad();
                loadResult.addError("Unable to load node connection " + c
                        + ", source node does not exist");
            }
            fixSourcePortIfNecessary(sourceNodePersistor, c);

            int destIDSuffix = c.getDestSuffix();
            NodeContainerPersistor destNodePersistor =
                m_nodeContainerLoaderMap.get(destIDSuffix);
            if (destNodePersistor == null && destIDSuffix != -1) {
                setDirtyAfterLoad();
                loadResult.addError("Unable to load node connection " + c
                        + ", destination node does not exist");
            }
            fixDestPortIfNecessary(destNodePersistor, c);

            if (!m_connectionSet.add(c)) {
                setDirtyAfterLoad();
                loadResult.addError(
                        "Duplicate connection information: " + c);
            }
        }
        NodeSettingsRO inPorts = EMPTY_SETTINGS;
        UIInformation inPortsBarUIInfo = null;
        String uiInfoClassName = null;
        try {
            inPorts = loadInPortsSetting(m_workflowSett);
            if (inPorts != null) {
                uiInfoClassName = loadInPortsBarUIInfoClassName(inPorts);
            }
        } catch (InvalidSettingsException e) {
            String error =
                "Unable to load class name for inport bar's "
                    + "UI information: " + e.getMessage();
            getLogger().debug(error, e);
            setDirtyAfterLoad();
            loadResult.addError(error);
        }
        if (uiInfoClassName != null) {
            inPortsBarUIInfo = loadUIInfoInstance(uiInfoClassName);
            if (inPortsBarUIInfo != null) {
                try {
                    loadInPortsBarUIInfo(inPortsBarUIInfo, inPorts);
                } catch (InvalidSettingsException e) {
                    String error =
                        "Unable to load inport bar's UI information: "
                        + e.getMessage();
                    getLogger().debug(error, e);
                    setDirtyAfterLoad();
                    loadResult.addError(error);
                    inPortsBarUIInfo = null;
                }
            }
        }
        NodeSettingsRO outPorts = null;
        m_inPortsBarUIInfo = inPortsBarUIInfo;
        UIInformation outPortsBarUIInfo = null;
        uiInfoClassName = null;
        try {
            outPorts = loadOutPortsSetting(m_workflowSett);
            if (outPorts != null) {
                uiInfoClassName = loadOutPortsBarUIInfoClassName(outPorts);
            }
        } catch (InvalidSettingsException e) {
            String error =
                    "Unable to load class name for outport bar's UI information"
                            + ", no UI information available: "
                            + e.getMessage();
            setDirtyAfterLoad();
            getLogger().debug(error, e);
            loadResult.addError(error);
        }
        if (uiInfoClassName != null) {
            outPortsBarUIInfo = loadUIInfoInstance(uiInfoClassName);
            if (outPortsBarUIInfo != null) {
                try {
                    loadOutPortsBarUIInfo(outPortsBarUIInfo, outPorts);
                } catch (InvalidSettingsException e) {
                    String error =
                        "Unable to load outport bar's UI information: "
                        + e.getMessage();
                    getLogger().debug(error, e);
                    setDirtyAfterLoad();
                    loadResult.addError(error);
                    outPortsBarUIInfo = null;
                }
            }
        }
        m_outPortsBarUIInfo = outPortsBarUIInfo;
        exec.setProgress(1.0);
    }

    /** Fixes source port index if necessary. Fixes the mandatory
     * flow variable port object.
     * @param sourcePersistor The persistor of the source node.
     * @param c The connection template to be fixed.
     */
    protected void fixSourcePortIfNecessary(
            final NodeContainerPersistor sourcePersistor,
            final ConnectionContainerTemplate c) {
        if (sourcePersistor
                instanceof SingleNodeContainerPersistorVersion1xx) {
            // correct port index only for ordinary nodes (no new flow
            // variable ports on meta nodes)
            int index = c.getSourcePort();
            c.setSourcePort(index + 1);
        }
    }

    /** Fixes destination port index if necessary. For v1.x flows, e.g.,
     * the indices of model and data ports were swapped.
     * Subclasses will overwrite this method (e.g. to enable loading flows,
     * which did not have the mandatory flow variable port object).
     * @param destPersistor The persistor of the destination node.
     * @param c The connection template to be fixed.
     */
    protected void fixDestPortIfNecessary(
            final NodeContainerPersistor destPersistor,
            final ConnectionContainerTemplate c) {
        if (destPersistor instanceof SingleNodeContainerPersistorVersion1xx) {
            SingleNodeContainerPersistorVersion1xx pers =
                (SingleNodeContainerPersistorVersion1xx)destPersistor;
            /* workflows saved with 1.x.x have misleading port indices for
             * incoming ports. Data ports precede the model ports (in their
             * index), although the GUI and the true ordering is the other
             * way around. */
            if (pers.shouldFixModelPortOrder()) {
                Node node = pers.getNode();
                int modelPortCount = 0;
                // first port is flow variable input port
                for (int i = 1; i < node.getNrInPorts(); i++) {
                    if (!node.getInputType(i).getPortObjectClass().
                            isAssignableFrom(BufferedDataTable.class)) {
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
            // variable ports on meta nodes)
            int index = c.getDestPort();
            c.setDestPort(index + 1);
        }
    }



    protected NodeSettingsRO readParentSettings() throws IOException {
        NodeSettings result = new NodeSettings("generated_wf_settings");
        result.addBoolean("isExecuted", false);
        result.addBoolean("isConfigured", false);
        return result;
    }

    /** This is overridden by the meta node loader (1.x.x) and returns
     * true for the "special" nodes.
     * @param settings node sub-element
     * @return true if to skip (though in 99.9% false)
     */
    protected boolean shouldSkipThisNode(final NodeSettingsRO settings) {
        return false;
    }

    protected int loadNodeIDSuffix(final NodeSettingsRO settings)
        throws InvalidSettingsException {
        return settings.getInt(KEY_ID);
    }

    protected String loadUIInfoClassName(final NodeSettingsRO settings)
    throws InvalidSettingsException {
        if (settings.containsKey(KEY_UI_INFORMATION)) {
            return settings.getString(KEY_UI_INFORMATION);
        }
        return null;
    }

    protected String fixUIInfoClassName(final String name) {
        if (("org.knime.workbench.editor2.extrainfo."
                + "ModellingNodeExtraInfo").equals(name)) {
            return "org.knime.core.node.workflow.NodeUIInformation";
        } else if (("org.knime.workbench.editor2.extrainfo."
                + "ModellingConnectionExtraInfo").equals(name)) {
            return "org.knime.core.node.workflow.ConnectionUIInformation";
        }
        return name;
    }

    /**
     * Creates the <code>UIInformaion</code> from given settings, describing
     * whatever additional information was stored (graphical layout?).
     *
     * @param className The name of the class to be loaded.
     * @return new <code>UIInformation</code> object or null
     */
    protected UIInformation loadUIInfoInstance(final String className) {
        if (className == null) {
            return null;
        }
        String fixedName = fixUIInfoClassName(className);
        try {
            // avoid NoClassDefFoundErrors by using magic class loader
            return (UIInformation)(GlobalClassCreator
                    .createClass(fixedName).newInstance());
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

    protected void loadUIInfoSettings(final UIInformation uiInfo,
            final NodeSettingsRO settings) throws InvalidSettingsException {
        uiInfo.load(settings, getLoadVersion());
    }

    /** Sub class hook o read port bar info.
     * @param settings Ignored.
     * @return null
     * @throws InvalidSettingsException Not actually thrown here.
     */
    protected String loadInPortsBarUIInfoClassName(
            final NodeSettingsRO settings) throws InvalidSettingsException {
        return null;
    }

    /** Sub-class hook to load port bar info.
     * @param uiInfo Ignored.
     * @param settings Ignored.
     * @throws InvalidSettingsException Not actually thrown
     */
    protected void loadInPortsBarUIInfo(final UIInformation uiInfo,
            final NodeSettingsRO settings) throws InvalidSettingsException {
        // sub classes override this
    }

    /** Sub-class hook to load port bar info.
     * @param settings Ignored.
     * @return null
     * @throws InvalidSettingsException Not actually thrown
     */
    protected String loadOutPortsBarUIInfoClassName(
            final NodeSettingsRO settings) throws InvalidSettingsException {
        // sub classes override this
        return null;
    }

    /** Load output port bars. This implementation does nothing, sub-classes
     * override this method.
     * @param uiInfo Ignored here.
     * @param settings Ignored here.
     * @throws InvalidSettingsException Not actually thrown here.
     */
    protected void loadOutPortsBarUIInfo(final UIInformation uiInfo,
            final NodeSettingsRO settings) throws InvalidSettingsException {
    }

    protected ReferencedFile loadNodeFile(final NodeSettingsRO settings,
            final ReferencedFile workflowDirRef) throws InvalidSettingsException {
        String fileString = settings.getString("node_settings_file");
        if (fileString == null) {
            throw new InvalidSettingsException("Unable to read settings "
                    + "file for node " + settings.getKey());
        }
        File workflowDir = workflowDirRef.getFile();
        // fileString is something like "File Reader(#1)/settings.xml", thus
        // it contains two levels of the hierarchy. We leave it here to the
        // java.io.File implementation to resolve these levels
        File fullFile = new File(workflowDir, fileString);
        if (!fullFile.isFile() || !fullFile.canRead()) {
            throw new InvalidSettingsException("Unable to read settings "
                    + "file " + fullFile.getAbsolutePath());
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

    protected boolean loadIsMetaNode(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        String factory = settings.getString("factory");
        return ObsoleteMetaNodeWorkflowPersistorVersion1xx.
            OLD_META_NODES.contains(factory);
    }

    protected NodeSettingsRO loadSettingsForConnections(final NodeSettingsRO set)
            throws InvalidSettingsException {
        return set.getNodeSettings(KEY_CONNECTIONS);
    }

    protected ConnectionContainerTemplate loadConnection(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        int sourceID = settings.getInt("sourceID");
        int destID = loadConnectionDestID(settings);
        int sourcePort = loadConnectionSourcePort(settings);
        int destPort = loadConnectionDestPort(settings);
        // this attribute is in most cases not present (not saved)
        boolean isDeletable = settings.getBoolean("isDeletable", true);
        if (sourceID != -1 && sourceID == destID) {
            throw new InvalidSettingsException("Source and Destination must "
                    + "not be equal, id is " + sourceID);
        }
        UIInformation uiInfo = null;
        try {
            String uiInfoClass = loadUIInfoClassName(settings);
            uiInfo = loadUIInfoInstance(uiInfoClass);
            if (uiInfo != null) {
                loadUIInfoSettings(uiInfo, settings);
            }
        } catch (InvalidSettingsException ise) {
            getLogger().debug("Could not load UI information for connection "
                    + "between nodes " + sourceID + " and " + destID);
        } catch (Throwable t) {
            getLogger().warn("Exception while loading connection UI "
                    + "information between nodes " + sourceID + " and "
                    + destID, t);
        }
        return new ConnectionContainerTemplate(sourceID, sourcePort, destID,
                destPort, isDeletable, uiInfo);
    }

    protected int loadConnectionDestID(final NodeSettingsRO settings)
        throws InvalidSettingsException {
        return settings.getInt("targetID");
    }

    protected int loadConnectionDestPort(final NodeSettingsRO settings)
    throws InvalidSettingsException {
        return settings.getInt("targetPort");
    }

    protected int loadConnectionSourcePort(final NodeSettingsRO settings)
    throws InvalidSettingsException {
        return settings.getInt("sourcePort");
    }

    protected NodeSettingsRO loadSettingsForNodes(final NodeSettingsRO set)
            throws InvalidSettingsException {
        return set.getNodeSettings(KEY_NODES);
    }

    /** Sub class hook o read workflow name.
     * @param set Ignored.
     * @return "Workflow Manager"
     * @throws InvalidSettingsException Not actually thrown here.
     */
    protected String loadWorkflowName(final NodeSettingsRO set)
            throws InvalidSettingsException {
        return "Workflow Manager";
    }

    /**
     * Load workflow variables (not available in 1.3.x flows).
     * @param settings To load from.
     * @return The variables in a list.
     * @throws InvalidSettingsException If any settings-related error occurs.
     */
    protected List<FlowVariable> loadWorkflowVariables(
            final NodeSettingsRO settings) throws InvalidSettingsException {
        return Collections.emptyList();
    }

    /** Loads credentials, this method returns an empty list.
     * Credentials added for v2.2
     * @param settings to load from.
     * @return the credentials list
     * @throws InvalidSettingsException If this fails for any reason.
     */
    protected List<Credentials> loadCredentials(
            final NodeSettingsRO settings) throws InvalidSettingsException {
        return Collections.emptyList();
    }

    /** Load annotations (added in v2.3).
     * @param workflowSett to load from
     * @return non-null list.
     * @throws InvalidSettingsException If this fails for any reason.
     */
    protected List<WorkflowAnnotation> loadWorkflowAnnotations(
            final NodeSettingsRO workflowSett) throws InvalidSettingsException {
        return Collections.emptyList();
    }

    /** Sub class hook o read port settings.
     * @param settings Ignored.
     * @return null
     * @throws InvalidSettingsException Not actually thrown here.
     */
    protected NodeSettingsRO loadInPortsSetting(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        return null;
    }

    /** Sub class hook o read port settings.
     * @param settings Ignored.
     * @return null
     * @throws InvalidSettingsException Not actually thrown here.
     */
    protected NodeSettingsRO loadInPortsSettingsEnum(
            final NodeSettingsRO settings) throws InvalidSettingsException {
        return null;
    }

    /** Sub class hook o read port settings.
     * @param settings Ignored.
     * @return null
     * @throws InvalidSettingsException Not actually thrown here.
     */
    protected WorkflowPortTemplate loadInPortTemplate(
            final NodeSettingsRO settings)
            throws InvalidSettingsException {
        throw new InvalidSettingsException(
                "No ports for meta nodes in version 1.x.x");
    }

    /** Sub class hook o read port settings.
     * @param settings Ignored.
     * @return null
     * @throws InvalidSettingsException Not actually thrown here.
     */
    protected NodeSettingsRO loadOutPortsSetting(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        return null;
    }

    /** Sub class hook o read port settings.
     * @param settings Ignored.
     * @return null
     * @throws InvalidSettingsException Not actually thrown here.
     */
    protected NodeSettingsRO loadOutPortsSettingsEnum(
            final NodeSettingsRO settings) throws InvalidSettingsException  {
        return null;
    }

    /** Sub class hook o read port settings.
     * @param settings Ignored.
     * @return null
     * @throws InvalidSettingsException Not actually thrown here.
     */
    protected WorkflowPortTemplate loadOutPortTemplate(
            final NodeSettingsRO settings) throws InvalidSettingsException {
        throw new InvalidSettingsException(
            "No ports for meta nodes in version 1.x.x");
    }

    /** Sub class hook to check whether there is a "loaded with no data" file.
     * @param workflowFile Ignored here
     * @return true
     */
    protected boolean loadIfMustWarnOnDataLoadError(final File workflowFile) {
        return true;
    }

    protected NodeContainerMetaPersistorVersion1xx
            createNodeContainerMetaPersistor(final ReferencedFile baseDir) {
        return new NodeContainerMetaPersistorVersion1xx(baseDir);
    }

    protected SingleNodeContainerPersistorVersion1xx
            createSingleNodeContainerPersistor() {
        return new SingleNodeContainerPersistorVersion1xx(
                this, getVersionString());
    }

    protected WorkflowPersistorVersion1xx createWorkflowPersistor() {
        return new ObsoleteMetaNodeWorkflowPersistorVersion1xx(
                getGlobalTableRepository(), getVersionString());
    }

    private int getRandomNodeID() {
        // some number between 10k and 20k, hopefully unique.
        int nodeIDSuffix = 10000 + (int)(Math.random() * 10000);
        while (m_nodeContainerLoaderMap.containsKey(nodeIDSuffix)) {
            nodeIDSuffix += 1;
        }
        return nodeIDSuffix;
    }

}
