/*
 * ------------------------------------------------------------------
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
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * -------------------------------------------------------------------
 * 
 * History
 *      21.06.06 (bw & po): reviewed
 */
package org.knime.core.data;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
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
import org.knime.core.data.renderer.DataValueRendererFamily;
import org.knime.core.data.renderer.DefaultDataValueRendererFamily;
import org.knime.core.data.renderer.SetOfRendererFamilies;
import org.knime.core.eclipseUtil.GlobalClassCreator;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.config.ConfigRO;
import org.knime.core.node.config.ConfigWO;

/**
 * Type description associated with a certain implementation of a
 * {@link DataCell}. It essentially keeps the list of compatible (i.e.
 * castable) {@link DataValue} interfaces, a list of renderers for this type,
 * and (potentially more than one) comparator for data cells of this type.
 *  
 * <p>
 * Also, it provides the possibility to get a common super type for two given
 * <code>DataCell</code> types. In case they are not compatible to each other, 
 * then the returned <code>DataType</code> is calculated based on the 
 * intersection of both compatible <code>DataValue</code> lists.
 * 
 * - non-native with cell classes
 * - missing cell
 * 
 * <p>
 * On details of data types in KNIME see the <a
 * href="package-summary.html">package description</a> and the <a
 * href="doc-files/newtypes.html">
 * manual on how to define customized data types</a>.
 * 
 * @see org.knime.core.data.DataCell
 * @see org.knime.core.data.DataValue
 * @author Bernd Wiswedel, University of Konstanz
 */
public final class DataType {

    /**
     * Implementation of the missing cell. This DataCell does not implement
     * any DataValue interfaces but is compatible to any value class. 
     * This class is the very only implementation whose isMissing() method 
     * returns true. 
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
         * 
         * @see org.knime.core.data.DataCell#equalsDataCell(
         * org.knime.core.data.DataCell)
         */
        @Override
        public boolean equalsDataCell(final DataCell dc) {
            // guaranteed not to be called on and with a missing cell.....
            return false;
        }

        /**
         * 
         * @see org.knime.core.data.DataCell#hashCode()
         */
        @Override
        public int hashCode() {
            return toString().hashCode();
        }

        /**
         * 
         * @see org.knime.core.data.DataCell#isMissingInternal()
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
         * 
         * @see org.knime.core.data.DataCell#toString()
         */
        @Override
        public String toString() {
            return "?";
        }
    }
    
    /** Config key for the cell class of native types. */
    private static final String CFG_CELL_NAME = "cell_class";
    /** Config key for the preferred value class flag. */
    private static final String CFG_HAS_PREF_VALUE = "has_pref_value_class";
    /** Config key for the value classes. */
    private static final String CFG_VALUE_CLASSES = "value_classes";

    /** Map of the referring <code>DataCell</code> serializer. */
    private static final Map<Class<? extends DataCell>, DataCellSerializer>
        CLASS_TO_SERIALIZER_MAP =
            new HashMap<Class<? extends DataCell>, DataCellSerializer>();
    
    
    /** 
     * Map containing all DataCell.class-DataType tuples (essentially this
     * stores for each DataCell implementation which DataValue interfaces
     * it implements). Whenever getType(Class &lt;? extends DataCell&gt;)
     * is called and this map does not contain the corresponding DataType,
     * the type will be created (using one of the DataType constructors) and
     * added to this map. This map makes sure that the getType() method
     * is fast and that there will be no duplicate DataType instances for 
     * different instances of the DataCell implementation.
     */
    private static final Map<Class<? extends DataCell>, DataType> 
        CLASS_TO_TYPE_MAP = new HashMap<Class<? extends DataCell>, DataType>();
    
    /**
     * The String representation comparator. Fall back comparator if no other is
     * available.
     */
    private static final DataValueComparator FALLBACK_COMP = 
        new DataValueComparator() {
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
     * <code>DataValue</code> interface. 
     */
    private static final Map<Class<? extends DataValue>, UtilityFactory> 
        VALUE_CLASS_TO_UTILITY = 
            new HashMap<Class<? extends DataValue>, UtilityFactory>();
    
    /**
     * Recursive method that walks up the inheritance tree of a given class and 
     * determines all DataValue interfaces being implemented. Any such detected 
     * interface will be added to the passed set. Used from the constructor to 
     * determine implemented interfaces of a DataCell class. 
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
     * Clones the given DataType but changes its preferred value class to the 
     * passed preferred value. The returned non-native <code>DataType</code> 
     * may or may not be equal to the <code>from</code> DataType depending on 
     * the preferred value class of <code>from</code> and will be created newly.
     * 
     * @param from the DataType to clone
     * @param preferred the new preferred value class
     * @return a cloned new DataType with the given preferred value class
     * @throws IllegalArgumentException 
     *  if <code>from.isCompatible(preferred)</code> returns <code>false</code>
     * @throws NullPointerException If any argument is <code>null</code>
     */
    public static DataType cloneChangePreferredValue(final DataType from,
            final Class<? extends DataValue> preferred) {
        return new DataType(from, preferred);
    }
    
    /**
     * Returns a DataCellSerializer for the runtime class of a DataCell or 
     * <code>null</code> if the passed class of type DataCell does not implement
     * <pre>
     *   public static &lt;T extends DataCell&gt; 
     *       DataCellSerializer&lt;T&gt; getCellSerializer(
     *       final Class&lt;T&gt; cl) {
     * </pre>
     * The DataCellSerializer is defined
     * through a static access method in DataCell. If no such method exists or
     * the method can't be invoked (using reflection), this method returns 
     * <code>null</code> and ordinary java serialization is used for
     * storing/loading the cell. See also the class documentation of 
     * {@link DataCell} for more information on the static access method. 
     * @param <T> the runtime class of the DataCell
     * @param cl the datacell's class
     * @return the DataCellSerializer defined in the DataCell implementation
     * or <code>null</code> if no such serializer is available.
     * @throws NullPointerException If the argument is <code>null</code>.
     */
    @SuppressWarnings("unchecked") // access to CLASS_TO_SERIALIZER_MAP
    public static <T extends DataCell> 
        DataCellSerializer<T> getCellSerializer(final Class<T> cl) {
        if (cl == null) {
            throw new NullPointerException("Class argument must not be null.");
        }
        if (CLASS_TO_SERIALIZER_MAP.containsKey(cl)) {
            return CLASS_TO_SERIALIZER_MAP.get(cl);
        }
        DataCellSerializer<T> result = null;
        Exception exception = null;
        try {
            Method method = cl.getMethod("getCellSerializer");
            Class rType = method.getReturnType();
            /* The following test realizes
             * DataCellSerializer<T>.class.isAssignableFrom(rType).
             * Unfortunately one can't check the generic(!) return type as
             * above since the type information is lost at compile time.
             * We have to make sure here that the runtime class of the return
             * value matches the class information of the DataCell class as
             * DataCells may potentially be overwritten and we do not accept
             * the implementation of the superclass as we lose information
             * of the more specialized class when we use the superclass' 
             * serializer.
             */ 
            boolean isAssignable =
                DataCellSerializer.class.isAssignableFrom(rType);
            boolean hasRType = false;
            if (isAssignable) {
                Type genType = method.getGenericReturnType();
                hasRType = isCellSerializer(rType, cl) 
                    || isCellSerializer(genType, cl);
                if (!hasRType) {
                    Type[] ins = rType.getGenericInterfaces();
                    for (int i = 0; (i < ins.length) && !hasRType; i++) {
                        hasRType = isCellSerializer(ins[i], cl);
                    }
                }
            }
            if (!hasRType) {
                LOGGER.coding("Class \"" + cl.getSimpleName() + "\" defines " 
                        + "method \"getCellSerializer\" but the method has " 
                        + "the wrong return type (\"" 
                        + method.getGenericReturnType() + "\", expected \"" 
                        + DataCellSerializer.class.getName() + "<" 
                        + cl.getName() + ">\"); using serialization instead.");
            } else {
                Object typeObject = method.invoke(null);
                result = (DataCellSerializer<T>)typeObject;
            }
        } catch (NoSuchMethodException nsme) {
            LOGGER.debug("Class \"" + cl.getSimpleName()
                    + "\" does not define method \"getCellSerializer\", using " 
                    + "ordinary (but slow) java serialization.");
        } catch (InvocationTargetException ite) {
            exception = ite;
        } catch (NullPointerException npe) {
            exception = npe;
        } catch (IllegalAccessException iae) {
            exception = iae;
        } catch (ClassCastException cce) {
            exception = cce;
        }
        if (exception != null) {
            LOGGER.coding("Class \"" + cl.getSimpleName()
                    + "\" defines method \"getCellSerializer\" but there was a "
                    + "problem invoking it", exception);
            result = null;
        }
        CLASS_TO_SERIALIZER_MAP.put(cl, result);
        return result;
    }

    /**
     * Returns a type which is compatible to only those values, both given types
     * are compatible to, i.e. it contains the intersection of both value lists.
     * This super type can be safely asked for a comparator for cells of both
     * specified types, or a renderer for DataCells of any of the given types.
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
     * <code>DataType</code> has no preferred value class, an empty list 
     * of compatible value classes, and the cell class is <code>null</code>.   
     * 
     * @param type1 Type 1.
     * @param type2 Type 2.
     * @return A type compatible to types both arguments are compatible to.
     * @throws NullPointerException If one of the given types is 
     *          <code>null</code>.
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
     * A cell representing a missing data cell with no value. The type
     * of the returned data cell will be compatible to any DataValue interface
     * (although you can't cast the returned cell to the value) and will have
     * default icons, renderers, and comparators.
     * @return Singleton of a missing cell.
     */
    public static DataCell getMissingCell() {
        return MissingCell.INSTANCE;
    }

    /** 
     * Internal access method to determine the preferred DataValue class to
     * a given DataCell implementation. This method tries to invoke a static 
     * method on the argument with the following signature
     * <pre>
     *   public static Class<? extends DataValue> getPreferredValueClass()
     * </pre>
     * If no such method exists, this method returns null. If it 
     * exists but has the wrong scope or return value, a warning message is 
     * logged and null is returned. 
     * @param cl the runtime class of the DataCell
     * @return its preferred value interface or <code>null</code>
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
     * Returns the runtime <code>DataType</code> for a <code>DataCell</code> 
     * implementation. If no such <code>DataType</code> has been created 
     * before (i.e. this method is called the first time with the current
     * argument), the return value is created and hashed. The returned 
     * <code>DataType</code> will claim to be compatible to all 
     * <code>DataValue</code> classes the cell is implementing (i.e. what 
     * <code>DataValue</code> interfaces are implemented by 
     * <code>cl</code>) and will bundle meta information of the cell such as 
     * renderer, icon, and comparators.
     *  
     * <p>This method is the only way to determine the <code>DataType</code>
     * of a <code>DataCell</code>. However, most standard cell implementations
     * have a static member for convenience access, e.g. 
     * {@link org.knime.core.data.def.DoubleCell#TYPE DoubleCell.TYPE}.
     * 
     * @see DataCell
     * @see DataValue
     * @see DataCell#getType()
     * 
     * @param cl the runtime class of DataCell for which the DataType 
     *         is requested
     * @return the corresponding type <code>cl</code>, never <code>null</code>
     * @throws NullPointerException if the argument is <code>null</code>
     */
    public static DataType getType(final Class<? extends DataCell> cl) {
        if (cl == null) {
            throw new NullPointerException("Class must not be null.");
        }
        DataType result = CLASS_TO_TYPE_MAP.get(cl);
        if (result == null) {
            result = new DataType(cl);
            CLASS_TO_TYPE_MAP.put(cl, result);
        }
        return result;
    }
    
    /* bis hierhin ge-reviewt */
    
    /** 
     * Determines the <code>UtilityFactory</code> to a given 
     * <code>DataValue</code> implementation. 
     * This method tries to access a static field in the DataValue class: 
     * <pre>
     *   public static final UtilityFactory UTILITY;
     * </pre>
     * If no such field exists, this method returns the factory of one of the 
     * super interfaces. If no UTILITY member can be found, finally the 
     * DataValue class returns a correct implementation. If it exists but has 
     * the wrong access scope or if it is not static, a warning message is 
     * logged and the member of one of the super interface is returned.
     * 
     * @param cl The runtime class of the DataCell.
     * @return The utility factory given in the cell implementation.
     * @throws NullPointerException If the argument or the found UTILITY member 
     *         is <code>null</code>.
     */
    public static UtilityFactory getUtilityFor(
        final Class<? extends DataValue> cl) {
        if (cl == null) {
            throw new NullPointerException("Class argument must not be null.");
        }
        UtilityFactory result = VALUE_CLASS_TO_UTILITY.get(cl);
        if (result == null) {
            Exception exception = null;
            try {
                // TODO: use super interface if the current field has
                // wrong modifiers or has wrong class.
                Field typeField = cl.getField("UTILITY");
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
                LOGGER.coding("DataValue interface \"" + cl.getSimpleName() 
                        + "\" seems to have a problem with the static field " 
                        + "\"UTILITY\"", exception);
                // fall back - no meta information available
                result = DataValue.UTILITY;
            }
            VALUE_CLASS_TO_UTILITY.put(cl, result);
        }
        return result;
    }
    
    /**
     * Helper method that checks if the passed Type is a parameterized
     * type (like DataCellSerializer&lt;someType&gt; and that it is assignable
     * from the given cell class. This method is used to check if the return
     * class of getCellSerializer in a DataCell has the correct signature.
     */
    private static boolean isCellSerializer(
            final Type c, final Class<? extends DataCell> cellClass) {
        boolean b = c instanceof ParameterizedType;
        if (b) {
            ParameterizedType parType = (ParameterizedType)c;
            Type[] args = parType.getActualTypeArguments();
            b = b && (args.length >= 1);
            b = b && (args[0] instanceof Class);
            b = b && cellClass.isAssignableFrom((Class)args[0]);
        }
        return b;
    }
    
    /**
     * Loads <code>DataType</code> from a given <code>Config</code>.
     * @param config Load <code>DataType</code> from.
     * @return A <code>DataType</code> which fits the given <code>Config</code>.
     * @throws InvalidSettingsException If the <code>DataType</code> could not
     *         be loaded from the given <code>Config</code>.
     */
    @SuppressWarnings("unchecked") // type casts
    public static DataType load(final ConfigRO config) 
            throws InvalidSettingsException {
        String cellClassName = config.getString(CFG_CELL_NAME);
        if (cellClassName != null) {
            try {
                Class<? extends DataCell> cellClass = 
                    (Class<? extends DataCell>)
                    GlobalClassCreator.createClass(cellClassName);
                return getType(cellClass);
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
            return new DataType(hasPrefValueClass, valueClasses);
        } catch (IllegalArgumentException iae) {
            throw new InvalidSettingsException(iae);
        }
    }
    
    /** Cell class, used, e.g. for toString() method. */
    private final Class<? extends DataCell> m_cellClass;
    
    /** Flag if the first element in m_valueClasses contains the 
     * preferred value class. */
    private final boolean m_hasPreferredValueClass;
    
    /** List of all implemented DataValue interfaces. */
    private final List<Class<? extends DataValue>> m_valueClasses;
    
    /**
     * Creates a new, non-native <code>DataType</code>. This method is used 
     * from the <code>load(Config)</code> method.
     * @param hasPreferredValue If preferred value is set.
     * @param valueClasses All implemented <code>DataValue</code> interfaces.
     * @throws IllegalArgumentException If the cell class does not implement
     *         all given value class interfaces.
     */
    private DataType(final boolean hasPreferredValue, 
            final List<Class<? extends DataValue>> valueClasses) {
        m_cellClass = null;
        m_hasPreferredValueClass = hasPreferredValue;
        m_valueClasses = valueClasses;
    }

    /**
     * Creates a new type for the passed <code>DataCell</code> class. This 
     * implementation determines all DataValue interfaces that the cell is 
     * implementing and also retrieves their meta information. This constructor 
     * is used by the static getType() method.
     */
    private DataType(final Class<? extends DataCell> cl) {
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
    }
    
    /** 
     * Constructor that is used when the preferred value class should change
     * in the given DataType. This DataType is typically never assigned
     * to one particular DataCell class (otherwise the constructor
     * DataType(Class) would have been used) and therefore this type is not
     * cached. This means, the resulting type is not native, i.e. the cell class
     * is null.
     * 
     * @param type The type to clone.
     * @param preferred The new preferred value class of the new type.
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
        m_cellClass = null;
    }

    /**
     * Types are equal if the set of compatible value classes matches (ordering
     * does not matter) and both types have an preferred value class and the 
     * class is the same or both do not have a preferred value class. 
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
     * A comparator for data cell objects of this type. Will return the native
     * comparator (if provided). If no comparator is available the
     * comparator of the string representations will be used.
     * @return A comparator for cells of this type.
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
     * Get an icon representing this type. This is used in table headers and
     * lists, for instance.
     * 
     * @return An icon for this type.
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

    /** Get the preferred value class of the current DataType or 
     * <code>null</code> if not available. The preferred value class is
     * defined through the DataCell implementation. This method returns
     * <code>null</code> if either the cell implementation lacks the 
     * information or this DataType has been created as an intersection of
     * two types whose preferred value classes do not match.
     * @return The preferred value class or <code>null</code>.  
     */ 
    public Class<? extends DataValue> getPreferredValueClass() {
        if (m_hasPreferredValueClass) {
            return m_valueClasses.get(0);
        }
        return null;
    }

    /**
     * Returns the set of all renderers that are available for this DataType.
     * The returned DataValueRendererFamily will contain all renderers that are
     * supported or available through the compatible {@link DataValue} 
     * interfaces. If no renderer was declared by the DataValue interfaces, 
     * this method will make sure that at least a default renderer (using the 
     * {@link DataCell#toString() DataCell's toString()} method) is returned. 
     * 
     * @param spec The column spec to the column for which the renderer will be
     *            used. Most of the renderer implementations won't need column
     *            domain information but some do. For instance a class that
     *            renders the double value in the column according to the
     *            min/max values in the column domain.
     * @return All renderers that are available for this DataType.
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
     * Get a copy of all <code>DataValue</code>s to which this type is
     * compatible to. The returned List is non-modifiable, subsequent changes to
     * the list will fail with an exception. The list does not contain 
     * duplicates.
     * @return A non-modifiable list of compatible types.
     */
    public List<Class<? extends DataValue>> getValueClasses() {
        return Collections.unmodifiableList(m_valueClasses);
    }

    /**
     * @see java.lang.Object#hashCode()
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
     * @throws NullPointerException If the type argument is <code>null</code>.
     */
    public boolean isASuperTypeOf(final DataType type) {
        if (type == null) {
            throw new NullPointerException("Type argument must not be null.");
        }
        if (type == this) {
            // we get out of here easy
            return true;
        }
        boolean result = true;
        for (Class<? extends DataValue> cl : m_valueClasses) {
            if (!type.isCompatible(cl)) {
                result = false;
                break;
            }
        }
        return result;
    }

    /**
     * Checks if the given <code>DataValue.class</code> is compatible to this
     * type. If it returns <code>true</code> the {@link DataCell}s of this 
     * type can be type-casted to the <code>valueClass</code>. This 
     * method returns <code>false</code> if the argument is <code>null</code>.
     * 
     * @param valueClass Class to check compatibility for.
     * @return <code>true</code> if compatible.
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
     * Save this <code>DataType</code> to the given <code>Config</code>. Writes
     * all interval members, cell class, if preferred value, and a list of 
     * cell classes.
     * @param config Write to this <code>Config</code>.
     */
    public void save(final ConfigWO config) {
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
     * Returns the simple name of the cell class (if any) or "Non-native" +
     * the abbreviations of all compatible values classes.
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        if (m_cellClass != null) {
            return m_cellClass.getSimpleName(); 
        }
        String valuesToString = Arrays.toString(m_valueClasses.toArray());
        return "Non-Native " + valuesToString;
    }
}
