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

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.knime.core.node.port.report.ReportConfiguration;
import org.knime.shared.workflow.def.CipherDef;
import org.knime.shared.workflow.def.ComponentDialogSettingsDef;
import org.knime.shared.workflow.def.ComponentMetadataDef;
import org.knime.shared.workflow.def.ComponentNodeDef;
import org.knime.shared.workflow.def.PortDef;
import org.knime.shared.workflow.def.ReportConfigurationDef;
import org.knime.shared.workflow.def.TemplateInfoDef;
import org.knime.shared.workflow.def.WorkflowDef;
import org.knime.shared.workflow.def.impl.ComponentDialogSettingsDefBuilder;
import org.knime.shared.workflow.def.impl.PortDefBuilder;
import org.knime.shared.workflow.def.impl.PortTypeDefBuilder;
import org.knime.shared.workflow.storage.util.PasswordRedactor;

/**
 * Provides a {@link ComponentNodeDef} view on a component node in a workflow.
 *
 * @author hornm
 * @author Carl Witt, KNIME GmbH, Berlin, Germany
 */
public class SubnodeContainerToDefAdapter extends SingleNodeContainerToDefAdapter implements ComponentNodeDef {

    private final SubNodeContainer m_nc;

    private final WorkflowDef m_workflow;

    /**
     * @param nc
     * @param passwordHandler
     */
    public SubnodeContainerToDefAdapter(final SubNodeContainer nc, final PasswordRedactor passwordHandler) {
        super(nc, passwordHandler);
        m_nc = nc;
        // copy workflow immediately to avoid propagating changes recursively
        // through node container reference held here (not a copy!)
        m_workflow = CoreToDefUtil.copyToDefWithUISettings(m_nc.getWorkflowManager(), m_passwordHandler);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WorkflowDef getWorkflow() {
        return m_workflow;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<PortDef> getInPorts() {
        var result = IntStream.range(0, m_nc.getNrInPorts())//
            .filter(portIdx -> !m_nc.isReportPort(portIdx, true)) //
            .mapToObj(m_nc::getInPort)//
            .map(CoreToDefUtil::toPortDef)//
            .collect(Collectors.toList());
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<PortDef> getOutPorts() {
        return IntStream.range(0, m_nc.getNrOutPorts()) //
            .filter(portIdx -> !m_nc.isReportPort(portIdx, false)) //
            .mapToObj(m_nc::getOutPort)//
            .map(CoreToDefUtil::toPortDef)//
            .collect(Collectors.toList()); // don't use 'toList()' (TODO - result should be read-only)
    }

    private static PortDef modifyToOptional(final PortDef def) {
        var modifiedPortType = new PortTypeDefBuilder(def.getPortType()).setOptional(true) //
                .setHidden(true) //
                .build();
        return new PortDefBuilder(def) //
                .setPortType(modifiedPortType).build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Integer getVirtualInNodeId() {
        return m_nc.getVirtualInNodeID().getIndex();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Integer getVirtualOutNodeId() {
        return m_nc.getVirtualOutNodeID().getIndex();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ComponentDialogSettingsDef getDialogSettings() {
        return new ComponentDialogSettingsDefBuilder()//
                .setConfigurationLayoutJSON(//
                    m_nc.getSubnodeConfigurationLayoutStringProvider().getConfigurationLayoutString())//
                .setLayoutJSON(m_nc.getSubnodeLayoutStringProvider().getLayoutString())//
                .setHideInWizard(m_nc.isHideInWizard())//
                .setCssStyles(m_nc.getCssStyles())//
                .build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ComponentMetadataDef getMetadata() {
        return CoreToDefUtil.toComponentMetadataDef(m_nc.getMetadata());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TemplateInfoDef getTemplateInfo() {
        // unclear if this needs to be a link here or a full template information
        return CoreToDefUtil.toTemplateInfoDef(m_nc.getTemplateInformation());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeTypeEnum getNodeType() {
        return NodeTypeEnum.COMPONENT;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CipherDef getCipher() {
        return m_nc.getWorkflowManager().getWorkflowCipher().toDef();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ReportConfigurationDef getReportConfiguration() {
        return m_nc.getReportConfiguration().map(ReportConfiguration::toDef).orElse(null);
    }

}
