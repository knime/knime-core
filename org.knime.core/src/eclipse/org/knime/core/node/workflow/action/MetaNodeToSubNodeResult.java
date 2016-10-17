/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
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
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
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
 *   Nov 12, 2015 (wiswedel): created
 */
package org.knime.core.node.workflow.action;

import org.knime.core.api.node.workflow.action.IMetaNodeToSubNodeResult;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.WorkflowLock;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.node.workflow.WorkflowPersistor;

/**
 * Result object of {@link WorkflowManager#convertMetaNodeToSubNode(org.knime.core.node.workflow.NodeID)}
 *
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 * @noreference This class is not intended to be referenced by clients.
 * @since 3.1
 */
public final class MetaNodeToSubNodeResult implements IMetaNodeToSubNodeResult {

    private NodeID m_nodeID;
    private WorkflowPersistor m_undoPersistor;
    private WorkflowManager m_wfm;

    /** Construct action object - only to be called from WFM.
     * @param wfm The workflow manager
     * @param nodeID the id of the metanode to wrap.
     * @param undoPersistor The persistor to undo the operation.
     */
    public MetaNodeToSubNodeResult(final WorkflowManager wfm, final NodeID nodeID, final WorkflowPersistor undoPersistor) {
        m_wfm = CheckUtils.checkArgumentNotNull(wfm);
        m_nodeID = CheckUtils.checkArgumentNotNull(nodeID);
        m_undoPersistor = CheckUtils.checkArgumentNotNull(undoPersistor);
    }

    /** @return true if undo-able, that is node can be removed (hence converted, then removed). */
    @Override
    public boolean canUndo() {
        return m_wfm.canRemoveNode(m_nodeID);
    }

    /** Perform the undo. */
    @Override
    public void undo() {
        try (WorkflowLock lock = m_wfm.lock()) { // prevent events to be sent too early
            m_wfm.removeNode(m_nodeID);
            m_wfm.paste(m_undoPersistor);
        }
    }

}
