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
