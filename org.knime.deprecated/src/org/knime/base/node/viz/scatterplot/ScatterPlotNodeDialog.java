/*
 * 
 * --------------------------------------------------------------------- *
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
 * --------------------------------------------------------------------- *
 */
package org.knime.base.node.viz.scatterplot;

import org.knime.core.node.defaultnodedialog.DefaultNodeDialogPane;
import org.knime.core.node.defaultnodedialog.DialogComponentNumber;

/**
 * The dialog for the Scatterplotter. The user must set the things that can't be
 * changed after execution, like the X- and Y-column, and the number of rows to
 * display.
 * 
 * @author ohl, University of Konstanz
 */
public class ScatterPlotNodeDialog extends DefaultNodeDialogPane {

    private final int m_fromMIN = 1;

    private final int m_fromSTART = 1;

    private final int m_cntMIN = 1;

    private final int m_cntSTART = 1000;

    /**
     * Creates a new Scatter Plot dialog.
     */
    public ScatterPlotNodeDialog() {
        super();

        addDialogComponent(new DialogComponentNumber(
                ScatterPlotNodeModel.CFGKEY_FROMROW, "First row to display:",
                m_fromMIN, Integer.MAX_VALUE, m_fromSTART));
        addDialogComponent(new DialogComponentNumber(
                ScatterPlotNodeModel.CFGKEY_ROWCNT,
                "No. of rows to display:", m_cntMIN, Integer.MAX_VALUE,
                m_cntSTART));
    }
}
