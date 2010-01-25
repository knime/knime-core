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
import org.knime.core.node.workflow.FlowLoopContext.RestoredFlowLoopContext;
import org.knime.core.node.workflow.SingleNodeContainer.MemoryPolicy;
import org.knime.core.node.workflow.SingleNodeContainer.SingleNodeContainerSettings;
import org.knime.core.node.workflow.WorkflowPersistorVersion200.LoadVersion;
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
    protected String loadNodeFactoryClassName(final NodeSettingsRO parentSettings,
            final NodeSettingsRO settings) throws InvalidSettingsException {
        return settings.getString(KEY_FACTORY_NAME);
    }
    
    @Override
    protected String loadNodeFile(final NodeSettingsRO settings) 
        throws InvalidSettingsException {
        return settings.getString("node_file");
    }
    
    /** {@inheritDoc} */
    @Override
    protected SingleNodeContainerSettings loadSNCSettings(
            final NodeSettingsRO settings, 
            final NodePersistorVersion1xx nodePersistor)
    throws InvalidSettingsException {
        if (LoadVersion.V200.equals(LoadVersion.get(getVersionString()))) {
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
    protected List<FlowObject> loadFlowObjects(
            final NodeSettingsRO settings)
        throws InvalidSettingsException {
        List<FlowObject> result = new ArrayList<FlowObject>();
        NodeSettingsRO stackSet = settings.getNodeSettings("scope_stack");
        for (String key : stackSet.keySet()) {
            NodeSettingsRO sub = stackSet.getNodeSettings(key);
            String type = sub.getString("type");
            if ("variable".equals(type)) {
                FlowVariable v = FlowVariable.load(sub);
                result.add(v);
            } else if ("loopcontext".equals(type)) {
                result.add(new RestoredFlowLoopContext());
//                int tailID = sub.getInt("tailID");
            } else {
                throw new InvalidSettingsException(
                        "Unknown flow object type: " + type);
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
        boolean nodeDirExists = nodeDir.exists();
        boolean nodeDirDeleted = FileUtil.deleteRecursively(nodeDir);
        nodeDir.mkdirs();
        if (!nodeDir.isDirectory() || !nodeDir.canWrite()) {
                throw new IOException("Unable to write or create directory \""
                        + nodeDirRef + "\"");
        }
        String debug;
        if (nodeDirExists) {
            if (nodeDirDeleted) {
                debug = "Replaced node directory \"" + nodeDirRef + "\"";
            } else {
                debug = "Failed to replace node directory \"" + nodeDirRef 
                    + "\" -- writing into existing directory";
            }
        } else {
            debug = "Created node directory \"" + nodeDirRef + "\"";
        }
        getLogger().debug(debug);
        NodeSettings settings = new NodeSettings(SETTINGS_FILE_NAME);
        saveNodeFactoryClassName(settings, snc);
        ReferencedFile nodeXMLFileRef = saveNodeFileName(settings, nodeDirRef);
        saveFlowObjectStack(settings, snc);
        saveSNCSettings(settings, snc);
        NodeContainerMetaPersistorVersion200 metaPersistor = 
            createNodeContainerMetaPersistor(nodeDirRef);
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
    
    protected void saveFlowObjectStack(final NodeSettingsWO settings,
            final SingleNodeContainer nc) {
        NodeSettingsWO stackSet = settings.addNodeSettings("scope_stack");
        FlowObjectStack stack = nc.getFlowObjectStack();
        @SuppressWarnings("unchecked")
        Iterable<FlowObject> myObjs = stack == null ? Collections.EMPTY_LIST
                : stack.getFlowObjectsOwnedBy(nc.getID());
        int c = 0;
        for (FlowObject s : myObjs) {
            if (s instanceof FlowVariable) {
                FlowVariable v = (FlowVariable)s;
                NodeSettingsWO sub = stackSet.addNodeSettings("Variable_" + c);
                sub.addString("type", "variable");
                v.save(sub);
            } else if (s instanceof FlowLoopContext) {
                NodeSettingsWO sub = stackSet.addNodeSettings("Loop_" + c);
                sub.addString("type", "loopcontext");
            } else {
                getLogger().error("Saving of flow objects of type \"" 
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
