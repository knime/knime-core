/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 * 
 * Copyright, 2003 - 2006
 * Universitaet Konstanz, Germany.
 * Lehrstuhl fuer Angewandte Informatik
 * Prof. Dr. Michael R. Berthold
 * 
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner.
 * -------------------------------------------------------------------
 * 
 */
package de.unikn.knime.core.node;

import de.unikn.knime.core.data.DataRow;
import de.unikn.knime.core.data.DataTable;
import de.unikn.knime.core.data.DataTableSpec;
import de.unikn.knime.core.data.RowIterator;
import de.unikn.knime.core.data.container.ColumnRearranger;
import de.unikn.knime.core.data.container.RearrangeColumnsTable;
import de.unikn.knime.core.data.container.TableSpecReplacerTable;

/**
 * An <code>ExecutionContext</code> provides storage capacities during a
 * {@link de.unikn.knime.core.node.NodeModel#execute( BufferedDataTable[],
 * ExecutionContext) NodeModel's execution}. Furthermore it allows to report
 * progress of the execution and to check for cancellation events.
 * 
 * Any derived class of <code>NodeModel</code> that has at least one data
 * output will need to create a <code>BufferedDataTable</code> as return value
 * of the execute method. These <code>BufferedDataTable</code> can only be
 * created by means of an <code>ExecutionContext</code> using one of the
 * <code>create...</code> methods. There are basically three different ways to
 * create the output table:
 * <dl>
 * <a name="new_data"/>
 * <dt><strong>New data</strong></dt>
 * <dd>Use the {@link #createDataContainer(DataTableSpec)} method to create a
 * container to which rows are sequentially added. The final result will be
 * available through the container's {@link BufferedDataContainer#getTable()}
 * method. Alternatively you can also use the
 * {@link #createBufferedDataTable(DataTable, ExecutionMonitor)} method which
 * will traverse the argument table and cache everything. These method shall be
 * used when the entire output must be cached (thus also resulting in using more
 * disc space when the workflow is saved). </dd>
 * <a name="new_column"/>
 * <dt><strong>Some columns of the input have changed</strong></dt>
 * <dd>This is the case, for instance when you just append a single column to
 * the input table (or filter/replace existing columns from it). The method to
 * use here is {@link #createBufferedDataTable(BufferedDataTable, 
 * ColumnRearranger, ExecutionMonitor)}. When the workflow is saved, only the
 * columns that have changed are stored to disc.</dd>
 * <a name="new_spec"/>
 * <dt><strong>The table spec of the input changes</strong></dt>
 * <dd>This happens for nodes that rename a column or add some properties to
 * the table spec. The input data itself is left untouched. Use the
 * {@link #createSpecReplacerTable(BufferedDataTable, DataTableSpec)} here.</dd>
 * </dl>
 * 
 * <p>Apart from creating BufferedDataTable, objects of this class are also 
 * reponsible to report progress information. See the super class for more 
 * information.
 * 
 * @author wiswedel, University of Konstanz
 */
public class ExecutionContext extends ExecutionMonitor {

    private final Node m_node;

    /** Creates new object based on a progress monitor and a node as parent
     * of any created buffered data table. 
     * @param progMon To report progress to.
     * @param node The parent of any BufferedDataTable being created.
     */
    public ExecutionContext(
            final NodeProgressMonitor progMon, final Node node) {
        super(progMon);
        if (node == null) {
            throw new NullPointerException("Argument must not be null.");
        }
        m_node = node;
    }

    /**
     * Caches the table argument and returns a reference to a BufferedDataTable
     * wrapping the content. When saving the workflow, the entire data is
     * written to disc. This method is provided for convenience. (All it does
     * is to create a BufferedDataContainer, adding the rows to it and 
     * returning a handle to it.) 
     * <p>This method refers to the first way of storing data, 
     * see <a href="#new_data">here</a>.
     * @param table The table to cache.
     * @param subProgressMon The execution monitor to report progress to. In
     * most cases this is the object on which this method is invoked. It may 
     * however be an sub progress monitor.
     * @return A table ready to be returned in the execute method.
     * @throws CanceledExecutionException If canceled.
     */
    public BufferedDataTable createBufferedDataTable(final DataTable table,
            final ExecutionMonitor subProgressMon)
            throws CanceledExecutionException {
        BufferedDataContainer c = createDataContainer(table.getDataTableSpec(),
                true);
        int row = 0;
        try {
            for (RowIterator it = table.iterator(); it.hasNext(); row++) {
                DataRow next = it.next();
                String message = "Caching row #" + (row + 1) + " (\""
                        + next.getKey() + "\")";
                subProgressMon.setMessage(message);
                subProgressMon.checkCanceled();
                c.addRowToTable(next);
            }
        } finally {
            c.close();
        }
        BufferedDataTable out = c.getTable();
        out.setOwnerRecursively(m_node);
        return out;
    }

    /** Performs the creation of buffered datatables for an array of DataTables.
     * @param tables The tables to cache.
     * @param exec The execution monitor for progress, cancel
     * @return The cached array of tables.
     * @throws CanceledExecutionException If canceled.
     * @see #createBufferedDataTable(DataTable, ExecutionMonitor)
     */
    public BufferedDataTable[] createBufferedDataTables(
            final DataTable[] tables, final ExecutionMonitor exec)
            throws CanceledExecutionException {
        BufferedDataTable[] temp = new BufferedDataTable[tables.length];
        for (int i = 0; i < tables.length; i++) {
            temp[i] = createBufferedDataTable(tables[i], exec);
        }
        return temp;

    }


    /**
     * Creates a container to which rows can be added. Use this method if
     * you sequentially generate new rows. Add those by using the 
     * <code>addRow(DataRow)</code> method and finally close the container and
     * get the result by invoking <code>getTable()</code>. All rows will be
     * cached. 
     * <p>This method refers to the first way of storing data, 
     * see <a href="#new_data">here</a>.
     * @param spec The spec to open the container.
     * @return A container to which rows can be added and which provides
     * the <code>BufferedDataTable</code>.
     * @throws NullPointerException If the argument is <code>null</code>.
     */
    public BufferedDataContainer createDataContainer(final DataTableSpec spec) {
        return createDataContainer(spec, true);
    }

    /**
     * Creates a container to which rows can be added. Use this method if
     * you sequentially generate new rows. Add those by using the 
     * <code>addRow(DataRow)</code> method and finally close the container and
     * get the result by invoking <code>getTable()</code>. All rows will be
     * cached. 
     * <p>This method refers to the first way of storing data, 
     * see <a href="#new_data">here</a>.
     * @param spec The spec to open the container.
     * @param initDomain If the domain information from the argument shall
     * be used to initialize the domain (min, max, possible values). If false,
     * the domain will be determined on the fly.
     * @return A container to which rows can be added and which provides
     * the <code>BufferedDataTable</code>.
     * @throws NullPointerException If the spec argument is <code>null</code>.
     */
    public BufferedDataContainer createDataContainer(final DataTableSpec spec,
            final boolean initDomain) {
        return new BufferedDataContainer(spec, initDomain, m_node);
    }

    /**
     * Creates a new <code>BufferedDataTable</code> based on a given input table
     * (<code>in</code>) whereby only some of the columns of <code>in</code>
     * have changed. 
     * <p>When the workflow is saved, only the columns that changed will be 
     * stored to disc, see also the class description for 
     * <a href="#new_column">details</a>.
     * @param in The input table, i.e. reference table.
     * @param rearranger The object which performs the reassembling of columns.
     * @param subProgressMon Typically the object on which this method is 
     * performed unless the processing is only a part of the total work.
     * @return A new table which can be returned in the execute method.
     * @throws CanceledExecutionException If canceled.
     */
    public BufferedDataTable createBufferedDataTable(
            final BufferedDataTable in, final ColumnRearranger rearranger,
            final ExecutionMonitor subProgressMon)
            throws CanceledExecutionException {
        RearrangeColumnsTable t = RearrangeColumnsTable.create(
                rearranger, in, subProgressMon);
        BufferedDataTable out = new BufferedDataTable(t);
        out.setOwnerRecursively(m_node);
        return out;
    }

    /**
     * Creates a new <code>BufferedDataTable</code> based on a given input table
     * (<code>in</code>) whereby only the table spec of it has changed.
     * <p>When the workflow is saved, only the spec needs to be saved, see also
     * the class description for <a href="#new_spec">details</a>.
     * @param in The input table, i.e. reference table.
     * @param newSpec The new table spec of <code>in</code>.
     * @return A new table which can be returned in the execute method.
     */
    public BufferedDataTable createSpecReplacerTable(
            final BufferedDataTable in, final DataTableSpec newSpec) {
        TableSpecReplacerTable t = new TableSpecReplacerTable(in, newSpec);
        BufferedDataTable out = new BufferedDataTable(t);
        out.setOwnerRecursively(m_node);
        return out;
    }
}
