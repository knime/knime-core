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
 *   Jun 9, 2008 (wiswedel): created
 */
package org.knime.core.node.workflow;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.knime.core.node.workflow.FlowVariable.Scope;
import org.knime.core.node.workflow.SingleNodeContainer.SingleNodeContainerSettings;

/**
 * Used when a node (or a set of nodes) is copied via the clipboard (Ctrl-C, Ctrl-V). This class
 * represents a single node container copy.
 *
 * @author Bernd Wiswedel, University of Konstanz
 */
abstract class CopySingleNodeContainerPersistor implements SingleNodeContainerPersistor {

    private final List<FlowObject> m_flowObjectList;
    private final SingleNodeContainerSettings m_sncSettings;
    private final CopyNodeContainerMetaPersistor m_metaPersistor;

    /** Create copy persistor.
     * @param original To copy from
     * @param preserveDeletableFlag Whether to keep the "is-deletable" flags
     *        in the target.
     * @param isUndoableDeleteCommand If to keep the location of the node
     *        directories (important for undo of delete commands, see
     *        {@link WorkflowManager#copy(boolean, WorkflowCopyContent)}
     *        for details.)
     */
    CopySingleNodeContainerPersistor(final SingleNodeContainer original,
            final boolean preserveDeletableFlag, final boolean isUndoableDeleteCommand) {
        FlowObjectStack stack = original.getFlowObjectStack();
        List<FlowObject> objs;
        if (stack != null) {
            objs = stack.getFlowObjectsOwnedBy(original.getID(), Scope.Local);
        } else {
            objs = Collections.emptyList();
        }
        m_flowObjectList = new ArrayList<FlowObject>(objs.size());
        for (FlowObject o : objs) {
            m_flowObjectList.add(o.cloneAndUnsetOwner());
        }
        m_sncSettings = original.getSingleNodeContainerSettings().clone();
        m_metaPersistor = new CopyNodeContainerMetaPersistor(original, preserveDeletableFlag, isUndoableDeleteCommand);
    }

    /** {@inheritDoc} */
    @Override
    public List<FlowObject> getFlowObjects() {
        if (m_flowObjectList.isEmpty()) {
            return Collections.emptyList();
        }
        List<FlowObject> clones = new ArrayList<FlowObject>(m_flowObjectList.size());
        for (FlowObject o : m_flowObjectList) {
            clones.add(o.clone());
        }
        return clones;
    }

    /** {@inheritDoc} */
    @Override
    public NodeContainerMetaPersistor getMetaPersistor() {
        return m_metaPersistor;
    }

    /** {@inheritDoc} */
    @Override
    public boolean needsResetAfterLoad() {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isDirtyAfterLoad() {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public boolean mustComplainIfStateDoesNotMatch() {
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public SingleNodeContainerSettings getSNCSettings() {
        return m_sncSettings;
    }

}
