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
 *   29.09.2006 (Fabian Dill): created
 */
package org.knime.base.node.viz.plotter.box;

import javax.swing.JCheckBox;

import org.knime.base.node.viz.plotter.columns.MultiColumnPlotterProperties;
import org.knime.base.node.viz.plotter.props.BoxPlotAppearanceTab;
import org.knime.core.data.DoubleValue;

/**
 * Tab to select whether to normalize the drawing or not.
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class BoxPlotterProperties extends MultiColumnPlotterProperties {
    
    private final BoxPlotAppearanceTab m_normalizeTab;
    
    /**
     * 
     * 
     */
    @SuppressWarnings("unchecked")
    public BoxPlotterProperties() {
        super(DoubleValue.class);
        m_normalizeTab = new BoxPlotAppearanceTab();
        addTab(m_normalizeTab.getDefaultName(), m_normalizeTab);
    }
    
    /**
     * 
     * @return the checkbox to force normalized presentation.
     */
    public JCheckBox getNormalizeCheckBox() {
        return m_normalizeTab.getNormalizeCheckBox();
    }
    

}
