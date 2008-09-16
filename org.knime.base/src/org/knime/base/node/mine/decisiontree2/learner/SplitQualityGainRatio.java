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
 * Implements the gain ratio split quality measure.
 *
 * @author Christoph Sieb, University of Konstanz
 */
public class SplitQualityGainRatio extends SplitQualityMeasure {

    private static final double LOG2_DIVISOR = Math.log(2.0);

    private double m_originalEntropy;

    /**
     * A gain ratio index is better if it is larger than the other one.
     *
     * {@inheritDoc}
     */
    @Override
    public boolean isBetter(final double quality1, final double quality2) {
        return quality1 > quality2;
    }

    /**
     * A gain ratio index is better if it is larger or equal than the other one.
     *
     * {@inheritDoc}
     */
    @Override
    public boolean isBetterOrEqual(final double quality1,
            final double quality2) {
        return quality1 >= quality2;
    }

    /**
     * Calculates the gain ratio split index.
     * <p>
     * For a dataset T the gain ratio index is:
     * <p>
     * gainRatio(T) = gain(T) / splitInfo(T)
     * <p>
     *
     * @param allOverRecords the allover number of records with known values in
     *            the partition to split; corresponds to N in the formula
     * @param partitionFrequency the frequencies of the different patitions;
     *            corresponds to nx in the formula
     * @param partitionClassFrequency all class frequencies Pj (second
     *            dimension) for all partitions Tx (first dimension
     * @param numUnknownRecords the number of records with unknown (missing)
     *            value of the relevant attribute; used to weight the quality
     *            measure
     * @return the gain ratio split index
     */
    @Override
    public double measureQuality(final double allOverRecords,
            final double[] partitionFrequency,
            final double[][] partitionClassFrequency,
            final double numUnknownRecords) {

        double infoAll = 0.0;
        double knownAndUnknownRecords = allOverRecords + numUnknownRecords;

        // now subtract the information for each partition weighted by the
        // relative size of the partition
        for (int i = 0; i < partitionFrequency.length; i++) {
            double weightedInfo =
                    information(partitionFrequency[i],
                            partitionClassFrequency[i]);
            infoAll += weightedInfo;

        }
        infoAll = m_originalEntropy - infoAll / allOverRecords;
        // weight the information gain with the fraction of known-value-records
        infoAll *= allOverRecords / knownAndUnknownRecords;

        return infoAll;
    }

    private static double information(final double allOverCount,
            final double[] classFrequencies) {
        if (allOverCount == 0.0) {
            return 0.0;
        }
        double information = 0.0;
        double log2AllOver = log2(allOverCount);
        for (int i = 0; i < classFrequencies.length; i++) {
            information -=
                    classFrequencies[i]
                            * (log2(classFrequencies[i]) - log2AllOver);
        }
        return information;
    }

    private static double log2Mult(final double num) {
        if (num < 1E-6) {
            return 0.0;
        }
        return num * Math.log(num) / LOG2_DIVISOR;
    }

    private static double log2(final double num) {
        if (num < 1E-6) {
            return 0.0;
        }
        return Math.log(num) / LOG2_DIVISOR;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getWorstValue() {
        return -1.0;
    }

    /**
     * Calculates the entropy of the distribution before a split. Therefore the
     * entropy can be reused for several calculations. {@inheritDoc}
     */
    @Override
    public void initQualityMeasure(final double[] classFrequencies,
            final double allOverRecords) {
        m_originalEntropy =
                information(allOverRecords, classFrequencies) / allOverRecords;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "Gain";
    }

    /**
     * The post processing of the gain ration measure normalizes the info gain
     * with the split info (see c4.5).
     *
     * {@inheritDoc}
     */
    @Override
    public double postProcessMeasure(final double qualityMeasure,
            final double allOverRecords, final double[] partitionFrequency,
            final double numUnknownRecords) {

        double splitInfo = 0.0;
        double knownAndUnknownRecords = allOverRecords + numUnknownRecords;

        for (int i = 0; i < partitionFrequency.length; i++) {
            // calculate the split info
            // we need the relative frequency according to all (known and unkown
            // value) records
            double relativeFrequency =
                    partitionFrequency[i] / knownAndUnknownRecords;
            splitInfo -= log2Mult(relativeFrequency);
        }

        // the fraction of unknown valued records must also be regarded in split
        // info
        double unknownFraction = numUnknownRecords / knownAndUnknownRecords;
        splitInfo -= log2Mult(unknownFraction);

        // the info gain is the info gain (qualityMeasure) divided by the split
        // info
        return qualityMeasure / splitInfo;
    }
}
