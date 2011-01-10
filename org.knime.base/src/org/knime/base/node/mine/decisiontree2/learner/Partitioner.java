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
 *   03.08.2007 (sieb): created
 */
package org.knime.base.node.mine.decisiontree2.learner;


/**
 * Partitions a table according to a given split.
 *
 * @author Christoph Sieb, University of Konstanz
 */
public class Partitioner {

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

        // iterate over the rows and assign them to the corresponding
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
