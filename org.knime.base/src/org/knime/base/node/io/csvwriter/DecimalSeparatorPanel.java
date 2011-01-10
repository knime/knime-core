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
 * ---------------------------------------------------------------------
 *
 * History
 *   20.11.2008 (ohl): created
 */
package org.knime.base.node.io.csvwriter;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * Panel of the CSV writer dialog for specifying the decimal separator.
 *
 * @author ohl, University of Konstanz
 */
public class DecimalSeparatorPanel extends JPanel {

    private final JTextField m_decSeparator = new JTextField();

    private final JLabel m_warning = new JLabel();

    /**
     *
     */
    public DecimalSeparatorPanel() {

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createTitledBorder(BorderFactory
                .createEtchedBorder(), "Decimal Separator:"));
        add(Box.createVerticalStrut(20));
        add(createTextPanel());
        add(Box.createVerticalStrut(10));
        add(createEditPanel());
        add(createWarningPanel());
        add(Box.createVerticalGlue());
        add(Box.createVerticalGlue());
    }

    private JPanel createTextPanel() {

        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));

        Box lineBox;
        lineBox = Box.createHorizontalBox();
        lineBox.add(new JLabel("Specify a character that should be used as "));
        lineBox.add(Box.createHorizontalGlue());
        textPanel.add(lineBox);
        lineBox = Box.createHorizontalBox();
        lineBox.add(new JLabel("decimal separator in floating point values. "));
        lineBox.add(Box.createHorizontalGlue());
        textPanel.add(lineBox);
        lineBox = Box.createHorizontalBox();
        lineBox.add(new JLabel("Specify only one character."));
        lineBox.add(Box.createHorizontalGlue());
        textPanel.add(lineBox);

        return textPanel;
    }

    private JPanel createEditPanel() {

        m_decSeparator.setColumns(5);
        m_decSeparator.setPreferredSize(new Dimension(100, 25));
        m_decSeparator.setMaximumSize(new Dimension(100, 25));

        m_decSeparator.getDocument().addDocumentListener(
                new DocumentListener() {
                    @Override
                    public void removeUpdate(final DocumentEvent e) {
                        decSepChanged();
                    }

                    @Override
                    public void insertUpdate(final DocumentEvent e) {
                        decSepChanged();
                    }

                    @Override
                    public void changedUpdate(final DocumentEvent e) {
                        decSepChanged();
                    }
                });

        JPanel editPanel = new JPanel();
        editPanel.setLayout(new BoxLayout(editPanel, BoxLayout.Y_AXIS));

        Box editBox = Box.createHorizontalBox();
        editBox.add(new JLabel("Enter decimal separator character:"));
        editBox.add(Box.createHorizontalStrut(5));
        editBox.add(m_decSeparator);
        editBox.add(Box.createHorizontalGlue());

        editPanel.add(editBox);
        return editPanel;
    }

    private Component createWarningPanel() {

        m_warning.setForeground(m_warning.getBackground());
        m_warning.setText(
                "Long text to get enough space at layout time "
                + "actual and useful text is set.");
        JPanel warnPanel = new JPanel();
        warnPanel.setLayout(new BoxLayout(warnPanel, BoxLayout.Y_AXIS));

        Box lineBox;
        lineBox = Box.createHorizontalBox();
        lineBox.add(m_warning);
        // make sure we reserve enough space for a message
        lineBox.add(Box.createVerticalStrut(30));
        lineBox.add(Box.createHorizontalGlue());

        warnPanel.add(lineBox);
        return warnPanel;
    }

    /**
     * Updates the values in the components from the passed settings object.
     *
     * @param settings the object holding the values to load.
     */
    void loadValuesIntoPanel(final FileWriterNodeSettings settings) {

        m_decSeparator.setText("" + settings.getDecimalSeparator());
        // update warning label
        decSepChanged();
    }

    /**
     * Saves the current values from the panel into the passed object.
     *
     * @param settings the object to write the values into
     */
    void saveValuesFromPanelInto(final FileWriterNodeSettings settings) {
        String decSep = m_decSeparator.getText();
        if (decSep.isEmpty()) {
            settings.setDecimalSeparator('.');
        } else {
            settings.setDecimalSeparator(decSep.charAt(0));
        }
    }

    private void decSepChanged() {
        String decSep = m_decSeparator.getText();
        String warn = "";

        if (decSep.isEmpty()) {
            warn = "The default decimal separator '.' is used.";
        } else if (decSep.length() > 1) {
            warn =
                    "Only the first character ('" + decSep.charAt(0)
                            + "') is used";
        } else {
            char sep = decSep.charAt(0);
            if (sep != '.') {
                String dblVal = Double.toString((-1234567890123.7654321e02));
                if (dblVal.indexOf(sep) >= 0) {
                    warn =
                        "The character is ignored if contained in a "
                        + "floating point number";
                }
            }
        }
        if (warn.length() == 0) {
            m_warning.setForeground(m_warning.getBackground());
            m_warning.setText("Long text to get enough space at layout time "
                    + "actual and useful text is set.");
        } else {
            m_warning.setForeground(Color.red);
            m_warning.setText(warn);
        }
        revalidate();
        repaint();
        invalidate();
        repaint();

    }
}
