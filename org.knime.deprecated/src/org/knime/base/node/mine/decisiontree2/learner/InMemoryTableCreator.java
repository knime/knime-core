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
 * ---------------------------------------------------------------------
 *
 * History
 *   31.07.2007 (sieb): created
 */
package org.knime.base.node.mine.decisiontree2.learner;

import java.util.ArrayList;
import java.util.List;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.NominalValue;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;

/**
 * Creates an in memory representation of the given {@link BufferedDataTable}.
 * The table only includes valid attributes (nominal, numeric and the class
 * attribute).
 *
 * @author Christoph Sieb, University of Konstanz
 */
public class InMemoryTableCreator {

    private final BufferedDataTable m_table;

    private final int m_classColumnIndex;

    private int m_removedRowsDueToMissingClassValue;

    private final double m_minNumberRowsPerNode;

    private final boolean m_skipColumns;

    /**
     * Creates a creator from the given table and the specified class index.
     *
     * @param table the data table from which to create the attribute lists
     * @param classColumnIndex the class column index
     * @param minNumberRowsPerNode the minimum number of nodes per leaf; used to
     *            determine whether this tables distribution of class values is
     *            pure enough
     */
    public InMemoryTableCreator(final BufferedDataTable table,
            final int classColumnIndex, final double minNumberRowsPerNode) {
        this(table, classColumnIndex, minNumberRowsPerNode, false);
    }


    /**
     * Creates a creator from the given table and the specified class index.
     *
     * @param table the data table from which to create the attribute lists
     * @param classColumnIndex the class column index
     * @param minNumberRowsPerNode the minimum number of nodes per leaf; used to
     *            determine whether this tables distribution of class values is
     *            pure enough
     * @param skipColumns true to skip nominal columns that have no domain
     *      values information
     */
    public InMemoryTableCreator(final BufferedDataTable table,
            final int classColumnIndex, final double minNumberRowsPerNode,
            final boolean skipColumns) {
        m_table = table;
        m_classColumnIndex = classColumnIndex;
        m_removedRowsDueToMissingClassValue = 0;
        m_minNumberRowsPerNode = minNumberRowsPerNode;
        m_skipColumns = skipColumns;
    }


    /**
     * Creates the {@link InMemoryTable}.
     *
     * @param exec the {@link ExecutionContext} to report the progress to
     *
     * @return the {@link InMemoryTable}
     * @throws CanceledExecutionException thrown if the creation process is
     *             canceled by the user
     */
    public InMemoryTable createInMemoryTable(final ExecutionContext exec)
            throws CanceledExecutionException {
        exec.checkCanceled();
        exec.setMessage("Prepare In-Memory table creation...");

        // reset to 0
        m_removedRowsDueToMissingClassValue = 0;
        DataTableSpec spec = m_table.getDataTableSpec();

        // get the valid attribute indices
        int[] attributeIndices = getValidColums(spec);

        // create the mapper objects
        ValueMapper<DataCell> classValueMapper = new ValueMapper<DataCell>();
        ValueMapper<String> attributeNameMapper = new ValueMapper<String>();
        @SuppressWarnings("unchecked")
        ValueMapper<DataCell>[] attributeValueMapper =
                new ValueMapper[attributeIndices.length];

        // fill the attribute name mapper and
        // create value mappers for those attributes that are nominal
        int i = 0;
        for (int includedIndex : attributeIndices) {
            DataColumnSpec columnSpec = spec.getColumnSpec(includedIndex);
            attributeNameMapper.getIndexMayBeAdded(columnSpec.getName());
            if (columnSpec.getType().isCompatible(NominalValue.class)) {
                attributeValueMapper[i] = new ValueMapper<DataCell>();
            }
            i++;
        }

        // create the in memory table to which the rows are added
        InMemoryTable resultTable =
                new InMemoryTable(attributeValueMapper, classValueMapper,
                        attributeNameMapper, m_minNumberRowsPerNode);

        // copy the data from the table to the in memory table and update
        // the value mappers
        double counter = 0;
        int numRows = m_table.getRowCount();
        for (org.knime.core.data.DataRow row : m_table) {

            // report progress and check memory footprint
            counter++;
            if (counter % 100 == 0) {
                exec.checkCanceled();
                // check the memory size
                DecisionTreeLearnerNodeModel.checkMemory();
                exec.setProgress(counter / numRows, "Processing row no. "
                                + (int)counter + " of " + numRows);
            }

            // first get the class value
            DataCell classValue = row.getCell(m_classColumnIndex);
            if (classValue.isMissing()) {
                // rows with missing class values are not added to the table
                m_removedRowsDueToMissingClassValue++;
                continue;
            }
            int classMapping = classValueMapper.getIndexMayBeAdded(classValue);
            // then create a double array for the mapped data row
            double[] attributeValues = new double[attributeIndices.length];
            i = 0;
            // for each attribute included, get the data cell create a mapping
            // and put the mapping into the double array (int to double,
            // implicit cast)
            for (int includedIndex : attributeIndices) {
                DataCell value = row.getCell(includedIndex);
                // !!! NOTE: missing values are encoded as not a number (NaN)
                // Thus, explicit numeric values set to NaN are treaded as
                // missing values (assumption in the context of this decision
                // tree)
                if (attributeValueMapper[i] != null) {
                    // the attribute is nominal, i.e. the nominal value
                    // must be mapped by the mapper and put into the double
                    // array
                    if (value.isMissing()) {
                        attributeValues[i] = Double.NaN;
                    } else {
                        attributeValues[i] =
                                attributeValueMapper[i]
                                        .getIndexMayBeAdded(value);
                    }
                } else {
                    // else this attribute is a numeric one
                    // get it as double value and put it direclty into the
                    // double array
                    if (value.isMissing()) {
                        attributeValues[i] = Double.NaN;
                    } else {
                        attributeValues[i] =
                                ((DoubleValue)row.getCell(includedIndex))
                                        .getDoubleValue();
                    }
                }

                i++;
            }
            // create a weighted row from the class value and the double
            // array; the initial weight is 1.0 (i.e. 100%)
            resultTable.addRow(new DataRowWeighted(new ClassValueDataRow(
                    attributeValues, classMapping), 1.0));
        }

        exec.setMessage("Finished creation of In-Memory table");
        resultTable.pack();
        return resultTable;
    }

    /**
     * Returns an array with valid attribute indices.
     *
     * @param spec the {@link DataTableSpec} of the underlying
     *            {@link BufferedDataTable}
     * @return an integer array with the valid attribute indices
     */
    private int[] getValidColums(final DataTableSpec spec) {
        // valid are those columns that are either double values or are nominal
        // values
        List<Integer> result = new ArrayList<Integer>();
        for (int i = 0; i < spec.getNumColumns(); i++) {
            if (i == m_classColumnIndex) {
                continue;
            }
            DataColumnSpec columnSpec = spec.getColumnSpec(i);
            if (columnSpec.getType().isCompatible(DoubleValue.class)) {
                result.add(i);
            } else if (columnSpec.getType().isCompatible(NominalValue.class)) {
                if (!m_skipColumns || columnSpec.getDomain().hasValues()) {
                    result.add(i);
                }
            }
        }
        int[] resultArray = new int[result.size()];
        int i = 0;
        for (Integer value : result) {
            resultArray[i] = value.intValue();
            i++;
        }
        return resultArray;
    }

    /**
     * Returns the number of rows removed during table creation due to missing
     * class values.
     *
     * @return the number of rows removed during table creation due to missing
     *         class values
     */
    public int getRemovedRowsDueToMissingClassValue() {
        return m_removedRowsDueToMissingClassValue;
    }

}
