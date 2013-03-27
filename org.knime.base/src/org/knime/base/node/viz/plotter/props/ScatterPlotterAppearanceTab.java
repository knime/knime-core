/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
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
 *   03.10.2006 (Fabian Dill): created
 */
package org.knime.base.node.viz.plotter.props;

import java.awt.Dimension;
import java.text.ParseException;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ChangeListener;

import org.knime.base.node.viz.plotter.scatter.ScatterPlotterProperties;
import org.knime.core.node.NodeLogger;

/**
 * Control elements to adjust the dot size and the jitter rate.
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class ScatterPlotterAppearanceTab extends PropertiesTab {
    
    private static final NodeLogger LOGGER = NodeLogger.getLogger(
            ScatterPlotterAppearanceTab.class);
    
    
    private final JSpinner m_dotSizeSpinner;
    
    private final JSlider m_jitterSlider;
    
    private static final int MIN_DOT_SIZE = 1;
    private static final int MAX_DOT_SIZE = 150;
    
    
    /**
     * Dot size, shape and jitter.
     *
     */
    public ScatterPlotterAppearanceTab() {
        m_dotSizeSpinner = new JSpinner(new SpinnerNumberModel(
                ScatterPlotterProperties.DEFAULT_DOT_SIZE,
                MIN_DOT_SIZE, MAX_DOT_SIZE, 1));
        m_jitterSlider = new JSlider();
        
        m_jitterSlider.setPreferredSize(new Dimension(
                COMPONENT_WIDTH, 
                m_jitterSlider.getPreferredSize().height)); 
        Box all = Box.createHorizontalBox();
        all.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));
        all.add(Box.createHorizontalStrut(SMALL_SPACE));
        all.add(new JLabel("Dot Size"));
        all.add(Box.createHorizontalStrut(SMALL_SPACE));
        all.add(m_dotSizeSpinner);
        all.add(Box.createHorizontalStrut(SPACE));
        all.add(new JLabel("Jitter"));
        all.add(Box.createHorizontalStrut(SMALL_SPACE));
        all.add(m_jitterSlider);
        all.add(Box.createHorizontalGlue());
        add(all);
    }
    
    /**
     * 
     * @return the slider to adjust the jitter rate.
     */
    public JSlider getJitterSlider() {
        return m_jitterSlider;
    }
    
    /**
     * 
     * @param listener change listener for the dot size.
     */
    public void addDotSizeChangeListener(final ChangeListener listener) {
        m_dotSizeSpinner.addChangeListener(listener);
    }
    
    /**
     * 
     * @param dotSize sets the dotsize value.
     */
    public void setDotSize(final int dotSize) {
        m_dotSizeSpinner.setValue(dotSize);
    }
    
    
    
    /**
     * Read the current value from the spinner assuming it contains Integers.
     * @return int the current value of the dot size spinner.
     */
    public int getDotSize() {
        try {
            m_dotSizeSpinner.commitEdit();
        } catch (ParseException e) {
            // if the spinner has the focus, the currently edited value
            // might not be commited. Now it is!
            LOGGER.warn(e, e);
        }
        SpinnerNumberModel snm = (SpinnerNumberModel)m_dotSizeSpinner
            .getModel();
        return snm.getNumber().intValue();
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public String getDefaultName() {
        return "Appearance";
    }

}
