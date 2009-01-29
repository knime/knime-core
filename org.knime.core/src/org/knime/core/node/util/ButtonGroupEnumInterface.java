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
 *    10.01.2007 (Tobias Koetter): created
 */

package org.knime.core.node.util;


/**
 * Used to create a {@link javax.swing.ButtonGroup} in the 
 * {@link org.knime.core.node.defaultnodesettings.DialogComponentButtonGroup} 
 * class. 
 * <p>Should be implemented by an {@link java.lang.Enum} for simplicity.</p>
 * @author Tobias Koetter, University of Konstanz
 */
public interface ButtonGroupEnumInterface {

    /**
     * @return the text to display
     */
    public String getText();
    
    /**
     * Should return the ButtonGroup wide unique action command. This is 
     * typically the return value of the {@link java.lang.Enum#name()} method.
     * @return the unique action command of this option
     */
    public String getActionCommand();
    
    /**
     * @return the tool tip of this option could be <code>null</code>
     */
    public String getToolTip();
 
    /**
     * @return <code>true</code> if this is the default option
     */
    public boolean isDefault();
}
