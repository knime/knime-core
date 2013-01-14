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
 *   19.03.2007 (sieb): created
 */
package org.knime.base.node.mine.decisiontree2.learner;

/**
 * Implements the gain ratio split quality measure.
 *
 * @author Christoph Sieb, University of Konstanz
 */
@Deprecated
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
