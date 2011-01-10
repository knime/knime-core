/* 
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
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
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
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
 * -------------------------------------------------------------------
 *
 */
package org.knime.base.data.join;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.knime.base.data.filter.column.FilterColumnTable;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowIterator;

/**
 * Creates new table by appending all columns from <i>right</i> table to the
 * <i>left</i> table. This, of course, only if both table agree on the row
 * keys.
 * 
 * <p>
 * What is worth to notice about the order: Ideally, the order in both tables is
 * the same. If that is not the case, the order of the <i>left</i> table is the
 * one that counts, followed by the ones that are in the <i>right</i> table but
 * not in the <i>left</i> one.
 * 
 * <p>
 * If any table contains row keys that the other does not, the columns provided
 * by the respectively other table are filled with missing cells. If the column
 * type is one of {@link org.knime.core.data.def.DoubleCell},
 * {@link org.knime.core.data.def.IntCell} or
 * {@link org.knime.core.data.def.StringCell}, the singleton instances of
 * missing cells (No, it is not missing, but declared as having a missing
 * value). Otherwise, this implementation will throw an exception during the
 * iteration, saying that it cannot instantiate a missing cell. (If it could do
 * so, it would still violate the column type given in table spec.)
 * 
 * @author Bernd Wiswedel, University of Konstanz
 */
public class JoinedTable implements DataTable {
    /** Method on how to treat duplicate column names: fail execution. */
    public static final String METHOD_FAIL = "exception";

    /** Method on how to treat duplicate column names: filter. */
    public static final String METHOD_FILTER = "filter_out";

    /** Method on how to treat duplicate column names: append suffix. */
    public static final String METHOD_APPEND_SUFFIX = "append_suffix";

    private final DataTableSpec m_spec;

    private final DataTable m_left;

    private final DataTable m_right;

    private final boolean m_inMemory;

    /**
     * Has an error been reported that there is a problem with the sorting and
     * the iterator may have quadratic complexity.
     */
    private boolean m_printedErrorOnSorting = false;

    /**
     * Has an error been reported that keys in the right table don't occur in
     * the left table or vice versa.
     */
    private boolean m_printedErrorOnMissing = false;

    /**
     * Creates new table with the left part defined by <code>left</code> and
     * the appended right part given by <code>right</code>. If duplicate
     * column names occur, an exception is thrown
     * 
     * @param left the left part of the this table
     * @param right and the corresponding right part
     * @throws IllegalArgumentException if the tables contain the same column
     *             identifier
     * @throws NullPointerException if either table is <code>null</code>
     * @throws IllegalArgumentException if duplicate column names exist
     */
    public JoinedTable(final DataTable left, final DataTable right) {
        this(left, right, METHOD_FAIL, null, false);
    }

    /**
     * Creates a new table with the left part defined by <code>left</code> and
     * the appended right part given by <code>right</code>. Duplicate column
     * names are treated as given by the <code>duplicateMethod</code>
     * argument, i.e.
     * <ul>
     * <li> {@link #METHOD_FAIL} throw exception</li>
     * <li> {@link #METHOD_FILTER} ignore duplicates in the right column</li>
     * <li> {@link #METHOD_APPEND_SUFFIX} append a suffix given by the
     * <code>suffix</code> argument to occuring duplicates</li>
     * </ul>
     * 
     * @param left the left part of the this table
     * @param right and the corresponding right part
     * @param duplicateMethod the method on how to treat duplicates
     * @param suffix the suffix that is used when the method is
     *            {@link #METHOD_APPEND_SUFFIX}. In case of another any other
     *            method this argument is ignored.
     * @param inMemory <code>true</code> if the tables should be joined in
     *            memory (faster but may need much memory)
     * @throws IllegalArgumentException in case of duplicate column names and no
     *             special treatment is requested
     * @throws NullPointerException if either table is <code>null</code>
     */
    public JoinedTable(final DataTable left, final DataTable right,
            final String duplicateMethod, final String suffix,
            final boolean inMemory) {
        // that will horribly bomb if the table have columns in common...
        DataTableSpec lSpec = left.getDataTableSpec();
        DataTableSpec rSpec = right.getDataTableSpec();
        m_spec = createSpec(lSpec, rSpec, duplicateMethod, suffix);
        DataTable newRight = right;
        if (METHOD_FILTER.equals(duplicateMethod)) {
            String[] survivors = getSurvivers(lSpec, rSpec);
            if (survivors.length < rSpec.getNumColumns()) {
                newRight = new FilterColumnTable(right, survivors);
            }
        }
        m_left = left;
        m_right = newRight;
        m_inMemory = inMemory;
    }

    /**
     * {@inheritDoc}
     */
    public DataTableSpec getDataTableSpec() {
        return m_spec;
    }

    /**
     * {@inheritDoc}
     */
    public RowIterator iterator() {
        if (m_inMemory) {
            return new InMemoryIterator(this);
        } else {
            return new JoinedTableRowIterator(this);
        }
    }

    /**
     * Get reference to underlying left table.
     * 
     * @return the reference
     */
    protected DataTable getLeftTable() {
        return m_left;
    }

    /**
     * Get reference to underlying right table.
     * 
     * @return the reference
     */
    protected DataTable getRightTable() {
        return m_right;
    }

    /**
     * Creates a new DataTableSpec as an result of merging a <code>left</code>
     * and a <code>right</code> table. The method fails with an exception when
     * there are duplicate column names
     * 
     * @param left the left part of the this table
     * @param right the corresponding right part
     * @return the spec as result of merging both table specs
     * @throws IllegalArgumentException in case of duplicate column names and no
     *             special treatment is requested
     * @throws NullPointerException if either table is <code>null</code>
     */
    public static final DataTableSpec createSpec(final DataTableSpec left,
            final DataTableSpec right) {
        return createSpec(left, right, METHOD_FAIL, /* ignore */null);
    }

    /**
     * Creates a new DataTableSpec as an result of merging a <code>left</code>
     * and a <code>right</code> table. Duplicate of column names are treated
     * as given by the <code>duplicateMethod</code> argument, i.e.
     * <ul>
     * <li> {@link #METHOD_FAIL} throw exception</li>
     * <li> {@link #METHOD_FILTER} ignore duplicates in the right column</li>
     * <li> {@link #METHOD_APPEND_SUFFIX} append a suffix given by the
     * <code>suffix</code> argument to occuring duplicates</li>
     * </ul>
     * 
     * @param left the left part of the this table
     * @param right and the corresponding right part
     * @param duplicateMethod the method on how to treat duplicates
     * @param suffix the suffix that is used when the method is
     *            {@link #METHOD_APPEND_SUFFIX}. In case of another any other
     *            method this argument is ignored.
     * @return the spec as result of merging both table specs
     * @throws IllegalArgumentException in case of duplicate column names and no
     *             special treatment is requested
     * @throws NullPointerException if either table is <code>null</code>
     */
    public static final DataTableSpec createSpec(final DataTableSpec left,
            final DataTableSpec right, final String duplicateMethod,
            final String suffix) {
        DataColumnSpec[] leftCols;
        DataColumnSpec[] rightCols;
        if (METHOD_FAIL.equals(duplicateMethod)) {
            leftCols = new DataColumnSpec[left.getNumColumns()];
            rightCols = new DataColumnSpec[right.getNumColumns()];
            Set<String> hash = new HashSet<String>();
            for (int i = 0; i < left.getNumColumns(); i++) {
                leftCols[i] = left.getColumnSpec(i);
                hash.add(leftCols[i].getName());
            }
            for (int i = 0; i < right.getNumColumns(); i++) {
                rightCols[i] = right.getColumnSpec(i);
                if (hash.contains(rightCols[i].getName())) {
                    throw new IllegalArgumentException("Duplicate column: " 
                            + rightCols[i].getName());
                }
            }
        } else if (METHOD_FILTER.equals(duplicateMethod)) {
            String[] survivers = getSurvivers(left, right);
            DataTableSpec newRight = FilterColumnTable.createFilterTableSpec(
                    right, survivers);
            leftCols = new DataColumnSpec[left.getNumColumns()];
            rightCols = new DataColumnSpec[newRight.getNumColumns()];
            for (int i = 0; i < left.getNumColumns(); i++) {
                leftCols[i] = left.getColumnSpec(i);
            }
            for (int i = 0; i < newRight.getNumColumns(); i++) {
                rightCols[i] = newRight.getColumnSpec(i);
            }
        } else if (METHOD_APPEND_SUFFIX.equals(duplicateMethod)) {
            final int rightColCount = right.getNumColumns();
            HashSet<String> newInvented = new HashSet<String>();
            DataColumnSpec[] newCols = new DataColumnSpec[rightColCount];
            for (int i = 0; i < rightColCount; i++) {
                DataColumnSpec col = right.getColumnSpec(i);
                String name = col.getName();
                boolean invented = false;
                while (left.containsName(name) || newInvented.contains(name)) {
                    invented = true;
                    do {
                        name = name.toString() + suffix;
                        // we need also the keep track that we don't "invent" a
                        // name that is used in the right table already
                    } while (right.containsName(name));
                }
                if (invented) {
                    newInvented.add(name);
                    DataColumnSpecCreator creator = new DataColumnSpecCreator(
                            col);
                    creator.setName(name);
                    newCols[i] = creator.createSpec();
                } else {
                    newCols[i] = col;
                }
            }
            DataTableSpec newRight = new DataTableSpec(newCols);
            leftCols = new DataColumnSpec[left.getNumColumns()];
            rightCols = new DataColumnSpec[newRight.getNumColumns()];
            for (int i = 0; i < left.getNumColumns(); i++) {
                leftCols[i] = left.getColumnSpec(i);
            }
            for (int i = 0; i < right.getNumColumns(); i++) {
                rightCols[i] = newRight.getColumnSpec(i);
            }
        } else {
            throw new IllegalArgumentException(
                    "Unknown method: " + duplicateMethod);
        }
        boolean isLeftContainColorHandler = false;
        boolean isLeftContainSizeHandler = false;
        boolean isLeftContainShapeHandler = false;
        for (DataColumnSpec s : leftCols) {
            isLeftContainColorHandler |= s.getColorHandler() != null;
            isLeftContainSizeHandler |= s.getSizeHandler() != null;
            isLeftContainShapeHandler |= s.getShapeHandler() != null;
        }
        for (int i = 0; i < rightCols.length; i++) {
            DataColumnSpec s = rightCols[i];
            boolean removeColorHandler = false;
            if (s.getColorHandler() != null && isLeftContainColorHandler) {
                removeColorHandler = true;
            }
            boolean removeSizeHandler = false;
            if (s.getSizeHandler() != null && isLeftContainSizeHandler) {
                removeSizeHandler = true;
            }
            boolean removeShapeHandler = false;
            if (s.getShapeHandler() != null && isLeftContainShapeHandler) {
                removeShapeHandler = true;
            }
            if (removeColorHandler || removeSizeHandler || removeShapeHandler) {
                DataColumnSpecCreator c = new DataColumnSpecCreator(s);
                if (removeColorHandler) {
                    c.setColorHandler(null);
                }
                if (removeSizeHandler) {
                    c.setSizeHandler(null);
                }
                if (removeShapeHandler) {
                    c.setShapeHandler(null);
                }
                rightCols[i] = c.createSpec();
            }
        }
        DataColumnSpec[] sp = 
            new DataColumnSpec[leftCols.length + rightCols.length];
        System.arraycopy(leftCols, 0, sp, 0, leftCols.length);
        System.arraycopy(rightCols, 0, sp, leftCols.length, rightCols.length);
        return new DataTableSpec(sp);
    }

    /* Determines set of unique names, duplicates in right are skipped. */
    private static String[] getSurvivers(final DataTableSpec left,
            final DataTableSpec right) {
        // hash column names from right table
        HashSet<String> hash = new HashSet<String>();
        for (int i = 0; i < left.getNumColumns(); i++) {
            String name = left.getColumnSpec(i).getName();
            hash.add(name);
        }
        // determine the "survivers"
        ArrayList<String> survivers = new ArrayList<String>();
        for (int i = 0; i < right.getNumColumns(); i++) {
            String name = right.getColumnSpec(i).getName();
            if (!hash.contains(name)) {
                survivers.add(name);
            }
        }
        return survivers.toArray(new String[0]);
    }
    
    /**
     * Requested by iterated.
     * 
     * @return the printedErrorOnMissing
     */
    boolean isPrintedErrorOnMissing() {
        return m_printedErrorOnMissing;
    }

    /**
     * @param printedErrorOnMissing the printedErrorOnMissing to set
     */
    void setPrintedErrorOnMissing(final boolean printedErrorOnMissing) {
        m_printedErrorOnMissing = printedErrorOnMissing;
    }

    /**
     * Requested by iterated.
     * 
     * @return the printedErrorOnSorting
     */
    boolean isPrintedErrorOnSorting() {
        return m_printedErrorOnSorting;
    }

    /**
     * @param printedErrorOnSorting the printedErrorOnSorting to set
     */
    void setPrintedErrorOnSorting(final boolean printedErrorOnSorting) {
        m_printedErrorOnSorting = printedErrorOnSorting;
    }

    /**
     * Fills an array with missing values according to the spec.
     * 
     * @param spec the spec of the table
     * @return an array of missing cells with the appropriate length and types
     * @throws IllegalStateException if no missing cell can be instantiated
     */
    public static DataCell[] createMissingCells(final DataTableSpec spec) {
        DataCell[] allMissingCells = new DataCell[spec.getNumColumns()];
        for (int i = 0; i < allMissingCells.length; i++) {
            allMissingCells[i] = DataType.getMissingCell();
        }
        return allMissingCells;
    }
}
