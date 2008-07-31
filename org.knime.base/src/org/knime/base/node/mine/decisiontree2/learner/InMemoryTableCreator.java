/*
 * ------------------------------------------------------------------
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
import org.knime.core.node.NodeLogger;

/**
 * Creates an in memory representation of the given {@link BufferedDataTable}.
 * The table only includes valid attributes (nominal, numeric and the class
 * attribute).
 * 
 * @author Christoph Sieb, University of Konstanz
 */
public class InMemoryTableCreator {

    private static final NodeLogger LOGGER =
            NodeLogger.getLogger(InMemoryTableCreator.class);

    private BufferedDataTable m_table;

    private int m_classColumnIndex;

    private int m_removedRowsDueToMissingClassValue;

    private double m_minNumberRowsPerNode;

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
        m_table = table;
        m_classColumnIndex = classColumnIndex;
        m_removedRowsDueToMissingClassValue = 0;
        m_minNumberRowsPerNode = minNumberRowsPerNode;
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
        LOGGER.info("Start creation of In-Memory table from the input table");

        // reset to 0
        m_removedRowsDueToMissingClassValue = 0;
        DataTableSpec spec = m_table.getDataTableSpec();

        // get the valid attribute indices
        int[] attributeIndices = getValidColums(spec, m_classColumnIndex);

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
                exec
                        .setProgress(counter / numRows, "Performing row "
                                + (int)counter);
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
            resultTable.addRow(new DataRowWeighted(new ClassValueDataRow(attributeValues,
                    classMapping), 1.0));
        }

        LOGGER.info("Finished creation of In-Memory table");
        resultTable.pack();
        return resultTable;
    }

    /**
     * Returns an array with valid attribute indices.
     * 
     * @param spec the {@link DataTableSpec} of the underlying
     *            {@link BufferedDataTable}
     * @param classColumnIndex the class column index which is not included
     * @return an integer array with the valid attribute indices
     */
    private static int[] getValidColums(final DataTableSpec spec,
            final int classColumnIndex) {
        // valid are those columns that are either double values or are nominal
        // values
        List<Integer> result = new ArrayList<Integer>();
        for (int i = 0; i < spec.getNumColumns(); i++) {
            if (i == classColumnIndex) {
                continue;
            }
            DataColumnSpec columnSpec = spec.getColumnSpec(i);
            if (columnSpec.getType().isCompatible(DoubleValue.class)) {
                result.add(i);
            } else if (columnSpec.getType().isCompatible(NominalValue.class)) {
                result.add(i);
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
