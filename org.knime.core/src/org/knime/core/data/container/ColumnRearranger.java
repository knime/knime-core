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
 * History
 *   Jun 20, 2006 (wiswedel): created
 */
package org.knime.core.data.container;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Vector;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;


/**
 * Meta object to describe in which way a table shall be modified (column-based)
 * to create a new table.
 * <p>
 * A <code>ColumnRearranger</code> is used in <code>NodeModel</code>
 * implementations that perform column based operations on columns of the input
 * table, such as a column filter node (that simply hides some columns from the
 * input table) or a node that appends/replaces certain columns.
 * 
 * <p>
 * The following example demonstrates the usage of a
 * <code>ColumnRearranger</code> to append a column to a given table, which
 * contains the sum of the first two columns of the input table (given that
 * these columns are numeric). The node model implementation would contain code
 * as follows.
 * 
 * <pre>
 * public BufferedDataTable[] execute(BufferedDataTable[] in, 
 *     ExecutionContext exec) throws Exception {
 *     ColumnRearranger c = createColumnRearranger(in[0].getDataTableSpec());
 *     BufferedDataTable out = exec.createColumnRearrangeTable(in[0], c, exec);
 *     return new BufferedDataTable[]{out};
 * }
 * 
 * public DataTableSpec[] configure(DataTableSpec[] in)
 *         throws InvalidSettingsException {
 *     DataColumnSpec c0 = in[0].getColumnSpec(0);
 *     DataColumnSpec c1 = in[0].getColumnSpec(1);
 *     if (!c0.getType().isCompatibleTo(DoubleValue.class)) {
 *         throw new InvalidSettingsException(
 *           &quot;Invalid type at first column.&quot;);
 *     }
 *     if (!c1.getType().isCompatibleTo(DoubleValue.class)) {
 *         throw new InvalidSettingsException(
 *           &quot;Invalid type at second column.&quot;);
 *     }
 *     ColumnRearranger c = createColumnRearranger(in[0]);
 *     DataTableSpec result = c.createSpec();
 *     return new DataTableSpec[]{result};
 * }
 * </pre>
 * 
 * The createColumnRearranger method is a local helper method, which is called
 * from both the <code>execute</code> and the <code>configure</code> method:
 * 
 * <pre>
 * private ColumnRearranger createColumnRearranger(DataTableSpec in) {
 *     ColumnRearranger c = new ColumnRearranger(in);
 *     // column spec of the appended column
 *     DataColumnSpec newColSpec = new DataColumnSpecCreator(
 *     &quot;sum_of_0_and_1&quot;, DoubleCell.TYPE).createSpec();
 *     // utility object that performs the calculation
 *     CellFactory factory = new SingleCellFactory(newColSpec) {
 *         public DataCell getCell(DataRow row) {
 *             DataCell c0 = row.getCellAt(0);
 *             DataCell c1 = row.getCellAt(1);
 *             if (c0.isMissing() || c1.isMissing()) {
 *                 return DataType.getMissingCell();
 *             } else {
 *                 // configure method has checked if column 0 and 1 are numeric
 *                 // safe to type cast
 *                 double d0 = ((DoubleValue)c0).getDoubleValue();
 *                 double d1 = ((DoubleValue)c1).getDoubleValue();
 *                 return new DoubleCell(d0 + d1);
 *             }
 *         }
 *     };
 *     c.append(factory);
 *     return c;
 * }
 * </pre>
 * 
 * @see org.knime.core.data.container.CellFactory
 * @see org.knime.core.node.ExecutionContext#createColumnRearrangeTable(
 *      org.knime.core.node.BufferedDataTable, ColumnRearranger,
 *      org.knime.core.node.ExecutionMonitor)
 * @author Bernd Wiswedel, University of Konstanz
 */
public final class ColumnRearranger {
    
    private final Vector<SpecAndFactoryObject> m_includes;
    private final DataTableSpec m_originalSpec;
    
    /** Creates new object based on the spec of the table underlying the
     * newly created table.
     * @param original The table which serves as reference.
     * @throws NullPointerException If the argument is <code>null</code>.
     */
    public ColumnRearranger(final DataTableSpec original) {
        m_includes = new Vector<SpecAndFactoryObject>();
        m_includes.ensureCapacity(original.getNumColumns());
        for (int i = 0; i < original.getNumColumns(); i++) {
            m_includes.add(new SpecAndFactoryObject(
                    original.getColumnSpec(i), i));
        }
        m_originalSpec = original;
    }
    
    /** Removes all columns from the current settings, whose index is not 
     * contained in the argument <code>colIndices</code>. In other words,
     * the number of columns in the spec that would be created after this method
     * has been called, is <code>colIndices.length</code>.
     * 
     * <p>
     * Note: Any subsequent invocation of this method or any other method that
     * refers to column indices is based on the reduced set of columns.
     * @param colIndices The indices of the columns to keep.
     * @throws IndexOutOfBoundsException If any value in the argument array
     * is out of bounds (smaller 0 or greater/equal to the current number of
     * columns) or the array contains duplicates.
     * @throws NullPointerException If the argument is <code>null</code> 
     */
    public void keepOnly(final int... colIndices) {
        HashSet<Integer> hash = new HashSet<Integer>();
        final int currentSize = m_includes.size();
        for (int i : colIndices) {
            if (i < 0 || i >= currentSize) {
                throw new IndexOutOfBoundsException("Invalid index: " + i);
            }
            hash.add(i);
        }
        if (hash.size() != colIndices.length) {
            throw new IndexOutOfBoundsException("Duplicates in argument: "
                    + Arrays.toString(colIndices));
        }
        // traverse backwards!
        for (int i = currentSize - 1; i >= 0; i--) {
            if (!hash.remove(i)) {
                m_includes.remove(i);
            }
        }
        assert m_includes.size() == colIndices.length;
        assert hash.isEmpty();
    }
    
    /** Removes all columns from the current settings, whose column name is not 
     * contained in the argument <code>colNames</code>. In other words,
     * the number of columns in the spec that would be created after this method
     * has been called, is <code>colNames.length</code>.
     * 
     * @param colNames The names of the columns to keep.
     * @throws IllegalArgumentException If any value in the argument array
     * is invalid, i.e. null or not contained in the current set of columns or
     * if the array contains duplicates.
     * @throws NullPointerException If the argument is <code>null</code> 
     */
    public void keepOnly(final String... colNames) {
        HashSet<String> found = new HashSet<String>(Arrays.asList(colNames));
        // check for null elements
        if (found.contains(null)) {
            throw new IllegalArgumentException("Argument contains null: "
                    + Arrays.toString(colNames));
        }
        // check for duplicates
        if (found.size() != colNames.length) {
            throw new IllegalArgumentException("Argument contains duplicates: "
                    + Arrays.toString(colNames));
        }
        for (Iterator<SpecAndFactoryObject> it = m_includes.iterator(); 
            it.hasNext();) {
            SpecAndFactoryObject cur = it.next();
            if (!found.remove(cur.getColSpec().getName())) {
                it.remove();
            }
        }
        if (!found.isEmpty()) {
            throw new IllegalArgumentException(
                    "No such column name(s) in " + getClass().getSimpleName()
                    + ": " + Arrays.toString(found.toArray()));
        }
        assert m_includes.size() == colNames.length;
    }
    
    /** Removes all columns whose index is contained in the argument array.
     *
     * <p>
     * Note: Any subsequent invocation of this method or any other method that
     * refers to column indices is based on the reduced set of columns.
     * @param colIndices The indices of the columns to remove.
     * @throws IndexOutOfBoundsException If any element in the array is out of
     * bounds (i.e. smaller than 0 or greater/equal the current number of 
     * columns) or the argument contains duplicates.
     * @throws NullPointerException If the argument is <code>null</code>.
     */
    public void remove(final int... colIndices) {
        int[] copy = new int[colIndices.length];
        System.arraycopy(colIndices, 0, copy, 0, copy.length);
        Arrays.sort(copy);
        // check for duplicates
        for (int i = 1; i < copy.length; i++) {
            if (copy[i] == copy[i - 1]) {
                throw new IndexOutOfBoundsException("Duplicates encountered "
                        + Arrays.toString(colIndices));
            }
        }
        for (int i = copy.length - 1; i >= 0; i--) {
            m_includes.remove(copy[i]);
        }
    }
    
    /** Removes all columns from the current set of columns whose name is 
     * contained in the argument array.
     * @param colNames The names of the columns to remove.
     * @throws NullPointerException If any element is null
     */
    public void remove(final String... colNames) {
        HashSet<String> found = new HashSet<String>(Arrays.asList(colNames));
        // check for null elements
        if (found.contains(null)) {
            throw new IllegalArgumentException("Argument contains null: "
                    + Arrays.toString(colNames));
        }
        // check for duplicates
        if (found.size() != colNames.length) {
            throw new IllegalArgumentException("Argument contains duplicates: "
                    + Arrays.toString(colNames));
        }
        for (Iterator<SpecAndFactoryObject> it = m_includes.iterator(); 
            it.hasNext();) {
                SpecAndFactoryObject cur = it.next();
                if (found.remove(cur.getColSpec().getName())) {
                    it.remove();
                }
            }
        if (!found.isEmpty()) {
            throw new IllegalArgumentException(
                    "No such column name(s) in " + getClass().getSimpleName()
                    + ": " + Arrays.toString(found.toArray()));
        }
    }
    
    /** Get the current index of the column with name <code>colName</code>. 
     * Note, the index may change if any of the modifier methods is called.
     * @param colName The name of the column to find.
     * @return The index of the column whose name equals <code>colName</code>
     * or -1 if it is not contained.
     * @throws NullPointerException If the argument is <code>null</code>.
     */
    public int indexOf(final String colName) {
        if (colName == null) {
            throw new NullPointerException("Argument must not be null.");
        }
        for (int i = 0; i < m_includes.size(); i++) {
            SpecAndFactoryObject cur = m_includes.get(i);
            if (cur.getColSpec().getName().equals(colName)) {
                return i; 
            }
        }
        return -1;
    }
    
    /**
     * Moves the column at index <code>from</code> to the index <code>to</code>.
     * This method can be used to re-sort the set of columns.
     * 
     * If <code>from</code> is greater than <code>to</code>, then the column 
     * indices will be affected as follows:
     * <ul>
     * <li>Any column before <code>to</code> (excl) and after <code>from</code>
     * (excl) will have the index it had previously.</li>
     * <li>Any column between <code>to</code> (incl) and <code>from</code>
     * will have an index shifted upward by one.</li>
     * </ul>
     * If the <code>from</code> is smaller than <code>to</code>, the column
     * indices will change as follows:
     * <ul>
     * <li>The columns before <code>from</code> (excl) and after <code>to</code>
     * (incl) will have the index they had previously.</li>
     * <li>The columns between <code>from</code> and <code>to</code> (excl)
     * will have an index one less than they had before.</li>
     * </ul>
     * 
     * <p> This method is inherently expensive as it shifts elements 
     * back and forth. If you change the order of all columns (and hence would 
     * need to call this method often), use the permute method instead. 
     *
     * @param from The from index. 
     * @param to The destination index.
     * @throws IndexOutOfBoundsException If any of the values is out of range,
     * i.e. less than 0 or greater or equal to the current set of columns. 
     * @see #move(String, int)
     * @see #permute(String[])
     */
    public void move(final int from, final int to) {
        if (from < to) {
            SpecAndFactoryObject val = m_includes.get(from);
            m_includes.insertElementAt(val, to);
            m_includes.remove(from);
        } else {
            SpecAndFactoryObject val = m_includes.remove(from);
            m_includes.insertElementAt(val, to);
        }
    }
    
    /**
     * Moves the column named <code>colName</code> to the index
     * <code>to</code>. This method can be used to re-sort the set of
     * columns. The implementation first determines the index of the
     * argument column and then calls {@link #move(int, int)} with the correct
     * arguments.
     * 
     * <p>This method is expensive if called multiple times (for instance when
     * re-sorting the entire table). See the {@link #move(int, int)} method for
     * details.
     * @param colName The name of the column in question.
     * @param to The destination index.
     * @throws IndexOutOfBoundsException If <code>to</code> is out of range,
     *             i.e. less than 0 or greater or equal to the current set of
     *             columns.
     * @throws NullPointerException If <code>colName</code> is null.
     * @throws IllegalArgumentException 
     *             If there is no column <code>colName</code>.
     * @see #move(int, int)
     * @see #permute(String[])
     */
    public void move(final String colName, final int to) {
        for (int i = 0; i < m_includes.size(); i++) {
            if (colName.equals(m_includes.get(i).getColSpec().getName())) {
                move(i, to);
                return;
            }
        }
        throw new IllegalArgumentException(
                "No such column \"" + colName + "\"");
    }
    
    /** Changes the order of the columns according to the argument array. 
     * The array must contain the column names in the desired order. 
     * If this rearrange object contains names that are not contained in the
     * argument array, those columns are moved to the end of the new ordering. 
     * 
     * <p>This method is efficient compared to the implementation of the 
     * {@link #move(int, int)} method and should be used if the entire table 
     * is to be re-sorted. 
     * 
     * @param colNamesInOrder The new column ordering. It may contain fewer 
     * names than actually present in this re-arrange object. However, it must 
     * not contain unknown columns, nor should it contain duplicates or null
     * elements.
     * @throws NullPointerException If the argument is <code>null</code> or 
     *          contains <code>null</code> elements.
     * @throws IllegalArgumentException If the array contains duplicates or 
     *          unknown columns.
     */
    public void permute(final String[] colNamesInOrder) {
        final HashMap<String, Integer> order = new HashMap<String, Integer>();
        for (int i = 0; i < colNamesInOrder.length; i++) {
            String name = colNamesInOrder[i];
            if (name == null) {
                throw new NullPointerException(
                        "List must not contain null elements");
            }
            if (order.put(name, i) != null) {
                throw new IllegalArgumentException(
                        "Duplicate column names in argument array: " + name);
            }
        }
        // add remaining columns to order
        for (SpecAndFactoryObject spec : m_includes) {
            if (!order.containsKey(spec.getColSpec().getName())) {
                order.put(spec.getColSpec().getName(), order.size());
            }
        }
        // argument array contains unknown column names
        if (order.size() != m_includes.size()) {
            assert order.size() > m_includes.size();
            // determine bad column names
            for (SpecAndFactoryObject spec : m_includes) {
                order.remove(spec.getColSpec().getName());
            }
            throw new IllegalArgumentException("Column name(s) not found: "
                    + Arrays.toString(order.keySet().toArray()));
        }
        Collections.sort(m_includes, new Comparator<SpecAndFactoryObject>() {
           /** {@inheritDoc} */
            public int compare(final SpecAndFactoryObject o1, 
                    final SpecAndFactoryObject o2) {
                String s1 = o1.getColSpec().getName();
                String s2 = o2.getColSpec().getName();
                return order.get(s1).compareTo(order.get(s2)); 
            } 
        });
    }
    
    /** Inserts the columns provided by <code>fac</code> at a given position.
     * Any columns before that position stay where they are, the column at
     * the position and any thereafter are shifted to the right by the number
     * of columns provided by <code>fac</code>.
     * @param position The position (index) where to insert the new columns.
     * @param fac The factory from which we get the new columns.
     * @throws IndexOutOfBoundsException If position is invalid.
     * @throws NullPointerException If <code>fac</code> is <code>null</code>.
     */
    public void insertAt(final int position, final CellFactory fac) {
        DataColumnSpec[] colSpecs = fac.getColumnSpecs();
        SpecAndFactoryObject[] ins = new SpecAndFactoryObject[colSpecs.length];
        for (int i = 0; i < ins.length; i++) {
            ins[i] = new SpecAndFactoryObject(fac, i, colSpecs[i]);
        }
        // traverse backwards! Important here.
        for (int i = ins.length - 1; i >= 0; i--) {
            m_includes.insertElementAt(ins[i], position);
        }
    }

    /** Appends the columns provided by <code>fac</code> to the end of
     * the current column set.
     * @param fac The factory from which we get the new columns.
     * @throws NullPointerException If <code>fac</code> is <code>null</code>.
     */
    public void append(final CellFactory fac) {
        insertAt(m_includes.size(), fac);
    }
    
    /** Replaces a single column. The target column is specified by the 
     * <code>colName</code> argument and the new column is given through the
     * <code>newCol</code> cell factory.
     * <p><strong>Note:</strong>The newCol argument must only specify one 
     * single column. If you need to replace one column by many others, use
     * the <code>remove(colName)</code> in conjunction with the 
     * <code>insertAt(position, newCol)</code> method. 
     * @param newCol The column factory for the <strong>single</strong> new 
     *         column.
     * @param colName The name of the column to replace.
     * @throws NullPointerException If any argument is null.
     * @throws IndexOutOfBoundsException If newCol provides not exactly one new
     *          column
     * @throws IllegalArgumentException If <code>colName</code> is not 
     *          contained in the current set of columns.
     */
    public void replace(final CellFactory newCol, final String colName) {
        int index = indexOf(colName);
        if (index < 0) {
            throw new IllegalArgumentException("No such column: " + colName);
        }
        replace(newCol, index);
    }
    
    /** Replaces a set of columns. The columns to be replaced are specified by
     * the <code>colIndex</code> argument and the new columns is given through
     * the <code>newCol</code> cell factory.
     * <p><strong>Note:</strong>The newCol argument must specify exactly as 
     * many columns as there are in <code>colIndex</code>. If you want to 
     * remove more (or fewer) columns as given by <code>fac</code>, you can
     * always accomplish this using the remove, indexOf, and/or inserAt methods.
     * @param fac The column factory for the new columns.
     * @param colIndex The indices of the columns to be replaced.
     * @throws NullPointerException If any argument is null.
     * @throws IndexOutOfBoundsException If <code>fac</code> provides not 
     *          exactly as many columns as colIndex.length or the colIndex
     *          argument contains invalid entries.
     */
    public void replace(final CellFactory fac, final int... colIndex) {
        DataColumnSpec[] colSpecs = fac.getColumnSpecs();
        if (colSpecs.length != colIndex.length) {
            throw new IndexOutOfBoundsException(
                    "Arguments do not apply to same number of columns: "
                    + colSpecs.length + " vs. " + colIndex.length + ")");
        }
        for (int i = 0; i < colSpecs.length; i++) {
            remove(colIndex[i]);
            SpecAndFactoryObject s = 
                new SpecAndFactoryObject(fac, i, colSpecs[i]);
            m_includes.insertElementAt(s, colIndex[i]);
        }
    }
    
    /** Access method for the internal data structure.
     * @return The current set of columns.
     */
    Vector<SpecAndFactoryObject> getIncludes() {
        return m_includes;
    }
    
    /** Access method for the internal data structure.
     * @return The original spec as passed in the constructor.
     */
    DataTableSpec getOriginalSpec() {
        return m_originalSpec;
    }
    
    /** Creates the data table spec on the current set of columns. Subsequent
     * changes to this object will also change the return value of this method.
     * You may want to call this method during configure in order to create the
     * output spec of your node.
     * @return The table spec reflecting the current set of columns.
     */
    public DataTableSpec createSpec() {
        final int size = m_includes.size();
        DataColumnSpec[] colSpecs = new DataColumnSpec[size];
        for (int i = 0; i < size; i++) {
            colSpecs[i] = m_includes.get(i).getColSpec();
        }
        return new DataTableSpec(colSpecs);
    }
    
    /** Utility class that helps us with internal data structures. */
    static final class SpecAndFactoryObject {
        private final CellFactory m_factory;
        private final DataColumnSpec m_colSpec;
        private final int m_columnInFactory;
        private final int m_originalIndex;
        
        private SpecAndFactoryObject(
                final DataColumnSpec colSpec, final int originalIndex) {
            m_colSpec = colSpec;
            m_columnInFactory = -1;
            m_factory = null;
            m_originalIndex = originalIndex;
        }
        
        private SpecAndFactoryObject(final CellFactory cellFactory, 
                final int index, final DataColumnSpec colSpec) {
            m_colSpec = colSpec;
            m_factory = cellFactory;
            m_columnInFactory = index;
            m_originalIndex = -1;
        }

        /**
         * @return Returns the colSpec.
         */
        final DataColumnSpec getColSpec() {
            return m_colSpec;
        }

        /**
         * @return Returns the columnInFactory.
         */
        final int getColumnInFactory() {
            return m_columnInFactory;
        }

        /**
         * @return Returns the factory.
         */
        final CellFactory getFactory() {
            return m_factory;
        }
        
        /**
         * @return If the column is created through a cell factory.
         */
        final boolean isNewColumn() {
            return m_factory != null;
        }

        /**
         * @return Returns the originalIndex.
         */
        final int getOriginalIndex() {
            return m_originalIndex;
        }
    }
    
}
