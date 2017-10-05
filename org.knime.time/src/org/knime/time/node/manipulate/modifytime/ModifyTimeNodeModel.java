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
 *   Oct 28, 2016 (simon): created
 */
package org.knime.time.node.manipulate.modifytime;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.time.localdate.LocalDateCellFactory;
import org.knime.core.data.time.localdate.LocalDateValue;
import org.knime.core.data.time.localdatetime.LocalDateTimeCellFactory;
import org.knime.core.data.time.localdatetime.LocalDateTimeValue;
import org.knime.core.data.time.zoneddatetime.ZonedDateTimeCellFactory;
import org.knime.core.data.time.zoneddatetime.ZonedDateTimeValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.streamable.simple.SimpleStreamableFunctionNodeModel;
import org.knime.core.node.util.filter.column.DataColumnSpecFilterConfiguration;
import org.knime.core.node.util.filter.column.DataTypeColumnFilter;
import org.knime.core.util.UniqueNameGenerator;
import org.knime.time.util.SettingsModelDateTime;

/**
 * The node model of the node which modifies time.
 *
 * @author Simon Schmid, KNIME.com, Konstanz, Germany
 */
final class ModifyTimeNodeModel extends SimpleStreamableFunctionNodeModel {

    @SuppressWarnings("unchecked")
    static final DataTypeColumnFilter LOCAL_DATE_FILTER = new DataTypeColumnFilter(LocalDateValue.class);

    @SuppressWarnings("unchecked")
    static final DataTypeColumnFilter DATE_TIME_FILTER =
        new DataTypeColumnFilter(ZonedDateTimeValue.class, LocalDateTimeValue.class);

    static final String OPTION_APPEND = "Append selected columns";

    static final String OPTION_REPLACE = "Replace selected columns";

    static final String MODIFY_OPTION_APPEND = "Append time";

    static final String MODIFY_OPTION_CHANGE = "Change time";

    static final String MODIFY_OPTION_REMOVE = "Remove time";

    private DataColumnSpecFilterConfiguration m_colSelect = createDCFilterConfiguration(LOCAL_DATE_FILTER);

    private final SettingsModelString m_isReplaceOrAppend = createReplaceAppendStringBool();

    private final SettingsModelString m_suffix = createSuffixModel(m_isReplaceOrAppend);

    private final SettingsModelString m_modifyAction = createModifySelectModel();

    private final SettingsModelDateTime m_time = createTimeModel();

    private final SettingsModelDateTime m_timeZone = createTimeZoneModel();

    private boolean m_hasValidatedConfiguration = false;

    /**
     * @param typeColumnFilter column filter
     * @return the column select model, used in both dialog and model.
     */
    static DataColumnSpecFilterConfiguration createDCFilterConfiguration(final DataTypeColumnFilter typeColumnFilter) {
        return new DataColumnSpecFilterConfiguration("col_select", typeColumnFilter);
    }

    /** @return the string model, used in both dialog and model. */
    static SettingsModelString createReplaceAppendStringBool() {
        return new SettingsModelString("replace_or_append", OPTION_REPLACE);
    }

    /**
     * @param replaceOrAppendModel model for the replace/append button group
     * @return the string model, used in both dialog and model.
     */
    static SettingsModelString createSuffixModel(final SettingsModelString replaceOrAppendModel) {
        final SettingsModelString suffixModel = new SettingsModelString("suffix", "(modified time)");
        replaceOrAppendModel.addChangeListener(
            e -> suffixModel.setEnabled(replaceOrAppendModel.getStringValue().equals(OPTION_APPEND)));
        suffixModel.setEnabled(false);
        return suffixModel;
    }

    /** @return the string select model, used in both dialog and model. */
    static SettingsModelString createModifySelectModel() {
        return new SettingsModelString("modify_select", MODIFY_OPTION_APPEND);
    }

    /** @return the date time model, used in both dialog and model. */
    static SettingsModelDateTime createTimeModel(){
        return new SettingsModelDateTime("time", null, LocalTime.now().withNano(0), null);
    }

    /** @return the date time model, used in both dialog and model. */
    static SettingsModelDateTime createTimeZoneModel(){
        return new SettingsModelDateTime("time_zone", null, null, ZoneId.systemDefault());
    }

    /** {@inheritDoc} */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        if (!m_hasValidatedConfiguration) {
            throw new InvalidSettingsException("Node must be configured!");
        }
        DataTableSpec in = inSpecs[0];
        ColumnRearranger r = createColumnRearranger(in);
        DataTableSpec out = r.createSpec();
        return new DataTableSpec[]{out};
    }

    /** {@inheritDoc} */
    @Override
    protected ColumnRearranger createColumnRearranger(final DataTableSpec inSpec) {
        final ColumnRearranger rearranger = new ColumnRearranger(inSpec);
        final String[] includeList = m_colSelect.applyTo(inSpec).getIncludes();
        final int[] includeIndices =
            Arrays.stream(m_colSelect.applyTo(inSpec).getIncludes()).mapToInt(s -> inSpec.findColumnIndex(s)).toArray();

        // determine the data type of output
        DataType dataType;
        if (m_modifyAction.getStringValue().equals(MODIFY_OPTION_REMOVE)) {
            dataType = LocalDateCellFactory.TYPE;
        } else {
            if (m_modifyAction.getStringValue().equals(MODIFY_OPTION_CHANGE)) {
                dataType = LocalDateTimeCellFactory.TYPE;
            } else {
                if (m_timeZone.useZone()) {
                    dataType = ZonedDateTimeCellFactory.TYPE;
                } else {
                    dataType = LocalDateTimeCellFactory.TYPE;
                }
            }
        }

        int i = 0;
        for (final String includedCol : includeList) {
            if (inSpec.getColumnSpec(includedCol).getType().equals(ZonedDateTimeCellFactory.TYPE)
                && m_modifyAction.getStringValue().equals(MODIFY_OPTION_CHANGE)) {
                dataType = ZonedDateTimeCellFactory.TYPE;
            }
            if (m_isReplaceOrAppend.getStringValue().equals(OPTION_REPLACE)) {
                final DataColumnSpecCreator dataColumnSpecCreator = new DataColumnSpecCreator(includedCol, dataType);
                final SingleCellFactory cellFac =
                    createCellFactory(dataColumnSpecCreator.createSpec(), includeIndices[i++], m_timeZone.getZone());
                rearranger.replace(cellFac, includedCol);
            } else {
                final DataColumnSpec dataColSpec =
                    new UniqueNameGenerator(inSpec).newColumn(includedCol + m_suffix.getStringValue(), dataType);
                final SingleCellFactory cellFac = createCellFactory(dataColSpec, includeIndices[i++], m_timeZone.getZone());
                rearranger.append(cellFac);
            }
        }
        return rearranger;
    }

    private SingleCellFactory createCellFactory(final DataColumnSpec dataColSpec, final int index, final ZoneId zone) {
        if (m_modifyAction.getStringValue().equals(MODIFY_OPTION_APPEND)) {
            return new AppendTimeCellFactory(dataColSpec, index, zone);
        } else if (m_modifyAction.getStringValue().equals(MODIFY_OPTION_CHANGE)) {
            return new ChangeTimeCellFactory(dataColSpec, index);
        } else {
            return new RemoveTimeCellFactory(dataColSpec, index);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_colSelect.saveConfiguration(settings);
        m_isReplaceOrAppend.saveSettingsTo(settings);
        m_suffix.saveSettingsTo(settings);
        m_time.saveSettingsTo(settings);
        m_timeZone.saveSettingsTo(settings);
        m_modifyAction.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_isReplaceOrAppend.validateSettings(settings);
        m_suffix.validateSettings(settings);
        m_time.validateSettings(settings);
        m_timeZone.validateSettings(settings);
        m_modifyAction.validateSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_isReplaceOrAppend.loadSettingsFrom(settings);
        m_suffix.loadSettingsFrom(settings);
        m_time.loadSettingsFrom(settings);
        m_timeZone.loadSettingsFrom(settings);
        m_modifyAction.loadSettingsFrom(settings);
        boolean includeLocalDateTime = m_modifyAction.getStringValue().equals(MODIFY_OPTION_APPEND);
        m_colSelect = createDCFilterConfiguration(includeLocalDateTime ? LOCAL_DATE_FILTER : DATE_TIME_FILTER);
        m_colSelect.loadConfigurationInModel(settings);
        m_hasValidatedConfiguration = true;
    }

    private final class AppendTimeCellFactory extends SingleCellFactory {
        private final int m_colIndex;

        private final ZoneId m_zone;

        AppendTimeCellFactory(final DataColumnSpec inSpec, final int colIndex, final ZoneId zone) {
            super(inSpec);
            m_colIndex = colIndex;
            m_zone = zone;
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
            final LocalDateValue localDateCell = (LocalDateValue)cell;
            final LocalTime localTime = m_time.getLocalTime();
            if (m_timeZone.useZone()) {
                return ZonedDateTimeCellFactory
                    .create(ZonedDateTime.of(LocalDateTime.of(localDateCell.getLocalDate(), localTime), m_zone));
            } else {
                return LocalDateTimeCellFactory.create(LocalDateTime.of(localDateCell.getLocalDate(), localTime));
            }
        }
    }

    private final class ChangeTimeCellFactory extends SingleCellFactory {
        private final int m_colIndex;

        ChangeTimeCellFactory(final DataColumnSpec inSpec, final int colIndex) {
            super(inSpec);
            m_colIndex = colIndex;
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
            final LocalTime localTime = m_time.getLocalTime();
            if (cell instanceof LocalDateTimeValue) {
                return LocalDateTimeCellFactory
                    .create(LocalDateTime.of(((LocalDateTimeValue)cell).getLocalDateTime().toLocalDate(), localTime));
            }
            return ZonedDateTimeCellFactory
                .create(ZonedDateTime.of(((ZonedDateTimeValue)cell).getZonedDateTime().toLocalDate(), localTime,
                    ((ZonedDateTimeValue)cell).getZonedDateTime().getZone()));
        }
    }

    private final class RemoveTimeCellFactory extends SingleCellFactory {
        private final int m_colIndex;

        RemoveTimeCellFactory(final DataColumnSpec inSpec, final int colIndex) {
            super(inSpec);
            m_colIndex = colIndex;
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
            if (cell instanceof LocalDateTimeValue) {
                return LocalDateCellFactory.create(((LocalDateTimeValue)cell).getLocalDateTime().toLocalDate());
            }
            return LocalDateCellFactory.create(((ZonedDateTimeValue)cell).getZonedDateTime().toLocalDate());
        }
    }
}
