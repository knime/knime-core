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
 *   10.06.2008 (Fabian Dill): created
 */
package org.knime.workbench.editor2.editparts;

import org.eclipse.draw2d.IFigure;
import org.knime.workbench.editor2.figures.ProgressFigure;
import org.knime.workbench.editor2.figures.SubworkflowFigure;



public class SubworkflowEditPart extends NodeContainerEditPart {

    @Override
    protected IFigure createFigure() {
        // create the visuals for the node container.
        final SubworkflowFigure nodeFigure =
                new SubworkflowFigure(new ProgressFigure());

        // init the user specified node name
        nodeFigure.setCustomName(getCustomName());

        return nodeFigure;
    }
    
    
}
