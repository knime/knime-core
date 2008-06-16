/*
 * ------------------------------------------------------------------ *
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
 *   20.02.2008 (Fabian Dill): created
 */
package org.knime.workbench.editor2.figures;

import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.geometry.Rectangle;

/**
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class WorkflowOutPortBarFigure extends AbstractWorkflowPortBarFigure {
    
//    private static final NodeLogger LOGGER = NodeLogger.getLogger(
//            WorkflowOutPortBarFigure.class);

    
    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public void paint(final Graphics graphics) {
        Rectangle parent = getParent().getBounds().getCopy();
        // TODO: do we need the ui info or is it enough to ask for a 
        // flag initialized?
        if (!isInitialized()) {
            Rectangle newBounds = new Rectangle(
                    parent.width - WIDTH 
                        - AbstractPortFigure.WF_PORT_SIZE - OFFSET, 
                    OFFSET, WIDTH + AbstractPortFigure.WF_PORT_SIZE, 
                    parent.height - (2 * OFFSET));
            setInitialized(true);
            setBounds(newBounds);
        }
        super.paint(graphics);
    }
    
    @Override
    protected void fillShape(Graphics graphics) {
        graphics.fillRectangle(
                getBounds().x + AbstractPortFigure.WF_PORT_SIZE, 
                getBounds().y, 
                getBounds().width - AbstractPortFigure.WF_PORT_SIZE, 
                getBounds().height);
    }
    
    
    @Override
    protected void outlineShape(Graphics graphics) {
        Rectangle r = getBounds().getCopy();
        r.x += AbstractPortFigure.WF_PORT_SIZE;
        r.width -= AbstractPortFigure.WF_PORT_SIZE;
        int x = r.x + lineWidth / 2;
        int y = r.y + lineWidth / 2;
        int w = r.width - Math.max(1, lineWidth);
        int h = r.height - Math.max(1, lineWidth);
        graphics.drawRectangle(x, y, w, h);
    }
    
}
