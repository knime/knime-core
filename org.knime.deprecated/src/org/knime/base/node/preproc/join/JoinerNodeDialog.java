/* 
 * -------------------------------------------------------------------
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
 *   01.08.2005 (bernd): created
 */
package org.knime.base.node.preproc.join;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import org.knime.base.data.join.JoinedTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;


/**
 * 
 * @author Bernd Wiswedel, University of Konstanz
 */
public class JoinerNodeDialog extends NodeDialogPane {
    private final JRadioButton m_dontExcecuteButton;

    private final JRadioButton m_filterDuplicatesButton;

    private final JRadioButton m_appendSuffixButton;

    private final JTextField m_suffixText;

    private final JCheckBox m_ignoreMissingRows;

    /**
     * Constructs new dialog, sets title.
     */
    public JoinerNodeDialog() {
        super();
        m_suffixText = new JTextField(8);
        ButtonGroup buttonGroup = new ButtonGroup();
        ActionListener actionListener = new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                m_suffixText.setEnabled(m_appendSuffixButton.isSelected());
            }
        };
        m_dontExcecuteButton = new JRadioButton("Don't execute");
        m_dontExcecuteButton.setToolTipText("Will fail the execution "
                + "of the node when duplicates are encountered");
        buttonGroup.add(m_dontExcecuteButton);
        m_dontExcecuteButton.addActionListener(actionListener);
        m_filterDuplicatesButton = new JRadioButton("Filter duplicates");
        m_filterDuplicatesButton.setToolTipText("Will filter columns in the "
                + "second table that are contained in the first table");
        buttonGroup.add(m_filterDuplicatesButton);
        m_filterDuplicatesButton.addActionListener(actionListener);
        m_appendSuffixButton = new JRadioButton("Append suffix: ");
        m_appendSuffixButton.setToolTipText("Will append a fixed suffix "
                + "to duplicate column names in the second table");
        buttonGroup.add(m_appendSuffixButton);
        m_appendSuffixButton.addActionListener(actionListener);
        m_dontExcecuteButton.doClick();
        JPanel buttonPanel = new JPanel(new GridLayout(0, 1));
        buttonPanel.setBorder(BorderFactory.createTitledBorder(""));
        buttonPanel.add(m_dontExcecuteButton);
        buttonPanel.add(m_filterDuplicatesButton);
        JPanel flowPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 5));
        flowPanel.add(m_appendSuffixButton);
        flowPanel.add(m_suffixText);
        buttonPanel.add(flowPanel);

        m_ignoreMissingRows = new JCheckBox("Remove rows missing in one table");
        buttonPanel.add(m_ignoreMissingRows);

        JPanel finalPanel = new JPanel(new BorderLayout());
        finalPanel.add(buttonPanel, BorderLayout.CENTER);

        addTab("Duplicate Columns", finalPanel);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) throws NotConfigurableException {
        String method = settings.getString(
                JoinerNodeModel.CFG_DUPLICATE_METHOD, JoinedTable.METHOD_FAIL);
        String suffix = settings.getString(JoinerNodeModel.CFG_SUFFIX, "_dupl");
        if (JoinedTable.METHOD_APPEND_SUFFIX.equals(method)) {
            m_appendSuffixButton.setSelected(true);
        } else if (JoinedTable.METHOD_FILTER.equals(method)) {
            m_filterDuplicatesButton.setSelected(true);
        } else {
            m_dontExcecuteButton.setSelected(true);
        }
        m_suffixText.setText(suffix);
        m_ignoreMissingRows.setSelected(settings.getBoolean(
                JoinerNodeModel.CFG_IGNORE_MISSING_ROWS, false));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        String method;
        if (m_filterDuplicatesButton.isSelected()) {
            method = JoinedTable.METHOD_FILTER;
        } else if (m_appendSuffixButton.isSelected()) {
            method = JoinedTable.METHOD_APPEND_SUFFIX;
        } else {
            method = JoinedTable.METHOD_FAIL;
        }
        String suffix = m_suffixText.getText();
        settings.addString(JoinerNodeModel.CFG_DUPLICATE_METHOD, method);
        settings.addString(JoinerNodeModel.CFG_SUFFIX, suffix);
        settings.addBoolean(JoinerNodeModel.CFG_IGNORE_MISSING_ROWS,
                m_ignoreMissingRows.isSelected());
    }
}
