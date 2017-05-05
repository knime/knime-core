/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 * ---------------------------------------------------------------------
 *
 * History
 *   Jan 24, 2017 (simon): created
 */
package org.knime.time.node.manipulate.datetimeshift;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Period;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.Arrays;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.IntValue;
import org.knime.core.data.LongValue;
import org.knime.core.data.MissingCell;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.time.duration.DurationValue;
import org.knime.core.data.time.localdate.LocalDateCellFactory;
import org.knime.core.data.time.localdate.LocalDateValue;
import org.knime.core.data.time.localdatetime.LocalDateTimeCellFactory;
import org.knime.core.data.time.localdatetime.LocalDateTimeValue;
import org.knime.core.data.time.localtime.LocalTimeCellFactory;
import org.knime.core.data.time.localtime.LocalTimeValue;
import org.knime.core.data.time.period.PeriodValue;
import org.knime.core.data.time.zoneddatetime.ZonedDateTimeCellFactory;
import org.knime.core.data.time.zoneddatetime.ZonedDateTimeValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelColumnFilter2;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.streamable.simple.SimpleStreamableFunctionNodeModel;
import org.knime.core.util.UniqueNameGenerator;
import org.knime.time.Granularity;
import org.knime.time.util.DurationPeriodFormatUtils;

/**
 * The node model of the node which shifts date&time columns.
 *
 * @author Simon Schmid, KNIME.com, Konstanz, Germany
 */
final class DateTimeShiftNodeModel extends SimpleStreamableFunctionNodeModel {

    static final String OPTION_APPEND = "Append selected columns";

    static final String OPTION_REPLACE = "Replace selected columns";

    static final String OPTION_PERIOD_COLUMN = "Period/Duration Column";

    static final String OPTION_PERIOD_VALUE = "Period/Duration Value";

    static final String OPTION_NUMERICAL_COLUMN = "Numerical Column";

    static final String OPTION_NUMERICAL_VALUE = "Numerical Value";

    private final SettingsModelColumnFilter2 m_colSelect = createColSelectModel();

    private final SettingsModelString m_isReplaceOrAppend = createReplaceAppendModel();

    private final SettingsModelString m_suffix = createSuffixModel(m_isReplaceOrAppend);

    private final SettingsModelString m_periodSelection = createPeriodSelectionModel();

    private final SettingsModelString m_periodColSelect = createPeriodColSelectModel(m_periodSelection);

    private final SettingsModelString m_periodValue = createPeriodValueModel(m_periodSelection);

    private final SettingsModelString m_numericalSelection = createNumericalSelectionModel();

    private final SettingsModelString m_numericalColSelect = createNumericalColSelectModel(m_numericalSelection);

    private final SettingsModelInteger m_numericalValue = createNumericalValueModel(m_numericalSelection);

    private final SettingsModelString m_numericalGranularity = createNumericalGranularityModel();

    /** @return the string model, used in both dialog and model. */
    @SuppressWarnings("unchecked")
    public static SettingsModelColumnFilter2 createColSelectModel() {
        return new SettingsModelColumnFilter2("col_select", LocalDateValue.class, LocalTimeValue.class,
            LocalDateTimeValue.class, ZonedDateTimeValue.class);
    }

    /** @return the string model, used in both dialog and model. */
    public static SettingsModelString createReplaceAppendModel() {
        return new SettingsModelString("replace_or_append", OPTION_REPLACE);
    }

    /**
     * @param replaceOrAppendModel model for the replace/append button group
     * @return the string model, used in both dialog and model.
     */
    public static SettingsModelString createSuffixModel(final SettingsModelString replaceOrAppendModel) {
        final SettingsModelString suffixModel = new SettingsModelString("suffix", "(shifted)");
        replaceOrAppendModel.addChangeListener(
            e -> suffixModel.setEnabled(replaceOrAppendModel.getStringValue().equals(OPTION_APPEND)));
        suffixModel.setEnabled(false);
        return suffixModel;
    }

    /** @return the string model, used in both dialog and model. */
    public static SettingsModelString createPeriodSelectionModel() {
        return new SettingsModelString("period_selection", OPTION_PERIOD_COLUMN);
    }

    /**
     * @param periodSelectionModel model for the period selection button group
     * @return the string model, used in both dialog and model.
     */
    public static SettingsModelString createPeriodColSelectModel(final SettingsModelString periodSelectionModel) {
        final SettingsModelString model = new SettingsModelString("period_col_select", "");
        periodSelectionModel.addChangeListener(l -> model.setEnabled(
            periodSelectionModel.getStringValue().equals(OPTION_PERIOD_COLUMN) && periodSelectionModel.isEnabled()));
        return model;
    }

    /**
     * @param periodSelectionModel model for the period selection button group
     * @return the string model, used in both dialog and model.
     */
    public static SettingsModelString createPeriodValueModel(final SettingsModelString periodSelectionModel) {
        final SettingsModelString model = new SettingsModelString("period_value", "");
        periodSelectionModel.addChangeListener(l -> model.setEnabled(
            periodSelectionModel.getStringValue().equals(OPTION_PERIOD_VALUE) && periodSelectionModel.isEnabled()));
        model.setEnabled(false);
        return model;
    }

    /** @return the string model, used in both dialog and model. */
    public static SettingsModelString createNumericalSelectionModel() {
        final SettingsModelString model = new SettingsModelString("numerical_selection", OPTION_NUMERICAL_COLUMN);
        model.setEnabled(false);
        return model;
    }

    /**
     * @param numericalSelectionModel model for the numerical selection button group
     * @return the string model, used in both dialog and model.
     */
    public static SettingsModelString createNumericalColSelectModel(final SettingsModelString numericalSelectionModel) {
        final SettingsModelString model = new SettingsModelString("numerical_col_select", "");
        numericalSelectionModel.addChangeListener(
            l -> model.setEnabled(numericalSelectionModel.getStringValue().equals(OPTION_NUMERICAL_COLUMN)
                && numericalSelectionModel.isEnabled()));
        model.setEnabled(false);
        return model;
    }

    /**
     * @param numericalSelectionModel model for the numerical selection button group
     * @return the string model, used in both dialog and model.
     */
    public static SettingsModelInteger createNumericalValueModel(final SettingsModelString numericalSelectionModel) {
        final SettingsModelInteger model = new SettingsModelInteger("numerical_value", 1);
        numericalSelectionModel.addChangeListener(
            l -> model.setEnabled(numericalSelectionModel.getStringValue().equals(OPTION_NUMERICAL_VALUE)
                && numericalSelectionModel.isEnabled()));
        model.setEnabled(false);
        return model;
    }

    /**
     * @return the string model, used in both dialog and model.
     */
    public static SettingsModelString createNumericalGranularityModel() {
        final SettingsModelString model = new SettingsModelString("numerical_granularity", "");
        model.setEnabled(false);
        return model;
    }

    /** {@inheritDoc} */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        if (m_periodColSelect.isEnabled() && m_periodColSelect.getStringValue().equals("")) {
            throw new InvalidSettingsException("No configuration available!");
        }
        if (m_periodSelection.isEnabled()) {
            if (m_periodSelection.getStringValue().equals(OPTION_PERIOD_COLUMN)) {
                final String periodColName = m_periodColSelect.getStringValue();
                if (inSpecs[0].findColumnIndex(periodColName) < 0) {
                    throw new InvalidSettingsException("Column " + periodColName + " not found in input table!");
                }
                if (!(inSpecs[0].getColumnSpec(periodColName).getType().isCompatible(DurationValue.class)
                    || inSpecs[0].getColumnSpec(periodColName).getType().isCompatible(PeriodValue.class))) {
                    throw new InvalidSettingsException("Column " + periodColName + " is not compatible!");
                }
            }
        } else {
            if (m_numericalSelection.getStringValue().equals(OPTION_NUMERICAL_COLUMN)) {
                final String numericalColName = m_numericalColSelect.getStringValue();
                if (inSpecs[0].findColumnIndex(numericalColName) < 0) {
                    throw new InvalidSettingsException("Column " + numericalColName + " not found in input table!");
                }
                if (!(inSpecs[0].getColumnSpec(numericalColName).getType().isCompatible(IntValue.class)
                    || inSpecs[0].getColumnSpec(numericalColName).getType().isCompatible(LongValue.class))) {
                    throw new InvalidSettingsException("Column " + numericalColName + " is not compatible!");
                }
            }
        }
        DataTableSpec in = inSpecs[0];
        ColumnRearranger r = createColumnRearranger(in);
        DataTableSpec out = r.createSpec();
        return new DataTableSpec[]{out};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ColumnRearranger createColumnRearranger(final DataTableSpec spec) throws InvalidSettingsException {
        ColumnRearranger rearranger = new ColumnRearranger(spec);
        String[] includeList = m_colSelect.applyTo(spec).getIncludes();
        int[] includeIndices =
            Arrays.stream(m_colSelect.applyTo(spec).getIncludes()).mapToInt(s -> spec.findColumnIndex(s)).toArray();
        int i = 0;

        int periodColIndex = spec.findColumnIndex(m_periodColSelect.getStringValue());
        int numericalColIndex = spec.findColumnIndex(m_numericalColSelect.getStringValue());
        boolean isPeriod;

        if (m_periodSelection.isEnabled()) {
            if (m_periodColSelect.isEnabled()) {
                if (spec.getColumnSpec(periodColIndex).getType().isCompatible(PeriodValue.class)) {
                    isPeriod = true;
                } else {
                    isPeriod = false;
                }
            } else {
                periodColIndex = -1;
                try {
                    DurationPeriodFormatUtils.parsePeriod(m_periodValue.getStringValue());
                    isPeriod = true;
                } catch (DateTimeParseException e) {
                    isPeriod = false;
                }
            }
        } else {
            if (!m_numericalColSelect.isEnabled()) {
                numericalColIndex = -1;
            }
            if (!Granularity.fromString(m_numericalGranularity.getStringValue()).isPartOfDate()) {
                isPeriod = false;
            } else {
                isPeriod = true;
            }
        }
        for (String includedCol : includeList) {
            if (m_isReplaceOrAppend.getStringValue().equals(OPTION_REPLACE)) {
                final SingleCellFactory cellFac;
                final DataColumnSpec dataColSpec =
                    new DataColumnSpecCreator(includedCol, spec.getColumnSpec(includedCol).getType()).createSpec();
                if (isPeriod) {
                    cellFac = new DateTimeShiftPeriodCellFactory(dataColSpec, includeIndices[i++], periodColIndex,
                        numericalColIndex);
                } else {
                    cellFac = new DateTimeShiftDurationCellFactory(dataColSpec, includeIndices[i++], periodColIndex,
                        numericalColIndex);
                }
                rearranger.replace(cellFac, includedCol);
            } else {
                final DataColumnSpec dataColSpec = new UniqueNameGenerator(spec)
                    .newColumn(includedCol + m_suffix.getStringValue(), spec.getColumnSpec(includedCol).getType());
                final SingleCellFactory cellFac;
                if (isPeriod) {
                    cellFac = new DateTimeShiftPeriodCellFactory(dataColSpec, includeIndices[i++], periodColIndex,
                        numericalColIndex);
                } else {
                    cellFac = new DateTimeShiftDurationCellFactory(dataColSpec, includeIndices[i++], periodColIndex,
                        numericalColIndex);
                }
                rearranger.append(cellFac);
            }
        }
        return rearranger;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_colSelect.saveSettingsTo(settings);
        m_isReplaceOrAppend.saveSettingsTo(settings);
        m_suffix.saveSettingsTo(settings);
        m_periodSelection.saveSettingsTo(settings);
        m_periodColSelect.saveSettingsTo(settings);
        m_periodValue.saveSettingsTo(settings);
        m_numericalSelection.saveSettingsTo(settings);
        m_numericalColSelect.saveSettingsTo(settings);
        m_numericalValue.saveSettingsTo(settings);
        m_numericalGranularity.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_colSelect.validateSettings(settings);
        m_isReplaceOrAppend.validateSettings(settings);
        m_suffix.validateSettings(settings);
        m_periodSelection.validateSettings(settings);
        m_periodColSelect.validateSettings(settings);
        m_periodValue.validateSettings(settings);
        m_numericalSelection.validateSettings(settings);
        m_numericalColSelect.validateSettings(settings);
        m_numericalValue.validateSettings(settings);
        m_numericalGranularity.validateSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_colSelect.loadSettingsFrom(settings);
        m_isReplaceOrAppend.loadSettingsFrom(settings);
        m_suffix.loadSettingsFrom(settings);
        m_periodSelection.loadSettingsFrom(settings);
        m_periodColSelect.loadSettingsFrom(settings);
        m_periodValue.loadSettingsFrom(settings);
        m_numericalSelection.loadSettingsFrom(settings);
        m_numericalColSelect.loadSettingsFrom(settings);
        m_numericalValue.loadSettingsFrom(settings);
        m_numericalGranularity.loadSettingsFrom(settings);
    }

    private final class DateTimeShiftPeriodCellFactory extends SingleCellFactory {

        private final int m_colIndex;

        private final int m_periodColIdx;

        private final int m_numericalColIdx;

        /**
         * @param newColSpec new column spec
         * @param colIndex index of column to shift
         * @param periodColIdx index of column which has the period shift value (<0, if static value used)
         * @param numericalColIdx index of column which has the numerical shift value (<0, if static value used)
         */
        public DateTimeShiftPeriodCellFactory(final DataColumnSpec newColSpec, final int colIndex,
            final int periodColIdx, final int numericalColIdx) {
            super(newColSpec);
            m_colIndex = colIndex;
            m_periodColIdx = periodColIdx;
            m_numericalColIdx = numericalColIdx;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public DataCell getCell(final DataRow row) {
            final DataCell cell = row.getCell(m_colIndex);
            if (cell.isMissing()) {
                return cell;
            }
            Period period = null;
            if (m_periodSelection.isEnabled()) {
                if (m_periodColIdx >= 0) {
                    if (row.getCell(m_periodColIdx).isMissing()) {
                        return new MissingCell("The period cell containing the value to shift is missing.");
                    }
                    period = ((PeriodValue)row.getCell(m_periodColIdx)).getPeriod();
                } else {
                    period = DurationPeriodFormatUtils.parsePeriod(m_periodValue.getStringValue());
                }
            } else {
                final String granularity = m_numericalGranularity.getStringValue();
                final long numericalValue;
                if (m_numericalColIdx >= 0) {
                    DataCell numericalCell = row.getCell(m_numericalColIdx);
                    if (numericalCell.isMissing()) {
                        return new MissingCell("The numerical cell containing the value to shift is missing.");
                    }
                    numericalValue = ((LongValue)numericalCell).getLongValue();
                } else {
                    numericalValue = m_numericalValue.getIntValue();
                }
                try {
                    period = (Period)Granularity.fromString(granularity).getPeriodOrDuration(numericalValue);
                } catch (ArithmeticException e) {
                    setWarningMessage("A missing value has been generated due to integer overflow.");
                    return new MissingCell(e.getMessage());
                }
            }

            if (cell instanceof LocalDateValue) {
                final LocalDate localDate = ((LocalDateValue)cell).getLocalDate();
                return LocalDateCellFactory.create(localDate.plus(period));
            }
            if (cell instanceof LocalDateTimeValue) {
                final LocalDateTime localDateTime = ((LocalDateTimeValue)cell).getLocalDateTime();
                return LocalDateTimeCellFactory.create(localDateTime.plus(period));
            }
            if (cell instanceof ZonedDateTimeValue) {
                final ZonedDateTime zonedDateTime = ((ZonedDateTimeValue)cell).getZonedDateTime();
                return ZonedDateTimeCellFactory.create(zonedDateTime.plus(period));
            }
            throw new IllegalStateException("Unexpected data type: " + cell.getClass());
        }

    }

    private final class DateTimeShiftDurationCellFactory extends SingleCellFactory {

        private final int m_colIndex;

        private final int m_durationColIdx;

        private final int m_numericalColIdx;

        /**
         * @param newColSpec new column spec
         * @param colIndex index of column to shift
         * @param durationColIdx index of column which has the duration shift value (<0, if static value used)
         * @param numericalColIdx index of column which has the numerical shift value (<0, if static value used)
         */
        public DateTimeShiftDurationCellFactory(final DataColumnSpec newColSpec, final int colIndex,
            final int durationColIdx, final int numericalColIdx) {
            super(newColSpec);
            m_colIndex = colIndex;
            m_durationColIdx = durationColIdx;
            m_numericalColIdx = numericalColIdx;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public DataCell getCell(final DataRow row) {
            final DataCell cell = row.getCell(m_colIndex);
            if (cell.isMissing()) {
                return cell;
            }
            Duration duration = null;
            if (m_periodSelection.isEnabled()) {
                if (m_durationColIdx >= 0) {
                    if (row.getCell(m_durationColIdx).isMissing()) {
                        return new MissingCell("The duration cell containing the value to shift is missing.");
                    }
                    duration = ((DurationValue)row.getCell(m_durationColIdx)).getDuration();
                } else {
                    duration = DurationPeriodFormatUtils.parseDuration(m_periodValue.getStringValue());
                }
            } else {
                final String granularity = m_numericalGranularity.getStringValue();
                final long numericalValue;
                if (m_numericalColIdx >= 0) {
                    final DataCell numericalCell = row.getCell(m_numericalColIdx);
                    if (numericalCell.isMissing()) {
                        return new MissingCell("The numerical cell containing the value to shift is missing.");
                    }
                    numericalValue = ((LongValue)numericalCell).getLongValue();
                } else {
                    numericalValue = m_numericalValue.getIntValue();
                }
                try {
                    duration = (Duration)Granularity.fromString(granularity).getPeriodOrDuration(numericalValue);
                } catch (ArithmeticException e) {
                    setWarningMessage("A missing value has been generated due to integer overflow.");
                    return new MissingCell(e.getMessage());
                }
            }

            if (cell instanceof LocalTimeValue) {
                final LocalTime localTime = ((LocalTimeValue)cell).getLocalTime();
                return LocalTimeCellFactory.create(localTime.plus(duration));
            }
            if (cell instanceof LocalDateTimeValue) {
                final LocalDateTime localDateTime = ((LocalDateTimeValue)cell).getLocalDateTime();
                return LocalDateTimeCellFactory.create(localDateTime.plus(duration));
            }
            if (cell instanceof ZonedDateTimeValue) {
                final ZonedDateTime zonedDateTime = ((ZonedDateTimeValue)cell).getZonedDateTime();
                return ZonedDateTimeCellFactory.create(zonedDateTime.plus(duration));
            }
            throw new IllegalStateException("Unexpected data type: " + cell.getClass());
        }

    }
}
