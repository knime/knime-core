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
 *   Feb 9, 2017 (simon): created
 */
package org.knime.time.node.extract;

import java.time.Duration;
import java.time.Period;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.MissingCell;
import org.knime.core.data.container.AbstractCellFactory;
import org.knime.core.data.container.CellFactory;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.IntCell.IntCellFactory;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.LongCell.LongCellFactory;
import org.knime.core.data.time.duration.DurationCell;
import org.knime.core.data.time.duration.DurationCellFactory;
import org.knime.core.data.time.period.PeriodCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.streamable.simple.SimpleStreamableFunctionNodeModel;
import org.knime.core.util.UniqueNameGenerator;
import org.knime.time.node.manipulate.datetimeshift.Granularity;

/**
 * The node model of the node which extracts duration or period fields.
 *
 * @author Simon Schmid, KNIME.com, Konstanz, Germany
 */
public class ExtractDurationPeriodFieldsNodeModel extends SimpleStreamableFunctionNodeModel {

    static final String MODUS_SINGLE = "Single Field";

    static final String MODUS_SEVERAL = "Several Fields";

    private final SettingsModelString m_colSelectModel = createColSelectModel();

    private final SettingsModelString m_modusSelectModel = createModSelectModel();

    private final SettingsModelString m_durationFieldSelectModel = createDurationFieldSelectionModel();

    private final SettingsModelString m_periodFieldSelectModel = createPeriodFieldSelectionModel();

    private final SettingsModelBoolean m_hourModel = createFieldBooleanModel(Granularity.HOUR.toString());

    private final SettingsModelBoolean m_minuteModel = createFieldBooleanModel(Granularity.MINUTE.toString());

    private final SettingsModelBoolean m_secondModel = createFieldBooleanModel(Granularity.SECOND.toString());

    private final SettingsModelBoolean m_nanoModel = createFieldBooleanModel(Granularity.NANOSECOND.toString());

    private final SettingsModelBoolean m_yearModel = createFieldBooleanModel(Granularity.YEAR.toString());

    private final SettingsModelBoolean m_monthModel = createFieldBooleanModel(Granularity.MONTH.toString());

    private final SettingsModelBoolean m_dayModel = createFieldBooleanModel(Granularity.DAY.toString());

    private final SettingsModelBoolean[] m_durModels =
        new SettingsModelBoolean[]{m_hourModel, m_minuteModel, m_secondModel, m_nanoModel};

    private final SettingsModelBoolean[] m_perModels =
        new SettingsModelBoolean[]{m_yearModel, m_monthModel, m_dayModel};

    /** @return the string model, used in both dialog and model. */
    static SettingsModelString createColSelectModel() {
        return new SettingsModelString("col_select", null);
    }

    /** @return the string model, used in both dialog and model. */
    static SettingsModelString createModSelectModel() {
        return new SettingsModelString("modus", MODUS_SINGLE);
    }

    /** @return the string model, used in both dialog and model. */
    static SettingsModelString createDurationFieldSelectionModel() {
        return new SettingsModelString("duration_field_select", Granularity.HOUR.toString());
    }

    /** @return the string model, used in both dialog and model. */
    static SettingsModelString createPeriodFieldSelectionModel() {
        return new SettingsModelString("period_field_select", Granularity.YEAR.toString());
    }

    /** @return the boolean model, used in both dialog and model. */
    static SettingsModelBoolean createFieldBooleanModel(final String key) {
        return new SettingsModelBoolean(key, false);
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

        final boolean isDurationColumn =
            spec.getColumnSpec(m_colSelectModel.getStringValue()).getType().equals(DurationCellFactory.TYPE);
        final List<DataColumnSpec> colSpecs = new ArrayList<>();
        if (m_modusSelectModel.getStringValue().equals(MODUS_SINGLE)) {
            colSpecs.add(
                new UniqueNameGenerator(spec).newColumn((isDurationColumn ? m_durationFieldSelectModel.getStringValue()
                    : m_periodFieldSelectModel.getStringValue()), LongCell.TYPE));
        } else {
            if (isDurationColumn) {
                for (final SettingsModelBoolean model : m_durModels) {
                    if (model.getBooleanValue()) {
                        if (model.equals(m_nanoModel)) {
                            colSpecs.add(new UniqueNameGenerator(spec).newColumn(model.getConfigName(), IntCell.TYPE));
                        } else {
                            colSpecs.add(new UniqueNameGenerator(spec).newColumn(model.getConfigName(), LongCell.TYPE));
                        }
                    }
                }
            } else {
                for (final SettingsModelBoolean model : m_perModels) {
                    if (model.getBooleanValue()) {
                        if (model.getBooleanValue()) {
                            colSpecs.add(new UniqueNameGenerator(spec).newColumn(model.getConfigName(), LongCell.TYPE));
                        }
                    }
                }
            }
        }
        final DataColumnSpec[] colSpecsArray = new DataColumnSpec[colSpecs.size()];
        colSpecs.toArray(colSpecsArray);
        final CellFactory cellFac = new ExtractDurationPeriodFieldsCellFactory(
            spec.findColumnIndex(m_colSelectModel.getStringValue()), colSpecsArray);
        rearranger.append(cellFac);
        return rearranger;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_colSelectModel.saveSettingsTo(settings);
        m_modusSelectModel.saveSettingsTo(settings);
        m_durationFieldSelectModel.saveSettingsTo(settings);
        m_periodFieldSelectModel.saveSettingsTo(settings);
        m_hourModel.saveSettingsTo(settings);
        m_minuteModel.saveSettingsTo(settings);
        m_secondModel.saveSettingsTo(settings);
        m_nanoModel.saveSettingsTo(settings);
        m_yearModel.saveSettingsTo(settings);
        m_monthModel.saveSettingsTo(settings);
        m_dayModel.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_colSelectModel.validateSettings(settings);
        m_modusSelectModel.validateSettings(settings);
        m_durationFieldSelectModel.validateSettings(settings);
        m_periodFieldSelectModel.validateSettings(settings);
        m_hourModel.validateSettings(settings);
        m_minuteModel.validateSettings(settings);
        m_secondModel.validateSettings(settings);
        m_nanoModel.validateSettings(settings);
        m_yearModel.validateSettings(settings);
        m_monthModel.validateSettings(settings);
        m_dayModel.validateSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_colSelectModel.loadSettingsFrom(settings);
        m_modusSelectModel.loadSettingsFrom(settings);
        m_durationFieldSelectModel.loadSettingsFrom(settings);
        m_periodFieldSelectModel.loadSettingsFrom(settings);
        m_hourModel.loadSettingsFrom(settings);
        m_minuteModel.loadSettingsFrom(settings);
        m_secondModel.loadSettingsFrom(settings);
        m_nanoModel.loadSettingsFrom(settings);
        m_yearModel.loadSettingsFrom(settings);
        m_monthModel.loadSettingsFrom(settings);
        m_dayModel.loadSettingsFrom(settings);
    }

    private final class ExtractDurationPeriodFieldsCellFactory extends AbstractCellFactory {

        private final int m_colIdx;

        public ExtractDurationPeriodFieldsCellFactory(final int colIdx, final DataColumnSpec... colSpecs) {
            super(colSpecs);
            m_colIdx = colIdx;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public DataCell[] getCells(final DataRow row) {
            final DataCell[] newCells = new DataCell[getColumnSpecs().length];
            final DataCell cell = row.getCell(m_colIdx);
            if (cell.isMissing()) {
                Arrays.fill(newCells, cell);
                return newCells;
            }
            /*
             * if duration column
             */
            if (cell.getType().equals(DurationCellFactory.TYPE)) {
                final Duration duration = ((DurationCell)cell).getDuration();
                if (m_modusSelectModel.getStringValue().equals(MODUS_SINGLE)) {
                    if (m_durationFieldSelectModel.getStringValue().equals(Granularity.HOUR.toString())) {
                        newCells[0] = LongCellFactory.create(duration.toHours());
                        return newCells;
                    }
                    if (m_durationFieldSelectModel.getStringValue().equals(Granularity.MINUTE.toString())) {
                        newCells[0] = LongCellFactory.create(duration.toMinutes());
                        return newCells;
                    }
                    if (m_durationFieldSelectModel.getStringValue().equals(Granularity.SECOND.toString())) {
                        // Because java.time.Duration stores only positive nanoseconds, one second needs to be
                        // subtracted, if the duration is negative and nanoseconds != 0
                        final long seconds = duration.getSeconds();
                        newCells[0] =
                            LongCellFactory.create((seconds < 0 && duration.getNano() > 0) ? (seconds + 1) : seconds);
                        return newCells;
                    }
                    if (m_durationFieldSelectModel.getStringValue().equals(Granularity.NANOSECOND.toString())) {
                        try {
                            newCells[0] = LongCellFactory.create(duration.toNanos());
                        } catch (ArithmeticException e) {
                            newCells[0] = new MissingCell(e.getMessage());
                            setWarningMessage(
                                "The duration in row '" + row.getKey() + "' was too big to convert to nanoseconds!");
                        }
                        return newCells;
                    }
                    throw new IllegalStateException("Unexpected field: " + m_durationFieldSelectModel.getStringValue());
                } else {
                    int i = 0;
                    for (final SettingsModelBoolean model : m_durModels) {
                        if (model.getBooleanValue()) {
                            if (Granularity.HOUR.toString().equals(model.getConfigName())) {
                                newCells[i] = LongCellFactory.create(duration.getSeconds() / 3600);
                            } else {
                                if (Granularity.MINUTE.toString().equals(model.getConfigName())) {
                                    newCells[i] = LongCellFactory.create((duration.toMinutes()) % (60));
                                } else {
                                    if (Granularity.SECOND.toString().equals(model.getConfigName())) {
                                        // Because java.time.Duration stores only positive nanoseconds, one second needs to be
                                        // subtracted, if the duration is negative and nanoseconds != 0
                                        final long seconds = duration.getSeconds() % 60;
                                        newCells[i] = LongCellFactory
                                            .create((seconds < 0 && duration.getNano() > 0) ? (seconds + 1) : seconds);
                                    } else {
                                        if (Granularity.NANOSECOND.toString().equals(model.getConfigName())) {
                                            final int nano = duration.getNano();
                                            newCells[i] = IntCellFactory.create((duration.getSeconds() < 0 && nano > 0)
                                                ? (nano - 1_000_000_000) : nano);
                                        } else {
                                            throw new IllegalStateException(
                                                "Unexpected field: " + model.getConfigName());
                                        }
                                    }
                                }
                            }
                            i++;
                        }
                    }
                    return newCells;
                }
            }
            /*
             * if period column
             */
            else {
                final Period period = ((PeriodCell)cell).getPeriod();
                if (m_modusSelectModel.getStringValue().equals(MODUS_SINGLE)) {
                    if (m_periodFieldSelectModel.getStringValue().equals(Granularity.YEAR.toString())) {
                        newCells[0] = LongCellFactory.create(period.toTotalMonths() / 12);
                        return newCells;
                    }
                    if (m_periodFieldSelectModel.getStringValue().equals(Granularity.MONTH.toString())) {
                        newCells[0] = LongCellFactory.create(period.toTotalMonths());
                        return newCells;
                    }
                    throw new IllegalStateException("Unexpected field: " + m_periodFieldSelectModel.getStringValue());
                } else {
                    int i = 0;
                    for (final SettingsModelBoolean model : m_perModels) {
                        if (model.getBooleanValue()) {
                            if (Granularity.YEAR.toString().equals(model.getConfigName())) {
                                newCells[i] = LongCellFactory.create(period.getYears());
                            } else {
                                if (Granularity.MONTH.toString().equals(model.getConfigName())) {
                                    newCells[i] = LongCellFactory.create(period.getMonths());
                                } else {
                                    if (Granularity.DAY.toString().equals(model.getConfigName())) {
                                        newCells[i] = LongCellFactory.create(period.getDays());
                                    } else {
                                        throw new IllegalStateException("Unexpected field: " + model.getConfigName());
                                    }
                                }
                            }
                            i++;
                        }
                    }
                    return newCells;
                }
            }
        }

    }
}
