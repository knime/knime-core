/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2008 - 2009
 * KNIME.com, Zurich, Switzerland
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.com
 * email: contact@knime.com
 * ---------------------------------------------------------------------
 * 
 * History
 *   28.09.2009 (Fabian Dill): created
 */
package org.knime.timeseries.node.preset;

import java.util.Calendar;

import org.knime.core.data.date.DateAndTimeCell;
import org.knime.core.data.date.DateAndTimeValue;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.timeseries.util.DialogComponentCalendar;
import org.knime.timeseries.util.SettingsModelCalendar;

/**
 * Dialog to select the column containing the {@link DateAndTimeValue}s that
 * should be preset and choose a default date/time that should be set where no 
 * date/time is available.
 * 
 * @author Fabian Dill, KNIME.com, Zurich, Switzerland
 */
public class TimePresetNodeDialog extends DefaultNodeSettingsPane {
    
    /**
     * 
     * @return the calendar model representing the date and time to preset
     */
    static SettingsModelCalendar createCalendarModel() {
        return new SettingsModelCalendar("time.preset.calendar", 
                Calendar.getInstance(DateAndTimeCell.UTC_TIMEZONE), 
                true, false);
    }
    
    /**
     * 
     * @return the model for the selected column
     */
    static SettingsModelString createColumnSelectionModel() {
        return new SettingsModelString("time.preset.selected.column", "");
    }
    
    /**
     * 
     * @return the settings model for the replace missing value checkbox  
     */
    static SettingsModelBoolean createReplaceMissingValuesModel() {
        return new SettingsModelBoolean("time.preset.replace.missingVals", 
                false);
    }
    
    
    /**
     * 
     */
    public TimePresetNodeDialog() {
        addDialogComponent(new DialogComponentColumnNameSelection(
                createColumnSelectionModel(), "Select time column", 
                0, DateAndTimeValue.class));
        addDialogComponent(new DialogComponentCalendar(
                createCalendarModel(), "Configure the date/time to preset"));
        addDialogComponent(new DialogComponentBoolean(
                createReplaceMissingValuesModel(), 
                "Replace missing values with preset date/time?"));
    }

}
