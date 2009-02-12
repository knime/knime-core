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
 *   17.10.2005 (Fabian Dill): created
 */
package org.knime.core.node.interrupt;

import java.awt.GridBagConstraints;

/**
 * A helper class for the use of the GridBagLayout.
 * 
 * @author Fabian Dill, University of Konstanz
 */
public abstract class LayoutHelper {
    
    //to prohibit derivation and instantiation. 
    private LayoutHelper() {
    }

    /**
     * Creates an instance of GridBagConstraints with the following implicit
     * properties: weightx and weighty = 1. anchor = CENTER. fill = BOTH. The
     * rest is set by the passed parameters.
     * 
     * @param gridx - the x position in the grid.
     * @param gridy - the y position in the grid.
     * @param width - the number of gridBags in x direction.
     * @param height - the number of gridBags in y direction.
     * @return - GridBagConstraints with the set properties.
     */
    public static GridBagConstraints makegbc(
            final int gridx, final int gridy, 
            final int width, final int height) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = gridx;
        gbc.gridy = gridy;
        gbc.gridwidth = width;
        gbc.gridheight = height;
        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.BOTH;
        return gbc;
    }
}
