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
 *   Jun 12, 2020 (carlwitt): created
 */
package org.knime.core.data.join;

import java.util.Arrays;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.filestore.internal.NotInWorkflowDataRepository;
import org.knime.core.data.join.JoinSpecification.OutputRowOrder;
import org.knime.core.data.join.JoinTest.JoinMode;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.DefaultNodeProgressMonitor;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.Node;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeProgressMonitor;
import org.knime.core.node.port.PortType;
import org.knime.core.node.workflow.SingleNodeContainer;
import org.knime.core.node.workflow.virtual.parchunk.VirtualParallelizedChunkPortObjectInNodeFactory;

@SuppressWarnings({"unchecked", "rawtypes"})
public abstract class JoinTestInput {

    private static final NodeProgressMonitor PROGRESS;

    public static final ExecutionContext EXEC;

    static {
        PROGRESS = new DefaultNodeProgressMonitor();

        EXEC = new ExecutionContext(PROGRESS,
            new Node((NodeFactory)new VirtualParallelizedChunkPortObjectInNodeFactory(new PortType[0])),
            SingleNodeContainer.MemoryPolicy.CacheSmallInMemory, NotInWorkflowDataRepository.newInstance());
    }

    protected Object[] m_leftJoinColumns;
    protected Object[] m_rightJoinColumns;

    protected String[] m_leftIncludeColumns;
    protected String[] m_rightIncludeColumns;

    BufferedDataTable[] m_tables;

    protected JoinTestInput() {
        configureJoin();
    }

    /**
     * Hook to initialize members in subclasses {@link #m_leftJoinColumns}, {@link #m_leftIncludeColumns}, etc.
     */
    abstract void configureJoin();

    /**
     * @return the left input table to the join operation
     */
    abstract BufferedDataTable left();

    /**
     * @return the right input table to the join operation
     */
    abstract BufferedDataTable right();

    /**
     * @return the expected rows in the joined table in the specified order.
     */
    abstract DataRow[] ordered(final JoinMode mode, final OutputRowOrder order);

    /**
     * @param order the order in which to return the rows
     * @return the unmatched rows from the left table
     */
    abstract DataRow[] leftOuter(OutputRowOrder order);

    /**
     * @param order the order in which to return the rows
     * @return the unmatched rows from the right table
     */
    abstract DataRow[] rightOuter(OutputRowOrder order);

    JoinSpecification getJoinSpecification(final JoinMode joinMode, final OutputRowOrder outputRowOrder) throws InvalidSettingsException {

        JoinTableSettings leftSettings = JoinTableSettings.left(joinMode.m_retainLeftUnmatched, m_leftJoinColumns,
            m_leftIncludeColumns, left());
        JoinTableSettings rightSettings = JoinTableSettings.right(joinMode.m_retainRightUnmatched, m_rightJoinColumns,
            m_rightIncludeColumns, right());

        return new JoinSpecification.Builder(leftSettings, rightSettings)
            .columnNameDisambiguator(name -> name.concat("*"))
            .mergeJoinColumns(false)
            .conjunctive(true)
            .outputRowOrder(outputRowOrder)
            .rowKeyFactory(JoinSpecification.createConcatRowKeysFactory("+"))
            .retainMatched(joinMode.m_retainMatches)
            .build();
    }

    /**
     * Helper to create tables in concise notation.
     *
     * @param columnNames comma-separated header, e.g., "Column1,ColumnB,ColumnZ"
     * @param rows each string being input to {@link #defaultRow(String...)}
     */
    static BufferedDataTable table(final String columnNames, final String... rows) {
       return table(columnNames, Arrays.stream(rows).map(JoinTestInput::defaultRow).toArray(DataRow[]::new));
    }

    static BufferedDataTable table(final String columnNames, final DataRow... rows) {
        return table(columnNames, false, rows);
    }

    /**
     * Helper to create tables in concise notation.
     *
     * @param columnNames comma-separated header, e.g., "Column1,ColumnB,ColumnZ"
     * @param rows each string being input to {@link #defaultRow(String...)}
     */
    static BufferedDataTable table(final String columnNames, final boolean storeRowOffsets, final DataRow... rows) {
        DataColumnSpec[] columns = Arrays.stream(columnNames.split(","))
            .map(name -> new DataColumnSpecCreator(name, StringCell.TYPE).createSpec()).toArray(DataColumnSpec[]::new);
        DataTableSpec spec = new DataTableSpec(columns);
        if (storeRowOffsets) {
            spec = OrderedRow.withOffset(spec);
        }

        BufferedDataContainer container = EXEC.createDataContainer(spec);
        Arrays.stream(rows).forEach(container::addRowToTable);
        container.close();
        return container.getTable();
    }

    /**
     * @param compactFormat comma-separated values, first is row key, rest denotes cell contents, e.g., "Row0,a,2,e"
     */
    static DataRow defaultRow(final String compactFormat) {
        String[] keyAndValues = compactFormat.split(",");
        DataCell[] cells = Arrays.stream(keyAndValues).skip(1)
            .map(value -> "?".equals(value) ? DataType.getMissingCell() : new StringCell(value))
            .toArray(DataCell[]::new);
        return new DefaultRow(new RowKey(keyAndValues[0]), cells);
    }

    static DataRow defaultRow(final String compactFormat, final long offset) {
        DataRow defaultRow = defaultRow(compactFormat);
        return OrderedRow.withOffset(defaultRow, offset);
    }

    static String dataRowToString(final DataRow row) {
        StringBuilder buffer = new StringBuilder(row.getKey().toString());
        buffer.append(": (");
        for (int i = 0; i < row.getNumCells(); i++) {
            buffer.append(row.getCell(i).toString());
            // separate by ", "
            if (i != row.getNumCells() - 1) {
                buffer.append(", ");
            }
        }
        buffer.append(")");
        return buffer.toString();
    }

}