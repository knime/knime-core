/*
 * ------------------------------------------------------------------
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
 *   07.02.2007 (Rosaria Silipo): created
 */
package org.knime.timeseries.node.display.timeplot;

import org.knime.base.node.viz.plotter.DataProvider;
import org.knime.core.data.date.DateAndTimeValue;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponent;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;


/**
 * Lets the user define the maximum number of rows to be displayed
 * and the column to use to label the x-axis values.
 * 
 * @author Rosaria Silipo
 */
public class TimePlotNodeDialog extends DefaultNodeSettingsPane {
    

    /**
     * Creates a new default visualization dialog with the maximum number of 
     * rows to display as defined in 
     * {@link org.knime.base.node.viz.plotter.DataProvider#END}.
     * 
     */
    public TimePlotNodeDialog() {
        this(DataProvider.END);
    }
    
    /**
     * Creates a new default visualization dialog with the maximum number of 
     * rows to display as defined by the passed parameter.
     * 
     * @param defaultNrOfRows default value for the number of rows to display
     */
    @SuppressWarnings("unchecked")
    public TimePlotNodeDialog(final int defaultNrOfRows) {
        super();
        addDialogComponent(new DialogComponentNumber(
                new SettingsModelIntegerBounded(
                TimePlotNodeModel.CFG_END, defaultNrOfRows, 
                1, Integer.MAX_VALUE), "No. of rows to display:", 10));
        addDialogComponent(new DialogComponentBoolean(
                new SettingsModelBoolean(
                        TimePlotNodeModel.CFG_ANTIALIAS,false), 
                        "Enable Antialiasing"));  
        
        SettingsModelString columnName =
            new SettingsModelString(TimePlotNodeModel.CFG_COLUMN_NAME,
                    null);
        DialogComponent columnChooser =
            new DialogComponentColumnNameSelection(columnName,
                    "Columns containing Timestamp: ", 0, DateAndTimeValue.class);
        addDialogComponent(columnChooser);
    }

}
