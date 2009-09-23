/*
 * -------------------------------------------------------------------
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
 *   09.08.2005 (bernd): created
 */
package org.knime.base.node.preproc.append.row;

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

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;


/**
 * Dialog that allows for treatment of duplicate row keys. Possible options are:
 * (1) skip duplicate rows, (2) append suffix to key.
 *
 * @author Bernd Wiswedel, University of Konstanz
 */
public class AppendedRowsNodeDialog extends NodeDialogPane {

    private final JRadioButton m_appendSuffixButton;

    private final JRadioButton m_skipRowButton;

    private final JTextField m_suffixField;

    private final JRadioButton m_useInterSectionButton;

    private final JRadioButton m_useUnionButton;

    private final JCheckBox m_enableHiliting;

    /**
     * Constructor to init the gui and set a title.
     */
    public AppendedRowsNodeDialog() {
        ActionListener actionListener = new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                m_suffixField.setEnabled(m_appendSuffixButton.isSelected());
            }
        };
        m_suffixField = new JTextField(8);
        ButtonGroup buttonGroup = new ButtonGroup();
        m_skipRowButton = new JRadioButton("Skip Rows");
        m_skipRowButton.setToolTipText("Will skip duplicate rows and print "
                + "and print a warning message.");
        m_skipRowButton.addActionListener(actionListener);
        buttonGroup.add(m_skipRowButton);
        m_appendSuffixButton = new JRadioButton("Append Suffix: ");
        m_appendSuffixButton.setToolTipText("Will append a suffix to any "
                + "duplicate row ID to make it unique.");
        m_appendSuffixButton.addActionListener(actionListener);
        buttonGroup.add(m_appendSuffixButton);
        JPanel panel = new JPanel(new GridLayout(0, 1));
        JPanel centerPanel = new JPanel(new GridLayout(0, 1));
        centerPanel.setBorder(BorderFactory
                .createTitledBorder("Duplicate row ID handling"));
        JPanel skipButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        skipButtonPanel.add(m_skipRowButton);
        centerPanel.add(skipButtonPanel);
        JPanel suffixButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        suffixButtonPanel.add(m_appendSuffixButton);
        suffixButtonPanel.add(m_suffixField);
        centerPanel.add(suffixButtonPanel);
        m_skipRowButton.doClick();
        panel.add(centerPanel);

        ButtonGroup bGroup = new ButtonGroup();
        m_useInterSectionButton = new JRadioButton(
                "Use intersection of columns");
        m_useUnionButton = new JRadioButton("Use union of columns");
        bGroup.add(m_useInterSectionButton);
        bGroup.add(m_useUnionButton);
        JPanel intersectPanel = new JPanel(new GridLayout(0, 1));
        intersectPanel.setBorder(BorderFactory
                .createTitledBorder("Column handling"));
        JPanel intersectButPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        intersectButPanel.add(m_useInterSectionButton);
        intersectPanel.add(intersectButPanel);
        JPanel unionButPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        unionButPanel.add(m_useUnionButton);
        intersectPanel.add(unionButPanel);
        panel.add(intersectPanel);

        JPanel hilitePanel = new JPanel(new BorderLayout());
        hilitePanel.setBorder(BorderFactory
                .createTitledBorder("Hiliting"));
        m_enableHiliting = new JCheckBox("Enable hiliting");
        hilitePanel.add(m_enableHiliting);
        panel.add(hilitePanel, BorderLayout.CENTER);
        addTab("Settings", panel);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) throws NotConfigurableException {
        boolean appendSuffix = settings.getBoolean(
                AppendedRowsNodeModel.CFG_APPEND_SUFFIX, false);
        String suffix = settings.getString(AppendedRowsNodeModel.CFG_SUFFIX,
                "x");
        if (appendSuffix) {
            m_appendSuffixButton.doClick();
        } else {
            m_skipRowButton.doClick();
        }
        m_suffixField.setText(suffix);

        boolean isUseIntersection = settings.getBoolean(
                AppendedRowsNodeModel.CFG_INTERSECT_COLUMNS, false);
        if (isUseIntersection) {
            m_useInterSectionButton.doClick();
        } else {
            m_useUnionButton.doClick();
        }
        m_enableHiliting.setSelected(settings.getBoolean(
                AppendedRowsNodeModel.CFG_HILITING, false));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        boolean isSuffix = m_appendSuffixButton.isSelected();
        boolean isIntersection = m_useInterSectionButton.isSelected();
        String suffix = m_suffixField.getText();
        settings.addBoolean(AppendedRowsNodeModel.CFG_APPEND_SUFFIX, isSuffix);
        settings.addString(AppendedRowsNodeModel.CFG_SUFFIX, suffix);
        settings.addBoolean(AppendedRowsNodeModel.CFG_INTERSECT_COLUMNS,
                isIntersection);
        settings.addBoolean(AppendedRowsNodeModel.CFG_HILITING,
                m_enableHiliting.isSelected());
    }
}
