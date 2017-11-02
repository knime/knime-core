/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
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
 *   29.07.2016 (Adrian Nembach): created
 */
package org.knime.base.node.mine.treeensemble2.sample.row;

import java.util.Collection;

import org.apache.commons.math.random.RandomData;
import org.knime.base.node.mine.treeensemble2.data.NominalValueRepresentation;
import org.knime.base.node.mine.treeensemble2.data.TreeTargetNominalColumnData;

/**
 * Abstract implementation of the {@link RowSampler} interface for all samplers
 * that draw samples from different subsets of the total set of rows and then combine them.
 *
 * @author Adrian Nembach, KNIME.com
 * @param <T> the {@link RowSample} type that is used (with or without replacement).
 */
public abstract class AbstractClassificationRowSampler <T extends RowSample> extends AbstractRowSampler<T> {

    private final int[] m_offsets;

    /**
     * @param fraction the fraction that should be used (NOTE: the interpretation of fraction depends on the sampling strategy)
     * @param subsetSelector the subset selector to draw samples (with or without replacement)
     * @param targetColumn the <b>sorted</b> target column. We extract the offsets of the different class buckets.
     */
    public AbstractClassificationRowSampler(final double fraction, final SubsetSelector<T> subsetSelector,
        final TreeTargetNominalColumnData targetColumn) {
        super(fraction, subsetSelector, targetColumn.getNrRows());
        final NominalValueRepresentation[] targetVals = targetColumn.getMetaData().getValues();
        final int[] offsets = new int[targetVals.length];
        int offset = 0;
        for (int i = 0; i < targetVals.length; i++) {
            offsets[i] = offset;
            offset += targetVals[i].getTotalFrequency();
        }
        m_offsets = offsets;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public T createRowSample(final RandomData rd) {
        final Collection<T> subsets = getSubsets(rd);
        final T rowSample = getSubsetSelector().combine(subsets, m_offsets, getNrRows());
        return rowSample;
    }

    /**
     * Subclasses draw multiple samples from different subsets of the full set of rows.<br>
     * Usually those samples are drawn from sets consisting of rows with the same class.
     *
     * @param rd {@link RandomData} of the respective TreeLearner
     * @return a collection of RowSamples that corresponds to samples drawn from within each class bucket.
     */
    protected abstract Collection<T> getSubsets(final RandomData rd);

    /**
     * The first entry (offset[0]) is always 0.
     *
     * @return an array containing the starting points of the class buckets in the target column.
     */
    protected int[] getOffsets() {
        return m_offsets;
    }
}
