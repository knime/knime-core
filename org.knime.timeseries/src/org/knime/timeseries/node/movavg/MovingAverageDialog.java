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
 *   January 13, 2007 (rosaria): created from String2Smileys
 */
package org.knime.timeseries.node.movavg;

//import java.util.LinkedList;
//import java.util.List;

//import javax.swing.JCheckBox;

import java.util.LinkedList;
import java.util.List;

import org.knime.core.data.DoubleValue;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnFilter;
import org.knime.core.node.defaultnodesettings.DialogComponentNumberEdit;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
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

    // private final JCheckBox m_AppendColumn = new JCheckBox();

    /** Constructor adding three components. */
    @SuppressWarnings("unchecked")
    public MovingAverageDialog() {

        LinkedList ll = new LinkedList();
        List<String> listAllowedWeightsFunctions = ll;
        listAllowedWeightsFunctions.add(MovingAverage.WEIGHT_FUNCTIONS.Simple
                .name());
        listAllowedWeightsFunctions
                .add(MovingAverage.WEIGHT_FUNCTIONS.Exponential.name());

        addDialogComponent(new DialogComponentStringSelection(
                createWeightModel(), "Type of Moving Average: ",
                listAllowedWeightsFunctions));

        addDialogComponent(new DialogComponentNumberEdit(
                createWindowLengthModel(),
                "Window Length (odd number of samples): "));
        
        addDialogComponent(new DialogComponentBoolean(
                createReplaceColumnModel(), "Replace columns"));

        addDialogComponent(new DialogComponentColumnFilter(
                createColumnNamesModel(), 0, false, DoubleValue.class));
    }

    /**
     * 
     * @return the settings model for the column name
     */
    static SettingsModelFilterString createColumnNamesModel() {
        return new SettingsModelFilterString("column_names");
    }

    /**
     * 
     * @return the model for the window length
     */
    static SettingsModelOddIntegerBounded createWindowLengthModel() {
        return new SettingsModelOddIntegerBounded("win_length", 21, 3, 1001);
    }

    /**
     * 
     * @return the model for the weight (simple or exponential)
     */
    static SettingsModelString createWeightModel() {
        return new SettingsModelString("weights",
                MovingAverage.WEIGHT_FUNCTIONS.Simple.name());
    }
    
    /**
     * 
     * @return model for the replace column checkbox
     */
    static SettingsModelBoolean createReplaceColumnModel() {
        return new SettingsModelBoolean("replace_column", false);
    }
}
