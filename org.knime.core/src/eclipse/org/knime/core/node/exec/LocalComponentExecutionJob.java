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
 *   1 Jul 2022 (jasper): created
 */
package org.knime.core.node.exec;

import org.knime.core.node.port.PortObject;
import org.knime.core.node.workflow.NodeStateChangeListener;
import org.knime.core.node.workflow.SubNodeContainer;

/**
 * Job that executes a component in a local thread. It can only execute components, and is configurable.
 *
 * @author Jasper Krauter, KNIME GmbH, Konstanz
 */
public class LocalComponentExecutionJob extends LocalNodeExecutionJob {

    private final SubNodeContainer m_snc;

    private final ThreadComponentExecutionJobManagerSettings m_settings;

    private NodeStateChangeListener m_executionCanceller;

    /**
     * @param snc
     * @param data
     * @param settings
     */
    public LocalComponentExecutionJob(final SubNodeContainer snc, final PortObject[] data,
        final ThreadComponentExecutionJobManagerSettings settings) {
        super(snc, data);
        m_snc = snc;
        m_settings = settings;
    }

    @Override
    protected void beforeExecute() {
        if (m_settings.isCancelOnFailure()) {
            m_executionCanceller = e -> {
                if (m_snc.isThisEventFatal(e) && m_snc.getWorkflowManager().canCancelAll()) {
                    // Cancel execution of the underlying workflow, not the component itself.
                    // This way, the internal error is propagated to the component.
                    m_snc.getWorkflowManager().cancelExecution();
                }
            };
            m_snc.getWorkflowManager().getNodeContainers().stream()
                .forEach(nc -> nc.addNodeStateChangeListener(m_executionCanceller));

        }
    }

    @Override
    protected void afterExecute() {
        if (m_settings.isCancelOnFailure()) {
            m_snc.getWorkflowManager().getNodeContainers().stream()
                .forEach(nc -> nc.removeNodeStateChangeListener(m_executionCanceller));
        }
    }

}
