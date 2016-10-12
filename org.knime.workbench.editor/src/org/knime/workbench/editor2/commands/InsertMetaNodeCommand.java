/*
 * ------------------------------------------------------------------------
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
 * -------------------------------------------------------------------
 *
 */
package org.knime.workbench.editor2.commands;

import java.util.Collections;

import org.eclipse.draw2d.geometry.Point;
import org.eclipse.gef.RootEditPart;
import org.knime.core.api.node.workflow.IConnectionContainer;
import org.knime.core.api.node.workflow.IWorkflowManager;
import org.knime.core.node.workflow.WorkflowPersistor;
import org.knime.workbench.editor2.editparts.ConnectionContainerEditPart;

/**
 * GEF command for adding a <code>metanode</code> to a connection. The new metanode will be connected to the source
 * and target node of the passed connection.
 *
 * @author Tim-Oliver Buchholz, KNIME.com, Zurich, Switzerland
 */
public class InsertMetaNodeCommand extends CreateMetaNodeCommand {
    private IConnectionContainer m_edge;

    private final RootEditPart m_root;

    private DeleteCommand m_delete;

    private InsertHelper m_ih;

    /**
     * @param manager the workflow manager
     * @param persistor the workflow persistor
     * @param location the insert location of the new metanode
     * @param snapToGrid should metanode snap to grid
     * @param edge on which the metanode should be inserted
     */
    public InsertMetaNodeCommand(final IWorkflowManager manager, final WorkflowPersistor persistor,
        final Point location, final boolean snapToGrid, final ConnectionContainerEditPart edge) {
        super(manager, persistor, location, snapToGrid);
        m_root = edge.getRoot();
        m_edge = edge.getModel();
        m_ih = new InsertHelper(getHostWFM(), m_edge);

        // delete command handles undo and restores all connections and node correctly
        m_delete = new DeleteCommand(Collections.singleton(edge), manager);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canExecute() {
        return super.canExecute() && m_delete.canExecute() && m_ih.canInsertNode(true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canRedo() {
        return super.canExecute() && m_delete.canExecute() && m_ih.canInsertNode(false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute() {
        m_delete.execute();
        super.execute();
        m_ih.reconnect(m_container, m_snapToGrid, m_location.x, m_location.y);
        // the connections are not always properly re-drawn after "unmark". (Eclipse bug.) Repaint here.
        m_root.refresh();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void undo() {
        super.undo();
        m_delete.undo();
    }
}
