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
 *   12.01.2005 (Florian Georg): created
 */
package de.unikn.knime.workbench.ui.perspective;

import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

/**
 * Factory for creating the Modeller Perspective.
 * 
 * @author Florian Georg, University of Konstanz
 */
public class ModellerPerspectiveFactory implements IPerspectiveFactory {
    /**
     * @see org.eclipse.ui.IPerspectiveFactory
     *      #createInitialLayout(org.eclipse.ui.IPageLayout)
     */
    public void createInitialLayout(final IPageLayout layout) {
        // layout.addView(IPageLayout.ID_RES_NAV,IPageLayout.LEFT,0.3f,
        // IPageLayout.ID_EDITOR_AREA);
    }
}
