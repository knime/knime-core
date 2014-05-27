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
 * ---------------------------------------------------------------------
 *
 * Created on Oct 4, 2013 by wiswedel
 */
package org.knime.core.node.workflow;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.knime.core.internal.ReferencedFile;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeAndBundleInformation;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.flowvariable.FlowVariablePortObject;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.wizard.WizardNodeLayoutInfo;
import org.knime.core.node.workflow.FileWorkflowPersistor.LoadVersion;
import org.knime.core.node.workflow.WorkflowPersistor.LoadResult;
import org.knime.core.node.workflow.WorkflowPersistor.WorkflowPortTemplate;
import org.knime.core.util.LockFailedException;

/**
 *
 * @author wiswedel
 * @since 2.10
 */
public class FileSubNodeContainerPersistor extends FileSingleNodeContainerPersistor implements
    SubNodeContainerPersistor {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(FileSubNodeContainerPersistor.class);

    private WorkflowPortTemplate[] m_inPortTemplates;

    private WorkflowPortTemplate[] m_outPortTemplates;

    private final FileWorkflowPersistor m_workflowPersistor;

    private int m_virtualInNodeIDSuffix = -1;

    private int m_virtualOutNodeIDSuffix = -1;

    private Map<Integer, WizardNodeLayoutInfo> m_layoutInfo;

    /**
     * @param workflowPersistor
     * @param nodeSettingsFile
     * @param loadHelper
     * @param version
     */
    public FileSubNodeContainerPersistor(final FileWorkflowPersistor workflowPersistor,
        final ReferencedFile nodeSettingsFile, final WorkflowLoadHelper loadHelper, final LoadVersion version) {
        super(workflowPersistor, nodeSettingsFile, loadHelper, version);
        m_workflowPersistor = new FileWorkflowPersistor(workflowPersistor.getGlobalTableRepository(),
            workflowPersistor.getFileStoreHandlerRepository(),
            new ReferencedFile(nodeSettingsFile.getParent(), WorkflowPersistor.WORKFLOW_FILE),
            getLoadHelper(), getLoadVersion(), false) {
                @Override
                public void postLoad(final WorkflowManager wfm, final LoadResult loadResult) {
                    NodeContainerParent ncParent = wfm.getDirectNCParent();
                    if (!(ncParent instanceof SubNodeContainer)) {
                        String error = String.format("Parent is not instance of %s but %s",
                            SubNodeContainer.class.getSimpleName(),
                            ncParent == null ?  "<null>" : ncParent.getClass().getSimpleName());
                        LOGGER.error(error);
                        setNeedsResetAfterLoad();
                        setDirtyAfterLoad();
                        loadResult.addError(error);
                    } else {
                        SubNodeContainer subnode = (SubNodeContainer)ncParent;
                        try {
                            subnode.postLoadWFM();
                        } catch (Exception e) {
                            String error = String.format("Post-load error (%s): %s",
                                e.getClass().getSimpleName(), e.getMessage());
                            LOGGER.error(error, e);
                            loadResult.addError(error, false);
                            setDirtyAfterLoad();
                            setNeedsResetAfterLoad();
                        }
                    }
                }
        };
    }

    /**
     * Called by {@link org.knime.core.node.Node} to update the message field in the
     * {@link org.knime.core.node.NodeModel} class.
     *
     * @return the msg or null.
     */
    public String getNodeMessage() {
        NodeMessage nodeMessage = getMetaPersistor().getNodeMessage();
        if (nodeMessage != null && !nodeMessage.getMessageType().equals(NodeMessage.Type.RESET)) {
            return nodeMessage.getMessage();
        }
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public SubNodeContainer getNodeContainer(final WorkflowManager wm, final NodeID id) {
        return new SubNodeContainer(wm, id, this);
    }

    /** {@inheritDoc} */
    @Override
    public WorkflowPersistor getWorkflowPersistor() {
        return m_workflowPersistor;
    }

    /** {@inheritDoc} */
    @Override
    public WorkflowPortTemplate[] getOutPortTemplates() {
        return m_outPortTemplates;
    }

    /** {@inheritDoc} */
    @Override
    public WorkflowPortTemplate[] getInPortTemplates() {
        return m_inPortTemplates;
    }

    /** {@inheritDoc} */
    @Override
    public int getVirtualInNodeIDSuffix() {
        return m_virtualInNodeIDSuffix;
    }

    /** {@inheritDoc} */
    @Override
    public int getVirtualOutNodeIDSuffix() {
        return m_virtualOutNodeIDSuffix;
    }

    /** {@inheritDoc} */
    @Override
    public Map<Integer, WizardNodeLayoutInfo> getLayoutInfo() {
        return m_layoutInfo;
    }

    /** {@inheritDoc} */
    @Override
    public void preLoadNodeContainer(final WorkflowPersistor parentPersistor, final NodeSettingsRO parentSettings,
        final LoadResult result) throws InvalidSettingsException, IOException {
        super.preLoadNodeContainer(parentPersistor, parentSettings, result);
        NodeSettingsRO nodeSettings = getNodeSettings(); // assigned by super
        try {
            int i = nodeSettings.getInt("virtual-in-ID");
            CheckUtils.checkSetting(i >= 0, "Node ID < 0: %d", i);
            m_virtualInNodeIDSuffix = i;
        } catch (InvalidSettingsException e) {
            String error = "Can't load virtual input node ID: " + e.getMessage();
            result.addError(error);
            getLogger().error(error, e);
            setDirtyAfterLoad();
        }
        try {
            int i = nodeSettings.getInt("virtual-in-ID");
            CheckUtils.checkSetting(i >= 0, "Node ID < 0: %d", i);
            m_virtualInNodeIDSuffix = i;
        } catch (InvalidSettingsException e) {
            String error = "Can't load virtual input node ID: " + e.getMessage();
            result.addError(error);
            getLogger().error(error, e);
            setDirtyAfterLoad();
        }

        Set<String> inportSetKeys = Collections.emptySet();
        NodeSettingsRO inportsSettings = null;
        try {
            inportsSettings = nodeSettings.getNodeSettings("inports");
            // input of subnode is represented by output of virtual in node.
            inportSetKeys = inportsSettings.keySet();
            // an extra for hidden flow var port
            m_inPortTemplates = new WorkflowPortTemplate[inportSetKeys.size() + 1];
            m_inPortTemplates[0] = new WorkflowPortTemplate(0, FlowVariablePortObject.TYPE_OPTIONAL);
            for (int i = 1; i < m_inPortTemplates.length; i++) { // fallback values, correctly set below
                m_inPortTemplates[i] = new WorkflowPortTemplate(i, BufferedDataTable.TYPE);
            }
        } catch (InvalidSettingsException e) {
            String error = "Can't load virtual input port information: " + e.getMessage();
            result.addError(error);
            getLogger().error(error, e);
            setDirtyAfterLoad();
            m_inPortTemplates = new WorkflowPortTemplate[0];
        }
        for (String key : inportSetKeys) {
            try {
                @SuppressWarnings("null")
                NodeSettingsRO inportSetting = inportsSettings.getNodeSettings(key);
                WorkflowPortTemplate portTemplate = loadPort(inportSetting, inportSetKeys.size());
                m_inPortTemplates[portTemplate.getPortIndex()] = portTemplate;
            } catch (InvalidSettingsException e) {
                String error = "Could not load input port information: " + e.getMessage();
                result.addError(error);
                getLogger().error(error, e);
                setDirtyAfterLoad();
                continue;
            }
        }

        try {
            int i = nodeSettings.getInt("virtual-out-ID");
            CheckUtils.checkSetting(i >= 0, "Node ID < 0: %d", i);
            m_virtualOutNodeIDSuffix = i;
        } catch (InvalidSettingsException e) {
            String error = "Can't load virtual output node ID: " + e.getMessage();
            result.addError(error);
            getLogger().error(error, e);
            setDirtyAfterLoad();
        }

        Set<String> outportSetKeys = Collections.emptySet();
        NodeSettingsRO outportsSettings = null;
        try {
            outportsSettings = nodeSettings.getNodeSettings("outports");
            // output of subnode is represented by input of virtual out node.
            outportSetKeys = outportsSettings.keySet();
            m_outPortTemplates = new WorkflowPortTemplate[outportSetKeys.size() + 1];
            m_outPortTemplates[0] = new WorkflowPortTemplate(0, FlowVariablePortObject.TYPE);
            for (int i = 1; i < m_outPortTemplates.length; i++) { // fallback values, correctly set below
                m_outPortTemplates[i] = new WorkflowPortTemplate(i, BufferedDataTable.TYPE);
            }
        } catch (InvalidSettingsException e) {
            String error = "Can't load virtual output port information: " + e.getMessage();
            result.addError(error);
            getLogger().error(error, e);
            setDirtyAfterLoad();
            m_outPortTemplates = new WorkflowPortTemplate[0];
        }
        for (String key : outportSetKeys) {
            try {
                @SuppressWarnings("null")
                NodeSettingsRO outportSetting = outportsSettings.getNodeSettings(key);
                WorkflowPortTemplate portTemplate = loadPort(outportSetting, outportSetKeys.size());
                m_outPortTemplates[portTemplate.getPortIndex()] = portTemplate;
            } catch (InvalidSettingsException e) {
                String error = "Could not load output port information: " + e.getMessage();
                result.addError(error);
                getLogger().error(error, e);
                setDirtyAfterLoad();
                continue;
            }
        }

        Set<String> layoutSetKeys = Collections.emptySet();
        m_layoutInfo = new HashMap<Integer, WizardNodeLayoutInfo>();
        NodeSettingsRO layoutSettings = null;
        try {
            layoutSettings = nodeSettings.getNodeSettings("layoutInfos");
            layoutSetKeys = layoutSettings.keySet();
            for (String key : layoutSetKeys) {
                NodeSettingsRO singleLayoutSetting = layoutSettings.getNodeSettings(key);
                int nodeID = singleLayoutSetting.getInt("nodeID");
                WizardNodeLayoutInfo layoutInfo = WizardNodeLayoutInfo.loadFromNodeSettings(singleLayoutSetting);
                m_layoutInfo.put(nodeID, layoutInfo);
            }
        } catch (InvalidSettingsException e) {
            String error = "Could not load subnode layout information: " + e.getMessage();
            //result.addError(error);
            getLogger().error(error, e);
            //setDirtyAfterLoad();
        }

        m_workflowPersistor.preLoadNodeContainer(parentPersistor, parentSettings, result);
    }

    private WorkflowPortTemplate loadPort(final NodeSettingsRO portSetting,
        final int max) throws InvalidSettingsException {
        int index = portSetting.getInt("index");
        CheckUtils.checkSetting(index >= 0 && index < max, "Index must be in [0:%d]: %d", max - 1, index);
        NodeSettingsRO portTypeSettings = portSetting.getNodeSettings("type");
        PortType portType = PortType.load(portTypeSettings);
        // one off due to flow var port
        return new WorkflowPortTemplate(index + 1, portType);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void loadNodeContainer(final Map<Integer, BufferedDataTable> tblRep,
        final ExecutionMonitor exec, final LoadResult result)
        throws InvalidSettingsException, CanceledExecutionException, IOException {
        super.loadNodeContainer(tblRep, exec, result);
        m_workflowPersistor.loadNodeContainer(tblRep, exec, result);
    }

    /** {@inheritDoc} */
    @Override
    NodeSettingsRO loadNCAndWashModelSettings(final NodeSettingsRO settingsForNode, final NodeSettingsRO modelSettings,
        final Map<Integer, BufferedDataTable> tblRep, final ExecutionMonitor exec, final LoadResult result)
        throws InvalidSettingsException, CanceledExecutionException, IOException {
        return modelSettings;
    }

    /** {@inheritDoc} */
    @Override
    NodeSettingsRO loadSettingsForNode(final LoadResult loadResult) throws IOException {
        NodeSettingsRO nodeSettings = getNodeSettings();
        if (getLoadVersion().ordinal() < LoadVersion.V280.ordinal()) {
            throw new IOException("No subnodes in version " + getLoadVersion());
        } else {
            return nodeSettings;
        }
    }

    /** {@inheritDoc} */
    @Override
    public void guessPortTypesFromConnectedNodes(final NodeAndBundleInformation nodeInfo,
        final NodeSettingsRO additionalFactorySettings, final ArrayList<PersistorWithPortIndex> upstreamNodes,
        final ArrayList<List<PersistorWithPortIndex>> downstreamNodes) {
        // no applicable for sub nodes
    }

    /** {@inheritDoc} */
    @Override
    public PortType getDownstreamPortType(final int index) {
        if (m_outPortTemplates != null && index < m_outPortTemplates.length) {
            return m_outPortTemplates[index].getPortType();
        }
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public PortType getUpstreamPortType(final int index) {
        if (m_inPortTemplates != null && index < m_inPortTemplates.length) {
            return m_inPortTemplates[index].getPortType();
        }
        return null;
    }

    /**
     * @param subnodeNC
     * @param settings
     * @param exec
     * @param nodeDirRef
     * @param saveHelper
     * @throws LockFailedException
     * @throws CanceledExecutionException
     * @throws IOException
     */
    static void save(final SubNodeContainer subnodeNC, final NodeSettings settings, final ExecutionMonitor exec,
        final ReferencedFile nodeDirRef, final WorkflowSaveHelper saveHelper) throws IOException, CanceledExecutionException,
        LockFailedException {
        NativeNodeContainer virtualInNode = subnodeNC.getVirtualInNode();
        settings.addInt("virtual-in-ID", virtualInNode.getID().getIndex());
        NodeSettingsWO inportsSettings = settings.addNodeSettings("inports");
        // input of subnode is represented by output of virtual in node.
        for (int i = 1; i < virtualInNode.getNrOutPorts(); i++) { // start at one to skip the hidden flow var port
            NodeSettingsWO inportSetting = inportsSettings.addNodeSettings("inport_" + (i - 1));
            inportSetting.addInt("index", i - 1);
            NodeSettingsWO portTypeSettings = inportSetting.addNodeSettings("type");
            virtualInNode.getOutputType(i).save(portTypeSettings);
        }
        NativeNodeContainer virtualOutNode = subnodeNC.getVirtualOutNode();
        settings.addInt("virtual-out-ID", virtualOutNode.getID().getIndex());
        // output of subnode is represented by input of virtual in node.
        NodeSettingsWO outportsSettings = settings.addNodeSettings("outports");
        for (int i = 1; i < virtualOutNode.getNrInPorts(); i++) {  // start at one to skip the hidden flow var port
            NodeSettingsWO inportSetting = outportsSettings.addNodeSettings("outport_" + (i - 1));
            inportSetting.addInt("index", i - 1);
            NodeSettingsWO portTypeSettings = inportSetting.addNodeSettings("type");
            virtualOutNode.getInPort(i).getPortType().save(portTypeSettings);
        }
        Map<Integer, WizardNodeLayoutInfo> layoutInfoMap = subnodeNC.getLayoutInfo();
        Integer[] layoutIDs = layoutInfoMap.keySet().toArray(new Integer[0]);
        NodeSettingsWO layoutInfoSettings = settings.addNodeSettings("layoutInfos");
        for (int i = 0; i < layoutInfoMap.size(); i++) {
            NodeSettingsWO curLayoutInfoSettings = layoutInfoSettings.addNodeSettings("layoutInfo_" + (i));
            curLayoutInfoSettings.addInt("nodeID", layoutIDs[i]);
            WizardNodeLayoutInfo.saveToNodeSettings(curLayoutInfoSettings, layoutInfoMap.get(layoutIDs[i]));
        }
        WorkflowManager workflowManager = subnodeNC.getWorkflowManager();
        FileWorkflowPersistor.save(workflowManager, nodeDirRef, exec, saveHelper);
    }


}
