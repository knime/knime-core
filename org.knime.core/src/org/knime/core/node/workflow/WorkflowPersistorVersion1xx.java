/* 
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
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
import java.util.HashMap;
import java.util.HashSet;
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
import org.knime.core.node.PortObject;
import org.knime.core.node.PortType;

/**
 * 
 * @author wiswedel, University of Konstanz
 */
class WorkflowPersistorVersion1xx implements WorkflowPersistor {

    /** The node logger for this class. */
    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(WorkflowPersistorVersion1xx.class);
    
    private static final PortType FALLBACK_PORTTYPE = 
        new PortType(PortObject.class);
    
    private static final NodeSettingsRO EMPTY_SETTINGS = 
        new NodeSettings("<<empty>>");

    private final TreeMap<Integer, NodeContainerPersistor> 
        m_nodeContainerLoaderMap;

    private final HashSet<ConnectionContainerTemplate> m_connectionSet;
    
    private NodeContainerMetaPersistor m_metaPersistor;
    
    private final HashMap<Integer, ContainerTable> m_globalTableRepository;
    
    private WorkflowPortTemplate[] m_inPortTemplates;
    private WorkflowPortTemplate[] m_outPortTemplates;
    private UIInformation m_inPortsBarUIInfo;
    private UIInformation m_outPortsBarUIInfo;
    
    private String m_loadVersion;
    private String m_name;
    
    private boolean m_needsResetAfterLoad;
    
    private NodeSettingsRO m_workflowSett;
    private ReferencedFile m_workflowDir;
    
    static boolean canReadVersion(final String versionString) {
        boolean result = versionString.equals("0.9.0");
        result |= versionString.equals("1.0");
        result |= versionString.matches("1\\.[01234]\\.[0-9].*");
        return result;
    }
    
    WorkflowPersistorVersion1xx(
            final HashMap<Integer, ContainerTable> tableRep) {
        m_globalTableRepository = tableRep;
        m_nodeContainerLoaderMap = 
            new TreeMap<Integer, NodeContainerPersistor>();
        m_connectionSet = new HashSet<ConnectionContainerTemplate>();
    }
    
    /** {@inheritDoc} */
    public String getLoadVersion() {
        return m_loadVersion;
    }

    /** {@inheritDoc} */
    public Set<ConnectionContainerTemplate> getConnectionSet() {
        return m_connectionSet;
    }

    /** {@inheritDoc} */
    public Map<Integer, NodeContainerPersistor> getNodeLoaderMap() {
        return m_nodeContainerLoaderMap;
    }
    
    /** {@inheritDoc} */
    public NodeContainerMetaPersistor getMetaPersistor() {
        return m_metaPersistor;
    }
    
    /** {@inheritDoc} */
    public HashMap<Integer, ContainerTable> getGlobalTableRepository() {
        return m_globalTableRepository;
    }
    
    /** {@inheritDoc} */
    public NodeContainer getNodeContainer(final WorkflowManager parent, 
            final NodeID id) {
        return parent.createSubWorkflow(this, id);
    }
    
    /** {@inheritDoc} */
    public String getName() {
        return m_name;
    }
    
    /** {@inheritDoc} */
    public WorkflowPortTemplate[] getInPortTemplates() {
        return m_inPortTemplates;
    }
    
    /** {@inheritDoc} */
    public WorkflowPortTemplate[] getOutPortTemplates() {
        return m_outPortTemplates;
    }
    
    /** {@inheritDoc} */
    public UIInformation getInPortsBarUIInfo() {
        return m_inPortsBarUIInfo;
    }
    
    /** {@inheritDoc} */
    public UIInformation getOutPortsBarUIInfo() {
        return m_outPortsBarUIInfo;
    }
    
    /** {@inheritDoc} */
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
    public LoadResult preLoadNodeContainer(final ReferencedFile nodeFileRef, 
            final NodeSettingsRO parentSettings) 
    throws InvalidSettingsException, CanceledExecutionException, IOException {
        LoadResult loadResult = new LoadResult();
        if (nodeFileRef == null || !nodeFileRef.getFile().isFile()) {
            String error = "Can't read workflow file \"" + nodeFileRef + "\"";
            throw new IOException(error);
        }
        File nodeFile = nodeFileRef.getFile();
        ReferencedFile parentRef = nodeFileRef.getParent();
        if (parentRef == null) {
            throw new IOException("Parent directory of file \"" + nodeFileRef 
                    + "\" is not represented by " 
                    + ReferencedFile.class.getSimpleName() + " object");
        }
        m_metaPersistor = createNodeContainerMetaPersistor(parentRef);
        InputStream in = new BufferedInputStream(new FileInputStream(nodeFile));
        NodeSettingsRO subWFSettings = NodeSettings.loadFromXML(in);
        m_workflowSett = subWFSettings;
        m_workflowDir = parentRef;

        try {
            m_loadVersion = loadVersion(m_workflowSett);
        } catch (InvalidSettingsException e) {
            String error = "Unable to load version string: " + e.getMessage();
            LOGGER.debug(error, e);
            loadResult.addError(error);
            // this will enforce the WFM to save everything from scratch
            m_loadVersion = "1.3.0";
        }
        
        try {
            m_name = loadWorkflowName(m_workflowSett);
        } catch (InvalidSettingsException e) {
            String error = "Unable to load workflow name: " + e.getMessage();
            LOGGER.debug(error, e);
            loadResult.addError(error);
            m_name = "Workflow";
        }
        LoadResult metaLoadResult = m_metaPersistor.load(subWFSettings);
        if (metaLoadResult.hasErrors()) {
            loadResult.addError(metaLoadResult);
            setNeedsResetAfterLoad();
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
            LOGGER.debug(error, e);
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
                LOGGER.debug(error, e);
                loadResult.addError(error);
                setNeedsResetAfterLoad();
                continue;
            }
            int index = p.getPortIndex();
            if (index < 0 || index >= inPortCount) {
                loadResult.addError("Invalid inport index " + index);
                setNeedsResetAfterLoad();
                continue;
            }
            if (m_inPortTemplates[index] != null) {
                loadResult.addError(
                        "Duplicate inport definition for index: " + index);
            }
            m_inPortTemplates[index] = p;
        }
        for (int i = 0; i < m_inPortTemplates.length; i++) {
            if (m_inPortTemplates[i] == null) {
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
            LOGGER.debug(error, e);
            loadResult.addError(error);
            setNeedsResetAfterLoad();
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
                LOGGER.debug(error, e);
                loadResult.addError(error);
                setNeedsResetAfterLoad();
                continue;
            }
            int index = p.getPortIndex();
            if (index < 0 || index >= outPortCount) {
                loadResult.addError("Invalid inport index " + index);
                setNeedsResetAfterLoad();
                continue;
            }
            if (m_outPortTemplates[index] != null) {
                loadResult.addError(
                        "Duplicate outport definition for index: " + index);
            }
            m_outPortTemplates[index] = p;
        }
        for (int i = 0; i < m_outPortTemplates.length; i++) {
            if (m_outPortTemplates[i] == null) {
                loadResult.addError("Assigning fallback port type for "
                        + "missing output port " + i);
                m_outPortTemplates[i] = 
                    new WorkflowPortTemplate(i, FALLBACK_PORTTYPE);
            }
        }
        return loadResult;
    }
    
    /** {@inheritDoc} */
    public LoadResult loadNodeContainer(final Map<Integer, BufferedDataTable> tblRep, 
            final ExecutionMonitor exec) 
            throws CanceledExecutionException, IOException {
        if (m_workflowDir == null || m_workflowSett == null) {
            throw new IllegalStateException("The method preLoadNodeContainer "
                    + "has either not been called or failed");
        }
        LoadResult loadResult = new LoadResult();
        /* read nodes */
        NodeSettingsRO nodes;
        try {
            nodes = loadSettingsForNodes(m_workflowSett);
        } catch (InvalidSettingsException e) {
            String error = "Can't load nodes in workflow, config not found: "
                + e.getMessage();
            LOGGER.debug(error, e);
            loadResult.addError(error);
            setNeedsResetAfterLoad();
            // stop loading here
            return loadResult;
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
                LOGGER.debug(error, e);
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
                LOGGER.debug(error, e);
                loadResult.addError(error);
            }
            boolean isMeta;
            try {
                isMeta = loadIsMetaNode(nodeSetting);
            } catch (InvalidSettingsException e) {
                String error = "Can't retrieve meta flag for contained node "
                    + "with id suffix " + nodeIDSuffix + ", attempting to read"
                    + "ordinary (not-meta) node: " + e.getMessage();
                LOGGER.debug(error, e);
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
                LOGGER.debug(error, e);
                loadResult.addError(error);
                uiInfoClassName = null;
            }
            if (uiInfoClassName != null) {
                uiInfo = loadUIInfoInstance(uiInfoClassName);
                try {
                    // avoid NoClassDefFoundErrors by using magic class loader
                    uiInfo = (UIInformation)(GlobalClassCreator
                            .createClass(uiInfoClassName).newInstance());
                } catch (Exception e) {
                    String error = "Unable to load UI information class \""
                        + uiInfoClassName + "\" to node with ID suffix " 
                        + nodeIDSuffix + ", no UI information available: "
                        + e.getMessage();
                    LOGGER.debug(error, e);
                    loadResult.addError(error);
                    uiInfo = null;
                }
                if (uiInfo != null) {
                    try {
                        loadUIInfoSettings(uiInfo, nodeSetting);
                    } catch (InvalidSettingsException e) {
                        String error = "Unable to load UI information to " 
                            + "node with ID suffix " + nodeIDSuffix
                            + ", no UI information available: " 
                            + e.getMessage();
                        LOGGER.debug(error, e);
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
                LOGGER.debug(error, e);
                loadResult.addError(error);
                continue;
            }
            try {
                LoadResult childResult =
                    persistor.preLoadNodeContainer(nodeFile, nodeSetting);
                if (childResult.hasErrors()) {
                    loadResult.addError("Errors during loading "
                            + (isMeta ? "meta " : "") + "node "
                            + "with ID suffix " + nodeIDSuffix, childResult);
                }
            } catch (CanceledExecutionException e) {
                throw e;
            } catch (Throwable e) {
                String error = "Unable to load node with ID suffix " 
                        + nodeIDSuffix + " into workflow, skipping it: "
                        + e.getMessage();
                if (e instanceof InvalidSettingsException 
                        || e instanceof IOException) {
                    LOGGER.debug(error, e);
                } else {
                    LOGGER.error(error, e);
                }
                loadResult.addError(error);
                continue;
            }
            NodeContainerMetaPersistor meta = persistor.getMetaPersistor();
            if (m_nodeContainerLoaderMap.containsKey(nodeIDSuffix)) {
                int randomID = getRandomNodeID();
                loadResult.addError("Duplicate id encountered in workflow: " 
                        + nodeIDSuffix + ", uniquifying to random id " 
                        + randomID + ", this possibly screws the connections");
                nodeIDSuffix = randomID;
            }
            meta.setNodeIDSuffix(nodeIDSuffix);
            meta.setUIInfo(uiInfo);
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
            LOGGER.debug(error, e);
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
                LOGGER.debug(error, e);
                loadResult.addError(error);
                continue;
            }
            int sourceIDSuffix = c.getSourceSuffix();
            if (!m_nodeContainerLoaderMap.containsKey(sourceIDSuffix)
                    && sourceIDSuffix != -1) {
                loadResult.addError("Unable to load node connection " + c
                        + ", source node does not exist");
            }
            int targetIDSuffix = c.getDestSuffix();
            NodeContainerPersistor targetNodePersistor = 
                m_nodeContainerLoaderMap.get(targetIDSuffix);
            if (targetNodePersistor == null && targetIDSuffix != -1) {
                loadResult.addError("Unable to load node connection " + c
                        + ", destination node does not exist");
            } else if (targetNodePersistor instanceof 
                    SingleNodeContainerPersistorVersion1xx) {
                /* workflows saved with 1.x.x have misleading port indices for
                 * incoming ports. Data ports precede the model ports (in
                 * their index), although the GUI and the true ordering is
                 * the other way around. */
                if (((SingleNodeContainerPersistorVersion1xx)
                        targetNodePersistor).shouldFixModelPortOrder()) {
                    Node node = ((SingleNodeContainerPersistorVersion1xx)
                            targetNodePersistor).getNode();
                    fixDestPortIfNecessary(node, c);
                }
            }
                
            if (!m_connectionSet.add(c)) {
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
            LOGGER.debug(error, e);
            loadResult.addError(error);
            uiInfoClassName = null;
        }
        if (uiInfoClassName != null) {
            inPortsBarUIInfo = loadUIInfoInstance(uiInfoClassName);
            try {
                // avoid NoClassDefFoundErrors by using magic class loader
                inPortsBarUIInfo = (UIInformation)(GlobalClassCreator
                        .createClass(uiInfoClassName).newInstance());
            } catch (Exception e) {
                String error =
                    "Unable to load inport bar's UI information: "
                    + e.getMessage();
                LOGGER.debug(error, e);
                loadResult.addError(error);
                inPortsBarUIInfo = null;
            }
            if (inPortsBarUIInfo != null) {
                try {
                    loadInPortsBarUIInfo(inPortsBarUIInfo, inPorts);
                } catch (InvalidSettingsException e) {
                    String error =
                        "Unable to load inport bar's UI information: "
                        + e.getMessage();
                    LOGGER.debug(error, e);
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
            LOGGER.debug(error, e);
            loadResult.addError(error);
            uiInfoClassName = null;
        }
        if (uiInfoClassName != null) {
            outPortsBarUIInfo = loadUIInfoInstance(uiInfoClassName);
            try {
                // avoid NoClassDefFoundErrors by using magic class loader
                outPortsBarUIInfo = (UIInformation)(GlobalClassCreator
                        .createClass(uiInfoClassName).newInstance());
            } catch (Exception e) {
                String error =
                        "Unable to load outport bar's UI information "
                                + "class \"" + uiInfoClassName
                                + "\", no UI information available: "
                                + e.getMessage();
                LOGGER.debug(error, e);
                loadResult.addError(error);
                outPortsBarUIInfo = null;
            }
            if (outPortsBarUIInfo != null) {
                try {
                    loadOutPortsBarUIInfo(outPortsBarUIInfo, outPorts);
                } catch (InvalidSettingsException e) {
                    String error = 
                        "Unable to load outport bar's UI information: "
                        + e.getMessage();
                    LOGGER.debug(error, e);
                    loadResult.addError(error);
                    outPortsBarUIInfo = null;
                }
            }
        }
        m_outPortsBarUIInfo = outPortsBarUIInfo;
        exec.setProgress(1.0);
        return loadResult;
    }
    
    /** This is overridden by the meta node loader (1.x.x) and returns
     * true for the "special" nodes.
     * @param settings node sub-element
     * @return true if to skip (though in 99.9% false)
     */ 
    protected boolean shouldSkipThisNode(final NodeSettingsRO settings) {
        return false;
    }
    
    protected String loadVersion(final NodeSettingsRO settings) 
        throws InvalidSettingsException {
        return settings.getString("version");
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
        try {
            // avoid NoClassDefFoundErrors by using magic class loader
            return (UIInformation)(GlobalClassCreator
                    .createClass(className).newInstance());
        } catch (Exception e) {
            LOGGER.warn("UIInfo class \"" + className 
                    + "\" could not be loaded", e);
            return null;
        }
    }

    protected void loadUIInfoSettings(final UIInformation uiInfo,
            final NodeSettingsRO settings) throws InvalidSettingsException {
        uiInfo.load(settings);
    }

    protected String loadInPortsBarUIInfoClassName(
            final NodeSettingsRO settings) throws InvalidSettingsException {
        return null;
    }
    
    protected void loadInPortsBarUIInfo(final UIInformation uiInfo,
            final NodeSettingsRO settings) throws InvalidSettingsException {
    }
    
    protected String loadOutPortsBarUIInfoClassName(
            final NodeSettingsRO settings) throws InvalidSettingsException {
        return null;
    }
    
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
        // java.util.File implementation to resolve these levels
        File fullFile = new File(workflowDir, fileString);
        if (!fullFile.isFile() || !fullFile.canRead()) {
            throw new InvalidSettingsException("Unable to read settings "
                    + "file " + fullFile.getAbsolutePath());
        }
        Stack<String> children = new Stack<String>();
        while (!fullFile.getAbsoluteFile().equals(workflowDir)) {
            children.push(fullFile.getName());
            fullFile = fullFile.getParentFile();
        }
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
        int sourcePort = settings.getInt("sourcePort");
        int destPort = loadConnectionDestPort(settings);
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
            LOGGER.debug("Could not load UI information for connection "
                    + "between nodes " + sourceID + " and " + destID);
        } catch (Throwable t) {
            LOGGER.warn("Exception while loading connection UI information "
                    + "between nodes " + sourceID + " and " + destID, t);
        }
        return new ConnectionContainerTemplate(sourceID, sourcePort, destID,
                destPort, uiInfo);
    }
    
    protected int loadConnectionDestID(final NodeSettingsRO settings) 
        throws InvalidSettingsException {
        return settings.getInt("targetID");
    }
    
    protected int loadConnectionDestPort(final NodeSettingsRO settings) 
    throws InvalidSettingsException {
        return settings.getInt("targetPort");
    }
    
    protected NodeSettingsRO loadSettingsForNodes(final NodeSettingsRO set)
            throws InvalidSettingsException {
        return set.getNodeSettings(KEY_NODES);
    }
    
    protected String loadWorkflowName(final NodeSettingsRO set)
            throws InvalidSettingsException {
        return "Workflow Manager";
    }

    protected NodeSettingsRO loadInPortsSetting(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        return null;
    }

    protected NodeSettingsRO loadInPortsSettingsEnum(
            final NodeSettingsRO settings) throws InvalidSettingsException {
        return null;
    }
    
    protected WorkflowPortTemplate loadInPortTemplate(NodeSettingsRO settings) 
            throws InvalidSettingsException {
        throw new InvalidSettingsException(
                "No ports for meta nodes in version 1.x.x");
    }
    
    protected NodeSettingsRO loadOutPortsSetting(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        return null;
    }
    
    protected NodeSettingsRO loadOutPortsSettingsEnum(
            final NodeSettingsRO settings) throws InvalidSettingsException  {
        return null;
    }
    
    protected WorkflowPortTemplate loadOutPortTemplate(NodeSettingsRO settings)
            throws InvalidSettingsException {
        throw new InvalidSettingsException(
            "No ports for meta nodes in version 1.x.x");
    }

    protected NodeContainerMetaPersistorVersion1xx
            createNodeContainerMetaPersistor(final ReferencedFile baseDir) {
        return new NodeContainerMetaPersistorVersion1xx(baseDir);
    }

    protected SingleNodeContainerPersistorVersion1xx 
            createSingleNodeContainerPersistor() {
        return new SingleNodeContainerPersistorVersion1xx(
                getGlobalTableRepository());
    }

    protected WorkflowPersistorVersion1xx createWorkflowPersistor() {
        return new ObsoleteMetaNodeWorkflowPersistorVersion1xx(
                getGlobalTableRepository());
    }
    
    private int getRandomNodeID() {
        // some number between 10k and 20k, hopefully unique.
        int nodeIDSuffix = 10000 + (int)(Math.random() * 10000);
        while (m_nodeContainerLoaderMap.containsKey(nodeIDSuffix)) {
            nodeIDSuffix += 1;
        }
        return nodeIDSuffix;
    }
    
    private void fixDestPortIfNecessary(final Node node, 
            final ConnectionContainerTemplate c) {
        int modelPortCount = 0;
        for (int i = 0; i < node.getNrInPorts(); i++) {
            if (!node.getInputType(i).getPortObjectClass().isAssignableFrom(
                    BufferedDataTable.class)) {
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

}
