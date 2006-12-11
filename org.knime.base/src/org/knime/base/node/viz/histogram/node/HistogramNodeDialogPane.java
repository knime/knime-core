/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2006
 * University of Konstanz, Germany.
 * Chair for Bioinformatics and Information Mining
 * Prof. Dr. Michael R. Berthold
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
 *   08.06.2006 (Tobias Koetter): created
 */
package org.knime.base.node.viz.histogram.node;

import org.knime.core.data.DataValue;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * The dialog of the {@link HistogramNodeModel} where the user can
 * define the x column and the number of rows. 
 * @author Tobias Koetter, University of Konstanz
 */
public class HistogramNodeDialogPane extends DefaultNodeSettingsPane {
    
    private static final String X_COL_SEL_LABEL = "X column:";
    
    /**
     * Constructor for class HistogramNodeDialogPane.
     * 
     */
    @SuppressWarnings("unchecked")
    protected HistogramNodeDialogPane() {
        super();
        addDialogComponent(new DialogComponentNumber(new SettingsModelInteger(
                HistogramNodeModel.CFGKEY_NO_OF_ROWS, 
                HistogramNodeModel.DEFAULT_NO_OF_ROWS),
                "No. of rows to display:", 1));
        
         addDialogComponent(new DialogComponentColumnNameSelection(
                 new SettingsModelString(
                 HistogramNodeModel.CFGKEY_X_COLNAME, ""),
                 HistogramNodeDialogPane.X_COL_SEL_LABEL, 0, 
                 DataValue.class));
    }
}
