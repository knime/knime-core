/*
 * ------------------------------------------------------------------
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
 * -------------------------------------------------------------------
 * 
 * History
 *   18.07.2005 (ohl): created
 */
package org.knime.base.node.preproc.filter.row;

import java.awt.Color;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.knime.base.node.preproc.filter.row.rowfilter.RowFilter;
import org.knime.base.node.preproc.filter.row.rowfilter.RowIDRowFilter;
import org.knime.core.node.InvalidSettingsException;

/**
 * 
 * @author Peter Ohl, University of Konstanz
 */
public class RowIDRowFilterPanel extends RowFilterPanel {
    private JLabel m_errText;

    private JTextField m_regExpr;

    private JCheckBox m_caseSensitive;

    private JCheckBox m_startsWith;

    /**
     * Creates a new panel for a row ID filter.
     */
    public RowIDRowFilterPanel() {
        super(400, 350);

        m_errText = new JLabel();
        m_errText.setForeground(Color.RED);
        m_regExpr = new JTextField();
        m_caseSensitive = new JCheckBox("case sensitive match.");
        m_startsWith = new JCheckBox("row ID must only start with expression.");
        m_startsWith
                .setToolTipText("if not checked the entire row ID must match");
        m_regExpr.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(final DocumentEvent e) {
                regExprChanged();
            }

            public void removeUpdate(final DocumentEvent e) {
                regExprChanged();
            }

            public void changedUpdate(final DocumentEvent e) {
                regExprChanged();
            }
        });

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createTitledBorder(BorderFactory
                .createEtchedBorder(), "Row ID pattern:"));

        Box exprBox = Box.createHorizontalBox();
        exprBox.add(new JLabel("regular expression to match:"));
        exprBox.add(Box.createHorizontalStrut(3));
        exprBox.add(m_regExpr);
        exprBox.add(Box.createHorizontalGlue());

        panel.add(Box.createVerticalStrut(10));
        panel.add(exprBox);
        panel.add(Box.createVerticalStrut(7));
        panel.add(m_caseSensitive);
        panel.add(Box.createVerticalStrut(4));
        panel.add(m_startsWith);
        panel.add(Box.createVerticalStrut(10));
        panel.add(m_errText);
        panel.add(Box.createVerticalGlue());

        this.add(panel);

        updateErrText();
    }

    /**
     * Called when the textfield content changes.
     */
    protected void regExprChanged() {
        updateErrText();
    }

    private void updateErrText() {
        m_errText.setText("");
        if (m_regExpr.getText().length() <= 0) {
            m_errText.setText("Enter a valid regular expression");
            return;
        }
        try {
            Pattern.compile(m_regExpr.getText());
        } catch (PatternSyntaxException pse) {
            m_errText.setText("Error in regular expression ('"
                    + pse.getMessage() + "')");
        }
    }

    /**
     * @return <code>true</code> if an error message is currently displayed in
     *         the panel
     */
    boolean hasErrors() {
        return m_errText.getText().length() != 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void loadSettingsFromFilter(final RowFilter filter)
            throws InvalidSettingsException {
        if (!(filter instanceof RowIDRowFilter)) {
            throw new InvalidSettingsException("RegExpr filter panel can only "
                    + "load settings from a RegExprRowFilter");
        }

        RowIDRowFilter reFilter = (RowIDRowFilter)filter;

        m_caseSensitive.setSelected(reFilter.getCaseSensitivity());
        m_startsWith.setSelected(reFilter.getStartsWith());
        m_regExpr.setText(reFilter.getRegExpr());
        regExprChanged();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RowFilter createFilter(final boolean include)
            throws InvalidSettingsException {
        // just in case, because the err text is the indicator for err existence
        updateErrText();

        if (hasErrors()) {
            throw new InvalidSettingsException(m_errText.getText());
        }

        return new RowIDRowFilter(m_regExpr.getText(), include, m_caseSensitive
                .isSelected(), m_startsWith.isSelected());

    }
}
