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

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.workflow.WorkflowPersistor.LoadResult;
import org.knime.core.node.workflow.WorkflowPersistor.WorkflowPortTemplate;
import org.knime.core.node.workflow.def.DefToCoreUtil;
import org.knime.core.workflow.def.ComponentDef;

/**
 * TODO {@link NodeLoader} is a start for getting rid of Persistor interfaces that mix data access and logic.
 *
 * @author hornm
 * @author Carl Witt, KNIME GmbH, Berlin, Germany
 */
public class DefComponentLoader extends DefSingleNodeLoader implements SubNodeContainerPersistor, NodeLoader {

    private ComponentDef m_def;

    /**
     * @param def
     * @param loadHelper
     */
    public DefComponentLoader(final ComponentDef def, final WorkflowLoadHelper loadHelper) {
        super(def, loadHelper);
        m_def = def;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WorkflowPersistor getWorkflowPersistor() {
        return new DefWorkflowPersistor(m_def, m_def.getWorkflow(), m_loadHelper);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WorkflowPortTemplate[] getInPortTemplates() {
        return m_def.getInPorts().stream().map(p -> {
            WorkflowPortTemplate t = new WorkflowPortTemplate(p.getIndex(), DefToCoreUtil.toPortType(p.getPortType()));
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
            WorkflowPortTemplate t = new WorkflowPortTemplate(p.getIndex(), DefToCoreUtil.toPortType(p.getPortType()));
            t.setPortName(p.getName());
            return t;
        }).toArray(WorkflowPortTemplate[]::new);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getVirtualInNodeIDSuffix() {
        return m_def.getVirtualInNodeIDSuffix();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getVirtualOutNodeIDSuffix() {
        return m_def.getVirtualOutNodeIDSuffix();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isHideInWizard() {
        return m_def.getDialogSettings().isHideInWizard();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getCssStyles() {
        return m_def.getDialogSettings().getCssStyles();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SubnodeContainerLayoutStringProvider getSubnodeLayoutStringProvider() {
        return new SubnodeContainerLayoutStringProvider(m_def.getDialogSettings().getLayoutJSON());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SubnodeContainerConfigurationStringProvider getSubnodeConfigurationStringProvider() {
        return new SubnodeContainerConfigurationStringProvider(m_def.getDialogSettings().getConfigurationLayoutJSON());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ComponentMetadata getMetadata() {
        return DefToCoreUtil.toComponentMetadata(m_def.getMetadata());
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
    public NodeContainer getNodeContainer(final WorkflowManager parent, final NodeID id) {
        return new SubNodeContainer(parent, id, this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void preLoadNodeContainer(final WorkflowPersistor parentPersistor, final NodeSettingsRO parentSettings,
        final LoadResult loadResult) throws InvalidSettingsException, IOException {
        // TODO
        throw new IllegalStateException("Not implemented");
    }

}
