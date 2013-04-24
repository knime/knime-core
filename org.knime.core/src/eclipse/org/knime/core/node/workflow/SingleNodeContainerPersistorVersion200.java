/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
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
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.knime.core.internal.ReferencedFile;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.Node;
import org.knime.core.node.NodeAndBundleInformation;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodePersistorVersion1xx;
import org.knime.core.node.NodePersistorVersion200;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.workflow.FlowLoopContext.RestoredFlowLoopContext;
import org.knime.core.node.workflow.FlowVariable.Scope;
import org.knime.core.node.workflow.SingleNodeContainer.MemoryPolicy;
import org.knime.core.node.workflow.SingleNodeContainer.SingleNodeContainerSettings;
import org.knime.core.node.workflow.WorkflowPersistorVersion200.LoadVersion;
import org.knime.core.util.FileUtil;

/**
 *
 * @author wiswedel, University of Konstanz
 * @noinstantiate This class is not intended to be instantiated by clients.
 * @noextend This class is not intended to be subclassed by clients.
 */
public class SingleNodeContainerPersistorVersion200 extends
        SingleNodeContainerPersistorVersion1xx {

    private static final NodeLogger SAVE_LOGGER =
        NodeLogger.getLogger(SingleNodeContainerPersistorVersion200.class);

    public static final String NODE_FILE = "node.xml";

    /** Load persistor.
     * @param workflowPersistor
     * @param nodeSettingsFile
     * @param version
     */
    public SingleNodeContainerPersistorVersion200(
            final WorkflowPersistorVersion1xx workflowPersistor,
            final ReferencedFile nodeSettingsFile,
            final WorkflowLoadHelper loadHelper,
            final LoadVersion version) {
        super(workflowPersistor, new NodeContainerMetaPersistorVersion200(
                nodeSettingsFile, loadHelper, version), version);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    NodePersistorVersion200 createNodePersistor(final ReferencedFile nodeConfigFile) {
        return new NodePersistorVersion200(this, getLoadVersion(), nodeConfigFile);
    }

    /** {@inheritDoc} */
    @Override
    NodeAndBundleInformation loadNodeFactoryInfo(final NodeSettingsRO parentSettings,
            final NodeSettingsRO settings) throws InvalidSettingsException {
        return NodeAndBundleInformation.load(settings, getLoadVersion());
    }

    /** {@inheritDoc} */
    @Override
    NodeSettingsRO loadAdditionalFactorySettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        // added in v2.6 (during hittisau 2012) without changing the version
        // number (current load version is V250("2.5.0"))
        if (settings.containsKey("factory_settings")) {
            return settings.getNodeSettings("factory_settings");
        }
        return null;
    }

    /** {@inheritDoc} */
    @Override
    ReferencedFile loadNodeFile(final NodeSettingsRO settings) throws InvalidSettingsException {
        ReferencedFile nodeDir = getMetaPersistor().getNodeContainerDirectory();
        return new ReferencedFile(nodeDir, settings.getString("node_file"));
    }

    /** {@inheritDoc} */
    @Override
    SingleNodeContainerSettings loadSNCSettings(
            final NodeSettingsRO settings,
            final NodePersistorVersion1xx nodePersistor)
    throws InvalidSettingsException {
        if (LoadVersion.V200.equals(getLoadVersion())) {
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
    List<FlowObject> loadFlowObjects(
            final NodeSettingsRO settings)
        throws InvalidSettingsException {
        List<FlowObject> result = new ArrayList<FlowObject>();
        LoadVersion loadVersion = getLoadVersion();
        NodeSettingsRO stackSet;
        if (loadVersion.ordinal() < LoadVersion.V220.ordinal()) {
            stackSet = settings.getNodeSettings("scope_stack");
        } else {
            stackSet = settings.getNodeSettings("flow_stack");
        }
        for (String key : stackSet.keySet()) {
            NodeSettingsRO sub = stackSet.getNodeSettings(key);
            String type = sub.getString("type");
            if ("variable".equals(type)) {
                FlowVariable v = FlowVariable.load(sub);
                result.add(v);
            } else if ("loopcontext".equals(type)) {
                result.add(new RestoredFlowLoopContext());
//                int tailID = sub.getInt("tailID");
            } else if ("loopcontext_execute".equals(type)) {
                result.add(new InnerFlowLoopContext());
            } else if ("loopcontext_inactive".equals(type)) {
                FlowLoopContext flc = new FlowLoopContext();
                flc.inactiveScope(true);
                result.add(flc);
            } else if ("scopecontext".equals(type)) {
                result.add(new FlowScopeContext());
            } else if ("scopecontext_inactive".equals(type)) {
                FlowScopeContext slc = new FlowScopeContext();
                slc.inactiveScope(true);
                result.add(slc);
            } else {
                throw new InvalidSettingsException(
                        "Unknown flow object type: " + type);
            }
        }
        return result;
    }

    /** {@inheritDoc} */
    @Override
    boolean shouldFixModelPortOrder() {
        return false;
    }

    protected static String save(final SingleNodeContainer snc,
            final ReferencedFile nodeDirRef, final ExecutionMonitor exec,
            final boolean isSaveData) throws CanceledExecutionException,
            IOException {
        String settingsDotXML = snc.getParent().getCipherFileName(SETTINGS_FILE_NAME);
        ReferencedFile sncWorkingDirRef = snc.getNodeContainerDirectory();
        if (nodeDirRef.equals(sncWorkingDirRef) && !snc.isDirty()) {
            return settingsDotXML;
        }
        File nodeDir = nodeDirRef.getFile();
        boolean nodeDirExists = nodeDir.exists();
        boolean nodeDirDeleted =
            deleteChildren(nodeDir, SingleNodeContainer.DROP_DIR_NAME);
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
        SAVE_LOGGER.debug(debug);

        // get drop directory in "home" (the designated working dir)
        ReferencedFile nodeDropDirInWDRef = sncWorkingDirRef == null ? null
                : new ReferencedFile(
                        sncWorkingDirRef, SingleNodeContainer.DROP_DIR_NAME);

        ReferencedFile nodeDropDirRef = new ReferencedFile(
                nodeDirRef, SingleNodeContainer.DROP_DIR_NAME);

        // if node container directory is set and we write into a new location
        if (nodeDropDirInWDRef != null
                && !nodeDropDirRef.equals(nodeDropDirInWDRef)) {

            // this code is executed in either of the two cases:
            // - Node was copy&paste from node with drop folder
            //   (its (freshly copied) drop folder is currently in /tmp)
            // - Node is saved into new location (saveAs) -- need to copy
            //   the drop folder there (either from /tmp or from working dir)
            File dropInSource = nodeDropDirRef.getFile();
            File dropInTarget = new File(
                    nodeDir, SingleNodeContainer.DROP_DIR_NAME);
            if (dropInSource.exists()) {
                FileUtil.copyDir(dropInSource, dropInTarget);
            }
        }
        NodeSettings settings = new NodeSettings(SETTINGS_FILE_NAME);
        saveNodeFactory(settings, snc);
        ReferencedFile nodeXMLFileRef =
            saveNodeFileName(snc, settings, nodeDirRef);
        saveFlowObjectStack(settings, snc);
        saveSNCSettings(settings, snc);
        NodeContainerMetaPersistorVersion200.save(settings, snc, nodeDirRef);
        NodePersistorVersion200.save(snc, nodeXMLFileRef, exec, isSaveData
                && snc.getInternalState().equals(InternalNodeContainerState.EXECUTED));
        File nodeSettingsXMLFile = new File(nodeDir, settingsDotXML);
        OutputStream os = new FileOutputStream(nodeSettingsXMLFile);
        os = snc.getParent().cipherOutput(os);
        settings.saveToXML(os);
        if (sncWorkingDirRef == null) {
            // set working dir so that we can unset the dirty flag
            sncWorkingDirRef = nodeDirRef;
            snc.setNodeContainerDirectory(sncWorkingDirRef);
        }
        if (nodeDirRef.equals(sncWorkingDirRef)) {
            snc.unsetDirty();
        }
        exec.setProgress(1.0);
        return settingsDotXML;
    }

    /**
     * @noreference This method is not intended to be referenced by clients.
     */
    protected static void saveNodeFactory(final NodeSettingsWO settings,
            final SingleNodeContainer nc) {
        final Node node = nc.getNode();
        // node info to missing node is the info to the actual instance, not MissingNodeFactory
        NodeAndBundleInformation nodeInfo = node.getNodeAndBundleInformation();
        nodeInfo.save(settings);

        NodeSettingsWO subSets = settings.addNodeSettings("factory_settings");
        node.getFactory().saveAdditionalFactorySettings(subSets);
    }

    protected static ReferencedFile saveNodeFileName(
            final SingleNodeContainer snc, final NodeSettingsWO settings,
            final ReferencedFile nodeDirectoryRef) {
        String fileName = NODE_FILE;
        fileName = snc.getParent().getCipherFileName(fileName);
        settings.addString("node_file", fileName);
        return new ReferencedFile(nodeDirectoryRef, fileName);
    }

    protected static void saveSNCSettings(final NodeSettingsWO settings,
            final SingleNodeContainer snc) {
        snc.saveSNCSettings(settings);
    }

    protected static void saveFlowObjectStack(final NodeSettingsWO settings,
            final SingleNodeContainer nc) {
        NodeSettingsWO stackSet = settings.addNodeSettings("flow_stack");
        FlowObjectStack stack = nc.getOutgoingFlowObjectStack();
        @SuppressWarnings("unchecked")
        Iterable<FlowObject> myObjs = stack == null ? Collections.EMPTY_LIST
                : stack.getFlowObjectsOwnedBy(
                        nc.getID(), /*exclude*/Scope.Local);
        int c = 0;
        for (FlowObject s : myObjs) {
            if (s instanceof FlowVariable) {
                FlowVariable v = (FlowVariable)s;
                NodeSettingsWO sub = stackSet.addNodeSettings("Variable_" + c);
                sub.addString("type", "variable");
                v.save(sub);
            } else if (s instanceof FlowLoopContext) {
                if (!((FlowLoopContext)s).isInactiveScope()) {
                    NodeSettingsWO sub = stackSet.addNodeSettings("Loop_" + c);
                    sub.addString("type", "loopcontext");
                } else {
                    NodeSettingsWO sub = stackSet.addNodeSettings("Inactive_Loop_" + c);
                    sub.addString("type", "loopcontext_inactive");
                }
            } else if (s instanceof InnerFlowLoopContext) {
                NodeSettingsWO sub =
                    stackSet.addNodeSettings("Loop_Execute_" + c);
                sub.addString("type", "loopcontext_execute");
            } else if (s instanceof FlowScopeContext) {
                if (!((FlowScopeContext)s).isInactiveScope()) {
                    NodeSettingsWO sub = stackSet.addNodeSettings("Scope_" + c);
                    sub.addString("type", "scopecontext");
                } else {
                    NodeSettingsWO sub = stackSet.addNodeSettings("Inactive_Scope_" + c);
                    sub.addString("type", "scopecontext_inactive");
                }
            } else {
                SAVE_LOGGER.error("Saving of flow objects of type \""
                        + s.getClass().getSimpleName() +  "\" not implemented");
            }
            c += 1;
        }
    }

    /** Delete content of directory, skipping (direct) childs as given in
     * 2nd argument. Use case is: to delete a node directory but skip its
     * drop folder.
     * @param directory The directory whose content is to be deleted
     * @param exclude A list of direct child names that are to be skipped
     * @return false if directory does not exist, true if non-listed children
     *         are deleted
     */
    private static boolean deleteChildren(
            final File directory, final String... exclude) {
        if (!directory.isDirectory()) {
            return false;
        }
        HashSet<String> excludeSet =
            new HashSet<String>(Arrays.asList(exclude));
        File[] children = directory.listFiles();
        if (children == null) {
            return true;
        }
        boolean success = true;
        for (File f : children) {
            if (!excludeSet.contains(f.getName())) {
                boolean s = FileUtil.deleteRecursively(f);
                success &= s;
            }
        }
        return success;
    }

}
