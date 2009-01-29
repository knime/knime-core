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
 *   19.03.2007 (sieb): created
 */
package org.knime.base.node.mine.decisiontree2.learner;

/**
 * The abstract class for split quality measures like gini or gain ratio.
 *
 * @author Christoph Sieb, University of Konstanz
 */
public abstract class SplitQualityMeasure implements Cloneable{

    /**
     * Calculates the quality for a given split.
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
     * @return the quality for a given split
     */
    public abstract double measureQuality(final double allOverRecords,
            final double[] partitionFrequency,
            final double[][] partitionClassFrequency,
            final double numUnknownRecords);

    /**
     * Determines if the first passed quality is better or equal compared to the
     * second quality.
     *
     * @param quality1 first quality to compare
     * @param quality2 second quality to compare
     * @return true, iff the first quality is better or equal to the second
     *         quality
     */
    public abstract boolean isBetterOrEqual(final double quality1,
            final double quality2);

    /**
     * Determines if the first passed quality is better compared to the second
     * quality.
     *
     * @param quality1 first quality to compare
     * @param quality2 second quality to compare
     * @return true, iff the first quality is better to the second quality
     */
    public abstract boolean isBetter(final double quality1,
            final double quality2);

    /**
     * Returns the worst value for this quality measure.
     *
     * @return the worst value for this quality measure
     */
    public abstract double getWorstValue();

    /**
     * Some quality measures, like the information gain, calculate a quality of
     * a previous distribution compared to a new one. This previous distribution
     * can be reused. For those cases a init method is provided that enable pre
     * calculations to increase performance.
     *
     * @param classFrequencies the class frequencies
     * @param allOverRecords the overall count
     */
    public abstract void initQualityMeasure(final double[] classFrequencies,
            final double allOverRecords);

    /**
     * {@inheritDoc}
     */
    @Override
    public abstract String toString();

    /**
     * Some quality measures need normalization when compared to other
     * attributes. As this normalization is not required when the quality is
     * compared inside a single attribute, this method allows to perform post
     * processing (normalization) of quality measures to avoid a lot of
     * unnecessary calculations.
     *
     * @param qualityMeasure the quality measure to post process
     * @param allOverRecords the allover number of known (non-missing) records
     * @param partitionFrequency the frequencies of the potential split
     *            partitions
     * @param numUnknownRecords the number of unknown (missing) records
     *
     * @return the post processed quality measure
     */
    public abstract double postProcessMeasure(final double qualityMeasure,
            final double allOverRecords, final double[] partitionFrequency,
            final double numUnknownRecords);

    /**
     * {@inheritDoc}
     */
    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}
