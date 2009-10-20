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
 * --------------------------------------------------------------------- *
 * 
 */
package org.knime.base.node.io.database;

import java.util.Arrays;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JPanel;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelFilterString;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.database.DatabasePortObjectSpec;

/**
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
final class DBRowFilterNodeDialogPane extends NodeDialogPane {

    @SuppressWarnings("unchecked")
    private final DialogComponentColumnNameSelection m_column
        = new DialogComponentColumnNameSelection(createColumnModel(),
                "", 0, DataValue.class);
    
    private final DialogComponentStringSelection m_operator
        = new DialogComponentStringSelection(createOperatorModel(), 
                "", Arrays.asList("=", "<>", "!=", ">", "<", ">=", "<=", 
                        "BETWEEN", "LIKE", "IN", "IS NULL", "IS NOT NULL"));
    
    private final DialogComponentString m_value = new DialogComponentString(
            createValueModel(), "");
    
    /**
     * Create query dialog with text box to enter table name.
     */
    DBRowFilterNodeDialogPane() {
        Box optionPanel = Box.createVerticalBox();
        JPanel column = m_column.getComponentPanel();
        column.setBorder(BorderFactory.createTitledBorder(" Column "));
        optionPanel.add(column);
        JPanel operator = m_operator.getComponentPanel();
        operator.setBorder(BorderFactory.createTitledBorder(" Operator "));
        optionPanel.add(operator);
        JPanel value = m_value.getComponentPanel();
        value.setBorder(BorderFactory.createTitledBorder(" Value "));
        optionPanel.add(value);
        super.addTab("Row Filter", optionPanel);  
    }
    
    /**
     * @return string settings model for operator selection
     */
    static final SettingsModelString createOperatorModel() {
        return new SettingsModelString("operator", null);
    }

    /**
     * @return string settings model for column selection
     */
    static final SettingsModelString createColumnModel() {
        return new SettingsModelString("column", null);
    }
    
    /**
     * @return string settings model for the filter value
     */
    static final SettingsModelString createValueModel() {
        return new SettingsModelString("value", "");
    }
        
    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final PortObjectSpec[] ports) throws NotConfigurableException {
        DatabasePortObjectSpec dbSpec = (DatabasePortObjectSpec) ports[0];
        final DataTableSpec[] specs; 
        if (dbSpec == null) {
            specs = new DataTableSpec[]{null};
        } else {
            specs = new DataTableSpec[]{dbSpec.getDataTableSpec()};
        }
        m_column.loadSettingsFrom(settings, specs);
        m_operator.loadSettingsFrom(settings, specs);
        m_value.loadSettingsFrom(settings, specs);
    }
    
    /**
     * @return new settings model for column filter
     */
    static final SettingsModelFilterString createColumnFilterModel() {
        return new SettingsModelFilterString("column_filter");
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        m_column.saveSettingsTo(settings);
        m_operator.saveSettingsTo(settings);
//        if (m_column.getSelectedAsSpec().getType().isCompatible(
//                StringValue.class)) {
//            SettingsModelString value = 
//                (SettingsModelString) m_value.getModel();
//            String str = value.getStringValue();
//            if (str.length() > 0) {
//                if (str.charAt(0) != (char) '\'') {
//                    str = '\'' + str;
//                }
//                if (str.charAt(str.length() - 1) != (char) '\'') {
//                    str += '\'';
//                }
//                if (value.getStringValue().length() < str.length()) {
//                    value.setStringValue(str);
//                }
//            } else {
//                value.setStringValue("''");
//            }
//        }
        m_value.saveSettingsTo(settings);
    }
}
