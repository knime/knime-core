/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.com; Email: contact@knime.com
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
 * Created on Mar 17, 2013 by wiswedel
 */
package org.knime.base.node.preproc.columnlag;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.Border;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.util.ColumnSelectionPanel;
import org.knime.core.node.util.DataValueColumnFilter;

/**
 * Dialog to node.
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
final class LagColumnNodeDialogPane extends NodeDialogPane {

    private final ColumnSelectionPanel m_columnChooser;
    private final JSpinner m_lagIntervalSpinner;
    private final JSpinner m_lagSpinner;
    private final JCheckBox m_skipInitialIncompleteRowsChecker;
    private final JCheckBox m_skipLastIncompleteRowsChecker;

    /** Inits fields, add tab. */
    @SuppressWarnings("unchecked")
    LagColumnNodeDialogPane() {
        m_columnChooser = new ColumnSelectionPanel((Border)null,
                                                   new DataValueColumnFilter(DataValue.class), false, true);
        m_lagIntervalSpinner = new JSpinner(new SpinnerNumberModel(1, 1, Integer.MAX_VALUE, 1));
        m_lagSpinner = new JSpinner(new SpinnerNumberModel(1, 1, Integer.MAX_VALUE, 1));
        m_skipInitialIncompleteRowsChecker = new JCheckBox("Skip initial incomplete rows");
        m_skipLastIncompleteRowsChecker = new JCheckBox("Skip last incomplete rows");
        addTab("Configuration", createPanel());
    }

    /** Create and layout new panel.
     * @return ...
     */
    private JPanel createPanel() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        JPanel result = new JPanel(new GridBagLayout());

        gbc.gridx = 0;
        gbc.gridy = 0;
        result.add(new JLabel("Column to lag "), gbc);
        gbc.gridx += 1;
        result.add(m_columnChooser, gbc);

        gbc.gridy += 1;
        gbc.gridx = 0;
        result.add(new JLabel("Lag "), gbc);
        gbc.gridx += 1;
        result.add(m_lagSpinner, gbc);

        gbc.gridy += 1;
        gbc.gridx = 0;
        result.add(new JLabel("Lag interval "), gbc);
        gbc.gridx += 1;
        result.add(m_lagIntervalSpinner, gbc);

        gbc.gridy += 1;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        result.add(m_skipInitialIncompleteRowsChecker, gbc);

        gbc.gridy += 1;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        result.add(m_skipLastIncompleteRowsChecker, gbc);
        return result;
    }

    /** {@inheritDoc} */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
                                    final DataTableSpec[] specs) {
        LagColumnConfiguration cfg = new LagColumnConfiguration();
        cfg.loadSettingsInDialog(settings, specs[0]);
        String col = cfg.getColumn();
        try {
            m_columnChooser.update(specs[0], col, col == null);
        } catch (NotConfigurableException e) {
            throw new RuntimeException("Unexpected exception as row id selection is allowed", e);
        }
        m_lagIntervalSpinner.setValue(cfg.getLagInterval());
        m_lagSpinner.setValue(cfg.getLag());
        m_skipInitialIncompleteRowsChecker.setSelected(cfg.isSkipInitialIncompleteRows());
        m_skipLastIncompleteRowsChecker.setSelected(cfg.isSkipLastIncompleteRows());
    }

    /** {@inheritDoc} */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        LagColumnConfiguration cfg = new LagColumnConfiguration();
        cfg.setColumn(m_columnChooser.rowIDSelected() ? null : m_columnChooser.getSelectedColumn());
        cfg.setLagInterval((Integer)m_lagIntervalSpinner.getValue());
        cfg.setLag((Integer)m_lagSpinner.getValue());
        cfg.setSkipInitialIncompleteRows(m_skipInitialIncompleteRowsChecker.isSelected());
        cfg.setSkipLastIncompleteRows(m_skipLastIncompleteRowsChecker.isSelected());
        cfg.saveSettings(settings);
    }

}
