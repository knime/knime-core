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
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.workflow.def.DefToCoreUtil;
import org.knime.core.util.LoadVersion;
import org.knime.core.util.workflowalizer.AuthorInformation;
import org.knime.core.workflow.def.ComponentDef;
import org.knime.core.workflow.def.NativeNodeDef;
import org.knime.core.workflow.def.NodeDef;
import org.knime.core.workflow.def.NodeRefDef;
import org.knime.core.workflow.def.WorkflowDef;

/**
 *
 * @author hornm
 */
public class DefWorkflowPersistor implements WorkflowPersistor {

    private final WorkflowDef m_def;

    private final NodeContainerMetaPersistor m_metaPersistor;

    private final Map<Integer, NodeContainerPersistor> m_nodeContainerLoaderMap;

    /**
     * @param def
     */
    public DefWorkflowPersistor(final WorkflowDef def) {
        m_def = def;
        m_metaPersistor = new DefNodeContainerMetaPersistor(def);
        m_nodeContainerLoaderMap = new HashMap<>();
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
        return m_def.getName();
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
        return m_def.getInPorts().stream().map(p -> {
            WorkflowPortTemplate t = new WorkflowPortTemplate(p.getIndex(), DefToCoreUtil.toPortType(p.getType()));
            t.setPortName(p.getName());
            return t;
        }).toArray(WorkflowPortTemplate[]::new);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WorkflowPortTemplate[] getOutPortTemplates() {
        return m_def.getOutPorts().stream().map(p -> {
            WorkflowPortTemplate t = new WorkflowPortTemplate(p.getIndex(), DefToCoreUtil.toPortType(p.getType()));
            t.setPortName(p.getName());
            return t;
        }).toArray(WorkflowPortTemplate[]::new);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeUIInformation getInPortsBarUIInfo() {
        return DefToCoreUtil.toNodeUIInformation(m_def.getInPortsBarUIInfo());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeUIInformation getOutPortsBarUIInfo() {
        return DefToCoreUtil.toNodeUIInformation(m_def.getOutPortsBarUIInfo());
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
        return DefToCoreUtil.toTemplateInfo(m_def.getTemplateInfo());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AuthorInformation getAuthorInformation() {
        return DefToCoreUtil.toAuthorInformation(m_def.getAuthorInformation());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeContainerMetaPersistor getMetaPersistor() {
        return m_metaPersistor;
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
            NodeContainerPersistor persistor;
            if (node instanceof WorkflowDef) {
                persistor = new DefWorkflowPersistor((WorkflowDef)node);
            } else if(node instanceof NativeNodeDef) {
               persistor = new DefNativeNodeContainerPersistor((NativeNodeDef)node) ;
            } else if(node instanceof ComponentDef) {
               persistor = new DefSubNodeContainerPersistor((ComponentDef)node);
            } else {
                throw new IllegalStateException("Unknown node type " + node.getClass().getSimpleName());
            }
            m_nodeContainerLoaderMap.put(nodeRef.getId(), persistor);
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

}
