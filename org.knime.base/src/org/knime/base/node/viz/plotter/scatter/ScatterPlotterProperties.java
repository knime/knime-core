/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2010
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * -------------------------------------------------------------------
 * 
 * History
 *   13.09.2006 (Fabian Dill): created
 */
package org.knime.base.node.viz.plotter.scatter;

import javax.swing.JSlider;
import javax.swing.event.ChangeListener;

import org.knime.base.node.viz.plotter.columns.TwoColumnProperties;
import org.knime.base.node.viz.plotter.props.ScatterPlotterAppearanceTab;
import org.knime.core.data.DataValue;

/**
 * In addition to the 
 * {@link org.knime.base.node.viz.plotter.columns.TwoColumnProperties} a 
 * tab to adjust the dot size and the jitter rate is provided.
 * 
 * @see org.knime.base.node.viz.plotter.columns.TwoColumnProperties
 * @see org.knime.base.node.viz.plotter.props.ScatterPlotterAppearanceTab
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class ScatterPlotterProperties extends TwoColumnProperties {

    /** the default initial dot size. */
    public static final int DEFAULT_DOT_SIZE = 5;
    
    
    private final ScatterPlotterAppearanceTab m_appearance;
    
    /**
     * Default tab, two column selection and ranges and dot size and jitter
     * rate adjustment.
     *
     */
    public ScatterPlotterProperties() {
        this(new Class[]{DataValue.class}, new Class[]{DataValue.class});
    }
    
    /**
     * A constructor to restrict the column selection boxes to certain 
     * {@link org.knime.core.data.DataType}s.
     * 
     * @param allowedXTypes
     * @param allowedYTypes
     */
    @SuppressWarnings("unchecked")
    public ScatterPlotterProperties(final Class[] allowedXTypes,
    		final Class[] allowedYTypes) {
    	super(allowedXTypes, allowedYTypes);
        m_appearance = new ScatterPlotterAppearanceTab();
        addTab(m_appearance.getDefaultName(), m_appearance);
    }
    
    /**
     * 
     * @return the slider to adjust the jitter rate.
     */
    public JSlider getJitterSlider() {
        return m_appearance.getJitterSlider();
    }
    
    /**
     * 
     * @param listener change listener for the dot size.
     */
    public void addDotSizeChangeListener(final ChangeListener listener) {
        m_appearance.addDotSizeChangeListener(listener);
    }
    
    
    /**
     * Read the current value from the spinner assuming it contains Integers.
     * @return int the current value of the dot size spinner.
     */
    protected int getDotSize() {
        return m_appearance.getDotSize();
    }
}
