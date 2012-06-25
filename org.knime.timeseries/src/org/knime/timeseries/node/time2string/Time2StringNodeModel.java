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
 *   28.09.2009 (Fabian Dill): created
 */
package org.knime.timeseries.node.time2string;

import java.text.SimpleDateFormat;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.date.DateAndTimeCell;
import org.knime.core.data.date.DateAndTimeValue;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.streamable.simple.SimpleStreamableFunctionNodeModel;
import org.knime.core.node.util.StringHistory;
import org.knime.timeseries.node.stringtotimestamp.String2DateDialog;

/**
 * Takes a column containing {@link DateAndTimeValue}s and converts them into
 * strings by using a {@link SimpleDateFormat} which can be selected or entered
 * in the dialog.
 *
 * @author Fabian Dill, KNIME.com, Zurich, Switzerland
 */
public class Time2StringNodeModel extends SimpleStreamableFunctionNodeModel {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(
            Time2StringNodeModel.class);

    /** Suffix to append to existing column name as proposal for new col name.
     */
    static final String COL_NAME_SUFFIX = "string";

    private final SettingsModelString m_selectedCol = String2DateDialog
        .createColumnSelectionModel();

    private final SettingsModelString m_newColName = String2DateDialog
        .createColumnNameModel();

    private final SettingsModelBoolean m_replaceCol = String2DateDialog
        .createReplaceModel();

    private final SettingsModelString m_pattern = String2DateDialog
        .createFormatModel();

    /** {@inheritDoc}
     * @since 2.6 */
    @Override
    protected ColumnRearranger createColumnRearranger(
            final DataTableSpec inSpec) throws InvalidSettingsException {
        // check if input has dateandtime column
        if (!inSpec.containsCompatibleType(DateAndTimeValue.class)) {
            throw new InvalidSettingsException(
                    "Input table must contain at least timestamp column!");
        }
        // currently selected column still there?
        String selectedColName = m_selectedCol.getStringValue();
        if (selectedColName != null && !selectedColName.isEmpty()) {
            if (!inSpec.containsName(selectedColName)) {
                throw new InvalidSettingsException(
                        "Column " + selectedColName
                        + " not found in input spec!");
            }
        } else {
            // no value set: auto-configure -> choose first timeseries
            for (DataColumnSpec colSpec : inSpec) {
                if (colSpec.getType().isCompatible(DateAndTimeValue.class)) {
                    String colName = colSpec.getName();
                    m_selectedCol.setStringValue(colName);
                    setWarningMessage("Auto-configure: selected " + colName);
                    break;
                }
            }
        }
        ColumnRearranger rearranger = new ColumnRearranger(inSpec);
        // if replace -> use original column name
        final boolean replace = m_replaceCol.getBooleanValue();
        String colName = DataTableSpec.getUniqueColumnName(inSpec,
                m_newColName.getStringValue());
        if (replace) {
            colName = m_selectedCol.getStringValue();
        }
        DataColumnSpecCreator specCreator = new DataColumnSpecCreator(
                colName, StringCell.TYPE);
        final SimpleDateFormat dateFormat = new SimpleDateFormat(
                m_pattern.getStringValue());
        dateFormat.setTimeZone(DateAndTimeCell.UTC_TIMEZONE);
        final int colIdx = inSpec.findColumnIndex(
                m_selectedCol.getStringValue());
        SingleCellFactory factory = new SingleCellFactory(
                specCreator.createSpec()) {
            @Override
            public DataCell getCell(final DataRow row) {
                DataCell dc = row.getCell(colIdx);
                if (dc.isMissing()) {
                    return DataType.getMissingCell();
                }
                if (dc.getType().isCompatible(DateAndTimeValue.class)) {
                    DateAndTimeValue v = (DateAndTimeValue)dc;
                    String result = dateFormat.format(
                            v.getUTCCalendarClone().getTime());
                    return new StringCell(result);
                }
                LOGGER.error("Encountered unsupported data type: "
                        + dc.getType() + " in row: " + row.getKey());
                return DataType.getMissingCell();
            }
        };
        if (!replace) {
            rearranger.append(factory);
        } else {
            rearranger.replace(factory, m_selectedCol.getStringValue());
        }
        return rearranger;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // nothing to do
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
    throws InvalidSettingsException {
        m_newColName.loadSettingsFrom(settings);
        m_replaceCol.loadSettingsFrom(settings);
        m_selectedCol.loadSettingsFrom(settings);
        m_pattern.loadSettingsFrom(settings);
        String dateFormat = m_pattern.getStringValue();
        // if it is not a predefined one -> store it
        if (!String2DateDialog.PREDEFINED_FORMATS.contains(dateFormat)) {
            StringHistory.getInstance(String2DateDialog.FORMAT_HISTORY_KEY).add(
                    dateFormat);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_newColName.saveSettingsTo(settings);
        m_replaceCol.saveSettingsTo(settings);
        m_selectedCol.saveSettingsTo(settings);
        m_pattern.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_newColName.validateSettings(settings);
        m_replaceCol.validateSettings(settings);
        m_selectedCol.validateSettings(settings);
        SettingsModelBoolean replaceModelClone = m_replaceCol
            .createCloneWithValidatedValue(settings);
        // only if the original column is not replaced we need to check the new
        // column name
        boolean replace = replaceModelClone.getBooleanValue();
        if (replace) {
            return;
        }
        // check for valid and unique column name != null && !empty
        SettingsModelString colNameClone = m_newColName
            .createCloneWithValidatedValue(settings);
        String newColName = colNameClone.getStringValue();
        if (newColName == null || newColName.isEmpty()) {
            throw new InvalidSettingsException(
                    "New column name must not be empty!");
        }

        m_pattern.validateSettings(settings);
        SettingsModelString patternStringModel = m_pattern
            .createCloneWithValidatedValue(settings);
        String patternString = patternStringModel.getStringValue();
        // validate the pattern
        try {
            new SimpleDateFormat(patternString);
        } catch (Exception e) {
            throw new InvalidSettingsException("Pattern " + patternString
                    + " is invalid!");
        }
    }

}
