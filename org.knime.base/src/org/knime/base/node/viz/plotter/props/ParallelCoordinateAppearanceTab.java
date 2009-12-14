/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2009
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


/**
 * Adds a box with "Show / Hide Dots" check box and a checkbox to draw curves
 * and a checkbox for the line thickness.
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class ParallelCoordinateAppearanceTab extends PropertiesTab {
    
    private JCheckBox m_showDotsBox;
    private JCheckBox m_drawCurves;
    private JSpinner m_lineThickness;
    
    /**
     * Adds a box with "Show / Hide Dots" check box and a checkbox
     * to draw curves and a checkbox for the line thickness.
     *
     */
    public ParallelCoordinateAppearanceTab() {
        m_showDotsBox = new JCheckBox("Show / Hide Dots", true);
        m_drawCurves = new JCheckBox("Draw Curves instead of lines", false);
        m_lineThickness = new JSpinner(
                new SpinnerNumberModel(2, 1, 999, 1));
        Box appBox = Box.createHorizontalBox();
        appBox.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));
        appBox.add(Box.createHorizontalStrut(SMALL_SPACE));
        appBox.add(m_showDotsBox);
        appBox.add(Box.createHorizontalStrut(SPACE));
        appBox.add(m_drawCurves);
        appBox.add(Box.createHorizontalStrut(SPACE));
        appBox.add(new JLabel("Line thickness:"));
        appBox.add(Box.createHorizontalStrut(SMALL_SPACE));
        appBox.add(m_lineThickness);
        appBox.add(Box.createHorizontalStrut(SMALL_SPACE));
        add(appBox);
    }
    
    /**
     * 
     * @return the checkbox whether the dots should be painted.
     */
    public JCheckBox getShowDotsBox() {
        return m_showDotsBox;
    }
    
    /**
     * 
     * @return the chekbox whether to draw the lines as curves.
     */
    public JCheckBox getDrawCurvesBox() {
        return m_drawCurves;
    }
    
    /**
     * 
     * @return the spinner for the line thickness adjustment.
     */
    public JSpinner getThicknessSpinner() {
        return m_lineThickness;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public String getDefaultName() {
        return "Appearance";
    }

}
