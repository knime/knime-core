/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2010
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

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.container.CloseableRowIterator;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.port.PortObjectSpec;

import org.knime.base.node.viz.aggregation.AggregationMethod;
import org.knime.base.node.viz.histogram.HistogramLayout;
import org.knime.base.node.viz.histogram.datamodel.AbstractHistogramVizModel;
import org.knime.base.node.viz.histogram.datamodel.FixedHistogramDataModel;
import org.knime.base.node.viz.histogram.datamodel.FixedHistogramVizModel;
import org.knime.base.node.viz.histogram.util.BinningUtil;
import org.knime.base.node.viz.histogram.util.ColorColumn;

import java.awt.Color;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Collection;


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
        //set the all rows select box to true as default value since that's the
        //reason why we have two implementations and this one is the one which
        //should handle a large amount of data.
        setAllRowsDefault(true);
    }
    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
    throws InvalidSettingsException {
        super.validateSettings(settings);
        try {
            m_noOfBins.validateSettings(settings);
        } catch (final InvalidSettingsException e) {
            //this is an older implementation
            LOGGER.debug("Old implementation found");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
    throws InvalidSettingsException {
        super.loadValidatedSettingsFrom(settings);
        try {
            m_noOfBins.loadSettingsFrom(settings);
        } catch (final InvalidSettingsException e) {
            m_noOfBins.setIntValue(
                    AbstractHistogramVizModel.DEFAULT_NO_OF_BINS);
            LOGGER.debug("Old settings found using default number of bins");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        super.saveSettingsTo(settings);
        m_noOfBins.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void createHistogramModel(final ExecutionContext exec,
            final int noOfRows, final BufferedDataTable table)
    throws CanceledExecutionException {
        LOGGER.debug("Entering createHistogramModel(exec, table) "
                + "of class FixedColumnHistogramNodeModel.");
        final Collection<ColorColumn> aggrColumns = getAggrColumns();
        final int noOfBins =
            BinningUtil.calculateIntegerMaxNoOfBins(m_noOfBins.getIntValue(),
                    getXColSpec());
        m_model =
            new FixedHistogramDataModel(getXColSpec(),
                    AggregationMethod.getDefaultMethod(), aggrColumns,
                    noOfBins);
        if (noOfRows < 1) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("No rows available");
            }
            return;
        }
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
        if (aggrColumns != null) {
            int idx = 0;
            for (final ColorColumn aggrCol : aggrColumns) {
                aggrColIdxs[idx++] = tableSpec.findColumnIndex(
                        aggrCol.getColumnName());
            }
        }
        final CloseableRowIterator rowIterator = table.iterator();
        try {
            for (int rowCounter = 0; rowCounter < noOfRows
            && rowIterator.hasNext(); rowCounter++) {
                final DataRow row = rowIterator.next();
                final Color color =
                    tableSpec.getRowColor(row).getColor(false, false);
                if (aggrColSize < 1) {
                    m_model.addDataRow(row.getKey(), color,
                            row.getCell(xColIdx), DataType.getMissingCell());
                } else {
                    final DataCell[] aggrCells = new DataCell[aggrColSize];
                    for (int i = 0, length = aggrColIdxs.length;
                        i < length; i++) {
                        aggrCells[i] = row.getCell(aggrColIdxs[i]);
                    }
                    m_model.addDataRow(row.getKey(), color,
                            row.getCell(xColIdx), aggrCells);
                }

                progress += progressPerRow;
                exec.setProgress(progress, "Adding data rows to histogram...");
                exec.checkCanceled();
            }
        } finally {
            if (rowIterator != null) {
                rowIterator.close();
            }
        }
        exec.setMessage("Sorting rows...");
        exec.setProgress(1.0, "Histogram finished.");
        LOGGER.debug("Exiting createHistogramModel(exec, table) "
                + "of class FixedColumnHistogramNodeModel.");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final PortObjectSpec[] inSpecs)
        throws InvalidSettingsException {
        final DataTableSpec[] specs = super.configure(inSpecs);
        //enable/disable the number of bins spinner depending on the selected
        //x column. We have to set in here and in the dialog because the dialog
        //settings are replaced by the settings from the SettingsModelString
        //of the node model!!!
        final DataColumnSpec xColSpec = getXColSpec();
        m_noOfBins.setEnabled((xColSpec == null
                || xColSpec.getType().isCompatible(DoubleValue.class)));
        return specs;
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
    protected AbstractHistogramVizModel getHistogramVizModel() {
        if (m_model == null) {
            return null;
        }
        final AbstractHistogramVizModel vizModel = new FixedHistogramVizModel(
                m_model.getRowColors(), m_model.getClonedBins(),
                m_model.getClonedMissingValueBin(), m_model.getXColumnSpec(),
                m_model.getAggrColumns(),
                m_model.getAggrMethod(),
                HistogramLayout.getDefaultLayout());
        return vizModel;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadHistogramInternals(final File dataDir,
            final ExecutionMonitor exec) throws Exception {
        try {
            m_model = FixedHistogramDataModel.loadFromFile(dataDir, exec);
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
