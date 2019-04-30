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
import static org.knime.core.data.container.DataContainer.ASYNC_CACHE_SIZE;
import static org.knime.core.data.container.DataContainer.INIT_DOMAIN;
import static org.knime.core.data.container.DataContainer.MAX_ASYNC_WRITE_THREADS;
import static org.knime.core.data.container.DataContainer.MAX_CELLS_IN_MEMORY;
import static org.knime.core.data.container.DataContainer.MAX_POSSIBLE_VALUES;
import static org.knime.core.data.container.DataContainer.SYNCHRONOUS_IO;

import java.io.IOException;

import org.junit.Test;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableDomainCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataTableSpecCreator;
import org.knime.core.data.IDataTableDomainCreator;
import org.knime.core.util.DuplicateChecker;
import org.knime.core.util.DuplicateKeyException;
import org.knime.core.util.IDuplicateChecker;

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
        final DataTableSpec spec = new DataTableSpecCreator().createSpec();
        final DataContainerSettings settings = DataContainerSettings.getDefault();
        assertEquals("Wrong default (cache size)", ASYNC_CACHE_SIZE, settings.getAsyncCacheSize());
        assertEquals("Wrong default (number of cells in memory)", MAX_CELLS_IN_MEMORY, settings.getMaxCellsInMemory());
        assertEquals("Wrong default (number of possible domain values)", MAX_POSSIBLE_VALUES,
            settings.getMaxDomainValues());
        assertEquals("Wrong default (asynchronous IO flag)", SYNCHRONOUS_IO, settings.useSyncIO());
        assertEquals("Wrong default (intialize domain flag)", INIT_DOMAIN, settings.getInitializeDomain());
        assertEquals("Wrong default (asynchrnous write threads)", MAX_ASYNC_WRITE_THREADS,
            settings.getMaxAsyncWriteThreads());
        assertTrue("Wrong default (domain creator)",
            settings.createDomainCreator(spec) instanceof DataTableDomainCreator);
        assertTrue("Wrong default (duplicate checker)", settings.createDuplicateChecker() instanceof DuplicateChecker);
        assertNotNull("Wrong default (BufferSettings are null)", settings.getBufferSettings());
        assertTrue("Wrong default (Default BufferSettings are different to those provided by the DataContainerSettings",
            settings.getBufferSettings().equals(BufferSettings.getDefault()));
    }

    /**
     * Tests that values can be changed and that the default instance stays unchanged.
     */
    @SuppressWarnings("static-method")
    @Test
    public void testChangedValues() {
        final DataContainerSettings def = DataContainerSettings.getDefault();

        final int cacheSize = def.getAsyncCacheSize() * -1;
        final int maxCellsInMemory = def.getMaxCellsInMemory() * -1;
        final int maxPossibleValues = def.getMaxDomainValues() * -1;
        final boolean syncIO = !def.useSyncIO();
        final boolean initDomain = !def.getInitializeDomain();
        final int maxAsyncWriteThreads = def.getMaxAsyncWriteThreads() * -1;
        final BufferSettings bSettings =
            def.getBufferSettings().withLRUCacheSize(def.getBufferSettings().getLRUCacheSize() * -1);

        final DataContainerSettings settings = DataContainerSettings.getDefault()//
            .withAsyncCacheSize(cacheSize)//
            .withMaxCellsInMemory(maxCellsInMemory)//
            .withMaxDomainValues(maxPossibleValues)//
            .withSyncIO(syncIO)//
            .withInitializedDomain(initDomain)//
            .withMaxAsyncWriteThreads(maxAsyncWriteThreads)//
            .withBufferSettings(bSettings);

        assertEquals("Modified settings created wrong cache size", cacheSize, settings.getAsyncCacheSize());
        assertEquals("Modified settings created wrong maximum number of cells in memory", maxCellsInMemory,
            settings.getMaxCellsInMemory());
        assertEquals("Modified settings created wrong maximum number of possible domain values", maxPossibleValues,
            settings.getMaxDomainValues());
        assertEquals("Modified settings created wrong synchronous IO flag", syncIO, settings.useSyncIO());
        assertEquals("Modified settings created wrong initialize domain flag", initDomain,
            settings.getInitializeDomain());
        assertEquals("Modified settings created wrong initialize maximum number of asynchronous write threads",
            maxAsyncWriteThreads, settings.getMaxAsyncWriteThreads());
        assertNotEquals("Default settings has been modified (chache size)", def.getAsyncCacheSize(),
            settings.getAsyncCacheSize());
        assertNotEquals("Default settings has been modified (number of cells in memory)", def.getMaxCellsInMemory(),
            settings.getMaxCellsInMemory());
        assertNotEquals("Default settings has been modified (number of possible domain values)",
            def.getMaxDomainValues(), settings.getMaxDomainValues());
        assertNotEquals("Default settings has been modified (number of asynchronous write threads)",
            def.getMaxAsyncWriteThreads(), settings.getMaxAsyncWriteThreads());
        assertNotEquals("Default settings has been modified (synchronous IO flag)", def.useSyncIO(),
            settings.useSyncIO());
        assertNotEquals("Default settings has been modified (initialize domain flag)", def.getInitializeDomain(),
            settings.getInitializeDomain());
        assertNotEquals("Default BufferSettings have not been modified", def.getBufferSettings().equals(bSettings));
    }

    /**
     * Tests that functions can be changed and that the default instance is not changed.
     */
    @Test
    public void testChangeFunctions() {
        class UnitDomainCreator implements IDataTableDomainCreator {

            @Override
            public void updateDomain(final DataRow row) {
            }

            @Override
            public void setMaxPossibleValues(final int maxVals) {
            }

            @Override
            public DataTableSpec createSpec() {
                return null;
            }

            @Override
            public DataTableSpec getInputSpec() {
                return null;
            }

            @Override
            public int getMaxPossibleVals() {
                return 0;
            }

            @Override
            public void merge(final IDataTableDomainCreator dataTableDomainCreator) {
                // TODO Auto-generated method stub

            }

        }

        class UnitDuplicateChecker implements IDuplicateChecker {

            /**
             * {@inheritDoc}
             */
            @Override
            public void addKey(final String s) throws DuplicateKeyException, IOException {
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public void checkForDuplicates() throws DuplicateKeyException, IOException {
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public void clear() {
            }

        }

        final DataContainerSettings def = DataContainerSettings.getDefault();
        final DataContainerSettings settings = def.withDuplicateChecker(() -> new UnitDuplicateChecker())
            .withDomainCreator((s, b) -> new UnitDomainCreator());

        assertFalse("Default settings created wrong domain instance",
            def.createDomainCreator(new DataTableSpecCreator().createSpec()) instanceof UnitDomainCreator);
        assertFalse("Default settings created wrong duplicate checker instance",
            def.createDuplicateChecker() instanceof UnitDuplicateChecker);

        assertTrue("Modified settings created wrong domain instance",
            settings.createDomainCreator(null) instanceof UnitDomainCreator);
        assertTrue("Modified settings created wrong duplicate checker instance",
            settings.createDuplicateChecker() instanceof UnitDuplicateChecker);
    }

}
