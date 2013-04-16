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
 * Implements the gini index split quality measure. This gini index is
 * subtracted from 1 (worst value), thus the gini index is also better if it is
 * larger than another gini index (same as for gain ratio).
 *
 * @author Christoph Sieb, University of Konstanz
 */
@Deprecated
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
