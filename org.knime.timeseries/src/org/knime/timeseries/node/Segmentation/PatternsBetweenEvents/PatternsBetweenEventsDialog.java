/* 
 * -------------------------------------------------------------------
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
 * -------------------------------------------------------------------
 * 
 * History
 *   January 26, 2007 (rosaria): created 
 */
package org.knime.timeseries.node.Segmentation.PatternsBetweenEvents;

import org.knime.core.data.DoubleValue;
import org.knime.core.data.StringValue;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponent;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * This dialog lets the user choose the column that contains the string values
 * of the events. 
 * 
 * @author Rosaria Silipo
 */
public class PatternsBetweenEventsDialog extends DefaultNodeSettingsPane {

    /** Constructor adding three components. */
    @SuppressWarnings("unchecked")
    
    public PatternsBetweenEventsDialog() {
           
        // choose column name with events
        SettingsModelString columnName =
            new SettingsModelString(
                    PatternsBetweenEventsNodeModel.CFG_COLUMN_NAME_EVENTS,
                    null);

        DialogComponent columnChooser =
            new DialogComponentColumnNameSelection(columnName,
                    "Columns containing Events: ", 0, StringValue.class);
        addDialogComponent(columnChooser);

        // choose column name to use as output
        SettingsModelString columnName1 =
            new SettingsModelString(
                    PatternsBetweenEventsNodeModel.CFG_COLUMN_NAME_OUTPUT,
                    null);

        DialogComponent columnChooser1 =
            new DialogComponentColumnNameSelection(columnName1,
                    "Select column to generate output: ", 
                    0, DoubleValue.class);
        addDialogComponent(columnChooser1);

        // checkbox for "all columns"
        SettingsModelBoolean allColumns =
            new SettingsModelBoolean(
                    PatternsBetweenEventsNodeModel.CFG_ALL_COLUMNS,
                    false);

        DialogComponent allColumnsCheckBox =
            new DialogComponentBoolean(allColumns,
                    "All columns:");
        addDialogComponent(allColumnsCheckBox);

        // set event name
        SettingsModelString eventName =
            new SettingsModelString(
                    PatternsBetweenEventsNodeModel.CFG_EVENT_NAME,
                    null);
       DialogComponent editEventName = 
            new DialogComponentString(eventName,  "event name");
        addDialogComponent(editEventName);
    }
}
