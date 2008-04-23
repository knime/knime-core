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
 *   Jan 25, 2008 (wiswedel): created
 */
package org.knime.core.node.workflow;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.knime.core.data.container.ContainerTable;
import org.knime.core.internal.ReferencedFile;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodePersistorVersion1xx;
import org.knime.core.node.NodePersistorVersion200;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NodePersistor.LoadNodeModelSettingsFailPolicy;
import org.knime.core.node.workflow.ScopeVariable.Type;
import org.knime.core.util.FileUtil;

/**
 * 
 * @author wiswedel, University of Konstanz
 */
public class SingleNodeContainerPersistorVersion200 extends
        SingleNodeContainerPersistorVersion1xx {
    
    private static final String NODE_FILE = "node.xml";

    public SingleNodeContainerPersistorVersion200(
            final HashMap<Integer, ContainerTable> tableRep) {
        super(tableRep);
    }
    
    /** {@inheritDoc} */
    @Override
    protected NodePersistorVersion200 createNodePersistor(
            final LoadNodeModelSettingsFailPolicy failPolicy) {
        return new NodePersistorVersion200(failPolicy);
    }
    
    /** {@inheritDoc} */
    @Override
    protected String loadNodeFactoryClassName(NodeSettingsRO parentSettings,
            NodeSettingsRO settings) throws InvalidSettingsException {
        return settings.getString(KEY_FACTORY_NAME);
    }
    
    @Override
    protected String loadNodeFile(NodeSettingsRO settings) 
        throws InvalidSettingsException {
        return settings.getString("node_file");
    }
    
    /** {@inheritDoc} */
    @Override
    protected List<ScopeObject> loadScopeObjects(
            final NodeSettingsRO settings)
        throws InvalidSettingsException {
        // TODO skip this step
        if (!settings.containsKey("scope_stack")) {
            return Collections.emptyList();
        }
        List<ScopeObject> result = new ArrayList<ScopeObject>();
        NodeSettingsRO stackSet = settings.getNodeSettings("scope_stack");
        for (String key : stackSet.keySet()) {
            NodeSettingsRO sub = stackSet.getNodeSettings(key);
            String name = sub.getString("name");
            String typeS = sub.getString("type");
            if (typeS == null || name == null) {
                throw new InvalidSettingsException("name or type is null");
            }
            Type type;
            try {
                type = Type.valueOf(typeS);
            } catch (final IllegalArgumentException e) {
                throw new InvalidSettingsException("invalid type " + typeS);
            }
            ScopeVariable v;
            switch (type) {
            case DOUBLE:
                v = new ScopeVariable(name, sub.getDouble("value"));
                break;
            case INTEGER:
                v = new ScopeVariable(name, sub.getInt("value"));
                break;
            case STRING:
                v = new ScopeVariable(name, sub.getString("value"));
                break;
            default:
                throw new InvalidSettingsException("Unknown type " + type);
            }
            result.add(v);
        }
        return result;
    }
    
    /** {@inheritDoc} */
    @Override
    protected void loadNodeStateIntoMetaPersistor(
            final NodePersistorVersion1xx nodePersistor) {
    }
    
    /** {@inheritDoc} */
    @Override
    protected boolean shouldFixModelPortOrder() {
        return false;
    }
    
    protected String save(final SingleNodeContainer snc,
            final ReferencedFile nodeDirRef, final ExecutionMonitor exec,
            final boolean isSaveData) throws CanceledExecutionException,
            IOException {
        if (nodeDirRef.equals(snc.getNodeContainerDirectory()) 
                && !snc.isDirty()) {
            return SETTINGS_FILE_NAME;
        }
        File nodeDir = nodeDirRef.getFile();
        FileUtil.deleteRecursively(nodeDir);
        nodeDir.mkdirs();
        if (!nodeDir.isDirectory()) {
                throw new IOException("Unable to read or create directory \""
                        + nodeDirRef + "\"");
        }
        NodeSettings settings = new NodeSettings(SETTINGS_FILE_NAME);
        saveNodeFactoryClassName(settings, snc);
        ReferencedFile nodeXMLFileRef = saveNodeFileName(settings, nodeDirRef);
        saveScopeObjectStack(settings, snc);
        NodeContainerMetaPersistorVersion200 metaPersistor = 
            createNodeContainerMetaPersistor(null);
        metaPersistor.save(snc, settings, exec, isSaveData);
        NodePersistorVersion200 persistor = createNodePersistor(
            translateToFailPolicy(snc.getState()));
        persistor.save(snc.getNode(), nodeXMLFileRef, exec, isSaveData 
                && snc.getState().equals(NodeContainer.State.EXECUTED));
        File nodeSettingsXMLFile = new File(nodeDir, SETTINGS_FILE_NAME);
        settings.saveToXML(new FileOutputStream(nodeSettingsXMLFile));
        if (snc.getNodeContainerDirectory() == null) {
            snc.setNodeContainerDirectory(nodeDirRef);
        }
        if (nodeDirRef.equals(snc.getNodeContainerDirectory())) {
            snc.unsetDirty();
        }
        return SETTINGS_FILE_NAME;
    }
    
    protected void saveNodeFactoryClassName(final NodeSettingsWO settings,
            final SingleNodeContainer nc) {
        String cl = nc.getNode().getFactory().getClass().getName();
        settings.addString(KEY_FACTORY_NAME, cl);
    }
    
    protected ReferencedFile saveNodeFileName(final NodeSettingsWO settings, 
            final ReferencedFile nodeDirectoryRef) {
        String fileName = NODE_FILE;
        settings.addString("node_file", fileName);
        return new ReferencedFile(nodeDirectoryRef, fileName);
    }
    
    protected void saveScopeObjectStack(final NodeSettingsWO settings,
            final SingleNodeContainer nc) {
        NodeSettingsWO stackSet = settings.addNodeSettings("scope_stack");
        ScopeObjectStack stack = nc.getScopeObjectStack();
        @SuppressWarnings("unchecked")
        Iterable<ScopeObject> myObjs = stack == null ? Collections.EMPTY_LIST
                : stack.getScopeObjectsOwnedBy(nc.getID());
        for (ScopeObject s : myObjs) {
            if (s instanceof ScopeVariable) {
                ScopeVariable v = (ScopeVariable)s;
                NodeSettingsWO sub = stackSet.addNodeSettings(v.getName());
                sub.addString("name", v.getName());
                sub.addString("type", v.getType().name());
                switch (v.getType()) {
                case INTEGER:
                    sub.addInt("value", v.getIntValue());
                    break;
                case DOUBLE:
                    sub.addDouble("value", v.getDoubleValue());
                    break;
                case STRING:
                    sub.addString("value", v.getStringValue());
                    break;
                default:
                    assert false : "Unknown variable type: " + v.getType();
                }
            } else {
                NodeLogger.getLogger(getClass()).error(
                        "Saving of scope objects not implemented");
            }
        }
    }


    
    /** {@inheritDoc} */
    @Override
    protected NodeContainerMetaPersistorVersion200 
            createNodeContainerMetaPersistor(final ReferencedFile baseDir) {
        return new NodeContainerMetaPersistorVersion200(baseDir);
    }

}
