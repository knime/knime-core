/* -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 * 
 * Copyright, 2003 - 2006
 * Universitaet Konstanz, Germany.
 * Lehrstuhl fuer Angewandte Informatik
 * Prof. Dr. Michael R. Berthold
 * 
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner.
 * -------------------------------------------------------------------
 * 
 * History
 *   05.06.2009 (Fabian Dill): created
 */
package org.knime.timeseries.node.generator;

import java.util.Calendar;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.timeseries.util.DialogComponentCalendar;
import org.knime.timeseries.util.SettingsModelCalendar;

/**
 * @author Fabian Dill
 *
 */
public class DateGeneratorNodeDialog extends DefaultNodeSettingsPane {

    /**
     * 
     */
    public DateGeneratorNodeDialog() {
       addDialogComponent(new DialogComponentNumber(
               createNumberOfRowsModel(), "Number of rows:", 10));
       addDialogComponent(new DialogComponentCalendar(
               createStartingPointModel(), "Starting point:"));
       addDialogComponent(new DialogComponentCalendar(
               createEndPointModel(), "End point:"));
    }
    
    
    /**
     * 
     * @return the model for the number of rows to be generated
     */
    static SettingsModelInteger createNumberOfRowsModel() {
        return new SettingsModelIntegerBounded(
                "number-of-generated-rows",
                1000, 1, Integer.MAX_VALUE);
    }

    /**
     * 
     * @return the calendar model for the starting point
     */
    static SettingsModelCalendar createStartingPointModel() {
        Calendar c = Calendar.getInstance();
        c.roll(Calendar.YEAR, false);
        return new SettingsModelCalendar("starting-point", c);
    }

    /**
     * 
     * @return the calendar model for the end point
     */
    static SettingsModelCalendar createEndPointModel() {
        return new SettingsModelCalendar("end-point", null);
    }
    
}
