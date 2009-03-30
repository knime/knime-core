/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
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

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.CloseableRowIterator;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.port.PortObjectSpec;

import org.knime.base.node.util.DataArray;
import org.knime.base.node.util.DefaultDataArray;
import org.knime.base.node.viz.aggregation.AggregationMethod;
import org.knime.base.node.viz.histogram.HistogramLayout;
import org.knime.base.node.viz.histogram.datamodel.AbstractHistogramVizModel;
import org.knime.base.node.viz.histogram.datamodel.HSBColorComparator;
import org.knime.base.node.viz.histogram.datamodel.InteractiveHistogramDataModel;
import org.knime.base.node.viz.histogram.datamodel.InteractiveHistogramVizModel;
import org.knime.base.node.viz.histogram.impl.AbstractHistogramPlotter;
import org.knime.base.node.viz.histogram.util.BinningUtil;
import org.knime.base.node.viz.histogram.util.ColorColumn;

import java.awt.Color;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.SortedSet;
import java.util.TreeSet;


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
     * {@inheritDoc}
     */
    @Override
    protected void createHistogramModel(final ExecutionContext exec,
            final int noOfRows, final BufferedDataTable data)
    throws CanceledExecutionException {
        LOGGER.debug("Entering createHistogramModel(exec, dataTable) "
                + "of class HistogramNodeModel.");
       if (noOfRows == 0) {
           m_model = null;
           return;
       }
       if (exec == null) {
           throw new NullPointerException("exec must not be null");
       }
       if (data == null) {
           throw new IllegalArgumentException(
                   "Table shouldn't be null");
       }
       ExecutionMonitor subExec = exec.createSubProgress(0.5);
       exec.setMessage("Adding rows to histogram model...");
        final DataArray dataArray =
            new DefaultDataArray(data, 1, noOfRows, subExec);
       exec.setMessage("Adding row color to histogram...");
       final SortedSet<Color> colorSet =
           new TreeSet<Color>(HSBColorComparator.getInstance());
       subExec = exec.createSubProgress(0.5);
       final double progressPerRow = 1.0 / noOfRows;
       double progress = 0.0;
       final CloseableRowIterator rowIterator = data.iterator();
       try {
           for (int i = 0; i < noOfRows && rowIterator.hasNext();
               i++) {
               final DataRow row = rowIterator.next();
               final Color color = data.getDataTableSpec().
                   getRowColor(row).getColor(false, false);
               if (!colorSet.contains(color)) {
                   colorSet.add(color);
               }
               progress += progressPerRow;
               subExec.setProgress(progress,
                       "Adding data rows to histogram...");
               subExec.checkCanceled();
           }
       } finally {
           if (rowIterator != null) {
               rowIterator.close();
           }
       }
       exec.setProgress(1.0, "Histogram finished.");
       m_model = new InteractiveHistogramDataModel(dataArray,
               new ArrayList<Color>(colorSet));
       LOGGER.debug("Exiting createHistogramModel(exec, dataTable) "
                + "of class HistogramNodeModel.");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        super.reset();
        m_model = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final PortObjectSpec[] inSpecs)
    throws InvalidSettingsException {
        try {
            return super.configure(inSpecs);
        } catch (final Exception e) {
            final DataTableSpec spec = (DataTableSpec)inSpecs[0];
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
     * {@inheritDoc}
     */
    @Override
    protected AbstractHistogramVizModel getHistogramVizModel() {
        if (m_model == null) {
            return null;
        }
        final AbstractHistogramVizModel vizModel =
            new InteractiveHistogramVizModel(m_model.getRowColors(),
                    AggregationMethod.getDefaultMethod(),
                    HistogramLayout.getDefaultLayout(), getTableSpec(),
                    m_model.getDataRows(), getXColSpec(),
                getAggrColumns(), BinningUtil.calculateIntegerMaxNoOfBins(
                        AbstractHistogramVizModel.DEFAULT_NO_OF_BINS,
                        getXColSpec()));
        return vizModel;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadHistogramInternals(final File dataDir,
            final ExecutionMonitor exec) throws Exception {
        try {
            m_model = InteractiveHistogramDataModel.loadFromFile(dataDir, exec);
        } catch (final FileNotFoundException e) {
            LOGGER.debug("Previous implementations haven't stored the data");
            m_model = null;
        } catch (final Exception e) {
            LOGGER.warn("Error while loadHistogramInternals of "
                    + "FixedColumn implementation: " + e.getMessage());
            m_model = null;
            throw e;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveHistogramInternals(final File dataDir,
            final ExecutionMonitor exec) throws Exception {
        if (m_model == null) {
            return;
        }
        try {
            m_model.save2File(dataDir, exec);
        } catch (final Exception e) {
            LOGGER.warn("Error while saveHistogramInternals of "
                    + "FixedColumn implementation: " + e.getMessage());
            throw e;
        }
    }
}
