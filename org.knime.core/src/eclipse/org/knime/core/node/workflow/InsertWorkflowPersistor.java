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
 * History
 *   Feb 10, 2009 (wiswedel): created
 */
package org.knime.core.node.workflow;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.knime.core.internal.ReferencedFile;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeAndBundleInformationPersistor;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.port.PortType;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.util.LoadVersion;
import org.knime.core.util.workflowalizer.AuthorInformation;

/**
 * Persistor that is used when a workflow (a project) is loaded. It is used
 * to past a single (meta-) node into ROOT.
 * @author Bernd Wiswedel, University of Konstanz
 */
final class InsertWorkflowPersistor implements WorkflowPersistor {

    private final TemplateNodeContainerPersistor m_nodePersistor;

    /**
     *
     */
    InsertWorkflowPersistor(final TemplateNodeContainerPersistor nodePersistor) {
        CheckUtils.checkArgumentNotNull(nodePersistor, "Must not be null");
        m_nodePersistor = nodePersistor;
    }

    /**
     * TODO: this is a quick fix to reuse the current infrastructure for workflow loading, specifically
     * WorkflowManager#loadContent(NodeContainerPersistor, Map, FlowObjectStack, ExecutionMonitor,
     * org.knime.core.node.workflow.WorkflowPersistor.LoadResult, boolean) Which expected some fields to be overriden
     * temporarily, e.g., replace connections with an empty set (that's the purpose of the InsertWorkflowPersistor) but
     * the InsertPersistor was initially intended for {@link TemplateNodeContainerPersistor}s only.
     *
     * So I wrapped the {@link WorkflowPersistor} in a {@link TemplateNodeContainerPersistor} to then create an
     * {@link InsertWorkflowPersistor} for it.
     *
     * @param persistor
     */
    public InsertWorkflowPersistor(final WorkflowPersistor persistor) {

        // XXX handling this via a dynamic proxy would be nice
        this(new TemplateNodeContainerPersistor() {

            @Override
            public void preLoadNodeContainer(final WorkflowPersistor parentPersistor, final NodeSettingsRO parentSettings,
                final LoadResult loadResult) throws InvalidSettingsException, IOException {
                // TODO workflow persistor doesn't have this -> comes from
                throw new IllegalStateException("not to be called");
            }

            @Override
            public void guessPortTypesFromConnectedNodes(final NodeAndBundleInformationPersistor nodeInfo,
                final NodeSettingsRO additionalFactorySettings, final ArrayList<PersistorWithPortIndex> upstreamNodes,
                final ArrayList<List<PersistorWithPortIndex>> downstreamNodes) {
                // TODO workflow persistor doesn't have this
                throw new IllegalStateException("not to be called");
            }

            @Override
            public boolean isProject() {
                return persistor.isProject();
            }

            public WorkflowContext getWorkflowContext() {
                return persistor.getWorkflowContext();
            }

            public Set<ConnectionContainerTemplate> getConnectionSet() {
                return persistor.getConnectionSet();
            }

            public Set<ConnectionContainerTemplate> getAdditionalConnectionSet() {
                return persistor.getAdditionalConnectionSet();
            }

            public WorkflowDataRepository getWorkflowDataRepository() {
                return persistor.getWorkflowDataRepository();
            }

            public WorkflowPortTemplate[] getInPortTemplates() {
                return persistor.getInPortTemplates();
            }

            public NodeUIInformation getInPortsBarUIInfo() {
                return persistor.getInPortsBarUIInfo();
            }

            @Override
            public LoadVersion getLoadVersion() {
                return persistor.getLoadVersion();
            }

            public String getName() {
                return persistor.getName();
            }

            public WorkflowCipher getWorkflowCipher() {
                return persistor.getWorkflowCipher();
            }

            public MetaNodeTemplateInformation getTemplateInformation() {
                return persistor.getTemplateInformation();
            }

            public AuthorInformation getAuthorInformation() {
                return persistor.getAuthorInformation();
            }

            public List<FlowVariable> getWorkflowVariables() {
                return persistor.getWorkflowVariables();
            }

            public List<Credentials> getCredentials() {
                return persistor.getCredentials();
            }

            public List<WorkflowAnnotation> getWorkflowAnnotations() {
                return persistor.getWorkflowAnnotations();
            }

            public NodeSettingsRO getWizardExecutionControllerState() {
                return persistor.getWizardExecutionControllerState();
            }

            public List<ReferencedFile> getObsoleteNodeDirectories() {
                return persistor.getObsoleteNodeDirectories();
            }

            public Map<Integer, NodeContainerPersistor> getNodeLoaderMap() {
                // FIXME why do signatures differ?
                return (Map<Integer, NodeContainerPersistor>)persistor.getNodeLoaderMap();
            }

            public void postLoad(final WorkflowManager wfm, final LoadResult loadResult) {
                persistor.postLoad(wfm, loadResult);
            }

            public WorkflowPortTemplate[] getOutPortTemplates() {
                return persistor.getOutPortTemplates();
            }

            public NodeUIInformation getOutPortsBarUIInfo() {
                return persistor.getOutPortsBarUIInfo();
            }

            public EditorUIInformation getEditorUIInformation() {
                return persistor.getEditorUIInformation();
            }

            @Override
            public boolean mustWarnOnDataLoadError() {
                return persistor.mustWarnOnDataLoadError();
            }

            @Override
            public NodeContainerMetaPersistor getMetaPersistor() {
                return persistor.getMetaPersistor();
            }

            @Override
            public NodeContainer getNodeContainer(final WorkflowManager parent,
                    final NodeID id) {
                return persistor.getNodeContainer(parent, id);
            }

            @Override
            public boolean isDirtyAfterLoad() {
                return persistor.isDirtyAfterLoad();
            }

            @Override
            public boolean mustComplainIfStateDoesNotMatch() {
                return persistor.mustComplainIfStateDoesNotMatch();
            }

            @Override
            public void loadNodeContainer(final Map<Integer, BufferedDataTable> tblRep,
                    final ExecutionMonitor exec, final LoadResult loadResult) throws InvalidSettingsException, CanceledExecutionException, IOException {
                persistor.loadNodeContainer(tblRep, exec, loadResult);
            }

            @Override
            public boolean needsResetAfterLoad() {
                return persistor.needsResetAfterLoad();
            }

            public InputStream decipherInput(final InputStream input) throws IOException {
                return persistor.decipherInput(input);
            }

            @Override
            public PortType getDownstreamPortType(final int index) {
                throw new IllegalStateException("not to be called");
            }

            @Override
            public PortType getUpstreamPortType(final int index) {
                throw new IllegalStateException("not to be called");
            }

            @Override
            public void setDirtyAfterLoad() {
                throw new IllegalStateException("not to be called");

            }

            @Override
            public void setNameOverwrite(final String nameOverwrite) {
                throw new IllegalStateException("not to be called");

            }

            @Override
            public void setOverwriteTemplateInformation(final MetaNodeTemplateInformation templateInfo) {
                throw new IllegalStateException("not to be called");
            }
        });
    }

    /** {@inheritDoc} */
    @Override
    public boolean isProject() {
        throw new IllegalStateException("not to be called");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WorkflowContext getWorkflowContext() {
        throw new IllegalStateException("not to be called");
    }

    /** {@inheritDoc} */
    @Override
    public Set<ConnectionContainerTemplate> getConnectionSet() {
        return Collections.emptySet();
    }

    /** {@inheritDoc} */
    @Override
    public Set<ConnectionContainerTemplate> getAdditionalConnectionSet() {
        return Collections.emptySet();
    }

    /** {@inheritDoc} */
    @Override
    public WorkflowDataRepository getWorkflowDataRepository() {
        throw new IllegalStateException("no filestore repository for root wfm");
    }

    /** {@inheritDoc} */
    @Override
    public WorkflowPortTemplate[] getInPortTemplates() {
        throw new IllegalStateException("no imports on root wfm");
    }

    /** {@inheritDoc} */
    @Override
    public NodeUIInformation getInPortsBarUIInfo() {
        throw new IllegalStateException("no ui information on root wfm");
    }

    /** {@inheritDoc} */
    @Override
    public LoadVersion getLoadVersion() {
        return m_nodePersistor.getLoadVersion();
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        throw new IllegalStateException("can't set name on root");
    }

    /** {@inheritDoc} */
    @Override
    public WorkflowCipher getWorkflowCipher() {
        throw new IllegalStateException("no workflow cipher on root");
    }

    /** {@inheritDoc} */
    @Override
    public MetaNodeTemplateInformation getTemplateInformation() {
        throw new IllegalStateException("No template information on root");
    }

    /** {@inheritDoc} */
    @Override
    public AuthorInformation getAuthorInformation() {
        throw new IllegalStateException("No author information on root");
    }

    /** {@inheritDoc} */
    @Override
    public List<FlowVariable> getWorkflowVariables() {
        throw new IllegalStateException("can't set workflow variables on root");
    }

    /** {@inheritDoc} */
    @Override
    public List<Credentials> getCredentials() {
        throw new IllegalStateException("can't set credentials on root");
    }

    /** {@inheritDoc} */
    @Override
    public List<WorkflowAnnotation> getWorkflowAnnotations() {
        return Collections.emptyList();
    }

    /** {@inheritDoc} */
    @Override
    public NodeSettingsRO getWizardExecutionControllerState() {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public List<ReferencedFile> getObsoleteNodeDirectories() {
        return Collections.emptyList();
    }

    /** {@inheritDoc} */
    @Override
    public Map<Integer, NodeContainerPersistor> getNodeLoaderMap() {
        return Collections.singletonMap(
                m_nodePersistor.getMetaPersistor().getNodeIDSuffix(),
                (NodeContainerPersistor)m_nodePersistor);
    }

    /** {@inheritDoc} */
    @Override
    public void postLoad(final WorkflowManager wfm, final LoadResult loadResult) {
    }

    /** {@inheritDoc} */
    @Override
    public WorkflowPortTemplate[] getOutPortTemplates() {
        throw new IllegalStateException("no outports on root wfm");
    }

    /** {@inheritDoc} */
    @Override
    public NodeUIInformation getOutPortsBarUIInfo() {
        throw new IllegalStateException("no ui information on root wfm");
    }

    /** {@inheritDoc} */
    @Override
    public EditorUIInformation getEditorUIInformation() {
        throw new IllegalStateException("no editor information on root wfm");
    }

    /** {@inheritDoc} */
    @Override
    public boolean mustWarnOnDataLoadError() {
        return m_nodePersistor.mustWarnOnDataLoadError();
    }

    /** {@inheritDoc} */
    @Override
    public NodeContainerMetaPersistor getMetaPersistor() {
        throw new IllegalStateException("no meta persistor for root wfm");
    }

    /** {@inheritDoc} */
    @Override
    public NodeContainer getNodeContainer(final WorkflowManager parent,
            final NodeID id) {
        throw new IllegalStateException("root has no parent, can't add node");
    }

    /** {@inheritDoc} */
    @Override
    public boolean isDirtyAfterLoad() {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public boolean mustComplainIfStateDoesNotMatch() {
        throw new IllegalStateException("root has not meaningful state");
    }

    /** {@inheritDoc} */
    @Override
    public void loadNodeContainer(final Map<Integer, BufferedDataTable> tblRep,
            final ExecutionMonitor exec, final LoadResult loadResult) {
        // no op
    }

    /** {@inheritDoc} */
    @Override
    public boolean needsResetAfterLoad() {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public InputStream decipherInput(final InputStream input) {
        throw new IllegalStateException("Method not to be called.");
    }

}
