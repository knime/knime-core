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
 * -------------------------------------------------------------------
 *
 * History
 *   08.06.2006 (Tobias Koetter): created
 */
package org.knime.base.node.viz.pie.node.interactive;

import java.awt.Color;
import java.io.File;

import org.knime.base.node.viz.pie.datamodel.interactive.InteractivePieDataModel;
import org.knime.base.node.viz.pie.datamodel.interactive.InteractivePieVizModel;
import org.knime.base.node.viz.pie.node.PieNodeModel;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.ExecutionMonitor;




/**
 * The NodeModel class of the interactive pie chart implementation.
 *
 * @author Tobias Koetter, University of Konstanz
 */
public class InteractivePieNodeModel
extends PieNodeModel<InteractivePieVizModel> {

    private InteractivePieDataModel m_model;

    /**
     * The constructor.
     */
    protected InteractivePieNodeModel() {
        super(); // one input, no outputs
        //if we set the node to autoExecutable = true the execute method
        //gets also called when the workspace is reloaded from file
        setAutoExecutable(true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InteractivePieVizModel getVizModelInternal()  {
        if (m_model == null) {
            return null;
        }
        final InteractivePieVizModel vizModel =
            new InteractivePieVizModel(m_model, getPieColumnName(),
                    getAggregationColumnName());
        return vizModel;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void resetPieData() {
        m_model = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadPieInternals(final File dataDir,
            final ExecutionMonitor exec) {
       //nothing to do
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void savePieInternals(final File dataDir,
            final ExecutionMonitor exec) {
        //nothing to do
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void createModel(final DataColumnSpec pieColSpec,
            final DataColumnSpec aggColSpec, final DataTableSpec spec,
            final int noOfRows) {
        m_model = new InteractivePieDataModel(spec, noOfRows);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void addDataRow(final DataRow row, final Color rowColor,
            final DataCell pieCell, final DataCell aggrCell) {
        m_model.addDataRow(row, rowColor, pieCell, aggrCell);
    }
}
