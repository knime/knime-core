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
 *   20.09.2006 (Fabian Dill): created
 */
package org.knime.exp.node.view.plotter.node;

import org.knime.core.node.defaultnodedialog.DefaultNodeDialogPane;
import org.knime.core.node.defaultnodedialog.DialogComponentNumber;
import org.knime.exp.node.view.plotter.DataProvider;

/**
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class DefaultVisualizationNodeDialog extends DefaultNodeDialogPane {
    
    private final int m_fromMIN = 1;

    private final int m_fromSTART = DataProvider.START;

    private final int m_cntMIN = 1;

    private final int m_cntSTART = DataProvider.END;

    /**
     * Creates a new Default visualization dialog with start row and last row
     * to display.
     *
     * 
     */
    public DefaultVisualizationNodeDialog() {
        super();

        addDialogComponent(new DialogComponentNumber(
                DefaultVisualizationNodeModel.CFG_START, 
                "First row to display:",
                m_fromMIN, Integer.MAX_VALUE, m_fromSTART));
        addDialogComponent(new DialogComponentNumber(
                DefaultVisualizationNodeModel.CFG_END,
                "No. of rows to display:", m_cntMIN, Integer.MAX_VALUE,
                m_cntSTART));
    }

}
