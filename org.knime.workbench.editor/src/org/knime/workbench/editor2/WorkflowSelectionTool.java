/*
 * ------------------------------------------------------------------
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
 * ---------------------------------------------------------------------
 * 
 * History
 *   28.06.2007 (sieb): created
 */
package org.knime.workbench.editor2;

import org.eclipse.gef.EditPartViewer;
import org.eclipse.gef.tools.SelectionTool;
import org.eclipse.swt.events.MouseEvent;

/**
 * 
 * @author sieb, University of Konstanz
 */
public class WorkflowSelectionTool extends SelectionTool {

    private int m_xLocation;

    private int m_yLocation;

    private boolean m_rightButton;

    /**
     * {@inheritDoc}
     */
    @Override
    public void mouseDown(final MouseEvent e, final EditPartViewer viewer) {

        // remember the location during mouse down
        m_xLocation = e.x;
        m_yLocation = e.y;
        if (e.button == 3) {
            m_rightButton = true;
        } else {
            m_rightButton = false;
        }
        super.mouseDown(e, viewer);
    }

    public boolean getRightButton() {
        return m_rightButton;
    }

    public int getXLocation() {
        return m_xLocation;
    }

    public int getYLocation() {
        return m_yLocation;
    }
}
