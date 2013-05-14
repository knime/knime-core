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
 * ------------------------------------------------------------------------
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
import org.knime.core.data.date.DateAndTimeValue;
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
public class ExtractTimeWindowNodeModel extends NodeModel {

    private final SettingsModelString m_columnName
    = ExtractTimeWindowNodeDialog.createColumnNameModel();

    private final SettingsModelCalendar m_fromDate
        = ExtractTimeWindowNodeDialog.createFromModel();

    private final SettingsModelCalendar m_toDate
        = ExtractTimeWindowNodeDialog.createToModel();

        /** Inits node, 1 input, 1 output. */
    public ExtractTimeWindowNodeModel() {
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
                if (cs.getType().isCompatible(DateAndTimeValue.class)) {
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
            if (colSpec.getType().isCompatible(DateAndTimeValue.class)
                    && colSpec.getDomain().hasBounds()) {
                DataCell lower = colSpec.getDomain().getLowerBound();
                DataCell upper = colSpec.getDomain().getUpperBound();
                if (lower != null && upper != null
                        && lower.getType().isCompatible(
                                DateAndTimeValue.class)
                        && upper.getType().isCompatible(
                                DateAndTimeValue.class)) {
                    Calendar c = ((DateAndTimeValue)lower)
                        .getUTCCalendarClone();
                    m_fromDate.setCalendar(c);
                    c = ((DateAndTimeValue)upper).getUTCCalendarClone();
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
            if (!colSpec.getType().isCompatible(DateAndTimeValue.class)) {
                throw new InvalidSettingsException("Column \"" + m_columnName
                        + "\" does not contain string values: "
                        + colSpec.getType().toString());
            }
        }
        validateFromTo(m_fromDate.getCalendar(), m_toDate.getCalendar());
        // we return input specs since only rows are filtered
        // (no structural changes)
        return inSpecs.clone();
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
                Calendar time = ((DateAndTimeValue)cell).getUTCCalendarClone();
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
