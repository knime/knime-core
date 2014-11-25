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
 * -------------------------------------------------------------------
 *
 */
package org.knime.base.node.preproc.datavalidator;

import static org.knime.core.node.util.CheckUtils.checkArgument;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

import org.knime.base.node.preproc.datavalidator.DataValidatorConfiguration.RejectBehavior;
import org.knime.base.node.preproc.datavalidator.DataValidatorConfiguration.UnknownColumnHandling;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.util.RadionButtonPanel;

/**
 * Main dialog panel for the DataValidator node.
 *
 * @author Marcel Hanser, University of Konstanz
 */
public class DataValidatorSpecNodeDialogPane extends NodeDialogPane {

    private RadionButtonPanel<UnknownColumnHandling> m_unkownColumnsHandling;

    private RadionButtonPanel<RejectBehavior> m_failBehavior;

    private DataValidatorColPanel m_dataValidatorColPanel;

    private JPanel m_tabPanel;

    /**
     * Constructs new dialog with an appropriate dialog title.
     */
    public DataValidatorSpecNodeDialogPane() {
        super();

        m_failBehavior = new RadionButtonPanel<>("Behavior on validation issue", RejectBehavior.values());
        m_unkownColumnsHandling = new RadionButtonPanel<>("Handling of unkown columns", UnknownColumnHandling.values());

        JPanel generalConfigPanel = new JPanel(new GridLayout(0, 2));
        generalConfigPanel.add(m_failBehavior);
        generalConfigPanel.add(m_unkownColumnsHandling);
        generalConfigPanel.setBorder(BorderFactory.createTitledBorder("General settings"));

        //        m_southernPanel.add(generalConfigPanel, BorderLayout.SOUTH);

        m_tabPanel = new JPanel(new BorderLayout());
        m_tabPanel.add(generalConfigPanel, BorderLayout.SOUTH);

        addTab("Validation Settings", m_tabPanel);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final DataTableSpec[] specs)
        throws NotConfigurableException {

        DataValidatorConfiguration dataValidatorConfiguration = new DataValidatorConfiguration(false);
        dataValidatorConfiguration.loadConfigurationInDialog(settings, null);

        List<DataValidatorColConfiguration> individuals = dataValidatorConfiguration.getIndividualConfigurations();
        checkArgument(individuals.size() <= 1, "Wrong amount of configurations. Only one expected");
        DataValidatorColConfiguration toSet =
            !individuals.isEmpty() ? individuals.get(0) : new DataValidatorColConfiguration();

        if (m_dataValidatorColPanel != null) {
            m_tabPanel.remove(m_dataValidatorColPanel);
        }
        List<DataColumnSpec> columnSpecs = new ArrayList<>();
        DataTableSpec spec = specs[1];
        for (DataColumnSpec s : spec) {
            columnSpecs.add(s);
        }

        m_dataValidatorColPanel = new DataValidatorColPanel(null, false, toSet, columnSpecs);
        m_dataValidatorColPanel.setPreferredSize(new Dimension(0, 253));
        m_tabPanel.add(m_dataValidatorColPanel, BorderLayout.CENTER);
        m_failBehavior.setSelectedValue(dataValidatorConfiguration.getFailingBehavior());
        m_unkownColumnsHandling.setSelectedValue(dataValidatorConfiguration.getUnkownColumnsHandling());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        DataValidatorConfiguration dataValidatorConfiguration = new DataValidatorConfiguration(false);

        dataValidatorConfiguration.setIndividualConfigurations(Collections.singletonList(m_dataValidatorColPanel
            .getSettings()));
        dataValidatorConfiguration.setRemoveUnkownColumns(m_unkownColumnsHandling.getSelectedValue());
        dataValidatorConfiguration.setFailingBehavior(m_failBehavior.getSelectedValue());

        dataValidatorConfiguration.saveSettings(settings);
    }
}