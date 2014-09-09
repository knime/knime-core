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
 */

package org.knime.core.node.port.database.aggregation.function.custom;


import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JPanel;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataValue;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.SettingsModelString;


/**
 * {@link JPanel} that allows the user to specify layout mapping settings.
 *
 * @author Tobias Koetter
 * @since 2.11
 */
public class CustomDBAggregationFuntionSettingsPanel extends JPanel {

    private static final long serialVersionUID = 1;
    private final DialogComponentColumnNameSelection m_secondColumnComponent;
    private DialogComponentString m_resultColComponent;
    private DialogComponentString m_functionComponent;

    /**
     * @param settings the {@link CustomDBAggregationFuntionSettings} to use
     */
    @SuppressWarnings("unchecked")
    public CustomDBAggregationFuntionSettingsPanel(final CustomDBAggregationFuntionSettings settings) {
        setLayout(new GridBagLayout());
        final GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.LINE_START;
        c.gridx = 0;
        c.gridy = 0;
        //resizing does not work with default dialog components
//        c.weightx = 1;
//        c.fill = GridBagConstraints.HORIZONTAL;
        final SettingsModelString functionModel = settings.getFunctionModel();
        m_functionComponent = new DialogComponentString(functionModel, "Function: ", true, 47);
        m_functionComponent.setToolTipText("The place holder '" + CustomDBAggregationFuntionSettings.COLUMN_NAME
            + "' is replaced by the actual column name if present.");
        add(m_functionComponent.getComponentPanel(), c);
        c.gridx++;
//        c.weightx = 0;
//        c.fill = GridBagConstraints.NONE;
        final JButton resetButton = new JButton("Reset");
        resetButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent e) {
                functionModel.setStringValue(CustomDBAggregationFuntionSettings.DEFAULT_FUNCTION);
            }
        });
        add(resetButton, c);
        c.gridy++;
        c.gridx = 0;
        c.gridwidth = 2;
        m_resultColComponent =
                new DialogComponentString(settings.getResultColumnNameModel(), "Result column name: ", true, 40);
        add(m_resultColComponent.getComponentPanel(), c);
        c.gridwidth = 1;
        c.gridx = 0;
        c.gridy++;
        m_secondColumnComponent = new DialogComponentColumnNameSelection(settings.getSecondColumnModel(),
            "Optional second column: ", 0, false, true, DataValue.class);
        m_secondColumnComponent.setToolTipText("The place holder '"
            + CustomDBAggregationFuntionSettings.SECOND_COLUMN_NAME
            + "' is replaced by the actual column name if present.");
        add(m_secondColumnComponent.getComponentPanel(), c);
        c.gridx++;
        final JButton addPlaceHolderButton = new JButton("Add place holder");
        addPlaceHolderButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                final String stringValue = functionModel.getStringValue();
                final int idx = stringValue.lastIndexOf(")");
                final String newVal;
                if (idx < 0) {
                    newVal = stringValue + CustomDBAggregationFuntionSettings.SECOND_COLUMN_NAME;
                } else {
                    newVal = stringValue.substring(0, idx) + ", "
                        + CustomDBAggregationFuntionSettings.SECOND_COLUMN_NAME + stringValue.substring(idx);
                }
                functionModel.setStringValue(newVal);
            }
        });
        add(addPlaceHolderButton, c);
    }

    /**
     * Read value(s) of this dialog component from the configuration object.
     * This method will be called by the dialog pane only.
     *
     * @param settings the <code>NodeSettings</code> to read from
     * @param spec the input {@link DataTableSpec}
     * @throws NotConfigurableException If there is no chance for the dialog
     *             component to be valid (i.e. the settings are valid), e.g. if
     *             the given spec lacks some important columns or column types.
     */
    public void loadSettingsFrom(final NodeSettingsRO settings, final DataTableSpec spec)
            throws NotConfigurableException {
        final DataTableSpec[] specs = new DataTableSpec[] {spec};
        m_functionComponent.loadSettingsFrom(settings, specs);
        m_resultColComponent.loadSettingsFrom(settings, specs);
        m_secondColumnComponent.loadSettingsFrom(settings, specs);

    }
}
