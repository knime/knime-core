/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
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
 *   03.08.2007 (sieb): created
 */
package org.knime.base.node.mine.decisiontree2.learner;

import org.knime.core.node.NodeLogger;

/**
 * Partitions a table according to a given split.
 *
 * @author Christoph Sieb, University of Konstanz
 */
public class Partitioner {

    private static final NodeLogger LOGGER =
            NodeLogger.getLogger(Partitioner.class);

    /**
     * This field holds the resulting partitioning, i.e. the tables resulting
     * from partitioning the input table.
     */
    private InMemoryTable[] m_partitionedTables;

    private boolean m_couldBeUsefullPartitioned;

    private static boolean satisfiesMinNumExamplesPerPartition(
            final InMemoryTable[] tables, final double minNumExamples) {

        int filledPartitions = 0;

        for (InMemoryTable table : tables) {
            if (table.getSumOfWeights() >= minNumExamples) {
                filledPartitions++;
            }
            if (filledPartitions >= 2) {
                return true;
            }
        }

        return false;
    }

    /**
     * Partitions the data table according to the given split.
     *
     * @param table the data table to partition
     * @param split the split according to which the table is split
     * @param minNumExamples a partitioning is only created if there are at
     *            least two partitions with at least minNumExamples examples per
     *            partition
     */
    public Partitioner(final InMemoryTable table, final Split split,
            final double minNumExamples) {
        LOGGER.debug("Perform partitioning.");

        // create the new partition data tables
        boolean useSplitAttributeFurther = split.canBeFurtherUsed();
        int splitAttributeIndex = split.getAttributeIndex();
        InMemoryTable[] partitionTables =
                new InMemoryTable[split.getNumberPartitions()];
        // init the partition tables from the parent table
        for (int i = 0; i < partitionTables.length; i++) {
            partitionTables[i] = new InMemoryTable(table);
            partitionTables[i].setConsiderAttribute(splitAttributeIndex,
                    useSplitAttributeFurther);
        }

        // iterate over the rows and assigne them to the correspondign
        // partition table
        // for the missing values get the partition weights from the split
        double[] partitionWeights = split.getPartitionWeights();
        for (DataRowWeighted row : table) {
            int partitionIndex = split.getPartitionForRow(row);
            if (partitionIndex >= 0) {
                // the split attribute value is not missing
                partitionTables[partitionIndex].addRow(row);
            } else {
                // the split attribute value is missing
                // so add the row to each partition with the weight proportional
                // to the valid number of rows in each partition
                // (this information was collected during split calculation,
                // see "partitionWeights" above)
                for (int i = 0; i < partitionTables.length; i++) {
                    double newWeight = row.getWeight() * partitionWeights[i];
                    partitionTables[i].addRow(new DataRowWeighted(row,
                            newWeight));
                }
            }
        }

        // pack the table
        for (InMemoryTable partitionTable : partitionTables) {
            partitionTable.pack();
        }

        // delete the undelying data row array
        // NOTE: just the array is garbage collected, not the rows itself
        // as they are distributed over the new partition tables
        table.freeUnderlyingDataRows();

        // if there is only one partition filled, i.e. just one
        // class distribution contains all records
        // we do not have to continue to partition, as further splits
        // will not result in a different partition
        // also if there are two or more partitions but there are not at least
        // the given number of min examples per partition
        m_couldBeUsefullPartitioned = true;
        if (!satisfiesMinNumExamplesPerPartition(partitionTables,
                minNumExamples)) {
            m_couldBeUsefullPartitioned = false;
            return;
        }

        m_partitionedTables = partitionTables;
    }

    /**
     * Return the partition tables.
     *
     * @return the partitioned tables.
     * @throws IllegalAccessException thrown if no useful partitioning could be
     *             created, i.e. this is the case if all records fall in one
     *             partition
     */
    public InMemoryTable[] getPartitionTables() throws IllegalAccessException {

        if (m_couldBeUsefullPartitioned) {
            return m_partitionedTables;
        }

        // TODO: debug this should never happen
        throw new IllegalAccessException(
                "No useful partitioning could be created.");
    }

    /**
     * Whether a useful partition could be created. More precisely, there must
     * exist at least two partitions that contain at least a given number of
     * examples (defined in the constructor).
     *
     * @return if true a useful partition could be created and can be get via
     *         "getPartitionedLists"
     */
    public boolean couldBeUsefulPartitioned() {
        return m_couldBeUsefullPartitioned;
    }
}
