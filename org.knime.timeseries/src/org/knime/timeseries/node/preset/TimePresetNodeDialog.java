/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2010
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
 * ------------------------------------------------------------------------
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
