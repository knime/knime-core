/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2009
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
 * History
 *      21.06.06 (bw & po): reviewed
 */
package org.knime.core.data;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.Icon;

import org.knime.core.data.DataValue.UtilityFactory;
import org.knime.core.data.collection.CollectionDataValue;
import org.knime.core.data.renderer.DataValueRendererFamily;
import org.knime.core.data.renderer.DefaultDataValueRendererFamily;
import org.knime.core.data.renderer.SetOfRendererFamilies;
import org.knime.core.eclipseUtil.GlobalClassCreator;
import org.knime.core.internal.SerializerMethodLoader;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.config.ConfigRO;
import org.knime.core.node.config.ConfigWO;
import org.knime.core.node.util.ConvenienceMethods;

/**
 * Type description associated with a certain implementation of a
 * {@link DataCell}. It essentially keeps the list of compatible 
 * {@link DataValue} interfaces (i.e.
 * for which a type cast is possible), a list of 
 * {@link org.knime.core.data.renderer.DataValueRenderer} for this type, and 
 * (potentially more than one) {@link DataValueComparator} for {@link DataCell} 
 * of this type.
 *  
 * <p>
 * There are two forms of <code>DataType</code>s: the so-called native type, 
 * which is assigned to a certain {@link org.knime.core.data.DataCell}, 
 * and the non-native type, which only consists of a list of compatible 
 * {@link org.knime.core.data.DataValue} classes. 
 *  
 * <p>
 * Furthermore, it provides the possibility to get a common super type for two 
 * {@link org.knime.core.data.DataCell}s. In case they are not compatible to 
 * each other, the returned <code>DataType</code> is calculated based on the 
 * intersection of both compatible {@link org.knime.core.data.DataValue} lists.
 * 
 * <p>
 * In addition, the {@link org.knime.core.data.DataCell} representing missing 
 * values is defined in this class and can be accessed via 
 * {@link #getMissingCell()}.
 * 
 * <p>
 * On details of <code>DataType</code> in KNIME see the <a
 * href="package-summary.html">package description</a> and the <a
 * href="doc-files/newtypes.html">
 * manual</a> on how to define customized <code>DataType</code>s.
 * 
 * @see org.knime.core.data.DataCell
 * @see org.knime.core.data.DataValue
 * @author Bernd Wiswedel, University of Konstanz
 */
public final class DataType {
    
    /**
     * Implementation of the missing cell. This 
     * {@link org.knime.core.data.DataCell} does not implement
     * any {@link org.knime.core.data.DataValue} interfaces but is compatible 
     * to any value class. This class is the very only implementation whose 
     * isMissing() method returns true. 
     */
    private static final class MissingCell extends DataCell {
        
        /** Singleton to be used. */
        static final MissingCell INSTANCE = new MissingCell();

        /** 
         * Returns always <code>DataValue.class</code> as the most generic data
         * value.
         * @return <code>DataValue.class</code> always.
         */
        public static Class<? extends DataValue> getPreferredValueClass() {
            return DataValue.class;
        }
        
        /** Don't let anyone instantiate this class. */
        private MissingCell() {
        }
        
        /**
         * {@inheritDoc}
         */
        @Override
        public boolean equalsDataCell(final DataCell dc) {
            // guaranteed not to be called on and with a missing cell
            return false;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int hashCode() {
            return toString().hashCode();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        boolean isMissingInternal() {
            return true;
        }

        /**
         * Overridden here to return the singleton. This method is being 
         * called  by the java reflection mechanism.
         */
        private Object readResolve() {
            return INSTANCE;
        }
        
        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return "?";
        }
    }
    
    /** Config key for the cell class of native types. */
    private static final String CFG_CELL_NAME = "cell_class";
    /** Config key for the collection elements type. */
    private static final String CFG_COLL_ELEMENT_TYPE = 
        "collection_element_type";
    /** Config key for the preferred value class flag. */
    private static final String CFG_HAS_PREF_VALUE = "has_pref_value_class";
    /** Config key for value classes. */
    private static final String CFG_VALUE_CLASSES = "value_classes";

    /** Map of the referring <code>DataCell</code> serializer. */
    private static final Map<Class<? extends DataCell>, DataCellSerializer>
        CLASS_TO_SERIALIZER_MAP =
            new HashMap<Class<? extends DataCell>, DataCellSerializer>();
    
    /** 
     * Map containing all <code>DataCell.class-DataType</code> tuples 
     * (essentially this stores for each {@link org.knime.core.data.DataCell} 
     * implementation which {@link org.knime.core.data.DataValue} interfaces
     * it implements). Whenever <code>getType(Class &lt;? extends DataCell&gt;)
     * </code> is called and this map does not contain the corresponding 
     * <code>DataType</code>, the type will be created (using one of the 
     * <code>DataType</code> constructors) and
     * added to this map. This map makes sure that the <code>getType()</code> 
     * method is fast and that there will be no duplicate <code>DataType</code> 
     * instances for  different instances of the 
     * {@link org.knime.core.data.DataValue} implementation.
     */
    private static final Map<ClassAndSubDataTypePair, DataType> 
        CLASS_TO_TYPE_MAP = new HashMap<ClassAndSubDataTypePair, DataType>();
    
    /**
     * The String representation comparator. Fall back comparator if no other is
     * available.
     */
    private static final DataValueComparator FALLBACK_COMP = 
        new DataValueComparator() {
        /**
         * Compares two DataValues based on their String representation 
         * (<code>toString()</code>). 
         * @throws NullPointerException if one or both of the passed data 
         * values is <code>null</code>
         * 
         * @see org.knime.core.data.DataValueComparator#compareDataValues(
         * org.knime.core.data.DataValue, org.knime.core.data.DataValue)
         */
        @Override
        protected int compareDataValues(
                final DataValue v1, final DataValue v2) {
            return v1.toString().compareTo(v2.toString());
        }
    };
    
    /** Logger for DataType class. */
    private static final NodeLogger LOGGER = 
        NodeLogger.getLogger(DataType.class);
    
    /** 
     * Hashmap to retrieve the <code>UtilityFactory</code> for each 
     * {@link org.knime.core.data.DataValue} interface. 
     */
    private static final Map<Class<? extends DataValue>, UtilityFactory> 
        VALUE_CLASS_TO_UTILITY = 
            new HashMap<Class<? extends DataValue>, UtilityFactory>();
    
    /**
     * Recursive method that walks up the inheritance tree of a given class and 
     * determines all {@link org.knime.core.data.DataValue} interfaces being 
     * implemented. Any such detected interface will be added to the passed set.
     * Used from the constructor to determine implemented interfaces of a 
     * {@link org.knime.core.data.DataCell} class. 
     * 
     * @see DataType#DataType(Class)
     */
    private static void addDataValueInterfaces(
            final Set<Class<? extends DataValue>> set, final Class<?> current) {
        // all interfaces that current implements
        for (Class<?> c : current.getInterfaces()) {
            if (DataValue.class.isAssignableFrom(c)) {
                @SuppressWarnings("unchecked")
                Class<? extends DataValue> cv = (Class<? extends DataValue>)c;
                // hash the utility object
                getUtilityFor(cv);
                set.add(cv);
            }
            // interfaces may extend other interface, handle them here!
            addDataValueInterfaces(set, c);
        }
        Class<?> superClass = current.getSuperclass();
        if (superClass != null) {
            addDataValueInterfaces(set, superClass);
        }
    }

    /** 
     * Clones the given <code>DataType</code> but changes its preferred value 
     * class to the passed <code>preferred</code> value. The returned non-native
     * <code>DataType</code> may or may not be equal to the <code>from</code> 
     * <code>DataType</code> depending on the preferred value class of 
     * <code>from</code> <code>DataType</code> and will be created newly.
     * 
     * @param from the <code>DataType</code> to clone
     * @param preferred the new preferred value class
     * @return a new <code>DataType</code> with the given preferred value class
     * @throws IllegalArgumentException 
     *  if <code>from.isCompatible(preferred)</code> returns <code>false</code>
     * @throws NullPointerException if any argument is <code>null</code>
     */
    public static DataType cloneChangePreferredValue(final DataType from,
            final Class<? extends DataValue> preferred) {
        return new DataType(from, preferred);
    }
    
    /**
     * Returns a {@link org.knime.core.data.DataCellSerializer} for the runtime 
     * class of a {@link org.knime.core.data.DataCell} or <code>null</code> if 
     * the passed class of type {@link org.knime.core.data.DataCell} does not 
     * implement
     * <pre>
     *   public static &lt;T extends DataCell&gt; 
     *       DataCellSerializer&lt;T&gt; getCellSerializer(
     *       final Class&lt;T&gt; cl) {
     * </pre>
     * The {@link org.knime.core.data.DataCellSerializer} is defined
     * through a static access method in {@link DataCell}. If no such method 
     * exists or the method can't be invoked (using reflection), this method 
     * returns <code>null</code> and ordinary Java serialization is used for
     * saving and loading the cell. See also the class documentation of 
     * {@link org.knime.core.data.DataCell} for more information on the static 
     * access method.
     *  
     * @param <T> the runtime class of the {@link org.knime.core.data.DataCell}
     * @param cl the <code>DataCell</code>'s class
     * @return the {@link org.knime.core.data.DataCellSerializer} defined in 
     * the {@link org.knime.core.data.DataCell} implementation
     * or <code>null</code> if no such serializer is available
     * @throws NullPointerException if the argument is <code>null</code>
     */
    @SuppressWarnings("unchecked") // access to CLASS_TO_SERIALIZER_MAP
    public static <T extends DataCell> 
        DataCellSerializer<T> getCellSerializer(final Class<T> cl) {
        if (CLASS_TO_SERIALIZER_MAP.containsKey(cl)) {
            return CLASS_TO_SERIALIZER_MAP.get(cl);
        }
        DataCellSerializer<T> result = null;
        try {
            result = (DataCellSerializer<T>)SerializerMethodLoader.
            getSerializer(cl, DataCellSerializer.class, 
                    "getCellSerializer", false);
        } catch (NoSuchMethodException nsme) {
            if (KNIMEConstants.ASSERTIONS_ENABLED) {
                LOGGER.warn("Class \"" + cl.getSimpleName() + "\" does not " 
                        + "define method \"getCellSerializer\", using ordinary "
                        + "(but slow) java serialization. This warning " 
                        + "does not appear if assertions are off.", nsme);
            }
        }
        CLASS_TO_SERIALIZER_MAP.put(cl, result);
        return result;
    }

    /**
     * Returns a type which is compatible to only those values, both given types
     * are compatible to, i.e. it contains the intersection of both value lists.
     * This super type can be safely asked for a comparator for cells of both
     * specified types, or a renderer for {@link org.knime.core.data.DataCell}s 
     * of any of the given types.
     * The returned object could be one of the arguments passed in, if one type
     * is compatible to all (and more) types the other is compatible to.
     * 
     * <p>
     * As there could be more than one common compatible type (if both cells
     * implement multiple value interfaces), the common super type could be a
     * new instance of <code>DataType</code> with no native type, which only 
     * contains the list of those common compatible types.
     * 
     * <p>
     * If both types have no common data values, the resulting 
     * <code>DataType</code> has no preferred value class; its list of 
     * compatible value classes will only contain (the class representation of)
     * {@link DataValue}, and the cell class is <code>null</code>.   
     * 
     * @param type1 type 1
     * @param type2 type 2
     * @return a type compatible to types both arguments are compatible to
     * @throws NullPointerException if one of the given types is 
     *          <code>null</code>
     */
    public static DataType getCommonSuperType(final DataType type1,
            final DataType type2) {
        if ((type1 == null) || (type2 == null)) {
            throw new NullPointerException("Cannot build super type of"
                    + " a null type");
        }
        // if one of the types represents the missing cell type, we
        // return the other type. 
        if ((type1.m_cellClass != null) 
                && type1.m_cellClass.equals(MissingCell.class)) {
            return type2;
        }
        if ((type2.m_cellClass != null) 
                && type2.m_cellClass.equals(MissingCell.class)) {
            return type1;
        }
            
        // handles also the equals case
        if (type1.isASuperTypeOf(type2)) {
            return type1;
        }
        if (type2.isASuperTypeOf(type1)) {
            return type2;
        }
        return new DataType(type1, type2);
    }

    /**
     * A cell representing a missing {@link org.knime.core.data.DataCell} 
     * where the value is unknown. 
     * The type of the returned data cell is compatible to any 
     * <code>DataValue</code> interface (although you can't cast the returned 
     * cell to the value) and has default icons, renderers, and comparators.
     * 
     * @return singleton of a missing cell
     */
    public static DataCell getMissingCell() {
        return MissingCell.INSTANCE;
    }

    /** 
     * Internal access method to determine the preferred 
     * {@link org.knime.core.data.DataValue} class to a given 
     * {@link org.knime.core.data.DataCell} implementation. This method tries 
     * to invoke a static method on the argument with the following signature
     * <pre>
     *   public static Class<? extends DataValue> getPreferredValueClass()
     * </pre>
     * If no such method exists, this method returns <code>null</code>. If it 
     * exists but has the wrong scope or return value, a warning message is 
     * logged and <code>null</code> is returned. 
     * @param cl the runtime class of the {@link org.knime.core.data.DataCell}
     * @return its preferred {@link org.knime.core.data.DataValue} interface or 
     * <code>null</code>
     * @throws NullPointerException if the argument is <code>null</code>
     */
    private static Class<? extends DataValue> getPreferredValueClassFor(
            final Class<? extends DataCell> cl) {
        if (cl == null) {
            throw new NullPointerException("Class argument must not be null.");
        }
        Exception exception = null;
        try {
            Method method = cl.getMethod("getPreferredValueClass");
            Class<? extends DataValue> result = 
                (Class<? extends DataValue>)method.invoke(null);
            LOGGER.debug(result.getSimpleName() + " is the preferred value "
                    + "class of cell implementation " + cl.getSimpleName() 
                    + ", made sanity check");
            return result;
        } catch (NoSuchMethodException nsme) {
            // no such method - perfectly ok, ignore it.
            LOGGER.debug("Class \"" + cl.getSimpleName() + "\" doesn't " 
                    + "have a default value class (it does not implement "
                    + "a static method \"getPreferredValueClass()\"). " 
                    + "Returning null");
            return null;
        } catch (InvocationTargetException ite) {
            exception = ite;
        } catch (NullPointerException npe) {
            exception = npe;
        } catch (IllegalAccessException iae) {
            exception = iae;
        } catch (ClassCastException cce) {
            exception = cce;
        }
        LOGGER.coding("Class \"" + cl.getSimpleName() + "\" has a problem " 
                + "with the static method \"getPreferredValueClass\"."
                + " Returning null. "
                + "Caught an " + exception.getClass().getSimpleName(), 
                exception);
        return null;
    }
    
    /** 
     * Returns the runtime <code>DataType</code> for a 
     * {@link org.knime.core.data.DataCell} 
     * implementation. If no such <code>DataType</code> has been created 
     * before (i.e. this method is called the first time with the current
     * argument), the return value is created and hashed. The returned 
     * <code>DataType</code> will claim to be compatible to all 
     * {@link org.knime.core.data.DataValue} classes the cell is implementing 
     * (i.e. what {@link org.knime.core.data.DataValue} interfaces are 
     * implemented by <code>cell</code>) and will bundle meta information of the
     * cell such as renderer, icon, and comparators.
     *  
     * <p>
     * This method is the only way to determine the <code>DataType</code>
     * of a {@link org.knime.core.data.DataCell}. However, most standard cell 
     * implementations have a static member for convenience access, e.g. 
     * {@link org.knime.core.data.def.DoubleCell#TYPE DoubleCell.TYPE}.
     * 
     * <p>
     * If the argument class implements a {@link CollectionDataValue} 
     * (a special cell that bundles multiple cells into one), consider to use
     * {@link #getType(Class, DataType)} in order to retain the sub element 
     * type. In most cases, however, this method is the preferred way to 
     * determine the <code>DataType</code> to a cell class.
     * 
     * @see org.knime.core.data.DataCell
     * @see org.knime.core.data.DataValue
     * @see org.knime.core.data.DataCell#getType()
     * 
     * @param cell the runtime class of {@link org.knime.core.data.DataCell} for
     * which the <code>DataType</code> is requested
     * @return the corresponding <code>DataType</code> <code>cell</code>, 
     * never <code>null</code>
     * @throws NullPointerException if the argument is <code>null</code>
     */
    public static DataType getType(final Class<? extends DataCell> cell) {
        if (cell == null) {
            throw new NullPointerException("Class must not be null.");
        }
        ClassAndSubDataTypePair key = new ClassAndSubDataTypePair(cell, null);
        DataType result = CLASS_TO_TYPE_MAP.get(key);
        if (result == null) {
            result = new DataType(cell, null);
            CLASS_TO_TYPE_MAP.put(key, result);
        }
        return result;
    }
    
    /** Implementation of {@link #getType(Class)} dedicated for special cell
     * classes that represent collections of {@link DataCell}. The second 
     * argument <code>collectionElementType</code> must be non-null if and only
     * if <code>cellImplementsCollectionDataValue</code> implements 
     * {@link CollectionDataValue}, otherwise an 
     * {@link IllegalArgumentException} is thrown.
     * 
     * <p>For a general description of types representing collections,
     * refer to the section in the 
     * <a href="doc-files/newtypes.html#typecollection">manual</a>.
     * 
     * @param cellImplementsCollectionDataValue The cell class hosting the
     * collection by implementing {@link CollectionDataValue}. 
     * (Other cell are also accepted, but then
     *  <code>collectionElementType</code> must be null.)  
     * @param collectionElementType The type of the elements in the collection.
     * @return A data type representing that type of collection.
     * @throws IllegalArgumentException As outlined above.
     * @throws NullPointerException If the class argument is null.
     */
    public static DataType getType(
            final Class<? extends DataCell> cellImplementsCollectionDataValue,
            final DataType collectionElementType) {
        if (cellImplementsCollectionDataValue == null) {
            throw new NullPointerException("Cell class must not be null.");
        }
        ClassAndSubDataTypePair key = new ClassAndSubDataTypePair(
                cellImplementsCollectionDataValue, collectionElementType);
        DataType result = CLASS_TO_TYPE_MAP.get(key);
        if (result == null) {
            if (!CollectionDataValue.class.isAssignableFrom(
                    cellImplementsCollectionDataValue) 
                    && collectionElementType != null) {
                throw new IllegalArgumentException("Cell class \"" 
                        + cellImplementsCollectionDataValue.getSimpleName() 
                        + " does not implement \"" 
                        + CollectionDataValue.class.getSimpleName() + "\"");
            }
            result = new DataType(
                    cellImplementsCollectionDataValue, collectionElementType);
            CLASS_TO_TYPE_MAP.put(key, result);
        }
        return result;
    }

    /** 
     * Determines the <code>UtilityFactory</code> for a given 
     * {@link org.knime.core.data.DataValue} implementation. 
     * This method tries to access a static field in the 
     * {@link org.knime.core.data.DataValue} class: 
     * <pre>
     *   public static final UtilityFactory UTILITY;
     * </pre>
     * If no such field exists, this method returns the factory of one of the 
     * super interfaces. If it exists but has the wrong access scope or if it 
     * is not static, a warning message is logged and the member of one of the 
     * super interfaces is returned. If no <code>UTILITY</code> member can be 
     * found, finally the {@link org.knime.core.data.DataValue} class returns a
     * correct implementation, since it is at the top of the hierarchy. 
     * 
     * @param value the runtime class of the 
     *        {@link org.knime.core.data.DataCell}
     * @return the <code>UtilityFactory</code> given in the cell implementation
     * @throws NullPointerException if the argument or the found 
     * <code>UTILITY</code> member is <code>null</code>
     */
    public static UtilityFactory getUtilityFor(
        final Class<? extends DataValue> value) {
        if (value == null) {
            throw new NullPointerException("Class argument must not be null.");
        }
        UtilityFactory result = VALUE_CLASS_TO_UTILITY.get(value);
        if (result == null) {
            Exception exception = null;
            try {
                // Java will fetch a static field that is public, if you
                // declare it to be non-static or give it the wrong scope, it 
                // automatically retrieves the static field from a super
                // class/interface (from which super interface it gets it,
                // depends pretty much on the order after the "extends ..."
                // statement) If this field has the wrong type, a coding
                // problem is reported.
                Field typeField = value.getField("UTILITY");
                Object typeObject = typeField.get(null);
                result = (DataValue.UtilityFactory)typeObject;
                if (result == null) {
                    throw new NullPointerException("UTILITY is null.");
                }
            } catch (NoSuchFieldException nsfe) {
                exception = nsfe;
            } catch (NullPointerException npe) {
                exception = npe;
            } catch (IllegalAccessException iae) {
                exception = iae;
            } catch (ClassCastException cce) {
                exception = cce;
            }
            if (exception != null) {
                LOGGER.coding("DataValue interface \"" + value.getSimpleName() 
                        + "\" seems to have a problem with the static field " 
                        + "\"UTILITY\"", exception);
                // fall back - no meta information available
                result = DataValue.UTILITY;
            }
            VALUE_CLASS_TO_UTILITY.put(value, result);
        }
        return result;
    }
    
    /**
     * Loads a <code>DataType</code> from a given 
     * {@link org.knime.core.node.config.ConfigRO}.
     * 
     * @param config load <code>DataType</code> from
     * @return a <code>DataType</code> which fits the given 
     * {@link org.knime.core.node.config.ConfigRO}
     * @throws InvalidSettingsException if the <code>DataType</code> could not
     *         be loaded from the given 
     *         {@link org.knime.core.node.config.ConfigRO}
     */
    @SuppressWarnings("unchecked") // type casts
    public static DataType load(final ConfigRO config) 
            throws InvalidSettingsException {
        String cellClassName = config.getString(CFG_CELL_NAME);
        // collection elements class name, null for "ordinary" cells
        DataType collectionElementType = null;
        if (config.containsKey(CFG_COLL_ELEMENT_TYPE)) {
            collectionElementType = 
                load(config.getConfig(CFG_COLL_ELEMENT_TYPE));
        }
        // if it has a class name it is a native type
        if (cellClassName != null) {
            try {
                Class<? extends DataCell> cellClass = 
                    (Class<? extends DataCell>)
                    GlobalClassCreator.createClass(cellClassName);
                return getType(cellClass, collectionElementType);
            } catch (ClassCastException cce) {
                throw new InvalidSettingsException(cellClassName 
                    + " Class not derived from DataCell: " + cce.getMessage());
            } catch (ClassNotFoundException cnfe) {
                throw new InvalidSettingsException(cellClassName 
                    + " Class not found: " + cnfe.getMessage());
            }
        }
        
        // non-native type, must have the following fields.
        boolean hasPrefValueClass = config.getBoolean(CFG_HAS_PREF_VALUE);
        String[] valueClassNames = config.getStringArray(CFG_VALUE_CLASSES);
        List<Class<? extends DataValue>> valueClasses = 
            new ArrayList<Class<? extends DataValue>>();
        for (int i = 0; i < valueClassNames.length; i++) {
            try {
                valueClasses.add((Class<? extends DataValue>) 
                        GlobalClassCreator.createClass(valueClassNames[i]));
            } catch (ClassCastException cce) {
                throw new InvalidSettingsException(valueClassNames[i] 
                    + " Class not derived from DataValue: " + cce.getMessage());
            } catch (ClassNotFoundException cnfe) {
                throw new InvalidSettingsException(valueClassNames[i] 
                    + " Class not found: " + cnfe.getMessage());
            }
        }
        try { 
            return new DataType(
                    hasPrefValueClass, valueClasses, collectionElementType);
        } catch (IllegalArgumentException iae) {
            throw new InvalidSettingsException(iae);
        }
    }
    
    /** Cell class defines a native type. */
    private final Class<? extends DataCell> m_cellClass;
    
    /** Flag if the first element in m_valueClasses contains the 
     * preferred value class. */
    private final boolean m_hasPreferredValueClass;
    
    /** List of all implemented {@link org.knime.core.data.DataValue} 
     * interfaces. */
    private final List<Class<? extends DataValue>> m_valueClasses;
    
    private final DataType m_collectionElementType;
    
    /**
     * Creates a new, non-native <code>DataType</code>. This method is used 
     * from the {@link #load(ConfigRO)} method.
     * @param hasPreferredValue if preferred value is set
     * @param valueClasses all implemented {@link org.knime.core.data.DataValue}
     * interfaces
     */
    private DataType(final boolean hasPreferredValue, 
            final List<Class<? extends DataValue>> valueClasses,
            final DataType collectionElementType) {
        m_cellClass = null;
        m_hasPreferredValueClass = hasPreferredValue;
        m_valueClasses = valueClasses;
        boolean hasCollectionDataValue = false;
        for (Class<? extends DataValue> v : m_valueClasses) {
            if (CollectionDataValue.class.isAssignableFrom(v)) {
                hasCollectionDataValue = true;
            }
        }
        if (collectionElementType != null && !hasCollectionDataValue) {
            throw new IllegalArgumentException(
                    "None of the given DataValue interfaces subclasses \"" 
                    + CollectionDataValue.class.getSimpleName() + "\" although "
                    + "a collection element type was provided");
        }
        if (hasCollectionDataValue && collectionElementType == null) {
            throw new IllegalArgumentException("The type represents a "
                    + "collection but no element type was provided");
        }
        m_collectionElementType = collectionElementType;
    }

    /**
     * Creates a new type for the passed {@link org.knime.core.data.DataCell} 
     * class. This implementation determines all 
     * {@link org.knime.core.data.DataValue} interfaces that the cell is 
     * implementing and also retrieves their meta information. This constructor 
     * is used by the static {@link #getType(Class)} method.
     */
    private DataType(final Class<? extends DataCell> cl, 
            final DataType elementType) {
        // filter for classes that extend DataValue
        LinkedHashSet<Class<? extends DataValue>> valueClasses = 
            new LinkedHashSet<Class<? extends DataValue>>();
        addDataValueInterfaces(valueClasses, cl);
        Class<? extends DataValue> preferred = 
            DataType.getPreferredValueClassFor(cl);
        if ((preferred != null) && !valueClasses.contains(preferred)) {
            LOGGER.coding("Class \"" + cl.getSimpleName() + "\" declares \"" 
                    + preferred + "\" as its preferred value class but does " 
                    + "not implement the interface!");
        }
        m_valueClasses = new ArrayList<Class<? extends DataValue>>();
        m_hasPreferredValueClass = preferred != null;
        // put preferred value class at the first position in m_valueClasses
        if (m_hasPreferredValueClass) {
            m_valueClasses.add(preferred);
            valueClasses.remove(preferred);
        }
        m_valueClasses.addAll(valueClasses);
        m_cellClass = cl;
        if (CollectionDataValue.class.isAssignableFrom(cl) 
                && elementType == null) {
            throw new IllegalArgumentException("The type represents a "
                    + "collection but no element type was provided");
        }
        if (!CollectionDataValue.class.isAssignableFrom(cl)
                && elementType != null) {
            throw new IllegalArgumentException("The type does not represent "
                    + "a collection, element type must be null");
        }
        m_collectionElementType = elementType;
    }
    
    /** 
     * Constructor that is used when the preferred value class should change
     * in the given <code>DataType</code>. This <code>DataType</code> is 
     * typically never assigned to one particular 
     * {@link org.knime.core.data.DataCell} class (otherwise the constructor
     * <code>DataType(Class)</code> would have been used) and therefore this 
     * type is not cached. This means, the resulting <code>DataType</code> is 
     * not native, i.e. the cell class is <code>null</code>.
     * 
     * @param type the type to clone
     * @param preferred the new preferred value class of the new type
     */
    private DataType(final DataType type, 
            final Class<? extends DataValue> preferred) {
        if (!type.m_valueClasses.contains(preferred)) {
            throw new IllegalArgumentException("Invalid preferred " 
                    + "value class: " + preferred.getSimpleName());
        }
        // override, not assigned to any data cell implementation
        m_cellClass = null;
        m_valueClasses = new ArrayList<Class<? extends DataValue>>(
                type.m_valueClasses.size());
        m_valueClasses.add(preferred);
        for (Class<? extends DataValue> c : type.m_valueClasses) {
            if (!c.equals(preferred)) {
                m_valueClasses.add(c);
            }
        }
        m_collectionElementType = type.m_collectionElementType;
        m_hasPreferredValueClass = true;
    }

    /**
     * Determines the list of compatible value interfaces as intersection of
     * the two arguments. This constructor is used by the 
     * {@link #getCommonSuperType(DataType, DataType)} method.
     */
    private DataType(final DataType type1, final DataType type2) {
        // preferred class must match, if any
        m_hasPreferredValueClass =  
            type1.m_hasPreferredValueClass && type2.m_hasPreferredValueClass
            && type1.m_valueClasses.get(0).equals(type2.m_valueClasses.get(0));
        // linked set ensures that the first element is the preferred value 
        LinkedHashSet<Class<? extends DataValue>> hash = 
            new LinkedHashSet<Class<? extends DataValue>>();
        hash.addAll(type1.m_valueClasses);
        hash.retainAll(type2.m_valueClasses);
        m_valueClasses = new ArrayList<Class<? extends DataValue>>(hash);
        if (type1.m_collectionElementType != null
                && type2.m_collectionElementType != null) {
            boolean containsCollectionValue = false;
            for (Class<? extends DataValue> v : m_valueClasses) {
                if (CollectionDataValue.class.isAssignableFrom(v)) {
                    containsCollectionValue = true;
                }
            }
            if (containsCollectionValue) {
                m_collectionElementType = getCommonSuperType(
                        type1.m_collectionElementType, 
                        type2.m_collectionElementType);
            } else {
                m_collectionElementType = null;
            }
        } else {
            m_collectionElementType = null;
        }
        m_cellClass = null;
    }
    
    /** Get the type of the elements in collection or <code>null</code> if
     * this type does not represent a collection. This method returns a non-null
     * value if and only if this type is {@link #isCompatible(Class) is 
     * compatible} to {@link CollectionDataValue}.
     * @return the type of the elements in a collection or null.
     */
    public DataType getCollectionElementType() {
        return m_collectionElementType;
    }
    
    /** Does this type represent a collection. This method is a convenience 
     * short cut for <code>isCompatible(CollectionDataValue.class)</code>.
     * 
     * <p>If this method returns true, {@link #getCollectionElementType()} is
     * guaranteed to return a non-null value.
     * @return If this type represent 
     */
    public boolean isCollectionType() {
        return isCompatible(CollectionDataValue.class);
    }

    /**
     * Types are equal if the list of compatible 
     * {@link org.knime.core.data.DataValue} classes matches (ordering
     * does not matter), both types have the same preferred value class or both 
     * do not have a preferred value class.
     * @param other the object to check with
     * @return <code>true</code> if the both types have the same preferred value
     *         class and the list of compatible types matches, otherwise
     *         <code>false</code>
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(final Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof DataType)) {
            return false;
        }
        DataType o = (DataType)other;
        // both have a preferred value or both do not. 
        if (m_hasPreferredValueClass != o.m_hasPreferredValueClass) {
            return false;
        }
        // if they do, both preferred value classes must match
        if (m_hasPreferredValueClass) {
            Class<? extends DataValue> myPreferred = m_valueClasses.get(0);
            Class<? extends DataValue> oPreferred = o.m_valueClasses.get(0);
            if (!myPreferred.equals(oPreferred)) {
                return false;
            }
        }
        if (!m_valueClasses.containsAll(o.m_valueClasses)) {
            return false;
        }
        if (!o.m_valueClasses.containsAll(m_valueClasses)) {
            return false;
        }
        return true;
    }

    /**
     * A comparator for {@link org.knime.core.data.DataCell} objects of this 
     * type. Will return the native comparator (if provided), or the first 
     * comparator of the value classes found. If no comparator is available the
     * comparator of the <code>String</code> representation will be returned.
     * @return a comparator for cells of this type
     */
    public DataValueComparator getComparator() {
        for (Class<? extends DataValue> cl : m_valueClasses) {
            UtilityFactory fac = getUtilityFor(cl);
            DataValueComparator comparator = fac.getComparator();
            if (comparator != null) {
                return comparator;
            }
        }
        return FALLBACK_COMP; 
    }
    
    /**
     * Gets and returns an icon from the {@link UtilityFactory} representing 
     * this type. This is used in table headers and lists, for instance.
     * 
     * @return an icon for this type
     */
    public Icon getIcon() {
        for (Class<? extends DataValue> cl : m_valueClasses) {
            UtilityFactory fac = getUtilityFor(cl);
            Icon icon = fac.getIcon();
            if (icon != null) {
                return icon;
            }
        }
        assert false : "Couldn't find proper icon.";
        return DataValue.UTILITY.getIcon(); 
    }

    /** 
     * Returns the preferred value class of the current <code>DataType</code> 
     * or <code>null</code> if not available. The preferred value class is
     * defined through the {@link org.knime.core.data.DataCell} 
     * implementation. This method returns <code>null</code> if either the cell 
     * implementation lacks the information or this <code>DataType</code> has 
     * been created as an intersection of two types whose preferred value 
     * classes do not match.
     * 
     * @return the preferred value class or <code>null</code>  
     */ 
    public Class<? extends DataValue> getPreferredValueClass() {
        if (m_hasPreferredValueClass) {
            return m_valueClasses.get(0);
        }
        return null;
    }

    /**
     * Returns a family of all renderers that are available for this 
     * <code>DataType</code>. The returned 
     * {@link org.knime.core.data.renderer.DataValueRendererFamily} will contain
     * all renderers that are supported or available through the compatible 
     * {@link org.knime.core.data.DataValue} interfaces. If no renderer was 
     * declared by the {@link org.knime.core.data.DataValue} interfaces, 
     * this method will make sure that at least a default renderer (using the 
     * {@link DataCell#toString()} method) is returned.
     * 
     * <p> 
     * The {@link DataColumnSpec} is passed to all renderer families retrieved
     * from the underlying {@link UtilityFactory}. Most of the renderer 
     * implementations won't need column domain information but some do. For 
     * instance a class that renders the double value in the column according to
     * the minimum/maximum values in the {@link DataColumnDomain}.
     * 
     * @param spec the column spec to the column for which the renderer will be
     *            used
     * @return a family of all renderers that are available for this 
     *         <code>DataType</code>
     */
    public DataValueRendererFamily getRenderer(final DataColumnSpec spec) {
        ArrayList<DataValueRendererFamily> list = 
            new ArrayList<DataValueRendererFamily>();
        // first add the preferred value class, if any
        for (Class<? extends DataValue> cl : m_valueClasses) {
            UtilityFactory fac = getUtilityFor(cl);
            DataValueRendererFamily fam = fac.getRendererFamily(spec);
            if (fam != null) {
                list.add(fam);
            }
        }
        if (list.isEmpty()) {
            list.add(new DefaultDataValueRendererFamily());
        }
        return new SetOfRendererFamilies(list);
    }

    /**
     * Returns a copy of all {@link org.knime.core.data.DataValue}s to which 
     * this <code>DataType</code> is compatible to. The returned 
     * <code>List</code> is non-modifiable, subsequent changes to the list will
     * fail with an exception. The list does not contain duplicates.
     * 
     * @return a non-modifiable list of compatible <code>DataType</code>s
     */
    public List<Class<? extends DataValue>> getValueClasses() {
        return Collections.unmodifiableList(m_valueClasses);
    }

    /**
     * The hash code is based on the preferred value flag and the hash codes of 
     * all {@link DataValue} classes.
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        int result = Boolean.valueOf(m_hasPreferredValueClass).hashCode();
        for (Class<? extends DataValue> cl : m_valueClasses) {
            result ^= cl.hashCode();
        }
        return result;
    }

    /**
     * Returns <code>true</code> if this <code>type</code> is a supertype of the
     * passed type, that is, the argument is compatible to all {@link DataValue}
     * classes of this type (and may be more).
     * In other words, this object is more general than the argument or this
     * object supports less compatible values than the argument.
     * 
     * <p>
     * This is mostly used to test if a given {@link DataCell} can be added to
     * a given {@link DataColumnSpec}. The {@link DataCell}'s type must be 
     * compatible to (at least) all {@link DataValue} interfaces the column's 
     * type is compatible to. If the column's type is a supertype of the cell's 
     * type, it's safe to add the cell to the column.
     * 
     * @param type the type to test, whether this is a supertype of it
     * @return <code>true</code> if this type is a (one of many possible) 
     *         supertype of the argument type, otherwise <code>false</code>
     * @throws NullPointerException if the type argument is <code>null</code>
     * @see #isCompatible(Class)
     */
    public boolean isASuperTypeOf(final DataType type) {
        if (type == null) {
            throw new NullPointerException("Type argument must not be null.");
        }
        if (type == this) {
            return true;
        }
        for (Class<? extends DataValue> cl : m_valueClasses) {
            if (!type.isCompatible(cl)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks if the given <code>DataValue.class</code> is compatible to this
     * type. It returns <code>true</code>, if  
     * {@link org.knime.core.data.DataCell}s of this type can be type-casted to 
     * the <code>valueClass</code> or if one assignable value class was found or
     * if this type represents the missing cell. This method returns 
     * <code>false</code>, if the argument is <code>null</code> or if no 
     * assignable class was found.  

     * @param valueClass class to check compatibility for
     * @return <code>true</code> if compatible
     */
    public boolean isCompatible(
            final Class<? extends DataValue> valueClass) {
        if ((m_cellClass != null) && m_cellClass.equals(MissingCell.class)) {
            return true;
        }
        for (Class<? extends DataValue> cl : m_valueClasses) {
            if (valueClass.isAssignableFrom(cl)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Saves this <code>DataType</code> to the given 
     * {@link org.knime.core.node.config.ConfigWO}. 
     * If it is a native type only the class name of the cell class is stored. 
     * Otherwise the names of all value classes and whether it has a preferred 
     * value is written to the {@link org.knime.core.node.config.ConfigWO}.
     *  
     * @param config write to this {@link org.knime.core.node.config.ConfigWO}
     */
    public void save(final ConfigWO config) {
        if (m_collectionElementType != null) {
            ConfigWO sub = config.addConfig(CFG_COLL_ELEMENT_TYPE);
            m_collectionElementType.save(sub);
        }

        if (m_cellClass == null) {
            config.addString(CFG_CELL_NAME, null);
            config.addBoolean(CFG_HAS_PREF_VALUE, m_hasPreferredValueClass);
            String[] valueClasses = new String[m_valueClasses.size()];
            for (int i = 0; i < valueClasses.length; i++) {
                valueClasses[i] = m_valueClasses.get(i).getName();
            }
            config.addStringArray(CFG_VALUE_CLASSES, valueClasses);
        } else {
            // only memorize cell class (is hashed anyway)
            config.addString(CFG_CELL_NAME, m_cellClass.getName());
        }
    }
   
    /**
     * Returns the simple name of the {@link DataCell} class (if any) or 
     * <i>Non-Native</i> the <code>toString()</code> results of all compatible 
     * values classes.
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        if (m_cellClass != null) {
            b.append(m_cellClass.getSimpleName()); 
        } else {
            b.append("Non-Native ");
            b.append(Arrays.toString(m_valueClasses.toArray()));
        }
        if (getCollectionElementType() != null) {
            b.append(" (Collection of: ");
            b.append(getCollectionElementType().toString());
            b.append(")");
        }
        return b.toString();
    }
    
    private static final class ClassAndSubDataTypePair {
        private final Class<? extends DataCell> m_cellClass;
        private final DataType m_elementDataType;
        
        /**
         * 
         */
        ClassAndSubDataTypePair(
                final Class<? extends DataCell> cellClass,
                final DataType elementDataType) {
            assert cellClass != null : "Cell class is null";
            m_cellClass = cellClass;
            m_elementDataType = elementDataType;
        }
        
        /** {@inheritDoc} */
        @Override
        public String toString() {
            StringBuilder b = new StringBuilder();
            b.append("Cell: ");
            b.append(m_cellClass.getSimpleName());
            if (m_elementDataType != null) {
                b.append(" (element type: ");
                b.append(m_elementDataType.toString());
                b.append(")");
            }
            return b.toString();
        }
        
        /** {@inheritDoc} */
        @Override
        public int hashCode() {
            int hash = m_cellClass.hashCode();
            if (m_elementDataType != null) {
                hash = hash ^ m_elementDataType.hashCode();
            }
            return hash;
        }
        
        /** {@inheritDoc} */
        @Override
        public boolean equals(final Object obj) {
            if (obj == this) {
                return true;
            }
            if (!(obj instanceof ClassAndSubDataTypePair)) {
                return false;
            }
            ClassAndSubDataTypePair d = (ClassAndSubDataTypePair)obj;
            if (!d.m_cellClass.equals(m_cellClass)) {
                return false;
            }
            // handles null cases
            return ConvenienceMethods.areEqual(
                    d.m_elementDataType, m_elementDataType);
        }
    }
}
