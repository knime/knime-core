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
 *   13.10.2006 (Fabian Dill): created
 */
package org.knime.base.node.viz.plotter.dendrogram;

import javax.swing.JCheckBox;
import javax.swing.JSpinner;

import org.knime.base.node.viz.plotter.AbstractPlotterProperties;
import org.knime.base.node.viz.plotter.props.LinePlotterAppearanceTab;

/**
 * In addition to the 
 * {@link org.knime.base.node.viz.plotter.AbstractPlotterProperties} a 
 * {@link org.knime.base.node.viz.plotter.props.LinePlotterAppearanceTab} is
 * added, to adjust the dot size and line thickness and to show or hide the 
 * dots.
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class DendrogramPlotterProperties extends AbstractPlotterProperties {

    private LinePlotterAppearanceTab m_appearance;
    
    /**
     * Normal properties with an appearance tab to show/hide dots,
     * dot size and line thickness.
     *
     */
    public DendrogramPlotterProperties() {
        super();
        m_appearance = new LinePlotterAppearanceTab();
        addTab(m_appearance.getDefaultName(), m_appearance);
    }
    
    /**
     * 
     * @return the spinner for the dotsize.
     */
    public JSpinner getDotSizeSpinner() {
        return m_appearance.getDotSizeSpinner();
    }
    
    /**
     * 
     * @return the spinner for the line thickness
     */
    public JSpinner getThicknessSpinner() {
        return m_appearance.getThicknessSpinner();
    }
    
    /**
     * 
     * @return the checkbox to show hide dots.
     */
    public JCheckBox getShowDotsBox() {
        return m_appearance.getShowDotsBox();
    }
    
}
