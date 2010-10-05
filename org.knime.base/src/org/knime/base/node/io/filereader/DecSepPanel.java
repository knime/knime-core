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
 *   04.05.2006 (ohl): created
 */
package org.knime.base.node.io.filereader;

import java.awt.Container;
import java.awt.Dimension;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * Implements the tab panel for the decimal separator settings (in the advaced
 * settings dialog).
 *
 * @author Peter Ohl, University of Konstanz
 */
public class DecSepPanel extends JPanel {

    private JTextField m_decSep;

    private JTextField m_thousandSep;

    /**
     * Creates a panel to set the decimal separator and initializes it from the
     * passed object.
     *
     * @param settings the settings to initialize to panel from
     */
    DecSepPanel(final FileReaderSettings settings) {
        this.setSize(520, 375);
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        add(Box.createVerticalStrut(20));
        add(Box.createVerticalGlue());
        add(getTextBox());
        add(Box.createVerticalStrut(10));
        add(getPanel());
        add(Box.createVerticalGlue());

        loadSettings(settings);

    }

    private Container getPanel() {
        Box decSepBox = Box.createHorizontalBox();
        decSepBox.add(Box.createHorizontalGlue());
        decSepBox.add(new JLabel("Decimal separator character:"));
        decSepBox.add(Box.createHorizontalStrut(5));
        m_decSep = new JTextField(2);
        m_decSep.setPreferredSize(new Dimension(50, 25));
        m_decSep.setMinimumSize(new Dimension(50, 25));
        m_decSep.setMaximumSize(new Dimension(50, 25));
        decSepBox.add(m_decSep);

        Box thousSepBox = Box.createHorizontalBox();
        thousSepBox.add(Box.createHorizontalGlue());
        thousSepBox.add(new JLabel("Thousands separator character:"));
        thousSepBox.add(Box.createHorizontalStrut(5));
        m_thousandSep = new JTextField(2);
        m_thousandSep.setPreferredSize(new Dimension(50, 25));
        m_thousandSep.setMinimumSize(new Dimension(50, 25));
        m_thousandSep.setMaximumSize(new Dimension(50, 25));
        thousSepBox.add(m_thousandSep);


        Box innerBox = Box.createVerticalBox();
        innerBox.add(Box.createVerticalGlue());
        innerBox.add(decSepBox);
        innerBox.add(Box.createVerticalStrut(10));
        innerBox.add(thousSepBox);
        innerBox.add(Box.createVerticalGlue());

        Box result = Box.createHorizontalBox();
        result.add(Box.createHorizontalGlue());
        result.add(innerBox);
        result.add(Box.createHorizontalGlue());
        return result;
    }

    private Container getTextBox() {
        Box result = Box.createVerticalBox();
        result.add(new JLabel("Enter new separators "
                + "(used for double columns only)."));
        result.add(Box.createVerticalStrut(20));
        result.add(new JLabel("The decimal separator is the floating"
                + " point character (required),"));
        result.add(new JLabel("the thousands separator groups"
                + " thousands and can be omitted."));
        result.add(Box.createVerticalGlue());
        return result;
    }

    private void loadSettings(final FileReaderSettings settings) {
        m_decSep.setText("" + settings.getDecimalSeparator());
        if (settings.getThousandsSeparator() == '\0') {
            m_thousandSep.setText("");
        } else {
            m_thousandSep.setText("" + settings.getThousandsSeparator());
        }
    }

    /**
     * Checks if the settings in the panel are good for applying them.
     *
     * @return null if all settings are okay, or an error message if settings
     *         can't be taken over.
     *
     */
    String checkSettings() {
        String decSep = m_decSep.getText();
        String thousandSep = m_thousandSep.getText();
        if ((decSep == null) || (decSep.length() == 0)) {
            return "No decimal separator specified.";
        }
        if (decSep.length() > 1) {
            return "Please enter only one character as decimal separator.";
        }
        if (thousandSep.length() > 1) {
            return "Please enter only one character as thousands separator.";
        }
        if (decSep.equals(thousandSep)) {
            return "Decimal and thousands separator can't be the same.";
        }
        return null;
    }

    /**
     * Writes the current settings of the panel into the passed settings object.
     *
     * @param settings the object to write settings in
     * @return true if the new settings are different from the one passed in.
     */
    boolean overrideSettings(final FileReaderNodeSettings settings) {
        char oldDecSep = settings.getDecimalSeparator();
        char oldThousSep = settings.getThousandsSeparator();
        char newDecSep = m_decSep.getText().charAt(0);
        char newThousSep = '\0'; // i.e. no thousands separator
        if (m_thousandSep.getText().length() > 0) {
            newThousSep = m_thousandSep.getText().charAt(0);
        }
        boolean changed =
                (oldDecSep != newDecSep) || (oldThousSep != newThousSep);
        // clear the thousand sep first.
        settings.setThousandsSeparator('\0');
        settings.setDecimalSeparator(newDecSep);
        settings.setThousandsSeparator(newThousSep);
        if (changed) {
            settings.setDecimalSeparatorUserSet(true);
        }
        return changed;
    }
}
