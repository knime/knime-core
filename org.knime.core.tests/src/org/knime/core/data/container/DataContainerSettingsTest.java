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
 *   Feb 14, 2019 (Mark Ortmann, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.core.data.container;

import static org.junit.Assert.assertNotEquals;

import java.util.OptionalInt;

import org.junit.Test;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataTableSpecCreator;
import org.knime.core.data.def.DoubleCell;

import junit.framework.TestCase;

/**
 * Class testing the immutability of the {@link DataContainerSettings}.
 *
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 */
public final class DataContainerSettingsTest extends TestCase {

    /**
     * Tests that the default instance of the container settings uses the proper default settings.
     */
    @SuppressWarnings({"static-method", "deprecation"})
    @Test
    public void testDefault() {
        final DataContainerSettings settings = DataContainerSettings.getDefault();
//        assertEquals("Wrong default (cache size)", ASYNC_CACHE_SIZE, settings.getRowBatchSize());
        assertEquals("Wrong default (number of cells in memory)", OptionalInt.empty(), settings.getMaxCellsInMemory());
        assertEquals("Wrong default (number of possible domain values)", DataContainerSettings.MAX_POSSIBLE_VALUES,
            settings.getMaxDomainValues());
//        assertEquals("Wrong default (asynchronous IO flag)", SYNCHRONOUS_IO, settings.isForceSequentialRowHandling());
//        assertEquals("Wrong default (initialize domain flag)", INIT_DOMAIN, settings.getInitializeDomain());
        assertEquals("Wrong default (asynchrnous write threads)", Runtime.getRuntime().availableProcessors(),
            settings.getMaxContainerThreads());
        assertEquals("Wrong default (container threads)", Runtime.getRuntime().availableProcessors(),
            settings.getMaxThreadsPerContainer());
        assertEquals("Default duplicate checking", true, settings.isCheckDuplicateRowKeys());
        assertEquals("Default domain checking", true, settings.isEnableDomainUpdate());
        assertNotNull("Wrong default (BufferSettings are null)", settings.getBufferSettings());

        assertTrue(
            "Wrong default (Default BufferSettings LRU flag is different to that provided by the DataContainerSettings",
            settings.getBufferSettings().useLRU() == BufferSettings.getDefault().useLRU());

        assertTrue(
            "Wrong default (Default BufferSettings LRU cache size is different to that provided by the DataContainerSettings",
            settings.getBufferSettings().getLRUCacheSize() == BufferSettings.getDefault().getLRUCacheSize());

        final DataTableSpec spec = new DataTableSpecCreator()
            .addColumns(new DataColumnSpecCreator("test", DoubleCell.TYPE).createSpec()).createSpec();
        assertTrue(
            "Wrong default (Default BufferSettings output format is different to that provided by the DataContainerSettings",
            settings.getBufferSettings().getOutputFormat(spec).getClass()
                .equals(BufferSettings.getDefault().getOutputFormat(spec).getClass()));

    }

    /**
     * Tests that values can be changed and that the default instance stays unchanged.
     */
    @SuppressWarnings("static-method")
    @Test
    public void testChangedValues() {
        final DataContainerSettings def = DataContainerSettings.getDefault();

        final int cacheSize = def.getRowBatchSize() * -1;
        final int maxCellsInMemory = 35;
        final int maxPossibleValues = def.getMaxDomainValues() * -1;
        final boolean syncIO = !def.isForceSequentialRowHandling();
        final boolean initDomain = !def.isInitializeDomain();
        final int maxThreadsPerDataContainer = def.getMaxThreadsPerContainer() * -1;
        final int maxContainerThreads = def.getMaxThreadsPerContainer() * -1;
        final BufferSettings bSettings = def.getBufferSettings();
        final boolean enableDuplicateChecking = !def.isCheckDuplicateRowKeys();
        final boolean enableDomainUpdate = !def.isEnableDomainUpdate();


        final DataContainerSettings settings = DataContainerSettings.internalBuilder()
            .withRowBatchSize(cacheSize)//
            .withMaxCellsInMemory(maxCellsInMemory)//
            .withMaxDomainValues(maxPossibleValues)//
            .withForceSequentialRowHandling(syncIO)//
            .withMaxContainerThreads(maxContainerThreads)//
            .withMaxThreadsPerContainer(maxThreadsPerDataContainer)//
            .withBufferSettings(b -> b.withLRUCacheSize(def.getBufferSettings().getLRUCacheSize() * -1))//
            .withInitializedDomain(initDomain)//
            .withCheckDuplicateRowKeys(enableDuplicateChecking)//
            .withDomainUpdate(enableDomainUpdate)//
            .build();

        assertEquals("Modified settings created wrong cache size", cacheSize, settings.getRowBatchSize());
        assertEquals("Modified settings created wrong maximum number of cells in memory", maxCellsInMemory,
            settings.getMaxCellsInMemory().orElseThrow());
        assertEquals("Modified settings created wrong maximum number of possible domain values", maxPossibleValues,
            settings.getMaxDomainValues());
        assertEquals("Modified settings created wrong synchronous IO flag", syncIO, settings.isForceSequentialRowHandling());
        assertEquals("Modified settings created wrong initialize domain flag", initDomain,
            settings.isInitializeDomain());
        assertEquals("Modified settings created wrong maximum number of container threads", maxContainerThreads,
            settings.getMaxContainerThreads());
        assertEquals("Modified settings created wrong maximum number of threads per data container",
            maxThreadsPerDataContainer, settings.getMaxThreadsPerContainer());
        assertEquals("domain update property is updated", enableDomainUpdate, settings.isEnableDomainUpdate());
        assertEquals("row key duplicate check property is updated", enableDuplicateChecking,
            settings.isCheckDuplicateRowKeys());
        assertNotEquals("Default settings has been modified (chache size)", def.getRowBatchSize(),
            settings.getRowBatchSize());
        assertNotEquals("Default settings has been modified (number of cells in memory)", def.getMaxCellsInMemory(),
            settings.getMaxCellsInMemory());
        assertNotEquals("Default settings has been modified (number of possible domain values)",
            def.getMaxDomainValues(), settings.getMaxDomainValues());
        assertNotEquals("Default settings has been modified (max number of container threads)",
            def.getMaxContainerThreads(), settings.getMaxContainerThreads());
        assertNotEquals("Default settings has been modified (number of threads per container)",
            def.getMaxThreadsPerContainer(), settings.getMaxThreadsPerContainer());
        assertNotEquals("Default settings has been modified (synchronous IO flag)", def.isForceSequentialRowHandling(),
            settings.isForceSequentialRowHandling());
        assertNotEquals("Default settings has been modified (initialize domain flag)", def.isInitializeDomain(),
            settings.isInitializeDomain());
        assertNotEquals("Default settings have been modified (domain update property)", def.isEnableDomainUpdate(),
            settings.isEnableDomainUpdate());
        assertNotEquals("Default settings have been modified (row key duplicate check property)",
            def.isCheckDuplicateRowKeys(), settings.isCheckDuplicateRowKeys());
        assertEquals("Modified BufferSettings created wrong value", def.getBufferSettings(), bSettings);
    }

    /**
     * Tests that the number of threads per container cannot be assigned a value larger than the maximum total number of
     * container threads.
     */
    @SuppressWarnings("static-method")
    @Test
    public void testMaxThreadsPerContainerRestriction() {
        final DataContainerSettings def = DataContainerSettings.getDefault();
        // try setting the number of threads per container value higher than allowed
        final DataContainerSettings modified = DataContainerSettings.internalBuilder()
            .withMaxThreadsPerContainer(2 * def.getMaxContainerThreads()).build();
        assertTrue(modified.getMaxThreadsPerContainer() == def.getMaxContainerThreads());
    }

    /**
     * Max cells property must never be negative.
     */
    @SuppressWarnings("static-method")
    @Test
    public void testMaxCellsRestriction() {
        final DataContainerSettings def = DataContainerSettings.getDefault();
        assertEquals("Max cells property expected to be not set", OptionalInt.empty(), def.getMaxCellsInMemory());

        DataContainerSettings s1 = DataContainerSettings.internalBuilder().withMaxCellsInMemory(50).build();
        assertEquals("Max cells property after setting valid value", 50, s1.getMaxCellsInMemory().orElseThrow());

        DataContainerSettings s2 = DataContainerSettings.internalBuilder().withMaxCellsInMemory(-1).build();
        assertEquals("Max cells property after setting valid value", OptionalInt.empty(), s2.getMaxCellsInMemory());
    }

}
