/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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
 *   02.02.2006 (mb): created
 *   25.10.2006 (tg): cleanup
 *   31.10.2006 (tm, cs): reviewed
 */
package org.knime.core.data;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.IntPredicate;

import org.apache.commons.lang3.ArrayUtils;
import org.knime.core.data.meta.DataColumnMetaData;
import org.knime.core.data.property.ColorHandler;
import org.knime.core.data.property.ShapeHandler;
import org.knime.core.data.property.SizeHandler;
import org.knime.core.data.property.ValueFormatHandler;
import org.knime.core.data.property.filter.FilterHandler;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.util.CheckUtils;

/**
 * A factory class to create a {@link DataColumnSpec} (as the only way from
 * outside this package). It can be created from an existing spec or by
 * specifying a column name and {@link DataType}. Setter functions allow to
 * overwrite all available members within the creator but the (later) created
 * {@link DataColumnSpec} will be read-only after creation.
 *
 * <p>
 * In addition, a {@link ColorHandler}, {@link SizeHandler}, and/or
 * {@link ShapeHandler} can be set optionally to specify color, shape, and size.
 * An {@link DataColumnProperties} object can be used to specify annotations as
 * key-value pairs.
 *
 * @see DataColumnSpec
 * @see #createSpec()
 *
 * @author Michael Berthold, University of Konstanz
 */
public final class DataColumnSpecCreator {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(DataColumnSpecCreator.class);

    /** Keeps the column name. */
    private String m_name;

    /** Keeps the column type. */
    private DataType m_type;

    /** Keeps the column domain with additional meta-info. */
    private DataColumnDomain m_domain;

    /** Contains column annotations, cannot be null. */
    private DataColumnProperties m_properties;

    /** Holds the SizeHandler if one was set or null. */
    private SizeHandler m_sizeHandler = null;

    /** Holds the ShapeHandler if one was set or null. */
    private ShapeHandler m_shapeHandler = null;

    /** Holds the FilterHandler if one was set or null. */
    private FilterHandler m_filterHandler = null;

    /** Holds the ColorHandler if one was set or null. */
    private ColorHandler m_colorHandler = null;

    /** Holds the ColorHandler if one was set or null. */
    private ValueFormatHandler m_valueFormatHandler = null;

    /** Holds the names array (by default and array containing column name),
     * something different for array types or BitVector type.
     */
    private String[] m_elementNames;

    /**
     * Creator for the object managing meta data.
     */
    private DataColumnMetaDataManager.Creator m_metaDataCreator;

    /**
     * Counter that is used when the setName() method is called with an
     * empty string. It will create an artificial name with a guaranteed
     * unique index.
     */
    private static int emptyColumnCount = 0;

    /**
     * Initializes the creator with the given column name and type. The
     * <code>DataColumnProperties</code> are left empty and color, size, and
     * shape handler are set to <code>null</code>.
     *
     * @param name the column name
     * @param type the column type
     * @throws NullPointerException if either the column name or type is
     *             <code>null</code>
     */
    public DataColumnSpecCreator(final String name, final DataType type) {
        setName(name);
        setType(type);
        setDomain(new DataColumnDomain(null, null, null));
        setProperties(new DataColumnProperties());
        m_metaDataCreator = new DataColumnMetaDataManager.Creator();
    }

    /**
     * Initializes the creator with a given {@link DataColumnSpec}.
     *
     * @param cspec other spec
     */
    public DataColumnSpecCreator(final DataColumnSpec cspec) {
        m_name = cspec.getName();
        assert m_name != null : "Column name must not be null!";
        List<String> elNames = cspec.getElementNames();
        m_elementNames = elNames.toArray(new String[elNames.size()]);
        m_type = cspec.getType();
        assert m_type != null : " Column type must not be null!";
        m_domain = cspec.getDomain();
        assert m_domain != null : "domain must not be null!";
        // get the immutable properties
        m_properties = cspec.getProperties();
        assert m_properties != null : "properties must not be null!";
        // property size
        m_sizeHandler = cspec.getSizeHandler();
        // property shape
        m_shapeHandler = cspec.getShapeHandler();
        // property color
        m_colorHandler = cspec.getColorHandler();
        // property filter
        m_filterHandler = cspec.getFilterHandler().orElse(null);
        // value formatter
        m_valueFormatHandler = cspec.getValueFormatHandler();

        m_metaDataCreator = new DataColumnMetaDataManager.Creator(cspec.getMetaDataManager());
    }

    private void updateType(final DataType other) {
        if (m_type.equals(other)) {
            return; // same type -> no update necessary
        }
        // the types differ so we have to use a common super type instead
        final DataType common = DataType.getCommonSuperType(m_type, other);
        assert common.isASuperTypeOf(m_type);
        assert common.isASuperTypeOf(other);
        m_type = common;
    }

    /**
     * Options for the {@link DataColumnSpecCreator#merge(DataColumnSpec, Set)} method.
     *
     * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
     * @since 4.1
     */
    public enum MergeOptions {
            /**
             * Allow the data type of the merged columns to differ. In this case the {@link DataColumnSpecCreator} will
             * have the common super-type returned by {@link DataType#getCommonSuperType(DataType, DataType)} after the
             * merge is completed.
             */
            ALLOW_VARYING_TYPES,
            /**
             * Allow the element names of the merged columns to differ. If this option is used, the element names of the
             * second column are dropped.
             */
            ALLOW_VARYING_ELEMENT_NAMES;
    }

    /**
     * Merges the existing {@link DataColumnSpec} with a second
     * {@link DataColumnSpec} using the provided {@link MergeOptions options}.
     * The domain information, meta data and properties from both DataColumnSpecs are merged,
     * Color, Shape and Size-Handlers are compared (must be equal).
     *
     * @param cspec2 the second {@link DataColumnSpec}.
     * @param options the {@link MergeOptions} for merging
     *
     * @see DataTableSpec#mergeDataTableSpecs(DataTableSpec...)
     * @throws IllegalArgumentException if the structure (name and depending on <b>allowDifferentTypes</b> type) does
     *             not match, if the domain or meta data cannot be merged, if the Color-,
     *             Shape- or SizeHandlers are different or the sub element
     *             names are not equal.
     *
     * @since 4.1
     */
    public void merge(final DataColumnSpec cspec2, final Set<MergeOptions> options) {
        final boolean allowVaryingTypes = options.contains(MergeOptions.ALLOW_VARYING_TYPES);
        CheckUtils.checkArgument(isCompatible(cspec2, allowVaryingTypes),
            "Structures of DataColumnSpecs do not match.");
        if (allowVaryingTypes) {
            updateType(cspec2.getType());
        }
        mergeDomains(cspec2.getDomain());
        m_metaDataCreator.merge(cspec2.getMetaDataManager());
        mergeColorHandlers(cspec2.getColorHandler());
        mergeShapeHandlers(cspec2.getShapeHandler());
        mergeSizeHandlers(cspec2.getSizeHandler());
        mergeValueFormatHandlers(cspec2.getValueFormatHandler());
        mergeFilterHandlers(cspec2.getFilterHandler().orElse(null));
        mergeProperties(cspec2.getProperties());
        mergeElementNames(cspec2.getElementNames(), options.contains(MergeOptions.ALLOW_VARYING_ELEMENT_NAMES));
    }

    private void mergeElementNames(final List<String> elementNames, final boolean allowVaryingElementNames) {
        String[] elNames2Array = elementNames.toArray(new String[elementNames.size()]);
        String[] elNamesArray =
            m_elementNames == null ? new String[]{m_name} : m_elementNames;
        if (!Arrays.deepEquals(elNamesArray, elNames2Array) && !allowVaryingElementNames) {
            throw new IllegalArgumentException("Element names are not equal");
        }
    }

    /**
     * Takes the intersection of the properties
     */
    private void mergeProperties(final DataColumnProperties prop2) {
        Map<String, String> mergedProps = new HashMap<>();
        Enumeration<String> e = m_properties.properties();
        while (e.hasMoreElements()) {
            String key = e.nextElement();
            String value = m_properties.getProperty(key);
            if (prop2.getProperty(key) != null
                    && prop2.getProperty(key).equals(value)) {
                mergedProps.put(key, value);
            }
        }

        if (mergedProps.size() != m_properties.size()) {
            setProperties(new DataColumnProperties(mergedProps));
        }
    }

    private void mergeFilterHandlers(final FilterHandler filterHandler) {
        if (!Objects.equals(m_filterHandler, filterHandler)) {
            LOGGER.warn("Column has already a filter handler attached, ignoring new handler.");
        }
    }

    private void mergeSizeHandlers(final SizeHandler sizeHandler2) {
        if (!Objects.equals(m_sizeHandler, sizeHandler2)) {
            LOGGER.warn("Column has already a size handler attached, ignoring new handler.");
        }
    }

    private void mergeShapeHandlers(final ShapeHandler shapeHandler2) {
        if (!Objects.equals(m_shapeHandler, shapeHandler2)) {
            LOGGER.warn("Column has already a shape handler attached, ignoring new handler.");
        }
    }

    private void mergeColorHandlers(final ColorHandler colorHandler2) {
        if (!Objects.equals(m_colorHandler, colorHandler2)) {
            LOGGER.warn("Column has already a color handler attached, ignoring new handler.");
        }
    }

    private void mergeValueFormatHandlers(final ValueFormatHandler valueFormatHandler2) {
        if (!Objects.equals(m_valueFormatHandler, valueFormatHandler2)) {
            LOGGER.warn("Column already has a value format handler attached, ignoring new handler.");
        }
    }

    private Set<DataCell> mergePossibleValues(final Set<DataCell> oValues) {
        final Set<DataCell> myValues = m_domain.getValues();
        Set<DataCell> newValues;
        if (myValues == null || oValues == null) {
            newValues = null;
        } else if (myValues.equals(oValues)) {
            newValues = myValues;
        } else {
            newValues = new LinkedHashSet<>(myValues);
            newValues.addAll(oValues);
        }
        return newValues;
    }

    private static DataCell mergeBound(final DataCell myBound, final DataCell otherBound,
        final DataValueComparator comparator, final IntPredicate takeOther) {
        if (myBound == null || otherBound == null) {
            return null;
        } else if (myBound.equals(otherBound)) {
            return myBound;
        } else if (takeOther.test(comparator.compare(myBound, otherBound))) {
            return otherBound;
        } else {
            return myBound;
        }
    }

    private void mergeDomains(final DataColumnDomain domain2) {
        boolean hasDomainChanged = false;
        final Set<DataCell> newValues = mergePossibleValues(domain2.getValues());
        // != is safe because we return m_domain.getValues() if the other domain has the same values
        hasDomainChanged |= newValues != m_domain.getValues();

        DataValueComparator comparator = m_type.getComparator();

        final DataCell myLower = m_domain.getLowerBound();
        final DataCell newLower = mergeBound(myLower, domain2.getLowerBound(), comparator, i -> i > 0);
        hasDomainChanged |= newLower != myLower;

        final DataCell myUpper = m_domain.getUpperBound();
        DataCell newUpper = mergeBound(myUpper, domain2.getUpperBound(), comparator, i1 -> i1 < 0);
        hasDomainChanged |= newUpper != myUpper;
        if (hasDomainChanged) {
            setDomain(new DataColumnDomain(newLower, newUpper, newValues));
        }
    }

    /**
     * The spec is compatible if it has the same name and depending on whether the type should
     * be enforced, the same type.
     */
    private boolean isCompatible(final DataColumnSpec cspec2, final boolean allowDifferentType) {
        return cspec2.getName().equals(m_name) && (allowDifferentType || cspec2.getType().equals(m_type));
    }

    /**
     * Merges the existing {@link DataColumnSpec} with a second
     * {@link DataColumnSpec}. If they have equal structure, the domain
     * information and properties from both DataColumnSpecs is merged,
     * Color, Shape and Size-Handlers are compared (must be equal).
     *
     * @param cspec2 the second {@link DataColumnSpec}.
     *
     * @see DataTableSpec#mergeDataTableSpecs(DataTableSpec...)
     * @throws IllegalArgumentException if the structure (type and name) does
     *             not match, if the domain cannot be merged, if the Color-,
     *             Shape- or SizeHandlers are different or the sub element
     *             names are not equal.
     */
    public void merge(final DataColumnSpec cspec2) {
        merge(cspec2, EnumSet.noneOf(MergeOptions.class));
    }

    /**
     * Set (new) column name. If the column name is empty or consists only of
     * whitespaces, a warning is logged and an artificial name is created.
     *
     * @param name the (new) column name
     * @throws NullPointerException if the column name is <code>null</code>
     */
    public void setName(final String name) {
        if (name == null) {
            throw new NullPointerException("Name of DataColumnSpec must not"
                    + " be null!");
        }
        String validName = name.trim();
        if (validName.length() == 0) {
            validName = "<empty_" + (++emptyColumnCount) + ">";
            LOGGER.warn("Column name \"" + name + "\" is invalid, "
                    + "replacing by \"" + validName + "\"");
        }
        m_name = validName;
    }

    /**
     * Set names of elements when this column contains a vector type. The default value is an empty array as
     * per {@link DataColumnSpec#getElementNames()}. If this method is call with argument <code>null</code>, an
     * empty array will be passed on to the {@link DataColumnSpec} constructor.
     * @param elNames The elements names/identifiers to set.
     * @throws NullPointerException If the argument contains <code>null</code> elements.
     * @see DataColumnSpec#getElementNames()
     */
    public void setElementNames(final String[] elNames) {
        if (elNames == null) {
            m_elementNames = null;
        } else {
            if (Arrays.asList(elNames).contains(null)) {
                throw new NullPointerException(
                        "Argument array contains null elements");
            }
            m_elementNames = new String[elNames.length];
            System.arraycopy(elNames, 0, m_elementNames, 0, elNames.length);
        }
    }

    /**
     * Set (new) column type.
     *
     * @param type the (new) column type
     * @throws NullPointerException if the column type is <code>null</code>
     */
    public void setType(final DataType type) {
        if (type == null) {
            throw new NullPointerException("Type of DataColumnSpec must not"
                    + " be null!");
        }
        m_type = type;
    }

    /**
     * Set (new) domain. If a <code>null</code> domain is set, an empty domain
     * will be created.
     *
     * @param domain the (new) domain, if <code>null</code> an empty default
     *            domain will be created
     */
    public void setDomain(final DataColumnDomain domain) {
        if (domain == null) {
            m_domain = new DataColumnDomain(null, null, null);
        } else {
            m_domain = domain;
        }
    }

    /**
     * Set (new) column properties. If a <code>null</code> properties object
     * is passed, a new empty property object will be created.
     *
     * @param props the (new) properties, if <code>null</code> an empty props
     *            object is created
     */
    public void setProperties(final DataColumnProperties props) {
        if (props == null) {
            m_properties = new DataColumnProperties();
        } else {
            m_properties = props;
        }
    }

    /**
     * Set (new) <code>SizeHandler</code> which can be <code>null</code>.
     *
     * @param sizeHdl the (new) <code>SizeHandler</code> or <code>null</code>
     */
    public void setSizeHandler(final SizeHandler sizeHdl) {
        m_sizeHandler = sizeHdl;
    }

    /**
     * Set (new) <code>ShapeHandler</code> which can be <code>null</code>.
     *
     * @param shapeHdl the (new) <code>ShapeHandler</code> or
     *            <code>null</code>
     */
    public void setShapeHandler(final ShapeHandler shapeHdl) {
        m_shapeHandler = shapeHdl;
    }

    /**
     * Set (new) <code>FilterHandler</code> which can be <code>null</code>.
     *
     * @param filterHdl the (new) <code>FilterHandler</code> or <code>null</code>
     * @since 3.3
     */
    public void setFilterHandler(final FilterHandler filterHdl) {
        m_filterHandler = filterHdl;
    }

    /**
     * Set (new) <code>ColorHandler</code> which can be <code>null</code>.
     *
     * @param colorHdl the (new) <code>ColorHandler</code> or
     *            <code>null</code>
     */
    public void setColorHandler(final ColorHandler colorHdl) {
        m_colorHandler = colorHdl;
    }

    /**
     * Set {@link ValueFormatHandler} which can be <code>null</code>.
     *
     * @param valueFormatHandler nullable
     * @since 5.1
     */
    public void setValueFormatHandler(final ValueFormatHandler valueFormatHandler) {
        m_valueFormatHandler = valueFormatHandler;
    }

    /**
     * Adds the provided {@link DataColumnMetaData metaData} by either overwriting existing meta data for the
     * associated DataValue (<b>overwrite</b> set to true)
     * or merging it with existing meta data (overwrite set to false).
     *
     * @param metaData the {@link DataColumnMetaData} to add
     * @param overwrite if set to true, any stored meta data for the type {@link DataColumnMetaData metaData}
     * refers to is ovewritten, otherwise the meta data is merged
     * (potentially leading to an exception if the merge fails)
     * @since 4.1
     */
    public void addMetaData(final DataColumnMetaData metaData, final boolean overwrite) {
        m_metaDataCreator.addMetaData(metaData, overwrite);
    }

    /**
     * Removes {@link DataColumnMetaData} of class <b>metaDataClass</b>.
     *
     * @param metaDataClass the class of the {@link DataColumnMetaData} that should be removed
     * @since 4.1
     */
    public void removeMetaData(final Class<? extends DataColumnMetaData> metaDataClass) {
        m_metaDataCreator.remove(metaDataClass);
    }

    /**
     * Drops all {@link DataColumnMetaData} stored in the column creator.
     *
     * @since 4.1
     */
    public void removeAllMetaData() {
        m_metaDataCreator.clear();
    }

    /**
     * Returns the {@link DataType} of this {@link DataColumnSpecCreator}.
     *
     * @return the {@link DataType}
     * @since 4.1
     */
    public DataType getType() {
        return m_type;
    }

    /**
     * Removes all handlers from this creator which are then set to
     * <code>null</code> for the next call of <code>#createSpec()</code>.
     */
    public void removeAllHandlers() {
        this.setSizeHandler(null);
        this.setColorHandler(null);
        this.setShapeHandler(null);
        this.setFilterHandler(null);
    }

    /**
     * Creates and returns a new <code>DataColumnSpec</code> using the
     * internal properties of this creator.
     *
     * @return newly created <code>DataColumnSpec</code>
     */
    public DataColumnSpec createSpec() {
        String[] elNames = Objects.requireNonNullElse(m_elementNames, ArrayUtils.EMPTY_STRING_ARRAY);
        return new DataColumnSpec(m_name, elNames, m_type, m_domain, m_properties, m_sizeHandler, m_colorHandler,
            m_shapeHandler, m_valueFormatHandler, m_filterHandler, m_metaDataCreator.create());
    }
}
