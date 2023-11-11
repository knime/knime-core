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
 * Created on Oct 5, 2013 by wiswedel
 */
package org.knime.core.node.workflow;

import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;

import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.port.report.ReportConfiguration;
import org.knime.core.node.workflow.WorkflowPersistor.LoadResult;
import org.knime.core.node.workflow.WorkflowPersistor.WorkflowPortTemplate;

/**
 *
 * @author wiswedel
 * @since 2.10
 */
public class CopySubNodeContainerPersistor
    extends CopySingleNodeContainerPersistor implements SubNodeContainerPersistor {

    private final WorkflowPersistor m_workflowPersistor;
    private final WorkflowPortTemplate[] m_inPortTemplates;
    private final WorkflowPortTemplate[] m_outPortTemplates;
    private final int m_virtualInNodeIDSuffix;
    private final int m_virtualOutNodeIDSuffix;
    private final SubnodeContainerLayoutStringProvider m_subnodeLayoutStringProvider;
    private final SubnodeContainerConfigurationStringProvider m_subnodeConfigurationStringProvider;
    private final boolean m_hideInWizard;
    private final String m_customCSS;
    private ComponentMetadata m_componentMetadata;
    private final MetaNodeTemplateInformation m_templateInformation;
    private final ReportConfiguration m_reportConfiguration;

    /**
     * @param original
     * @param preserveDeletableFlags
     * @param isUndoableDeleteCommand
     * @since 3.7
     */
    public CopySubNodeContainerPersistor(final SubNodeContainer original,
        final boolean preserveDeletableFlags,
        final boolean isUndoableDeleteCommand) {
        super(original, preserveDeletableFlags, isUndoableDeleteCommand);
        m_workflowPersistor = new CopyWorkflowPersistor(original.getWorkflowManager(), preserveDeletableFlags,
            isUndoableDeleteCommand) {
            @Override
            public void postLoad(final WorkflowManager wfm, final LoadResult loadResult) {
                NodeContainerParent ncParent = wfm.getDirectNCParent();
                SubNodeContainer subnode = (SubNodeContainer)ncParent;
                subnode.postLoadWFM();
            }
        };
        // port info from virtual nodes (excludes possible report port)
        final var virtualInNode = original.getVirtualInNode();
        m_inPortTemplates = IntStream.range(0, virtualInNode.getNrOutPorts())
                .mapToObj(i -> new WorkflowPortTemplate(i, virtualInNode.getOutPort(i).getPortType()))
                .toArray(WorkflowPortTemplate[]::new);
        final var virtualOutNode = original.getVirtualOutNode();
        m_outPortTemplates = IntStream.range(0, virtualOutNode.getNrInPorts())
            .mapToObj(i -> new WorkflowPortTemplate(i, virtualOutNode.getInPort(i).getPortType()))
            .toArray(WorkflowPortTemplate[]::new);
        m_virtualInNodeIDSuffix = original.getVirtualInNode().getID().getIndex();
        m_virtualOutNodeIDSuffix = original.getVirtualOutNode().getID().getIndex();
        m_subnodeLayoutStringProvider = original.getSubnodeLayoutStringProvider();
        m_subnodeConfigurationStringProvider = original.getSubnodeConfigurationLayoutStringProvider();
        m_hideInWizard = original.isHideInWizard();
        m_customCSS = new String(original.getCssStyles());
        m_componentMetadata = original.getMetadata();
        m_templateInformation = original.getTemplateInformation().clone();
        m_reportConfiguration = original.getReportConfiguration().orElse(null);
    }

    /** {@inheritDoc} */
    @Override
    public SubNodeContainer getNodeContainer(final WorkflowManager parent, final NodeID id) {
        return new SubNodeContainer(parent, id, this);
    }

    /** {@inheritDoc} */
    @Override
    public void loadNodeContainer(
            final Map<Integer, BufferedDataTable> tblRep,
            final ExecutionMonitor exec, final LoadResult loadResult) {
    }

    /** {@inheritDoc} */
    @Override
    public WorkflowPersistor getWorkflowPersistor() {
        return m_workflowPersistor;
    }

    /** {@inheritDoc} */
    @Override
    public WorkflowPortTemplate[] getInPortTemplates() {
        return m_inPortTemplates;
    }

    /** {@inheritDoc} */
    @Override
    public WorkflowPortTemplate[] getOutPortTemplates() {
        return m_outPortTemplates;
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

    /**
     * {@inheritDoc}
     */
    @Override
    public ComponentMetadata getMetadata() {
        return m_componentMetadata;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MetaNodeTemplateInformation getTemplateInformation() {
        return m_templateInformation;
    }

    @Override
    public Optional<ReportConfiguration> getReportConfiguration() {
        return Optional.ofNullable(m_reportConfiguration);
    }

}

