/*
 * ----------------------------------------------------------------------------
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
