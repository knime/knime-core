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
 *   Apr 26, 2007 (ohl): created
 */
package org.knime.base.node.io.filereader;

import java.awt.Container;
import java.awt.Dimension;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.knime.core.node.NodeLogger;

/**
 *
 * @author ohl, University of Konstanz
 */
class LimitRowsPanel extends JPanel {

    private JCheckBox m_limitRows;

    private JTextField m_maxNumber;

    private String m_lastValidValue;

    /**
     * Constructs the panels and loads it with the settings from the passed
     * object.
     *
     * @param settings containing the settings to show in the panel
     */
    LimitRowsPanel(final FileReaderNodeSettings settings) {
        initialize();
        loadSettings(settings);
    }

    private void initialize() {
        this.setSize(520, 375);
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        add(Box.createVerticalStrut(20));
        add(Box.createVerticalGlue());
        add(getTextBox());
        add(Box.createVerticalStrut(10));
        add(getPanel());
        add(Box.createVerticalGlue());
        add(Box.createVerticalStrut(20));
    }

    private Container getPanel() {

        m_limitRows = new JCheckBox("Read only the first");
        m_maxNumber = new JTextField();
        m_maxNumber.setColumns(6);
        m_maxNumber.setPreferredSize(new Dimension(125, 25));
        m_maxNumber.setMaximumSize(new Dimension(125, 25));

        m_limitRows.setSelected(false);
        m_maxNumber.setEnabled(false);

        // make sure we always have a valid value. Reject invalid characters.
        m_maxNumber.getDocument().addDocumentListener(new DocumentListener() {

            public void removeUpdate(final DocumentEvent e) {
                checkAndFixTextfield();
            }

            public void insertUpdate(final DocumentEvent e) {
                checkAndFixTextfield();
            }

            public void changedUpdate(final DocumentEvent e) {
                checkAndFixTextfield();
            }

        });

        m_limitRows.addChangeListener(new ChangeListener() {
            public void stateChanged(final ChangeEvent e) {
                // en/disable the textfield
                m_maxNumber.setEnabled(m_limitRows.isSelected());
                // also - if the textfield is enabled and empty set
                // a
                // value
                if (m_maxNumber.isEnabled()) {
                    if ((m_maxNumber.getText() == null)
                            || (m_maxNumber.getText().trim().length() == 0)) {
                        // set a valid value, so we can safely
                        // assume
                        // that if
                        // there is a value in there it's always
                        // valid.
                        m_maxNumber.setText("1000");
                        m_lastValidValue = "1000";
                    }
                }

            }
        });

        Box result = Box.createHorizontalBox();
        result.add(Box.createHorizontalGlue());
        result.add(m_limitRows);
        result.add(Box.createHorizontalStrut(3));
        result.add(m_maxNumber);
        result.add(Box.createHorizontalStrut(3));
        result.add(new JLabel("lines from the file."));
        result.add(Box.createHorizontalGlue());
        return result;
    }

    /*
     * ensures that the textfield contains a valid integer number. If it doesn't
     * it changes it content to the last valid number. Also updates the last
     * valid value variable.
     */
    private void checkAndFixTextfield() {
        // this is called from inside the change listener - invoke is later
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {

                if (m_maxNumber.getText().trim().length() == 0) {
                    // can't handle no empty strings.
                    m_lastValidValue = "0";
                    m_maxNumber.setText(m_lastValidValue);
                } else {
                    // make sure we only get numbers
                    String s = m_maxNumber.getText();
                    boolean onlyNumbers = true;
                    for (int i = s.length() - 1; i >= 0; i--) {
                        char c = s.charAt(i);
                        if ((c < '0') || (c > '9')) {
                            onlyNumbers = false;
                            break;
                        }
                    }
                    if (!onlyNumbers) {
                        m_maxNumber.setText(m_lastValidValue);
                    } else {
                        m_lastValidValue = m_maxNumber.getText();
                    }
                }
            }
        });
    }

    private Container getTextBox() {
        Box result = Box.createVerticalBox();
        result.add(Box.createVerticalGlue());
        result.add(new JLabel("Check this to read only a limited number of"));
        result.add(new JLabel("rows from the file. Enter the maximum number"));
        result.add(new JLabel("to read."));
        result.add(Box.createVerticalStrut(5));
        result
                .add(new JLabel(
                        "If unchecked, the entire file will be read in."));

        result.add(Box.createVerticalGlue());
        return result;
    }

    /**
     * Checks the current values in the panel.
     *
     * @return null, if settings are okay and can be applied. An error message
     *         if not.
     */
    String checkSettings() {
        // we've made huge efforts to ensure a valid panel all the time.
        return null;
    }

    /**
     * Transfers the current settings from the panel in the passed object.
     * Overwriting the corresponding values in the object.
     *
     * @param settings the settings object to fill in the currently set values
     * @return always false.
     */
    boolean overrideSettings(final FileReaderNodeSettings settings) {
        if (m_limitRows.isSelected()) {
            // the text in the maxNumber should really be a number
            try {
                settings.setMaximumNumberOfRowsToRead(Long
                        .parseLong(m_maxNumber.getText().trim()));
            } catch (NumberFormatException nfe) {
                // that's kinda bad...
                NodeLogger.getLogger(LimitRowsPanel.class).error(
                        "Entered invalid number for "
                                + "maximum number of rows to "
                                + "read. Reading ALL rows!");
                settings.setMaximumNumberOfRowsToRead(-1);
            }
        } else {
            settings.setMaximumNumberOfRowsToRead(-1);
        }
        // no need to re-analyze the file if this number changes.
        return false;
    }

    /**
     * Transfers the corresponding values from the passed object into the panel.
     *
     * @param settings object holding the values to display in the panel
     */
    private void loadSettings(final FileReaderNodeSettings settings) {
        final long maxNumOfRows = settings.getMaximumNumberOfRowsToRead();

        if (maxNumOfRows < 0) {
            m_limitRows.setSelected(false);
        } else {
            m_limitRows.setSelected(true);
            m_maxNumber.setText(Long.toString(maxNumOfRows));
            m_lastValidValue = m_maxNumber.getText();
        }
    }
}
