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
 *   03.08.2016 (Adrian Nembach): created
 */
package org.knime.base.node.mine.treeensemble2.sample.row;

import static org.junit.Assert.assertEquals;

import org.apache.commons.math.random.RandomData;
import org.junit.Test;
import org.knime.base.node.mine.treeensemble2.data.TestDataGenerator;

/**
 * Contais unit tests for the {@link EqualSizeRowSampler} implementation of the {@link RowSampler} interface.
 *
 * @author Adrian Nembach, KNIME.com
 */
public class EqualSizeRowSamplerTest {

    @Test
    public void testCreateRowSampleNoReplacement() throws Exception {
        final SubsetSelector<SubsetNoReplacementRowSample> selector = SubsetNoReplacementSelector.getInstance();
        double fraction = 0.5;

        EqualSizeRowSampler<SubsetNoReplacementRowSample> sampler =
            new EqualSizeRowSampler<SubsetNoReplacementRowSample>(fraction, selector, SamplerTestUtil.TARGET);
        final RandomData rd = TestDataGenerator.createRandomData();

        SubsetNoReplacementRowSample sample = sampler.createRowSample(rd);
        assertEquals(6, sample.getIncludedBitSet().cardinality());
        assertEquals(15, sample.getNrRows());

        fraction = 1.0;

        sampler = new EqualSizeRowSampler<SubsetNoReplacementRowSample>(fraction, selector, SamplerTestUtil.TARGET);
        sample = sampler.createRowSample(rd);
        assertEquals(12, sample.getIncludedBitSet().cardinality());
        assertEquals(15, sample.getNrRows());
        // check if the full minority class is included
        for (int i = 11; i < 15; i++) {
            assertEquals(1, sample.getCountFor(i));
        }

    }

    @Test
    public void testCreateRowSampleWithReplacement() throws Exception {
        final SubsetSelector<SubsetWithReplacementRowSample> selector = SubsetWithReplacementSelector.getInstance();
        double fraction = 0.5;

        EqualSizeRowSampler<SubsetWithReplacementRowSample> sampler =
            new EqualSizeRowSampler<SubsetWithReplacementRowSample>(fraction, selector, SamplerTestUtil.TARGET);
        final RandomData rd = TestDataGenerator.createRandomData();

        SubsetWithReplacementRowSample sample = sampler.createRowSample(rd);
        assertEquals(6, SamplerTestUtil.countRows(sample));
        assertEquals(15, sample.getNrRows());

        fraction = 1.0;
        sampler = new EqualSizeRowSampler<SubsetWithReplacementRowSample>(fraction, selector, SamplerTestUtil.TARGET);
        sample = sampler.createRowSample(rd);
        assertEquals(12, SamplerTestUtil.countRows(sample));
        assertEquals(15, sample.getNrRows());

        int minorityCount = 0;
        for (int i = 11; i < 15; i++) {
            minorityCount += sample.getCountFor(i);
        }
        assertEquals(4, minorityCount);

    }

}
