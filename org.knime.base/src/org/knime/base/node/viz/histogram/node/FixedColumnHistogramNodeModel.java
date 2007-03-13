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
package org.knime.base.node.viz.histogram.node;

import java.awt.Color;
import java.io.File;
import java.util.Collection;

import org.knime.base.node.viz.histogram.AggregationMethod;
import org.knime.base.node.viz.histogram.HistogramLayout;
import org.knime.base.node.viz.histogram.datamodel.AbstractHistogramVizModel;
import org.knime.base.node.viz.histogram.datamodel.ColorColumn;
import org.knime.base.node.viz.histogram.datamodel.FixedHistogramDataModel;
import org.knime.base.node.viz.histogram.datamodel.FixedHistogramDataRow;
import org.knime.base.node.viz.histogram.datamodel.FixedHistogramVizModel;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowIterator;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.NodeLogger;


/**
 * The NodeModel class of the histogram plotter.
 * 
 * @author Tobias Koetter, University of Konstanz
 */
public class FixedColumnHistogramNodeModel extends AbstractHistogramNodeModel {
    
    private static final NodeLogger LOGGER = 
        NodeLogger.getLogger(FixedColumnHistogramNodeModel.class);

    /**The data model on which the plotter based on.*/
    private FixedHistogramDataModel m_model;
    
    /**
     * The constructor.
     */
    protected FixedColumnHistogramNodeModel() {
        super(1, 0); // one input, no outputs
        //if we set the node to autoExecutable = true the execute method
        //gets also called when the workspace is reloaded from file
//        setAutoExecutable(true);
    }
    
    /**
     * @see org.knime.base.node.viz.histogram.node.AbstractHistogramNodeModel
     * #createHistogramModel(org.knime.core.node.ExecutionContext, 
     * org.knime.core.data.DataTable)
     */
    @Override
    protected void createHistogramModel(final ExecutionContext exec, 
            final DataTable table) throws CanceledExecutionException {
        LOGGER.debug("Entering createHistogramModel(exec, table) "
                + "of class FixedColumnHistogramNodeModel.");
        final int noOfRows = getNoOfRows();
        final Collection<ColorColumn> aggrColumns = getAggrColumns();
        m_model = 
            new FixedHistogramDataModel(getXColSpec(), noOfRows, aggrColumns);
        exec.setMessage("Adding data rows to histogram...");
        final double progressPerRow = 1.0 / noOfRows;
        double progress = 0.0;
        int aggrColSize = 0;
        if (aggrColumns != null) {
            aggrColSize = aggrColumns.size();
        }
        final DataTableSpec tableSpec = getTableSpec();
        final int xColIdx = getXColIdx();
        final RowIterator rowIterator = table.iterator();
        for (int rowCounter = 0; rowCounter < noOfRows 
        && rowIterator.hasNext(); rowCounter++) {
            final DataRow row = rowIterator.next();
            final Color color = 
                tableSpec.getRowColor(row).getColor(false, false);
            FixedHistogramDataRow histoRow;
            if (aggrColSize < 1) {
                histoRow = new FixedHistogramDataRow(
                        row.getKey(), color, row.getCell(xColIdx),
                        DataType.getMissingCell());
            } else {
                DataCell[] aggrCells = new DataCell[aggrColSize];
                int cellIdx = 0;
                for (ColorColumn aggrCol : aggrColumns) {
                    aggrCells[cellIdx++] = 
                        row.getCell(aggrCol.getColumnIndex());
                }
                histoRow = new FixedHistogramDataRow(
                        row.getKey(), color, row.getCell(xColIdx),
                        aggrCells);
            }
            m_model.addDataRow(histoRow);
            progress += progressPerRow;
            exec.setProgress(progress, "Adding data rows to histogram...");
            exec.checkCanceled();
        }
        exec.setMessage("Sorting rows...");
        //call this method to force the sorting
        m_model.getSortedRows();
        exec.setProgress(1.0, "Histogram finished.");
        LOGGER.debug("Exiting createHistogramModel(exec, table) "
                + "of class FixedColumnHistogramNodeModel.");
    }

    /**
     * @see org.knime.core.node.NodeModel#reset()
     */
    @Override
    protected void reset() {
        super.reset();
        m_model = null;
    }

    /**
     * @see org.knime.base.node.viz.histogram.node.AbstractHistogramNodeModel
     * #getHistogramVizModel()
     */
    @Override
    protected AbstractHistogramVizModel getHistogramVizModel() {
        if (m_model == null) {
            return null;
        }
        final FixedHistogramVizModel vizModel = new FixedHistogramVizModel(
                m_model.getRowColors(),
                AggregationMethod.getDefaultMethod(), 
                HistogramLayout.getDefaultLayout(), 
                m_model.getSortedRows(), m_model.getXColumnSpec(), 
                m_model.getAggrColumns(),
                AbstractHistogramVizModel.DEFAULT_NO_OF_BINS);
        return vizModel;
    }

    /**
     * @see org.knime.base.node.viz.histogram.node.AbstractHistogramNodeModel
     * #loadInternals(java.io.File, org.knime.core.node.ExecutionMonitor)
     */
    @Override
    protected void loadInternals(final File nodeInternDir, 
            final ExecutionMonitor exec) {
        
    }

    /**
     * @see org.knime.base.node.viz.histogram.node.AbstractHistogramNodeModel#saveInternals(java.io.File, org.knime.core.node.ExecutionMonitor)
     */
    @Override
    protected void saveInternals(final File nodeInternDir, 
            final ExecutionMonitor exec) {
        
    }
}
