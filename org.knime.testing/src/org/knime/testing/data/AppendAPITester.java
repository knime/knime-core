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
 *   Feb 24, 2023 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.testing.data;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.knime.testing.data.TableBackendTestUtils.assertTableEquals;
import static org.knime.testing.data.TableBackendTestUtils.intFactory;
import static org.knime.testing.data.TableBackendTestUtils.rowIDFactory;

import java.util.function.LongFunction;

import org.knime.core.data.TableBackend.AppendConfig;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InternalTableAPI;
import org.knime.testing.data.TableBackendTestUtils.Column;

/**
 * Tester for the {@link InternalTableAPI}.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
final class AppendAPITester extends AbstractTableBackendAPITester {

    private static final LongFunction<String> LEFT_IDS = rowIDFactory("Row0", "Row1", "Row2");
    private static final Column BAR = new Column("bar", intFactory(4, 5, 6));
    private static final Column FOO = new Column("foo", intFactory(1, 2, 3));

    private static final LongFunction<String> RIGHT_IDS = rowIDFactory("Row3", "Row4", "Row5");
    private static final Column BAZ = new Column("baz", intFactory(7, 8, 9));
    private static final Column BUZ = new Column("buz", intFactory(10, 11, 12));

    private final BufferedDataTable m_left;

    private final BufferedDataTable m_right;

    /**
     * @param exec the ExecutionContext to use for testing
     */
    AppendAPITester(final ExecutionContext exec) {
        super(exec);
        try {
            m_left = createTable(LEFT_IDS, FOO,BAR);
            m_right = createTable(RIGHT_IDS, BAZ, BUZ);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize test tables for AppendAPITester.", e);
        }
    }

    /**
     * Tests {@link InternalTableAPI#append(ExecutionContext, AppendConfig, BufferedDataTable, BufferedDataTable)}.
     *
     * @throws Exception if the tests fail
     */
    void testAppendRightIDs() throws Exception {
        var appendedRightIDs = InternalTableAPI.append(getExec(), AppendConfig.rowIDsFromTable(1), m_left, m_right);
        var expected = createTable(RIGHT_IDS, FOO, BAR, BAZ, BUZ);
        assertTableEquals(expected, appendedRightIDs);
    }

    void testAppendLeftIDs() throws Exception {
        var appendedLeftIDs = InternalTableAPI.append(getExec(), AppendConfig.rowIDsFromTable(0), m_left, m_right);
        var expected = createTable(LEFT_IDS, FOO, BAR, BAZ, BUZ);
        assertTableEquals(expected, appendedLeftIDs);
    }

    void testAppendMatchingIDsWithMatchingIDs() throws Exception {
        var rightWithMatchingIDs = createTable(LEFT_IDS, BAZ, BUZ);
        var appendedMatchingIDs =
            InternalTableAPI.append(getExec(), AppendConfig.matchingRowIDs(), m_left, rightWithMatchingIDs);
        var expected = createTable(LEFT_IDS, FOO, BAR, BAZ, BUZ);
        assertTableEquals(expected, appendedMatchingIDs);
    }

    void testAppendMatchingIDsWithNonMatchingIDs() {
        assertThrows(IllegalArgumentException.class,
            () -> InternalTableAPI.append(getExec(), AppendConfig.matchingRowIDs(), m_left, m_right));
    }


}
