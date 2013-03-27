/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
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
 * ---------------------------------------------------------------------
 *
 * History
 *   Feb 1, 2008 (wiswedel): created
 */
package org.knime.base.collection.list.split;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowKey;
import org.knime.core.data.collection.BlobSupportDataCellIterator;
import org.knime.core.data.collection.CollectionDataValue;
import org.knime.core.data.container.BlobWrapperDataCell;
import org.knime.core.data.container.CellFactory;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.util.Pair;

/**
 * NodeModel for the collection split node.
 * @author Bernd Wiswedel, University of Konstanz
 */
final class CollectionSplitNodeModel extends NodeModel {

    private final CollectionSplitSettings m_settings;

    /** One input, one output. */
    public CollectionSplitNodeModel() {
        super(1, 1);
        m_settings = new CollectionSplitSettings();
    }

    /** {@inheritDoc} */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        BufferedDataTable table = inData[0];
        DataTableSpec spec = table.getDataTableSpec();
        ExecutionMonitor execForCR = exec;
        getTargetColIndex(spec); // validate settings
        DataColumnSpec[] colSpecs;
        switch (m_settings.getCountElementsPolicy()) {
        case Count:
            execForCR = exec.createSubProgress(0.7);
            ExecutionMonitor e = exec.createSubProgress(0.3);
            colSpecs = countNewColumns(table, e);
            break;
        case UseElementNamesOrFail:
            colSpecs = getColSpecsByElementNames(spec);
            break;
        case BestEffort:
            try {
                colSpecs = getColSpecsByElementNames(spec);
            } catch (InvalidSettingsException ise) {
                execForCR = exec.createSubProgress(0.7);
                e = exec.createSubProgress(0.3);
                colSpecs = countNewColumns(table, e);
            }
            break;
        default: throw new InvalidSettingsException("Unsupported policy: "
                + m_settings.getCountElementsPolicy());
        }
        Pair<ColumnRearranger, SplitCellFactory> pair =
            createColumnRearranger(spec, colSpecs);
        BufferedDataTable out = exec.createColumnRearrangeTable(
                table, pair.getFirst(), execForCR);
        String warnMessage = pair.getSecond().getWarnMessage();
        if (warnMessage != null) {
            setWarningMessage(warnMessage);
        }
        if (m_settings.isDetermineMostSpecificDataType()) {
            out = refineTypes(out, pair.getSecond(), exec);
        }
        return new BufferedDataTable[]{out};
    }

    /** {@inheritDoc} */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        DataTableSpec spec = inSpecs[0];
        if (m_settings.getCollectionColName() == null) {
            m_settings.initDefaults(spec);
            if (m_settings.getCollectionColName() != null) {
                setWarningMessage("Auto-guessed settings, splitting column \""
                        + m_settings.getCollectionColName() + "\"");
            }
        }
        getTargetColIndex(spec); // validate settings
        if (m_settings.isDetermineMostSpecificDataType()) {
            return new DataTableSpec[]{null};
        }
        switch (m_settings.getCountElementsPolicy()) {
        case Count:
            return new DataTableSpec[]{null};
        case UseElementNamesOrFail:
            DataColumnSpec[] colSpecs = getColSpecsByElementNames(spec);
            ColumnRearranger rearranger =
                createColumnRearranger(spec, colSpecs).getFirst();
            return new DataTableSpec[]{rearranger.createSpec()};
        case BestEffort:
            try {
                colSpecs = getColSpecsByElementNames(spec);
                rearranger = createColumnRearranger(spec, colSpecs).getFirst();
                return new DataTableSpec[]{rearranger.createSpec()};
            } catch (InvalidSettingsException ise) {
                return new DataTableSpec[]{null};
            }
        default: throw new InvalidSettingsException("Unsupported policy: "
                + m_settings.getCountElementsPolicy());
        }
    }

    /** Iterate the argument table, determine maximum element count,
     * return freshly created column specs. */
    private DataColumnSpec[] countNewColumns(final BufferedDataTable table,
            final ExecutionMonitor exec) throws InvalidSettingsException,
            CanceledExecutionException {
        DataTableSpec spec = table.getDataTableSpec();
        int i = 0;
        int rowCount = table.getRowCount();
        int max = 0;
        int targetColIndex = getTargetColIndex(spec);
        for (DataRow row : table) {
            DataCell c = row.getCell(targetColIndex);
            if (!c.isMissing()) {
                max = Math.max(((CollectionDataValue)c).size(), max);
            }
            exec.setProgress((i++) / (double)rowCount,
                    "Determining maximum element count, row \"" + row.getKey()
                    + "\" (" + i + "/" + rowCount + ")");
            exec.checkCanceled();
        }
        HashSet<String> hashNames = new HashSet<String>();
        for (DataColumnSpec s : spec) {
            hashNames.add(s.getName());
        }
        if (m_settings.isReplaceInputColumn()) {
            hashNames.remove(spec.getColumnSpec(targetColIndex).getName());
        }
        DataType elementType = spec.getColumnSpec(
                targetColIndex).getType().getCollectionElementType();
        DataColumnSpec[] newColSpec = new DataColumnSpec[max];
        for (int j = 0; j < newColSpec.length; j++) {
            String baseName = "Split Value " + (j + 1);
            String newName = baseName;
            int uniquifier = 1;
            while (!hashNames.add(newName)) {
                newName = baseName + "(#" + (uniquifier++) + ")";
            }
            newColSpec[j] = new DataColumnSpecCreator(
                    baseName, elementType).createSpec();
        }
        return newColSpec;
    }

    /** Validate settings and get the target column index. */
    private int getTargetColIndex(final DataTableSpec spec)
        throws InvalidSettingsException {
        String colName = m_settings.getCollectionColName();
        if (colName == null || colName.length() == 0) {
            throw new InvalidSettingsException("Not configured");
        }
        final int colIndex = spec.findColumnIndex(colName);
        if (colIndex < 0) {
            throw new InvalidSettingsException("No such column: " + colName);
        }
        DataColumnSpec cs = spec.getColumnSpec(colIndex);
        if (!cs.getType().isCompatible(CollectionDataValue.class)) {
            throw new InvalidSettingsException("Column \"" + colName
                    + "\" does not contain collection.");
        }
        return colIndex;
    }

    /** Retype the argument table to use the types as determined by the
     * cell factory. */
    private BufferedDataTable refineTypes(final BufferedDataTable table,
            final SplitCellFactory fac, final ExecutionContext exec) {
        HashMap<String, Integer> colMap = new HashMap<String, Integer>();
        DataTableSpec spec = table.getDataTableSpec();
        DataColumnSpec[] newColSpecs = new DataColumnSpec[spec.getNumColumns()];
        for (int i = 0; i < spec.getNumColumns(); i++) {
            colMap.put(spec.getColumnSpec(i).getName(), i);
            newColSpecs[i] = spec.getColumnSpec(i);
        }
        DataColumnSpec[] oldReplacedSpecs = fac.getColumnSpecs();
        for (int i = 0; i < oldReplacedSpecs.length; i++) {
            DataColumnSpec s = oldReplacedSpecs[i];
            Integer index = colMap.get(s.getName());
            DataColumnSpecCreator creator =
                new DataColumnSpecCreator(newColSpecs[index]);
            creator.setType(fac.getCommonTypes()[i]);
            newColSpecs[index] = creator.createSpec();
        }
        DataTableSpec newSpec = new DataTableSpec(spec.getName(), newColSpecs);
        return exec.createSpecReplacerTable(table, newSpec);
    }

    /** Get new column specs as inferred from the element names in the
     * collection column. */
    private DataColumnSpec[] getColSpecsByElementNames(final DataTableSpec spec)
        throws InvalidSettingsException {
        int colIndex = getTargetColIndex(spec);
        DataColumnSpec colSpec = spec.getColumnSpec(colIndex);
        List<String> elementNames = colSpec.getElementNames();
        if (elementNames.isEmpty()) {
            throw new InvalidSettingsException("Input column \""
                    + colSpec.getName() + "\" does not provide element names; "
                    + "consider to change option in dialog or make sure that"
                    + "the input table contains the necessary information.");
        }
        DataType type = colSpec.getType().getCollectionElementType();
        HashSet<String> hashNames = new HashSet<String>();
        for (DataColumnSpec s : spec) {
            hashNames.add(s.getName());
        }
        if (m_settings.isReplaceInputColumn()) {
            hashNames.remove(colSpec.getName());
        }
        DataColumnSpec[] newColSpec = new DataColumnSpec[elementNames.size()];
        for (int i = 0; i < newColSpec.length; i++) {
            String baseName = elementNames.get(i);
            int uniquifier = 1;
            while (!hashNames.add(baseName)) {
                baseName = elementNames.get(i) + "(#" + (uniquifier++) + ")";
            }
            newColSpec[i] = new DataColumnSpecCreator(
                    baseName, type).createSpec();
        }
        return newColSpec;
    }

    /** Create rearrange object, setup the table. */
    private Pair<ColumnRearranger, SplitCellFactory> createColumnRearranger(
            final DataTableSpec spec, final DataColumnSpec[] newColSpecs)
        throws InvalidSettingsException {
        int colIndex = getTargetColIndex(spec);
        SplitCellFactory fac = new SplitCellFactory(colIndex, newColSpecs);
        ColumnRearranger arranger = new ColumnRearranger(spec);
        if (m_settings.isReplaceInputColumn()) {
            arranger.remove(colIndex);
            arranger.insertAt(colIndex, fac);
        } else {
            arranger.append(fac);
        }
        return new Pair<ColumnRearranger, SplitCellFactory>(arranger, fac);
    }

    /** {@inheritDoc} */
    @Override
    protected void reset() {

    }

    /** {@inheritDoc} */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_settings.saveSettingsTo(settings);
    }

    /** {@inheritDoc} */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        CollectionSplitSettings s = new CollectionSplitSettings();
        s.loadSettingsInModel(settings);
    }

    /** {@inheritDoc} */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_settings.loadSettingsInModel(settings);
    }

    /** {@inheritDoc} */
    @Override
    protected void loadInternals(
            final File nodeInternDir, final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
    }

    /** {@inheritDoc} */
    @Override
    protected void saveInternals(
            final File nodeInternDir, final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
    }

    /** CellFactory being used to split the column. */
    private static final class SplitCellFactory implements CellFactory {

        private final DataColumnSpec[] m_colSpecs;
        private final DataType[] m_commonTypes;
        private final int m_colIndex;
        private String m_warnMessage;

        /** Create new cell factory.
         * @param colIndex Index of collection column
         * @param colSpecs The column specs of the new columns.
         */
        SplitCellFactory(final int colIndex, final DataColumnSpec[] colSpecs) {
            m_commonTypes = new DataType[colSpecs.length];
            m_colSpecs = colSpecs;
            m_colIndex = colIndex;
        }

        /** {@inheritDoc} */
        @Override
        public DataColumnSpec[] getColumnSpecs() {
            return m_colSpecs;
        }

        /** {@inheritDoc} */
        @Override
        public DataCell[] getCells(final DataRow row) {
            DataCell inCell = row.getCell(m_colIndex);
            DataCell[] result = new DataCell[m_colSpecs.length];
            Arrays.fill(result, DataType.getMissingCell());
            if (inCell.isMissing()) {
                if (m_warnMessage == null) {
                    m_warnMessage = "Some rows contain missing values";
                }
                return result;
            }
            CollectionDataValue v = (CollectionDataValue)inCell;
            Iterator<DataCell> it = v.iterator();
            for (int i = 0; i < m_colSpecs.length && it.hasNext(); i++) {
                DataCell next;
                DataType type;
                if (it instanceof BlobSupportDataCellIterator) {
                    next =
                        ((BlobSupportDataCellIterator)it).nextWithBlobSupport();
                    if (next instanceof BlobWrapperDataCell) {
                        // try to not access the cell (will get deserialized)
                        BlobWrapperDataCell bw = (BlobWrapperDataCell)next;
                        type = DataType.getType(bw.getBlobClass());
                    } else {
                        type = next.getType();
                    }
                } else {
                    next = it.next();
                    type = next.getType();
                }
                if (m_commonTypes[i] == null) {
                    m_commonTypes[i] = type;
                } else {
                    m_commonTypes[i] =
                        DataType.getCommonSuperType(m_commonTypes[i], type);
                }
                result[i] = next;
            }
            if (it.hasNext()) {
                m_warnMessage = "At least one row had more elements than "
                    + "specified; row was truncated.";
            }
            return result;
        }

        /** {@inheritDoc} */
        @Override
        public void setProgress(final int curRowNr, final int rowCount,
                final RowKey lastKey, final ExecutionMonitor exec) {
            exec.setProgress(curRowNr / (double)rowCount, "Split row "
                    + curRowNr + " (\"" + lastKey + "\")");
        }

        /** @return the commonTypes */
        public DataType[] getCommonTypes() {
            return m_commonTypes;
        }

        /** @return the warnMessage or null */
        public String getWarnMessage() {
            return m_warnMessage;
        }

    }
}
