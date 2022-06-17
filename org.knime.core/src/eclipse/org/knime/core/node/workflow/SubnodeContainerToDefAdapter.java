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
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.knime.shared.workflow.def.CipherDef;
import org.knime.shared.workflow.def.ComponentDialogSettingsDef;
import org.knime.shared.workflow.def.ComponentMetadataDef;
import org.knime.shared.workflow.def.ComponentNodeDef;
import org.knime.shared.workflow.def.PortDef;
import org.knime.shared.workflow.def.TemplateLinkDef;
import org.knime.shared.workflow.def.TemplateMetadataDef;
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

    /**
     * @param nc
     * @param passwordHandler
     */
    public SubnodeContainerToDefAdapter(final SubNodeContainer nc, final PasswordRedactor passwordHandler) {
        super(nc, passwordHandler);
        m_nc = nc;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WorkflowDef getWorkflow() {
        return new WorkflowManagerToDefAdapter(m_nc.getWorkflowManager(), m_passwordHandler);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<List<PortDef>> getInPorts() {
        var result = IntStream.range(0, m_nc.getNrInPorts())//
            .mapToObj(m_nc::getInPort)//
            .map(CoreToDefUtil::toPortDef)//
            .collect(Collectors.toList());
        // Set optional and hidden as true to the first in port type (aka micky mouse)
        var firstInPortDef = CoreToDefUtil.toPortDef(m_nc.getInPort(0));
        result.set(0, modifyToOptional(firstInPortDef));
        return Optional.ofNullable(result.isEmpty() ? null : result);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<List<PortDef>> getOutPorts() {
        var result = IntStream.range(0, m_nc.getNrOutPorts())//
            .mapToObj(m_nc::getOutPort)//
            .map(CoreToDefUtil::toPortDef)//
            .collect(Collectors.toList());
        // Set optional and hidden as true to the first out port type (aka micky mouse)
        var firstOutPortDef = CoreToDefUtil.toPortDef(m_nc.getOutPort(0));
        result.set(0, modifyToOptional(firstOutPortDef));
        return Optional.ofNullable(result.isEmpty() ? null : result);
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
    public Optional<ComponentDialogSettingsDef> getDialogSettings() {
        return Optional.of(new ComponentDialogSettingsDefBuilder()//
                .strict()//
                .setConfigurationLayoutJSON(m_nc.getSubnodeConfigurationLayoutStringProvider().getConfigurationLayoutString())//
                .setLayoutJSON(m_nc.getSubnodeLayoutStringProvider().getLayoutString())//
                .setHideInWizard(m_nc.isHideInWizard())//
                .setCssStyles(m_nc.getCssStyles())//
                .build());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<ComponentMetadataDef> getMetadata() {
        return Optional.of(CoreToDefUtil.toComponentMetadataDef(m_nc.getMetadata()));
    }

    @Override
    public Optional<TemplateMetadataDef> getTemplateMetadata() {
        // this is applied only to nodes in a workflow, which are by definition not standalone components
        return Optional.empty();
    }

    @Override
    public Optional<TemplateLinkDef> getTemplateLink() {
        return CoreToDefUtil.toTemplateLinkDef(m_nc.getTemplateInformation());
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
    public Optional<CipherDef> getCipher() {
        return Optional.of(m_nc.getWorkflowCipher().toDef());
    }

}
