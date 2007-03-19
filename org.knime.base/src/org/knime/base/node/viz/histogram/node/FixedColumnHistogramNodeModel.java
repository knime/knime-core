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
import java.io.FileNotFoundException;
import java.util.Collection;

import org.knime.base.node.viz.histogram.AggregationMethod;
import org.knime.base.node.viz.histogram.HistogramLayout;
import org.knime.base.node.viz.histogram.datamodel.AbstractHistogramVizModel;
import org.knime.base.node.viz.histogram.datamodel.FixedHistogramDataModel;
import org.knime.base.node.viz.histogram.datamodel.FixedHistogramVizModel;
import org.knime.base.node.viz.histogram.util.ColorNameColumn;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowIterator;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;


/**
 * The NodeModel class of the histogram plotter.
 * 
 * @author Tobias Koetter, University of Konstanz
 */
public class FixedColumnHistogramNodeModel extends AbstractHistogramNodeModel {
    
    private static final NodeLogger LOGGER = 
        NodeLogger.getLogger(FixedColumnHistogramNodeModel.class);

    /**The number of bins configuration key.*/
    protected static final String CFGKEY_NO_OF_BINS = "noOfBins";
    
    private final SettingsModelInteger m_noOfBins = new SettingsModelInteger(
            CFGKEY_NO_OF_BINS, AbstractHistogramVizModel.DEFAULT_NO_OF_BINS);
    /**The data model on which the plotter based on.*/
    private FixedHistogramDataModel m_model;
    
    /**
     * The constructor.
     */
    protected FixedColumnHistogramNodeModel() {
        super(1, 0); // one input, no outputs
    }
    /**
     * @see org.knime.core.node.NodeModel #validateSettings(NodeSettingsRO)
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) 
    throws InvalidSettingsException {
        super.validateSettings(settings);
        m_noOfBins.validateSettings(settings);
    }

    /**
     * @see org.knime.core.node.NodeModel
     *      #loadValidatedSettingsFrom(NodeSettingsRO)
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) 
    throws InvalidSettingsException {
        super.loadValidatedSettingsFrom(settings);
        m_noOfBins.loadSettingsFrom(settings);
    }

    /**
     * @see org.knime.core.node.NodeModel #saveSettingsTo(NodeSettingsWO)
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        super.saveSettingsTo(settings);
        m_noOfBins.saveSettingsTo(settings);
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
        final Collection<ColorNameColumn> aggrColumns = getAggrColumns();
        final int noOfBins = m_noOfBins.getIntValue();
        m_model = 
            new FixedHistogramDataModel(getXColSpec(), aggrColumns, noOfBins);
        exec.setMessage("Adding data rows to histogram...");
        final double progressPerRow = 1.0 / noOfRows;
        double progress = 0.0;
        int aggrColSize = 0;
        if (aggrColumns != null) {
            aggrColSize = aggrColumns.size();
        }
        final DataTableSpec tableSpec = getTableSpec();
        final int xColIdx = getXColIdx();
        //save the aggregation column indices
        final int[] aggrColIdxs = new int[aggrColSize];
        if (aggrColSize > 0) {
            int idx = 0;
            for (ColorNameColumn aggrCol : aggrColumns) {
                aggrColIdxs[idx++] = tableSpec.findColumnIndex(
                        aggrCol.getColumnName());
            }
        }
        final RowIterator rowIterator = table.iterator();
        for (int rowCounter = 0; rowCounter < noOfRows 
        && rowIterator.hasNext(); rowCounter++) {
            final DataRow row = rowIterator.next();
            final Color color = 
                tableSpec.getRowColor(row).getColor(false, false);
            if (aggrColSize < 1) {
                m_model.addDataRow(row.getKey().getId(), color, 
                        row.getCell(xColIdx), DataType.getMissingCell());
            } else {
                DataCell[] aggrCells = new DataCell[aggrColSize];
                for (int i = 0, length = aggrColIdxs.length; i < length; i++) {
                    aggrCells[i] = row.getCell(aggrColIdxs[i]);
                }
                m_model.addDataRow(row.getKey().getId(), color, 
                        row.getCell(xColIdx), aggrCells);
            }
            
            progress += progressPerRow;
            exec.setProgress(progress, "Adding data rows to histogram...");
            exec.checkCanceled();
        }
        exec.setMessage("Sorting rows...");
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
                m_model.getRowColors(), m_model.getClonedBins(), 
                m_model.getClonedMissingValueBin(), m_model.getXColumnSpec(),
                m_model.getAggrColumns(),
                AggregationMethod.getDefaultMethod(), 
                HistogramLayout.getDefaultLayout());
        return vizModel;
    }

    /**
     * @see org.knime.base.node.viz.histogram.node.AbstractHistogramNodeModel#
     * loadHistogramInternals(java.io.File, 
     * org.knime.core.node.ExecutionMonitor)
     */
    @Override
    protected void loadHistogramInternals(final File dataDir, 
            final ExecutionMonitor exec) throws Exception {
        try {
            m_model = FixedHistogramDataModel.loadFromFile(dataDir, exec);
        } catch (FileNotFoundException e) {
            LOGGER.debug("Previous implementations haven't stored the data");
            m_model = null;
        } catch (Exception e) {
            LOGGER.warn("Error while saveHistogramInternals of "
                    + "FixedColumn implementation: " + e.getMessage());
            m_model = null;
            throw e;
        }
    }
    /**
     * @see org.knime.base.node.viz.histogram.node.AbstractHistogramNodeModel#
     * saveHistogramInternals(java.io.File, 
     * org.knime.core.node.ExecutionMonitor)
     */
    @Override
    protected void saveHistogramInternals(final File dataDir, 
            final ExecutionMonitor exec) throws Exception {
        if (m_model == null) {
            return;
        }
        try {
            m_model.save2File(dataDir, exec);
        } catch (Exception e) {
            LOGGER.warn("Error while saveHistogramInternals of "
                    + "FixedColumn implementation: " + e.getMessage());
            throw e;
        }
    }
}
