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
import org.knime.core.node.defaultnodedialog.DefaultNodeDialogPane;
import org.knime.core.node.defaultnodedialog.DialogComponentColumnSelection;
import org.knime.core.node.defaultnodedialog.DialogComponentNumber;

/**
 * The dialog of the {@link HistogramNodeModel} where the user can
 * define the x column and the number of rows. 
 * @author Tobias Koetter, University of Konstanz
 */
public class HistogramNodeDialogPane extends DefaultNodeDialogPane {
    /**
     * Constructor for class HistogramNodeDialogPane.
     * 
     */
    @SuppressWarnings("unchecked")
    protected HistogramNodeDialogPane() {
        super();
        addDialogComponent(new DialogComponentNumber(
                HistogramNodeModel.CFGKEY_NO_OF_ROWS,
                "No. of rows to display:", 1, Integer.MAX_VALUE, 
                HistogramNodeModel.DEFAULT_NO_OF_ROWS));
        
        DialogComponentColumnSelection colSel = 
            new DialogComponentColumnSelection(
                HistogramNodeModel.CFGKEY_X_COLNAME,
                "Select the xcolumn:", 0, DataValue.class);
        addDialogComponent(colSel);

    }
}
