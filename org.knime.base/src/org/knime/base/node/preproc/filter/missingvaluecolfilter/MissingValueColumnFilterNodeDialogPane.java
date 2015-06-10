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
package org.knime.base.node.preproc.filter.missingvaluecolfilter;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JPanel;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.SettingsModelNumber;
import org.knime.core.node.util.filter.column.DataColumnSpecFilterConfiguration;
import org.knime.core.node.util.filter.column.DataColumnSpecFilterPanel;

/**
 * This is the dialog for the missing value column filter. The user can specify which columns
 * should be checked against a certain missing value percentage.
 *
 * @author Tim-Oliver Buchholz, KNIME.com AG, Zurich, Switzerland
 */
public class MissingValueColumnFilterNodeDialogPane extends NodeDialogPane {

    private final DataColumnSpecFilterPanel m_filterPanel;

    private final DialogComponentNumber m_percentage;

    private final SettingsModelNumber m_percentageSettings;

    private final JPanel m_panel;

    /**
     * Creates a new {@link NodeDialogPane} for the column filter in order to
     * set the desired columns.
     */
    public MissingValueColumnFilterNodeDialogPane() {
        m_filterPanel = new DataColumnSpecFilterPanel();

        m_percentageSettings = MissingValueColumnFilterNodeModel.createSettingsModelNumber();

        m_percentage = new DialogComponentNumber(m_percentageSettings, "Missing value threshold (in %): ", 1, 3);

        m_panel = new JPanel(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.weightx = 1;
        c.weighty = 1;

        m_panel.add(m_filterPanel, c);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridy++;

        m_panel.add(m_percentage.getComponentPanel(), c);

        super.addTab("Configuration", m_panel);
    }

    /**
     * Calls the update method of the underlying filter panel.
     * @param settings the node settings to read from
     * @param specs the input specs
     * @throws NotConfigurableException if no columns are available for filtering
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) throws NotConfigurableException {
        final DataTableSpec spec = specs[0];
        if (spec == null || spec.getNumColumns() == 0) {
            throw new NotConfigurableException("No columns available for "
                    + "selection.");
        }

        DataColumnSpecFilterConfiguration config = MissingValueColumnFilterNodeModel.createDCSFilterConfiguration();
        config.loadConfigurationInDialog(settings, specs[0]);
        m_filterPanel.loadConfiguration(config, specs[0]);

        m_percentage.loadSettingsFrom(settings, specs);
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
        DataColumnSpecFilterConfiguration config = MissingValueColumnFilterNodeModel.createDCSFilterConfiguration();
        m_filterPanel.saveConfiguration(config);
        config.saveConfiguration(settings);

        m_percentage.saveSettingsTo(settings);
    }
}
