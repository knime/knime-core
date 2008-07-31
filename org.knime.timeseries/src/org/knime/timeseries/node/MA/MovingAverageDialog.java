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
package org.knime.timeseries.node.MA;

//import java.util.LinkedList;
//import java.util.List;

//import javax.swing.JCheckBox;

import java.util.LinkedList;
import java.util.List;

import org.knime.core.data.DoubleValue;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponent;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnFilter;
import org.knime.core.node.defaultnodesettings.DialogComponentNumberEdit;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelFilterString;
import org.knime.core.node.defaultnodesettings.SettingsModelOddIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * This dialog lets the user choose the column that contains the string values
 * that should be converted into Smiles values. 
 * 
 * @author Rosaria Silipo
 */
public class MovingAverageDialog extends DefaultNodeSettingsPane {

  //  private final JCheckBox m_AppendColumn = new JCheckBox();

    /** Constructor adding three components. */
    @SuppressWarnings("unchecked")
    
    public MovingAverageDialog() {
        
        LinkedList ll = new LinkedList();
        List<String> listAllowedWeightsFunctions = ll;
        listAllowedWeightsFunctions.add(MovingAverage.WeightFunctions.SIMPLE);
        listAllowedWeightsFunctions.add(
                MovingAverage.WeightFunctions.EXPONENTIAL);
                 
        SettingsModelString weightsSettings =
            new SettingsModelString(MovingAverageNodeModel.CFG_WEIGHTS,
                    "simple");

        DialogComponent weightsChooser =
            new DialogComponentStringSelection(
                    weightsSettings,
                    "Type of Moving Average: ", 
                    listAllowedWeightsFunctions);
        addDialogComponent(weightsChooser);
                
        SettingsModelOddIntegerBounded winLength =
            new SettingsModelOddIntegerBounded(
                    MovingAverageNodeModel.CFG_WIN_LENGTH,
                    MovingAverageNodeModel.DEFAULT_ELEMENTS, 
                    MovingAverageNodeModel.MIN_ELEMENTS, 
                    MovingAverageNodeModel.MAX_ELEMENTS);
        
       DialogComponent editNumber = 
            new DialogComponentNumberEdit(winLength, 
                    "Window Length (odd number of samples): ");
        addDialogComponent(editNumber);
        
        SettingsModelFilterString columnNames =
            new SettingsModelFilterString(
                    MovingAverageNodeModel.CFG_COLUMN_NAMES);

        DialogComponent columnChooser =
                new DialogComponentColumnFilter(columnNames,
                        0, DoubleValue.class);
        addDialogComponent(columnChooser);
        
    }
}
