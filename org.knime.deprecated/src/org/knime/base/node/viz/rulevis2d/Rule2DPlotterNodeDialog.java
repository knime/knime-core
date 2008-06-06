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
 *   30.06.2006 (Fabian Dill): created
 */
package org.knime.base.node.viz.rulevis2d;

import org.knime.base.node.viz.scatterplot.ScatterPlotNodeDialog;
import org.knime.core.node.defaultnodedialog.DialogComponent;
import org.knime.core.node.defaultnodedialog.DialogComponentNumber;


/**
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class Rule2DPlotterNodeDialog extends ScatterPlotNodeDialog {
    
    private static final int START_ROW = 1;
    private static final int MAX_ROW = 1000;
    
    
    /**
     * Creates a Dialog just like the scatter plotter but with a start row
     * and a maximum nuber of rows for the ruiles, too.
     *
     */
    public Rule2DPlotterNodeDialog() {
        super();
        DialogComponent startRow = new DialogComponentNumber(
                Rule2DNodeModel.CFG_RULES_START_ROW, 
                "First rule row to display:",
                START_ROW, MAX_ROW, START_ROW);
        addDialogComponent(startRow);
        DialogComponent maxRow = new DialogComponentNumber(
                Rule2DNodeModel.CFG_RULES_MAX_ROWS, 
                "Max. number of displayed rules:",
                START_ROW, MAX_ROW, MAX_ROW);
        addDialogComponent(maxRow);
    }

}
