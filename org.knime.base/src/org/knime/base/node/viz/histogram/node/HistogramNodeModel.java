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

import org.knime.base.node.viz.histogram.AbstractHistogramPlotter;
import org.knime.base.node.viz.histogram.AggregationMethod;
import org.knime.base.node.viz.histogram.HistogramLayout;
import org.knime.base.node.viz.histogram.datamodel.AbstractHistogramVizModel;
import org.knime.base.node.viz.histogram.datamodel.InteractiveHistogramDataModel;
import org.knime.base.node.viz.histogram.datamodel.InteractiveHistogramVizModel;
import org.knime.base.node.viz.histogram.util.ColorColumn;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowIterator;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;


/**
 * The NodeModel class of the interactive histogram plotter.
 * 
 * @author Tobias Koetter, University of Konstanz
 */
public class HistogramNodeModel extends AbstractHistogramNodeModel {
    private static final NodeLogger LOGGER = 
        NodeLogger.getLogger(HistogramNodeModel.class);
    
    /**The histogram data model which holds all information.*/
    private InteractiveHistogramDataModel m_model;
    
    /**
     * The constructor.
     */
    protected HistogramNodeModel() {
        super(1, 0); // one input, no outputs
        //if we set the node to autoExecutable = true the execute method
        //gets also called when the workspace is reloaded from file
        setAutoExecutable(true);
    }

    /**
     * @see org.knime.base.node.viz.histogram.node.AbstractHistogramNodeModel
     * #createHistogramModel(org.knime.core.node.ExecutionContext, 
     * org.knime.core.data.DataTable)
     */
    @Override
    protected void createHistogramModel(final ExecutionContext exec, 
            final DataTable dataTable) 
    throws CanceledExecutionException {
        LOGGER.debug("Entering createHistogramModel(exec, dataTable) "
                + "of class HistogramNodeModel.");
        final DataTableSpec tableSpec = getTableSpec();
       final int noOfRows = getNoOfRows();
       m_model = new InteractiveHistogramDataModel(tableSpec, noOfRows);
        exec.setMessage("Adding data rows to histogram...");
        final double progressPerRow = 1.0 / noOfRows;
        double progress = 0.0;
        final RowIterator rowIterator = dataTable.iterator();
        for (int i = 0; i < noOfRows && rowIterator.hasNext();
            i++) {
            final DataRow row = rowIterator.next();
            m_model.addDataRow(row);
            progress += progressPerRow;
            exec.setProgress(progress, "Adding data rows to histogram...");
            exec.checkCanceled();
        }
        exec.setProgress(1.0, "Histogram finished.");
        LOGGER.debug("Exiting createHistogramModel(exec, dataTable) "
                + "of class HistogramNodeModel.");
    }

    /**
     * @see org.knime.base.node.viz.histogram.node.AbstractHistogramNodeModel
     * #reset()
     */
    @Override
    protected void reset() {
        super.reset();
        m_model = null;
    }
    
    /**
     * @see org.knime.base.node.viz.histogram.node.AbstractHistogramNodeModel
     * #configure(org.knime.core.data.DataTableSpec[])
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) 
    throws InvalidSettingsException {
        try {
            return super.configure(inSpecs);
        } catch (Exception e) {
            final DataTableSpec spec = inSpecs[0];
            if (spec == null) {
                throw new IllegalArgumentException(
                        "No table specification found");
            }
            final int numColumns = spec.getNumColumns();
          if (numColumns < 1) {
              throw new InvalidSettingsException(
                      "Input table should have at least 1 column.");
          }
          boolean xFound = false;
          boolean aggrFound = false;
          for (int i = 0; i < numColumns; i++) {
              final DataColumnSpec columnSpec = spec.getColumnSpec(i);
              if (!xFound 
                      && AbstractHistogramPlotter.X_COLUMN_FILTER.includeColumn(
                      columnSpec)) {
                  setSelectedXColumnName(columnSpec.getName());
                  xFound = true;
              } else if (!aggrFound 
                      && AbstractHistogramPlotter.AGGREGATION_COLUMN_FILTER.
                      includeColumn(columnSpec)) {
                  setSelectedAggrColumns(new ColorColumn(Color.lightGray, 
                          columnSpec.getName()));
                  aggrFound = true;
              }
              if (xFound && aggrFound) {
                  break;
              }
          }
          if (!xFound) {
              throw new InvalidSettingsException(
                      "No column compatible with this node. Column needs to "
                      + "be nominal or numeric and must contain a valid "
                      + "domain. In order to compute the domain of a column "
                      + "use the DomainCalculator or ColumnFilter node.");
          }
        }
        return new DataTableSpec[0];
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
        final InteractiveHistogramVizModel vizModel = 
            new InteractiveHistogramVizModel(m_model.getRowColors(), 
                AggregationMethod.getDefaultMethod(), 
                HistogramLayout.getDefaultLayout(), getTableSpec(),
                m_model.getDataRows(), getXColSpec(), getAggrColumns(), 
                AbstractHistogramVizModel.DEFAULT_NO_OF_BINS);
        return vizModel;
    }

    /**
     * @see org.knime.base.node.viz.histogram.node.AbstractHistogramNodeModel
     * #loadHistogramInternals(java.io.File, 
     * org.knime.core.node.ExecutionMonitor)
     */
    @Override
    protected void loadHistogramInternals(final File dataDir, 
            final ExecutionMonitor exec) {
        //      nothing to do since it is auto executable
    }

    /**
     * @see org.knime.base.node.viz.histogram.node.AbstractHistogramNodeModel
     * #saveHistogramInternals(java.io.File, 
     * org.knime.core.node.ExecutionMonitor)
     */
    @Override
    protected void saveHistogramInternals(final File dataDir, 
            final ExecutionMonitor exec) {
        //      nothing to do since it is auto executable
    }
}
