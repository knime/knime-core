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
 * History
 *   ${date} (${user}): created
 */
package org.knime.workbench.editor2.commands;

import org.eclipse.draw2d.geometry.Point;
import org.eclipse.gef.editparts.ZoomManager;
import org.knime.core.def.node.workflow.IConnectionContainer;
import org.knime.core.def.node.workflow.IWorkflowManager;
import org.knime.core.node.workflow.ConnectionID;
import org.knime.core.node.workflow.ConnectionUIInformation;
import org.knime.core.node.workflow.ConnectionUIInformation.Builder;
import org.knime.workbench.editor2.WorkflowEditor;
import org.knime.workbench.editor2.editparts.ConnectionContainerEditPart;

/**
 * Command for moving an absolute bendpoint on the connection.
 *
 * @author Florian Georg, University of Konstanz
 */
public class NewBendpointMoveCommand extends AbstractKNIMECommand {
    private Point m_newLocation;

    private Point m_oldLocation;

    private int m_index;

    private ZoomManager m_zoomManager;

    private final ConnectionID m_connID;

    /**
     * New bendpoint move command.
     *
     * @param connection The connection model
     * @param manager The workflow manager that contains the connection.
     * @param index The bendpoint index
     * @param newLocation the new location
     * @param zoomManager The zoom manager.
     */
    public NewBendpointMoveCommand(
            final ConnectionContainerEditPart connection,
            final IWorkflowManager manager,
            final int index, final Point newLocation,
            final ZoomManager zoomManager) {
        super(manager);
        m_connID = connection.getModel().getID();

        m_index = index;
        m_newLocation = newLocation;
        m_zoomManager = zoomManager;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute() {
        IConnectionContainer connection = getConnection();
        ConnectionUIInformation uiInfo = connection.getUIInfo();
        ConnectionUIInformation.Builder uiInfoBuilder = ConnectionUIInformation.builder(connection.getUIInfo());
        int[] p = uiInfo.getBendpoint(m_index);
        m_oldLocation = new Point(p[0], p[1]);

        Point newLocation = m_newLocation.getCopy();
        WorkflowEditor.adaptZoom(m_zoomManager, newLocation, true);

        //TODO for every single bendpoint move a new ConnectionUIInformation object needs to be created
        uiInfoBuilder.removeBendpoint(m_index);
        uiInfoBuilder.addBendpoint(newLocation.x, newLocation.y, m_index);

        // issue notification
        connection.setUIInfo(uiInfoBuilder.build());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void redo() {
        IConnectionContainer connection = getConnection();
        ConnectionUIInformation.Builder uiInfoBuilder = ConnectionUIInformation.builder(connection.getUIInfo());
        uiInfoBuilder.removeBendpoint(m_index);

        Point newLocation = m_newLocation.getCopy();
        WorkflowEditor.adaptZoom(m_zoomManager, newLocation, true);

        uiInfoBuilder.addBendpoint(newLocation.x, newLocation.y, m_index);

        // issue notification
        connection.setUIInfo(uiInfoBuilder.build());

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void undo() {
        IConnectionContainer connection = getConnection();
        ConnectionUIInformation uiInfo = connection.getUIInfo();
        Point oldLocation = m_oldLocation.getCopy();

        Builder builder = ConnectionUIInformation.builder(uiInfo);
        builder.removeBendpoint(m_index);
        builder.addBendpoint(oldLocation.x, oldLocation.y, m_index);

        // issue notification
        connection.setUIInfo(builder.build());
    }

    /**
     * @return */
    private IConnectionContainer getConnection() {
        return getHostWFM().getConnection(m_connID);
    }


}
