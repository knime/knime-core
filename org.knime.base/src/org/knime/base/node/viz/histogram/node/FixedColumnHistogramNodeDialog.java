/* -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 * 
 * Copyright, 2003 - 2006
 * Universitaet Konstanz, Germany.
 * Lehrstuhl fuer Angewandte Informatik
 * Prof. Dr. Michael R. Berthold
 * 
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner.
 * -------------------------------------------------------------------
 * 
 * History
 *   02.05.2006 (koetter): created
 */
package org.knime.base.node.viz.histogram.node;

import org.knime.core.data.DataValue;
import org.knime.core.data.DoubleValue;
import org.knime.core.node.defaultnodedialog.DefaultNodeDialogPane;
import org.knime.core.node.defaultnodedialog.DialogComponentColumnSelection;
import org.knime.core.node.defaultnodedialog.DialogComponentNumber;

/**
 * <code>NodeDialog</code> for the "BayesianClassifier" Node.
 * This is the description of the Bayesian classifier
 * 
 * @author Tobias Koetter
 */
public class FixedColumnHistogramNodeDialog extends DefaultNodeDialogPane {
    
    private static final String X_COL_SEL_LABEL = "X column:";

    private static final String AGGR_COL_SEL_LABEL = "Aggregation column:";
    
    private final DialogComponentColumnSelection m_xColComp;
    
    private final DialogComponentColumnSelection m_aggrColComp;
    
    /**
     * New pane for configuring BayesianClassifier node dialog.
     */
    @SuppressWarnings("unchecked")
    public FixedColumnHistogramNodeDialog() {
        super();
        addDialogComponent(new DialogComponentNumber(
                FixedColumnHistogramNodeModel.CFGKEY_NO_OF_ROWS,
                "No. of rows to display:", 1, Integer.MAX_VALUE, 
                FixedColumnHistogramNodeModel.DEFAULT_NO_OF_ROWS));
        
         m_xColComp = new DialogComponentColumnSelection(
                 FixedColumnHistogramNodeModel.CFGKEY_X_COLNAME,
                 FixedColumnHistogramNodeDialog.X_COL_SEL_LABEL, 0, 
                 DataValue.class);
         addDialogComponent(m_xColComp);
         
         m_aggrColComp = new DialogComponentColumnSelection(
                 FixedColumnHistogramNodeModel.CFGKEY_AGGR_COLNAME,
                 FixedColumnHistogramNodeDialog.AGGR_COL_SEL_LABEL, 0, 
                 DoubleValue.class);
         addDialogComponent(m_aggrColComp);
         
    }
}
