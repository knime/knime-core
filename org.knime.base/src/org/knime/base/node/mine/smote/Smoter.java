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
package org.knime.base.node.mine.smote;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Map;
import java.util.Random;

import org.knime.base.data.append.row.AppendedRowsTable;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.RowIterator;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;

/**
 * Implementation of the <a
 * href="http://www.cs.cmu.edu/afs/cs/project/jair/pub/volume16/chawla02a.pdf">
 * Smote</a> algorithm. It's more a controller for the algorithm, ok.
 * 
 * <p>
 * The algorithm is called SMOTE:
 * 
 * <pre>
 *  Chawla, N.V., Bowyer, K.W., Hall, L.O., Kegelmeyer, W.P. (2002) 
 *  &quot;SMOTE: Synthetic Minority Over-sampling Technique&quot;,
 *  Journal of Artificial Intelligence Research, Volume 16, pages 321-357.
 * </pre>
 * 
 * @author Bernd Wiswedel, University of Konstanz
 */
class Smoter {

    private final BufferedDataTable m_inTable;

    private final int m_targetCol;

    private final BufferedDataContainer m_container;

    private final LinkedHashMap<DataCell, MutableInt> m_inStats;

    private int m_appendCounter;
    
    private final Random m_random;

    /**
     * Creates a new instance given the input table <code>in</code> and the
     * target column <code>colName</code>.
     * 
     * @param in the input table
     * @param colName the target column with class information
     * @param exec monitor to get canceled status from 
     *  (may be <code>null</code>)
     * @param rand The random generator, may be <code>null</code>.
     * @throws CanceledExecutionException if execution is canceled
     */
    public Smoter(final BufferedDataTable in, final String colName,
            final ExecutionContext exec, final Random rand) 
        throws CanceledExecutionException {
        final int col = in.getDataTableSpec().findColumnIndex(colName);
        if (col < 0) {
            throw new IllegalArgumentException("Table doesn't contain column: "
                    + colName);
        }
        m_random =  (rand == null ? new Random() : rand);
        m_inTable = in;
        m_targetCol = col;
        DataTableSpec outSpec = createFinalSpec(in.getDataTableSpec());
        m_container = exec.createDataContainer(outSpec);
        m_inStats = new LinkedHashMap<DataCell, MutableInt>();
        for (DataRow next : in) {
            checkCanceled(exec);
            DataCell clas = next.getCell(col);
            MutableInt counter = m_inStats.get(clas);
            if (counter == null) {
                counter = new MutableInt(1);
                m_inStats.put(clas, counter);
            } else {
                // I hope he doesn't mind if I change the value underneath.
                counter.increment();
            }
        }
    }

    /**
     * Get iterator of all classes that occur in the target column.
     * 
     * @return all available classes
     */
    public Iterator<DataCell> getClassValues() {
        return m_inStats.keySet().iterator();
    }

    /**
     * Get frequency of a class name in the input table. The argument must be an
     * entry of the iterator that is returned by {@link #getClassValues()}.
     * 
     * @param name the class name
     * @return the frequency
     */
    public int getCount(final DataCell name) {
        MutableInt count = m_inStats.get(name);
        if (count == null) {
            return -1;
        }
        return count.getInt();
    }

    /**
     * Get name of the majority class, i.e. the class that occurs most often.
     * 
     * @return name of majority class.
     */
    public DataCell getMajorityClass() {
        int max = Integer.MIN_VALUE;
        DataCell maxCell = null;
        for (Map.Entry<DataCell, MutableInt> entry : m_inStats.entrySet()) {
            DataCell name = entry.getKey();
            int count = entry.getValue().getInt();
            if (count > max) {
                max = count;
                maxCell = name;
            }
        }
        return maxCell;
    }

    /**
     * Oversample the class <code>name</code> such that <code>count</code>
     * new rows are inserted. The <code>kNN</code> nearest neighbors are
     * chosen as reference.
     * 
     * @param name the class name
     * @param count add this amount of new rows
     * @param kNN k nearest neighbor parameter
     * @param exec monitor to get canceled status from 
     *  (may be <code>null</code>)
     * @throws CanceledExecutionException if execution is canceled
     */
    public void smote(final DataCell name, final int count, final int kNN,
            final ExecutionMonitor exec) throws CanceledExecutionException {
        int origCount = getCount(name);
        if (origCount < 0) {
            throw new IllegalArgumentException("No such value: " + name);
        }
        // how often each input record is used at least as reference
        int countAtLeast = count / origCount;
        assert countAtLeast * origCount <= count;
        int[] fixedPart = new int[countAtLeast * origCount];
        for (int i = 0; i < fixedPart.length; i++) {
            fixedPart[i] = i % origCount;
        }

        int[] shuffleMe = new int[origCount];
        for (int i = 0; i < shuffleMe.length; i++) {
            shuffleMe[i] = i;
        }
        // guess from the name what this line does.
        shuffleMe = shuffle(shuffleMe);
        int[] indexesToUse = new int[count];
        System.arraycopy(fixedPart, 0, indexesToUse, 0, fixedPart.length);
        // number of cells that have the luck to serve one extra time as
        // reference
        int lucky = count - fixedPart.length;
        System.arraycopy(shuffleMe, 0, indexesToUse, fixedPart.length, lucky);
        shuffleMe = null;
        fixedPart = null;
        Arrays.sort(indexesToUse);

        // the counter in the input table for this particular class value
        int classCounter = -1;
        int pointer = 0;
        RowIterator it = m_inTable.iterator();
        while (pointer < indexesToUse.length) {
            checkCanceled(exec);
            assert it.hasNext();
            DataRow next = it.next();
            if (!next.getCell(m_targetCol).equals(name)) {
                continue;
            }
            classCounter++;
            if (indexesToUse[pointer] == classCounter) {
                DataRow[] neighbors = determineNeighbors(next, kNN, exec);
                while (pointer < indexesToUse.length
                        && indexesToUse[pointer] == classCounter) {
                    DataRow newRow = populate(next, neighbors);
                    m_container.addRowToTable(newRow);
                    pointer++;
                    exec.setProgress(pointer / (double)count);
                }
            }
        }
    }

    /*
     * Helper that determine the k NN of a given row. @param ref The reference
     * vector @param kNN Number NN @return The nearest neighbor in an array.
     */
    private DataRow[] determineNeighbors(final DataRow ref, final int kNN,
            final ExecutionMonitor exec) throws CanceledExecutionException {
        final DataCell clas = ref.getCell(m_targetCol);
        LinkedList<RowAndDistance> list = new LinkedList<RowAndDistance>();
        for (DataRow n : m_inTable) {
            checkCanceled(exec);
            // a potential neighbor?
            if (n.getCell(m_targetCol).equals(clas) && !ref.equals(n)) {
                double dis = distance(ref, n);
                insertIntoList(list, n, dis, kNN);
            }
        }
        DataRow[] neighbors = new DataRow[list.size()];
        int i = 0;
        for (Iterator<RowAndDistance> it = list.iterator(); it.hasNext(); i++) {
            RowAndDistance os = it.next();
            neighbors[i] = os.getRow();
        }
        return neighbors;
        // TODO add asserts
    }

    /* Helper to put a row into a list of a given length. */
    private void insertIntoList(final LinkedList<RowAndDistance> list,
            final DataRow row, final double dis, final int kNN) {
        // insert into the list
        ListIterator<RowAndDistance> lI = list.listIterator(list.size());
        double lastdis = Double.POSITIVE_INFINITY;
        while (lI.hasPrevious() && lastdis > dis) {
            RowAndDistance last = lI.previous();
            lastdis = last.getDist();
        }
        if (lI.hasNext()) {
            lI.next();
        }
        lI.add(new RowAndDistance(row, dis));

        // truncate the end of the list
        if (list.size() <= kNN) {
            return;
        }
        lI = list.listIterator(kNN - 1);
        double maxDis = Double.POSITIVE_INFINITY;
        if (lI.hasNext()) {
            RowAndDistance last = lI.next();
            maxDis = last.getDist();
        }
        while (lI.hasNext()) {
            RowAndDistance last = lI.next();
            double d = last.getDist();
            if (d > maxDis) {
                lI.remove();
            }
        }
    }

    /* Determines the Euclidean distance of two rows. */
    private double distance(final DataRow row1, final DataRow row2) {
        double d = 0.0;
        for (int i = 0; i < row1.getNumCells(); i++) {
            DataType t = m_inTable.getDataTableSpec().getColumnSpec(i)
                    .getType();
            if (t.isCompatible(DoubleValue.class)) {
                double dis;
                DataCell fCell = row1.getCell(i);
                DataCell tCell = row2.getCell(i);
                if (fCell.isMissing() || tCell.isMissing()) {
                    dis = 0.0;
                } else {
                    DoubleValue cell1 = (DoubleValue)fCell;
                    DoubleValue cell2 = (DoubleValue)tCell;
                    dis = cell1.getDoubleValue() - cell2.getDoubleValue();
                }
                d += dis * dis;
            }
        }
        return Math.sqrt(d);
    }

    /*
     * populates a given row <code>ref</code>, choosing any neighbor from
     * <code>neighbors</code>.
     */
    private DataRow populate(final DataRow ref, final DataRow[] neighbors) {
        final double fraction = m_random.nextDouble();
        final DataRow neigh;
        if (neighbors.length > 0) {
            neigh = neighbors[m_random.nextInt(neighbors.length)];
        } else {
            neigh = ref;
        }
        DataCell[] newCells = new DataCell[ref.getNumCells()];
        for (int i = 0; i < newCells.length; i++) {
            DataType t = m_inTable.getDataTableSpec().getColumnSpec(i)
                    .getType();
            if (t.isCompatible(DoubleValue.class)) {
                DataCell fCell = ref.getCell(i);
                DataCell tCell = neigh.getCell(i);
                if (fCell.isMissing() || tCell.isMissing()) {
                    newCells[i] = DataType.getMissingCell();
                } else {
                    double from = ((DoubleValue)fCell).getDoubleValue();
                    double to = ((DoubleValue)tCell).getDoubleValue();
                    double newVal = from + fraction * (to - from);
                    newCells[i] = new DoubleCell(newVal);
                }
            } else {
                newCells[i] = ref.getCell(i);
            }
        }
        String newName = ref.getKey().getString() + "dupl_"
                + m_appendCounter;
        m_appendCounter++;
        RowKey key = new RowKey(newName);
        return new DefaultRow(key, newCells);
    }

    /**
     * Closes this controller. The table can be retrieved now by invoking
     * {@link #getSmotedTable()}. Subsequent calls of
     * {@link #smote(DataCell, int, int, ExecutionMonitor)} will fail.
     */
    public void close() {
        m_container.close();
    }

    /**
     * Get final output table, including original input table and smoted table.
     * 
     * @return the new output table
     */
    public DataTable getSmotedTable() {
        DataTable attachMe = m_container.getTable();
        return new AppendedRowsTable(m_inTable, attachMe);
    }

    /* Check canceled status, clean up, i.e. close DataContainer */
    private void checkCanceled(final ExecutionMonitor exec)
            throws CanceledExecutionException {
        if (exec == null) {
            return;
        }
        try {
            exec.checkCanceled();
        } catch (CanceledExecutionException cee) {
            if (m_container.isOpen()) {
                m_container.close();
            }
            throw cee;
        }
    }

    /**
     * Creates the out spec when <i>smoting</i> the table with
     * <code>inSpec</code>. It replaces the data types of all
     * {@link DoubleValue}-compatible columns by {@link DoubleCell#TYPE}.
     * 
     * @param inSpec the table spec of the input table
     * @return the output table spec
     */
    static DataTableSpec createFinalSpec(final DataTableSpec inSpec) {
        final int colCount = inSpec.getNumColumns();
        DataColumnSpec[] colSpecs = new DataColumnSpec[colCount];
        for (int i = 0; i < colSpecs.length; i++) {
            DataColumnSpec cur = inSpec.getColumnSpec(i);
            // filter all column that are used and replace their type by
            // double type (think of an integer column which isn't an integer
            // column really once the Smoter adds new records
            if (cur.getType().isCompatible(DoubleValue.class)) {
                DataColumnSpecCreator colspeccreator 
                    = new DataColumnSpecCreator(cur);
                DataType oldType = cur.getType();
                // may be there was some strange double value type in the
                // column, use supertype of old type and DoubleCell.TYPE
                DataType newType = DataType.getCommonSuperType(oldType,
                        DoubleCell.TYPE);
                colspeccreator.setType(newType);
                // domain isn't change becaust it's a convex operation
                // (may be I should validate this statement - min and
                // max depends on the comparator being used)
                colSpecs[i] = colspeccreator.createSpec();
            } else {
                colSpecs[i] = cur;
            }
        }
        return new DataTableSpec(colSpecs);
    }

    /* Shuffles an int array. */
    private int[] shuffle(final int[] arg) {
        for (int i = arg.length; --i >= 0;) {
            int index = m_random.nextInt(i + 1);
            int swap = arg[i];
            arg[i] = arg[index];
            arg[index] = swap;
        }
        return arg;
    }

    /** Mutable int that is put into a set and is used as counter. */
    // TODO remove this and use the class in core.util
    private static class MutableInt {
        private int m_int;

        /**
         * Init the integer with specific value.
         * 
         * @param i The int to set.
         */
        MutableInt(final int i) {
            m_int = i;
        }

        /**
         * @return Returns the int.
         */
        int getInt() {
            return m_int;
        }

        /**
         * @param i The int to set.
         */
        void setInt(final int i) {
            m_int = i;
        }

        /** Increment the int by 1. */
        void increment() {
            m_int++;
        }

        @Override
        public String toString() {
            return Integer.toString(m_int);
        }
    }

    /** Helper that bind row to a distance. */
    private static class RowAndDistance {
        private final DataRow m_row;

        private final double m_dist;

        /**
         * @param row The row of interest
         * @param dist The distance of row to whatever
         */
        RowAndDistance(final DataRow row, final double dist) {
            m_row = row;
            m_dist = dist;
        }

        /**
         * @return Returns the dist.
         */
        double getDist() {
            return m_dist;
        }

        /**
         * @return Returns the row.
         */
        DataRow getRow() {
            return m_row;
        }
    }
}
