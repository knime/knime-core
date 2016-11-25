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
package org.knime.time.node.manipulate.modifytimezone;

import java.io.File;
import java.io.IOException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

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
import org.knime.core.data.time.localdatetime.LocalDateTimeCell;
import org.knime.core.data.time.localdatetime.LocalDateTimeCellFactory;
import org.knime.core.data.time.localdatetime.LocalDateTimeValue;
import org.knime.core.data.time.zoneddatetime.ZonedDateTimeCell;
import org.knime.core.data.time.zoneddatetime.ZonedDateTimeCellFactory;
import org.knime.core.data.time.zoneddatetime.ZonedDateTimeValue;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
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
import org.knime.core.node.util.filter.column.DataColumnSpecFilterConfiguration;
import org.knime.core.node.util.filter.column.DataTypeColumnFilter;
import org.knime.core.util.UniqueNameGenerator;

/**
 * The node model of the node which modifies a time zone.
 *
 * @author Simon Schmid, KNIME.com, Konstanz, Germany
 */
class ModifyTimeZoneNodeModel extends NodeModel {

    @SuppressWarnings("unchecked")
    static final DataTypeColumnFilter ZONED_AND_LOCAL_FILTER =
        new DataTypeColumnFilter(ZonedDateTimeValue.class, LocalDateTimeValue.class);

    @SuppressWarnings("unchecked")
    static final DataTypeColumnFilter ZONED_FILTER = new DataTypeColumnFilter(ZonedDateTimeValue.class);

    static final String OPTION_APPEND = "Append selected columns";

    static final String OPTION_REPLACE = "Replace selected columns";

    static final String MODIFY_OPTION_SET = "Set time zone";

    static final String MODIFY_OPTION_SHIFT = "Shift time zone";

    static final String MODIFY_OPTION_REMOVE = "Remove time zone";

    private DataColumnSpecFilterConfiguration m_colSelect = createDCFilterConfiguration(ZONED_AND_LOCAL_FILTER);

    private final SettingsModelString m_isReplaceOrAppend = createReplaceAppendStringBool();

    private final SettingsModelString m_suffix = createSuffixModel(m_isReplaceOrAppend);

    private final SettingsModelString m_timeZone = createTimeZoneSelectModel();

    private final SettingsModelString m_modifyAction = createModifySelectModel();

    /**
     * @param typeColumnFilter column filter
     * @return the column select model, used in both dialog and model.
     */
    public static DataColumnSpecFilterConfiguration
        createDCFilterConfiguration(final DataTypeColumnFilter typeColumnFilter) {
        return new DataColumnSpecFilterConfiguration("col_select", typeColumnFilter);
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
        final SettingsModelString suffixModel = new SettingsModelString("suffix", "(modified time zone)");
        replaceOrAppendModel.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent e) {
                if (replaceOrAppendModel.getStringValue().equals(OPTION_APPEND)) {
                    suffixModel.setEnabled(true);
                } else {
                    suffixModel.setEnabled(false);
                }
            }
        });

        suffixModel.setEnabled(false);
        return suffixModel;
    }

    /** @return the string select model, used in both dialog and model. */
    static SettingsModelString createTimeZoneSelectModel() {
        return new SettingsModelString("time_zone_select", ZoneId.systemDefault().getId());
    }

    /** @return the string select model, used in both dialog and model. */
    static SettingsModelString createModifySelectModel() {
        return new SettingsModelString("modify_select", MODIFY_OPTION_SET);
    }

    /**
     * one in, one out
     */
    protected ModifyTimeZoneNodeModel() {
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
        final int[] includeIndeces =
            Arrays.stream(m_colSelect.applyTo(inSpec).getIncludes()).mapToInt(s -> inSpec.findColumnIndex(s)).toArray();
        int i = 0;

        final ZoneId zone = ZoneId.of(m_timeZone.getStringValue());

        DataType dataType;
        if (m_modifyAction.getStringValue().equals(MODIFY_OPTION_REMOVE)) {
            dataType = LocalDateTimeCellFactory.TYPE;
        } else {
            dataType = ZonedDateTimeCellFactory.TYPE;
        }

        for (String includedCol : includeList) {
            if (m_isReplaceOrAppend.getStringValue().equals(OPTION_REPLACE)) {
                final DataColumnSpecCreator dataColumnSpecCreator = new DataColumnSpecCreator(includedCol, dataType);
                final SingleCellFactory cellFac;
                if (m_modifyAction.getStringValue().equals(MODIFY_OPTION_SET)) {
                    cellFac = new SetTimeZoneCellFactory(dataColumnSpecCreator.createSpec(), includeIndeces[i++], zone);
                } else if (m_modifyAction.getStringValue().equals(MODIFY_OPTION_SHIFT)) {
                    cellFac =
                        new ShiftTimeZoneCellFactory(dataColumnSpecCreator.createSpec(), includeIndeces[i++], zone);
                } else {
                    cellFac = new RemoveTimeZoneCellFactory(dataColumnSpecCreator.createSpec(), includeIndeces[i++]);
                }
                rearranger.replace(cellFac, includedCol);
            } else {
                DataColumnSpec dataColSpec =
                    new UniqueNameGenerator(inSpec).newColumn(includedCol + m_suffix.getStringValue(), dataType);
                final SingleCellFactory cellFac;
                if (m_modifyAction.getStringValue().equals(MODIFY_OPTION_SET)) {
                    cellFac = new SetTimeZoneCellFactory(dataColSpec, includeIndeces[i++], zone);
                } else if (m_modifyAction.getStringValue().equals(MODIFY_OPTION_SHIFT)) {
                    cellFac = new ShiftTimeZoneCellFactory(dataColSpec, includeIndeces[i++], zone);
                } else {
                    cellFac = new RemoveTimeZoneCellFactory(dataColSpec, includeIndeces[i++]);
                }
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
                RowInput in = (RowInput)inputs[0];
                RowOutput out = (RowOutput)outputs[0];
                DataTableSpec inSpec = in.getDataTableSpec();
                String[] includeList = m_colSelect.applyTo(inSpec).getIncludes();
                int[] includeIndeces = Arrays.stream(m_colSelect.applyTo(inSpec).getIncludes())
                    .mapToInt(s -> inSpec.findColumnIndex(s)).toArray();
                boolean isReplace = m_isReplaceOrAppend.getStringValue().equals(OPTION_REPLACE);
                SingleCellFactory[] cellFacs = new SingleCellFactory[includeIndeces.length];

                ZoneId zone = ZoneId.of(m_timeZone.getStringValue());

                DataType dataType;
                if (m_modifyAction.getStringValue().equals(MODIFY_OPTION_REMOVE)) {
                    dataType = LocalDateTimeCellFactory.TYPE;
                } else {
                    dataType = ZonedDateTimeCellFactory.TYPE;
                }

                if (isReplace) {
                    for (int i = 0; i < includeIndeces.length; i++) {
                        final DataColumnSpecCreator dataColumnSpecCreator =
                            new DataColumnSpecCreator(includeList[i], dataType);
                        if (m_modifyAction.getStringValue().equals(MODIFY_OPTION_SET)) {
                            cellFacs[i] =
                                new SetTimeZoneCellFactory(dataColumnSpecCreator.createSpec(), includeIndeces[i], zone);
                        } else if (m_modifyAction.getStringValue().equals(MODIFY_OPTION_SHIFT)) {
                            cellFacs[i] = new ShiftTimeZoneCellFactory(dataColumnSpecCreator.createSpec(),
                                includeIndeces[i], zone);
                        } else {
                            cellFacs[i] =
                                new RemoveTimeZoneCellFactory(dataColumnSpecCreator.createSpec(), includeIndeces[i]);
                        }
                    }
                } else {
                    for (int i = 0; i < includeIndeces.length; i++) {
                        final DataColumnSpec dataColSpec = new UniqueNameGenerator(inSpec)
                            .newColumn(includeList[i] + m_suffix.getStringValue(), dataType);
                        if (m_modifyAction.getStringValue().equals(MODIFY_OPTION_SET)) {
                            cellFacs[i] = new SetTimeZoneCellFactory(dataColSpec, includeIndeces[i], zone);
                        } else if (m_modifyAction.getStringValue().equals(MODIFY_OPTION_SHIFT)) {
                            cellFacs[i] = new ShiftTimeZoneCellFactory(dataColSpec, includeIndeces[i], zone);
                        } else {
                            cellFacs[i] = new RemoveTimeZoneCellFactory(dataColSpec, includeIndeces[i]);
                        }
                    }
                }

                DataRow row;
                while ((row = in.poll()) != null) {
                    exec.checkCanceled();
                    final DataCell[] datacells = new DataCell[includeIndeces.length];
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
        m_colSelect.saveConfiguration(settings);
        m_isReplaceOrAppend.saveSettingsTo(settings);
        m_suffix.saveSettingsTo(settings);
        m_timeZone.saveSettingsTo(settings);
        m_modifyAction.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        createDCFilterConfiguration(ZONED_AND_LOCAL_FILTER).loadConfigurationInModelChild(settings);
        m_isReplaceOrAppend.validateSettings(settings);
        m_suffix.validateSettings(settings);
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
        m_timeZone.loadSettingsFrom(settings);
        m_modifyAction.loadSettingsFrom(settings);
        boolean includeLocalDateTime = m_modifyAction.getStringValue().equals(MODIFY_OPTION_SET);
        m_colSelect = createDCFilterConfiguration(includeLocalDateTime ? ZONED_AND_LOCAL_FILTER : ZONED_FILTER);
        m_colSelect.loadConfigurationInModel(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // no internals
    }

    private final class SetTimeZoneCellFactory extends SingleCellFactory {
        private final int m_colIndex;

        private final ZoneId m_zone;

        SetTimeZoneCellFactory(final DataColumnSpec inSpec, final int colIndex, final ZoneId zone) {
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
            } else if (cell instanceof LocalDateTimeCell) {
                return ZonedDateTimeCellFactory
                    .create(ZonedDateTime.of(((LocalDateTimeCell)cell).getLocalDateTime(), m_zone));
            } else if (cell instanceof ZonedDateTimeCell) {
                return ZonedDateTimeCellFactory
                    .create(ZonedDateTime.of(((ZonedDateTimeCell)cell).getZonedDateTime().toLocalDateTime(), m_zone));
            }
            throw new IllegalStateException("Unexpected data type: " + cell.getClass());
        }
    }

    private final class ShiftTimeZoneCellFactory extends SingleCellFactory {
        private final int m_colIndex;

        private final ZoneId m_zone;

        ShiftTimeZoneCellFactory(final DataColumnSpec inSpec, final int colIndex, final ZoneId zone) {
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
            } else if (cell instanceof ZonedDateTimeCell) {
                return ZonedDateTimeCellFactory
                    .create(((ZonedDateTimeCell)cell).getZonedDateTime().toInstant().atZone(m_zone));
            }
            throw new IllegalStateException("Unexpected data type: " + cell.getClass());
        }
    }

    private final class RemoveTimeZoneCellFactory extends SingleCellFactory {
        private final int m_colIndex;

        RemoveTimeZoneCellFactory(final DataColumnSpec inSpec, final int colIndex) {
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
            } else if (cell instanceof ZonedDateTimeCell) {
                return LocalDateTimeCellFactory.create(((ZonedDateTimeCell)cell).getZonedDateTime().toLocalDateTime());
            }
            throw new IllegalStateException("Unexpected data type: " + cell.getClass());
        }
    }
}
