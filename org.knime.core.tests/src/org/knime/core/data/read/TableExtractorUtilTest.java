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
 *   8 Jul 2022 (BaernreutherPaul): created
 */
package org.knime.core.data.read;

import static org.assertj.core.api.Assertions.assertThat;
import static org.knime.core.data.v2.TableExtractorUtil.extractData;
import static org.knime.core.data.v2.TableExtractorUtil.extractDataUncancelled;
import static org.knime.core.data.v2.TableExtractorUtil.restrictToIndex;

import java.util.function.BiFunction;

import org.junit.jupiter.api.Test;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.v2.RowRead;
import org.knime.core.data.v2.TableExtractorUtil.Extractor;
import org.knime.core.node.BufferedDataTable;
import org.knime.testing.util.TableTestUtil;

/**
 *
 * @author Paul Bärnreuther
 */
class TableExtractorUtilTest {

    private static BufferedDataTable createTestTable() {
        var stringColumn = new TableTestUtil.ObjectColumn("string", StringCell.TYPE, new String[]{"foo", "bar", null});
        var doubleColumn = new TableTestUtil.ObjectColumn("double", DoubleCell.TYPE, new Double[]{1d, 2d, null});
        var secondStringColumn =
            new TableTestUtil.ObjectColumn("secondString", StringCell.TYPE, new String[]{null, "foo2", "bar2"});
        return TableTestUtil.createTableFromColumns(stringColumn, doubleColumn, secondStringColumn);
    }

    @Test
    void testExtractDataAllRows() {
        final var extractors = new IdleExtractor[]{new IdleExtractor(0), new IdleExtractor(1)};
        extractDataUncancelled(createTestTable(), extractors);
        assertThat(extractors[0].m_size).isEqualTo(3);
        assertThat(extractors[0].m_counter).isEqualTo(3);
        assertThat(extractors[1].m_size).isEqualTo(3);
        assertThat(extractors[1].m_counter).isEqualTo(3);
    }

    @Test
    void testExtractDataWithNumRows() {
        final var extractors = new IdleExtractor[]{new IdleExtractor(0), new IdleExtractor(1)};
        final var numRows = 2;
        extractData(createTestTable(), numRows, extractors);
        assertThat(extractors[0].m_size).isEqualTo(numRows);
        assertThat(extractors[0].m_counter).isEqualTo(numRows);
        assertThat(extractors[1].m_size).isEqualTo(numRows);
        assertThat(extractors[1].m_counter).isEqualTo(numRows);
    }

    @Test
    void testExtractDataWithNegativeNumRows() {
        final var extractor = new IdleExtractor(0);
        final int numRows = -1;
        extractData(createTestTable(), numRows, extractor);
        assertThat(extractor.m_size).isNull();
        assertThat(extractor.m_counter).isZero();
    }

    @Test
    void testRestrictToIndex() {
        final BiFunction<RowRead, Integer, Integer> bifunction = (row, i) -> i;
        final var function = restrictToIndex(bifunction, 8);
        // function is the constant function on 8;
        assertThat(function.apply(null)).isEqualTo(8);
    }

    private static class IdleExtractor implements Extractor {

        private final int m_colIndex;

        public Integer m_size;

        public int m_counter;

        public IdleExtractor(final int colIndex) {
            m_colIndex = colIndex;
            m_counter = 0;
        }

        @Override
        public void init(final int size) {
            m_size = size;
        }

        @Override
        public void readRow(final RowRead row, final int rowIndex) {
            m_counter += 1;
        }

        @Override
        public int[] getColumnIndices() {
            return new int[]{m_colIndex};
        }
    }
}