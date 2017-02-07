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
 *   Feb 6, 2017 (simon): created
 */
package org.knime.time.node.convert.stringtodurationperiod;

import java.time.format.DateTimeParseException;
import java.util.Arrays;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.MissingCell;
import org.knime.core.data.StringValue;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.time.duration.DurationCellFactory;
import org.knime.core.data.time.period.PeriodCellFactory;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelColumnFilter2;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.streamable.simple.SimpleStreamableFunctionNodeModel;
import org.knime.core.util.UniqueNameGenerator;

/**
 * The node model of the node which converts string cells to period or duration cells.
 *
 * @author Simon Schmid, KNIME.com, Konstanz, Germany
 */
public class StringToDurationPeriodNodeModel extends SimpleStreamableFunctionNodeModel {

    static final String OPTION_APPEND = "Append selected columns";

    static final String OPTION_REPLACE = "Replace selected columns";

    static final String OPTION_DURATION = "Create Duration";

    static final String OPTION_PERIOD = "Create Period";

    private final SettingsModelColumnFilter2 m_colSelect = createColSelectModel();

    private final SettingsModelString m_isReplaceOrAppend = createReplaceAppendStringBool();

    private final SettingsModelString m_suffix = createSuffixModel(m_isReplaceOrAppend);

    private final SettingsModelString m_durationOrPeriod = createDurationOrPeriodSelection();

    private final SettingsModelBoolean m_cancelOnFail = createCancelOnFailModel();

    private int m_failCounter;

    /** @return the column select model, used in both dialog and model. */
    @SuppressWarnings("unchecked")
    public static SettingsModelColumnFilter2 createColSelectModel() {
        return new SettingsModelColumnFilter2("col_select", StringValue.class);
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
        final SettingsModelString suffixModel = new SettingsModelString("suffix", "(Duration/Period)");
        replaceOrAppendModel.addChangeListener(
            e -> suffixModel.setEnabled(replaceOrAppendModel.getStringValue().equals(OPTION_APPEND)));
        suffixModel.setEnabled(false);
        return suffixModel;
    }

    /** @return the string model, used in both dialog and model. */
    public static SettingsModelString createDurationOrPeriodSelection() {
        return new SettingsModelString("duration_or_period", OPTION_DURATION);
    }

    /** @return the boolean model, used in both dialog and model. */
    static SettingsModelBoolean createCancelOnFailModel() {
        return new SettingsModelBoolean("cancel_on_fail", true);
    }

    /** {@inheritDoc} */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
        throws Exception {
        BufferedDataTable in = inData[0];
        ColumnRearranger r = createColumnRearranger(in.getDataTableSpec());
        BufferedDataTable out = exec.createColumnRearrangeTable(in, r, exec);
        if (m_failCounter > 0) {
            setWarningMessage(
                m_failCounter + " rows could not be converted. Check the message in the missing cells for details.");
        }
        return new BufferedDataTable[]{out};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ColumnRearranger createColumnRearranger(final DataTableSpec spec) throws InvalidSettingsException {
        final ColumnRearranger rearranger = new ColumnRearranger(spec);
        final String[] includeList = m_colSelect.applyTo(spec).getIncludes();
        final int[] includeIndices =
            Arrays.stream(m_colSelect.applyTo(spec).getIncludes()).mapToInt(s -> spec.findColumnIndex(s)).toArray();
        int i = 0;

        final DataType dataType;
        if (m_durationOrPeriod.getStringValue().equals(OPTION_DURATION)) {
            dataType = DurationCellFactory.TYPE;
        } else {
            dataType = PeriodCellFactory.TYPE;
        }

        for (String includedCol : includeList) {
            if (m_isReplaceOrAppend.getStringValue().equals(OPTION_REPLACE)) {
                DataColumnSpecCreator dataColumnSpecCreator = new DataColumnSpecCreator(includedCol, dataType);
                StringToDurationPeriodCellFactory cellFac =
                    new StringToDurationPeriodCellFactory(dataColumnSpecCreator.createSpec(), includeIndices[i++]);
                rearranger.replace(cellFac, includedCol);
            } else {
                DataColumnSpec dataColSpec =
                    new UniqueNameGenerator(spec).newColumn(includedCol + m_suffix.getStringValue(), dataType);
                StringToDurationPeriodCellFactory cellFac =
                    new StringToDurationPeriodCellFactory(dataColSpec, includeIndices[i++]);
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
        m_durationOrPeriod.saveSettingsTo(settings);
        m_cancelOnFail.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_colSelect.validateSettings(settings);
        m_isReplaceOrAppend.validateSettings(settings);
        m_suffix.validateSettings(settings);
        m_durationOrPeriod.validateSettings(settings);
        m_cancelOnFail.validateSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_colSelect.loadSettingsFrom(settings);
        m_isReplaceOrAppend.loadSettingsFrom(settings);
        m_suffix.loadSettingsFrom(settings);
        m_durationOrPeriod.loadSettingsFrom(settings);
        m_cancelOnFail.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        m_failCounter = 0;
    }

    private final class StringToDurationPeriodCellFactory extends SingleCellFactory {

        private final DataColumnSpec m_colSpec;

        private final int m_colIndex;

        /**
         * @param newColSpec
         * @param colIndex
         */
        public StringToDurationPeriodCellFactory(final DataColumnSpec newColSpec, final int colIndex) {
            super(newColSpec);
            m_colSpec = newColSpec;
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

            if (m_colSpec.getType().equals(DurationCellFactory.TYPE)) {
                try {
                    return DurationCellFactory.create(((StringValue)cell).getStringValue());
                } catch (DateTimeParseException e) {
                    if (m_cancelOnFail.getBooleanValue()) {
                        throw new IllegalArgumentException(
                            "Failed to parse duration in row '" + row.getKey() + ": " + e.getMessage());
                    }
                    m_failCounter++;
                    return new MissingCell(e.getMessage());
                }
            } else {
                try {
                    return PeriodCellFactory.create(((StringValue)cell).getStringValue());
                } catch (DateTimeParseException e) {
                    if (m_cancelOnFail.getBooleanValue()) {
                        throw new IllegalArgumentException(
                            "Failed to parse period in row '" + row.getKey() + ": " + e.getMessage());
                    }
                    m_failCounter++;
                    return new MissingCell(e.getMessage());
                }
            }
        }

    }

}
