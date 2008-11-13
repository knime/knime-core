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

import org.eclipse.draw2d.DelegatingLayout;
import org.eclipse.draw2d.RectangleFigure;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.workbench.editor2.commands.ChangeWorkflowPortBarCommand;
import org.knime.workbench.editor2.editparts.WorkflowInPortBarEditPart;
import org.knime.workbench.editor2.editparts.WorkflowOutPortBarEditPart;

/**
 * 
 * @author Fabian Dill, University of Konstanz
 */
public abstract class AbstractWorkflowPortBarFigure extends RectangleFigure {
    
    /** Default width for the port bar. */
    protected static final int WIDTH = 30;
    /** Default offset from the workflow borders. */
    protected static final int OFFSET = 10;
    
    private boolean m_isInitialized = false;

    
    /**
     * 
     */
    public AbstractWorkflowPortBarFigure() {
        super();
        DelegatingLayout layout = new DelegatingLayout();
        setLayoutManager(layout);
        setBackgroundColor(Display.getCurrent().getSystemColor(
                SWT.COLOR_GRAY));
    }
    
    /**
     * 
     * @param initialized true if the ui info was set to the model
     * 
     * @see WorkflowOutPortBarFigure#paint(org.eclipse.draw2d.Graphics)
     * @see WorkflowInPortBarFigure#paint(org.eclipse.draw2d.Graphics)
     * @see WorkflowOutPortBarEditPart
     * @see WorkflowInPortBarEditPart
     */
    public void setInitialized(final boolean initialized) {
        m_isInitialized = initialized;
        if (initialized) {
            revalidate();
        }
    }
    
    /**
     * 
     * @return true if the ui info was set to the model (first time painted 
     *  or loaded from {@link WorkflowManager}) 
     */
    public boolean isInitialized() {
        return m_isInitialized;
    }
    
    /**
     * 
     * @see ChangeWorkflowPortBarCommand#canExecute()
     * 
     * {@inheritDoc}
     */
    @Override
    public Dimension getMinimumSize(final int hint, final int hint2) {
        return new Dimension(AbstractPortFigure.WF_PORT_SIZE + 10, 
                AbstractPortFigure.WF_PORT_SIZE + 10);
    }

}
