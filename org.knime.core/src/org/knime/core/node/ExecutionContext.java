/*
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * -------------------------------------------------------------------
 *
 */
package org.knime.core.node;

import java.util.HashMap;

import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowIterator;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.ConcatenateTable;
import org.knime.core.data.container.ContainerTable;
import org.knime.core.data.container.JoinedTable;
import org.knime.core.data.container.RearrangeColumnsTable;
import org.knime.core.data.container.TableSpecReplacerTable;
import org.knime.core.data.container.WrappedTable;
import org.knime.core.node.workflow.SingleNodeContainer.MemoryPolicy;

/**
 * An <code>ExecutionContext</code> provides storage capacities during a
 * {@link org.knime.core.node.NodeModel#execute( BufferedDataTable[],
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
 * use here is {@link #createColumnRearrangeTable(BufferedDataTable,
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
 * responsible to report progress information. See the super class for more
 * information.
 *
 * @author Bernd Wiswedel, University of Konstanz
 */
public class ExecutionContext extends ExecutionMonitor {

    private final Node m_node;
    private final MemoryPolicy m_memoryPolicy;
    private final HashMap<Integer, ContainerTable> m_globalTableRepository;
    private final HashMap<Integer, ContainerTable> m_localTableRepository;

    /** Creates new object based on a progress monitor and a node as parent
     * of any created buffered data table.
     * @param progMon To report progress to.
     * @param node The parent of any BufferedDataTable being created.
     * themselves; used internally to identify tables that serialize blob cells.
     * @deprecated Use the constructor with a table repository argument instead.
     *             This constructor potentially does not support serialization
     *             of blobs.
     */
    @Deprecated
    public ExecutionContext(final NodeProgressMonitor progMon, final Node node) {
        // as it is deprecated we don't introduce an argument for mem policy
        this(progMon, node, MemoryPolicy.CacheSmallInMemory,
                new HashMap<Integer, ContainerTable>());
    }

    /**
     * Creates new object based on a progress monitor and a node as parent of
     * any created buffered data table.
     *
     * @param progMon To report progress to.
     * @param node The parent of any BufferedDataTable being created.
     * @param policy the policy according to which created BufferedDataTables
     *            behave
     * @param tableRepository A map to which BufferedDataTables register
     *            themselves; used internally to identify tables that serialize
     *            blob cells.
     */
    public ExecutionContext(final NodeProgressMonitor progMon, final Node node,
            final MemoryPolicy policy,
            final HashMap<Integer, ContainerTable> tableRepository) {
        super(progMon);
        if (node == null || tableRepository == null) {
            throw new NullPointerException("Argument must not be null.");
        }
        m_node = node;
        m_memoryPolicy = policy;
        m_globalTableRepository = tableRepository;
        m_localTableRepository = new HashMap<Integer, ContainerTable>();
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
        if (table instanceof BufferedDataTable) {
            return (BufferedDataTable) table;
        }
        BufferedDataContainer c = createDataContainer(
                table.getDataTableSpec(), true);
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
            temp[i] = createBufferedDataTable(tables[i], 
                    exec.createSubProgress(1.0 / tables.length));
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
        return createDataContainer(spec, initDomain, -1);
    }

    /**
     * Creates a container to which rows can be added, overwriting the
     * node's memory policy. This method has the same behavior as
     * {@link #createDataContainer(DataTableSpec, boolean)} except for the
     * last argument <code>maxCellsInMemory</code>. It controls the memory
     * policy of the data container (which is otherwise controlled by a user
     * setting in the dialog).
     *
     * <p>
     * <b>Note:</b> It's strongly advised to use
     * {@link #createDataContainer(DataTableSpec, boolean)} instead of this
     * method as the above method realizes the memory policy specified by the
     * user. Use this method only if you have good reasons to do so
     * (for instance if you create many containers, whose default memory
     * options would yield a high accumulated memory consumption).
     * @param spec The spec to open the container.
     * @param initDomain If the domain information from the argument shall
     * be used to initialize the domain (min, max, possible values). If false,
     * the domain will be determined on the fly.
     * @param maxCellsInMemory Number of cells to be kept in memory, especially
     * 0 forces the table to write to disk immediately. A value smaller than 0
     * will respect the user setting (as defined by the accompanying node).
     * @return A container to which rows can be added and which provides
     * the <code>BufferedDataTable</code>.
     * @throws NullPointerException If the spec argument is <code>null</code>.
     */
    public BufferedDataContainer createDataContainer(final DataTableSpec spec,
            final boolean initDomain, final int maxCellsInMemory) {
        return new BufferedDataContainer(spec, initDomain, m_node,
                m_memoryPolicy, maxCellsInMemory, m_globalTableRepository,
                m_localTableRepository);
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
    public BufferedDataTable createColumnRearrangeTable(
            final BufferedDataTable in, final ColumnRearranger rearranger,
            final ExecutionMonitor subProgressMon)
            throws CanceledExecutionException {
        RearrangeColumnsTable t = RearrangeColumnsTable.create(
                rearranger, in, subProgressMon, this);
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

   /**
    * Creates a new <code>BufferedDataTable</code> that simply wraps the
    * argument table. This is useful when a node just passes on the input table,
    * for example. If the implementation of NodeModel does not use this method
    * (but simply returns the input table directy), the framework will perform
    * the wrapping operation.
    * @param in The input table to wrap.
    * @return A new table which can be returned in the execute method.
    * @throws NullPointerException If the argument is null.
    */
    public BufferedDataTable createWrappedTable(final BufferedDataTable in) {
        WrappedTable t = new WrappedTable(in);
        BufferedDataTable out = new BufferedDataTable(t);
        out.setOwnerRecursively(m_node);
        return out;
    }

    /** Creates a new {@link BufferedDataTable}, which is  row-wise
     * concatenation of the argument tables. The order of the rows in the
     * returned table is defined through the order of the argument array
     * <code>tables</code> (the <code>BufferedDataTable</code> at index 0
     * provides the first set of rows.
     *
     * <p> The table specs of the argument tables must structurally match
     * (i.e. order of columns, column count, column names, and types). The
     * column domains (min, max and possible values) and properties will be
     * merged. (The merge of properties is based on a maximum intersection of
     * all properties.)
     *
     * <p>Property handlers (such as
     * {@link org.knime.core.data.property.ColorHandler Color},
     * {@link org.knime.core.data.property.ShapeHandler Shape}, and
     * {@link org.knime.core.data.property.SizeHandler}) attached to any of the
     * input columns need to be the same for all respective columns in the
     * remaining tables.
     *
     * <p>The {@link org.knime.core.data.RowKey RowKeys} must be unique, other
     * wise this method throws an exception.
     * @param exec For cancel checks (this method iterates all rows to
     * ensure uniqueness) and progress.
     * @param tables An array of tables to concatenate,
     * must not be <code>null</code> or empty.
     * @return The concatenated table.
     * @throws CanceledExecutionException If canceled.
     * @throws IllegalArgumentException If the table specs violate any
     * constraint mentioned above, the row keys are not unique, or the array
     * is empty.
     * @throws NullPointerException If any argument is <code>null</code>.
     */
    public BufferedDataTable createConcatenateTable(
            final ExecutionMonitor exec, final BufferedDataTable... tables)
        throws CanceledExecutionException {
        ConcatenateTable t = ConcatenateTable.create(exec, tables);
        BufferedDataTable out = new BufferedDataTable(t);
        out.setOwnerRecursively(m_node);
        return out;
    }

    /**
     * Creates a new {@link BufferedDataTable} that is a column based join of
     * the argument tables. The <code>left</code> table argument contributes
     * the first set of columns and the <code>right</code> table argument the
     * second set of columns. The tables must not contain duplicate columns
     * (i.e. columns with the same name). They do need to contain the same set
     * of rows though, i.e. the same row count and equal row keys in identical
     * order. If any of these constraints is not met, this method throws and
     * <code>IllegalArgumentException</code>.
     *
     * <p>
     * This method will traverse both tables ones to ensure that the row keys
     * are identical and are returned in the same order. It reports progress for
     * this sanity check to the <code>exec</code> argument.
     *
     * <p>
     * The returned table is only a view on both argument tables, i.e. any
     * subsequent iteration is carried out on the argument tables. This also
     * means that the returned table does only acquire little main memory and no
     * disc memory at all.
     *
     * @param left The table contributing the first set of columns.
     * @param right The table contributing the second set of columns.
     * @param exec For progress information and cancel checks, consider to use a
     *            {@link ExecutionMonitor#createSubProgress(double) sub
     *            execution monitor} when joining two tables is only part of the
     *            whole work.
     * @return A buffered data table as join of the two argument tables.
     * @throws CanceledExecutionException If progress has been canceled.
     * @throws NullPointerException If any argument is <code>null</code>.
     * @throws IllegalArgumentException If the tables contain duplicate columns
     *             or non-matching rows.
     * @see DataTableSpec#DataTableSpec(DataTableSpec, DataTableSpec)
     */
    public BufferedDataTable createJoinedTable(final BufferedDataTable left,
            final BufferedDataTable right, final ExecutionMonitor exec)
        throws CanceledExecutionException {
        JoinedTable jt = JoinedTable.create(left, right, exec);
        BufferedDataTable out = new BufferedDataTable(jt);
        out.setOwnerRecursively(m_node);
        return out;
    }

    /**
     * Creates a new execution context with a different max progress value.
     * This method is the counterpart to {@link #createSubProgress(double)}
     * {@link ExecutionMonitor}. A sub execution contexts has the same
     * properties as this object but it only reports progress to a limited value
     * of <code>maxProg</code>. It can therefore be used in, e.g. utility
     * classes which report progress in [0, 1], but whose progress is only
     * a small contribution to the overall progress.
     * @param maxProg The maximum progress, must be in [0,1]
     * @return A new execution context.
     */
    public ExecutionContext createSubExecutionContext(final double maxProg) {
        NodeProgressMonitor subProgress = createSubProgressMonitor(maxProg);
        return new ExecutionContext(subProgress, m_node, m_memoryPolicy,
                m_globalTableRepository);
    }

    /**
     * Creates a new execution context with a different max progress value and
     * swallowing any report messages. This method is the counterpart to
     * {@link #createSilentSubProgress(double)} in {@link ExecutionMonitor}. A
     * sub execution contexts has the same properties as this object but it only
     * reports progress to a limited value of <code>maxProg</code>. It will
     * also ignore any message, which is set using the
     * {@link #setMessage(String)} method. It can therefore be used in, e.g.
     * utility classes which report progress in [0, 1], but whose progress is
     * only a small contribution to the overall progress.
     *
     * @param maxProg The maximum progress, must be in [0,1]
     * @return A new execution context.
     */
    public ExecutionContext createSilentSubExecutionContext(
            final double maxProg) {
        NodeProgressMonitor subProgress =
            createSilentSubProgressMonitor(maxProg);
        return new ExecutionContext(subProgress, m_node, m_memoryPolicy,
                m_globalTableRepository);
    }

    /**
     * Get reference to the local table repository. It contains
     * <code>ContainerTable</code> objects that have been created during the
     * execution of a node. Some of which will be put into the global
     * repository after execution.
     * @return The local table repository.
     */
    HashMap<Integer, ContainerTable> getLocalTableRepository() {
        return m_localTableRepository;
    }
}
