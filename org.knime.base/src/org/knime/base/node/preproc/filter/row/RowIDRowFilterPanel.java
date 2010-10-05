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
