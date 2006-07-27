/* 
 * --------------------------------------------------------------------- *
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2006
 * University of Konstanz, Germany.
 * Chair for Bioinformatics and Information Mining
 * Prof. Dr. Michael R. Berthold
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any quesions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * --------------------------------------------------------------------- *
 * 
 * History
 *   09.01.2006(all): reviewed
 */
package de.unikn.knime.core.data;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import de.unikn.knime.core.data.property.ColorAttr;
import de.unikn.knime.core.data.property.SizeHandler;
import de.unikn.knime.core.node.InvalidSettingsException;
import de.unikn.knime.core.node.NodeLogger;
import de.unikn.knime.core.node.config.ConfigRO;
import de.unikn.knime.core.node.config.ConfigWO;

/**
 * DataTableSpecs are used in two ways: As meta information to specify the
 * structure of a <code>DataTable</code> (at a time when the actual table
 * still doesn't exist), and as part of a <code>DataTable</code> object to
 * store the structure of the table.
 * 
 * <p>
 * The spec specifies the characteristics i.e. column numbers, as well as column
 * types, names, and other column oriented information through a collection of
 * <code>DataColumnSpec</code>s.
 * 
 * <p>
 * Once a <code>DataTableSpec</code> is initialized, it must be final. That
 * is, if you want to add further information to a column (for instance, the
 * possible values in a column are known), you have to create a new instance of
 * a <code>DataTableSpec</code> carrying the new information. This spec can
 * then be propagated in the flow.
 * 
 * @see DataTable
 * @see DataColumnSpec
 * 
 * @author Peter Ohl, University of Konstanz
 */
public final class DataTableSpec implements Iterable<DataColumnSpec> {

    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(DataTableSpec.class);

    /** Keep an array of column specs. */
    private final DataColumnSpec[] m_columnSpecs;

    /** A name of this spec. */
    private final String m_name;

    /** The index of the column holding the SizeHandler or -1 if not set. */
    private final int m_sizeHandlerColIndex;

    /** The index of the column holding the ColorHandler or -1 if not set. */
    private final int m_colorHandlerColIndex;
   
    /** Keeps column to column index mapping for faster access. */
    private final Map<String, Integer> m_colIndexMap 
        = new HashMap<String, Integer>();

    /**
     * Creates an empty table spec with no columns defined and <i>default</i>
     * as name.
     */
    public DataTableSpec() {
        this("default");
    }

    /**
     * Creates an empty table spec with no columns defined.
     * 
     * @param name This spec's name.
     */
    public DataTableSpec(final String name) {
        this(name, new DataColumnSpec[0]);
    }

    /**
     * Creates a new <code>DataTableSpec</code>, which is built from an array
     * of <code>DataColumnSpec</code> elements.
     * 
     * @param colSpecs An array containing information about all columns.
     * @throws NullPointerException If the given column spec or one of its
     *             elements is <code>null</code>.
     * @throws IllegalArgumentException If the parameter array contains
     *             duplicates according to the
     *             {@link DataColumnSpec#getName() DataColumnSpec.getName()}
     *             method.
     */
    public DataTableSpec(final DataColumnSpec... colSpecs) {
        this("default", colSpecs);
    }

    /**
     * Creates a new <code>DataTableSpec</code>, which is built from an array
     * of <code>DataColumnSpec</code> elements.
     * 
     * @param colSpecs An array containing information about all columns.
     * @param name This spec's name, if <code>null</code> a default name is
     *            assigned.
     * @throws NullPointerException If the given column spec or one of its
     *             elements is <code>null</code>.
     * @throws IllegalArgumentException If the parameter array contains
     *             duplicates according to the
     *             {@link DataColumnSpec#getName() DataColumnSpec.getName()}
     *             method.
     */
    public DataTableSpec(final String name, final DataColumnSpec... colSpecs) {
        m_name = (name == null ? "default" : name);
        final int colCount = colSpecs.length;
        m_columnSpecs = new DataColumnSpec[colCount];
        HashSet<String> hash = new HashSet<String>();
        for (int i = 0; i < colCount; i++) {
            // disallow duplicates
            String currentName = colSpecs[i].getName();
            if (currentName == null) {
                throw new NullPointerException("Column name must not be null");
            }
            if (!hash.add(currentName)) {
                // find duplicate indices for a nice error message.
                for (int j = 0; j < i; j++) {
                    String otherName = colSpecs[j].getName();
                    if (currentName.equals(otherName)) {
                        throw new IllegalArgumentException(
                                "Duplicate column name \""
                                        + currentName.toString()
                                        + "\" at positions " + j + " and " + i
                                        + ".");
                    }
                }
            }
            m_colIndexMap.put(colSpecs[i].getName(), i);
            m_columnSpecs[i] = colSpecs[i];
        }
        m_sizeHandlerColIndex = searchSizeHandler();
        m_colorHandlerColIndex = searchColorHandler();
    }

    /**
     * Creates a new <code>DataTableSpec</code> based on a list of names and
     * types. The constructor uses the <code>DefaultDataColumnSpec</code> but
     * does not create additional information (values, ...).
     * 
     * @param names An array of names.
     * @param types An array of types.
     * @throws NullPointerException If names or types, or one of its elements is
     *             <code>null</code>.
     * @throws IllegalArgumentException If the <code>names</code> and
     *             <code>types</code> arrays don't have the same length or if
     *             the parameter array <code>names</code> contains duplicates.
     */
    public DataTableSpec(final String[] names, final DataType[] types) {
        this("default", names, types);
    }

    /**
     * Creates a new <code>DataTableSpec</code> based on a list of names and
     * types. The constructor uses the <code>DefaultDataColumnSpec</code> but
     * does not create additional information (values, ...).
     * 
     * @param name This spec's identifier, if null a default name will be used.
     * @param names An array of names.
     * @param types An array of types.
     * @throws NullPointerException If names or types, or one of its elements is
     *             <code>null</code>.
     * @throws IllegalArgumentException If the <code>names</code> and
     *             <code>types</code> arrays don't have the same length or if
     *             the parameter array <code>names</code> contains duplicates.
     */
    public DataTableSpec(final String name, final String[] names,
            final DataType[] types) {
        this(name, createColumnSpecs(names, types));
    }

    /**
     * Creates based an array of names and types and array of column specs.
     * 
     * @param names An array of column names.
     * @param types An array of column types.
     * @throws NullPointerException If one of the arrays is <code>null</code>.
     * @throws IllegalArgumentException If the arrays do not have the same
     *             length.
     * @return An array of <code>DataColumnSpec</code> elements.
     */
    public static final DataColumnSpec[] createColumnSpecs(
            final String[] names, final DataType[] types) {
        if (names.length != types.length) {
            throw new IllegalArgumentException("Parameter arrays names and "
                    + "types must have the same length (names: " + names.length
                    + ", types: " + types.length + ").");
        }
        final int colCount = names.length;
        HashSet<String> hash = new HashSet<String>();
        // encapsulate info from arguments into internal array of specs
        DataColumnSpec[] columnSpecs = new DataColumnSpec[colCount];
        for (int i = 0; i < colCount; i++) {
            if (!hash.add(names[i])) {
                // find duplicates for a nice error message
                for (int j = 0; j < i; j++) {
                    if (names[i].equals(names[j])) {
                        throw new IllegalArgumentException(
                                "Duplicate column name \""
                                        + names[j].toString()
                                        + "\" at positions " + j + " and " + i
                                        + ".");
                    }
                }
            }
            columnSpecs[i] = new DataColumnSpecCreator(names[i], types[i])
                    .createSpec();
           
        }
        return columnSpecs;
    }

    /**
     * Constructor for a new <code>DataTableSpec</code> based on two existing
     * specifications that are to be concatenated. The new spec name is combined
     * by both specs' names.
     * 
     * @param spec1 The first spec.
     * @param spec2 The second spec.
     * @throws NullPointerException If one of the given specs is
     *             <code>null</code>.
     * @throws IllegalArgumentException If the parameter specs contain duplicate
     *             names.
     */
    public DataTableSpec(final DataTableSpec spec1, final DataTableSpec spec2) {
        this(spec1.getName() + "+" + spec2.getName(), spec1, spec2);
    }

    /**
     * Constructor for a new <code>DataTableSpec</code> based on two existing
     * specifications that are to be concatenated.
     * 
     * @param name This spec's name.
     * @param spec1 The first spec.
     * @param spec2 The second spec.
     * @throws NullPointerException If one of the given specs is
     *             <code>null</code>.
     * @throws IllegalArgumentException If the parameter specs contain duplicate
     *             names.
     */
    public DataTableSpec(final String name, final DataTableSpec spec1,
            final DataTableSpec spec2) {
        this(name, appendTableSpecs(spec1, spec2));
    }

    private static DataColumnSpec[] appendTableSpecs(final DataTableSpec spec1,
            final DataTableSpec spec2) {
        final int l1 = spec1.getNumColumns();
        final int l2 = spec2.getNumColumns();
        // combine two column specs by copying them into one array of specs
        DataColumnSpec[] columnSpecs = new DataColumnSpec[l1 + l2];
        int idx = 0; // reused to iterate over spec1 then over spec2
        // copy spec1
        for (; idx < l1; idx++) {
            DataColumnSpec currentColumn = spec1.getColumnSpec(idx);
            String currentName = currentColumn.getName();
            // check for duplicates
            if (spec2.containsName(currentName)) {
                // find the index in spec2 where the duplicate is located
                for (int i = 0; i < l2; i++) {
                    if (currentName.equals(spec2.getColumnSpec(i).getName())) {
                        throw new IllegalArgumentException(
                                "Table specs to join contain the duplicate"
                                        + " column name \""
                                        + currentName.toString()
                                        + "\" at position " + idx + " and " + i
                                        + ".");
                    }
                }
                throw new InternalError("Bug in ColumnSpec.containsName()");
            }
            columnSpecs[idx] = currentColumn;
        }
        // copy spec2
        for (; idx < columnSpecs.length; idx++) {
            columnSpecs[idx] = spec2.getColumnSpec(idx - l1);
        }
        return columnSpecs;
    }

    private int searchSizeHandler() {
        int idx = -1;
        for (int i = 0; i < m_columnSpecs.length; i++) {
            if (m_columnSpecs[i].getSizeHandler() != null) {
                if (idx == -1) {
                    idx = i;
                } else {
                    LOGGER.coding("Found more SizeHandlers for columns: " + idx
                            + " and " + i + ".");
                }
            }
        }
        return idx;
    }

    private int searchColorHandler() {
        int idx = -1;
        for (int i = 0; i < m_columnSpecs.length; i++) {
            if (m_columnSpecs[i].getColorHandler() != null) {
                if (idx == -1) {
                    idx = i;
                } else {
                    LOGGER.coding("Found more ColorHandlers for columns: "
                            + idx + " and " + i + ".");
                }
            }
        }
        return idx;
    }

    /**
     * @return The name of this table spec.
     */
    public String getName() {
        return m_name;
    }

    /**
     * @return The number of columns in the table.
     */
    public int getNumColumns() {
        return m_columnSpecs.length;
    }

    /**
     * Returns column information of the column with the provided index.
     * 
     * @param index The column index within the table.
     * @return The column specification.
     * @throws ArrayIndexOutOfBoundsException If the index is out of range.
     */
    public DataColumnSpec getColumnSpec(final int index) {
        return m_columnSpecs[index];
    }

    /**
     * Return the size (in percent) that an object should have when displaying
     * information concerning this row (for instance in a scatterplot).
     * 
     * @param row the row for which the size is requested
     * @return size in [0,1] or -1 if an error occured (illegal cell, missing)
     */
    public double getRowSize(final DataRow row) {
        if (m_sizeHandlerColIndex == -1) {
            return SizeHandler.DEFAULT_SIZE;
        }
        return m_columnSpecs[m_sizeHandlerColIndex].getSizeHandler().getSize(
                row.getCell(m_sizeHandlerColIndex));
    }

    /**
     * Return the color that an object should have when displaying information
     * concerning this row (for instance in a scatterplot).
     * 
     * @param row the row for which the color is requested
     * @return color
     */
    public ColorAttr getRowColor(final DataRow row) {
        if (m_colorHandlerColIndex == -1) {
            return ColorAttr.DEFAULT;
        }
        return m_columnSpecs[m_colorHandlerColIndex].getColorHandler()
                .getColorAttr(row.getCell(m_colorHandlerColIndex));
    }

    /**
     * Returns column information of the column for the provided column name.
     * This method returns <code>null</code> if the argument is
     * <code>null</code>.
     * 
     * @param column The column to find spec for.
     * @return The column specification or null if not available.
     */
    public DataColumnSpec getColumnSpec(final String column) {
        int columnIndex = findColumnIndex(column);
        if (columnIndex == -1) {
            return null;
        }
        return m_columnSpecs[columnIndex];
    }

    /**
     * Returns <code>true</code> if <code>o</code> is a
     * <code>DataTableSpec</code> equal to this. Two specs are equal if they
     * have the same number of columns and the column specs of the same columns
     * are equal (that implies that the order of the columns has to be the
     * same). The domains of the column specs are not included into the
     * comparison.
     * 
     * @see #equalsWithDomain(DataTableSpec)
     * @param o The <code>DataTableSpec</code> to compare this with.
     * @return <code>true</code> If the two specs are identical, otherwise
     *         <code>false</code>.
     */
    @Override
    public boolean equals(final Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof DataTableSpec)) {
            return false;
        }
        DataTableSpec spec = (DataTableSpec)o;
        final int colCount = this.getNumColumns();
        // must have same number of columns to be identical
        if (spec.getNumColumns() != colCount) {
            return false;
        }
        // all column types and names must match
        for (int i = 0; i < colCount; i++) {
            DataColumnSpec thisColumn = getColumnSpec(i);
            DataColumnSpec otherColumn = spec.getColumnSpec(i);
            if (!thisColumn.equals(otherColumn)) {
                return false;
            }
        }
        // both are identical
        return true;
    }

    /**
     * Compares this to the given object. It returns <code>true</code> if
     * <code>spec</code> is identical to this. Two specs are equal if they
     * have equal <code>DataColumnSpec</code>s according to their
     * <code>equalWithDomain()</code> method. This implies that both specs
     * have to have the same number of columns and the order of the columns has
     * to be the same.
     * 
     * @param spec The <code>DataTableSpec</code> to compare this with.
     * @return <code>true</code> If the two specs are identical, otherwise
     *         <code>false</code>.
     * @see #equals(Object)
     * @see DataColumnSpec#equalsWithDomain
     */
    public boolean equalsWithDomain(final DataTableSpec spec) {
        if (spec == this) {
            return true;
        }
        if (spec == null) {
            return false;
        }
        final int colCount = this.getNumColumns();
        // must have same number of columns to be identical
        if (spec.getNumColumns() != colCount) {
            return false;
        }
        // all column types and names must match
        for (int i = 0; i < colCount; i++) {
            DataColumnSpec thisColumn = getColumnSpec(i);
            DataColumnSpec otherColumn = spec.getColumnSpec(i);
            if (!thisColumn.equalsWithDomain(otherColumn)) {
                return false;
            }
        }
        // both are identical
        return true;
    }

    /**
     * Combines the hashcode of all internal elements used to compare two
     * DataTableSpecs during equals.
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        /*
         * this hash code ignores the order of the columns. Thus, two specs
         * having the same columns (but in different order) end up with the same
         * hash code. Hopefully, this is ok anyway.
         */
        int tempHash = 0;
        for (int i = 0; i < getNumColumns(); i++) {
            int colHash = getColumnSpec(i).hashCode();
            tempHash ^= colHash;
        }
        return tempHash;
    }

    /**
     * Checks if the given column name occurs in this spec. This method returns
     * <code>false</code> if the argument is <code>null</code>.
     * 
     * @param columnName The column name to check.
     * @return <code>true</code> if this spec contains the column name.
     */
    public boolean containsName(final String columnName) {
        return findColumnIndex(columnName) >= 0;
    }

    /**
     * Finds the column with the specified name in the TableSpec and returns its
     * index, or -1 if the name doesn't exist in the table. This method returns
     * -1 if the argument is <code>null</code>. 
     * 
     * @param columnName the name to search for
     * @return the index of the column with the specified name, or -1 if not
     *         found.
     */
    public int findColumnIndex(final String columnName) {
        if (columnName == null) {
            return -1; 
        }
        if (m_colIndexMap.get(columnName) == null) {
            return -1;
        } 
        return m_colIndexMap.get(columnName);
    }

    /**
     * Checks if this spec contains a column with a type compatible to the given
     * <code>DataValue</code> class. This method returns <code>false</code>
     * if the argument is <code>null</code>.
     * 
     * @param valueClass the class of the data value interface to check for.
     * @return true if at least one column type in the spec is compatible to the
     *         provided value
     */
    public boolean containsCompatibleType(
            final Class<? extends DataValue> valueClass) {
        if (valueClass == null) {
            return false;
        }
        for (int i = 0; i < getNumColumns(); i++) {
            if (getColumnSpec(i).getType().isCompatible(valueClass)) {
                return true;
            }
        }
        return false;
    }

    /**
     * The string summary of all column spec of this table spec.
     * 
     * @return A string summary of all column specs.
     */
    @Override
    public String toString() {
        StringBuffer buffer = new StringBuffer("name=" + m_name + ",columns=[");
        for (int i = 0; i < getNumColumns(); i++) {
            DataColumnSpec spec = getColumnSpec(i);
            buffer.append(i > 0 ? ", " : "");
            buffer.append(spec.getName().toString());
        }
        buffer.append("]");
        return buffer.toString();
    }

    /**
     * Allows to iterate over all <code>DataColumnSpec</code>s in an easy
     * way.
     * 
     * @see java.lang.Iterable#iterator()
     */
    public Iterator<DataColumnSpec> iterator() {
        return (Collections.unmodifiableList(Arrays.asList(m_columnSpecs)))
                .iterator();
    }
    
    /** Key for this specs name. */
    private static final String CFG_SPEC_NAME   = "spec_name";
    /** Key for number of columns within this spec. */
    private static final String CFG_NR_COLUMNS  = "number_columns";
    /** Key for column spec sub-configs. */
    private static final String CFG_COLUMN_SPEC = "column_spec_";
    
    /**
     * Saves name and all <code>DataColumnSpec</code> objects to the given
     * <code>Config</code> object.
     * @param config Write column properties into this object.
     */
    public void save(final ConfigWO config) {
        config.addString(CFG_SPEC_NAME, m_name);
        config.addInt(CFG_NR_COLUMNS, m_columnSpecs.length);
        for (int i = 0; i < m_columnSpecs.length; i++) {
            ConfigWO column = config.addConfig(CFG_COLUMN_SPEC + i);
            m_columnSpecs[i].save(column);
        }
    }
    
    /**
     * Reads a DataColumnSpec objects from the given <code>Config</code> and 
     * returns a new <code>DataTableSpec</code> object.
     * @param config Read specs from.
     * @return A new table spec object.
     * @throws InvalidSettingsException If the name, number of columns, or
     *         a column spec could not be read,
     */
    public static DataTableSpec load(final ConfigRO config) 
            throws InvalidSettingsException {
        String name = config.getString(CFG_SPEC_NAME);
        int ncols = config.getInt(CFG_NR_COLUMNS);
        DataColumnSpec[] specs = new DataColumnSpec[ncols]; 
        for (int i = 0; i < ncols; i++) {
            ConfigRO column = config.getConfig(CFG_COLUMN_SPEC + i);
            specs[i] = DataColumnSpec.load(column);
        }
        return new DataTableSpec(name, specs);
    }

} // DataTableSpec
