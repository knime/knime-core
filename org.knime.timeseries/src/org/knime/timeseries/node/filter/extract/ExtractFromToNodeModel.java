/*
 * -------------------------------------------------------------------
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
 *   Jan 24, 2007 (rs): created
 */
package org.knime.timeseries.node.filter.extract;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.date.TimestampValue;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.timeseries.util.SettingsModelCalendar;

/**
 * This is the model for the node that extracts data from timestampFrom
 * to timestampTo from the input table.
 *
 * @author Rosaria Silipo
 */
public class ExtractFromToNodeModel extends NodeModel {

    private final SettingsModelString m_columnName
    = ExtractFromToDialog.createColumnNameModel();

    private final SettingsModelCalendar m_fromDate
        = ExtractFromToDialog.createFromModel();

    private final SettingsModelCalendar m_toDate
        = ExtractFromToDialog.createToModel();

        /** Inits node, 1 input, 1 output. */
    public ExtractFromToNodeModel() {
        super(1, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        int colIndex = -1;
        if (m_columnName.getStringValue() == null) {
            // no value yet -> auto-configure
            int i = 0;
            for (DataColumnSpec cs : inSpecs[0]) {
                if (cs.getType().isCompatible(TimestampValue.class)) {
                    colIndex = i;
                    // found first date compatible column
                    // -> auto-select it
                    m_columnName.setStringValue(cs.getName());
                    setWarningMessage("Auto-selected date column: "
                            + cs.getName());
                    break;
                }
                i++;
            }
            // if we did not found any time compatible column
            if (colIndex == -1) {
                throw new InvalidSettingsException("No column selected.");
            }
            // set the from and to calendars to the minimum and maximum date of
            // the auto-selected column
            DataColumnSpec colSpec = inSpecs[0].getColumnSpec(
                    m_columnName.getStringValue());
            if (colSpec.getType().isCompatible(TimestampValue.class)
                    && colSpec.getDomain().hasBounds()) {
                DataCell lower = colSpec.getDomain().getLowerBound();
                DataCell upper = colSpec.getDomain().getUpperBound();
                if (lower != null && upper != null
                        && lower.getType().isCompatible(TimestampValue.class)
                        && upper.getType().isCompatible(TimestampValue.class)) {
                    Calendar c = ((TimestampValue)lower).getUTCCalendarClone();
                    m_fromDate.setCalendar(c);
                    c = ((TimestampValue)upper).getUTCCalendarClone();
                    m_toDate.setCalendar(c);
                }
            }
        } else {
            // configured once -> we have a name selected
            colIndex = inSpecs[0]
                    .findColumnIndex(m_columnName.getStringValue());
            if (colIndex < 0) {
                throw new InvalidSettingsException("No such column: "
                        + m_columnName.getStringValue());
            }
            DataColumnSpec colSpec = inSpecs[0].getColumnSpec(colIndex);
            if (!colSpec.getType().isCompatible(TimestampValue.class)) {
                throw new InvalidSettingsException("Column \"" + m_columnName
                        + "\" does not contain string values: "
                        + colSpec.getType().toString());
            }
        }        
        validateFromTo(m_fromDate.getCalendar(), m_toDate.getCalendar());
        // we return input specs since only rows are filtered
        // (no structural changes)
        DataTableSpec[] outs = inSpecs.clone();
        return outs;
    }

    private void validateFromTo(final Calendar start, final Calendar end)
        throws InvalidSettingsException {
        if (end.before(start)
                || end.getTimeInMillis() == start.getTimeInMillis()) {
            throw new InvalidSettingsException("End point "
                    + end.getTime().toString()
                    + " must be after starting point "
                    + start.getTime().toString());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        BufferedDataTable in = inData[0];
        DataTableSpec outs = in.getDataTableSpec();
        final int colIndex = outs
                .findColumnIndex(m_columnName.getStringValue());
        BufferedDataContainer t = exec.createDataContainer(outs);
        final int totalRowCount = in.getRowCount();
        int currentIteration = 0;
        try {
            for (DataRow r : in) {
                // increment before printing to achieve a 1-based index
                currentIteration++;
                exec.checkCanceled();
                exec.setProgress(
                        currentIteration / (double)totalRowCount,
                        "Processing row " + currentIteration);

                DataCell cell = r.getCell(colIndex);
                if (cell.isMissing()) {
                    // do not include missing values -> skip it
                    continue;
                }
                Calendar time = ((TimestampValue)cell).getUTCCalendarClone();
                // use "compareTo" in order to include also the dates on the 
                // interval borders (instead of using "after" and "before", 
                // which is implemented as a real < or >
                if (time.compareTo(m_fromDate.getCalendar()) >= 0
                        && time.compareTo(m_toDate.getCalendar()) <= 0) {
                    t.addRowToTable(r);
                }
            }
        } finally {
            t.close();
        }
        return new BufferedDataTable[]{t.getTable()};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        // first do the basic checking
        m_columnName.validateSettings(settings);
        m_fromDate.validateSettings(settings);
        m_toDate.validateSettings(settings);
        // check whether the from date is equal or later than the to date
        Calendar from = ((SettingsModelCalendar)m_fromDate
                    .createCloneWithValidatedValue(settings)).getCalendar();
        Calendar to = ((SettingsModelCalendar)m_toDate
                .createCloneWithValidatedValue(settings)).getCalendar();
        if (from.getTimeInMillis() == to.getTimeInMillis()
                || to.before(from)) {
            throw new InvalidSettingsException(
                    "The starting point must be before the end point!");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_columnName.loadSettingsFrom(settings);
        m_fromDate.loadSettingsFrom(settings);
        m_toDate.loadSettingsFrom(settings);
   }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_fromDate.saveSettingsTo(settings);
        m_toDate.saveSettingsTo(settings);
        m_columnName.saveSettingsTo(settings);
   }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // nothing to do here
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // nothing to do here.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // nothing to do here
    }
}
