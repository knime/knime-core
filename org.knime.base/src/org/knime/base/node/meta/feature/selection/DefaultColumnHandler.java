/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME AG, Zurich, Switzerland
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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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
 *   31.05.2016 (Adrian Nembach): created
 */
package org.knime.base.node.meta.feature.selection;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.util.Pair;

/**
 * Handles ordinary columns as well as vector and collection columns.
 * Vector and collection columns are counted as single features (they are <b>NOT</b> split up).
 *
 * @author Adrian Nembach, KNIME.com
 */
class DefaultColumnHandler extends AbstractColumnHandler {

    private final List<String> m_columnNames;

    /**
     * @param constantColumns
     * @param inSpec
     */
    protected DefaultColumnHandler(final List<String> constantColumns, final DataTableSpec inSpec) {
        super(constantColumns);
        // columns are either constant or not
        m_columnNames = new ArrayList<>(inSpec.getNumColumns() - constantColumns.size());
        for (final DataColumnSpec colSpec : inSpec) {
            final String colName = colSpec.getName();
            if (!constantColumns.contains(colSpec.getName())) {
                m_columnNames.add(colName);
            }
        }
    }

    /**
     * Constructor for deserialization. DO NOT use for any other purpose.
     * @param staticColumns
     * @param inStream
     *
     * @throws IOException
     */
    protected DefaultColumnHandler(final List<String> staticColumns, final DataInputStream inStream)
        throws IOException {
        super(staticColumns);
        int colNamesSize = inStream.readInt();
        final List<String> columnNames = new ArrayList<>(colNamesSize);
        for (int i = 0; i < colNamesSize; i++) {
            columnNames.add(inStream.readUTF());
        }
        m_columnNames = columnNames;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BufferedDataTable[] getTables(final ExecutionContext exec, final BufferedDataTable[] inTables,
        final Collection<Integer> included, final boolean includeConstantColumns) throws CanceledExecutionException {
        final DataTableSpec inSpec = inTables[0].getDataTableSpec();
        final ColumnRearranger cr = createRearranger(included, inSpec, includeConstantColumns);
        final int numTables = inTables.length;
        final BufferedDataTable[] out = new BufferedDataTable[numTables];
        final double subProg = 1.0 / numTables;
        for (int i = 0; i < numTables; i++) {
            out[i] = exec.createColumnRearrangeTable(inTables[i], cr, exec.createSubProgress(subProg));
        }
        return out;
    }

    private ColumnRearranger createRearranger(final Collection<Integer> includedFeatures, final DataTableSpec inSpec,
        final boolean includeConstantColumns) {
        final ColumnRearranger cr = new ColumnRearranger(inSpec);
        final Collection<String> included = getIncludedColumns(includedFeatures, includeConstantColumns);
        // remove all columns that are not part of the feature level
        // (using cr.keepOnly would throw an exception if one of the included columns is missing in inSpec)
        for (final String colName : inSpec.getColumnNames()) {
            if (!included.contains(colName)) {
                cr.remove(colName);
            }
        }
        return cr;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Integer> getAvailableFeatures() {
        return IntStream.range(0, m_columnNames.size()).mapToObj(Integer::valueOf).collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<String> getIncludedColumns(final Collection<Integer> features, final boolean includeConstantColumns) {
        final List<String> included = new ArrayList<>();
        if (includeConstantColumns) {
            included.addAll(getConstantColumns());
        }
        features.stream().forEach(i -> included.add(m_columnNames.get(i)));
        return included;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataTableSpec getOutSpec(final List<Integer> features, final DataTableSpec inSpec,
        final boolean includeConstantColumns) {
        return createRearranger(features, inSpec, includeConstantColumns).createSpec();
    }

    /**
     * {@inheritDoc}
     *
     * @throws IOException
     */
    @Override
    protected void saveData(final DataOutputStream outputStream) throws IOException {
        outputStream.writeInt(m_columnNames.size());
        for (final String colName : m_columnNames) {
            outputStream.writeUTF(colName);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<String> getColumnNamesFor(final Collection<Integer> features) {
        return features.stream().map(i -> m_columnNames.get(i)).collect(Collectors.toList());
    }


    /**
     * {@inheritDoc}
     * @throws InvalidSettingsException
     */
    @Override
    public Pair<String, DataTableSpec> getOutSpecAndWarning(final Collection<Integer> features, final DataTableSpec inSpec,
        final boolean includeConstantColumns) throws InvalidSettingsException {
        Collection<String> colNames = getIncludedColumns(features, includeConstantColumns);
        int missing = 0;
        String missingTxt = "The following columns used in the selected  level are missing in the input table: ";
        for (String colName : colNames) {
            if (!inSpec.containsName(colName)) {
                missing++;
                missingTxt += colName + ", ";
            }
        }
        final String warning;
        final int numCols = includeConstantColumns ? features.size() + 1 : features.size();
        if (numCols > 0 && missing == numCols) {
            throw new InvalidSettingsException("Input table does not contain "
                    + "any of the columns used in the feature selection ");
        } else if (missing > 0) {
            warning = missingTxt.substring(0, missingTxt.length() - 2);
        } else {
            warning = null;
        }

        final ColumnRearranger cr = createRearranger(features, inSpec, includeConstantColumns);
        return new Pair<String, DataTableSpec>(warning, cr.createSpec());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getColumnNameFor(final Integer feature) {
        return m_columnNames.get(feature);
    }

}
