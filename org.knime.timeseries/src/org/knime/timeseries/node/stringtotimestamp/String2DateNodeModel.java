/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
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
 *   Jan 14, 2007 (rs): created
 */
package org.knime.timeseries.node.stringtotimestamp;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.StringValue;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.date.DateAndTimeCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.streamable.simple.SimpleStreamableFunctionWithInternalsNodeModel;
import org.knime.core.node.streamable.simple.SimpleStreamableOperatorInternals;
import org.knime.core.node.util.StringHistory;

/**
 * This is the model for the node that converts
 * {@link org.knime.core.data.def.StringCell}s into {@link DateAndTimeCell}s.
 *
 * @author Rosaria Silipo
 * @author Fabian Dill, KNIME.com AG, Zurich, Switzerland
 */
public class String2DateNodeModel extends
    SimpleStreamableFunctionWithInternalsNodeModel<SimpleStreamableOperatorInternals> {

    /** Key put into streamable internals to represent fail count. */
    private static final String INTERNALS_KEY_FAIL_COUNT = "fail_count";

    /**
     * Suffix to be appended to the selected column name to create a proposed
     * new column name.
     */
    static final String DEFAUL_COLUMN_NAME_SUFFIX = "time";

    private final SettingsModelString m_selectedColModel
        = String2DateDialog.createColumnSelectionModel();
    private final SettingsModelString m_newColNameModel
        = String2DateDialog.createColumnNameModel();
    private final SettingsModelBoolean m_replace
        = String2DateDialog.createReplaceModel();
    private final SettingsModelString m_formatModel
        = String2DateDialog.createFormatModel();
    private final SettingsModelBoolean m_cancelOnFail
        = String2DateDialog.createCancelOnFailModel();
    private final SettingsModelInteger m_failNumberModel
        = String2DateDialog.createFailNumberModel();

    private SimpleDateFormat m_dateFormat;

    private boolean m_useDate;

    private boolean m_useTime;

    private boolean m_useMillis;

    /** Inits node, 1 input, 1 output. */
    public String2DateNodeModel() {
        super(SimpleStreamableOperatorInternals.class);
        String2DateDialog.addColSelectionListener(m_selectedColModel,
                m_newColNameModel, DEFAUL_COLUMN_NAME_SUFFIX);
    }

    /** {@inheritDoc}
     * @since 2.6*/
    @Override
    protected SimpleStreamableOperatorInternals mergeStreamingOperatorInternals(
            final SimpleStreamableOperatorInternals[] internals) {
        long failCount = 0L;
        for (SimpleStreamableOperatorInternals m : internals) {
            long f = m.getConfig().getLong(INTERNALS_KEY_FAIL_COUNT, 0L);
            failCount += f;
        }
        SimpleStreamableOperatorInternals r =
            new SimpleStreamableOperatorInternals();
        r.getConfig().addLong(INTERNALS_KEY_FAIL_COUNT, failCount);
        return r;
    }

    /** {@inheritDoc}
     * @since 2.6*/
    @Override
    protected void finishStreamableExecution(
            final SimpleStreamableOperatorInternals internals) {
        long failCount = internals.getConfig().getLong(INTERNALS_KEY_FAIL_COUNT, 0);
        setFailMessage(failCount);
    }

    /** {@inheritDoc}
     * @throws InvalidSettingsException
     * @since 2.6*/
    @Override
    protected ColumnRearranger createColumnRearranger(final DataTableSpec spec,
            final SimpleStreamableOperatorInternals emptyInternals)
        throws InvalidSettingsException {
        if (m_formatModel.getStringValue() == null) {
            throw new InvalidSettingsException("No format selected.");
        }
        try {
            m_dateFormat = new SimpleDateFormat(m_formatModel.getStringValue());
        } catch (IllegalArgumentException ex) {
            throw new InvalidSettingsException("Invalid format: "
                    + m_formatModel.getStringValue());
        }
        m_dateFormat.setTimeZone(DateAndTimeCell.UTC_TIMEZONE);

        String selectedCol = m_selectedColModel.getStringValue();
        if (selectedCol == null || selectedCol.isEmpty()) {
            // try to find first String compatible one and auto-guess it
            for (DataColumnSpec cs : spec) {
                if (cs.getType().isCompatible(StringValue.class)) {
                    m_selectedColModel.setStringValue(cs.getName());
                    setWarningMessage(
                            "Auto-guessing first String compatible column: "
                            + cs.getName());
                    break;
                }
            }
        }
        // if still null -> no String compatible column at all
        if (selectedCol == null || selectedCol.isEmpty()) {
            throw new InvalidSettingsException(
                    "No String compatible column found!");
        }
        final int colIndex = spec.findColumnIndex(selectedCol);
        if (colIndex < 0) {
            throw new InvalidSettingsException("No such column: "
                    + selectedCol);
        }
        DataColumnSpec colSpec = spec.getColumnSpec(colIndex);
        if (!colSpec.getType().isCompatible(StringValue.class)) {
            throw new InvalidSettingsException("Column \""
                    + selectedCol + "\" does not contain string values: "
                    + colSpec.getType().toString());
        }
        ColumnRearranger result = new ColumnRearranger(spec);
        String uniqueColName = selectedCol;
        if (!m_replace.getBooleanValue()) {
            // if we do not have a default new column name yet
            // create one as done in
            // check whether the new column name is unique...
            uniqueColName = DataTableSpec.getUniqueColumnName(spec,
                    m_newColNameModel.getStringValue());
            m_newColNameModel.setStringValue(uniqueColName);
        }
        DataColumnSpec newColSpec = new DataColumnSpecCreator(uniqueColName,
                DateAndTimeCell.TYPE).createSpec();
        m_dateFormat = new SimpleDateFormat(m_formatModel.getStringValue());
        m_dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        SingleCellFactory c = new SingleCellFactory(newColSpec) {
            private int m_failCounter = 0;
            @Override
            public DataCell getCell(final DataRow row) {
                DataCell cell = row.getCell(colIndex);
                if (cell.isMissing() || !(cell instanceof StringValue)) {
                    return DataType.getMissingCell();
                }
                try {
                    String source = ((StringValue)cell).getStringValue();
                    Date date = m_dateFormat.parse(source);
                    Calendar calendar = DateAndTimeCell.getUTCCalendar();
                    calendar.setTimeInMillis(date.getTime());
                    // dependent on the type create the referring cell
                    return new DateAndTimeCell(calendar.getTimeInMillis(),
                            m_useDate, m_useTime, m_useMillis);
                } catch (ParseException pe) {
                    m_failCounter++;
                    if (m_cancelOnFail.getBooleanValue()
                            && m_failCounter >= m_failNumberModel
                                .getIntValue()) {
                        throw new RuntimeException(
                                "Maximum number of fails reached: "
                                + m_failNumberModel.getIntValue());
                    }
                    return DataType.getMissingCell();
                }
            }

            @Override
            public void afterProcessing() {
                setFailMessage(m_failCounter);
                emptyInternals.getConfig().addLong(
                        INTERNALS_KEY_FAIL_COUNT, m_failCounter);
            }
        };
        if (m_replace.getBooleanValue()) {
            result.replace(c, colIndex);
        } else {
            result.append(c);
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_selectedColModel.validateSettings(settings);
        m_newColNameModel.validateSettings(settings);
        SettingsModelString newColNameModel = m_newColNameModel
            .createCloneWithValidatedValue(settings);
        String newColName = newColNameModel.getStringValue();
        if (newColName == null || newColName.isEmpty()) {
            throw new InvalidSettingsException(
                    "Name for the new column must not be empty!");
        }
        m_replace.validateSettings(settings);
        m_formatModel.validateSettings(settings);
        SettingsModelString formatClone = m_formatModel
            .createCloneWithValidatedValue(settings);
        String format = formatClone.getStringValue();
        if (format == null || format.length() == 0) {
            throw new InvalidSettingsException("Format must not be empty!");
        }
        try {
            new SimpleDateFormat(format);
        } catch (Exception e) {
            String msg = "Invalid date format: \"" + format + "\".";
            String errMsg = e.getMessage();
            if (errMsg != null && !errMsg.isEmpty()) {
                msg += " Reason: " + errMsg;
            }
            throw new InvalidSettingsException(msg);
        }
        m_cancelOnFail.validateSettings(settings);
        m_failNumberModel.validateSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_selectedColModel.loadSettingsFrom(settings);
        m_newColNameModel.loadSettingsFrom(settings);
        m_replace.loadSettingsFrom(settings);
        m_formatModel.loadSettingsFrom(settings);
        m_cancelOnFail.loadSettingsFrom(settings);
        m_failNumberModel.loadSettingsFrom(settings);
        // define the type
        // if it contains H, m, s -> time
        // if it contains y, M or d -> date
        String dateformat = m_formatModel.getStringValue();
        m_useTime = containsTime(dateformat);
        m_useDate = containsDate(dateformat);
        m_useMillis = containsMillis(dateformat);
        // if it is not a predefined one -> store it
        if (!String2DateDialog.PREDEFINED_FORMATS.contains(dateformat)) {
            StringHistory.getInstance(String2DateDialog.FORMAT_HISTORY_KEY).add(
                    dateformat);
        }
    }

    private boolean containsTime(final String dateFormat) {
        return dateFormat.contains("H") || dateFormat.contains("m")
            || dateFormat.contains("s");
    }

    private boolean containsMillis(final String dateFormat) {
        if (!containsTime(dateFormat)) {
            return false;
        }
        return dateFormat.contains("S");
    }

    private boolean containsDate(final String dateFormat) {
        return dateFormat.contains("y") || dateFormat.contains("M")
            || dateFormat.contains("d");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_selectedColModel.saveSettingsTo(settings);
        m_newColNameModel.saveSettingsTo(settings);
        m_replace.saveSettingsTo(settings);
        m_formatModel.saveSettingsTo(settings);
        m_cancelOnFail.saveSettingsTo(settings);
        m_failNumberModel.saveSettingsTo(settings);
    }

    private void setFailMessage(final long failCounter) {
        if (failCounter > 0) {
            setWarningMessage("Couldn't parse " + failCounter
                    + " row(s) due to parsing errors "
                    + "(data format wrong?)");
        }
    }

}
