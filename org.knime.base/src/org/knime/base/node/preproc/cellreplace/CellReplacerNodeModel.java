/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2009
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
 *  propagated with or for interoperation with KNIME. The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 *
 * History
 *   Aug 7, 2010 (wiswedel): created
 */
package org.knime.base.node.preproc.cellreplace;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.collection.CollectionDataValue;
import org.knime.core.data.container.CellFactory;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelColumnName;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * This is the model implementation of CellReplacer. Replaces cells in a column
 * according to dictionary table (2nd input)
 *
 * @author Bernd Wiswedel
 */
public class CellReplacerNodeModel extends NodeModel {

    /** What should be done when no matching dictionary entry is available. */
    static enum NoMatchPolicy {
        /** Keep the original input (leave cell unmodified). */
        Input,
        /** Set missing value. */
        Missing;
    }

    private final SettingsModelString m_targetColModel;

    private final SettingsModelString m_noMatchPolicyModel;

    private final SettingsModelColumnName m_dictInputColModel;

    private final SettingsModelColumnName m_dictOutputColModel;

    private final SettingsModelBoolean m_appendColumnModel;

    private final SettingsModelString m_appendColumnNameModel;

    /**
     * Constructor for the node model.
     */
    protected CellReplacerNodeModel() {
        super(2, 1);
        m_targetColModel = createTargetColModel();
        m_noMatchPolicyModel = createNoMatchPolicyModel();
        m_dictInputColModel = createDictInputColModel();
        m_dictOutputColModel = createDictOutputColModel();
        m_appendColumnModel = createAppendColumnModel();
        m_appendColumnNameModel =
            createAppendColumnNameModel(m_appendColumnModel);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        BufferedDataTable table = inData[0];
        BufferedDataTable dictionary = inData[1];
        double tableSize = table.getRowCount();
        double dictSize = dictionary.getRowCount();
        double dictCreateAmount = dictSize / (dictSize + tableSize);
        double outputCreateAmount = tableSize / (dictSize + tableSize);
        ColumnRearranger rearranger =
                createColumnRearranger(inData[0].getDataTableSpec(), inData[1]
                        .getDataTableSpec(), inData[1], exec
                        .createSubProgress(dictCreateAmount));

        BufferedDataTable output =
                exec.createColumnRearrangeTable(table, rearranger, exec
                        .createSubProgress(outputCreateAmount));
        return new BufferedDataTable[]{output};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // no internals
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        ColumnRearranger rearranger =
                createColumnRearranger(inSpecs[0], inSpecs[1], null, null);
        return new DataTableSpec[]{rearranger.createSpec()};
    }

    private ColumnRearranger createColumnRearranger(final DataTableSpec spec,
            final DataTableSpec dictSpec, final BufferedDataTable dictTable,
            final ExecutionMonitor dictionaryInitExec)
            throws InvalidSettingsException {
        String targetCol = m_targetColModel.getStringValue();
        if (targetCol == null || targetCol.length() == 0) {
            throw new InvalidSettingsException("No target column selected");
        }
        final int targetColIndex = spec.findColumnIndex(targetCol);
        if (targetColIndex < 0) {
            throw new InvalidSettingsException("No such column \""
                    + targetCol + "\"");
        }
        final DataColumnSpec targetColSpec = spec.getColumnSpec(targetColIndex);

        final int dictInputColIndex =
                dictSpec.findColumnIndex(m_dictInputColModel.getStringValue());
        final boolean dictInputIsCollection;
        if (m_dictInputColModel.useRowID()) {
            dictInputIsCollection = false;
        } else if (dictInputColIndex < 0) {
            throw new InvalidSettingsException("No such column \""
                    + m_dictInputColModel.getStringValue() + "\"");
        } else {
            DataColumnSpec inS = dictSpec.getColumnSpec(dictInputColIndex);
            dictInputIsCollection = inS.getType().isCollectionType();
        }

        final int dictOutputColIndex =
                dictSpec.findColumnIndex(m_dictOutputColModel.getStringValue());
        final DataType dictOutputColType;
        if (m_dictOutputColModel.useRowID()) {
            dictOutputColType = StringCell.TYPE;
        } else {
            if (dictOutputColIndex < 0) {
                throw new InvalidSettingsException("No such column \""
                        + m_dictOutputColModel.getStringValue() + "\"");
            }
            dictOutputColType =
                    dictSpec.getColumnSpec(dictOutputColIndex).getType();
        }
        final NoMatchPolicy noMatchPolicy = getNoMatchPolicy();
        DataType outputType;
        switch (noMatchPolicy) {
        case Input:
            outputType =
                    DataType.getCommonSuperType(dictOutputColType,
                            targetColSpec.getType());
            break;
        default:
            outputType = dictOutputColType;
        }

        String newColName;
        if (m_appendColumnModel.getBooleanValue()) {
            String newName = m_appendColumnNameModel.getStringValue();
            if (newName == null || newName.length() == 0) {
                throw new InvalidSettingsException("No new column name given");
            }
            newColName = DataTableSpec.getUniqueColumnName(spec, newName);
        } else {
            newColName = targetColSpec.getName();
        }

        DataColumnSpecCreator replaceSpecCreator =
                new DataColumnSpecCreator(newColName, outputType);
        CellFactory c = new SingleCellFactory(replaceSpecCreator.createSpec()) {
            private Map<DataCell, DataCell> m_dictionaryMap;

            @Override
            public DataCell getCell(final DataRow row) {
                try {
                    ensureInitDictionaryMap();
                } catch (CanceledExecutionException e) {
                    // cancellation done by the framework
                    return DataType.getMissingCell();
                }
                DataCell cell = row.getCell(targetColIndex);
                DataCell output = m_dictionaryMap.get(cell);
                if (output == null) {
                    switch (noMatchPolicy) {
                    case Input:
                        return cell;
                    default:
                        return DataType.getMissingCell();
                    }
                }
                return output;
            }

            private void ensureInitDictionaryMap()
                    throws CanceledExecutionException {
                if (m_dictionaryMap == null) {
                    m_dictionaryMap = new HashMap<DataCell, DataCell>();
                    int i = 0;
                    double rowCount = dictTable.getRowCount();
                    for (DataRow r : dictTable) {
                        dictionaryInitExec.setProgress((i++) / rowCount,
                                "Reading dictionary into memory, row " + i);
                        dictionaryInitExec.checkCanceled();
                        DataCell output =
                                dictOutputColIndex < 0 ? new StringCell(r
                                        .getKey().getString()) : r
                                        .getCell(dictOutputColIndex);
                        DataCell input =
                                dictInputColIndex < 0 ? new StringCell(r
                                        .getKey().getString()) : r
                                        .getCell(dictInputColIndex);
                        if (input.isMissing()) {
                            addSearchPair(input, output);
                        } else if (dictInputIsCollection) {
                            CollectionDataValue v = (CollectionDataValue)input;
                            for (DataCell element : v) {
                                addSearchPair(element, output);
                            }
                        } else {
                            addSearchPair(input, output);
                        }
                    }
                }
            }

            private void addSearchPair(final DataCell input,
                    final DataCell output) {
                if (m_dictionaryMap.put(input, output) != null) {
                    setWarningMessage("Duplicate search key \"" + input + "\"");
                }
            }
        };
        ColumnRearranger result = new ColumnRearranger(spec);
        if (m_appendColumnModel.getBooleanValue()) {
            result.append(c);
        } else {
            result.replace(c, targetColIndex);
        }
        return result;
    }

    /** @return current no match policy.
     */
    NoMatchPolicy getNoMatchPolicy() {
        return NoMatchPolicy.valueOf(m_noMatchPolicyModel.getStringValue());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_targetColModel.saveSettingsTo(settings);
        m_noMatchPolicyModel.saveSettingsTo(settings);
        m_dictInputColModel.saveSettingsTo(settings);
        m_dictOutputColModel.saveSettingsTo(settings);
        m_appendColumnModel.saveSettingsTo(settings);
        m_appendColumnNameModel.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_targetColModel.loadSettingsFrom(settings);
        m_noMatchPolicyModel.loadSettingsFrom(settings);
        m_dictInputColModel.loadSettingsFrom(settings);
        m_dictOutputColModel.loadSettingsFrom(settings);
        m_appendColumnModel.loadSettingsFrom(settings);
        m_appendColumnNameModel.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_targetColModel.validateSettings(settings);
        SettingsModelString clone =
                m_noMatchPolicyModel.createCloneWithValidatedValue(settings);
        try {
            NoMatchPolicy.valueOf(clone.getStringValue());
        } catch (Exception e) {
            throw new InvalidSettingsException("Invalid policy: "
                    + clone.getStringValue());
        }
        m_dictInputColModel.validateSettings(settings);
        m_dictOutputColModel.validateSettings(settings);
        m_appendColumnModel.validateSettings(settings);
        m_appendColumnNameModel.validateSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // no internals
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // no internals
    }

    /** @return New settings model for target column. */
    static final SettingsModelString createTargetColModel() {
        return new SettingsModelString("targetCol", null);
    }

    /** @return New settings model for no match policy. */
    static final SettingsModelString createNoMatchPolicyModel() {
        return new SettingsModelString("noMatchPolicy", NoMatchPolicy.Missing
                .name());
    }

    /** @return New settings model for no dictionary input column. */
    static final SettingsModelColumnName createDictInputColModel() {
        return new SettingsModelColumnName("dictInputCol", null);
    }

    /** @return New settings model for no dictionary output column. */
    static final SettingsModelColumnName createDictOutputColModel() {
        return new SettingsModelColumnName("dictOutputCol", null);
    }

    /** @return New settings model for append column flag. */
    static final SettingsModelBoolean createAppendColumnModel() {
        return new SettingsModelBoolean("appendColumn", false);
    }

    /** @param appendColumnModel flag model (enable/disable)
     * @return New settings model for append column name. */
    static final SettingsModelString createAppendColumnNameModel(
            final SettingsModelBoolean appendColumnModel) {
        final SettingsModelString result = new SettingsModelString(
                "appendColumnName", "Replacement");
        appendColumnModel.addChangeListener(new ChangeListener() {
            /** {@inheritDoc} */
            @Override
            public void stateChanged(final ChangeEvent e) {
                result.setEnabled(appendColumnModel.getBooleanValue());
            }
        });
        result.setEnabled(appendColumnModel.getBooleanValue());
        return result;
    }

}
