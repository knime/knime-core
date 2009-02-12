/*
 * ------------------------------------------------------------------
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
 *   06.10.2006 (Fabian Dill): created
 */
package org.knime.core.data.property;

import javax.swing.JComboBox;

/**
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class ShapeSelectionComboBox extends JComboBox {
    
    /**
     * Creates Shape selection combo box with all shapes.
     * New shapes have to be registered here to be selectable.
     *
     */
    public ShapeSelectionComboBox() {
        super();
        for (ShapeFactory.Shape s : ShapeFactory.getShapes()) {
            addItem(s);
        }
        this.setBackground(this.getBackground());
        this.setRenderer(new ShapeSelectionListRenderer());
    }

}
