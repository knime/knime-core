/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
 * University of Konstanz, Germany
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
