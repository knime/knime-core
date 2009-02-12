/* 
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
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
 */
package org.knime.core.node.workflow;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;

import org.knime.core.data.container.ContainerTable;
import org.knime.core.internal.ReferencedFile;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortType;

/**
 * 
 * @author Bernd Wiswedel, University of Konstanz
 */
public class WorkflowPersistorVersion200 extends WorkflowPersistorVersion1xx {
    
    private static final String CFG_UIINFO_SUB_CONFIG = "ui_settings";
    /** Key for UI info's class name. */
    private static final String CFG_UIINFO_CLASS = "ui_classname";
    
    static final String VERSION_LATEST = "2.0.1";
    
    static boolean canReadVersion(final String versionString) {
        return versionString.equals("2.0.0") 
            || versionString.equals(VERSION_LATEST);
    }
    
    WorkflowPersistorVersion200() {
        super(null, VERSION_LATEST);
    }
    
    WorkflowPersistorVersion200(final HashMap<Integer, ContainerTable> tableRep,
            final String versionString) {
        super(tableRep, versionString);
    }
    
    protected String getSaveVersion() {
        return VERSION_LATEST;
    }
    
    @Override
    protected String loadWorkflowName(NodeSettingsRO set)
            throws InvalidSettingsException {
        return set.getString("name");
    }
    
    /** {@inheritDoc} */
    @Override
    protected NodeSettingsRO readParentSettings() throws IOException {
        return null; // only used in 1.3.x
    }
    
    @Override
    protected boolean loadIsMetaNode(NodeSettingsRO settings) 
            throws InvalidSettingsException {
        return settings.getBoolean("node_is_meta");
    }
    
    /** {@inheritDoc} */
    @Override
    protected String loadUIInfoClassName(final NodeSettingsRO settings) 
            throws InvalidSettingsException {
        if (settings.containsKey(CFG_UIINFO_CLASS)) {
            return settings.getString(CFG_UIINFO_CLASS);
        }
        return null;
    }
    
    /** {@inheritDoc} */
    @Override
    protected void loadUIInfoSettings(final UIInformation uiInfo,
            final NodeSettingsRO settings) throws InvalidSettingsException {
        // in previous releases, the settings were directly written to the 
        // top-most node settings object; since 2.0 they've been put into a 
        // separate sub-settings object
        NodeSettingsRO subSettings = 
            settings.getNodeSettings(CFG_UIINFO_SUB_CONFIG);
        super.loadUIInfoSettings(uiInfo, subSettings);
    }
    
    /** {@inheritDoc} */
    @Override
    protected int loadConnectionDestID(NodeSettingsRO settings)
            throws InvalidSettingsException {
        return settings.getInt("destID");
    }
    
    /** {@inheritDoc} */
    @Override
    protected int loadConnectionDestPort(NodeSettingsRO settings)
            throws InvalidSettingsException {
        return settings.getInt("destPort");
    }
    
    protected NodeSettingsRO loadInPortsSetting(final NodeSettingsRO settings)
        throws InvalidSettingsException {
        if (settings.containsKey("meta_in_ports")) {
            return settings.getNodeSettings("meta_in_ports");
        }
        return null;
    }
    
    /** {@inheritDoc} */
    @Override
    protected NodeSettingsRO loadInPortsSettingsEnum(NodeSettingsRO settings) 
        throws InvalidSettingsException {
        return settings.getNodeSettings("port_enum");
    }

    /** {@inheritDoc} */
    @Override
    protected WorkflowPortTemplate loadInPortTemplate(
            final NodeSettingsRO settings) throws InvalidSettingsException {
        int index = settings.getInt("index");
        String name = settings.getString("name");
        NodeSettingsRO portTypeSettings = settings.getNodeSettings("type");
        PortType type = PortType.load(portTypeSettings);
        WorkflowPortTemplate result = new WorkflowPortTemplate(index, type);
        result.setPortName(name);
        return result;
    }
    
    /** {@inheritDoc} */
    @Override
    protected String loadInPortsBarUIInfoClassName(
            final NodeSettingsRO settings) throws InvalidSettingsException {
        return loadUIInfoClassName(settings);
    }
    
    /** {@inheritDoc} */
    @Override
    protected void loadInPortsBarUIInfo(final UIInformation uiInfo,
            final NodeSettingsRO settings) throws InvalidSettingsException {
        loadUIInfoSettings(uiInfo, settings);
    }
    
    /** {@inheritDoc} */
    @Override
    protected String loadOutPortsBarUIInfoClassName(
            final NodeSettingsRO settings) throws InvalidSettingsException {
        return loadUIInfoClassName(settings);
    }
    
    /** {@inheritDoc} */
    @Override
    protected void loadOutPortsBarUIInfo(final UIInformation uiInfo,
            final NodeSettingsRO settings) throws InvalidSettingsException {
        loadUIInfoSettings(uiInfo, settings);
    }
    
    protected NodeSettingsRO loadOutPortsSetting(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        if (settings.containsKey("meta_out_ports")) {
            return settings.getNodeSettings("meta_out_ports");
        }
        return null;
    }
    
    /** {@inheritDoc} */
    @Override
    protected NodeSettingsRO loadOutPortsSettingsEnum(NodeSettingsRO settings) 
        throws InvalidSettingsException {
        return settings.getNodeSettings("port_enum");
    }
    
    /** {@inheritDoc} */
    @Override
    protected WorkflowPortTemplate loadOutPortTemplate(
            final NodeSettingsRO settings) throws InvalidSettingsException {
        int index = settings.getInt("index");
        String name = settings.getString("name");
        NodeSettingsRO portTypeSettings = settings.getNodeSettings("type");
        PortType type = PortType.load(portTypeSettings);
        WorkflowPortTemplate result = new WorkflowPortTemplate(index, type);
        result.setPortName(name);
        return result;
    }
    
    /** {@inheritDoc} */
    @Override
    protected boolean loadIfMustWarnOnDataLoadError(final File workflowDir) {
        return new File(workflowDir, SAVED_WITH_DATA_FILE).isFile();
    }

    protected void saveUIInfoClassName(final NodeSettingsWO settings,
            final UIInformation info) {
        settings.addString(CFG_UIINFO_CLASS, info != null 
                ? info.getClass().getName() : null);
    }
    
    protected void saveUIInfoSettings(final NodeSettingsWO settings,
            final UIInformation uiInfo) {
        if (uiInfo == null) {
            return;
        }
        // nest into separate sub config
        NodeSettingsWO subConfig = 
            settings.addNodeSettings(CFG_UIINFO_SUB_CONFIG);
        uiInfo.save(subConfig);
    }
    
    /** {@inheritDoc} */
    @Override
    protected NodeContainerMetaPersistorVersion200 
        createNodeContainerMetaPersistor(final ReferencedFile nodeDirectory) {
        return new NodeContainerMetaPersistorVersion200(nodeDirectory);
    }

    
    /** {@inheritDoc} */
    @Override
    protected WorkflowPersistorVersion200 createWorkflowPersistor() {
        return new WorkflowPersistorVersion200(
                getGlobalTableRepository(), getVersionString());
    }
    
    /** {@inheritDoc} */
    @Override
    protected SingleNodeContainerPersistorVersion200 
        createSingleNodeContainerPersistor() {
        return new SingleNodeContainerPersistorVersion200(
                this, getVersionString());
    }
    
    public String save(final WorkflowManager wm,
            final ReferencedFile workflowDirRef, final ExecutionMonitor execMon,
            final boolean isSaveData) throws IOException,
            CanceledExecutionException {
        if (workflowDirRef.equals(wm.getNodeContainerDirectory()) 
                && !wm.isDirty()) {
            return WORKFLOW_FILE;
        }
        File workflowDir = workflowDirRef.getFile();
        workflowDir.mkdirs();
        if (!workflowDir.isDirectory()) {
            throw new IOException("Unable to create or write directory \": " 
                    + workflowDir + "\"");
        }
        NodeSettings settings = 
            new NodeSettings(WorkflowPersistor.WORKFLOW_FILE);
        settings.addString(WorkflowManager.CFG_VERSION, getSaveVersion());
        saveWorkflowName(settings, wm.getName());
        NodeContainerMetaPersistorVersion200 metaPersistor = 
            createNodeContainerMetaPersistor(null);
        metaPersistor.save(wm, settings);
        
        NodeSettingsWO nodesSettings = saveSettingsForNodes(settings);
        Collection<NodeContainer> nodes = wm.getNodeContainers();
        double progRatio = 1.0 / (nodes.size() + 1);

        for (NodeContainer nextNode : nodes) {
            int id = nextNode.getID().getIndex();
            ExecutionMonitor subExec = execMon.createSubProgress(progRatio);
            execMon.setMessage(nextNode.getNameWithID());
            NodeSettingsWO sub = nodesSettings.addNodeSettings("node_" + id);
            saveNodeContainer(
                    sub, workflowDirRef, nextNode, subExec, isSaveData);
            subExec.setProgress(1.0);
        }

        execMon.setMessage("connection information");
        NodeSettingsWO connSettings = saveSettingsForConnections(settings);
        int connectionNumber = 0;
        for (ConnectionContainer cc : wm.getConnectionContainers()) {
            NodeSettingsWO nextConnectionConfig = connSettings
                    .addNodeSettings("connection_" + connectionNumber);
            saveConnection(nextConnectionConfig, cc);
            connectionNumber += 1;
        }
        int inCount = wm.getNrInPorts();
        NodeSettingsWO inPortsSetts = inCount > 0 
            ? saveInPortsSetting(settings) : null;
        NodeSettingsWO inPortsSettsEnum = null;
        if (inPortsSetts != null) {
            saveInportsBarUIInfoClassName(
                    inPortsSetts, wm.getInPortsBarUIInfo());
            saveInportsBarUIInfoSettings(
                    inPortsSetts, wm.getInPortsBarUIInfo());
            inPortsSettsEnum = saveInPortsEnumSetting(inPortsSetts);
        }
        for (int i = 0; i < inCount; i++) {
            NodeSettingsWO singlePort = saveInPortSetting(inPortsSettsEnum, i);
            saveInPort(singlePort, wm, i);
        }
        int outCount = wm.getNrOutPorts();
        NodeSettingsWO outPortsSetts = outCount > 0 
            ? saveOutPortsSetting(settings) : null;
        NodeSettingsWO outPortsSettsEnum = null;
        if (outPortsSetts != null) {
            saveOutportsBarUIInfoClassName(
                    outPortsSetts, wm.getOutPortsBarUIInfo());
            saveOutportsBarUIInfoSettings(
                    outPortsSetts, wm.getOutPortsBarUIInfo());
            outPortsSettsEnum = saveOutPortsEnumSetting(outPortsSetts);
        }
        for (int i = 0; i < outCount; i++) {
            NodeSettingsWO singlePort = 
                saveOutPortSetting(outPortsSettsEnum, i);
            saveOutPort(singlePort, wm, i);
        }
        File workflowFile = new File(workflowDir, WORKFLOW_FILE);
        settings.saveToXML(new FileOutputStream(workflowFile));
        File saveWithDataFile = new File(workflowDir, SAVED_WITH_DATA_FILE);
        BufferedWriter o = new BufferedWriter(new FileWriter(saveWithDataFile));
        o.write("Do not delete this file!");
        o.newLine();
        o.write("This file serves to indicate that the workflow was written " 
                + "as part of the usual save routine (not exported).");
        o.newLine();
        o.newLine();
        o.write("Workflow was last saved by user "); 
        o.write(System.getProperty("user.name"));
        o.write(" on " + new Date());
        o.close();
        if (wm.getNodeContainerDirectory() == null) {
            wm.setNodeContainerDirectory(workflowDirRef);
        }
        if (workflowDirRef.equals(wm.getNodeContainerDirectory())) {
            wm.unsetDirty();
        }
        execMon.setProgress(1.0);
        return WORKFLOW_FILE;
    }
    
    protected void saveWorkflowName(
            final NodeSettingsWO settings, final String name) {
        settings.addString("name", name);
    }

    /**
     * Save nodes in an own sub-config object as a series of configs.
     * 
     * @param settings To save to.
     * @return The sub config where subsequent writing takes place.
     */
    protected NodeSettingsWO saveSettingsForNodes(final NodeSettingsWO settings) {
        return settings.addNodeSettings(KEY_NODES);
    }

    /**
     * Save connections in an own sub-config object.
     * 
     * @param settings To save to.
     * @return The sub config where subsequent writing takes place.
     */
    protected NodeSettingsWO saveSettingsForConnections(
            final NodeSettingsWO settings) {
        return settings.addNodeSettings(KEY_CONNECTIONS);
    }

    protected void saveNodeContainer(final NodeSettingsWO settings,
            final ReferencedFile workflowDirRef, final NodeContainer container,
            final ExecutionMonitor exec, final boolean isSaveData)
            throws CanceledExecutionException, IOException {
        saveNodeIDSuffix(settings, container);
        int idSuffix = container.getID().getIndex();
        // name of sub-directory container node/sub-workflow settings
        // all chars which are not letter or number are replaced by '_'
        String nodeDirID = container.getName().replaceAll("[^a-zA-Z0-9 ]", "_")
                + " (#" + idSuffix + ")";

        ReferencedFile nodeDirectoryRef = 
            new ReferencedFile(workflowDirRef, nodeDirID);
        String fileName; 
        if (container instanceof WorkflowManager) {
            WorkflowPersistorVersion200 p = createWorkflowPersistor();
            fileName = p.save((WorkflowManager)container, nodeDirectoryRef,
                    exec, isSaveData);
        } else {
            SingleNodeContainerPersistorVersion200 p =
                createSingleNodeContainerPersistor();
            fileName = p.save((SingleNodeContainer)container, 
                    nodeDirectoryRef, exec, isSaveData);
        }
        saveFileLocation(settings, nodeDirID + "/" + fileName);
        saveIsMeta(settings, container);
        saveUIInfoClassName(settings, container.getUIInformation());
        saveUIInfoSettings(settings, container.getUIInformation());
    }
    
    protected void saveNodeIDSuffix(final NodeSettingsWO settings,
            final NodeContainer nc) {
        settings.addInt(KEY_ID, nc.getID().getIndex());
    }

    protected void saveFileLocation(final NodeSettingsWO settings,
            final String location) {
        settings.addString("node_settings_file", location);
    }
    
    protected void saveIsMeta(
            final NodeSettingsWO settings, final NodeContainer nc) {
        settings.addBoolean("node_is_meta", nc instanceof WorkflowManager);
    }
    
    protected NodeSettingsWO saveInPortsSetting(final NodeSettingsWO settings) {
        return settings.addNodeSettings("meta_in_ports");
    }

    protected NodeSettingsWO saveInPortsEnumSetting(
            final NodeSettingsWO settings) {
        return settings.addNodeSettings("port_enum");
    }
    
    protected NodeSettingsWO saveInPortSetting(
            final NodeSettingsWO settings, final int portIndex) {
        return settings.addNodeSettings("inport_" + portIndex);
    }
    
    protected void saveInportsBarUIInfoClassName(final NodeSettingsWO settings,
            final UIInformation info) {
        saveUIInfoClassName(settings, info);
    }
    
    protected void saveInportsBarUIInfoSettings(final NodeSettingsWO settings,
            final UIInformation uiInfo) {
        saveUIInfoSettings(settings, uiInfo);
    }
    
    protected void saveInPort(NodeSettingsWO settings, 
            WorkflowManager wm, final int portIndex) {
        WorkflowInPort inport = wm.getInPort(portIndex);
        settings.addInt("index", portIndex);
        settings.addString("name", inport.getPortName());
        NodeSettingsWO portTypeSettings = settings.addNodeSettings("type");
        inport.getPortType().save(portTypeSettings);
    }
    
    protected NodeSettingsWO saveOutPortsSetting(
            final NodeSettingsWO settings) {
        return settings.addNodeSettings("meta_out_ports");
    }
    
    protected NodeSettingsWO saveOutPortsEnumSetting(
            final NodeSettingsWO settings) {
        return settings.addNodeSettings("port_enum");
    }
    
    protected void saveOutportsBarUIInfoClassName(final NodeSettingsWO settings,
            final UIInformation info) {
        saveUIInfoClassName(settings, info);
    }
    
    protected void saveOutportsBarUIInfoSettings(final NodeSettingsWO settings,
            final UIInformation uiInfo) {
        saveUIInfoSettings(settings, uiInfo);
    }
    
    protected NodeSettingsWO saveOutPortSetting(
            final NodeSettingsWO settings, final int portIndex) {
        return settings.addNodeSettings("outport_" + portIndex);
    }
    
    protected void saveOutPort(NodeSettingsWO settings, 
            WorkflowManager wm, final int portIndex) {
        WorkflowOutPort outport = wm.getOutPort(portIndex);
        settings.addInt("index", portIndex);
        settings.addString("name", outport.getPortName());
        NodeSettingsWO portTypeSettings = settings.addNodeSettings("type");
        outport.getPortType().save(portTypeSettings);
    }
    
    protected void saveConnection(final NodeSettingsWO settings,
            final ConnectionContainer connection) {
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
        UIInformation uiInfo = connection.getUIInfo();
        if (uiInfo != null) {
            saveUIInfoClassName(settings, uiInfo);
            saveUIInfoSettings(settings, uiInfo);
        }
        if (!connection.isDeletable()) {
            settings.addBoolean("isDeletable", false);
        }
    }

    
}
