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
 *   Nov 13, 2022 (hornm): created
 */
package org.knime.core.webui.node.view.table.data;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.knime.core.data.container.ContainerTable;
import org.mockito.Mockito;

/**
 * Tests {@link TableCache}.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 */
public class TableCacheTest {

    @Test
    public void test() {

        var tableCache = new TableCache();
        assertThat(tableCache.wasUpdated()).isFalse();

        var keyValueVariants = successivelyModifiedKeyValues();
        var tablesToCache = new ContainerTable[keyValueVariants.length];

        // make sure the cache is updated whenever a 'key-value' changes
        for (var i = 0; i < tablesToCache.length; i++) {
            tablesToCache[i] = Mockito.mock(ContainerTable.class);
            final var final_i = i;
            tableCache.conditionallyUpdateCachedTable(() -> tablesToCache[final_i], false, keyValueVariants[i]);
            assertThat(tableCache.getCachedTableOrElse(null) == tablesToCache[i]).isTrue();
            assertThat(tableCache.wasUpdated()).isTrue();
        }

        // make sure the cache is NOT updated when no key-value changes
        tableCache.conditionallyUpdateCachedTable(() -> tablesToCache[0], false,
            keyValueVariants[tablesToCache.length - 1]);
        assertThat(tableCache.getCachedTableOrElse(null) == tablesToCache[0]).isFalse();
        assertThat(tableCache.wasUpdated()).isFalse();

        var fallbackTable = Mockito.mock(ContainerTable.class);

        // make sure the table cache is cleared
        tableCache.conditionallyUpdateCachedTable(null, true, null);
        assertThat(tableCache.wasUpdated()).isTrue();
        assertThat(tableCache.getCachedTableOrElse(() -> fallbackTable) == fallbackTable).isTrue();

        // make sure the table cache is cleared if the clear-method is called directly)
        tableCache.conditionallyUpdateCachedTable(() -> tablesToCache[0], false, keyValueVariants[0]);
        assertThat(tableCache.wasUpdated()).isTrue();
        assertThat(tableCache.getCachedTableOrElse(null) == tablesToCache[0]).isTrue();
        tableCache.clear();
        assertThat(tableCache.wasUpdated()).isTrue();
        assertThat(tableCache.getCachedTableOrElse(() -> fallbackTable) == fallbackTable).isTrue();

    }

    private static Object[][] successivelyModifiedKeyValues() {
        var keyValues1 = new Object[]{ //
            "string val", //
            true, //
            new String[]{"string arr val 1", "string arr val 2"}, //
            new String[][]{{"1", "2"}, {"3", "4"}} //
        };
        var keyValues2 = new Object[]{ //
            "string val mod", // modified
            true, //
            new String[]{"string arr val 1", "string arr val 2"}, //
            new String[][]{{"1", "2"}, {"3", "4"}} //
        };
        var keyValues3 = new Object[]{ //
            "string val mod", // modified
            false, // modified
            new String[]{"string arr val 1", "string arr val 2"}, //
            new String[][]{{"1", "2"}, {"3", "4"}} //
        };
        var keyValues4 = new Object[]{ //
            "string val mod", false, //
            new String[]{"string arr val 1", "string arr val 2", "string arr val 3"}, // modified
            new String[][]{{"1", "2"}, {"3", "4"}} //
        };
        var keyValues5 = new Object[]{ //
            "string key mod", //
            true, //
            new String[]{"string arr val 1", "string arr val 2", "string arr val 3"}, //
            new String[][]{{"1", "2"}, {"3", "4", "5"}} // modified
        };

        return new Object[][]{keyValues1, keyValues2, keyValues3, keyValues4, keyValues5};
    }

}
