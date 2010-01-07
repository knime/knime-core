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
 * History
 *   03.05.2007 (gabriel): created
 */
package org.knime.base.node.preproc.pivot;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataValue;
import org.knime.core.data.DoubleValue;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentButtonGroup;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * Pivot dialog pane used to selected the pivot and group column, and 
 * optional an aggregation column.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public class PivotNodeDialogPane extends DefaultNodeSettingsPane {
    
    /**
     * Defines the labels for the radio buttons, count occurrences and
     * enable aggregation.
     */
    static final String[] MAKE_AGGREGATION = new String[]
            {"Count co-occurrences", "Enable aggregation"};
    
    private final DialogComponentColumnNameSelection m_aggregation;
    private final DialogComponentStringSelection m_aggMethod;
    private final DialogComponentButtonGroup m_aggCheck;
    
    /**
     * Creates a new pivot dialog pane with two column selection boxes, one
     * for the group column - used as row ID - and one as the pivot column -
     * used in the column header, and (optional) an aggregation column. 
     */
    @SuppressWarnings("unchecked")
    PivotNodeDialogPane() {
        DialogComponentColumnNameSelection pivot = 
            new DialogComponentColumnNameSelection(createSettingsPivot(), 
                    "Pivot column: ", 0, DataValue.class);
        DialogComponentColumnNameSelection group = 
            new DialogComponentColumnNameSelection(createSettingsGroup(), 
                    "Group column: ", 0, DataValue.class);
        
        final SettingsModelString aggModel = createSettingsAggregation();
        m_aggregation = new DialogComponentColumnNameSelection(aggModel, 
                    "Aggregation column: ", 0, false, DoubleValue.class);
        
        final SettingsModelString aggMethodModel = 
            createSettingsAggregationMethod();
        m_aggMethod = new DialogComponentStringSelection(
                    aggMethodModel, "Aggregation method: ",
                    PivotAggregationMethod.METHODS.keySet());
        final SettingsModelString aggMakeModel = 
            createSettingsMakeAggregation();
        m_aggCheck = new DialogComponentButtonGroup(
                aggMakeModel, true, MAKE_AGGREGATION[0], MAKE_AGGREGATION);
        aggMakeModel.addChangeListener(new ChangeListener() {
            /** {@inheritDoc} */
            public void stateChanged(final ChangeEvent e) {
                if (aggMakeModel.isEnabled()) {
                    boolean b = aggMakeModel.getStringValue().equals(
                            MAKE_AGGREGATION[1]);
                    aggModel.setEnabled(b);
                    aggMethodModel.setEnabled(b);
                } else {
                    aggMakeModel.setStringValue(MAKE_AGGREGATION[0]);
                }
            }
        });

        // add components
        super.createNewGroup(" Pivot (column header) ");
        addDialogComponent(pivot);
        super.createNewGroup(" Group (row header) ");
        addDialogComponent(group);
        super.createNewGroup(" Aggregation (table content) ");
        addDialogComponent(m_aggCheck);
        addDialogComponent(m_aggregation);
        addDialogComponent(m_aggMethod);
        super.createNewGroup(" Advance ");
        addDialogComponent(new DialogComponentBoolean(
                createSettingsEnableHiLite(), "Enable hiliting"));
        final DialogComponentBoolean missComponent = new DialogComponentBoolean(
                createSettingsMissingValues(), "Ignore missing values");
        missComponent.setToolTipText("Ignore rows "
            + "containing missing values in pivot column.");
        addDialogComponent(missComponent);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void loadAdditionalSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) throws NotConfigurableException {
        super.loadAdditionalSettingsFrom(settings, specs);
        boolean enable = (m_aggregation.getSelected() != null);
        m_aggCheck.getModel().setEnabled(enable);
    }
    
    /**
     * @return settings model boolean for ignoring missing values
     */
    static final SettingsModelBoolean createSettingsMissingValues() {
        return new SettingsModelBoolean("missing_values", true);
    }
    
    /**
     * @return a settings model string as group column
     */
    static final SettingsModelString createSettingsGroup() {
        return new SettingsModelString("group_column", null);
    }
    
    /**
     * @return a settings model string as pivot column
     */
    static final SettingsModelString createSettingsPivot() {
        return new SettingsModelString("pivot_column", null);
    }
    
    /**
     * @return a settings model string as pivot column
     */
    static final SettingsModelString createSettingsAggregation() {
        SettingsModelString model = 
            new SettingsModelString("aggregation_column", null);
        model.setEnabled(false);
        return model;
    }

    /**
     * @return a settings model string as aggregation method
     */
    static final SettingsModelString createSettingsAggregationMethod() {
        SettingsModelString model =
           new SettingsModelString("aggregation_method", "SUM");
        model.setEnabled(false);
        return model;
    }

    /**
     * @return a settings model boolean for aggregation method on/off
     */
    static final SettingsModelString createSettingsMakeAggregation() {
        return new SettingsModelString("make_aggregation", MAKE_AGGREGATION[0]);
    }
    
    /**
     * @return a settings model boolean to enable/disable hiliting
     */
    static final SettingsModelBoolean createSettingsEnableHiLite() {
        return new SettingsModelBoolean("enable_hiliting", false);
    }

}
