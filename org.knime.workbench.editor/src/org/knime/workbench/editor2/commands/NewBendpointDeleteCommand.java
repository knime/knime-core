/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * -------------------------------------------------------------------
 * 
 * History
 *   ${date} (${user}): created
 */
package org.knime.workbench.editor2.commands;

import org.eclipse.gef.commands.Command;
import org.knime.core.node.workflow.ModellingConnectionExtraInfo;
import org.knime.workbench.editor2.editparts.ConnectionContainerEditPart;

/**
 * Command for deletion of connection bendpoints.
 * 
 * @author Florian Georg, University of Konstanz
 */
public class NewBendpointDeleteCommand extends Command {
    private int m_index;

    private ModellingConnectionExtraInfo m_extraInfo;

    private int[] m_point;

    private ConnectionContainerEditPart m_connection;

    /**
     * New Bendpoint deletion command.
     * 
     * @param connection The connecton container
     * @param index bendpoint index
     */
    public NewBendpointDeleteCommand(
            final ConnectionContainerEditPart connection,
            final int index) {
        m_connection = connection;
        m_extraInfo = (ModellingConnectionExtraInfo) connection
            .getUIInformation();
        m_index = index;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute() {
        m_point = m_extraInfo.getBendpoint(m_index);
        m_extraInfo.removeBendpoint(m_index);

        // issue notfication
        m_connection.setUIInformation(m_extraInfo);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void redo() {
        m_extraInfo.removeBendpoint(m_index);

        // issue notfication
        m_connection.setUIInformation(m_extraInfo);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void undo() {
        m_extraInfo.addBendpoint(m_point[0], m_point[1], m_index);

        // issue notfication
        m_connection.setUIInformation(m_extraInfo);
    }
}
