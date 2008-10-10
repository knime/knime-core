/*
 * ------------------------------------------------------------------
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
 *   20.09.2006 (Fabian Dill): created
 */
package org.knime.base.node.viz.plotter.node;

import org.knime.base.node.viz.plotter.DataProvider;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;


/**
 * Lets the user define the maximum number of rows to be displayed. 
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class DefaultVisualizationNodeDialog extends DefaultNodeSettingsPane {
    

    /**
     * Creates a new default visualization dialog with the maximum number of 
     * rows to display as defined in 
     * {@link org.knime.base.node.viz.plotter.DataProvider#END}.
     * 
     */
    public DefaultVisualizationNodeDialog() {
        this(DataProvider.END);
    }
    
    /**
     * Creates a new default visualization dialog with the maximum number of 
     * rows to display as defined by the passed parameter.
     * 
     * @param defaultNrOfRows default value for the number of rows to display
     */
    public DefaultVisualizationNodeDialog(final int defaultNrOfRows) {
        super();
        addDialogComponent(new DialogComponentNumber(
                new SettingsModelIntegerBounded(
                DefaultVisualizationNodeModel.CFG_END, defaultNrOfRows, 
                1, Integer.MAX_VALUE), "No. of rows to display:", 10));
        /*
         * moved to AbstractPlotterProperties
         *
        addDialogComponent(new DialogComponentBoolean(
                new SettingsModelBoolean(
                        DefaultVisualizationNodeModel.CFG_ANTIALIAS,false), 
                        "Enable Antialiasing"));
                        */        
    }

}
