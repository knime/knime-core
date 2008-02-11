/* 
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2007
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
import java.util.TreeMap;

import org.knime.core.data.container.ContainerTable;
import org.knime.core.eclipseUtil.GlobalClassCreator;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;

/**
 * 
 * @author wiswedel, University of Konstanz
 */
class WorkflowPersistorVersion1xx implements WorkflowPersistor {

    /** The node logger for this class. */
    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(WorkflowPersistorVersion1xx.class);

    private final TreeMap<Integer, NodeContainerPersistor> 
        m_nodeContainerLoaderMap;

    private final HashSet<ConnectionContainerTemplate> m_connectionSet;
    
    private NodeContainerMetaPersistor m_metaPersistor = 
        new NodeContainerMetaPersistorVersion1xx();    
    private final HashMap<Integer, ContainerTable> m_globalTableRepository;
    
    private WorkflowInPort[] m_inPorts;
    private WorkflowOutPort[] m_outPorts;
    
    private String m_name;
    
    static boolean canReadVersion(final String versionString) {
        boolean result = versionString.equals("0.9.0");
        result |= versionString.matches("1\\.[0123]\\.[0-9].*");
        return result;
    }
    
    WorkflowPersistorVersion1xx(
            final HashMap<Integer, ContainerTable> tableRep) {
        m_globalTableRepository = tableRep;
        m_nodeContainerLoaderMap = 
            new TreeMap<Integer, NodeContainerPersistor>();
        m_connectionSet = new HashSet<ConnectionContainerTemplate>();
    }

    /**
     * Loads the complete workflow from the given file.
     * 
     * @param workflowFile the workflow file
     * @param progMon a node progress monitor for reporting progress
     * @throws IOException if the workflow file can not be found or files to
     *             load node internals
     * @throws InvalidSettingsException if settings cannot be read
     * @throws CanceledExecutionException if loading was canceled
     * @throws WorkflowInExecutionException if the workflow is currently being
     *             executed
     * @throws WorkflowException if an exception occurs while loading the
     *             workflow structure
     */
    // public synchronized void load(final WorkflowManager wfm,
    // final File workflowFile, final NodeProgressMonitor progMon)
    // throws IOException, InvalidSettingsException,
    // CanceledExecutionException, WorkflowInExecutionException,
    // WorkflowException {
    // checkForRunningNodes("Workflow cannot be loaded");
    //
    // if (!workflowFile.isFile()
    // || !workflowFile.getName().equals(WORKFLOW_FILE)) {
    // throw new IOException("File must be named: \"" + WORKFLOW_FILE
    // + "\": " + workflowFile);
    // }
    //
    // // ==================================================================
    // // FIXME The following lines and the ones in the finally-block
    // // are just hacks to omit warnings messages during loading the flow.
    // // When the WFM is redesigned we need a proper way to do this.
    // if (m_parent == null) { // meta nodes must not do anything here!
    // NodeLogger.setIgnoreConfigureWarning(true);
    // }
    // // ==================================================================
    // try {
    // // load workflow topology
    // NodeSettingsRO settings = NodeSettings
    // .loadFromXML(new FileInputStream(workflowFile));
    // if (settings.containsKey(CFG_VERSION)) {
    // m_loadedVersion = settings.getString(CFG_VERSION);
    // if (m_loadedVersion == null) {
    // throw new WorkflowException(
    // "Refuse to load workflow: Workflow version not available.");
    // }
    // // first version was only labeled with 1.0 instead of 1.0.0
    // if (m_loadedVersion.equals("1.0")) {
    // m_loadedVersion = "1.0.0";
    // }
    // } else {
    // m_loadedVersion = "0.9.0"; // CeBIT 2006 version without
    // // version id
    // }
    // LOGGER.debug("Trying to parse version: " + m_loadedVersion);
    // String[] versionStrArray = m_loadedVersion.split("\\.");
    // int[] versionIntArray = new int[]{KNIMEConstants.MAJOR,
    // KNIMEConstants.MINOR, KNIMEConstants.REV};
    // if (versionStrArray.length != versionIntArray.length) {
    // throw new WorkflowException("Refuse to load workflow: Unknown"
    // + " workflow version \"" + m_loadedVersion + "\".");
    // }
    // for (int i = 0; i < versionIntArray.length; i++) {
    // int value = -1;
    // try {
    // value = Integer.parseInt(versionStrArray[i]);
    // } catch (NumberFormatException nfe) {
    // throw new WorkflowException(
    // "Refuse to load workflow: Unknown workflow version "
    // + "\"" + m_loadedVersion + "\".");
    // }
    // if (value < versionIntArray[i]) {
    // break;
    // } else if (value > versionIntArray[i]) {
    // throw new WorkflowException("Refuse to load workflow: "
    // + "The current KNIME version ("
    // + KNIMEConstants.VERSION
    // + ") is older than the workflow ("
    // + m_loadedVersion + ") you are trying to load.\n"
    // + "Please get a newer version of KNIME.");
    // }
    // }
    // if (!KNIMEConstants.VERSION.equalsIgnoreCase(m_loadedVersion)) {
    // if (m_parent == null) {
    // LOGGER
    // .warn("The current KNIME version ("
    // + KNIMEConstants.VERSION
    // + ") is different from the one that created the"
    // + " workflow ("
    // + m_loadedVersion
    // + ") you are trying to load. In some rare cases, it"
    // + " might not be possible to load all data"
    // + " or some nodes can't be configured."
    // + " Please re-configure and/or re-execute these"
    // + " nodes.");
    // }
    // }
    //
    // try {
    // load(settings);
    // } finally {
    //
    // File parentDir = workflowFile.getParentFile();
    //
    // // data files are loaded using a repository of reference tables;
    // // these lines serves to init the repository so nodes can put
    // // their data
    // // into this map, the repository is deleted when the loading is
    // // done
    //
    // // meta workflows must use their grand*-parent editor's id
    // // and only the grand-parent may initialize the repository with
    // // the id
    // while (wfm.m_parent != null) {
    // wfm = wfm.m_parent;
    // }
    // int loadID = System.identityHashCode(wfm);
    // if (wfm == this) {
    // BufferedDataTable.initRepository(loadID);
    // }
    // ArrayList<NodeContainer> failedNodes = new ArrayList<NodeContainer>();
    // // get all keys in there
    // try {
    // double nodeCounter = 1.0;
    // ExecutionMonitor execMon = new ExecutionMonitor(progMon);
    // for (int i = 0; i < topSortNodes().size(); i++) {
    // NodeContainer newNode = topSortNodes().get(i);
    // execMon.checkCanceled();
    // execMon.setMessage("Loading node: "
    // + newNode.getNameWithID());
    // try {
    // NodeSettingsRO nodeSetting = settings
    // .getNodeSettings(KEY_NODES)
    // .getNodeSettings("node_" + newNode.getID());
    // String nodeFileName = nodeSetting
    // .getString(KEY_NODE_SETTINGS_FILE);
    // File nodeFile = new File(parentDir, nodeFileName);
    // NodeProgressMonitor subProgMon = execMon
    // .createSubProgress(
    // 1.0 / topSortNodes().size())
    // .getProgressMonitor();
    // newNode.load(loadID, nodeFile, subProgMon);
    // } catch (IOException ioe) {
    // String msg = "Unable to load node: "
    // + newNode.getNameWithID()
    // + " -> reset and configure.";
    // LOGGER.error(msg, ioe);
    // failedNodes.add(newNode);
    // } catch (InvalidSettingsException ise) {
    // String msg = "Unable to load node: "
    // + newNode.getNameWithID()
    // + " -> reset and configure.";
    // LOGGER.error(msg, ise);
    // failedNodes.add(newNode);
    // } catch (Throwable e) {
    // String msg = "Unable to load node: "
    // + newNode.getNameWithID()
    // + " -> reset and configure.";
    // LOGGER.error(msg, e);
    // failedNodes.add(newNode);
    // }
    // progMon
    // .setProgress(nodeCounter
    // / topSortNodes().size());
    // // progMon.setMessage("Prog: " + nodeCounter
    // // / topSortNodes().size());
    // nodeCounter += 1.0;
    // }
    // } finally {
    // // put into a finally block because that may release much of
    // // memory
    //
    // // only the wfm that create the repos may clear it,
    // // otherwise
    // // the meta workflow clears it and not-yet-loaded nodes
    // // in the parent cannot be loaded
    // if (wfm == this) {
    // BufferedDataTable.clearRepository(loadID);
    // }
    // }
    // for (NodeContainer newNode : failedNodes) {
    // resetAndConfigureNode(newNode.getID());
    // }
    // }
    // } finally {
    // // ===============================================================
    // // FIXME The following linesand the one line above are just hacks
    // // to omit warnings messages during loading the flow.
    // // When the WFM is redesigned we need a proper way to do this.
    // if (m_parent == null) { // meta nodes must not do anything here!
    // NodeLogger.setIgnoreConfigureWarning(false);
    // }
    // // ===============================================================
    // }
    // }
    /**
     * Saves this workflow manager settings including nodes and connections into
     * the given file. In additon, all nodes' internal structures are stored -
     * if available, depending on the current node state, reset, configured, or
     * executed. For each node a directory is created (at the workflow file's
     * parent path) to save the node internals.
     * 
     * @param workflowFile To write workflow manager settings to.
     * @param progMon The monitor for the workflow saving progress.
     * @throws IOException If the workflow file can't be found.
     * @throws CanceledExecutionException If the saving process has been
     *             canceled.
     * @throws WorkflowInExecutionException if the workflow is currently being
     *             executed
     */
    // public synchronized void save(final File workflowFile,
    // final NodeProgressMonitor progMon) throws IOException,
    // CanceledExecutionException, WorkflowInExecutionException {
    // checkForRunningNodes("Workflow cannot be saved");
    //
    // if (workflowFile.isDirectory()
    // || !workflowFile.getName().equals(WORKFLOW_FILE)) {
    // throw new IOException("File must be named: \"" + WORKFLOW_FILE
    // + "\": " + workflowFile);
    // }
    //
    // // remove internals of all detached nodes first
    // for (NodeContainer cont : m_detachedNodes) {
    // cont.removeInternals();
    // }
    // m_detachedNodes.clear();
    //
    // }

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
    public WorkflowInPort[] getInPorts() {
        return m_inPorts;
    }
    
    /** {@inheritDoc} */
    public WorkflowOutPort[] getOutPorts() {
        return m_outPorts;
    }
    
    public void loadWorkflow(final NodeSettingsRO settings, 
            final File workflowDirectory, final ExecutionMonitor exec, 
            int loadID) throws CanceledExecutionException, IOException,
            InvalidSettingsException {
        m_name = loadWorkflowName(settings);
        /* read nodes */
        NodeSettingsRO nodes = loadSettingsForNodes(settings);
        for (String nodeKey : nodes.keySet()) {
            NodeSettingsRO nodeSetting = nodes.getNodeSettings(nodeKey);
            boolean isMeta = loadIsMetaNode(nodeSetting);
            NodeContainerPersistor persistor;
            if (isMeta) {
                persistor = createWorkflowPersistor();
            } else {
                persistor = createSingleNodeContainerPersistor();
            }
            int nodeIDSuffix = loadNodeIDSuffix(nodeSetting);
            UIInformation uiInfo = null;
            String uiInfoClassName = loadUIInfoClassName(nodeSetting);
            if (uiInfoClassName != null) {
                uiInfo = loadUIInfoInstance(uiInfoClassName);
                if (uiInfo != null) {
                    loadUIInfoSettings(uiInfo, nodeSetting);
                }
            }
            File nodeFile = loadNodeFile(nodeSetting, workflowDirectory);
            persistor.loadNodeContainer(nodeFile, exec, loadID);
            NodeContainerMetaPersistor meta = persistor.getMetaPersistor();
            meta.setNodeIDSuffix(nodeIDSuffix);
            meta.setUIInfo(uiInfo);
            if (m_nodeContainerLoaderMap.containsKey(nodeIDSuffix)) {
                throw new InvalidSettingsException("Duplicate id encountered "
                        + "in workflow: " + nodeIDSuffix);
            }
            m_nodeContainerLoaderMap.put(nodeIDSuffix, persistor);
        }

        /* read connections */
        NodeSettingsRO connections = loadSettingsForConnections(settings);
        for (String connectionKey : connections.keySet()) {
            ConnectionContainerTemplate c = loadConnection(connections
                    .getNodeSettings(connectionKey));
            int sourceIDSuffix = c.getSourceID();
            if (!m_nodeContainerLoaderMap.containsKey(sourceIDSuffix)
                    && sourceIDSuffix != -1) {
                LOGGER.warn("Unable to load node connection " + c
                        + ", source node does not exist");
            }
            int targetIDSuffix = c.getTargetID();
            if (!m_nodeContainerLoaderMap.containsKey(targetIDSuffix)
                    && targetIDSuffix != -1) {
                LOGGER.warn("Unable to load node connection " + c
                        + ", destination node does not exist");
            }
            if (!m_connectionSet.add(c)) {
                LOGGER.warn("Duplicate connection information: " + c);
            }
        }
        
        /* read in and outports */
        NodeSettingsRO inPorts = loadInPortsSetting(settings);
        int inPortCount = inPorts != null ? inPorts.keySet().size() : 0;
        m_inPorts = new WorkflowInPort[inPortCount];
        if (inPortCount > 0) {
            for (String key : inPorts.keySet()) {
                NodeSettingsRO sub = inPorts.getNodeSettings(key);
                WorkflowInPort p = loadInPort(sub);
                int index = p.getPortID();
                if (index < 0 || index >= inPortCount) {
                    throw new InvalidSettingsException(
                            "Invalid inport index " + index);
                }
                if (m_inPorts[index] != null) {
                    throw new InvalidSettingsException(
                            "Duplicate inport definition for index: " + index);
                }
                m_inPorts[index] = p;
            }
        }
        
        NodeSettingsRO outPorts = loadOutPortsSetting(settings);
        int outPortCount = outPorts != null ? outPorts.keySet().size() : 0;
        m_outPorts = new WorkflowOutPort[outPortCount];
        if (outPortCount > 0) {
            for (String key : outPorts.keySet()) {
                NodeSettingsRO sub = outPorts.getNodeSettings(key);
                WorkflowOutPort p = loadOutPort(sub);
                int index = p.getPortID();
                if (index < 0 || index >= outPortCount) {
                    throw new InvalidSettingsException(
                            "Invalid inport index " + index);
                }
                if (m_outPorts[index] != null) {
                    throw new InvalidSettingsException(
                            "Duplicate outport definition for index: " + index);
                }
                m_outPorts[index] = p;
            }
        }
    }
    
    /** {@inheritDoc} */
    public void loadNodeContainer(final File nodeFile, 
            final ExecutionMonitor exec, final int loadID) 
    throws InvalidSettingsException, CanceledExecutionException, IOException {
        m_metaPersistor = createNodeContainerMetaPersistor();
        if (nodeFile == null || !nodeFile.isFile()) {
            throw new InvalidSettingsException("Can't read file \"" 
                    + nodeFile.getAbsolutePath() +"\"");
        }
        InputStream in = new BufferedInputStream(new FileInputStream(nodeFile));
        NodeSettingsRO subWFSettings = NodeSettings.loadFromXML(in);
        m_metaPersistor.load(subWFSettings);
        loadWorkflow(subWFSettings, nodeFile.getParentFile(), exec, loadID);
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


    protected File loadNodeFile(final NodeSettingsRO settings,
            final File workflowDir) throws InvalidSettingsException {
        String fileString = settings.getString("node_settings_file");
        if (fileString == null) {
            throw new InvalidSettingsException("Unable to read settings "
                    + "file for node " + settings.getKey());
        }
        File result = new File(workflowDir, fileString);
        if (!result.isFile() || !result.canRead()) {
            throw new InvalidSettingsException("Unable to read settings "
                    + "file " + result.getAbsolutePath());
        }
        return result;
    }
    
    protected boolean loadIsMetaNode(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        return false;
    }

    protected NodeSettingsRO loadSettingsForConnections(final NodeSettingsRO set)
            throws InvalidSettingsException {
        return set.getNodeSettings(KEY_CONNECTIONS);
    }

    protected ConnectionContainerTemplate loadConnection(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        int sourceID = settings.getInt(KEY_SOURCE_ID);
        int targetID = settings.getInt(KEY_TARGET_ID);
        int sourcePort = settings.getInt(KEY_SOURCE_PORT);
        int targetPort = settings.getInt(KEY_TARGET_PORT);
        UIInformation uiInfo = null;
        try {
            String uiInfoClass = loadUIInfoClassName(settings);
            uiInfo = loadUIInfoInstance(uiInfoClass);
            if (uiInfo != null) {
                loadUIInfoSettings(uiInfo, settings);
            }
        } catch (InvalidSettingsException ise) {
            LOGGER.debug("Could not load UI information for connection "
                    + "between nodes " + sourceID + " and " + targetID);
        } catch (Throwable t) {
            LOGGER.warn("Exception while loading connection UI information "
                    + "between nodes " + sourceID + " and " + targetID, t);
        }
        return new ConnectionContainerTemplate(sourceID, sourcePort, targetID,
                targetPort, uiInfo);
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

    protected WorkflowInPort loadInPort(NodeSettingsRO settings) 
            throws InvalidSettingsException {
        throw new IllegalStateException(
                "No ports for meta nodes in version 1.x.x");
    }
    
    protected NodeSettingsRO loadOutPortsSetting(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        return null;
    }
    
    protected WorkflowOutPort loadOutPort(NodeSettingsRO settings)
            throws InvalidSettingsException {
        throw new IllegalStateException(
            "No ports for meta nodes in version 1.x.x");
    }

    protected NodeContainerMetaPersistorVersion1xx
            createNodeContainerMetaPersistor() {
        return new NodeContainerMetaPersistorVersion1xx();
    }

    protected SingleNodeContainerPersistorVersion1xx 
            createSingleNodeContainerPersistor() {
        return new SingleNodeContainerPersistorVersion1xx(
                getGlobalTableRepository());
    }

    protected WorkflowPersistorVersion1xx createWorkflowPersistor() {
        return new WorkflowPersistorVersion1xx(
                getGlobalTableRepository());
    }

}
