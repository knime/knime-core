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
 *   January 13, 2007 (rosaria): created from String2Smileys
 */
package org.knime.timeseries.node.FilterWindows;

//import java.util.LinkedList;
//import java.util.List;

//import javax.swing.JCheckBox;

import java.util.List;
import java.util.LinkedList;

import org.knime.core.data.DoubleValue;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponent;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentNumberEdit;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * This dialog lets the user choose the column that contains the string values
 * that should be converted into Smiles values. 
 * 
 * @author Rosaria Silipo
 */
public class FilterWindowsDialog extends DefaultNodeSettingsPane {

  //  private final JCheckBox m_AppendColumn = new JCheckBox();

    /** Constructor adding three components. */
    @SuppressWarnings("unchecked")
    
    public FilterWindowsDialog() {
        
        SettingsModelString columnName =
                new SettingsModelString(
                        FilterWindowsNodeModel.CFG_COLUMN_NAME,
                        null);

        DialogComponent columnChooser =
                new DialogComponentColumnNameSelection(columnName,
                        "Columns containing double values: ", 
                        0, DoubleValue.class);
        addDialogComponent(columnChooser);
        
        LinkedList ll = new LinkedList();
        List<String> listAllowedWeightsFunctions = ll;
        listAllowedWeightsFunctions.add(FilterWindows.WeightFunctions.NONE);
        listAllowedWeightsFunctions.add(FilterWindows.WeightFunctions.GAUSSIAN);
        listAllowedWeightsFunctions.add(FilterWindows.WeightFunctions.HAMMING);
                
        SettingsModelString weightsSettings =
            new SettingsModelString(FilterWindowsNodeModel.CFG_WEIGHTS,
                    "none");

        DialogComponent weightsChooser =
            new DialogComponentStringSelection(weightsSettings,
                    "Weight function: ", listAllowedWeightsFunctions);
        addDialogComponent(weightsChooser);
                
        SettingsModelIntegerBounded winLength =
            new SettingsModelIntegerBounded(
                    FilterWindowsNodeModel.CFG_WIN_LENGTH,
                    21, 3, 101);
        
       DialogComponent editNumber = 
            new DialogComponentNumberEdit(winLength, 
                    "Window Length: ");
        addDialogComponent(editNumber);
    }
}
