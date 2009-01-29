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
package org.knime.timeseries.node.Segmentation.OneSampleperDay;

import org.knime.core.data.TimestampValue;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponent;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

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
                
        SettingsModelString columnName =
            new SettingsModelString(OneSampleperDayNodeModel.CFG_COLUMN_NAME,
                    null);

        DialogComponent columnChooser =
            new DialogComponentColumnNameSelection(columnName,
                    "Columns containing Timestamp: ", 0, TimestampValue.class);
        addDialogComponent(columnChooser);

        SettingsModelString timestampAt =
            new SettingsModelString(
                    OneSampleperDayNodeModel.CFG_TIMESTAMP_AT,
                    OneSampleperDayNodeModel.DATE_FORMAT);
       DialogComponent editTimestampAt = 
            new DialogComponentString(timestampAt,  
                    "AT (change default time of day): ");
        addDialogComponent(editTimestampAt);
        
    }
}
