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
 *   Feb 26, 2019 (Mark Ortmann, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.core.data.container;

import org.junit.Test;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataTableSpecCreator;
import org.knime.core.data.container.storage.TableStoreFormat;
import org.knime.core.data.container.storage.TableStoreFormatRegistry;

import junit.framework.TestCase;

/**
 * Class testing the immutability of the {@link BufferSettings}.
 *
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 */
public final class BufferSettingsTest extends TestCase {

    /**
     * Tests that the default instance of the buffer settings uses the proper default settings.
     */
    @SuppressWarnings("static-method")
    @Test
    public void testDefault() {
        final DataTableSpec spec = new DataTableSpecCreator().createSpec();
        final BufferSettings settings = BufferSettings.getDefault();
        assertEquals("Wrong default (LRU cache size)", BufferSettings.DEF_LRU_CACHE_SIZE, settings.getLRUCacheSize());
        assertEquals("Wrong default (enable LRU cache flag)", BufferSettings.DEF_TABLE_CACHE.equals("LRU"),
            settings.useLRU());
        assertEquals("Wrong default (output format)",
            TableStoreFormatRegistry.getInstance().getInstanceTableStoreFormat(), settings.getOutputFormat(spec));
    }

    /**
     * Tests that values can be changed and that the default instance stays unchanged.
     */
    @SuppressWarnings("static-method")
    @Test
    public void testChangedValues() {
        final BufferSettings def = BufferSettings.getDefault();

        final int lruCacheSize = def.getLRUCacheSize() * -1;
        final boolean useLRU = !def.useLRU();
        final TableStoreFormat outputFormat = new DefaultTableStoreFormat();

        final BufferSettings settings = BufferSettings.builder() //
            .withOutputFormat(outputFormat)//
            .withLRU(useLRU)//
            .withLRUCacheSize(lruCacheSize)//
            .build();

        assertEquals("Modified settings created wrong LRU cache size", lruCacheSize, settings.getLRUCacheSize());
        assertEquals("Modified settings created wrong enable LRU flag", useLRU, settings.useLRU());
        assertTrue("Modified settings created wrong output format",
            outputFormat == settings.getOutputFormat(new DataTableSpecCreator().createSpec()));
        assertFalse("Default settings has been modified (output format)",
            def.getOutputFormat(new DataTableSpecCreator().createSpec()) == settings
                .getOutputFormat(new DataTableSpecCreator().createSpec()));
    }
}
