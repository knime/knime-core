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

import java.util.BitSet;
import java.util.Collection;
import java.util.Iterator;

import org.apache.commons.math.random.RandomData;
import org.knime.core.node.util.CheckUtils;

/**
 * Selects a subset with no replacement i.e. every index is only contained at most once in the subset.
 *
 * @author Adrian Nembach, KNIME.com
 */
public class SubsetNoReplacementSelector implements SubsetSelector<SubsetNoReplacementRowSample> {

    private static SubsetNoReplacementSelector INSTANCE = null;

    /**
     * Singleton constructor
     */
    private SubsetNoReplacementSelector() {
        // this class is a singleton
    }

    /**
     * @return an instance of {@link SubsetNoReplacementSelector}
     */
    public static SubsetNoReplacementSelector getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new SubsetNoReplacementSelector();
        }
        return INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SubsetNoReplacementRowSample select(final RandomData rd, final int nrTotal, final int nrSelect) {
        CheckUtils.checkArgument(nrTotal > 0, "The parameter nrTotal must be > 0 but was %d", nrTotal);
        CheckUtils.checkArgument(nrSelect > 0, "The parameter nrSelect must be > 0 but was %d", nrSelect);
        CheckUtils.checkArgument(nrTotal >= nrSelect, "nrTotal (%d) must always be >= nrSelect (%d).", nrTotal,
            nrSelect);
        CheckUtils.checkNotNull(rd, "rd may not be null.");

        final int[] includes = rd.nextPermutation(nrTotal, nrSelect);
        final BitSet bs = new BitSet(nrTotal);
        for (int i : includes) {
            bs.set(i);
        }
        final double fraction = ((double)nrSelect) / nrTotal;

        return new SubsetNoReplacementRowSample(bs, nrTotal, fraction);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SubsetNoReplacementRowSample combine(final Collection<SubsetNoReplacementRowSample> subsets,
        final int[] offsets, final int totalNrRows) {
        CheckUtils.checkArgumentNotNull(subsets);
        CheckUtils.checkArgumentNotNull(offsets);
        CheckUtils.checkArgument(subsets.size() == offsets.length,
            "The size of subsets (%d) and the length of offsets (%d) must match.", subsets.size(), offsets.length);
        CheckUtils.checkArgument(totalNrRows >= offsets.length,
            "The totalNrRows (%d) must be at least as large as the length of offsets (%d)", totalNrRows,
            offsets.length);
        double nrSelected = 0;
        final Iterator<SubsetNoReplacementRowSample> iterator = subsets.iterator();
        final BitSet bs = new BitSet(totalNrRows);
        for (int i = 0; i < subsets.size(); i++) {
            final int offset = offsets[i];
            final BitSet subsetBitSet = iterator.next().getIncludedBitSet();
            for (int j = subsetBitSet.nextSetBit(0); j >= 0; j = subsetBitSet.nextSetBit(j + 1)) {
                bs.set(j + offset);
                nrSelected += 1.0;
            }
        }
        final double fraction = nrSelected / totalNrRows;
        return new SubsetNoReplacementRowSample(bs, totalNrRows, fraction);
    }

}
