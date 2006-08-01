/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 * 
 * Copyright, 2003 - 2006
 * Universitaet Konstanz, Germany.
 * Lehrstuhl fuer Angewandte Informatik
 * Prof. Dr. Michael R. Berthold
 * 
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner.
 * -------------------------------------------------------------------
 * 
 * History
 *   29.05.2005 (Florian Georg): created
 */
package org.knime.workbench.editor2.figures;

import org.eclipse.draw2d.FreeformLayeredPane;

/**
 * The root figure, containing all diagram elements inside the workflow.
 * 
 * TODO a grid in the background (?GridLayer - where?)
 * 
 * @author Florian Georg, University of Konstanz
 */
public class WorkflowFigure extends FreeformLayeredPane {
    /**
     * New workflow root figure.
     */
    public WorkflowFigure() {
        // not opaque, so that we can directly select on the "background" layer
        this.setOpaque(false);
    }
}
