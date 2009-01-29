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
 * -------------------------------------------------------------------
 *
 * History
 *   18.03.2008 (ohl): created
 */
package org.knime.base.node.io.filereader;

import java.awt.Container;
import java.awt.Dimension;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * Implements the tab panel for the missing value pattern in string columns
 * (in the advanced settings dialog).
 *
 * @author Peter Ohl, University of Konstanz
 */
public class MissingValuePanel extends JPanel {

    private JTextField m_missingValue;
    private JLabel m_warnLabel;

    /**
     * Creates a panel to set the missing value pattern for string columns
     * and initializes it from the passed object.
     *
     * @param settings the settings to initialize to panel from
     */
    MissingValuePanel(final FileReaderSettings settings) {
        this.setSize(520, 375);
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        add(Box.createVerticalStrut(20));
        add(Box.createVerticalGlue());
        add(getTextBox());
        add(Box.createVerticalStrut(5));
        add(getPanel());
        add(Box.createVerticalStrut(3));
        add(getWarnBox());
        add(Box.createVerticalGlue());

        loadSettings(settings);

    }

    private Container getPanel() {

        Box missValBox = Box.createHorizontalBox();
        missValBox.add(Box.createHorizontalGlue());
        missValBox.add(new JLabel("StringType missing value pattern:"));
        missValBox.add(Box.createHorizontalStrut(5));
        m_missingValue = new JTextField(15);
        m_missingValue.setPreferredSize(new Dimension(150, 25));
        m_missingValue.setMinimumSize(new Dimension(150, 25));
        m_missingValue.setMaximumSize(new Dimension(150, 25));
        missValBox.add(m_missingValue);

        m_missingValue.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(final DocumentEvent e) {
                updateWarnBox();
            }

            public void removeUpdate(final DocumentEvent e) {
                updateWarnBox();
            }

            public void changedUpdate(final DocumentEvent e) {
                updateWarnBox();
            }
        });


        Box result = Box.createHorizontalBox();
        result.add(Box.createHorizontalGlue());
        result.add(missValBox);
        result.add(Box.createHorizontalGlue());
        return result;
    }

    private Container getTextBox() {
        Box result = Box.createVerticalBox();
        result.add(new JLabel("The entered pattern is "
                + "used in string columns only."));
        result.add(Box.createVerticalStrut(20));
        result.add(new JLabel("If the specified pattern is read as data item"
                + " from the"));
        result.add(new JLabel("file it will be represented by a data cell"
                + " with a missing value "));
        result.add(new JLabel("in the output table."));
        result.add(Box.createVerticalStrut(20));
        result.add(new JLabel("This global pattern is overridden by a "
                + "pattern"));
        result.add(new JLabel("specified for a specific column (when "
                + "clicking on the "));
        result.add(new JLabel("column header in the preview table)."));


        result.add(Box.createVerticalGlue());
        return result;
    }

    private Container getWarnBox() {
        m_warnLabel = new JLabel("");
        Box result = Box.createHorizontalBox();
        result.add(Box.createVerticalStrut(25));
        result.add(Box.createHorizontalGlue());
        result.add(m_warnLabel);
        result.add(Box.createHorizontalGlue());
        return result;
    }

    private void updateWarnBox() {

        m_warnLabel.setText("");

        // set a info message if the entered pattern has spaces
        String p = m_missingValue.getText();

        if (!p.isEmpty() && p.trim().isEmpty()) {
            // entered spaces only
            String msg = "INFO: pattern is " + p.length() + " space";
            if (p.length() > 1) {
                msg += "s";
            }
            m_warnLabel.setText(msg);
        } else if (p.length() != p.trim().length()) {
            m_warnLabel.setText("INFO: pattern has spaces at the beginning"
                    + " and/or at the end");
        }
    }

    private void loadSettings(final FileReaderSettings settings) {
        String p = settings.getMissValuePatternStrCols();
        if (p == null) {
            p = "";
        }
        m_missingValue.setText(p);
    }

    /**
     * Checks if the settings in the panel are good for applying them.
     *
     * @return null if all settings are okay, or an error message if settings
     *         can't be taken over.
     *
     */
    String checkSettings() {
        // we never complain
        return null;
    }

    /**
     * Writes the current settings of the panel into the passed settings object.
     *
     * @param settings the object to write settings in
     * @return true if the new settings are different from the one passed in.
     */
    boolean overrideSettings(final FileReaderSettings settings) {

        String newValue = m_missingValue.getText();
        if (newValue.isEmpty()) {
            newValue = null;
        }
        String oldValue = settings.getMissValuePatternStrCols();
        settings.setMissValuePatternStrCols(newValue);

        if (newValue == null) {
            return !(oldValue == null);
        } else {
            return !newValue.equals(oldValue);
        }
    }
}
