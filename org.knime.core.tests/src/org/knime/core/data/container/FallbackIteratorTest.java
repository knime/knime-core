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
 */
package org.knime.core.data.container;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.stream.IntStream;

import org.junit.Test;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.RowCursor;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.filestore.internal.NotInWorkflowDataRepository;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.DefaultNodeProgressMonitor;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.Node;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeProgressMonitor;
import org.knime.core.node.port.PortType;
import org.knime.core.node.workflow.SingleNodeContainer;
import org.knime.core.node.workflow.virtual.parchunk.VirtualParallelizedChunkPortObjectInNodeFactory;

/**
 * Tests for {@link FallbackDataRowCursor}.
 *
 * @author Christian Dietz, KNIME GmbH, Konstanz
 */
public class FallbackIteratorTest {

    private static final NotInWorkflowDataRepository REPO = NotInWorkflowDataRepository.newInstance();

    private static final NodeProgressMonitor PROGRESS = new DefaultNodeProgressMonitor();

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static final ExecutionContext exec() {
        return new ExecutionContext(PROGRESS,
            new Node((NodeFactory)new VirtualParallelizedChunkPortObjectInNodeFactory(new PortType[0])),
            SingleNodeContainer.MemoryPolicy.CacheSmallInMemory, REPO);
    }

    private static BufferedDataTable createTable(final int columnsFrom, final int columnsTo, final int offset,
        final int rowsFrom, final int rowsTo) {

        final DataTableSpec spec = new DataTableSpec(IntStream.range(columnsFrom, columnsTo)
            .mapToObj(i -> new DataColumnSpecCreator(Integer.toString(i), IntCell.TYPE).createSpec())
            .toArray(DataColumnSpec[]::new));

        final DataRow[] rows = IntStream.range(rowsFrom, rowsTo)//
            .mapToObj(i -> new DefaultRow(RowKey.createRowKey((long)i), //
                IntStream.range(columnsFrom, columnsTo)//
                    .mapToObj(j -> new IntCell(i * offset + j))//
                    .toArray(DataCell[]::new)))//
            .toArray(DataRow[]::new);

        final BufferedDataContainer cont = exec().createDataContainer(spec);

        // write the data
        for (final DataRow r : rows) {
            cont.addRowToTable(r);
        }

        cont.close();
        return cont.getTable();

    }

    @Test
    public void testIdentity() throws Exception {
        final BufferedDataTable fullTable = createTable(0, 16, 16, 0, 16);
        try (RowCursor cursor = fullTable.cursor(); CloseableRowIterator it = fullTable.iterator()) {
            assertEquals(cursor.getNumColumns(), fullTable.getDataTableSpec().getNumColumns());
            while (cursor.poll()) {
                assertTrue(it.hasNext());
                DataRow row = it.next();
                for (int i = 0; i < cursor.getNumColumns(); i++) {
                    cursor.getValue(i).equals(row.getCell(i));
                }
            }
        }
    }
}
