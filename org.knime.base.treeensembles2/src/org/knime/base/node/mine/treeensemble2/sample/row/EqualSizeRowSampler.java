/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
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

import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.math.random.RandomData;
import org.knime.base.node.mine.treeensemble2.data.NominalValueRepresentation;
import org.knime.base.node.mine.treeensemble2.data.TreeTargetNominalColumnData;

/**
 * Performs equal size row sampling, by first sampling in the minority class and adding from each other class the
 * same number of samples to the RowSample.
 *
 * @author Adrian Nembach, KNIME.com
 * @param <T> the {@link RowSample} type that is used (with or without replacement).
 */
public class EqualSizeRowSampler <T extends RowSample> extends AbstractClassificationRowSampler<T> {

    private final int m_minorityContribution;

    /**
     * @param fraction the fraction of rows from the minority class to use
     * @param subsetSelector the subset selector to draw samples (with or without replacement)
     * @param targetColumn the nominal target column. It is used to find the size of the minority class
     */
    public EqualSizeRowSampler(final double fraction, final SubsetSelector<T> subsetSelector,
        final TreeTargetNominalColumnData targetColumn) {
        super(fraction, subsetSelector, targetColumn);
        final NominalValueRepresentation[] targetVals = targetColumn.getMetaData().getValues();
        int minSize = Integer.MAX_VALUE;
        for (final NominalValueRepresentation targetVal : targetVals) {
            if (targetVal.getTotalFrequency() < minSize) {
                minSize = (int)targetVal.getTotalFrequency();
            }
        }
        m_minorityContribution = (int)Math.round(getFraction() * minSize);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Collection<T> getSubsets(final RandomData rd) {
        final int[] offsets = getOffsets();
        final Collection<T> subsets = new ArrayList<T>();
        final SubsetSelector<T> selector = getSubsetSelector();
        /*
         * The offsets array contains the positions at which the buckets of the different classes start
         * therefore the size of bucket i is offset[i + 1] - offset[i]
         */
        for (int i = 1; i < offsets.length; i++) {
           subsets.add(selector.select(rd, offsets[i] - offsets[i - 1], m_minorityContribution));
        }
        // for the last class the bucket ends at the total nr of rows and starts at its offset
        subsets.add(selector.select(rd, getNrRows() - offsets[offsets.length - 1], m_minorityContribution));
        return subsets;
    }

}
