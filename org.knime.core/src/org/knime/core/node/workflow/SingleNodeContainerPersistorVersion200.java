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
 * History
 *   Jan 25, 2008 (wiswedel): created
 */
package org.knime.core.node.workflow;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.knime.core.internal.ReferencedFile;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.Node;
import org.knime.core.node.NodePersistorVersion1xx;
import org.knime.core.node.NodePersistorVersion200;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.workflow.ScopeLoopContext.RestoredScopeLoopContext;
import org.knime.core.node.workflow.ScopeVariable.Type;
import org.knime.core.node.workflow.SingleNodeContainer.MemoryPolicy;
import org.knime.core.node.workflow.SingleNodeContainer.SingleNodeContainerSettings;
import org.knime.core.util.FileUtil;

/**
 * 
 * @author wiswedel, University of Konstanz
 */
public class SingleNodeContainerPersistorVersion200 extends
        SingleNodeContainerPersistorVersion1xx {
    
    private static final String NODE_FILE = "node.xml";

    public SingleNodeContainerPersistorVersion200(
            final WorkflowPersistorVersion200 workflowPersistor,
            final String versionString) {
        super(workflowPersistor, versionString);
    }
    
    /** {@inheritDoc} */
    @Override
    protected NodePersistorVersion200 createNodePersistor() {
        return new NodePersistorVersion200(this);
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
    protected SingleNodeContainerSettings loadSNCSettings(
            final NodeSettingsRO settings, 
            final NodePersistorVersion1xx nodePersistor)
    throws InvalidSettingsException {
        // TODO : don't use hard-coded strings here (what about "2.0.3"?)
        if ("2.0.0".equals(getVersionString())) {
            return super.loadSNCSettings(settings, nodePersistor);
        } else {
            // any version after 2.0 saves the snc settings in the settings.xml
            // (previously these settings were saves as part of the node.xml)
            SingleNodeContainerSettings sncs = 
                new SingleNodeContainerSettings();
            MemoryPolicy p;
            NodeSettingsRO sub =
                    settings.getNodeSettings(Node.CFG_MISC_SETTINGS);
            String memoryPolicy =
                    sub.getString(SingleNodeContainer.CFG_MEMORY_POLICY,
                            MemoryPolicy.CacheSmallInMemory.toString());
            if (memoryPolicy == null) {
                throw new InvalidSettingsException(
                        "Can't use null memory policy.");
            }
            try {
                p = MemoryPolicy.valueOf(memoryPolicy);
            } catch (IllegalArgumentException iae) {
                throw new InvalidSettingsException(
                        "Invalid memory policy: " + memoryPolicy);
            }
            sncs.setMemoryPolicy(p);
            return sncs;
        }
            
    }
    
    /** {@inheritDoc} */
    @Override
    protected List<ScopeObject> loadScopeObjects(
            final NodeSettingsRO settings)
        throws InvalidSettingsException {
        List<ScopeObject> result = new ArrayList<ScopeObject>();
        NodeSettingsRO stackSet = settings.getNodeSettings("scope_stack");
        for (String key : stackSet.keySet()) {
            NodeSettingsRO sub = stackSet.getNodeSettings(key);
            String type = sub.getString("type");
            if ("variable".equals(type)) {
                String name = sub.getString("name");
                String typeS = sub.getString("class");
                if (typeS == null || name == null) {
                    throw new InvalidSettingsException("name or type is null");
                }
                Type varType;
                try {
                    varType = Type.valueOf(typeS);
                } catch (final IllegalArgumentException e) {
                    throw new InvalidSettingsException("invalid type " + typeS);
                }
                ScopeVariable v;
                switch (varType) {
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
            } else if ("loopcontext".equals(type)) {
                result.add(new RestoredScopeLoopContext());
//                int tailID = sub.getInt("tailID");
            } else {
                throw new InvalidSettingsException(
                        "Unknown scope object type: " + type);
            }
        }
        return result;
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
        if (!nodeDir.isDirectory() || !nodeDir.canWrite()) {
                throw new IOException("Unable to write or create directory \""
                        + nodeDirRef + "\"");
        }
        NodeSettings settings = new NodeSettings(SETTINGS_FILE_NAME);
        saveNodeFactoryClassName(settings, snc);
        ReferencedFile nodeXMLFileRef = saveNodeFileName(settings, nodeDirRef);
        saveScopeObjectStack(settings, snc);
        saveSNCSettings(settings, snc);
        NodeContainerMetaPersistorVersion200 metaPersistor = 
            createNodeContainerMetaPersistor(null);
        metaPersistor.save(snc, settings);
        NodePersistorVersion200 persistor = createNodePersistor();
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
        exec.setProgress(1.0);
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
    
    protected void saveSNCSettings(final NodeSettingsWO settings, 
            final SingleNodeContainer snc) {
        snc.saveSNCSettings(settings);
    }
    
    protected void saveScopeObjectStack(final NodeSettingsWO settings,
            final SingleNodeContainer nc) {
        NodeSettingsWO stackSet = settings.addNodeSettings("scope_stack");
        ScopeObjectStack stack = nc.getScopeObjectStack();
        @SuppressWarnings("unchecked")
        Iterable<ScopeObject> myObjs = stack == null ? Collections.EMPTY_LIST
                : stack.getScopeObjectsOwnedBy(nc.getID());
        int c = 0;
        for (ScopeObject s : myObjs) {
            if (s instanceof ScopeVariable) {
                ScopeVariable v = (ScopeVariable)s;
                NodeSettingsWO sub = stackSet.addNodeSettings("Variable_" + c);
                sub.addString("type", "variable");
                sub.addString("name", v.getName());
                sub.addString("class", v.getType().name());
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
            } else if (s instanceof ScopeLoopContext) {
                NodeSettingsWO sub = stackSet.addNodeSettings("Loop_" + c);
                sub.addString("type", "loopcontext");
            } else {
                getLogger().error("Saving of scope objects of type \"" 
                        + s.getClass().getSimpleName() +  "\" not implemented");
            }
            c += 1;
        }
    }


    
    /** {@inheritDoc} */
    @Override
    protected NodeContainerMetaPersistorVersion200 
            createNodeContainerMetaPersistor(final ReferencedFile baseDir) {
        return new NodeContainerMetaPersistorVersion200(baseDir);
    }

}
