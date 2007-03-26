/* 
 * --------------------------------------------------------------------- *
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2007
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
 * --------------------------------------------------------------------- *
 * 
 * History
 *   09.01.2006(all): reviewed
 *   29.10.2006(tm, cs): reviewed
 */
package org.knime.core.data;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.knime.core.data.property.ColorAttr;
import org.knime.core.data.property.ShapeFactory;
import org.knime.core.data.property.SizeHandler;
import org.knime.core.data.property.ShapeFactory.Shape;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.config.ConfigRO;
import org.knime.core.node.config.ConfigWO;

/**
 * <code>DataTableSpec</code>s specify the structure of a {@link DataTable}.
 * 
 * <p>
 * The spec specifies the characteristics i.e. column numbers, as well as column
 * types, names, and other column oriented information through a collection of
 * {@link DataColumnSpec} objects. The names of the {@link DataColumnSpec}s
 * must be unique identifiers within a <code>DataTableSpec</code>.
 * 
 * <p>
 * Once a <code>DataTableSpec</code> is initialized, it is immutable. That is,
 * if you want to add further information to a column (for instance, the
 * possible values in a column), you have to create a new instance of a
 * <code>DataTableSpec</code> carrying the new information. A spec can be
 * propagated from node to node via the ports so that succeeding nodes know
 * about the table structure even if no data table is currently available.
 * 
 * <p>
 * In addition, the table spec provides a single {@link SizeHandler},
 * {@link org.knime.core.data.property.ColorHandler} and/or
 * {@link org.knime.core.data.property.ShapeHandler} if available. The handlers
 * are associated with a column. These property handlers can be used to assign
 * size, color, and shape to a row based on the {@link DataCell}s value in the
 * corresponding column. If there is more than one column that provides a
 * handler of a certain type (color, shape, size) the first handler is used.
 * <br />
 * A <code>DataTableSpec</code> can also have a name which does not need to be
 * unique.
 * 
 * @see DataTable
 * @see DataColumnSpec
 * 
 * @author Peter Ohl, University of Konstanz
 */
public final class DataTableSpec implements Iterable<DataColumnSpec> {

    /** Key for column spec sub-configs. */
    private static final String CFG_COLUMN_SPEC = "column_spec_";

    /** Key for number of columns within this spec. */
    private static final String CFG_NR_COLUMNS = "number_columns";

    /** Key for this specs name. */
    private static final String CFG_SPEC_NAME = "spec_name";

    private static final NodeLogger LOGGER =
            NodeLogger.getLogger(DataTableSpec.class);

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
                int index = spec2.findColumnIndex(currentName);
                throw new IllegalArgumentException(
                        "Table specs to join contain the duplicate"
                                + " column name \"" + currentName.toString()
                                + "\" at position " + idx + " and " + index
                                + ".");
            }
            columnSpecs[idx] = currentColumn;
        }
        // copy spec2
        for (; idx < columnSpecs.length; idx++) {
            columnSpecs[idx] = spec2.getColumnSpec(idx - l1);
        }
        return columnSpecs;
    }

    /**
     * Static helper method to create a {@link DataColumnSpec} array from the
     * given names and types.
     * 
     * @param names an array of column names
     * @param types an array of column types
     * @throws NullPointerException if one of the arrays is <code>null</code>
     * @throws IllegalArgumentException if the arrays do not have the same
     *             length
     * @return an array of <code>DataColumnSpec</code> elements
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
            columnSpecs[i] =
                    new DataColumnSpecCreator(names[i], types[i]).createSpec();

        }
        return columnSpecs;
    }

    /**
     * Reads all {@link DataColumnSpec} objects from the given {@link ConfigRO}
     * and returns a new <code>DataTableSpec</code> object containing them.
     * 
     * @param config object to read column specs from
     * @return a new table spec object containing the just read columns
     * @throws InvalidSettingsException if the name, number of columns, or a
     *             column spec could not be read
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

    /** Keeps column name to column index mapping for faster access. */
    private final Map<String, Integer> m_colIndexMap =
            new HashMap<String, Integer>();

    /** The index of the column holding the ColorHandler or -1 if not set. */
    private final int m_colorHandlerColIndex;

    /** Keep an array of column specs. */
    private final DataColumnSpec[] m_columnSpecs;

    /**
     * The name of this spec (also applied to the table this spec belongs to).
     */
    private final String m_name;

    /** The index of the column holding the ShapeHandler or -1 if not set. */
    private final int m_shapeHandlerColIndex;

    /** The index of the column holding the SizeHandler or -1 if not set. */
    private final int m_sizeHandlerColIndex;

    /**
     * An unmodifiable List to create an iterator from. Therefore, remove
     * operations are not allowed on an iterator object.
     */
    private final List<DataColumnSpec> m_columnSpecList;

    /**
     * Creates an empty <code>DataTableSpec</code> with no columns defined and
     * <i>default</i> as name.
     */
    public DataTableSpec() {
        this("default");
    }

    /**
     * Creates a new <code>DataTableSpec</code>, which is built from an array
     * of {@link DataColumnSpec} elements.
     * 
     * @param colSpecs an array containing information about all columns
     * @throws NullPointerException if the given column spec or one of its
     *             elements is <code>null</code>
     * @throws IllegalArgumentException if the parameter array contains
     *             duplicates according to the {@link DataColumnSpec#getName()}
     *             method
     */
    public DataTableSpec(final DataColumnSpec... colSpecs) {
        this("default", colSpecs);
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
     * Creates an empty <code>DataTableSpec</code> with no columns defined.
     * 
     * @param name this spec's name
     */
    public DataTableSpec(final String name) {
        this(name, new DataColumnSpec[0]);
    }

    /**
     * Creates a new <code>DataTableSpec</code>, which is built from an array
     * of {@link DataColumnSpec} elements.
     * 
     * @param colSpecs an array containing information about all columns
     * @param name this spec's name, if <code>null</code> a default name is
     *            assigned
     * @throws NullPointerException if the given column spec or one of its
     *             elements is <code>null</code>
     * @throws IllegalArgumentException if the parameter array contains
     *             duplicates according to the {@link DataColumnSpec#getName()}
     *             method
     */
    public DataTableSpec(final String name, final DataColumnSpec... colSpecs) {
        m_name = (name == null ? "default" : name);
        final int colCount = colSpecs.length;
        m_columnSpecs = new DataColumnSpec[colCount];
        for (int i = 0; i < colCount; i++) {
            // disallow duplicates
            String currentName = colSpecs[i].getName();
            if (currentName == null) {
                throw new NullPointerException("Column name must not be null.");
            }

            Integer duplicateValue =
                    m_colIndexMap.put(colSpecs[i].getName(), i);

            // if the value is unequal null there is a duplicate value
            // throw an exception
            if (duplicateValue != null) {
                throw new IllegalArgumentException("Duplicate column name \""
                        + currentName.toString() + "\" at positions "
                        + duplicateValue + " and " + i + ".");
            }
            m_columnSpecs[i] = colSpecs[i];
        }
        m_sizeHandlerColIndex = searchSizeHandler();
        m_colorHandlerColIndex = searchColorHandler();
        m_shapeHandlerColIndex = searchShapeHandler();

        // create an unmodifiable list to create iterators from
        m_columnSpecList =
                Collections.unmodifiableList(Arrays.asList(m_columnSpecs));
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

    /**
     * Creates a new <code>DataTableSpec</code> based on a list of names and
     * types. The constructor uses the {@link DataColumnSpec} but does not
     * create additional information (values, ...).
     * 
     * @param name this spec's identifier, if <code>null</code> a default name
     *            will be used
     * @param names an array of names
     * @param types an array of types
     * @throws NullPointerException if names or types, or one of its elements is
     *             <code>null</code>
     * @throws IllegalArgumentException if the <code>names</code> and
     *             <code>types</code> arrays don't have the same length or if
     *             the parameter array <code>names</code> contains duplicates
     */
    public DataTableSpec(final String name, final String[] names,
            final DataType[] types) {
        this(name, createColumnSpecs(names, types));
    }

    /**
     * Creates a new <code>DataTableSpec</code> based on a list of names and
     * types. The constructor uses the {@link DataColumnSpec} but does not
     * create additional information (values, ...).
     * 
     * @param names an array of names
     * @param types an array of types
     * @throws NullPointerException if names or types, or one of its elements is
     *             <code>null</code>
     * @throws IllegalArgumentException if the <code>names</code> and
     *             <code>types</code> arrays don't have the same length or if
     *             the parameter array <code>names</code> contains duplicates
     */
    public DataTableSpec(final String[] names, final DataType[] types) {
        this("default", names, types);
    }

    /**
     * Checks if this spec contains a column with a type compatible to the given
     * {@link DataValue} class. This method returns <code>false</code> if the
     * argument is <code>null</code>.
     * 
     * @param valueClass the class of the data value interface to check for
     * @return <code>true</code> if at least one column type in the spec is
     *         compatible to the provided value
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
     * Checks if the given column name occurs in this spec. This method returns
     * <code>false</code> if the argument is <code>null</code>.
     * 
     * @param columnName the column name to check
     * @return <code>true</code> if this spec contains the column name
     */
    public boolean containsName(final String columnName) {
        return findColumnIndex(columnName) >= 0;
    }

    /**
     * Returns <code>true</code> if <code>o</code> is a
     * <code>DataTableSpec</code> equal to this, that is <code>o == this</code>.
     * @param o the other <code>Object</code> to compare this with
     * @return <code>true</code> if the two objects have the same reference,
     *         otherwise <code>false</code>
     * @see Object#equals(Object)
     * @see #equalStructure(DataTableSpec)
     */
    @Override
    public boolean equals(final Object o) {
        return (this == o);
    }

    /**
     * Returns <code>true</code> if <code>spec</code> has the same column
     * names and types. Two specs are equal if they have the same number of 
     * columns and the column specs of the same columns are equal (that implies 
     * that the order of the columns has to be the same). The domains, 
     * properties, and handlers of the column specs are not included into the 
     * comparison.
     * 
     * @param spec the <code>DataTableSpec</code> to compare this with
     * @return <code>true</code> if the two specs have the same column names, 
     *         and types, otherwise <code>false</code>
     */
    public boolean equalStructure(final DataTableSpec spec) {
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
            if (!thisColumn.equalStructure(otherColumn)) {
                return false;
            }
        }
        // both are identical
        return true;        
    }     
       

    /**
     * Checks if both {@link DataTableSpec}s are equal; in contrast to
     * {@link #equals(Object)} the domain and properties of the columns must 
     * be equal as well. Two specs are equal if their {@link DataColumnSpec}s 
     * are equal according to the 
     * {@link DataColumnSpec#equalsWithDomain(DataColumnSpec)} method.
     * This implies that both specs have to have the same number of columns and
     * the order of the columns has to be the same.
     * 
     * @param spec the <code>DataTableSpec</code> to compare this with
     * @return <code>true</code> if the two specs have equal structure, domain,
     *         and properties, otherwise <code>false</code>
     *         
     * @see #equals(Object)
     * @see #equalStructure(DataTableSpec)
     * @deprecated use {@link #equalStructure(DataTableSpec)} and check if 
     *             domain and properties matches by yourself
     */
    @Deprecated
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
     * Returns column information of the column with the provided index.
     * 
     * @param index the column index within the table
     * @return the column specification
     * @throws ArrayIndexOutOfBoundsException if the index is out of range
     */
    public DataColumnSpec getColumnSpec(final int index) {
        return m_columnSpecs[index];
    }

    /**
     * Returns the {@link DataColumnSpec} of the column with the provided name.
     * This method returns <code>null</code> if the argument is
     * <code>null</code>.
     * 
     * @param column the column name to find the spec for
     * @return the column specification or <code>null</code> if not available
     */
    public DataColumnSpec getColumnSpec(final String column) {
        int columnIndex = findColumnIndex(column);
        if (columnIndex == -1) {
            return null;
        }
        return m_columnSpecs[columnIndex];
    }

    /**
     * Returns the name of this <code>DataTableSpec</code>.
     * 
     * @return the name of this table spec
     */
    public String getName() {
        return m_name;
    }

    /**
     * Returns the number of columns.
     * 
     * @return the number of columns
     */
    public int getNumColumns() {
        return m_columnSpecs.length;
    }

    /**
     * Returns the color that an object should have when displaying information
     * concerning this row (for instance in a scatterplot). The color is
     * determined by the {@link org.knime.core.data.property.ColorHandler} of
     * this spec, which is associated with exactly one column. The color
     * therefore depends on the value of the corresponding cell of the given
     * row.
     * 
     * @param row the row for which the color is requested
     * @return a color attr object holding the colors associate to that row
     */
    public ColorAttr getRowColor(final DataRow row) {
        if (m_colorHandlerColIndex == -1) {
            return ColorAttr.DEFAULT;
        }
        return m_columnSpecs[m_colorHandlerColIndex].getColorHandler()
                .getColorAttr(row.getCell(m_colorHandlerColIndex));
    }

    /**
     * Return the shape that an object should have when displaying information
     * concerning this row (for instance in a scatterplot). The shape is
     * determined by the {@link org.knime.core.data.property.ShapeHandler} of
     * this spec, which is associated with exactly one column. The shape
     * therefore depends on the value of the corresponding cell of the given
     * row.
     * 
     * @param row the row for which the shape is requested
     * @return the shape object associated with this row
     */
    public Shape getRowShape(final DataRow row) {
        if (m_shapeHandlerColIndex == -1) {
            return ShapeFactory.getShape(ShapeFactory.DEFAULT);
        }
        return m_columnSpecs[m_shapeHandlerColIndex].getShapeHandler()
                .getShape(row.getCell(m_shapeHandlerColIndex));
    }

    /**
     * Return the size (in percent) that an object should have when displaying
     * information concerning this row (for instance in a scatterplot). The size
     * is determined by the {@link SizeHandler} of this spec, which is
     * associated with exactly one column. The size therefore depends on the
     * value of the corresponding cell of the given row.
     * 
     * @param row the row for which the size is requested
     * @return size in [0,1] or -1 if an error occurred (illegal cell, missing)
     */
    public double getRowSize(final DataRow row) {
        if (m_sizeHandlerColIndex == -1) {
            return SizeHandler.DEFAULT_SIZE;
        }
        return m_columnSpecs[m_sizeHandlerColIndex].getSizeHandler().getSize(
                row.getCell(m_sizeHandlerColIndex));
    }

    /**
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
     * Returns an iterator for the contained {@link DataColumnSpec} elements.
     * The iterator does not support the remove method (table specs are
     * immutable).
     * 
     * @see java.lang.Iterable#iterator()
     */
    public Iterator<DataColumnSpec> iterator() {
        return m_columnSpecList.iterator();
    }

    /**
     * Saves the table spec name and all {@link DataColumnSpec}s to the given
     * {@link ConfigWO} object.
     * 
     * @param config the config object to save this table specs to
     */
    public void save(final ConfigWO config) {
        config.addString(CFG_SPEC_NAME, m_name);
        config.addInt(CFG_NR_COLUMNS, m_columnSpecs.length);
        for (int i = 0; i < m_columnSpecs.length; i++) {
            ConfigWO column = config.addConfig(CFG_COLUMN_SPEC + i);
            m_columnSpecs[i].save(column);
        }
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

    private int searchShapeHandler() {
        int idx = -1;
        for (int i = 0; i < m_columnSpecs.length; i++) {
            if (m_columnSpecs[i].getShapeHandler() != null) {
                if (idx == -1) {
                    idx = i;
                } else {
                    LOGGER.coding("Found more ShapeHandlers for columns: "
                            + idx + " and " + i + ".");
                }
            }
        }
        return idx;
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
    
    /**
     * This method merges two or more <code>DataTableSpec</code>s. 
     * If the <code>DataTableSpec</code>s have equal structure
     * (which is required if you call this method)
     * their domains, Color-, Shape and Size-Handlers are merged. That means:
     * <ul>
     * <li>If any of the columns has a color/shape/size handler attached, it
     * must be the same handler for the respective column in all tables.</li>
     * <li>If columns have properties attached (defined through 
     * {@link DataColumnSpec#getProperties()}), they will be merged, the 
     * merged <code>DataColumnSpec</code> will contain the intersection of all 
     * properties (key and value must be the same).</li>
     * <li>The {@link DataColumnSpec#getDomain() domains} will be updated, the 
     * possible values list will contain the union of all possible values and
     * the min/max values will be set appropriately. 
     * </ul>
     * 
     * <p>This factory method is used when two or more tables shall be 
     * (row-wise) concatenated, for instance in
     * {@link org.knime.core.node.ExecutionContext#createConcatenateTable(
     * org.knime.core.node.ExecutionMonitor, 
     * org.knime.core.node.BufferedDataTable[]) 
     * ExecutionContext#createConcatenateTable}.
     * @param specs The DataTableSpecs to merge. 
     * @return a DataTableSpec with merged domain information
     * from both input DataTableSpecs.
     * @throws IllegalArgumentException if the structures of the DataTableSpecs
     * do not match, the array is empty, or the array is <code>null</code>.
     */
    public static DataTableSpec mergeDataTableSpecs(
            final DataTableSpec... specs) {
        if (specs == null || Arrays.asList(specs).contains(null)) {
            throw new IllegalArgumentException("Argument array must not " 
                    + "be null, nor contain null values.");
        }
        if (specs.length == 0) {
            throw new IllegalArgumentException(
                    "Argument array must not be empty.");
        }
        // initialize with first DataTableSpec
        DataTableSpec firstSpec = specs[0];
        // make sure that all DataTableSpecs have equal structure
        for (int i = 1; i < specs.length; i++) {
            if (!firstSpec.equalStructure(specs[i])) {
                throw new IllegalArgumentException(
                        "Cannot merge DataTableSpecs,"
                                + " they don't have equal structure");
            }
        }
        DataColumnSpecCreator[] mergedColSpecCreators =
                new DataColumnSpecCreator[firstSpec.getNumColumns()];
        for (int i = 0; i < mergedColSpecCreators.length; i++) {
            mergedColSpecCreators[i] =
                    new DataColumnSpecCreator(firstSpec.getColumnSpec(i));
        }

        // merge with ColumnSpecs from other DataTableSpecs
        for (int i = 1; i < specs.length; i++) {
            DataTableSpec spec = specs[i];
            for (int c = 0; c < spec.getNumColumns(); c++) {
                mergedColSpecCreators[c].merge(spec.getColumnSpec(c));
            }
        }
        DataColumnSpec[] mergedcolspecs =
                new DataColumnSpec[mergedColSpecCreators.length];
        for (int i = 0; i < mergedcolspecs.length; i++) {
            mergedcolspecs[i] = mergedColSpecCreators[i].createSpec();
        }
        return new DataTableSpec(mergedcolspecs);
    }

    /**
     * The string summary of all column specs of this table spec.
     * 
     * @return A string summary of all column specs.
     */
    @Override
    public String toString() {
        StringBuilder buffer =
                new StringBuilder("name=" + m_name + ",columns=[");
        for (int i = 0; i < getNumColumns(); i++) {
            DataColumnSpec spec = getColumnSpec(i);
            buffer.append(i > 0 ? ", " : "");
            buffer.append(spec.getName().toString());
        }
        buffer.append("]");
        return buffer.toString();
    }

} // DataTableSpec
