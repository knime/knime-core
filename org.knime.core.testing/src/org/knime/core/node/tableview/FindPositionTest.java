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
 *   Jul 15, 2016 (wiswedel): created
 */
package org.knime.core.node.tableview;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.util.Arrays;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.knime.core.node.tableview.FindPosition.SearchOptions;

/**
 * Tests the rather cryptic logic in {@link FindPosition}.
 * @author wiswedel
 */
@RunWith(Parameterized.class)
public final class FindPositionTest {

    /** @return all accepted flag stats. */
    @Parameters(name="{index}: id={0}; colName={1}; data={2}")
    public static Iterable<Boolean[]> getParameters() {
        return Arrays.asList(
            new Boolean[]{true, true, true},
            new Boolean[]{true, true, false},
            new Boolean[]{true, false, true},
            new Boolean[]{true, false, false},
            new Boolean[]{false, true, true},
            new Boolean[]{false, true, false},
            new Boolean[]{false, false, true}
        );
    }

    private final boolean m_isSearchRowID;
    private final boolean m_isSearchColumnName;
    private final boolean m_isSearchData;

    /**
     * @param isSearchRowID
     * @param isSearchColumnName
     * @param isSearchData
     */
    public FindPositionTest(final boolean isSearchRowID, final boolean isSearchColumnName, final boolean isSearchData) {
        m_isSearchRowID = isSearchRowID;
        m_isSearchColumnName = isSearchColumnName;
        m_isSearchData = isSearchData;
    }

    @Test
    public void testNormalTable() throws Exception {
        FindPosition f = new FindPosition(1, 1,
            new SearchOptions(m_isSearchRowID, m_isSearchColumnName, m_isSearchData));
        f.reset();
        f.mark();
        f.toString(); // coverage is all
        if (m_isSearchColumnName) {
            assertThat(f.next(), is(true));
            assertThat("Expected header row", f.getSearchRow(), is(-1));
            assertThat("Expected first column", f.getSearchColumn(), is(0));
            assertThat("Not at mark", f.reachedMark(), is(false));
        }

        if (m_isSearchRowID) {
            assertThat(f.next(), is(true));
            assertThat("Expected first row key", f.getSearchRow(), is(0));
            assertThat("Expected rowID column", f.getSearchColumn(), is(-1));
            assertThat("Not at mark", f.reachedMark(), is(false));
        }

        if (m_isSearchData) {
            assertThat(f.next(), is(true));
            assertThat("Expected first data element", f.getSearchRow(), is(0));
            assertThat("Expected rowID column", f.getSearchColumn(), is(0));
            assertThat("Not at mark", f.reachedMark(), is(false));
        }

        assertThat(f.next(), is(false));
    }

    @Test
    public void testNoRowsTable() throws Exception {
        FindPosition f = new FindPosition(0, 1,
            new SearchOptions(m_isSearchRowID, m_isSearchColumnName, m_isSearchData));
        f.reset();
        f.mark();
        if (m_isSearchColumnName) {
            assertThat(f.next(), is(true));
            assertThat("Expected header row", f.getSearchRow(), is(-1));
            assertThat("Expected first column", f.getSearchColumn(), is(0));
            assertThat("Not at mark", f.reachedMark(), is(false));
        }

        assertThat(f.next(), is(false));
    }

    @Test
    public void testNoColumnsTable() throws Exception {
        FindPosition f = new FindPosition(1, 0,
            new SearchOptions(m_isSearchRowID, m_isSearchColumnName, m_isSearchData));
        f.reset();
        f.mark();

        if (m_isSearchRowID) {
            assertThat(f.next(), is(true));
            assertThat("Expected first row key", f.getSearchRow(), is(0));
            assertThat("Expected rowID column", f.getSearchColumn(), is(-1));
            assertThat("Not at mark", f.reachedMark(), is(false));
        }

        assertThat(f.next(), is(false));
    }

    @Test
    public void testNoRowsNoColumnsTable() throws Exception {
        FindPosition f = new FindPosition(0, 0,
            new SearchOptions(m_isSearchRowID, m_isSearchColumnName, m_isSearchData));
        f.reset();
        f.mark();
        assertThat(f.next(), is(false));
    }

}
