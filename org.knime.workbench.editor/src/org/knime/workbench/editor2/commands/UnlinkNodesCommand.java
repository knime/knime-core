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
 *   Apr 3, 2018 (loki): created
 */
package org.knime.workbench.editor2.commands;

import java.util.Collection;

import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.ConnectionContainer;
import org.knime.core.node.workflow.WorkflowManager;

/**
 * This is the Command association to the unlink nodes Action.
 *
 * @author loki der quaeler
 */
public class UnlinkNodesCommand extends AbstractKNIMECommand {
    private static final NodeLogger LOGGER = NodeLogger.getLogger(UnlinkNodesCommand.class);


    private final Collection<ConnectionContainer> m_connectionsToRemove;

    /**
     * The constructor for this class.
     *
     * @param removeTargets a non-null collection of 0 or more connections to be deleted
     * @param wm the <code>WorkflowManager</code> governing the workflow in which the targets exist.
     */
    public UnlinkNodesCommand(final Collection<ConnectionContainer> removeTargets, final WorkflowManager wm) {
        super(wm);

        m_connectionsToRemove = removeTargets;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canExecute() {
        if (!super.canExecute()) {
            return false;
        }

        return !m_connectionsToRemove.isEmpty();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute() {
        final WorkflowManager wm = getHostWFM();

        for (final ConnectionContainer cc : m_connectionsToRemove) {
            try {
                wm.removeConnection(cc);
            } catch (Exception e) {
                LOGGER.error("Could not delete existing connection from " + cc.getSource() + ":" + cc.getSourcePort()
                    + " to " + cc.getDest() + ":" + cc.getDestPort() + " due to: " + e.getMessage(), e);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void undo() {
        final WorkflowManager wm = getHostWFM();

        for (final ConnectionContainer cc : m_connectionsToRemove) {
            try {
                final ConnectionContainer restoredCC =
                    wm.addConnection(cc.getSource(), cc.getSourcePort(), cc.getDest(), cc.getDestPort());

                restoredCC.setUIInfo(cc.getUIInfo());
            } catch (Exception e) {
                LOGGER.error("Could not restore connection from " + cc.getSource() + ":" + cc.getSourcePort()
                    + " to " + cc.getDest() + ":" + cc.getDestPort() + " due to: " + e.getMessage(), e);
            }
        }
    }
}
