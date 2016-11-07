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
 *   Oct 28, 2016 (simon): created
 */
package org.knime.time.node.manipulate.adddate;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;

import org.knime.base.data.replace.ReplacedColumnsDataRow;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.append.AppendedColumnRow;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.time.localdatetime.LocalDateTimeCellFactory;
import org.knime.core.data.time.localtime.LocalTimeCell;
import org.knime.core.data.time.localtime.LocalTimeValue;
import org.knime.core.data.time.zoneddatetime.ZonedDateTimeCellFactory;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelColumnFilter2;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.streamable.InputPortRole;
import org.knime.core.node.streamable.OutputPortRole;
import org.knime.core.node.streamable.PartitionInfo;
import org.knime.core.node.streamable.PortInput;
import org.knime.core.node.streamable.PortOutput;
import org.knime.core.node.streamable.RowInput;
import org.knime.core.node.streamable.RowOutput;
import org.knime.core.node.streamable.StreamableOperator;
import org.knime.core.util.UniqueNameGenerator;

/**
 * The node model of the node which adds a date to a time cell.
 *
 * @author Simon Schmid, KNIME.com, Konstanz, Germany
 */
class AddDateNodeModel extends NodeModel {

    static final String OPTION_APPEND = "Append selected columns";

    static final String OPTION_REPLACE = "Replace selected columns";

    private final SettingsModelColumnFilter2 m_colSelect = createColSelectModel();

    private final SettingsModelString m_isReplaceOrAppend = createReplaceAppendStringBool();

    private final SettingsModelString m_suffix = createSuffixModel(m_isReplaceOrAppend);

    private final SettingsModelInteger m_year = createYearModel();

    private final SettingsModelString m_month = createMonthModel();

    private final SettingsModelInteger m_day = createDayModel();

    private final SettingsModelBoolean m_addZone = createZoneModelBool();

    private final SettingsModelString m_timeZone = createTimeZoneSelectModel(m_addZone);

    /** @return the column select model, used in both dialog and model. */
    @SuppressWarnings("unchecked")
    public static SettingsModelColumnFilter2 createColSelectModel() {
        return new SettingsModelColumnFilter2("col_select", LocalTimeValue.class);
    }

    /** @return the string model, used in both dialog and model. */
    public static SettingsModelString createReplaceAppendStringBool() {
        return new SettingsModelString("replace_or_append", OPTION_REPLACE);
    }

    /**
     * @param replaceOrAppendModel model for the replace/append button group
     * @return the string model, used in both dialog and model.
     */
    public static SettingsModelString createSuffixModel(final SettingsModelString replaceOrAppendModel) {
        final SettingsModelString suffixModel = new SettingsModelString("suffix", "(with date)");
        replaceOrAppendModel.addChangeListener(
            e -> suffixModel.setEnabled(replaceOrAppendModel.getStringValue().equals(OPTION_APPEND)));
        suffixModel.setEnabled(false);
        return suffixModel;
    }

    /** @return the integer model, used in both dialog and model. */
    public static SettingsModelIntegerBounded createYearModel() {
        return new SettingsModelIntegerBounded("year", LocalDate.now().getYear(), 0, Integer.MAX_VALUE);
    }

    /** @return the string model, used in both dialog and model. */
    public static SettingsModelString createMonthModel() {
        return new SettingsModelString("month", LocalDate.now().getMonth().toString());
    }

    /** @return the integer model, used in both dialog and model. */
    public static SettingsModelIntegerBounded createDayModel() {
        return new SettingsModelIntegerBounded("day", LocalDate.now().getDayOfMonth(), 0, 31);
    }

    /** @return the boolean model, used in both dialog and model. */
    static SettingsModelBoolean createZoneModelBool() {
        return new SettingsModelBoolean("zone_bool", false);
    }

    /** @return the string select model, used in both dialog and model. */
    static SettingsModelString createTimeZoneSelectModel(final SettingsModelBoolean zoneModelBool) {
        final SettingsModelString zoneSelectModel =
            new SettingsModelString("time_zone_select", ZoneId.systemDefault().getId());
        zoneSelectModel.setEnabled(false);
        zoneModelBool.addChangeListener(e -> zoneSelectModel.setEnabled(zoneModelBool.getBooleanValue()));
        return zoneSelectModel;
    }

    /**
     * one in, one out
     */
    protected AddDateNodeModel() {
        super(1, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        final ColumnRearranger columnRearranger = createColumnRearranger(inSpecs[0]);
        return new DataTableSpec[]{columnRearranger.createSpec()};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
        throws Exception {
        final ColumnRearranger columnRearranger = createColumnRearranger(inData[0].getDataTableSpec());
        final BufferedDataTable out = exec.createColumnRearrangeTable(inData[0], columnRearranger, exec);
        return new BufferedDataTable[]{out};
    }

    /**
     * @param inSpec table input spec
     * @return the CR describing the output
     */
    private ColumnRearranger createColumnRearranger(final DataTableSpec inSpec) {
        final ColumnRearranger rearranger = new ColumnRearranger(inSpec);
        final String[] includeList = m_colSelect.applyTo(inSpec).getIncludes();
        final int[] includeIndices =
            Arrays.stream(m_colSelect.applyTo(inSpec).getIncludes()).mapToInt(s -> inSpec.findColumnIndex(s)).toArray();
        int i = 0;
        final DataType dataType;
        if (m_addZone.getBooleanValue()) {
            dataType = ZonedDateTimeCellFactory.TYPE;
        } else {
            dataType = LocalDateTimeCellFactory.TYPE;
        }

        Month month = Month.valueOf(m_month.getStringValue().toUpperCase());
        ZoneId zone = ZoneId.of(m_timeZone.getStringValue());

        for (String includedCol : includeList) {
            if (m_isReplaceOrAppend.getStringValue().equals(OPTION_REPLACE)) {
                DataColumnSpecCreator dataColumnSpecCreator = new DataColumnSpecCreator(includedCol, dataType);
                AddDateCellFactory cellFac =
                    new AddDateCellFactory(dataColumnSpecCreator.createSpec(), includeIndices[i++], month, zone);
                rearranger.replace(cellFac, includedCol);
            } else {
                DataColumnSpec dataColSpec =
                    new UniqueNameGenerator(inSpec).newColumn(includedCol + m_suffix.getStringValue(), dataType);
                AddDateCellFactory cellFac = new AddDateCellFactory(dataColSpec, includeIndices[i++], month, zone);
                rearranger.append(cellFac);
            }
        }
        return rearranger;
    }

    /** {@inheritDoc} */
    @Override
    public InputPortRole[] getInputPortRoles() {
        return new InputPortRole[]{InputPortRole.DISTRIBUTED_STREAMABLE};
    }

    /** {@inheritDoc} */
    @Override
    public OutputPortRole[] getOutputPortRoles() {
        return new OutputPortRole[]{OutputPortRole.DISTRIBUTED};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StreamableOperator createStreamableOperator(final PartitionInfo partitionInfo,
        final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        return new StreamableOperator() {
            @Override
            public void runFinal(final PortInput[] inputs, final PortOutput[] outputs, final ExecutionContext exec)
                throws Exception {
                final RowInput in = (RowInput)inputs[0];
                final RowOutput out = (RowOutput)outputs[0];
                final DataTableSpec inSpec = in.getDataTableSpec();
                String[] includeList = m_colSelect.applyTo(inSpec).getIncludes();
                int[] includeIndeces = Arrays.stream(m_colSelect.applyTo(inSpec).getIncludes())
                    .mapToInt(s -> inSpec.findColumnIndex(s)).toArray();
                boolean isReplace = m_isReplaceOrAppend.getStringValue().equals(OPTION_REPLACE);
                DataType dataType;
                if (m_addZone.getBooleanValue()) {
                    dataType = ZonedDateTimeCellFactory.TYPE;
                } else {
                    dataType = LocalDateTimeCellFactory.TYPE;
                }

                Month month = Month.valueOf(m_month.getStringValue().toUpperCase());
                ZoneId zone = ZoneId.of(m_timeZone.getStringValue());

                AddDateCellFactory[] cellFacs = new AddDateCellFactory[includeIndeces.length];
                if (isReplace) {
                    for (int i = 0; i < includeIndeces.length; i++) {
                        DataColumnSpecCreator dataColumnSpecCreator =
                            new DataColumnSpecCreator(includeList[i], dataType);
                        cellFacs[i] =
                            new AddDateCellFactory(dataColumnSpecCreator.createSpec(), includeIndeces[i], month, zone);
                    }
                } else {
                    for (int i = 0; i < includeIndeces.length; i++) {
                        DataColumnSpec dataColSpec = new UniqueNameGenerator(inSpec)
                            .newColumn(includeList[i] + m_suffix.getStringValue(), dataType);
                        cellFacs[i] = new AddDateCellFactory(dataColSpec, includeIndeces[i], month, zone);
                    }
                }

                DataRow row;
                while ((row = in.poll()) != null) {
                    exec.checkCanceled();
                    DataCell[] datacells = new DataCell[includeIndeces.length];
                    for (int i = 0; i < includeIndeces.length; i++) {
                        if (isReplace) {
                            datacells[i] = cellFacs[i].getCell(row);
                        } else {
                            datacells[i] = cellFacs[i].getCell(row);
                        }
                    }
                    if (isReplace) {
                        out.push(new ReplacedColumnsDataRow(row, datacells, includeIndeces));
                    } else {
                        out.push(new AppendedColumnRow(row, datacells));
                    }
                }
                in.close();
                out.close();
            }
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // no internals
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // no internals
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_colSelect.saveSettingsTo(settings);
        m_isReplaceOrAppend.saveSettingsTo(settings);
        m_suffix.saveSettingsTo(settings);
        m_year.saveSettingsTo(settings);
        m_month.saveSettingsTo(settings);
        m_day.saveSettingsTo(settings);
        m_addZone.saveSettingsTo(settings);
        m_timeZone.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_colSelect.validateSettings(settings);
        m_isReplaceOrAppend.validateSettings(settings);
        m_suffix.validateSettings(settings);
        m_year.validateSettings(settings);
        m_month.validateSettings(settings);
        m_day.validateSettings(settings);
        m_addZone.validateSettings(settings);
        m_timeZone.validateSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_colSelect.loadSettingsFrom(settings);
        m_isReplaceOrAppend.loadSettingsFrom(settings);
        m_suffix.loadSettingsFrom(settings);
        m_year.loadSettingsFrom(settings);
        m_month.loadSettingsFrom(settings);
        m_day.loadSettingsFrom(settings);
        m_addZone.loadSettingsFrom(settings);
        m_timeZone.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // no internals
    }

    private final class AddDateCellFactory extends SingleCellFactory {
        private final int m_colIndex;

        private final ZoneId m_zone;

        private final Month m_monthValue;

        AddDateCellFactory(final DataColumnSpec inSpec, final int colIndex, final Month month, final ZoneId zone) {
            super(inSpec);
            m_colIndex = colIndex;
            m_monthValue = month;
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
            final LocalTimeCell localTimeCell = (LocalTimeCell)cell;
            final LocalDate localDate = LocalDate.of(m_year.getIntValue(), m_monthValue, m_day.getIntValue());
            if (m_addZone.getBooleanValue()) {
                return ZonedDateTimeCellFactory
                    .create(ZonedDateTime.of(LocalDateTime.of(localDate, localTimeCell.getLocalTime()), m_zone));
            } else {
                return LocalDateTimeCellFactory.create(LocalDateTime.of(localDate, localTimeCell.getLocalTime()));
            }
        }
    }
}
