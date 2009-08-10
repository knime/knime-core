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
package org.knime.timeseries.node.Segmentation.ExtractFromTo;

import org.knime.core.data.TimestampValue;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponent;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.timeseries.util.DialogComponentCalendar;
import org.knime.timeseries.util.SettingsModelCalendar;

/**
 * This dialog lets the user choose the column that contains the string values
 * that should be converted into Smiles values. 
 * 
 * @author Rosaria Silipo
 * @author Fabian Dill, KNIME.com GmbH
 */
public class ExtractFromToDialog extends DefaultNodeSettingsPane {

    /** Constructor adding three components. */
    @SuppressWarnings("unchecked")
    
    public ExtractFromToDialog() {
        DialogComponent columnChooser =
            new DialogComponentColumnNameSelection(createColumnNameModel(),
                    "Columns containing Timestamp: ", 0, TimestampValue.class);
        addDialogComponent(columnChooser);
        addDialogComponent(new DialogComponentCalendar(createFromModel(), 
                "Select starting point:"));
        addDialogComponent(new DialogComponentCalendar(
                createToModel(), "Select end point:"));
    }
    
    /**
     * 
     * @return settings model to store the selected column
     */
    static SettingsModelString createColumnNameModel() {
        return new SettingsModelString("column_name", null); 
    }
    
    /** @return settings model for the "from" date.*/
    static SettingsModelCalendar createFromModel() {
        return new SettingsModelCalendar("timestamp_from", null);
    }
    /** @return settings model for the "to" date.*/
    static SettingsModelCalendar createToModel() {
        return new SettingsModelCalendar("timestamp_to", null);
    }
    
}
