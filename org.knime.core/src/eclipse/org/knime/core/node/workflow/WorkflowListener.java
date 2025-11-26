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
 * -------------------------------------------------------------------
 *
 * History
 *   19.04.2005 (georg): created
 *   12.01.2006 (mb): clean up for code review
 */
package org.knime.core.node.workflow;

import org.knime.core.node.NodeLogger;

/**
 * Interface for listeners that receive workflow events.
 *
 * @author Florian Georg, University of Konstanz
 */
public interface WorkflowListener {

    /**
     * Called from the manager if something changed.
     *
     * @param event the event that occurred
     */
    void workflowChanged(final WorkflowEvent event);

    /**
     * Called from the manager if anything has changed, independent of the event. Also called if anything has changed
     * within any nested workflow (i.e. metanode or component).
     *
     * @since 5.10
     */
    default void workflowChanged() {
        //
    }

    /**
     * Called from the manager if something changed.
     * <p>
     * Catches *all* {@link RuntimeException}s that are thrown by the {@link #workflowChanged(WorkflowEvent)}. Use this
     * with caution!
     * </p>
     *
     * @param listener {@link WorkflowListener} instance to notify about state change
     * @param event the event that occurred
     * @since 5.5
     * @noreference This method is not intended to be referenced by clients.
     */
    static void callWorkflowChanged(final WorkflowListener listener, final WorkflowEvent event) {
        if (listener == null) {
            return;
        }
        try {
            if (event == null) {
                listener.workflowChanged();
            } else {
                listener.workflowChanged(event);
            }
        } catch (RuntimeException rex) {
            NodeLogger.getLogger(WorkflowListener.class) //
                .error("Caught an exception while notifying workflow state listeners, "
                    + "skipping throwing to preserve the state change", rex);
        }
    }

}
