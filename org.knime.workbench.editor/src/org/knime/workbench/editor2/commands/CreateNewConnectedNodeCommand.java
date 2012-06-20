/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
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
 * Created: Mar 30, 2011
 * Author: ohl
 */
package org.knime.workbench.editor2.commands;

import org.eclipse.draw2d.geometry.Point;
import org.eclipse.gef.EditPartViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.WorkflowManager;

/**
 * Creates a new node - and may auto connect it to another one.
 *
 * @author ohl, University of Konstanz
 */
public class CreateNewConnectedNodeCommand extends AbstractCreateNewConnectedNodeCommand {
    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(CreateNewConnectedNodeCommand.class);
    private final NodeFactory<? extends NodeModel> m_factory;

    /**
     * Creates a new node and connects it to the passed node - if it fits.
     *
     * @param viewer the workflow viewer
     * @param manager The workflow manager that should host the new node
     * @param factory The factory of the Node that should be added
     * @param location Initial visual location of the new node ABSOLTE COORDS!
     * @param connectTo node to which the new node should be connected to
     */
    public CreateNewConnectedNodeCommand(final EditPartViewer viewer,
            final WorkflowManager manager,
            final NodeFactory<? extends NodeModel> factory,
            final Point location, final NodeID connectTo) {
        super(viewer, manager, location, connectTo);
        m_factory = factory;
    }

    /**
     * We can execute, if all components were 'non-null' in the constructor.
     * {@inheritDoc}
     */
    @Override
    public boolean canExecute() {
        return m_factory != null && super.canExecute();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected NodeID createNewNode() {
        // Add node to workflow and get the container
        NodeID newID = null;
        WorkflowManager hostWFM = getHostWFM();
        try {
            newID = hostWFM.createAndAddNode(m_factory);
        } catch (Throwable t) {
            // if fails notify the user
            LOGGER.debug("Node cannot be created.", t);
            MessageBox mb =
                    new MessageBox(Display.getDefault().getActiveShell(),
                            SWT.ICON_WARNING | SWT.OK);
            mb.setText("Node cannot be created.");
            mb.setMessage("The node could not be created "
                    + "due to the following reason:\n" + t.getMessage());
            mb.open();
            return null;
        }
        return newID;
    }
}
