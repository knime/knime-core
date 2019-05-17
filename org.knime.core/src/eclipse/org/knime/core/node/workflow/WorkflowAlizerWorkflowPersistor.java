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
 *   May 16, 2019 (wiswedel): created
 */
package org.knime.core.node.workflow;

import java.io.IOException;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.util.LoadVersion;
import org.knime.core.util.workflowalizer2.Node;
import org.knime.core.util.workflowalizer2.WorkflowBundle;
import org.knime.core.util.workflowalizer2.Workflowalizer2;

/**
 * @noreference This class is not intended to be referenced by clients.
 * @noextend This class is not intended to be subclassed by clients.
 * @noinstantiate This class is not intended to be instantiated by clients.
 * @author wiswedel
 */
public class WorkflowAlizerWorkflowPersistor extends AbstractStorageWorkflowPersistor {

    private final WorkflowBundle m_workflowBundle;

    /**
     * @param workflowDataRepository
     * @param isProject
     */
    public WorkflowAlizerWorkflowPersistor(final WorkflowBundle workflowBundle,
        final WorkflowLoadHelper loadHelper, final LoadVersion loadVersion,
        final WorkflowDataRepository workflowDataRepository, final boolean isProject) {
        super(new WorkflowAlizerNodeContainerMetaPersistor(loadHelper, loadVersion), workflowDataRepository, isProject);
        m_workflowBundle = workflowBundle;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    NodeSettingsRO readWorkflowSettings() throws IOException {
        return Workflowalizer2.convert(m_workflowBundle.getWorkflow(), new NodeSettings("workflow"));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    String getWorkflowSource() {
        return "MongoDB";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    FromFileNodeContainerPersistor createNodeContainerPersitorLoad(
        final NodeSettingsRO nodeSetting, final NodeType nodeType)
        throws InvalidSettingsException {
        String lookupKey = m_workflowBundle.getWorkflow().getId() + "#" + nodeSetting.getKey();

        Node b = m_workflowBundle.getNodes().stream().filter(n -> n.getId().equals(lookupKey)).findFirst().get();
//        Node b = m_workflowBundle.getNodes().stream().filter(n -> n.getId().equals(lookupKey)).findFirst()
        //                .orElseThrow(s -> new InvalidSettingsException("No node with ID " + lookupKey));
        return new FileNativeNodeContainerPersistor(b, mustWarnOnDataLoadError(), getWorkflowDataRepository(), getLoadHelper(), getLoadVersion());
    }

}
