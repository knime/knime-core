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
 *   03.10.2006 (Fabian Dill): created
 */
package org.knime.base.node.viz.plotter.props;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.EtchedBorder;

import org.knime.base.node.viz.plotter.line.LinePlotter;

/**
 * Provides three checkboxes, one to show or hide the dots, one for the line
 * thickness and one for the dot size.
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class LinePlotterAppearanceTab extends PropertiesTab {
    
    private final JSpinner m_thickness;
    
    private final JSpinner m_dotSize;
    
    private final JCheckBox m_showDots;
    
    /**
     * Creates the tab with a show/hide dots checkbox, a dot size spinner
     * and a line thickness spinner.
     *
     */
    public LinePlotterAppearanceTab() {
        m_showDots = new JCheckBox("Show / Hide dots", true);
        m_thickness = new JSpinner(new SpinnerNumberModel(1, 1, 1000, 1));
        m_dotSize = new JSpinner(new SpinnerNumberModel(LinePlotter.SIZE,
                1, 1000, 1));
        Box appBox = Box.createHorizontalBox();
        appBox.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));
        appBox.add(Box.createHorizontalStrut(SMALL_SPACE));
        appBox.add(m_showDots);
        appBox.add(Box.createHorizontalStrut(SPACE));
        appBox.add(new JLabel("Thickness:"));
        appBox.add(Box.createHorizontalStrut(SMALL_SPACE));
        appBox.add(m_thickness);
        appBox.add(Box.createHorizontalStrut(SPACE));
        appBox.add(new JLabel("Dot Size:"));
        appBox.add(Box.createHorizontalStrut(SMALL_SPACE));
        appBox.add(m_dotSize);
        appBox.add(Box.createHorizontalStrut(SPACE));
        add(appBox);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public String getDefaultName() {
        return "Appearance";
    }
    
    
    /**
     * 
     * @return the check box for the show/ hide dots option.
     */
    public JCheckBox getShowDotsBox() {
        return m_showDots;
    }
    

    /**
     * 
     * @return the spinner for the line thickness.
     */
    public JSpinner getThicknessSpinner() {
        return m_thickness;
    }
    
    /**
     * 
     * @return the spinner for the dot size.
     */
    public JSpinner getDotSizeSpinner() {
        return m_dotSize;
    }

}
