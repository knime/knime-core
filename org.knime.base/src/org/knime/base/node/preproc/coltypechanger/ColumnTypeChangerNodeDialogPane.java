/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 * ------------------------------------------------------------------------
 */
package org.knime.base.node.preproc.coltypechanger;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.util.filter.column.DataColumnSpecFilterConfiguration;
import org.knime.core.node.util.filter.column.DataColumnSpecFilterPanel;

/**
 * This is the dialog for the column type changer. The user can specify which columns
 * should be converted in the output table.
 *
 * @author Tim-Oliver Buchholz, KNIME.com AG, Zurich
 * @since 2.10
 */
public class ColumnTypeChangerNodeDialogPane extends NodeDialogPane {

    private final JPanel m_panel;
    private final DataColumnSpecFilterPanel m_filterPanel;
    private final JLabel m_dateBoxLabel;
    private final JComboBox<String> m_dateBox;
    private final String[] dateFormats;

    /**
     * Creates a new {@link NodeDialogPane} for the column filter in order to
     * set the desired columns.
     */
    public ColumnTypeChangerNodeDialogPane() {
        m_panel = new JPanel();
        m_filterPanel = new DataColumnSpecFilterPanel();
        dateFormats = new String[]{"dd.MM.yy", "dd.MM.yy hh:mm:ss", "dd.MM.yy hh:mm:ss:SSS"};
        m_dateBox = new JComboBox<String>(dateFormats);
        m_dateBox.setEditable(true);
        m_dateBoxLabel = new JLabel("Choose a date format: ");

        m_panel.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridwidth = 2;
        c.gridx = 0;
        c.gridy = 0;
        m_panel.add(m_filterPanel, c);

        c.fill = GridBagConstraints.NONE;
        c.weightx = 0.5;
        c.anchor = GridBagConstraints.EAST;
        c.gridwidth = 1;
        c.insets = new Insets(20, 0, 0, 0);
        c.gridx = 0;
        c.gridy = 1;
        m_panel.add(m_dateBoxLabel, c);

        c.fill = GridBagConstraints.NONE;
        c.weightx = 0.5;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(20, 0, 0, 0);
        c.gridx = 1;
        c.gridy = 1;
        m_panel.add(m_dateBox, c);


        super.addTab("Configuration", m_panel);
    }

    /**
     * Calls the update method of the underlying filter panel.
     * @param settings the node settings to read from
     * @param specs the input specs
     * @throws NotConfigurableException if no columns are available for
     *             filtering
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) throws NotConfigurableException {
        final DataTableSpec spec = specs[0];
        if (spec == null || spec.getNumColumns() == 0) {
            throw new NotConfigurableException("No columns available for "
                    + "selection.");
        }

        DataColumnSpecFilterConfiguration config = ColumnTypeChangerNodeModel.createDCSFilterConfiguration();
        config.loadConfigurationInDialog(settings, specs[0]);
        m_filterPanel.loadConfiguration(config, specs[0]);
        m_dateBox.setSelectedItem(settings.getString("dateFormat", "dd.MM.yy"));

    }

    /**
     * Sets the list of columns to exclude inside the corresponding
     * <code>NodeModel</code> which are retrieved from the filter panel.
     * @param settings the node settings to write into
     * @throws InvalidSettingsException if one of the settings is not valid
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        DataColumnSpecFilterConfiguration config = ColumnTypeChangerNodeModel.createDCSFilterConfiguration();
        m_filterPanel.saveConfiguration(config);
        config.saveConfiguration(settings);

        settings.addString("dateFormat", m_dateBox.getEditor().getItem().toString());
    }
}