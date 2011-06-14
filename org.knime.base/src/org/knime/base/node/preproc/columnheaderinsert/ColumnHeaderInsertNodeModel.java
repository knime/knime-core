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
 *  propagated with or for interoperation with KNIME. The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 *
 */
package org.knime.base.node.preproc.columnheaderinsert;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowKey;
import org.knime.core.data.StringValue;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
final class ColumnHeaderInsertNodeModel extends NodeModel {

    private ColumnHeaderInsertConfig m_config;

    /**  Two ins, one out. */
    public ColumnHeaderInsertNodeModel() {
        super(2, 1);
    }

    /** {@inheritDoc} */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        if (m_config == null) {
            throw new InvalidSettingsException("No configuration available");
        }
        DataTableSpec dictTable = inSpecs[1];
        String lookupCol = m_config.getLookupColumn();
        if (lookupCol != null) {
            DataColumnSpec lookupColSpec = dictTable.getColumnSpec(lookupCol);
            if (lookupColSpec == null) {
                throw new InvalidSettingsException("No such lookup column in "
                        + "dictionary table (2nd input): " + lookupCol);
            }
            if (!lookupColSpec.getType().isCompatible(StringValue.class)) {
                throw new InvalidSettingsException("Lookup column \""
                        + lookupCol + "\" is not string compatible: "
                        + lookupColSpec.getType());
            }
        } else {
            // use row key column
        }
        String valueColumn = m_config.getValueColumn();
        DataColumnSpec valueColumnSpec = dictTable.getColumnSpec(valueColumn);
        if (valueColumnSpec == null) {
            throw new InvalidSettingsException("No such value column in "
                    + "dictionary table (2nd input): " + lookupCol);
        }
        if (!valueColumnSpec.getType().isCompatible(StringValue.class)) {
            throw new InvalidSettingsException("Value column \""
                    + valueColumn + "\" is not string compatible: "
                    + valueColumnSpec.getType());
        }
        return null;
    }

    /** {@inheritDoc} */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        // init name map
        LinkedHashMap<String, String> dictionaryMap =
            new LinkedHashMap<String, String>();
        DataTableSpec dataSpec = inData[0].getDataTableSpec();
        for (DataColumnSpec dataCol : dataSpec) {
            dictionaryMap.put(dataCol.getName(), null);
        }

        // read dictionary
        BufferedDataTable dictionaryTable = inData[1];
        DataTableSpec dictionaryTableSpec = dictionaryTable.getDataTableSpec();
        String lookupColumn = m_config.getLookupColumn();
        int lookupColIdx = lookupColumn == null ? -1
                : dictionaryTableSpec.findColumnIndex(lookupColumn);
        String valueColumnIdx = m_config.getValueColumn();
        int valueColIndex = dictionaryTableSpec.findColumnIndex(valueColumnIdx);
        int rowIndex = 0;
        final int rowCount = dictionaryTable.getRowCount();
        for (DataRow row : dictionaryTable) {
            RowKey key = row.getKey();
            exec.setProgress(rowIndex / (double)rowCount, "Reading dictionary, "
                + "row \"" + key + "\" (" + rowIndex + "/" + rowCount + ")");
            rowIndex += 1;
            String lookup;
            if (lookupColIdx < 0) {
                lookup = row.getKey().getString();
            } else {
                DataCell c = row.getCell(lookupColIdx);
                lookup = c.isMissing() ? null
                        : ((StringValue)c).getStringValue();
            }
            if (!dictionaryMap.containsKey(lookup)) {
                continue;
            }
            DataCell valueCell = row.getCell(valueColIndex);
            // if missing, assign original column name
            String value = valueCell.isMissing() ? lookup
                    : ((StringValue)valueCell).getStringValue();
            if (dictionaryMap.put(lookup, value) != null) {
                throw new Exception("Multiple occurrences of lookup key \""
                        + lookup + "\" in dictionary table; consider to remove "
                        + "duplicates using, e.g. the GroupBy node.");
            }
        }

        // check consistency in new column name values
        HashSet<String> uniqNames = new HashSet<String>();
        for (Map.Entry<String, String> e : dictionaryMap.entrySet()) {
            String value = e.getValue();
            if (value == null) {
                if (m_config.isFailIfNoMatch()) {
                    throw new Exception("No name assignment for column \""
                            + e.getKey() + "\" -- set the appropriate option "
                            + "in the configuration dialog to keep the "
                            + "original column name.");
                } else {
                    value = e.getKey(); // (try to) keep original name
                }
            }
            String newName = value;
            int unifier = 1;
            while (!uniqNames.add(newName)) {
                newName = value + " (#" + (unifier++) + ")";
            }
            e.setValue(newName);
        }

        // assign new names
        DataColumnSpec[] cols = new DataColumnSpec[dataSpec.getNumColumns()];
        for (int i = 0; i < cols.length; i++) {
            DataColumnSpec c = dataSpec.getColumnSpec(i);
            DataColumnSpecCreator creator = new DataColumnSpecCreator(c);
            creator.setName(dictionaryMap.get(c.getName()));
            cols[i] = creator.createSpec();
        }
        DataTableSpec outSpec = new DataTableSpec(dataSpec.getName(), cols);
        BufferedDataTable outTable =
            exec.createSpecReplacerTable(inData[0], outSpec);
        return new BufferedDataTable[] {outTable};
    }

    /** {@inheritDoc} */
    @Override
    protected void reset() {
        // no internals
    }

    /** {@inheritDoc} */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        if (m_config != null) {
            m_config.saveConfiguration(settings);
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        new ColumnHeaderInsertConfig().loadConfigurationInModel(settings);
    }

    /** {@inheritDoc} */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        ColumnHeaderInsertConfig c = new ColumnHeaderInsertConfig();
        c.loadConfigurationInModel(settings);
        m_config = c;
    }

    /** {@inheritDoc} */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // no internals
    }

    /** {@inheritDoc} */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // no internals
    }

}
