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
 *   May 9, 2020 (dietzc): created
 */
package org.knime.core.data.container.fast;

import java.io.File;
import java.io.IOException;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.ContainerTable;
import org.knime.core.data.container.RowContainer;
import org.knime.core.data.container.RowContainerFactory;
import org.knime.core.data.container.fast.AdapterRegistry.DataSpecAdapter;
import org.knime.core.data.table.arrow.ArrowTableStoreFactory;
import org.knime.core.data.table.store.TableStoreFactory;
import org.knime.core.internal.ReferencedFile;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.workflow.WorkflowDataRepository;

/**
 * {@link RowContainerFactory} for fast tables.
 *
 * @author Christian Dietz, KNIME GmbH
 */
public class FastTableRowContainerFactory implements RowContainerFactory {

    private static final String FAST_TABLE_CONTAINER_ROWKEY = "FAST_TABLE_CONTAINER_ROWKEY";

    private static final String FAST_TABLE_CONTAINER_SIZE = "FAST_TABLE_CONTAINER_SIZE";

    private static final String FAST_TABLE_CONTAINER_TYPE = "FAST_TABLE_CONTAINER_TYPE";

    private final static ArrowTableStoreFactory FACTORY = new ArrowTableStoreFactory();

    public FastTableRowContainerFactory() {
    }

    @Override
    public boolean supports(final DataTableSpec spec) {
        for (final DataColumnSpec colSpec : spec) {
            if (!AdapterRegistry.hasAdapter(colSpec.getType())) {
                return false;
            }
        }
        return true;
    }

    @Override
    public RowContainer create(final long tableId, final DataTableSpec spec, final File dest, final boolean isRowKey) {
        // TODO remove again
        System.out.println("using fast tables");
        return new FastTableRowContainer(tableId, dest, spec, FACTORY);
    }

    // TODO non-public!
    @SuppressWarnings({"resource", "javadoc"})
    public static ContainerTable readFromFileDelayed(final ReferencedFile fileRef, final DataTableSpec spec,
        final int id, final WorkflowDataRepository dataRepository, final NodeSettingsRO settings)
        throws InvalidSettingsException {
        try {
            final long size = settings.getLong(FAST_TABLE_CONTAINER_SIZE);
            final TableStoreFactory factory =
                (TableStoreFactory)Class.forName(settings.getString(FAST_TABLE_CONTAINER_TYPE)).newInstance();
            boolean isRowKey = settings.getBoolean(FAST_TABLE_CONTAINER_ROWKEY);
            // TODO store adapter somewhere when serializing for backwards compatibility?
            final DataSpecAdapter adapter = AdapterRegistry.createAdapter(spec, isRowKey);
            return new LazyFastTable(dataRepository, fileRef, id, spec, factory, adapter, size, isRowKey);
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException ex) {
            // TODO we stored the wrong factory
            throw new RuntimeException(ex);
        }
    }

    // TODO non-public
    @SuppressWarnings({"resource", "javadoc"})
    public static void saveToFile(final DataTable table, final File outFile, final NodeSettingsWO s,
        final ExecutionMonitor exec) throws IOException, CanceledExecutionException {
        if (!isFastTable(table)) {
            throw new IllegalArgumentException(
                "Implementation error. Only FastTables can be saved within FastTableRowContainer!");
        }
        final FastTable fastTable = (FastTable)table;
        s.addString(FAST_TABLE_CONTAINER_TYPE, fastTable.getStore().getFactory().getCanonicalName());
        s.addLong(FAST_TABLE_CONTAINER_SIZE, fastTable.size());
        s.addBoolean(FAST_TABLE_CONTAINER_ROWKEY, fastTable.isRowKeys());
        fastTable.saveToFile(outFile, s, exec);
    }

    /**
     * Checks if a {@link DataTable} is a FastTable. Purely introduced to be able to hide FastTable interface from rest
     * of world for now.
     *
     * TODO make this method unnecessary.
     *
     * @param table the {@link DataTable} to check
     * @return <code>true</code> if compatible.
     */
    public static boolean isFastTable(final DataTable table) {
        return table instanceof FastTable;
    }

}
