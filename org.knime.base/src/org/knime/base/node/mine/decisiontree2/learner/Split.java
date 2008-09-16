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
 *   22.02.2007 (sieb): created
 */
package org.knime.base.node.mine.decisiontree2.learner;

import org.knime.core.node.NodeLogger;

/**
 * Calculates the best split for a given attribute list and the original class
 * distribution.
 *
 * @author Christoph Sieb, University of Konstanz
 */
public abstract class Split {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(Split.class);

    private InMemoryTable m_table;

    private double m_bestQualityMeasure;

    private int m_attributeIndex;

    /**
     * The quality measure to be used for the best split point calculation.
     */
    protected final SplitQualityMeasure m_splitQualityMeasure;

    /**
     * Constructs the best split for the given attribute list and the class
     * distribution. The results can be retrieved from getter methods.
     *
     * @param table the table for which to create the split
     * @param attributeIndex the index specifying the attribute for which to
     *            calculate the split
     * @param splitQualityMeasure the quality measure to determine the best
     *            split (e.g. gini or gain ratio)
     */
    public Split(final InMemoryTable table, final int attributeIndex,
            final SplitQualityMeasure splitQualityMeasure) {
        LOGGER.debug("New split created.");
        m_table = table;
        m_splitQualityMeasure = splitQualityMeasure;
        m_attributeIndex = attributeIndex;
    }

    /**
     * Returns the {@link InMemoryTable}.
     *
     * @return the {@link InMemoryTable}
     */
    public InMemoryTable getTable() {
        return m_table;
    }

    /**
     * Returns the quality index of this split.
     *
     * @return the quality index of this split
     */
    public double getBestQualityMeasure() {
        return m_bestQualityMeasure;
    }

    /**
     * To set the quality index once calculated by the detailed
     * implementatioins.
     *
     * @param bestGini the gini index to set
     */
    protected void setBestQualityMeasure(final double bestGini) {
        m_bestQualityMeasure = bestGini;
    }

    /**
     * Return the number of partitions resulting from this split.
     *
     * @return the number of partitions resulting from this split
     */
    public abstract int getNumberPartitions();

    /**
     * Returns the name of this split's attribute.
     *
     * @return the name of this split's attribute
     */
    public String getSplitAttributeName() {
        return m_table.getAttributeName(m_attributeIndex);
    }

    /**
     * @return the name of the quality measure used by this split
     */
    public String getQualityMeasureName() {
        return m_splitQualityMeasure.toString();
    }

    /**
     * Returns the index of the attribute this split object is responsible for.
     *
     * @return the index of the attribute this split object is responsible for
     */
    public int getAttributeIndex() {
        return m_attributeIndex;
    }

    /**
     * Whether this split is a valid split. I.e. there exist a valid quality
     * measure.
     *
     * @return whether this split is a valid split
     */
    public boolean isValidSplit() {
        return !Double.isNaN(m_bestQualityMeasure);
    }

    /**
     * Returns true if it makes sense to use this split's attribute further in
     * deeper levels, false if not.
     *
     * @return true if it makes sense to use this split's attribute further in
     *         deeper levels, false if not
     */
    public abstract boolean canBeFurtherUsed();

    /**
     * Returns the partition the given row belongs to according to this split.
     * If the value of the split attribute is missing (i.e. NaN) -1 is returned.
     *
     * @param row the row for which to get the partition index
     * @return the partition the given row belongs to according to this split;
     *         if the value of the split attribute is missing (i.e. NaN) -1 is
     *         returned
     */
    public abstract int getPartitionForRow(DataRowWeighted row);

    /**
     * Returns the partition weights. The weights represent the relative
     * frequency of valid rows per partition. The weights are normally used to
     * adapt the weight of rows whose split value is missing. Such a row is then
     * assigned to each parition with the adapted weight.
     *
     * @return the partition weights
     */
    public abstract double[] getPartitionWeights();

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Split<");
        sb.append(m_table.getAttributeName(m_attributeIndex));
        sb.append("> ").append(m_splitQualityMeasure.toString());
        sb.append("(Worst:").append(m_splitQualityMeasure.getWorstValue())
                .append("):");
        sb.append(m_bestQualityMeasure);
        return sb.toString();
    }
}
