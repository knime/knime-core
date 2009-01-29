/* @(#)$RCSfile$ 
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

import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.tools.CellEditorLocator;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.swt.widgets.Text;

import org.knime.workbench.editor2.figures.NodeContainerFigure;

/**
 * The locator to locate the cell editor for the
 * <code>NodeContainerFigure</code>. The cell editor edits the user node
 * name.
 * 
 * @author Christoph Sieb, University of Konstanz
 */
public class UserNodeNameCellEditorLocator implements CellEditorLocator {
    private NodeContainerFigure m_nodeContainerFigure;

    /**
     * Creates a new <code>CellEditorLocator</code> for a user node name cell
     * editor.
     * 
     * @param containerFigure the node container figure which contains the user
     *            node name label
     */
    public UserNodeNameCellEditorLocator(
            final NodeContainerFigure containerFigure) {

        setLabel(containerFigure);
    }

    /**
     * {@inheritDoc}
     */
    public void relocate(final CellEditor celleditor) {

        Text text = (Text)celleditor.getControl();
        Rectangle rect = m_nodeContainerFigure.getNameLabelRectangle();
        m_nodeContainerFigure.translateToAbsolute(rect);
        org.eclipse.swt.graphics.Rectangle trim = text.computeTrim(0, 0, 0, 0);
        rect.translate(trim.x, trim.y);
        rect.width += trim.width;
        rect.height += trim.height;
        text.setBounds(rect.x, rect.y, rect.width, rect.height);
    }

    /**
     * @return the node container figure.
     */
    protected NodeContainerFigure getLabel() {

        return m_nodeContainerFigure;
    }

    /**
     * Sets the node container figure.
     * 
     * @param nodeContainerFigure The stickyNote to set
     */
    protected void setLabel(final NodeContainerFigure nodeContainerFigure) {

        m_nodeContainerFigure = nodeContainerFigure;
    }
}
