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
package org.knime.timeseries.node.filter.sample;

import java.util.Calendar;

import org.knime.core.data.date.TimestampCell;
import org.knime.core.data.date.TimestampValue;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponent;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.timeseries.util.DialogComponentTime;
import org.knime.timeseries.util.SettingsModelCalendar;

/**
 * This dialog lets the user choose the column that contains the string values
 * that should be converted into Smiles values. 
 * 
 * @author Rosaria Silipo
 */
public class OneSampleperDayDialog extends DefaultNodeSettingsPane {

  //  private final JCheckBox m_AppendColumn = new JCheckBox();

    /** Constructor adding three components. */
    @SuppressWarnings("unchecked")
    
    public OneSampleperDayDialog() {
        DialogComponent columnChooser =
            new DialogComponentColumnNameSelection(createColModel(),
                    "Columns containing Timestamp: ", 0, TimestampValue.class);
        
        addDialogComponent(columnChooser);

       DialogComponentTime editTimestampAt = 
            new DialogComponentTime(createTimeModel(),  
                    "Time of day (to draw the sample): ");
        addDialogComponent(editTimestampAt);
        
    }
    
    static SettingsModelCalendar createTimeModel() {
        Calendar cal = TimestampCell.getUTCCalendar();
        cal = TimestampCell.resetDateFields(cal);
        return new SettingsModelCalendar("OneSamplePerDay.time", cal, false, 
                true);
    }
    
    static SettingsModelString createColModel() {
        return new SettingsModelString("column_name", null); 
    }
}
