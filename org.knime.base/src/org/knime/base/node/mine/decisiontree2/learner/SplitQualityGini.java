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
 *   19.03.2007 (sieb): created
 */
package org.knime.base.node.mine.decisiontree2.learner;

/**
 * Implements the gini index split quality measure. This gini index is
 * subtracted from 1 (worst value), thus the gini index is also better if it is
 * larger than another gini index (same as for gain ratio).
 * 
 * @author Christoph Sieb, University of Konstanz
 */
public class SplitQualityGini extends SplitQualityMeasure {

    /**
     * A gini index is better if it is larger than the other one.
     * 
     * {@inheritDoc}
     */
    @Override
    public boolean isBetter(final double quality1, final double quality2) {
        return quality1 > quality2;
    }

    /**
     * A GINI index is better if it is larger than the other one.
     * 
     * {@inheritDoc}
     */
    @Override
    public boolean isBetterOrEqual(final double quality1, 
            final double quality2) {
        return quality1 >= quality2;
    }

    /**
     * Calculates the gini split index.
     * <p>
     * For a dataset T the gini index is: gini(T) = 1 - SUM(pj * pj) - for all
     * relative class frequencies pj (pj = Pj/|T|). Pj is the absolut class
     * frequency and nx the number of records in the data set
     * <p>
     * The gini for the split is: giniSplit(T) = SUM(nx/N*gini(Tx)) - for all
     * relative partition frequencies nx/N and all partitions Tx
     * 
     * @param allOverRecords the allover number of records with known values in
     *            the partition to split; corresponds to N in the formula
     * @param partitionFrequency the frequencies of the different patitions;
     *            corresponds to nx in the formula
     * @param partitionClassFrequency all class frequencies Pj (second
     *            dimension) for all partitions Tx (first dimension *
     * @param numUnknownRecords the number of records with unknown (missing)
     *            value of the relevant attribute; used to weight the quality
     *            measure
     * @return the gini split index
     */
    @Override
    public double measureQuality(final double allOverRecords,
            final double[] partitionFrequency,
            final double[][] partitionClassFrequency,
            final double numUnknownRecords) {
        double giniSplit = 0;

        // for all partitions
        for (int i = 0; i < partitionFrequency.length; i++) {
            // relative partition frequency nx/N
            double relativePartitionFrequ =
                    partitionFrequency[i] / allOverRecords;

            double giniPartition =
                    calculateGiniIndex(partitionFrequency[i],
                            partitionClassFrequency[i]);

            giniSplit += relativePartitionFrequ * giniPartition;
        }

        // invert the index; necessary for missing value weighting
        giniSplit = 1 - giniSplit;

        // LOGGER.debug("Gini index: " + giniSplit);
        // weight the gini index with the fraction of known valued records
        return (allOverRecords / (allOverRecords + numUnknownRecords))
                * giniSplit;
    }

    /**
     * Calculates the gini index.
     * <p>
     * For a dataset T the gini index is: gini(T) = 1 - SUM(pj * pj) - for all
     * relative class frequencies pj (pj = Pj/|T|). Pj is the absolut class
     * frequency and nx the number of records in the data set
     * 
     * @param alloverFrequency the allover number of records; the sum of all
     *            class frequencies
     * @param classFrequency class frequencies
     * @return the gini index
     */
    private static double calculateGiniIndex(final double alloverFrequency,
            final double[] classFrequency) {
        if (alloverFrequency == 0.0) {
            return 1.0;
        }

        double gini = 1;

        for (int i = 0; i < classFrequency.length; i++) {
            double relativeFrequency = 0.0;
            relativeFrequency = classFrequency[i] / alloverFrequency;
            gini -= relativeFrequency * relativeFrequency;
        }

        return gini;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getWorstValue() {
        return 0.0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initQualityMeasure(final double[] classFrequencies,
            final double allOverRecords) {
        // the gini index does not need to init the measure

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "Gini";
    }

    /**
     * The gini index need not to post process the measure.
     * 
     * {@inheritDoc}
     */
    @Override
    public double postProcessMeasure(final double qualityMeasure,
            final double allOverRecords, final double[] partitionFrequency,
            final double numUnknownRecords) {
        // just return the input quality measure
        return qualityMeasure;
    }

}
