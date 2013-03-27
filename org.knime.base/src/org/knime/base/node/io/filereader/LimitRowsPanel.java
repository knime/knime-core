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
 * ---------------------------------------------------------------------
 *
 * History
 *   Apr 26, 2007 (ohl): created
 */
package org.knime.base.node.io.filereader;

import java.awt.Container;
import java.awt.Dimension;

import javax.swing.BorderFactory;
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

    private JCheckBox m_skipLines;

    private JTextField m_skipNumber;

    private String m_lastValidSkips;

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

        JPanel skipLinesPanel = new JPanel();
        skipLinesPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(),
                "Skip lines at the beginning of the file:"));
        skipLinesPanel
                .setLayout(new BoxLayout(skipLinesPanel, BoxLayout.Y_AXIS));
        skipLinesPanel.add(getSkipPanel());
        add(skipLinesPanel);

        JPanel limitRowsPanel = new JPanel();
        limitRowsPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(),
                "Limit the number of rows read:"));
        limitRowsPanel.setLayout(
                new BoxLayout(limitRowsPanel, BoxLayout.Y_AXIS));
        limitRowsPanel.add(getLimitPanel());
        limitRowsPanel.add(getLimitTextBox());
        limitRowsPanel.add(Box.createVerticalGlue());
        add(limitRowsPanel);

    }

    private Container getSkipPanel() {

        m_skipLines = new JCheckBox("Skip the first");
        m_skipNumber = new JTextField();
        m_skipNumber.setColumns(6);
        m_skipNumber.setPreferredSize(new Dimension(125, 25));
        m_skipNumber.setMaximumSize(new Dimension(125, 25));

        m_skipLines.setSelected(false);
        m_skipNumber.setEnabled(false);

        // make sure we always have a valid value. Reject invalid characters.
        m_skipNumber.getDocument().addDocumentListener(new DocumentListener() {

            public void removeUpdate(final DocumentEvent e) {
                checkAndFixSkipCount();
            }

            public void insertUpdate(final DocumentEvent e) {
                checkAndFixSkipCount();
            }

            public void changedUpdate(final DocumentEvent e) {
                checkAndFixSkipCount();
            }

        });

        m_skipLines.addChangeListener(new ChangeListener() {
            public void stateChanged(final ChangeEvent e) {
                // en/disable the textfield
                m_skipNumber.setEnabled(m_skipLines.isSelected());
                // also - if the textfield is enabled and empty set a value
                if (m_skipNumber.isEnabled()) {
                    if ((m_skipNumber.getText() == null)
                            || (m_skipNumber.getText().trim().length() == 0)) {
                        // set a valid value, so we can safely
                        // assume that if there is a value in there
                        // it's always valid.
                        m_skipNumber.setText("0");
                        m_lastValidSkips = "0";
                    }
                }

            }
        });

        Box result = Box.createHorizontalBox();
//        result.add(Box.createHorizontalGlue());
        result.add(m_skipLines);
        result.add(Box.createHorizontalStrut(3));
        result.add(m_skipNumber);
        result.add(Box.createHorizontalStrut(3));
        result.add(new JLabel("lines from the beginning of the file."));
        result.add(Box.createHorizontalGlue());
        return result;
    }

    private Container getLimitPanel() {

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
//        result.add(Box.createHorizontalGlue());
        result.add(m_limitRows);
        result.add(Box.createHorizontalStrut(3));
        result.add(m_maxNumber);
        result.add(Box.createHorizontalStrut(3));
        result.add(new JLabel("table rows from the file."));
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

    /*
     * ensures that the textfield contains a valid integer number. If it doesn't
     * it changes it content to the last valid number. Also updates the last
     * valid value variable.
     */
    private void checkAndFixSkipCount() {
        // this is called from inside the change listener - invoke is later
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {

                if (m_skipNumber.getText().trim().length() == 0) {
                    // can't handle no empty strings.
                    m_lastValidSkips = "0";
                    m_skipNumber.setText(m_lastValidSkips);
                } else {
                    // make sure we only get numbers
                    String s = m_skipNumber.getText();
                    boolean onlyNumbers = true;
                    for (int i = s.length() - 1; i >= 0; i--) {
                        char c = s.charAt(i);
                        if ((c < '0') || (c > '9')) {
                            onlyNumbers = false;
                            break;
                        }
                    }
                    if (!onlyNumbers) {
                        m_skipNumber.setText(m_lastValidSkips);
                    } else {
                        m_lastValidSkips = m_skipNumber.getText();
                    }
                }
            }
        });
    }

    private Container getLimitTextBox() {
        Box result = Box.createVerticalBox();
        Box hbox = Box.createHorizontalBox();
        hbox.add(new JLabel("(the lines skipped above don't count in)"));
        hbox.add(Box.createHorizontalGlue());
        result.add(hbox);
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
        if (m_skipLines.isSelected()) {
            // the text in the maxNumber should really be a number
            try {
                settings.setSkipFirstLines(Long
                        .parseLong(m_skipNumber.getText().trim()));
            } catch (NumberFormatException nfe) {
                // that's kinda bad...
                NodeLogger.getLogger(LimitRowsPanel.class).error(
                        "Entered invalid number for "
                                + "number of rows to skip."
                                + "Reading ALL rows!");
                settings.setSkipFirstLines(0);
            }
        } else {
            settings.setSkipFirstLines(0);
        }
        // skipping lines may change col headers
        return true;
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

        final long skipRows = settings.getSkipFirstLines();
        if (skipRows <= 0) {
            m_skipLines.setSelected(false);
        } else {
            m_skipLines.setSelected(true);
            m_skipNumber.setText(Long.toString(skipRows));
            m_lastValidSkips = m_skipNumber.getText();
        }
    }
}
