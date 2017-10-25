/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.com; Email: contact@knime.com
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
 *   Feb 9, 2017 (simon): created
 */
package org.knime.time.node.convert.durationtonumber;

import java.time.Duration;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.MissingCell;
import org.knime.core.data.container.AbstractCellFactory;
import org.knime.core.data.container.CellFactory;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.DoubleCell.DoubleCellFactory;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.LongCell.LongCellFactory;
import org.knime.core.data.time.duration.DurationValue;
import org.knime.core.data.time.period.PeriodValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.streamable.simple.SimpleStreamableFunctionNodeModel;
import org.knime.core.util.UniqueNameGenerator;
import org.knime.time.util.Granularity;

/**
 * The node model of the node which converts durations to numbers.
 *
 * @author Simon Schmid, KNIME.com, Konstanz, Germany
 */
final class DurationToNumberNodeModel extends SimpleStreamableFunctionNodeModel {

    private final SettingsModelString m_colSelectModel = createColSelectModel();

    private final SettingsModelString m_durationFieldSelectModel = createDurationFieldSelectionModel();

    private final SettingsModelString m_typeSelectModel = createTypeSelectionModel();

    /** @return the string model, used in both dialog and model. */
    static SettingsModelString createColSelectModel() {
        return new SettingsModelString("col_select", null);
    }

    /** @return the string model, used in both dialog and model. */
    static SettingsModelString createDurationFieldSelectionModel() {
        return new SettingsModelString("duration_field_select", Granularity.HOUR.toString());
    }

    /** @return the string model, used in both dialog and model. */
    static SettingsModelString createTypeSelectionModel() {
        return new SettingsModelString("date_type_select", DataTypeMode.Truncated.name());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        final DataColumnSpec columnSpec = inSpecs[0].getColumnSpec(m_colSelectModel.getStringValue());
        if (columnSpec == null) {
            throw new InvalidSettingsException(
                "No column '" + m_colSelectModel.getStringValue() + "' in the input table found!");
        }
        if (!columnSpec.getType().isCompatible(DurationValue.class)) {
            if (columnSpec.getType().isCompatible(PeriodValue.class)) {
                throw new InvalidSettingsException(
                    "The selected column is a date-based duration, but must be time-based!");
            }
            throw new InvalidSettingsException("The selected column must be a time-based duration!");
        }
        return super.configure(inSpecs);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ColumnRearranger createColumnRearranger(final DataTableSpec spec) throws InvalidSettingsException {
        final ColumnRearranger rearranger = new ColumnRearranger(spec);

        if (m_colSelectModel.getStringValue() == null) {
            throw new InvalidSettingsException("Node must be configured!");
        }
        if (spec.findColumnIndex(m_colSelectModel.getStringValue()) < 0) {
            throw new InvalidSettingsException(
                "Column '" + m_colSelectModel.getStringValue() + "' not found in the input table.");
        }

        final boolean exact = m_typeSelectModel.getStringValue().equals(DataTypeMode.Exact.name());

        final DataColumnSpec colSpec = new UniqueNameGenerator(spec)
            .newColumn((m_durationFieldSelectModel.getStringValue()), exact ? DoubleCell.TYPE : LongCell.TYPE);
        final CellFactory cellFac = new ExtractDurationSingleFieldCellFactory(
            spec.findColumnIndex(m_colSelectModel.getStringValue()), exact, colSpec);
        rearranger.append(cellFac);
        return rearranger;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_colSelectModel.saveSettingsTo(settings);
        m_durationFieldSelectModel.saveSettingsTo(settings);
        m_typeSelectModel.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_colSelectModel.validateSettings(settings);
        m_durationFieldSelectModel.validateSettings(settings);
        m_typeSelectModel.validateSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_colSelectModel.loadSettingsFrom(settings);
        m_durationFieldSelectModel.loadSettingsFrom(settings);
        m_typeSelectModel.loadSettingsFrom(settings);
    }

    /**
     * {@link AbstractCellFactory} for single mode and duration.
     */
    private final class ExtractDurationSingleFieldCellFactory extends SingleCellFactory {
        private final int m_colIdx;

        private final boolean m_exact;

        public ExtractDurationSingleFieldCellFactory(final int colIdx, final boolean exact,
            final DataColumnSpec colSpec) {
            super(colSpec);
            m_colIdx = colIdx;
            m_exact = exact;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public DataCell getCell(final DataRow row) {
            final DataCell cell = row.getCell(m_colIdx);
            if (cell.isMissing()) {
                return cell;
            }

            final Duration duration = ((DurationValue)cell).getDuration();
            if (m_durationFieldSelectModel.getStringValue().equals(Granularity.HOUR.toString())) {
                if (m_exact) {
                    return DoubleCellFactory.create((double)duration.toNanos() / 1_000_000_000 / 3600);
                } else {
                    return LongCellFactory.create(duration.toHours());
                }
            }
            if (m_durationFieldSelectModel.getStringValue().equals(Granularity.MINUTE.toString())) {
                if (m_exact) {
                    return DoubleCellFactory.create((double)duration.toNanos() / 1_000_000_000 / 60);
                } else {
                    return LongCellFactory.create(duration.toMinutes());
                }
            }
            if (m_durationFieldSelectModel.getStringValue().equals(Granularity.SECOND.toString())) {
                if (m_exact) {
                    return DoubleCellFactory.create((double)duration.toNanos() / 1_000_000_000);
                } else {
                    // Because java.time.Duration stores only positive nanoseconds, one second needs to be
                    // added, if the duration is negative and nanoseconds != 0
                    final long seconds = duration.getSeconds();
                    return LongCellFactory.create((seconds < 0 && duration.getNano() > 0) ? (seconds + 1) : seconds);
                }
            }
            if (m_durationFieldSelectModel.getStringValue().equals(Granularity.MILLISECOND.toString())) {
                try {
                    if (m_exact) {
                        return DoubleCellFactory.create((double)duration.toNanos() / 1_000_000);
                    } else {
                        return LongCellFactory.create(duration.toMillis());
                    }
                } catch (ArithmeticException e) {
                    setWarningMessage(
                        "The duration in row '" + row.getKey() + "' was too big to convert to milliseconds!");
                    return new MissingCell(e.getMessage());
                }
            }
            if (m_durationFieldSelectModel.getStringValue().equals(Granularity.MICROSECOND.toString())) {
                try {
                    if (m_exact) {
                        return DoubleCellFactory.create((double)duration.toNanos() / 1000);
                    } else {
                        return LongCellFactory.create(duration.toNanos() / 1000);
                    }
                } catch (ArithmeticException e) {
                    setWarningMessage(
                        "The duration in row '" + row.getKey() + "' was too big to convert to microseconds!");
                    return new MissingCell(e.getMessage());
                }
            }
            if (m_durationFieldSelectModel.getStringValue().equals(Granularity.NANOSECOND.toString())) {
                try {
                    if (m_exact) {
                        return DoubleCellFactory.create(duration.toNanos());
                    } else {
                        return LongCellFactory.create(duration.toNanos());
                    }
                } catch (ArithmeticException e) {
                    setWarningMessage(
                        "The duration in row '" + row.getKey() + "' was too big to convert to nanoseconds!");
                    return new MissingCell(e.getMessage());
                }
            }
            throw new IllegalStateException("Unexpected field: " + m_durationFieldSelectModel.getStringValue());
        }
    }

}
