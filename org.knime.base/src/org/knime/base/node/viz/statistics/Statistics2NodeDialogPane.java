/* 
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2007
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
 * --------------------------------------------------------------------- *
 *
 */
package org.knime.base.node.viz.statistics;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnFilter;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelFilterString;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;

/**
 * Node dialog for the Statistics node.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public class Statistics2NodeDialogPane extends DefaultNodeSettingsPane {
	
	private final SettingsModelFilterString m_filterModel;
    
    /** Default constructor. */
    Statistics2NodeDialogPane() {
        addDialogComponent(new DialogComponentBoolean(
                createMedianModel(), 
                "Calculate median values (computationally expensive)"));
        createNewGroup("Nominal values");
        m_filterModel = createNominalFilterModel();
        addDialogComponent(new DialogComponentColumnFilter(m_filterModel, 0));
        DialogComponentNumber numNomValueComp = 
            new DialogComponentNumber(createNominalValuesModel(), 
             "Max no. of most frequent and infrequent values (in view): ", 5);
        numNomValueComp.setToolTipText(
             "Max no. of most frequent and infrequent "
                + "values per column displayed in the node view.");
        addDialogComponent(numNomValueComp);
        DialogComponentNumber numNomValueCompOutput = 
            new DialogComponentNumber(createNominalValuesModelOutput(), 
                "Max no. of possible values per column (in output table): ", 5);
        addDialogComponent(numNomValueCompOutput);
    }
    
    /**
     * @return create nominal filter model
     */
    static SettingsModelFilterString createNominalFilterModel() {
        return new SettingsModelFilterString("filter_nominal_columns");
    }
    
    /**
     * @return boolean model to compute median 
     */
    static SettingsModelBoolean createMedianModel() {
        return new SettingsModelBoolean("compute_median", false);
    }
    
    /**
     * @return int model to restrict number of nominal values
     */
    static SettingsModelIntegerBounded createNominalValuesModel() {
        return new SettingsModelIntegerBounded(
                "num_nominal-values", 20, 0, Integer.MAX_VALUE);
    }
    
    /**
     * @return int model to restrict number of nominal values for the output
     */
    static SettingsModelIntegerBounded createNominalValuesModelOutput() {
        return new SettingsModelIntegerBounded(
                "num_nominal-values_output", 1000, 0, Integer.MAX_VALUE);
    }
}
