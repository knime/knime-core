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
package org.knime.base.node.preproc.domain.editnominal.dic;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.HashMap;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.util.ViewUtils;
import org.knime.core.node.util.filter.column.DataColumnSpecFilterPanel;

/**
 * Dialog to node.
 *
 * @author Marcel Hanser
 */
final class EditNominalDomainDicNodeDialogPane extends NodeDialogPane {

    /**
     *
     */
    private static final String NEW_VALUES_LAST = "Domain values (2nd input) will be inserted";

    /**
     *
     */
    private static final String IGNORE_TYPES_NOT_MATCH = "If column types do not match";

    /**
     *
     */
    private static final String IGNORE_COLUMNS_NOT_PRESENT_IN_DATA = "If domain value columns are not present in data";

    private DataColumnSpecFilterPanel m_filterPanel;

    private JTextField m_maxAmountDomainValues;

    private Map<String, JRadioButton> m_buttonMap = new HashMap<String, JRadioButton>();

    /** Inits members, does nothing else. */
    public EditNominalDomainDicNodeDialogPane() {
        createEditNominalDomainTab();
    }

    private void createEditNominalDomainTab() {

        m_filterPanel = new DataColumnSpecFilterPanel();

        JPanel tabpanel = new JPanel(new BorderLayout());
        tabpanel.add(m_filterPanel, BorderLayout.NORTH);

        JPanel settings = new JPanel(new BorderLayout());

        settings.add(createRadioButtonGroup(IGNORE_COLUMNS_NOT_PRESENT_IN_DATA), BorderLayout.NORTH);

        JPanel secondSettings = new JPanel(new BorderLayout());

        secondSettings.add(createRadioButtonGroup(IGNORE_TYPES_NOT_MATCH), BorderLayout.NORTH);

        secondSettings.add(
            createRadioButtonGroup(NEW_VALUES_LAST, "Before existing domain values (1st input)",
                "After existing domain values (1st input)"), BorderLayout.CENTER);

        secondSettings.add(createTextField("Maximum amount of possbile domain values"), BorderLayout.SOUTH);

        settings.add(secondSettings, BorderLayout.SOUTH);

        tabpanel.add(settings, BorderLayout.SOUTH);

        addTab("Add Domain Values", tabpanel);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final DataTableSpec[] specs)
        throws NotConfigurableException {
        if (specs[0].getNumColumns() == 0) {
            throw new NotConfigurableException("No data at input.");
        }
        EditNominalDomainDicConfiguration forConfig = new EditNominalDomainDicConfiguration();
        forConfig.loadConfigurationInDialog(settings, specs[0], specs[1]);
        m_filterPanel.loadConfiguration(forConfig.getFilterConfiguration(), specs[1]);

        if (forConfig.isIgnoreDomainColumns()) {
            m_buttonMap.get(IGNORE_COLUMNS_NOT_PRESENT_IN_DATA).doClick();
        }
        if (forConfig.isIgnoreWrongTypes()) {
            m_buttonMap.get(IGNORE_TYPES_NOT_MATCH).doClick();
        }
        if (!forConfig.isAddNewValuesFirst()) {
            m_buttonMap.get(NEW_VALUES_LAST).doClick();
        }

        m_maxAmountDomainValues.setText(String.valueOf(forConfig.getMaxDomainValues()));

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        EditNominalDomainDicConfiguration configuration = new EditNominalDomainDicConfiguration();
        m_filterPanel.saveConfiguration(configuration.getFilterConfiguration());
        //        configuration.setColumnspecFilterCofig(m_filterPanel);
        configuration.setIgnoreDomainColumns(m_buttonMap.get(IGNORE_COLUMNS_NOT_PRESENT_IN_DATA).isSelected());
        configuration.setIgnoreWrongTypes(m_buttonMap.get(IGNORE_TYPES_NOT_MATCH).isSelected());
        configuration.setNewValuesFirst(!m_buttonMap.get(NEW_VALUES_LAST).isSelected());
        configuration.setMaxDomainValues(checkPositive(getInt(m_maxAmountDomainValues,
            "Maximum amount of possbile domain values is not a valid integer")));
        configuration.saveConfiguration(settings);
    }

    /**
     * @param int1
     * @param string
     * @return
     * @throws InvalidSettingsException
     */
    private int checkPositive(final int int1) throws InvalidSettingsException {
        CheckUtils.checkSetting(int1 >= 0, "Maximum amount of possbile domain values must not be negative");
        return int1;
    }

    /**
     * @param maxAmountDomainValues
     * @return
     * @throws InvalidSettingsException
     */
    private int getInt(final JTextField maxAmountDomainValues, final String exceptionText)
        throws InvalidSettingsException {
        String text = maxAmountDomainValues.getText();
        try {
            return Integer.valueOf(text);

        } catch (Exception e) {
            throw new InvalidSettingsException(exceptionText);
        }
    }

    /**
     * @param string
     */
    private JPanel createRadioButtonGroup(final String string) {
        return createRadioButtonGroup(string, "Fail", "Ignore column                   "
            + "                              ");
    }

    /**
     * @param string
     */
    private JPanel createRadioButtonGroup(final String string, final String first, final String second) {
        JPanel toReturn = new JPanel(new GridLayout(2, 1));
        toReturn.setBorder(BorderFactory.createTitledBorder(string));
        JRadioButton firstBut = new JRadioButton(first);
        JRadioButton secondBut = new JRadioButton(second);

        m_buttonMap.put(string, secondBut);

        ButtonGroup buttonGroup = new ButtonGroup();
        buttonGroup.add(firstBut);
        buttonGroup.add(secondBut);

        firstBut.setSelected(true);

        toReturn.add(firstBut);
        toReturn.add(secondBut);

        return toReturn;
    }

    /**
     * @return
     */
    private Component createTextField(final String title) {
        m_maxAmountDomainValues = new JTextField(6);
        JPanel inFlowLayout = ViewUtils.getInFlowLayout(FlowLayout.LEFT, m_maxAmountDomainValues);
        inFlowLayout.setBorder(BorderFactory.createTitledBorder(title));

        return inFlowLayout;
    }
}
