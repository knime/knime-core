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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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
import org.knime.base.node.mine.treeensemble2.data.TreeTargetNominalColumnData;

/**
 * Draws the same fraction of sample from within each class i.e. the class distribution of the whole sample
 * stays the same.
 *
 * @author Adrian Nembach, KNIME.com
 * @param <T> the type of {@link RowSample} that is produced by this sampler.
 */
public class StratifiedRowSampler <T extends RowSample> extends AbstractClassificationRowSampler<T> {


    /**
     * @param fraction the fraction of rows to use from each class
     * @param subsetSelector the subset selector to draw samples (with or without replacement)
     * @param targetColumn the nominal target column
     */
    public StratifiedRowSampler(final double fraction, final SubsetSelector<T> subsetSelector,
        final TreeTargetNominalColumnData targetColumn) {
        super(fraction, subsetSelector, targetColumn);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Collection<T> getSubsets(final RandomData rd) {
        final double fraction = getFraction();
        final int[] offsets = getOffsets();
        final Collection<T> subsets = new ArrayList<>(offsets.length);
        final SubsetSelector<T> selector = getSubsetSelector();
        /*
         * The offsets array contains the positions at which the buckets of the different classes start
         * therefore the size of bucket i is offset[i + 1] - offset[i]
         */
        for (int i = 1; i < offsets.length; i++) {
            final int classSize = offsets[i] - offsets[i - 1];
            final int nrSelect = (int)Math.round(fraction * classSize);
            subsets.add(selector.select(rd, classSize, nrSelect));
        }
        // handle last subset
        final int classSize = getNrRows() - offsets[offsets.length - 1];
        final int nrSelect = (int)Math.round(fraction * classSize);
        subsets.add(selector.select(rd, classSize, nrSelect));
        return subsets;
    }

}
