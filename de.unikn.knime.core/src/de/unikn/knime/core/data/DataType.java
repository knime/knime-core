/* -------------------------------------------------------------------
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
 * History
 *   07.07.2005 (mb): created
 */
package de.unikn.knime.core.data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import de.unikn.knime.core.data.renderer.DataCellRendererFamily;
import de.unikn.knime.core.data.renderer.DefaultDataCellRendererFamily;
import de.unikn.knime.core.data.renderer.SetOfRendererFamilies;

/**
 * The base class for each type associated with a certain type of data cell. It
 * is in charge of keeping the list of compatible (i.e. castable) data cell
 * types, the list of renderers for this type, and a comparator for data cells
 * of this type. Also, it will be used to create a common super type for two
 * given cell types in case they are not compatible to each other.
 * 
 * <p>In order to allow fast disc buffering of <code>DataCell</code> objects 
 * (other than the slow serializable technique) consider to implement the 
 * <code>DataCellSerializer</code> interface in your subclass of DataType. This
 * will allow the framework to fast-write DataCell objects using the DataType
 * as factory.
 * 
 * @see de.unikn.knime.core.data.DataCellSerializer
 * @author Michael Berthold, University of Konstanz
 */
public class DataType implements Serializable {

    /**
     * Singleton of a generic data type. This type is compatible to nothing! It
     * will always(!) return the fallback comparator (which uses the toString
     * result to compare datacells).
     */
    public static final DataType DATA_TYPE = new DataType();

    /*
     * Icon which is used as "fallback" representative when no specialized icon
     * is found in derivates of this class
     */
    private static final Icon ICON;

    /*
     * try loading this icon, if it fails we use null in the probably silly
     * assumtion everyone can deal with that
     */
    static {
        ImageIcon icon;
        try {
            ClassLoader loader = DataCell.class.getClassLoader();
            String path = DataCell.class.getPackage().getName().replace('.',
                    '/');
            icon = new ImageIcon(loader.getResource(path
                    + "/icon/defaulticon.png"));
        } catch (Exception e) {
            icon = null;
        }
        ICON = icon;
    }

    /*
     * The missing cell for this type. All derived types should implement it in
     * a similar way to return this singleton in the getMissingCell) method.
     */
    private DataCell m_missing = new DataCell() {
        @Override
        public DataType getType() {
            return DataType.this;
        }

        @Override
        public boolean isMissing() {
            return true;
        }

        @Override
        public String toString() {
            return "?";
        }

        @Override
        public boolean equalsDataCell(final DataCell dc) {
            // guaranteed not to be called on and with a missing cell.....
            return false;
        }

        @Override
        public int hashCode() {
            return toString().hashCode();
        }
    };

    /*
     * the String representation comparator we fall back to if no other is
     * available.
     */
    private static final DataCellComparator FALLBACK_COMP = 
        new DataCellComparator() {
        @Override
        protected int compareDataCells(final DataCell c1, final DataCell c2) {
            return c1.toString().compareTo(c2.toString());
        }
    };

    // the list of compatible DataType objects.
    private final ArrayList<DataType> m_typeList;

    /**
     * Creates a new type with an empty list of compatible types.
     */
    protected DataType() {
        m_typeList = new ArrayList<DataType>();
    }

    /**
     * Creates a new type with given list of compatible types. Only used to
     * create super types with no native type.
     * 
     * @param typeList The list of compatible types.
     * @throws ClassCastException If one of objects in the list is not of type
     *             <code>DataType</code>.
     * 
     * @see #addCompatibleType(DataType)
     */
    private DataType(final List<DataType> typeList) {
        m_typeList = new ArrayList<DataType>();
        for (DataType t : typeList) {
            addCompatibleType(t);
        }
    }

    /*
     * checks if this type has a native value. If not it's a constructed super
     * type
     */
    private boolean isNativeType() {
        return !isNonNativeType();
    }

    /* checks if this type is a constructed super type */
    private boolean isNonNativeType() {
        return this.getClass() == DataType.class;
    }

    /**
     * Adds a compatible type to this type. Use this in the constructor of any
     * derived DataType to add the types associated with the value interfaces
     * the data cell of this type implements.
     * 
     * @param type The <code>DataType</code> to add.
     * @throws NullPointerException If the given type is <code>null</code>.
     * @throws IllegalArgumentException if this native type is the same as the
     *             given native type.
     */
    protected final void addCompatibleType(final DataType type) {

        // do not add a non-native type to your list!
        if (type.isNonNativeType()) {
            throw new IllegalArgumentException("Don't add a non-native type"
                    + " to the list of compatible types");
        }
        // do not add your own type to the list of compatible types
        if (type.getNativeValue().equals(this.getNativeValue())) {
            throw new IllegalArgumentException("Don't add own native type to "
                    + "the list of compatible types");
        }
        // don't create cycles in the compatibility graph
        if (type.isCompatible(this.getNativeValue())) {
            throw new IllegalArgumentException("Can't make '" + type
                    + "' compatible to '" + this + "'. It would create a cycle"
                    + " in the compatibility graph.");
        }
        m_typeList.add(type);
    }

    /**
     * Derived classes must override this and return their native value
     * interface, i.e. the value interface class for which this type has been
     * introduced. Please overwrite and never return <code>null</code>!
     * 
     * @return native value interface class
     */
    protected Class<? extends DataValue> getNativeValue() {
        assert false; // override this in derived classes!
        return null;
    }

    /**
     * Derived classes should override this and provide a comparator that
     * compares datacells of their native type. If null is returned a comparator
     * of any compatible type will be used instead.
     * 
     * @return a comparator to compare data cells of the native type, or null if
     *         a comparator of a compatible type is as good.
     */
    protected DataCellComparator getNativeComparator() {
        return null;
    }

    /**
     * Get an icon representing this type. This is used in table headers and
     * lists, for instance.
     * 
     * <p>
     * Implementors who derive this class are invited to override this method
     * and return a more specific icon (of size 16x16).
     * 
     * @return An icon for this type.
     */
    public Icon getIcon() {
        return ICON;
    }

    /**
     * Get all renderers this type natively supports. Derived classes should
     * override this method to provide their own renderer family for the native
     * value class of this type.
     * 
     * <p>
     * Views that rely on renderer implementations will get a list of all
     * available renderer by invoking <code>getRenderer(DataColumnSpec)</code>
     * which makes sure that all renderer implementations of compatible types
     * and this native type are returned.
     * 
     * @param spec The column spec to the column for which the renderer will be
     *            used. Most of the renderer implementations won't need column
     *            domain information but some do. For instance a class that
     *            renders the double value in the column according to the
     *            min/max values in the column domain.
     * @return <code>null</code>
     */
    protected DataCellRendererFamily getNativeRenderer(
            final DataColumnSpec spec) {
        // avoid compiler warnings as spec is never read locally
        assert spec == spec;
        return null;
    }

    /**
     * Returns the set of all renderers that are available for this DataType.
     * The returned DataCellRendererFamily will contain all renderers that are
     * either natively supported or available through the compatible types. If
     * no renderer was declared by the compatible types nor the implementation
     * of this type ( <code>getNativeRenderer(DataColumnSpec)</code> has not
     * been overridden), this method will make sure that at least a default
     * renderer (using the <code>DataCell</code>'s<code>toString()</code>
     * method) is returned.
     * 
     * @param spec The column spec to the column for which the renderer will be
     *            used. Most of the renderer implementations won't need column
     *            domain information but some do. For instance a class that
     *            renders the double value in the column according to the
     *            min/max values in the column domain.
     * @return All renderers that are available for this DataType.
     */
    public final DataCellRendererFamily getRenderer(final DataColumnSpec spec) {
        ArrayList<DataCellRendererFamily> list = 
            new ArrayList<DataCellRendererFamily>();
        fillNativeRenderer(list, this, spec);
        if (list.isEmpty()) {
            list.add(new DefaultDataCellRendererFamily());
        }
        return new SetOfRendererFamilies(list);
    }

    /**
     * Helper function that iterates in a depth first search manner over all
     * compatible types and add their respective native renderer to the argument
     * list.
     * 
     * @param list The list containing all renderer
     * @param type The type to start from.
     */
    private static void fillNativeRenderer(
            final ArrayList<DataCellRendererFamily> list, final DataType type,
            final DataColumnSpec spec) {
        DataCellRendererFamily f = type.getNativeRenderer(spec);
        if (f != null) {
            list.add(f);
        }
        for (DataType t : type.m_typeList) {
            fillNativeRenderer(list, t, spec);
        }
    }

    /**
     * Checks if the given <code>DataValue.class</code> is compatible to this
     * type (or any of its compatible types). If it returns true the datacells
     * of this type can be typecasted to the <code>valueClass</code>. This
     * method returns <code>false</code> if the argument is <code>null</code>.
     * 
     * @param valueClass Class to check compatibilty for.
     * @return <code>true</code> if compatible to at least one type.
     */
    public final boolean isCompatible(
            final Class<? extends DataValue> valueClass) {
        boolean result = false;
        if (valueClass == null) {
            return false;
        }
        if (isNativeType()) {
            // check compatibility against this if this is a native type
            result = valueClass.isAssignableFrom(getNativeValue());
        }

        for (Iterator it = m_typeList.iterator(); !result && it.hasNext();) {
            DataType next = (DataType)it.next();
            result = next.isCompatible(valueClass);
        }
        return result;
    }

    /**
     * Returns true if this type is a supertype of the passed type. I.e. the
     * argument type is compatible to all types of this type (and may be more).
     * In other words, this object is more general than the argument or this
     * object supports less compatible types than the argument.
     * 
     * <p>
     * This is mostly used to test if a given data cell can be added to a given
     * data column. The data cell's type must be compatible to (at least) all
     * value interfaces the column's type is compatible to. If the column's type
     * is a supertype of the cells type, it's safe to add the cell to the
     * column.
     * 
     * @param type the type to test the supertypeness of this for.
     * @return true if this type is a (one of many possible) supertype of the
     *         argument type.
     */
    public final boolean isOneSuperTypeOf(final DataType type) {
        if (type == this) {
            // see if we get off easy
            return true;
        }

        boolean result = true;

        if (this.isNativeType()) {
            // argument must contain us
            if (!type.containsType(this)) {
                return false;
            }
        }

        // and all other types must be in the argument type as well
        for (Iterator it = m_typeList.iterator(); result && it.hasNext();) {
            if (!type.containsType((DataType)it.next())) {
                return false;
            }
        }
        return true;

    }

    /*
     * returns true if the argument is in our compatibility list or if it
     * matches our native value.
     */
    private boolean containsType(final DataType t) {
        if (this.isNativeType()
                && (this.getNativeValue().equals(t.getNativeValue()))) {
            return true;
        }
        return this.m_typeList.contains(t);
    }

    /**
     * @return A cell representing a missing data cell with no value. The type
     *         of the returned data cell is unknown (inner class).
     * 
     * Note: If you override this - and you should - it would be preferable to
     * create only one missing cell instance per type.
     */
    public DataCell getMissingCell() {
        return m_missing;
    }

    /**
     * @return a comparator for data cell objects of this type. Will return the
     *         native comparator (if provided), or any of the compatible types.
     *         If none of them provide a comparator the comparator of the string
     *         representations will be used
     */
    public final DataCellComparator getComparator() {

        DataCellComparator comp = getAnyNativeComparator();
        if (comp != null) {
            return comp;
        }
        // no comparator...use the default string representation comparator.
        return FALLBACK_COMP;
    }
    
    /** 
     * Get a copy of all <code>DataType</code>s to which this type is 
     * compatible to. The returned List is non-modifiable, subsequent changes 
     * to the list will fail with an exception. This list does not contain a 
     * reference to this type (altough it is self-compatible) and does not 
     * contain duplicates. 
     * @return A non-modifiable list of compatible types.
     * @see #addCompatibleType(DataType)
     * @see Collections#unmodifiableList(java.util.List)
     */
    public final List<DataType> getCompatibleTypes() {
        return Collections.unmodifiableList(m_typeList);
    }

    /*
     * this returns either the own native comparator or any native comparator of
     * any compatible type. It will return null if no native comparator is
     * avaliable. It will not fall back to the default string comparator.
     */
    private final DataCellComparator getAnyNativeComparator() {

        DataCellComparator comp = getNativeComparator();

        if (comp != null) {
            return comp;
        }

        // if we didn't get our native ask all compatibles type for one
        for (DataType t : m_typeList) {
            comp = t.getAnyNativeComparator();
            if (comp != null) {
                return comp;
            }
        }

        return null;
    }

    /**
     * Returns a type which is compatible to only those types both given types
     * are compatible to. I.e. it contains the intersection of both type lists.
     * This super type can be safely asked for a comparator for cells of both
     * specified types, or a renderer for datacells of any of the given types.
     * The returned object could be one of the arguments passed in, if one type
     * is compatible to all (and more) types the other is compatible to.
     * <p>
     * As there could be more than one common compatible type (if both cells
     * implement multiple value interfaces), the common super type could be a
     * new instance of "DataType" with no native type, which only contains the
     * list of those common compatible types.
     * 
     * @param type1 Type 1.
     * @param type2 Type 2.
     * @return a type compatible to types both arguments are compatible to.
     * @throws IllegalArgumentException If one of the given types is
     *             <code>null</code>.
     */
    public static DataType getCommonSuperType(final DataType type1,
            final DataType type2) {
        if (type1 == null || type2 == null) {
            throw new IllegalArgumentException("Cannot build super type of"
                    + " a null type");
        }
        // if identical?
        if (type1.equals(type2)) {
            return type1;
        }

        // is one the super type over the other?

        boolean type2isSuper = true;
        // let's see if type1 has everything of type2
        if (type2.isNativeType() && !type1.containsType(type2)) {
            type2isSuper = false;
        }
        if (type2isSuper) {
            for (DataType t2 : type2.m_typeList) {
                if (!type1.containsType(t2)) {
                    type2isSuper = false;
                    break;
                }
            }
        }
        if (type2isSuper) {
            return type2;
        }

        boolean type1isSuper = true;
        // for type1 to be supertype type2 must contain everything type1 has
        if (type1.isNativeType() && !type2.containsType(type1)) {
            type1isSuper = false;
        }
        if (type1isSuper) {
            for (DataType t1 : type1.m_typeList) {
                if (!type2.containsType(t1)) {
                    type1isSuper = false;
                    break;
                }
            }
        }
        if (type1isSuper) {
            return type1;
        }

        // now construct the super type containing all types of the intersection
        ArrayList<DataType> commonTypes = new ArrayList<DataType>();
        if (type2.m_typeList.contains(type1)) {
            assert type1.isNativeType(); // should only have added native
                                            // types
            commonTypes.add(type1);
        }
        if (type1.m_typeList.contains(type2)) {
            assert type2.isNativeType(); // should only have added native
                                            // types
            commonTypes.add(type2);
        }
        for (DataType currType : type1.m_typeList) {
            if (type2.m_typeList.contains(currType)) {
                commonTypes.add(currType);
            }
        }
        return new DataType(commonTypes);
    }

    /**
     * Types are equal, if their native type match (if they are native types)
     * AND if they are compatible to exactly the same types.
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(final Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof DataType)) {
            return false;
        }
        DataType otherType = (DataType)o;
        if ((otherType.isNativeType() && this.isNonNativeType())
                || (otherType.isNonNativeType() && this.isNativeType())) {
            return false;
        }
        // now both are either native or non-native
        if (isNativeType()
                && !this.getNativeValue().equals(otherType.getNativeValue())) {
            // if they are native, native type must match.
            return false;
        }
        if (this.m_typeList.size() != otherType.m_typeList.size()) {
            return false;
        }
        // assuming a list doesn't contain double entries a simple (one-sided)
        // contains should do (as they are of same size)
        for (DataType t : m_typeList) {
            if (!otherType.m_typeList.contains(t)) {
                return false;
            }
        }
        return true;
    }

    /**
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        if (isNativeType()) {
            return getNativeValue().hashCode();
        } else {
            int result = 0;
            for (DataType t : m_typeList) {
                result ^= t.getNativeValue().hashCode();
            }
            return result;
        }
    }

    /**
     * Returns "Non-native DataType".
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "Non-native DataType";
    }
}
