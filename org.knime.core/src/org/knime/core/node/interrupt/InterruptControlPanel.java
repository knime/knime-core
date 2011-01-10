/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
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
 * ----------------------------------------------------------------------------
 */
package org.knime.core.node.interrupt;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingConstants;

/**
 * A simple control panel holding the control elements necessary to control the
 * InterruptibleNodeModel, that is a "Run"-, "Break"- and "Finish"-Button and a
 * slider to adjust the delay. Additionally, the current iteration is displayed.
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class InterruptControlPanel extends JPanel {

    private static final int MINOR_TICKS = 5;

    private static final int MAJOR_TICKS = 20;

    /**
     * The identifier for the event to break and pause the execution of the
     * InterruptibleNodeModel.
     */
    public static final String BREAK = "Break";

    /**
     * The identifier for the event to start or resume the execution of the
     * InterruptibleNodeModel.
     */
    public static final String RUN = "Run";

    /**
     * The identifier for the event to execute exactly one iteration.
     */
    public static final String NEXT = "Next Step";
    /**
     * The identifier for the event to finish the execution of the
     * InterruptibleNodeModel.
     */
    public static final String FINISH = "Finish";

    private static final String DISPLAY = "This is iteration nr.: ";
    
    private static final int SLIDER_MAX = 100;

    // the buttons
    private JButton m_runBtn;

    private JButton m_breakBtn;

    private JButton m_finishBtn;
    
    private JButton m_nextStepBtn;

    // thr slider
    private JSlider m_delaySlider;

    //lbel to display the current iteration
    private JLabel m_iterationLabel;

    /**
     * Constructs the InterruptControlPanel with the buttons: "Run", "Break" and
     * "Finish" and the slider to adjust the delay. Below the control elements
     * the current iteration is displayed.
     */
    public InterruptControlPanel() {
        super();
        // set the control elements at the top
        JPanel upperPanel = new JPanel();
        upperPanel.setLayout(new FlowLayout());

        m_runBtn = new JButton(RUN);
        m_breakBtn = new JButton(BREAK);
        m_nextStepBtn = new JButton(NEXT);
        m_finishBtn = new JButton(FINISH);

        m_delaySlider = new JSlider(SwingConstants.HORIZONTAL, 0, SLIDER_MAX,
                InterruptibleNodeModel.INITIAL_DELAY);
        m_delaySlider.setMajorTickSpacing(MAJOR_TICKS);
        m_delaySlider.setMinorTickSpacing(MINOR_TICKS);
        m_delaySlider.setPaintTicks(true);
        m_delaySlider.setPaintLabels(true);

        upperPanel.add(m_runBtn);
        upperPanel.add(m_breakBtn);
        upperPanel.add(m_nextStepBtn);
        upperPanel.add(m_finishBtn);
        upperPanel.add(new JLabel("Delay:"));
        upperPanel.add(m_delaySlider);

        //iterations = new IterationDisplay();
        m_iterationLabel = new JLabel(DISPLAY + 0);
        m_iterationLabel.setForeground(Color.red);

        setLayout(new BorderLayout());
        add(upperPanel, BorderLayout.CENTER);
        add(m_iterationLabel, BorderLayout.SOUTH);
        
        setVisible(true);
    }

    /**
     * Returns the button for the BREAK event.
     * 
     * @return - the button for the BREAK event.
     */
    public JButton getBreakButton() {
        return m_breakBtn;
    }

    /**
     * Returns the slider to adjust the delay.
     * 
     * @return - the slider to adjust the delay.
     */
    public JSlider getDelaySlider() {
        return m_delaySlider;
    }

    /**
     * Returns the button for the RUN event.
     * 
     * @return - the button for the RUN event.
     */
    public JButton getRunButton() {
        return m_runBtn;
    }
    
    /**
     * Returns the button for the NEXT STEP event.
     * 
     * @return - the button for the NEXT STEP event.
     */
    public JButton getNextStepButton() {
        return m_nextStepBtn;
    }

    /**
     * Returns the button for the FINISH event.
     * 
     * @return - the button for the FINISH event.
     */
    public JButton getFinishButton() {
        return m_finishBtn;
    }

    /**
     * Updates the iteration display with the current iteration number.
     * @param iterationNr - the current iteration number.
     */
    public void setCurrentIteration(final int iterationNr) {
        m_iterationLabel.setText(DISPLAY + iterationNr);
    }

    /**
     * Returns the label responsible for displaying the current iteration.
     * @return - the label displaying the current iteration number.
     */
    public JLabel getIterationLabel() {
        return m_iterationLabel;
    }
}
