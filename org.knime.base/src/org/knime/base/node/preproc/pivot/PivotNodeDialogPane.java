/* 
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * --------------------------------------------------------------------- *
 * 
 * History
 *   03.05.2007 (gabriel): created
 */
package org.knime.base.node.preproc.pivot;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.data.DataValue;
import org.knime.core.data.DoubleValue;
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
        DialogComponentColumnNameSelection aggregation = 
            new DialogComponentColumnNameSelection(aggModel, 
                    "Aggregation column: ", 0, DoubleValue.class);
        
        final SettingsModelString aggMethodModel = 
            createSettingsAggregationMethod();
        DialogComponentStringSelection aggMethod =
            new DialogComponentStringSelection(
                    aggMethodModel, "Aggregation method: ",
                    PivotAggregationMethod.METHODS.keySet());
        final SettingsModelString aggMakeModel = 
            createSettingsMakeAggregation();
        DialogComponentButtonGroup aggCheck = new DialogComponentButtonGroup(
                aggMakeModel, true, MAKE_AGGREGATION[0], MAKE_AGGREGATION);
        aggMakeModel.addChangeListener(new ChangeListener() {
            /**
             * {@inheritDoc}
             */
            public void stateChanged(final ChangeEvent e) {
                boolean b = 
                    aggMakeModel.getStringValue().equals(MAKE_AGGREGATION[1]);
                aggModel.setEnabled(b);
                aggMethodModel.setEnabled(b);
            }
        });

        // add components
        super.createNewGroup(" Pivot (column header) ");
        addDialogComponent(pivot);
        super.createNewGroup(" Group (row header) ");
        addDialogComponent(group);
        super.createNewGroup(" Aggregation (table content) ");
        addDialogComponent(aggCheck);
        addDialogComponent(aggregation);
        addDialogComponent(aggMethod);
        super.createNewGroup(" Advance ");
        addDialogComponent(new DialogComponentBoolean(
                createSettingsEnableHiLite(), 
                "Enable hiliting"));
        addDialogComponent(new DialogComponentBoolean(
                createSettingsMissingValues(), 
                "Ignore missing values"));
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
        return new SettingsModelBoolean("enable_hiliting", true);
    }

}
