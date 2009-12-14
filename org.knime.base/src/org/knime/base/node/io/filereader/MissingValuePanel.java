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
