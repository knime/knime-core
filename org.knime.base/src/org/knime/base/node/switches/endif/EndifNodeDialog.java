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
 * -------------------------------------------------------------------
 *
 */
package org.knime.base.node.switches.endif;

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

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;

/**
 * Dialog for the node collecting data from two - potentially
 * inactive - branches. It allows for treatment of duplicate
 * row keys in case of both branches carrying data. Possible options
 * are: (1) skip duplicate rows, (2) append suffix to key.
 *
 * @author M. Berthold, University of Konstanz
 * (copied from AppendedRowsNodeDialog)
 */
public class EndifNodeDialog extends NodeDialogPane {

    private final JRadioButton m_appendSuffixButton;

    private final JRadioButton m_skipRowButton;

    private final JTextField m_suffixField;

    private final JCheckBox m_enableHiliting;

    /**
     * Constructor to init the gui and set a title.
     */
    public EndifNodeDialog() {
        ActionListener actionListener = new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                m_suffixField.setEnabled(m_appendSuffixButton.isSelected());
            }
        };
        m_suffixField = new JTextField(8);
        ButtonGroup buttonGroup = new ButtonGroup();
        m_skipRowButton = new JRadioButton("Skip Rows");
        m_skipRowButton.setToolTipText("Will skip duplicate rows and print "
                + "a warning message.");
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
            final PortObjectSpec[] specs) throws NotConfigurableException {
        boolean appendSuffix = settings.getBoolean(
                EndifNodeModel.CFG_APPEND_SUFFIX, false);
        String suffix = settings.getString(EndifNodeModel.CFG_SUFFIX,
                "x");
        if (appendSuffix) {
            m_appendSuffixButton.doClick();
        } else {
            m_skipRowButton.doClick();
        }
        m_suffixField.setText(suffix);

        m_enableHiliting.setSelected(settings.getBoolean(
                EndifNodeModel.CFG_HILITING, false));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        boolean isSuffix = m_appendSuffixButton.isSelected();
        String suffix = m_suffixField.getText();
        settings.addBoolean(EndifNodeModel.CFG_APPEND_SUFFIX, isSuffix);
        settings.addString(EndifNodeModel.CFG_SUFFIX, suffix);
        settings.addBoolean(EndifNodeModel.CFG_HILITING,
                m_enableHiliting.isSelected());
    }
}
