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
 *   18 Aug 2021 (carlwitt): created
 */
package org.knime.core.node.workflow;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.knime.shared.workflow.def.BoundsDef;
import org.knime.shared.workflow.def.CipherDef;
import org.knime.shared.workflow.def.MetaNodeDef;
import org.knime.shared.workflow.def.PortDef;
import org.knime.shared.workflow.def.TemplateLinkDef;
import org.knime.shared.workflow.def.TemplateMetadataDef;
import org.knime.shared.workflow.def.WorkflowDef;
import org.knime.shared.workflow.storage.util.PasswordRedactor;

/**
 *
 * @author Carl Witt, KNIME AG, Zurich, Switzerland
 */
public class MetanodeToDefAdapter extends NodeContainerToDefAdapter implements MetaNodeDef {

    private WorkflowManager m_wfm;

    /**
     * @param wfm
     * @param passwordHandler TODO
     */
    public MetanodeToDefAdapter(final WorkflowManager wfm, final PasswordRedactor passwordHandler) {
        super(wfm, passwordHandler);
        m_wfm = wfm;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<List<PortDef>> getInPorts() {
        if(m_wfm.getNrInPorts() == 0) {
            return Optional.empty();
        }
        return Optional.of(IntStream.range(0, m_wfm.getNrInPorts()).mapToObj(m_wfm::getInPort)
            .map(CoreToDefUtil::toPortDef).collect(Collectors.toList()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<List<PortDef>> getOutPorts() {
        if(m_wfm.getNrOutPorts() == 0) {
            return Optional.empty();
        }
        return Optional.of(IntStream.range(0, m_wfm.getNrOutPorts()).mapToObj(m_wfm::getOutPort).map(CoreToDefUtil::toPortDef)
            .collect(Collectors.toList()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<BoundsDef> getInPortsBarBounds() {
        // getInPortsBarUIInfo is null if the inports bar of a metanode was never changed
        return Optional.ofNullable(m_wfm.getInPortsBarUIInfo()).map(CoreToDefUtil::toBoundsDef);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<BoundsDef> getOutPortsBarBounds() {
        // getOutPortsBarUIInfo is null if the inports bar of a metanode was never changed
        return Optional.ofNullable(m_wfm.getOutPortsBarUIInfo()).map(CoreToDefUtil::toBoundsDef);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<TemplateLinkDef> getTemplateLink() {
        return CoreToDefUtil.toTemplateLinkDef(m_wfm.getTemplateInformation());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WorkflowDef getWorkflow() {
        return new WorkflowManagerToDefAdapter(m_wfm, m_passwordHandler);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeTypeEnum getNodeType() {
        return NodeTypeEnum.META;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<CipherDef> getCipher() {
        return m_wfm.getWorkflowCipher().toDef();
    }

    /**
     * @return empty - a metanode in a workflow is by definition not standalone.
     */
    @Override
    public Optional<TemplateMetadataDef> getTemplateMetadata() {
        return Optional.empty();
    }

}
