/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
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
 *   10.05.2005 (sieb): created
 */
package org.knime.workbench.editor2.directnodeedit;

import org.eclipse.gef.commands.Command;
import org.eclipse.gef.editpolicies.DirectEditPolicy;
import org.eclipse.gef.requests.DirectEditRequest;

import org.knime.workbench.editor2.editparts.NodeContainerEditPart;

/**
 * The edit policy to edit the user node name of a node directly in the figure.
 * 
 * @author Christoph Sieb, University of Konstanz
 */
public class UserNodeNameDirectEditPolicy extends DirectEditPolicy {
    /**
     * {@inheritDoc}
     */
    @Override
    protected Command getDirectEditCommand(final DirectEditRequest edit) {
        String labelText = (String)edit.getCellEditor().getValue();
        NodeContainerEditPart nodePart = (NodeContainerEditPart)getHost();

        UserNodeNameCommand command = new UserNodeNameCommand(
                nodePart.getNodeContainer(), labelText);
        
        return command;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void showCurrentEditValue(final DirectEditRequest request) {
        // hack to prevent async layout from placing the cell editor twice.
        getHostFigure().getUpdateManager().performUpdate();
    }
}
