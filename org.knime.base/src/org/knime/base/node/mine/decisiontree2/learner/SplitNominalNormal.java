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
 *   02.08.2007 (sieb): created
 */
package org.knime.base.node.mine.decisiontree2.learner;

/**
 * This class determines the best split for a nominal attribute. The split is
 * performed by creating one partition for each nominal value, i.e. the
 * branching degree of the tree.
 *
 * @author Christoph Sieb, University of Konstanz
 */
public class SplitNominalNormal extends SplitNominal {

    /**
     * The partition count for the valid (non-missing) nominal values.
     */
    private double[] m_valuePartitionValidCount;

    /**
     * The count for all valid (non-missing) nominal values.
     */
    private double m_alloverValidCount;

    /**
     * Constructs the best split for the given nominal attribute. The results
     * can be retrieved from getter methods.
     *
     * @param table the attribute list for which to create the split
     * @param attributeIndex the index of the attribute for which to calculate
     *            the split
     * @param splitQualityMeasure the split quality measure (e.g. gini or gain
     *            ratio)
     * @param minObjectsCount the minimumn number of objects in at least two
     *            partitions
     */
    public SplitNominalNormal(final InMemoryTable table,
            final int attributeIndex,
            final SplitQualityMeasure splitQualityMeasure,
            final double minObjectsCount) {

        super(table, attributeIndex, splitQualityMeasure);

        assert table.isNominal(attributeIndex);

        // get the counting histogram for this nominal attribute
        NominalValueHistogram nominalHistogram =
                table.getNominalValueHistogram(attributeIndex);
        double missingValueCount = nominalHistogram.getMissingValueCount();
        // get the underlying basic 2D array
        double[][] histogram = nominalHistogram.getHistogram();

        // calculate the partition count array and the allover count
        double alloverCount = 0.0;
        double[] partitionCounter = new double[histogram.length];
        double[] classCounter =
                new double[table.getClassFrequencyArray().length];
        int partitionIndex = 0;
        for (double[] partition : histogram) {
            int j = 0;
            for (double classCount : partition) {
                partitionCounter[partitionIndex] += classCount;
                classCounter[j] += classCount;
                alloverCount += classCount;
                j++;
            }
            partitionIndex++;
        }

        // init the quality measure
        m_splitQualityMeasure.initQualityMeasure(classCounter, alloverCount);

        // also check if there are at least 2 different nominal values that
        // count the given minimum number of data rows
        int numSatisfyingPartitions = 0;
        for (double count : partitionCounter) {
            if (count > minObjectsCount) {
                numSatisfyingPartitions++;
                if (numSatisfyingPartitions >= 2) {
                    break;
                }
            }
        }

        // if there are not enough rows in at least two partitions
        // this is not a valid split
        if (numSatisfyingPartitions < 2) {
            setBestQualityMeasure(Double.NaN);
            return;
        }

        // not a number means this is no valid split
        double qualityMeasure = Double.NaN;
        if (enoughExamplesPerPartition(partitionCounter, minObjectsCount)) {
            qualityMeasure =
                    m_splitQualityMeasure.measureQuality(alloverCount,
                            partitionCounter, histogram, missingValueCount);
            qualityMeasure =
                    m_splitQualityMeasure.postProcessMeasure(qualityMeasure,
                            alloverCount, partitionCounter, missingValueCount);
        }

        setBestQualityMeasure(qualityMeasure);
        m_valuePartitionValidCount = partitionCounter;
        m_alloverValidCount = alloverCount;
    }

    /**
     * Checks if there are at least two partitions with at least the given
     * minimum number of objects.
     *
     * @param partitionCounter the array with the counts of the partitions
     * @param minObjectsCount the min number of objects
     * @return true if there are at least two partitions with at least the given
     *         minimum number of objects
     */
    private static boolean enoughExamplesPerPartition(
            final double[] partitionCounter, final double minObjectsCount) {
        int count = 0;
        for (int i = 0; i < partitionCounter.length; i++) {
            if (partitionCounter[i] >= minObjectsCount) {
                count++;
            }
        }
        return count >= 2;
    }

    /**
     * The number of partitions of a normal nominal split corresponds to the
     * number of different nominal values of the attribute.
     *
     * {@inheritDoc}
     */
    @Override
    public int getNumberPartitions() {
        return getTable().getNumNominalValues(getAttributeIndex());
    }

    /**
     * For normal nominal splits it makes no sense to be used in deeper levels.
     *
     * {@inheritDoc}
     */
    @Override
    public boolean canBeFurtherUsed() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getPartitionForRow(final DataRowWeighted row) {
        double value = row.getValue(getAttributeIndex());
        if (Double.isNaN(value)) {
            return -1;
        }
        return (int)value;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double[] getPartitionWeights() {
        double[] weights = new double[getNumberPartitions()];
        for (int i = 0; i < weights.length; i++) {
            weights[i] = m_valuePartitionValidCount[i] / m_alloverValidCount;
        }
        return weights;
    }
}
