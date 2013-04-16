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
package org.knime.base.node.mine.decisiontree2.learner2;

/**
 * The abstract class for split quality measures like gini or gain ratio.
 *
 * @author Christoph Sieb, University of Konstanz
 * 
 * @since 2.6
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
