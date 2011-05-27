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
 * -------------------------------------------------------------------
 *
 * History
 *   ${date} (${user}): created
 */
package org.knime.workbench.editor2.commands;

import org.eclipse.draw2d.AbsoluteBendpoint;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.gef.editparts.ZoomManager;
import org.knime.core.node.workflow.ConnectionContainer;
import org.knime.core.node.workflow.ConnectionUIInformation;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.workbench.editor2.WorkflowEditor;
import org.knime.workbench.editor2.editparts.ConnectionContainerEditPart;

/**
 * Command for creation of connection bendpoints. The bendpoints are stored in a
 * default implementation of an <code>UIInformation</code> object.
 *
 * @author Florian Georg, University of Konstanz
 */
public class NewBendpointCreateCommand extends AbstractKNIMECommand {
    private final Point m_location;

    private final int m_index;

    private AbsoluteBendpoint m_bendpoint;

    private final ZoomManager m_zoomManager;

    private final NodeID m_destNodeID;

    private final int m_destPort;

    /**
     * New NewBendpointCreateCommand.
     *
     * @param connection The connection model
     * @param manager The workflow manager
     * @param index bendpoint index
     * @param location where ?
     * @param zoomManager The zoom manager
     */
    public NewBendpointCreateCommand(
            final ConnectionContainerEditPart connection,
            final WorkflowManager manager,
            final int index, final Point location,
            final ZoomManager zoomManager) {
        super(manager);
        m_index = index;
        m_location = location;
        m_zoomManager = zoomManager;
        ConnectionContainer cc = connection.getModel();
        m_destNodeID = cc.getDest();
        m_destPort = cc.getDestPort();
    }

    private ConnectionContainer getConnectionContainer() {
        return getHostWFM().getIncomingConnectionFor(m_destNodeID, m_destPort);
    }

    private ConnectionUIInformation getUIInfo(final ConnectionContainer conn) {
        ConnectionUIInformation uiInfo = conn.getUIInfo();
        if (uiInfo == null) {
            uiInfo = new ConnectionUIInformation();
        }
        return uiInfo;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute() {
        ConnectionContainer connection = getConnectionContainer();
        ConnectionUIInformation uiInfo = getUIInfo(connection);
        Point location = m_location.getCopy();
        WorkflowEditor.adaptZoom(m_zoomManager, location, true);

        m_bendpoint = new AbsoluteBendpoint(location);
        m_bendpoint.setLocation(location);
        uiInfo.addBendpoint(m_bendpoint.x, m_bendpoint.y, m_index);

        // we need this to fire some update event up
        connection.setUIInfo(uiInfo);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void redo() {
        ConnectionContainer connection = getConnectionContainer();
        ConnectionUIInformation uiInfo = getUIInfo(connection);
        Point location = m_location.getCopy();
        WorkflowEditor.adaptZoom(m_zoomManager, location, true);

        uiInfo.addBendpoint(location.x, location.y, m_index);

        // we need this to fire some update event up
        connection.setUIInfo(uiInfo);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void undo() {
        ConnectionContainer connection = getConnectionContainer();
        ConnectionUIInformation uiInfo = getUIInfo(connection);
        uiInfo.removeBendpoint(m_index);
        // we need this to fire some update event up
        connection.setUIInfo(uiInfo);
    }
}
