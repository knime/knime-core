/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2010
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
 *   22.02.2007 (sieb): created
 */
package org.knime.base.node.mine.decisiontree2.learner;


/**
 * Calculates the best split for a given attribute list and the original class
 * distribution.
 *
 * @author Christoph Sieb, University of Konstanz
 */
public abstract class Split {

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
