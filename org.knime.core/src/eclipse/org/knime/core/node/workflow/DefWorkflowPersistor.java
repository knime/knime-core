/*
 * ------------------------------------------------------------------------
 *
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
 * History
 *   May 20, 2021 (hornm): created
 */
package org.knime.core.node.workflow;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.knime.core.internal.ReferencedFile;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeAndBundleInformationPersistor;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.port.PortType;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.workflow.NodeContainer.NodeLocks;
import org.knime.core.node.workflow.def.DefToCoreUtil;
import org.knime.core.util.LoadVersion;
import org.knime.core.util.workflowalizer.AuthorInformation;
import org.knime.core.workflow.def.ComponentDef;
import org.knime.core.workflow.def.MetaNodeDef;
import org.knime.core.workflow.def.NativeNodeDef;
import org.knime.core.workflow.def.NodeDef;
import org.knime.core.workflow.def.NodeRefDef;
import org.knime.core.workflow.def.WorkflowDef;
import org.knime.core.workflow.def.WorkflowProjectDef;

/**
 *
 * @author hornm
 */
public class DefWorkflowPersistor implements WorkflowPersistor, TemplateNodeContainerPersistor {

    private final WorkflowProjectDef m_projectDef;
    private final WorkflowDef m_def;

    private final Map<Integer, NodeLoader> m_nodeContainerLoaderMap;
    private final WorkflowLoadHelper m_loadHelper;
//    private DefNodeContainerMetaPersistor m_metaPersistor;

    /**
     * Persistor for a project - like a workflow with additional metadata: load version, cipher, etc.
     *
     * @param projectDef can be null if the workflow is a sub workflow (e.g.,  metanode) instead of a project
     * @param def description of the workflow project as a POJO
     */
    public DefWorkflowPersistor(final WorkflowProjectDef projectDef, final WorkflowDef def, final WorkflowLoadHelper loadHelper) {
        CheckUtils.checkArgument(projectDef == null || projectDef.getWorkflow() == def,
            "Can not construct a persistor for a workflow that does not belong to the given workflow project.");
        m_projectDef = projectDef;
        m_def = def;
//        m_metaPersistor = new DefNodeContainerMetaPersistor(def, loadHelper);
        m_nodeContainerLoaderMap = new HashMap<>();
        m_loadHelper = loadHelper;
    }

    /**
     * Persistor for a project - like a workflow with additional metadata: load version, cipher, etc.
     *
     * @param projectDef
     */
    public DefWorkflowPersistor(final WorkflowProjectDef projectDef, final WorkflowLoadHelper loadHelper) {
        this(projectDef, projectDef.getWorkflow(), loadHelper);
    }

    /**
     * Persistor for a workflow, i.e., not a project but a sub-workflow within a workflow.
     *
     * @param def
     */
    public DefWorkflowPersistor(final WorkflowDef def, final WorkflowLoadHelper loadHelper) {
        this(null, def, loadHelper);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LoadVersion getLoadVersion() {
        // TODO
        return LoadVersion.UNKNOWN;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isProject() {
        return m_def.isProject();
    }

    /**
     * Populated in
     * {@link #loadNodeContainer(Map, ExecutionMonitor, org.knime.core.node.workflow.WorkflowPersistor.LoadResult)}.
     *
     * {@inheritDoc}
     */
    @Override
    public Map<Integer, ? extends NodeContainerPersistor> getNodeLoaderMap() {
        return m_nodeContainerLoaderMap;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<ConnectionContainerTemplate> getConnectionSet() {
        return m_def.getConnections().stream().map(def -> {
            // TODO non-deletable connections?
            // TODO add and store bendpoints
            return new ConnectionContainerTemplate(def.getSourceID(), def.getSourceID(), def.getDestID(),
                def.getDestID(), false, ConnectionUIInformation.builder().build());
        }).collect(Collectors.toSet());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<ConnectionContainerTemplate> getAdditionalConnectionSet() {
        return Collections.emptySet();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WorkflowDataRepository getWorkflowDataRepository() {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return m_def.getMetadata().getName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WorkflowCipher getWorkflowCipher() {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<FlowVariable> getWorkflowVariables() {
        return Collections.emptyList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Credentials> getCredentials() {
        return Collections.emptyList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<WorkflowAnnotation> getWorkflowAnnotations() {
        return m_def.getAnnotations().stream()
            .map(def -> new WorkflowAnnotation(DefToCoreUtil.toAnnotationData(new AnnotationData(), def)))
            .collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeSettingsRO getWizardExecutionControllerState() {
        // TODO
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WorkflowPortTemplate[] getInPortTemplates() {
        // TODO I assume a workflow (not a metanode) doesn't need this
        return new WorkflowPortTemplate[0];
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WorkflowPortTemplate[] getOutPortTemplates() {
        // TODO I assume a workflow (not a metanode) doesn't need this
        return new WorkflowPortTemplate[0];
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeUIInformation getInPortsBarUIInfo() {
        // TODO I assume a workflow (not a metanode) doesn't need this
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeUIInformation getOutPortsBarUIInfo() {
        // TODO I assume a workflow (not a metanode) doesn't need this
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EditorUIInformation getEditorUIInformation() {
        return DefToCoreUtil.toEditorUIInformation(m_def.getWorkflowEditorSettings());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean mustWarnOnDataLoadError() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MetaNodeTemplateInformation getTemplateInformation() {
        // TODO This should be metanode only
        return null; //DefToCoreUtil.toTemplateInfo(m_def.getTemplateInfo());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AuthorInformation getAuthorInformation() {
        return DefToCoreUtil.toAuthorInformation(m_def.getMetadata().getAuthorInformation());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeContainerMetaPersistor getMetaPersistor() {
        // TODO I wanted to get rid of the meta persistor because
        // all information should be in the def
        // also, workflow is not a node anymore and metapersistor is designed for nodes
        return new NodeContainerMetaPersistor() {

            @Override
            public int getNodeIDSuffix() {
                // needed in the wrapping InsertPersistor's getNodeLoaderMap
                return 0;
            }

            @Override
            public InternalNodeContainerState getState() {
                // TODO do we need to serialize this?
                // TODO is this the right default value?
                return InternalNodeContainerState.IDLE;
            }

            @Override
            public WorkflowLoadHelper getLoadHelper() {
                // needed in NodeContainer#init (not sure though why to use the
                return m_loadHelper;
            }

            @Override
            public void setUIInfo(final NodeUIInformation uiInfo) {
                // TODO Auto-generated method stub

            }

            @Override
            public void setNodeIDSuffix(final int nodeIDSuffix) {
                // TODO Auto-generated method stub

            }

            @Override
            public boolean load(final NodeSettingsRO settings, final NodeSettingsRO parentSettings, final LoadResult loadResult) {
                // TODO Auto-generated method stub
                return false;
            }

            @Override
            public boolean isDirtyAfterLoad() {
                // TODO Auto-generated method stub
                return false;
            }

            @Override
            public NodeUIInformation getUIInfo() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public NodeMessage getNodeMessage() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public NodeLocks getNodeLocks() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public ReferencedFile getNodeContainerDirectory() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public NodeAnnotationData getNodeAnnotationData() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public NodeSettingsRO getExecutionJobSettings() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public NodeExecutionJobManager getExecutionJobManager() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public String getCustomDescription() {
                // TODO Auto-generated method stub
                return null;
            }
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isDirtyAfterLoad() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeContainer getNodeContainer(final WorkflowManager parent, final NodeID id) {
        return parent.createSubWorkflow(this, id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean needsResetAfterLoad() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean mustComplainIfStateDoesNotMatch() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void loadNodeContainer(final Map<Integer, BufferedDataTable> tblRep, final ExecutionMonitor exec,
        final LoadResult loadResult) throws InvalidSettingsException, CanceledExecutionException, IOException {
        // TODO feed load result

        List<NodeRefDef> nodeRefs = m_def.getNodeRefs();
        for (NodeRefDef nodeRef : nodeRefs) {
            NodeDef node = m_def.getNodes().get(nodeRef.getReference());
            NodeLoader loader;
            if (node instanceof MetaNodeDef) {
                loader = new DefMetaNodeLoader((MetaNodeDef)node, m_loadHelper);
            } else if(node instanceof NativeNodeDef) {
               loader = new DefNativeNodeLoader((NativeNodeDef)node, m_loadHelper) ;
            } else if(node instanceof ComponentDef) {
               loader = new DefComponentLoader((ComponentDef)node, m_loadHelper);
            } else {
                throw new IllegalStateException("Unknown node type " + node.getClass().getSimpleName());
            }
//               persistor.preLoadNodeContainer(parent, getWizardExecutionControllerState(), loadResult);

            m_nodeContainerLoaderMap.put(nodeRef.getId(), loader);
        }

        // TODO handle missing node ids

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WorkflowContext getWorkflowContext() {
        WorkflowLoadHelper loadHelper = getMetaPersistor().getLoadHelper();
        return isProject() || loadHelper.isTemplateProject() ? loadHelper.getWorkflowContext() : null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ReferencedFile> getObsoleteNodeDirectories() {
        return Collections.emptyList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InputStream decipherInput(final InputStream input) throws IOException {
        return input;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void postLoad(final WorkflowManager wfm, final LoadResult loadResult) {
        //
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void preLoadNodeContainer(final WorkflowPersistor parentPersistor, final NodeSettingsRO parentSettings,
        final LoadResult loadResult) throws InvalidSettingsException, IOException {
        // TODO Auto-generated method stub

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void guessPortTypesFromConnectedNodes(final NodeAndBundleInformationPersistor nodeInfo,
        final NodeSettingsRO additionalFactorySettings, final ArrayList<PersistorWithPortIndex> upstreamNodes,
        final ArrayList<List<PersistorWithPortIndex>> downstreamNodes) {
        // TODO Auto-generated method stub

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PortType getDownstreamPortType(final int index) {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PortType getUpstreamPortType(final int index) {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setDirtyAfterLoad() {
        // TODO Auto-generated method stub

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setNameOverwrite(final String nameOverwrite) {
        // TODO Auto-generated method stub

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setOverwriteTemplateInformation(final MetaNodeTemplateInformation templateInfo) {
        // TODO Auto-generated method stub

    }

}
