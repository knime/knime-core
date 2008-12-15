/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * -------------------------------------------------------------------
 * 
 * History
 *   Feb 1, 2006 (wiswedel): created
 */
package org.knime.base.node.preproc.rename;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.CellFactory;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;


/**
 * NodeModel implementation for the renaming node.
 * 
 * @author Bernd Wiswedel, University of Konstanz
 */
public class RenameNodeModel extends NodeModel {
    /**
     * Config identifier for the NodeSettings object contained in the
     * NodeSettings which contains the settings.
     */
    public static final String CFG_SUB_CONFIG = "all_columns";

    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(RenameNodeModel.class);

    /** contains settings for each individual column. */
    private RenameColumnSetting[] m_settings;

    /**
     * Create new model, does nothing fancy.
     */
    public RenameNodeModel() {
        super(1, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        NodeSettingsWO subSettings = settings.addNodeSettings(CFG_SUB_CONFIG);
        if (m_settings != null) {
            for (RenameColumnSetting set : m_settings) {
                NodeSettingsWO subSub = subSettings.addNodeSettings(set
                        .getName().toString());
                set.saveSettingsTo(subSub);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        load(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_settings = load(settings);
    }

    /* Reads all settings from a settings object, used by validate and load. */
    private RenameColumnSetting[] load(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        NodeSettingsRO subSettings = settings.getNodeSettings(CFG_SUB_CONFIG);
        ArrayList<RenameColumnSetting> result = 
            new ArrayList<RenameColumnSetting>();
        for (String identifier : subSettings) {
            NodeSettingsRO col = subSettings.getNodeSettings(identifier);
            result.add(RenameColumnSetting.createFrom(col));
        }
        return result.toArray(new RenameColumnSetting[0]);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        BufferedDataTable in = inData[0];
        DataTableSpec inSpec = in.getDataTableSpec();
        final DataTableSpec outSpec = configure(new DataTableSpec[]{inSpec})[0];
        // indices of columns where to use the toString method
        // (for those columns it is not sufficient to just change the column
        // type, we do need to change the cells, too)
        ArrayList<Integer> toStringColumnsIndex = new ArrayList<Integer>();
        ArrayList<DataColumnSpec> toStringColumns = 
            new ArrayList<DataColumnSpec>();
        for (int i = 0; i < inSpec.getNumColumns(); i++) {
            DataType oldType = inSpec.getColumnSpec(i).getType();
            DataType newType = outSpec.getColumnSpec(i).getType();
            // using toString iff new type is String_Type and the old type
            // is not a string type compatible
            boolean useToStringMethod = newType.equals(StringCell.TYPE)
                    && !StringCell.TYPE.isASuperTypeOf(oldType);
            if (useToStringMethod) {
                toStringColumnsIndex.add(i);
                toStringColumns.add(outSpec.getColumnSpec(i));
            }
        }
        // a data table wrapper that returns the "right" DTS (no iteration)
        BufferedDataTable out = exec.createSpecReplacerTable(in, outSpec);
        // depending on whether we have to change some of the columns, we
        // create a ReplacedColumnsTable
        if (!toStringColumnsIndex.isEmpty()) {
            DataColumnSpec[] changedColumns = toStringColumns
                    .toArray(new DataColumnSpec[0]);
            int[] changedColumnsIndex = new int[toStringColumnsIndex.size()];
            for (int i = 0; i < changedColumnsIndex.length; i++) {
                changedColumnsIndex[i] = toStringColumnsIndex.get(i);
            }
            ToStringCellsFactory cellsFactory = new ToStringCellsFactory(
                    changedColumns, changedColumnsIndex);
            ColumnRearranger rearranger = new ColumnRearranger(out.getSpec());
            rearranger.replace(cellsFactory, changedColumnsIndex);
            out = exec.createColumnRearrangeTable(out, rearranger, exec);
        }
        return new BufferedDataTable[]{out};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        DataTableSpec inSpec = inSpecs[0];
        DataColumnSpec[] colSpecs = new DataColumnSpec[inSpec.getNumColumns()];
        HashMap<String, Integer> duplicateHash = new HashMap<String, Integer>();
        for (int i = 0; i < colSpecs.length; i++) {
            DataColumnSpec current = inSpec.getColumnSpec(i);
            String name = current.getName();
            RenameColumnSetting set = findSettings(name);
            DataColumnSpec newColSpec;
            if (set == null) {
                LOGGER.debug("No rename settings for column \"" + name
                        + "\" leaving untouched.");
                newColSpec = current;
            } else {
                newColSpec = set.configure(current);
            }
            String newName = newColSpec.getName();
            if (newName == null || newName.length() == 0) {
                String warnMessage = "Column name at index " + i + " is empty.";
                setWarningMessage(warnMessage);
                throw new InvalidSettingsException(warnMessage);
            }
            Integer duplIndex = duplicateHash.put(newName, i);
            if (duplIndex != null) {
                String warnMessage = "Duplicate column name \"" + newName 
                    + "\" at index " + duplIndex + " and " + i;
                setWarningMessage(warnMessage);
                throw new InvalidSettingsException(warnMessage);
            }
            colSpecs[i] = newColSpec;
        }
        return new DataTableSpec[]{new DataTableSpec(colSpecs)};
    }

    /**
     * Traverses the array m_settings and finds the settings object for a given
     * column.
     * 
     * @param colName The column name.
     * @return The settings to the column (if any), otherwise null.
     */
    private RenameColumnSetting findSettings(final String colName) {
        if (m_settings == null) {
            return null;
        }
        for (RenameColumnSetting set : m_settings) {
            if (set.getName().equals(colName)) {
                return set;
            }
        }
        return null;
    }

    /**
     * Helper class being used to use cell's toString() method to return
     * StringCells.
     */
    private static class ToStringCellsFactory implements CellFactory {
        private final DataColumnSpec[] m_returnSpecs;

        private final int[] m_columns;

        /**
         * Create a new factory.
         * @param returnSpecs the new, replacement spec
         * @param columns column indices to replace
         */
        public ToStringCellsFactory(final DataColumnSpec[] returnSpecs,
                final int[] columns) {
            m_returnSpecs = returnSpecs;
            m_columns = columns;
        }

        /**
         * {@inheritDoc}
         */
        public DataColumnSpec[] getColumnSpecs() {
            return m_returnSpecs;
        }

        /**
         * {@inheritDoc}
         */
        public void setProgress(final int curRowNr, final int rowCount,
                final RowKey lastKey, final ExecutionMonitor exec) {
            exec.setProgress(curRowNr / (double)rowCount, "Changed row "
                    + curRowNr + "/" + rowCount + " (\"" + lastKey + "\")");
        }

        /**
         * {@inheritDoc}
         */
        public DataCell[] getCells(final DataRow row) {
            DataCell[] result = new DataCell[m_columns.length];
            for (int i = 0; i < result.length; i++) {
                DataCell input = row.getCell(m_columns[i]);
                if (input.isMissing()) {
                    result[i] = DataType.getMissingCell();
                } else {
                    String s = input.toString();
                    result[i] = new StringCell(s);
                }
            }
            return result;
        }
    }
}
