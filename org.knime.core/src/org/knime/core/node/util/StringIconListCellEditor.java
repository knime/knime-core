/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2008 - 2009
 * KNIME.com, Zurich, Switzerland
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.com
 * email: contact@knime.com
 * ---------------------------------------------------------------------
 * 
 * History
 *   04.08.2009 (Fabian Dill): created
 */
package org.knime.core.node.util;

import javax.swing.JTextField;
import javax.swing.plaf.basic.BasicComboBoxEditor;

/**
 * 
 * @author Fabian Dill, KNIME.com, Zurich, Switzerland
 */
public class StringIconListCellEditor extends BasicComboBoxEditor {
    
    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public Object getItem() {
        // basic combo box editor has a JTextField as editor component
        String newValue = ((JTextField)getEditorComponent()).getText();
        return new DefaultStringIconOption(newValue);
    }
    
}
