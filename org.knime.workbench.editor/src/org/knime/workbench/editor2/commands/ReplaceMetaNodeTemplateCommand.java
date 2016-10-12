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
 *   25.03.2015 (tibuch): created
 */
package org.knime.workbench.editor2.commands;

import java.util.Collections;

import org.eclipse.draw2d.geometry.Point;
import org.eclipse.gef.RootEditPart;
import org.knime.core.api.node.workflow.IWorkflowManager;
import org.knime.workbench.editor2.editparts.NodeContainerEditPart;
import org.knime.workbench.explorer.filesystem.AbstractExplorerFileStore;

/**
 * GEF command for replacing a <code>Metanode Template</code> in the <code>WorkflowManager</code>.
 *
 * @author Tim-Oliver Buchholz, KNIME.com, Zurich, Switzerland
 */
public class ReplaceMetaNodeTemplateCommand extends CreateMetaNodeTemplateCommand {

    private NodeContainerEditPart m_node;

    private final RootEditPart m_root;

    private DeleteCommand m_delete;

    private ReplaceHelper m_rh;

    /**
     * @param manager the workflow manager
     * @param templateFolder the folder of the metanode template
     * @param location the insert location of the new metanode template
     * @param snapToGrid should metanode snap to grid
     * @param node which will be replaced by this metanode template
     */
    public ReplaceMetaNodeTemplateCommand(final IWorkflowManager manager,
        final AbstractExplorerFileStore templateFolder, final Point location, final boolean snapToGrid,
        final NodeContainerEditPart node) {
        super(manager, templateFolder, location, snapToGrid);
        m_node = node;
        m_root = node.getRoot();
        m_rh = new ReplaceHelper(manager, m_node.getNodeContainer());

        m_delete = new DeleteCommand(Collections.singleton(m_node), getHostWFM());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canExecute() {
        return super.canExecute() && m_delete.canExecute() && m_rh.replaceNode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute() {
        m_delete.execute();
        super.execute();
        m_rh.reconnect(m_container);
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
