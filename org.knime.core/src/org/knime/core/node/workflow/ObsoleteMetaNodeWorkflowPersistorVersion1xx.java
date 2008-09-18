/*
 * ------------------------------------------------------------------ *
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
 * ---------------------------------------------------------------------
 * 
 * History
 *   Mar 27, 2008 (wiswedel): created
 */
package org.knime.core.node.workflow;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.knime.core.data.container.ContainerTable;
import org.knime.core.internal.ReferencedFile;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortType;

/**
 * 
 * @author wiswedel, University of Konstanz
 */
public class ObsoleteMetaNodeWorkflowPersistorVersion1xx extends
        WorkflowPersistorVersion1xx {
    
    private enum MetaNodeType {
        ORDINARY,
        CROSSVALIDATION,
        LOOPER
    };
    
    public static final List<String> OLD_META_NODES = 
        Collections.unmodifiableList(Arrays.asList(new String[]{
                "org.knime.base.node.meta.MetaNodeFactory01",
                "org.knime.base.node.meta.MetaNodeFactory11",
                "org.knime.base.node.meta.MetaNodeFactory21",
                "org.knime.base.node.meta.MetaNodeFactory12",
                "org.knime.base.node.meta.MetaNodeFactory22",
                "org.knime.base.node.meta.xvalidation.XValidateNodeFactory",
                "org.knime.base.node.meta.looper.LooperFactory"
        }));
    
    private int[] m_dataInNodeIDs = new int[0];
    private int[] m_dataOutNodeIDs = new int[0];
    
    private MetaNodeType m_metaNodeType = MetaNodeType.ORDINARY;
    
    private static final NodeLogger LOGGER = NodeLogger.getLogger(
            ObsoleteMetaNodeWorkflowPersistorVersion1xx.class);
    
    public ObsoleteMetaNodeWorkflowPersistorVersion1xx(
            final HashMap<Integer, ContainerTable> globalRep) {
        super(globalRep);
    }
    
    /** {@inheritDoc} */
    @Override
    public LoadResult preLoadNodeContainer(final ReferencedFile nodeFileRef,
            final NodeSettingsRO parentSettings) throws IOException, 
            CanceledExecutionException, InvalidSettingsException {
        LoadResult result = new LoadResult();
        File setFile = nodeFileRef.getFile();
        if (!setFile.getName().equals("settings.xml")) {
            String warn = "Settings file of obsolete meta node is not "
                    + "named settings.xml: " + setFile.getName();
            LOGGER.warn(warn);
            result.addError(warn);
        }
        ReferencedFile parent = nodeFileRef.getParent();
        if (parent == null) {
            throw new IOException("Parent directory not represented by class "
                    + ReferencedFile.class);
        }
        ReferencedFile workflowKnimeRef = 
            new ReferencedFile(parent, "workflow.knime");
        File workflowKnime = workflowKnimeRef.getFile();
        if (!workflowKnime.isFile()) {
            throw new IOException(
                    "Can't find file " + workflowKnime.getAbsolutePath());
        }
        String factory = parentSettings.getString("factory");
        if ("org.knime.base.node.meta.xvalidation.XValidateNodeFactory".equals(
                factory)) {
            m_metaNodeType = MetaNodeType.CROSSVALIDATION;
        } else if ("org.knime.base.node.meta.looper.LooperFactory".equals(
                factory)) {
            m_metaNodeType = MetaNodeType.LOOPER;
        } else {
            m_metaNodeType = MetaNodeType.ORDINARY;
        }
        switch (m_metaNodeType) {
        case CROSSVALIDATION:
        case LOOPER:
        default:
        }
        NodeSettingsRO settings = NodeSettings.loadFromXML(
                new BufferedInputStream(new FileInputStream(setFile)));
        NodeSettingsRO modelSet = settings.getNodeSettings("model");
        m_dataInNodeIDs = modelSet.getIntArray("dataInContainerIDs");
        m_dataOutNodeIDs = modelSet.getIntArray("dataOutContainerIDs");
        result.addError(super.preLoadNodeContainer(
                workflowKnimeRef, parentSettings));
        String name = "Looper";
        switch (m_metaNodeType) {
        case CROSSVALIDATION:
            name = "Cross Validation";
        case LOOPER:
            result = new LoadResult();
            result.addError("Workflow contains obsolete \"" + name 
                    + "\" meta node implementation, not all settings could "
                    + "be restored, please re-configure and execute again.");
            setNeedsResetAfterLoad();
        default: 
        }
        return result;
    }
    
    /** {@inheritDoc} */
    @Override
    protected boolean shouldSkipThisNode(final NodeSettingsRO settings) {
        switch (m_metaNodeType) {
        case LOOPER:
            return false;
        default:
        }
        try {
            int idx = loadNodeIDSuffix(settings);
            return doesAnyArrayContain(idx, m_dataInNodeIDs, m_dataOutNodeIDs);
        } catch (InvalidSettingsException e) {
            return false; // will fail at later stage
        }
    }
    
    /** {@inheritDoc} */
    @Override
    protected SingleNodeContainerPersistorVersion1xx 
            createSingleNodeContainerPersistor() {
        return new ObsoleteSpecialNodeSingleNodeContainerPersistorVersion1xx(
                this);
    }
    
    /** {@inheritDoc} */
    @Override
    protected String loadWorkflowName(final NodeSettingsRO set)
            throws InvalidSettingsException {
        switch (m_metaNodeType) {
        case LOOPER:
            return "Looper";
        case CROSSVALIDATION:
            return "Cross Validation";
        default:
            StringBuilder b = new StringBuilder("Meta Node ");
            b.append(m_dataInNodeIDs.length);
            b.append(" : ");
            b.append(m_dataOutNodeIDs.length);
            return b.toString();
        }
    }

    /** {@inheritDoc} */
    @Override
    protected NodeSettingsRO loadOutPortsSetting(
            final NodeSettingsRO settings)
            throws InvalidSettingsException {
        NodeSettings template = new NodeSettings("outports");
        for (int i = 0; i < m_dataOutNodeIDs.length; i++) {
            NodeSettingsWO sub = template.addNodeSettings("data_" + i);
            sub.addInt("index", i);
            sub.addInt("node", m_dataOutNodeIDs[i]);
            sub.addString("type", "data");
        }
        return template;
    }
    
    /** {@inheritDoc} */
    @Override
    protected NodeSettingsRO loadOutPortsSettingsEnum(
            final NodeSettingsRO settings) throws InvalidSettingsException {
        return settings;
    }
    
    /** {@inheritDoc} */
    @Override
    protected WorkflowPortTemplate loadOutPortTemplate(
            final NodeSettingsRO settings) throws InvalidSettingsException {
        int index = settings.getInt("index");
        String stype = settings.getString("type");
        PortType type;
        if ("data".equals(stype)) {
            type = BufferedDataTable.TYPE;
//        } else if ("model".equals(stype)) {
//            type = NodeModel.OLDSTYLEMODELPORTTYPE;
        } else {
            throw new InvalidSettingsException("Unknown port: " + stype);
        }
        return new WorkflowPortTemplate(index, type);
    }
    
    /** {@inheritDoc} */
    @Override
    protected NodeSettingsRO loadInPortsSetting(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        NodeSettings template = new NodeSettings("inports");
        for (int i = 0; i < m_dataInNodeIDs.length; i++) {
            NodeSettingsWO sub = template.addNodeSettings("data_" + i);
            sub.addInt("index", i);
            sub.addInt("node", m_dataInNodeIDs[i]);
            sub.addString("type", "data");
        }
        return template;
    }
    
    /** {@inheritDoc} */
    @Override
    protected NodeSettingsRO loadInPortsSettingsEnum(
            final NodeSettingsRO settings) throws InvalidSettingsException {
        return settings;
    }
    
    /** {@inheritDoc} */
    @Override
    protected WorkflowPortTemplate loadInPortTemplate(
            final NodeSettingsRO settings) throws InvalidSettingsException {
        return loadOutPortTemplate(settings);
    }
    
    /** {@inheritDoc} */
    @Override
    protected NodeSettingsRO loadSettingsForConnections(
            final NodeSettingsRO set) throws InvalidSettingsException {
        NodeSettingsRO original = super.loadSettingsForConnections(set);
        if (!m_metaNodeType.equals(MetaNodeType.LOOPER)) {
            return original;
        }
        // looper nodes have two more connections (meta-in and out), faking them
        NodeSettings fake = new NodeSettings(original.getKey());
        original.copyTo(fake);
        String wfmInKey;
        int i = 0;
        do {
            wfmInKey = "workflow_connection" + i;
        } while (fake.containsKey(wfmInKey));
        NodeSettingsWO wfmIn = fake.addNodeSettings(wfmInKey);
        // ID is not read anyway...
        wfmIn.addInt("ID", Integer.MAX_VALUE - 1);
        wfmIn.addInt("sourceID", -1);
        wfmIn.addInt("sourcePort", 0);
        wfmIn.addInt("targetID", m_dataInNodeIDs[0]);
        wfmIn.addInt("targetPort", 0);
        String wfmOutKey;
        do {
            wfmOutKey = "workflow_connection" + i++;
        } while (fake.containsKey(wfmOutKey));
        NodeSettingsWO wfmOut = fake.addNodeSettings(wfmOutKey);
        // ID is not read ...
        wfmOut.addInt("ID", Integer.MAX_VALUE - 2);
        wfmOut.addInt("sourceID", m_dataOutNodeIDs[0]);
        wfmOut.addInt("sourcePort", 0);
        wfmOut.addInt("targetID", -1);
        wfmOut.addInt("targetPort", 0);
        return fake;
    }
    
    /** {@inheritDoc} */
    @Override
    protected ConnectionContainerTemplate loadConnection(
            final NodeSettingsRO settings) throws InvalidSettingsException {
        // in the looper node the obsolete data-in and -out nodes are
        // replaced by the loop head and end nodes (which were not present
        // in 1.x.x)
        if (m_metaNodeType.equals(MetaNodeType.LOOPER)) {
            return super.loadConnection(settings);
        }
        int sourceID = settings.getInt("sourceID");
        int destID = loadConnectionDestID(settings);
        int sourcePort = settings.getInt("sourcePort");
        int destPort = loadConnectionDestPort(settings);
        if (doesAnyArrayContain(sourceID, m_dataInNodeIDs)) {
            int i = 0;
            for (; i < m_dataInNodeIDs.length; i++) {
                if (m_dataInNodeIDs[i] == sourceID) {
                    sourcePort = i;
                }
            }
            sourceID = -1;
        }
        if (doesAnyArrayContain(destID, m_dataOutNodeIDs)) {
            int i = 0;
            for (; i < m_dataOutNodeIDs.length; i++) {
                if (m_dataOutNodeIDs[i] == destID) {
                    destPort = i;
                }
            }
            destID = -1;
        }
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
                destPort, /*isDeletable*/true, uiInfo);
    }
    
    private static boolean doesAnyArrayContain(final int value, 
            final int[]... arrays) {
        for (int[] array : arrays) {
            for (int i : array) {
                if (i == value) {
                    return true;
                }
            }
        }
        return false;
    }
    
    private class ObsoleteSpecialNodeSingleNodeContainerPersistorVersion1xx
        extends SingleNodeContainerPersistorVersion1xx {
        
        /**
         * 
         */
        public ObsoleteSpecialNodeSingleNodeContainerPersistorVersion1xx(
                final WorkflowPersistorVersion1xx workflowPersistor) {
            super(workflowPersistor);
        }
        
        /** {@inheritDoc} */
        @Override
        protected String loadNodeFactoryClassName(
                final NodeSettingsRO parentSettings, 
                final NodeSettingsRO settings) throws InvalidSettingsException {
            String f = super.loadNodeFactoryClassName(parentSettings, settings);
            switch (m_metaNodeType) {
            case LOOPER:
                String in = "org.knime.core.node.meta.DataInputNodeFactory";
                String out = "org.knime.core.node.meta.DataOutputNodeFactory";
                if (in.equals(f)) {
                    f = "org.knime.base.node.meta.looper."
                        + "ForLoopHeadNodeFactory";
                } else if (out.equals(f)) {
                    f = "org.knime.base.node.meta.looper."
                        + "ForLoopTailNodeFactory";
                }
                break;
            case CROSSVALIDATION:
                // the x-partitioner and aggregator node have the same name
                // in 1.x and 2.0 - which is good
            default:
            }
            return f;
        }
        
    }
    
}
