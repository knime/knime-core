/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
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
