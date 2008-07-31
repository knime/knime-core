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
 * -------------------------------------------------------------------
 * 
 * History
 *   03.10.2006 (Fabian Dill): created
 */
package org.knime.base.node.viz.plotter.props;

import javax.swing.JPanel;

/**
 * A convenient <code>JPanel</code> which defines some distances and a default 
 * name for this and can be added as a tab to the 
 * {@link org.knime.base.node.viz.plotter.AbstractPlotterProperties}. 
 * This can then be done with this simple code:
 * <pre>
 * public SomePlotterProperties() {
        m_someTab = new SomeTab();
        addTab(m_someTab.getDefaultName(), m_someTab);
    }
   </pre>
 * The idea behind the organization of the properties into very small functional
 * units is that it is reusable. If, for instance, a tab is defined to show or 
 * hide dots this may be useful for several plotters, whereas the properties as 
 * a collection of all this tabs may be to special to extend and reuse it as a
 * whole.
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
