/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.ContainerTable;
import org.knime.core.data.container.DataContainer;
import org.knime.core.internal.ReferencedFile;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeAndBundleInformationPersistor;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.PortUtil;
import org.knime.core.node.port.flowvariable.FlowVariablePortObject;
import org.knime.core.node.port.flowvariable.FlowVariablePortObjectSpec;
import org.knime.core.node.port.report.ReportConfiguration;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.workflow.MetaNodeTemplateInformation.Role;
import org.knime.core.node.workflow.WorkflowPersistor.LoadResult;
import org.knime.core.node.workflow.WorkflowPersistor.WorkflowPortTemplate;
import org.knime.core.node.workflow.metadata.MetadataVersion;
import org.knime.core.util.FileUtil;
import org.knime.core.util.LoadVersion;
import org.knime.core.util.LockFailedException;

/**
 *
 * @author wiswedel
 * @since 2.10
 */
public final class FileSubNodeContainerPersistor extends FileSingleNodeContainerPersistor implements
    SubNodeContainerPersistor, TemplateNodeContainerPersistor {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(FileSubNodeContainerPersistor.class);

    private WorkflowPortTemplate[] m_inPortTemplates;

    private WorkflowPortTemplate[] m_outPortTemplates;

    private FileWorkflowPersistor m_workflowPersistor;

    private String m_nameOverwrite;

    private int m_virtualInNodeIDSuffix = -1;

    private int m_virtualOutNodeIDSuffix = -1;

    private SubnodeContainerLayoutStringProvider m_subnodeLayoutStringProvider;

    private SubnodeContainerConfigurationStringProvider m_subnodeConfigurationStringProvider;

    private String m_customCSS;

    private boolean m_hideInWizard;

    private ComponentMetadata m_componentMetadata;

    private MetaNodeTemplateInformation m_templateInformation;

    private Optional<ReportConfiguration> m_reportConfiguration;

    /**
     * @param nodeSettingsFile
     * @param loadHelper
     * @param version
     * @param workflowDataRepository
     * @param mustWarnOnDataLoadError
     * @since 3.7
     */
    public FileSubNodeContainerPersistor(final ReferencedFile nodeSettingsFile, final WorkflowLoadHelper loadHelper,
        final LoadVersion version, final WorkflowDataRepository workflowDataRepository,
        final boolean mustWarnOnDataLoadError) {
        super(nodeSettingsFile, loadHelper, version, workflowDataRepository, mustWarnOnDataLoadError);
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

    /**
     * {@inheritDoc}
     * @since 4.2
     */
    @Override
    public SubnodeContainerLayoutStringProvider getSubnodeLayoutStringProvider() {
        return m_subnodeLayoutStringProvider;
    }

    /**
     * {@inheritDoc}
     * @since 4.3
     */
    @Override
    public SubnodeContainerConfigurationStringProvider getSubnodeConfigurationStringProvider() {
        return m_subnodeConfigurationStringProvider;
    }

    /**
     * {@inheritDoc}
     * @since 3.7
     */
    @Override
    public boolean isHideInWizard() {
        return m_hideInWizard;
    }

    /**
     * {@inheritDoc}
     * @since 3.7
     */
    @Override
    public String getCssStyles() {
        return m_customCSS;
    }

    /** {@inheritDoc} */
    @Override
    public void preLoadNodeContainer(final WorkflowPersistor parentPersistor, final NodeSettingsRO parentSettings,
        final LoadResult result) throws InvalidSettingsException, IOException {
        super.preLoadNodeContainer(parentPersistor, parentSettings, result);
        NodeSettingsRO nodeSettings = getNodeSettings(); // assigned by super
        m_workflowPersistor = createWorkflowPersistor(getMetaPersistor().getNodeSettingsFile(), getWorkflowDataRepository());
        if (m_nameOverwrite != null) {
            m_workflowPersistor.setNameOverwrite(m_nameOverwrite);
            m_nameOverwrite = null;
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

        final var loadVersion = getLoadVersion();
        if (loadVersion.isOlderThan(LoadVersion.V5100)) {
            try {
                m_componentMetadata = ComponentMetadata.load(nodeSettings, loadVersion);
            } catch (InvalidSettingsException e) {
                String error = "Unable to load component metadata: " + e.getMessage();
                getLogger().debug(error, e);
                setDirtyAfterLoad();
                result.addError(error);
                m_componentMetadata = ComponentMetadata.NONE;
            }
        } else {
            final var componentDir = getMetaPersistor().getNodeContainerDirectory().getFile().toPath();
            final var metadataFile = componentDir.resolve(WorkflowPersistor.COMPONENT_METADATA_FILE_NAME);
            try {
                m_componentMetadata = ComponentMetadata.fromXML(metadataFile, MetadataVersion.V1_0);
            } catch (IOException e) {
                String error = "Unable to load component metadata: " + e.getMessage();
                getLogger().debug(error, e);
                setDirtyAfterLoad();
                result.addError(error);
                m_componentMetadata = ComponentMetadata.NONE;
            }
        }

        try {
            if (m_templateInformation != null) {
                // template information was set after construction (this node is a link created from a template)
                assert m_templateInformation.getRole() == Role.Link;
            } else {
                m_templateInformation = MetaNodeTemplateInformation.load(nodeSettings, loadVersion);
                CheckUtils.checkSettingNotNull(m_templateInformation, "No template information");
            }
        } catch (InvalidSettingsException e) {
            String error = "Unable to load workflow template information: " + e.getMessage();
            getLogger().debug(error, e);
            setDirtyAfterLoad();
            result.addError(error);
            m_templateInformation = MetaNodeTemplateInformation.NONE;
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
        List<WorkflowPortTemplate> outPortTemplatesList = new ArrayList<>();
        try {
            outportsSettings = nodeSettings.getNodeSettings("outports");
            // output of subnode is represented by input of virtual out node.
            outportSetKeys = outportsSettings.keySet();
            outPortTemplatesList.add(new WorkflowPortTemplate(0, FlowVariablePortObject.TYPE_OPTIONAL));
            IntStream.range(0, outportSetKeys.size())
                // fallback values, correctly set below
                .forEach(i -> outPortTemplatesList.add(new WorkflowPortTemplate(i + 1, BufferedDataTable.TYPE)));
        } catch (InvalidSettingsException e) {
            String error = "Can't load virtual output port information: " + e.getMessage();
            result.addError(error);
            getLogger().error(error, e);
            setDirtyAfterLoad();
            outPortTemplatesList.clear();
        }
        for (String key : outportSetKeys) {
            try {
                @SuppressWarnings("null")
                NodeSettingsRO outportSetting = outportsSettings.getNodeSettings(key);
                WorkflowPortTemplate portTemplate = loadPort(outportSetting, outportSetKeys.size());
                outPortTemplatesList.set(portTemplate.getPortIndex(), portTemplate);
            } catch (InvalidSettingsException e) {
                String error = "Could not load output port information: " + e.getMessage();
                result.addError(error);
                getLogger().error(error, e);
                setDirtyAfterLoad();
                continue;
            }
        }

        m_reportConfiguration = loadReportConfiguration(nodeSettings, result);

        m_outPortTemplates = outPortTemplatesList.toArray(WorkflowPortTemplate[]::new);

        // added in 3.1, updated with 4.2
        m_subnodeLayoutStringProvider = new SubnodeContainerLayoutStringProvider(nodeSettings.getString("layoutJSON", ""));

        // added in 3.7, load with default values
        m_customCSS = nodeSettings.getString("customCSS", "");
        m_hideInWizard = nodeSettings.getBoolean("hideInWizard", false);

        m_workflowPersistor.preLoadNodeContainer(parentPersistor, parentSettings, result);

        // added in 4.3
        m_subnodeConfigurationStringProvider = new SubnodeContainerConfigurationStringProvider(nodeSettings.getString("configurationLayoutJSON", ""));
    }

    private Optional<ReportConfiguration> loadReportConfiguration(final NodeSettingsRO settings, final LoadResult result) {
        if (getLoadVersion().isOlderThan(LoadVersion.V5100) || !settings.containsKey("reportConfiguration")) {
            return Optional.empty(); // added in 5.1 (AP-20402)
        }
        try {
            final var configSettings = settings.getNodeSettings("reportConfiguration");
            return ReportConfiguration.load(configSettings);
        } catch (InvalidSettingsException e) {
            String error = "Unable to load report output: " + e.getMessage();
            result.addError(error);
            getLogger().error(error, e);
            setDirtyAfterLoad();
        }
        return Optional.empty();
    }

    private FileWorkflowPersistor createWorkflowPersistor(final ReferencedFile nodeSettingsFile,
        final WorkflowDataRepository workflowDataRepository) {
        String workflowKNIME = getNodeSettings().getString("workflow-file", WorkflowPersistor.WORKFLOW_FILE);
        return new FileWorkflowPersistor(workflowDataRepository,
            new ReferencedFile(nodeSettingsFile.getParent(), workflowKNIME), getLoadHelper(),
            getLoadVersion(), false) {
            @Override
            public void postLoad(final WorkflowManager wfm, final LoadResult loadResult) {
                NodeContainerParent ncParent = wfm.getDirectNCParent();
                if (!(ncParent instanceof SubNodeContainer)) {
                    String error =
                        String.format("Parent is not instance of %s but %s", SubNodeContainer.class.getSimpleName(),
                            ncParent == null ? "<null>" : ncParent.getClass().getSimpleName());
                    LOGGER.error(error);
                    setNeedsResetAfterLoad();
                    setDirtyAfterLoad();
                    loadResult.addError(error);
                } else {
                    SubNodeContainer subnode = (SubNodeContainer)ncParent;
                    try {
                        subnode.postLoadWFM();
                    } catch (Exception e) {
                        String error =
                            String.format("Post-load error (%s): %s", e.getClass().getSimpleName(), e.getMessage());
                        LOGGER.error(error, e);
                        loadResult.addError(error, false);
                        setDirtyAfterLoad();
                        setNeedsResetAfterLoad();
                    }
                }
            }
        };
    }

    private static WorkflowPortTemplate loadPort(final NodeSettingsRO portSetting,
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
            throw new IOException("No Component in version " + getLoadVersion());
        } else {
            return nodeSettings;
        }
    }

    /** {@inheritDoc} */
    @Override
    public void guessPortTypesFromConnectedNodes(final NodeAndBundleInformationPersistor nodeInfo,
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

    /** {@inheritDoc} */
    @Override
    public void setNameOverwrite(final String nameOverwrite) {
        if (m_workflowPersistor == null) {
            m_nameOverwrite = nameOverwrite;
        } else {
            m_workflowPersistor.setNameOverwrite(nameOverwrite);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void setOverwriteTemplateInformation(final MetaNodeTemplateInformation templateInfo) {
        m_templateInformation = templateInfo;
    }

    /** {@inheritDoc} */
    @Override
    public LoadVersion getLoadVersion() {
        return super.getLoadVersion();
    }

    /** {@inheritDoc} */
    @Override
    public boolean isProject() {
        return false;
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
            final ReferencedFile nodeDirRef, final WorkflowSaveHelper saveHelper)
            throws IOException, CanceledExecutionException, LockFailedException {
        NativeNodeContainer virtualInNode = subnodeNC.getVirtualInNode();
        // added in 4.3, see AP-15029
        settings.addString("workflow-file", subnodeNC.getCipherFileName(WorkflowPersistor.WORKFLOW_FILE));
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
        var reportConfiguration = subnodeNC.getReportConfiguration();
        reportConfiguration.ifPresent(rf -> rf.save(settings.addNodeSettings("reportConfiguration")));

        // changed in 5.1, see AP-20406
        final var metadataFile = nodeDirRef.getFile().toPath().resolve(WorkflowPersistor.COMPONENT_METADATA_FILE_NAME);
        subnodeNC.getMetadata().toXML(metadataFile);

        subnodeNC.getTemplateInformation().save(settings);
        settings.addString("layoutJSON", subnodeNC.getSubnodeLayoutStringProvider().getLayoutString());
        settings.addString("configurationLayoutJSON", subnodeNC.getSubnodeConfigurationLayoutStringProvider()
            .getConfigurationLayoutString());
        settings.addBoolean("hideInWizard", subnodeNC.isHideInWizard());
        settings.addString("customCSS", subnodeNC.getCssStyles());
        WorkflowManager workflowManager = subnodeNC.getWorkflowManager();
        FileWorkflowPersistor.save(workflowManager, nodeDirRef, exec, saveHelper);
    }

    /**
     * Helper to save example input data with a component.
     *
     * @param portObjects the example data to be stored
     * @param nodeDir the node directory of the component
     * @param exec to report progress while saving the data
     * @return settings that contain the file location and file names, port types, etc.
     */
    static NodeSettingsRO saveExampleInputData(final PortObject[] portObjects, final File nodeDir,
        final ExecutionMonitor exec) throws FileNotFoundException, IOException, CanceledExecutionException {
        final int portObjectsCount = portObjects.length;
        NodeSettings settings = new NodeSettings("exampleInputData");
        String subDirName = "exampleInputData";
        File subDirFile = new File(nodeDir, subDirName);
        settings.addString("location", subDirName);
        NodeSettingsWO portSettings = settings.addNodeSettings("content");
        FileUtil.deleteRecursively(subDirFile);
        subDirFile.mkdirs();

        exec.setMessage("Saving example data");
        for (int i = 0; i < portObjectsCount; i++) {
            PortObject t = portObjects[i];
            String objName = "object_" + i + ".zip";
            ExecutionMonitor subProgress = exec.createSubProgress(1.0 / portObjectsCount);
            NodeSettingsWO singlePortSetting = portSettings.addNodeSettings(objName);
            File portFile = new File(subDirFile, objName);
            singlePortSetting.addInt("index", i);
            if (t == null) {
                singlePortSetting.addString("type", "null");
            } else if (t instanceof BufferedDataTable table) {
                DataContainer.writeToZip(table, portFile, exec);
                singlePortSetting.addString("type", "table");
                singlePortSetting.addString("table_file", objName);
            } else if(t instanceof FlowVariablePortObject) {
                singlePortSetting.addString("type", "flow-vars");
            } else {
                singlePortSetting.addString("type", "non-table");
                singlePortSetting.addString("port_file", objName);
                PortUtil.writeObjectToFile(t, portFile, exec);
            }
            subProgress.setProgress(1.0);
        }
        return settings;
    }

    /**
     * Helper to copy the example input data from one component to another (new) component.
     *
     * @param sourceNodeDir the directory of the source component
     * @param exampleInputDataInfo the 'meta data' of the input data that holds the location, file names, port types
     *            etc.
     * @param targetNodeDir the directory to copy the data
     * @throws InvalidSettingsException if the passed settings don't contain the desired information
     * @throws IOException if the copying failed
     */
    static void copyExampleInputData(final File sourceNodeDir, final NodeSettingsRO exampleInputDataInfo,
        final File targetNodeDir) throws InvalidSettingsException, IOException {
        String dataDirName = exampleInputDataInfo.getString("location");
        FileUtil.copyDir(new File(sourceNodeDir, dataDirName), new File(targetNodeDir, dataDirName));
    }

    /**
     * Helper to load the example data spec stored with a component
     *
     * @param settings settings that contain information required to restore the data, i.e. the location, file names,
     *            port types etc.
     * @param nodeDir the directory of the component to load the data spec from
     * @return the loaded port object specs
     * @throws IOException if the data files couldn't be read
     * @throws InvalidSettingsException if the provided settings are not as expected
     */
    static PortObjectSpec[] loadExampleInputSpecs(final NodeSettingsRO settings, final ReferencedFile nodeDir)
        throws IOException, InvalidSettingsException {
        String subDirName = settings.getString("location");
        ReferencedFile subDirFile = new ReferencedFile(nodeDir, subDirName);
        NodeSettingsRO portSettings = settings.getNodeSettings("content");
        Set<String> keySet = portSettings.keySet();
        PortObjectSpec[] result = new PortObjectSpec[keySet.size()];
        for (String s : keySet) {
            NodeSettingsRO singlePortSetting = portSettings.getNodeSettings(s);
            int index = singlePortSetting.getInt("index");
            if (index < 0 || index >= result.length) {
                throw new InvalidSettingsException("Invalid index: " + index);
            }
            String type = singlePortSetting.getString("type");
            PortObjectSpec spec = null;
            if ("null".equals(type)) {
                // object stays null
            } else if ("table".equals(type)) {
                String fileName = singlePortSetting.getString("table_file");
                if (fileName != null) {
                    File portFile = new File(subDirFile.getFile(), fileName);
                    InputStream in = FileUtil.openInputStream(portFile.toString());
                    // must not use ZipFile here as it is known to have memory problems
                    // on large files, see e.g.
                    // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=5077277
                    try (ZipInputStream zipIn = new ZipInputStream(new BufferedInputStream(in))) {
                        ZipEntry entry = zipIn.getNextEntry();
                        // hardcoded constants here as we do not want additional
                        // functionality to DataContainer ... at least not yet.
                        if ("spec.xml".equals(entry != null ? entry.getName() : "")) {
                            NodeSettingsRO settingsFile = NodeSettings.loadFromXML(zipIn);
                            try {
                                NodeSettingsRO specSettings = settingsFile.getNodeSettings("table.spec");
                                spec = DataTableSpec.load(specSettings);
                            } catch (InvalidSettingsException ise) {
                                IOException ioe = new IOException("Unable to read spec from file");
                                ioe.initCause(ise);
                                throw ioe;
                            }
                        } else {
                            return null;
                        }
                    }
                }
            } else if ("flow-vars".equals(type)) {
                spec = FlowVariablePortObjectSpec.INSTANCE;
            } else if ("non-table".equals(type)) {
                String fileName = singlePortSetting.getString("port_file");
                if (fileName != null) {
                    File portFile = new File(subDirFile.getFile(), fileName);
                    spec = PortUtil.readObjectSpecFromFile(portFile);
                }
            } else {
                CheckUtils.checkSetting(false, "Unknown object reference %s", type);
            }
            result[index] = spec;
        }
        return result;
    }

    /**
     * Helper to load example data stored with a component.
     *
     * @param settings settings that contain information required to restore the data, i.e. the location, file names,
     *            port types etc.
     * @param nodeDir the component directory to load the data from
     * @param exec to report progress, listen to cancel events, and, most important, to create the new buffered data
     *            tables
     * @return the loaded port objects
     * @throws IOException if the data files couldn't be read
     * @throws InvalidSettingsException if the provided settings are not as expected
     */
    static PortObject[] loadExampleInputData(final NodeSettingsRO settings, final ReferencedFile nodeDir,
        final ExecutionContext exec) throws IOException, InvalidSettingsException, CanceledExecutionException {
        String subDirName = settings.getString("location");
        ReferencedFile subDirFile = new ReferencedFile(nodeDir, subDirName);
        NodeSettingsRO portSettings = settings.getNodeSettings("content");
        Set<String> keySet = portSettings.keySet();
        PortObject[] result = new PortObject[keySet.size()];
        for (String s : keySet) {
            ExecutionMonitor subProgress = exec.createSubProgress(1.0 / result.length);
            NodeSettingsRO singlePortSetting = portSettings.getNodeSettings(s);
            int index = singlePortSetting.getInt("index");
            if (index < 0 || index >= result.length) {
                throw new InvalidSettingsException("Invalid index: " + index);
            }
            String type = singlePortSetting.getString("type");
            PortObject object = null;
            if ("null".equals(type)) {
                // object stays null
            } else if ("table".equals(type)) {
                String dataFileName = singlePortSetting.getString("table_file");
                if (dataFileName != null) {
                    File portFile = new File(subDirFile.getFile(), dataFileName);
                    ContainerTable t = DataContainer.readFromZip(portFile);
                    object = exec.createBufferedDataTable(t, subProgress);
                    t.clear();
                }
            } else if("flow-vars".equals(type)) {
                object = FlowVariablePortObject.INSTANCE;
            } else if ("non-table".equals(type)) {
                String dataFileName = singlePortSetting.getString("port_file");
                if (dataFileName != null) {
                    File portFile = new File(subDirFile.getFile(), dataFileName);
                    object = PortUtil.readObjectFromFile(portFile, exec);
                }
            } else {
                CheckUtils.checkSetting(false, "Unknown object reference %s", type);
            }
            result[index] = object;
            subProgress.setProgress(1.0);
        }
        return result;
    }

    @Override
    public ComponentMetadata getMetadata() {
        return m_componentMetadata;
    }

    @Override
    public Optional<ReportConfiguration> getReportConfiguration() {
        return m_reportConfiguration;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MetaNodeTemplateInformation getTemplateInformation() {
        return m_templateInformation;
    }

}
