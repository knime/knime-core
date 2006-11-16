/* -------------------------------------------------------------------
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
 *   03.10.2006 (Fabian Dill): created
 */
package org.knime.exp.node.view.plotter.props;

import javax.swing.JPanel;

/**
 * 
 * @author Fabian Dill, University of Konstanz
 */
public abstract class PropertiesTab extends JPanel {
    
    /** Layout constant (space between elements). */
    public static final int SPACE = 15;
    
    /** Layout constant (space to the border). */
    public static final int SMALL_SPACE = 5;
    
    /** Layout constant (combobox width, etc). */
    public static final int COMPONENT_WIDTH = 150;
    
    /**
     * 
     * @return the default tab title
     */
    public abstract String getDefaultName();

}
