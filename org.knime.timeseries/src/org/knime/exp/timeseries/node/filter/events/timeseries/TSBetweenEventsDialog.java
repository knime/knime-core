/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
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
 *   January 24, 2007 (rosaria): created 
 */
package org.knime.exp.timeseries.node.filter.events.timeseries;

import org.knime.core.data.StringValue;
import org.knime.core.data.date.TimestampValue;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponent;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * This dialog lets the user choose the column that contains the string values
 * of the events. 
 * 
 * @author Rosaria Silipo
 */
public class TSBetweenEventsDialog extends DefaultNodeSettingsPane {

  //  private final JCheckBox m_AppendColumn = new JCheckBox();

    /** Constructor adding three components. */
    @SuppressWarnings("unchecked")
    
    public TSBetweenEventsDialog() {
                
        SettingsModelString columnName =
            new SettingsModelString(TSBetweenEventsNodeModel.CFG_COLUMN_NAME,
                    null);

        DialogComponent columnChooser =
            new DialogComponentColumnNameSelection(columnName,
                    "Columns containing Events: ", 0, StringValue.class);
        addDialogComponent(columnChooser);

        SettingsModelString eventName =
            new SettingsModelString(
                    TSBetweenEventsNodeModel.CFG_EVENT_NAME,
                    null);
       DialogComponent editEventName = 
            new DialogComponentString(eventName,  "event name");
        addDialogComponent(editEventName);
        
        SettingsModelString timestampOutput =
            new SettingsModelString(
                    TSBetweenEventsNodeModel.CFG_TIMESTAMP_OUTPUT,
                    "yyyy-MM-dd;HH:mm:ss.S");
       DialogComponent editTimestampOutput = 
            new DialogComponentString(timestampOutput,  
                    "Insert date for output table: ");
        addDialogComponent(editTimestampOutput);
        
/*        SettingsModelString dateFormat =
            new SettingsModelString(
                    TSBetweenEventsNodeModel.CFG_DATE_FORMAT,
                    "yyyy-MM-dd;HH:mm:ss.S");
       DialogComponent editDateFormat = 
            new DialogComponentString(dateFormat,  
                    "with timestamp format: ");
        addDialogComponent(editDateFormat);
        */

        SettingsModelString timeStampColumnName =
            new SettingsModelString(
                    TSBetweenEventsNodeModel.CFG_TIMESTAMP_COLUMN_NAME,
                    null);
       DialogComponent timeStampColumnChooser = 
            new DialogComponentColumnNameSelection(timeStampColumnName,  
                    "from column ", 0, TimestampValue.class);
        addDialogComponent(timeStampColumnChooser);

    }
}
