/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2007
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
 *   03.08.2007 (ohl): created
 */
package org.knime.testing.core;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * 
 * @author ohl, University of Konstanz
 */
class TestingDialog extends JDialog {

    private boolean m_closedViaOK = false;

    private String m_testNamePattern = null;

    private boolean m_analyze = false;

    private String m_analOutdir = null;

    private final JTextField m_pattern = new JTextField(20);

    private final JCheckBox m_anal =
            new JCheckBox("Analyze log file after run");

    private final JTextField m_analout = new JTextField(50);

    /**
     * Creates a modal dialog asking for user parameters. Set it visible to open
     * it. It returns after OK or Cancel was clicked.
     * 
     * @param parent the parent frame this should be modal to.
     * @param pattern default value for test name pattern
     * @param analyze default value for analyze checkbox
     * @param analOutDir default value for output dir
     */
    TestingDialog(final Frame parent, final String pattern,
            final boolean analyze, final String analOutDir) {
        super(parent);
        m_closedViaOK = false;
        setModal(true);

        initialize();

        // set default values
        m_pattern.setText(pattern);
        m_anal.setSelected(analyze);
        m_analout.setText(analOutDir);

        analChanged(); // update enable status
    }

    /**
     * This method initializes this.
     */
    private void initialize() {
        this.setSize(520, 375);
        this.setTitle("KNIME Testing");
        this.setContentPane(getPanel());
    }

    private JPanel getPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        // the box where the test name pattern is being entered.
        panel.add(getPatternBox());
        // the box for the analyze log file choices
        panel.add(getAnalBox());
        // the box with the OK and Cancel button
        panel.add(getControlBox());

        return panel;
    }

    private Component getPatternBox() {

        Box patternBox = Box.createVerticalBox();
        patternBox.setBorder(BorderFactory.createTitledBorder(BorderFactory
                .createEtchedBorder(), "Test selection:"));

        Box labelBox = Box.createHorizontalBox();
        labelBox.add(new JLabel("Enter name (regular expression) of "
                + "testcase(s) to run:"));
        labelBox.add(Box.createHorizontalGlue());
        patternBox.add(labelBox);

        Box editBox = Box.createHorizontalBox();
        editBox.add(Box.createHorizontalStrut(50));
        m_pattern.setPreferredSize(new Dimension(200, 25));
        m_pattern.setMinimumSize(new Dimension(200, 25));
        m_pattern.setMaximumSize(new Dimension(200, 25));
        editBox.add(m_pattern);
        editBox.add(Box.createHorizontalGlue());
        patternBox.add(editBox);

        labelBox = Box.createHorizontalBox();
        labelBox.add(Box.createHorizontalStrut(55));
        labelBox.add(new JLabel("(Empty input runs all.)"));
        labelBox.add(Box.createHorizontalGlue());
        patternBox.add(labelBox);

        return patternBox;
    }

    private Component getAnalBox() {

        Box analBox = Box.createVerticalBox();
        analBox.setBorder(BorderFactory.createTitledBorder(BorderFactory
                .createEtchedBorder(), "Log file analysis:"));

        Box checkBox = Box.createHorizontalBox();
        m_anal.addChangeListener(new ChangeListener() {
            public void stateChanged(final ChangeEvent e) {
                analChanged();
            }
        });
        analChanged(); // update en/disabled status now
        checkBox.add(m_anal);
        checkBox.add(Box.createHorizontalGlue());
        analBox.add(checkBox);

        Box labelBox = Box.createHorizontalBox();
        labelBox.add(Box.createHorizontalGlue());
        labelBox.add(new JLabel("Enter directory for analysis results:"));
        analBox.add(labelBox);

        Box editBox = Box.createHorizontalBox();
        editBox.add(Box.createHorizontalGlue());
        m_analout.setPreferredSize(new Dimension(300, 25));
        m_analout.setMinimumSize(new Dimension(300, 25));
        m_analout.setMaximumSize(new Dimension(300, 25));
        editBox.add(m_analout);
        analBox.add(editBox);

        labelBox = Box.createHorizontalBox();
        labelBox.add(Box.createHorizontalGlue());
        labelBox.add(new JLabel("(leave emtpy to use Java temp dir)"));
        analBox.add(labelBox);

        return analBox;
    }

    private Component getControlBox() {

        Dimension bSize = new Dimension(100, 25);

        Box controlBox = Box.createVerticalBox();

        controlBox.add(Box.createVerticalStrut(30));

        Box buttonBox = Box.createHorizontalBox();
        JButton ok = new JButton("OK");
        ok.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                m_testNamePattern = m_pattern.getText();
                m_analyze = m_anal.isSelected();
                m_analOutdir = m_analout.getText();

                m_closedViaOK = true;
                setVisible(false);
            }
        });
        ok.setPreferredSize(bSize);
        ok.setMinimumSize(bSize);
        ok.setMaximumSize(bSize);
        JButton cancel = new JButton("Cancel");
        cancel.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                m_closedViaOK = false;
                setVisible(false);
            }
        });
        cancel.setPreferredSize(bSize);
        cancel.setMinimumSize(bSize);
        cancel.setMaximumSize(bSize);
        buttonBox.add(Box.createHorizontalGlue());
        buttonBox.add(ok);
        buttonBox.add(Box.createVerticalStrut(20));
        buttonBox.add(cancel);
        buttonBox.add(Box.createVerticalStrut(40));

        controlBox.add(buttonBox);

        return controlBox;

    }

    /**
     * updates the enabled/disabled status of the components depending on the
     * check mark of the "analyze log file" box.
     */
    private void analChanged() {
        m_analout.setEnabled(m_anal.isSelected());
    }

    /**
     * After the dialog returned from setVisible(true) this methods tells,
     * whether the dialog was canceled or ok'ed.
     * 
     * @return true if the user pressed OK to close the dialog
     */
    boolean closedViaOK() {
        return m_closedViaOK;
    }

    /**
     * After the dialog returned from setVisible(true) this methods returns the
     * value entered as test name regular expression matching pattern.
     * 
     * @return the new regular expression to match test names against.
     */
    String getTestNamePattern() {
        return m_testNamePattern;
    }

    /**
     * After the dialog returned from setVisible(true) this methods returns the
     * user's choice whether to analyze the log file or not.
     * 
     * @return true if the log file should be analyzed.
     */
    boolean getAnalyzeLogFile() {
        return m_analyze;
    }

    /**
     * After the dialog returned from setVisible(true) this methods returns the
     * value entered as output dir for the log file analysis result dir.
     * 
     * @return the new dir to store the analysis results in
     */
    String getAnalysisOutputDir() {
        return m_analOutdir;
    }

    /**
     * The main.
     * 
     * @param args the args of main.
     */
    public static void main(final String[] args) {
        TestingDialog dlg = new TestingDialog(null, "foo", true, null);
        dlg.setVisible(true);
        dlg.dispose();
        if (!dlg.closedViaOK()) {
            System.out.println("Canceled.");
            return;
        }
        System.out.println("TestPattern: '" + dlg.getTestNamePattern() + "'");
        System.out.println("Analyze Log File?: "
                + (dlg.getAnalyzeLogFile() ? "yes, please." : "no thanks."));
        System.out.println("Anal Outdir: '" + dlg.getAnalysisOutputDir() + "'");
    }

}
