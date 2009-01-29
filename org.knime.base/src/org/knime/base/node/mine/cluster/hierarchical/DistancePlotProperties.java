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
 * ---------------------------------------------------------------------
 * 
 * History
 *   19.01.2007 (dill): created
 */
package org.knime.base.node.mine.cluster.hierarchical;

import javax.swing.JCheckBox;
import javax.swing.JSpinner;

import org.knime.base.node.viz.plotter.AbstractPlotterProperties;
import org.knime.base.node.viz.plotter.props.LinePlotterAppearanceTab;

/**
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class DistancePlotProperties extends AbstractPlotterProperties {
    
    private LinePlotterAppearanceTab m_appearanceTab;
    
    /**
     * Creates properties with the default tab (zooming, moving) and 
     * an appearance tab to show hide dots, line thickness and dot size.
     *
     */
    public DistancePlotProperties() {
        super();
        m_appearanceTab = new LinePlotterAppearanceTab();
        addTab("Appearance", m_appearanceTab);
    }

    
    /**
     * 
     * @return the spinner to adjust the dot size
     */
    public JSpinner getDotSizeSpinner() {
        return m_appearanceTab.getDotSizeSpinner();
    }
    
    /**
     * 
     * @return the spinner to adjust the line thickness
     */
    public JSpinner getThicknessSpinner() {
        return m_appearanceTab.getThicknessSpinner();
    }
    
    /**
     * 
     * @return the checkbox whether to show or hide the dots      
     */
    public JCheckBox getShowHideCheckbox() {
        return m_appearanceTab.getShowDotsBox();
    }
}
