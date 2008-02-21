/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2007
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
 * If you have any quesions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * ---------------------------------------------------------------------
 * 
 * History
 *   20.02.2008 (Fabian Dill): created
 */
package org.knime.workbench.editor2.figures;

import org.eclipse.draw2d.DelegatingLayout;
import org.eclipse.draw2d.RectangleFigure;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;

/**
 * 
 * @author Fabian Dill, University of Konstanz
 */
public abstract class AbstractWorkflowPortBarFigure extends RectangleFigure {
    
    protected static final int WIDTH = 30;
    protected static final int OFFSET = 10;
    
    private Rectangle m_bounds;

    
    public void setUIInfo(final Rectangle uiInfo) {
        m_bounds = uiInfo;
    }
    
    public Rectangle getUIInfo() {
        return m_bounds;
    }
    
    public AbstractWorkflowPortBarFigure() {
        super();
        DelegatingLayout layout = new DelegatingLayout();
        setLayoutManager(layout);
        setBackgroundColor(Display.getCurrent().getSystemColor(
                SWT.COLOR_GRAY));
    }

}
