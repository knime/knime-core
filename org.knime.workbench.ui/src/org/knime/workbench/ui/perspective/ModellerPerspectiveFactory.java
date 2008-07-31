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
 *   12.01.2005 (Florian Georg): created
 */
package org.knime.workbench.ui.perspective;

import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

/**
 * Factory for creating the Modeller Perspective.
 * 
 * @author Florian Georg, University of Konstanz
 */
public class ModellerPerspectiveFactory implements IPerspectiveFactory {
    /**
     * {@inheritDoc}
     */
    public void createInitialLayout(final IPageLayout layout) {
        // layout.addView(IPageLayout.ID_RES_NAV,IPageLayout.LEFT,0.3f,
        // IPageLayout.ID_EDITOR_AREA);
    }
}
