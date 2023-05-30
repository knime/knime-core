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
 * ------------------------------------------------------------------------
 */
package org.knime.core.data;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.knime.core.data.meta.DataColumnMetaData;
import org.knime.core.data.property.ColorHandler;
import org.knime.core.data.property.ShapeHandler;
import org.knime.core.data.property.SizeHandler;
import org.knime.core.data.property.filter.FilterHandler;
import org.knime.core.data.property.format.ValueFormatHandler;
import org.knime.core.data.property.format.ValueFormatModel;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.config.ConfigRO;
import org.knime.core.node.config.ConfigWO;
import org.knime.core.node.util.CheckUtils;

/**
 * A <code>DataColumnSpec</code> describes one column in a
 * {@link org.knime.core.data.DataTable}.
 * It contains information about type, name, domain, data properties, and
 * optionally color/size/shape handling. This class can only be created using
 * the {@link org.knime.core.data.DataColumnSpecCreator} within this package.
 *
 * @see DataColumnSpecCreator
 * @see DataTableSpec
 *
 * @author Michael Berthold, University of Konstanz
 *
 */
public final class DataColumnSpec {

    /** Keeps the column name. */
    private final String m_name;

    /** Names array of sub elements such as bit vector positions or
     * array types. By default contains m_name in an array of length 1.
     * We use a unmodifiable list here, this member is non-null.
     */
    private final List<String> m_elementNames;

    /** Keeps the column type. */
    private final DataType m_type;

    /** Keeps the column domain with additional meta-info. */
    private final DataColumnDomain m_domain;

    /** Contains column annotations. */
    private final DataColumnProperties m_properties;

    /** Holds the SizeHandler if one was set or null. */
    private final SizeHandler m_sizeHandler;

    /** Holds the ShapeHandler if one was set or null. */
    private final ShapeHandler m_shapeHandler;

    /** Holds the ColorHandler if one was set or null. */
    private final ColorHandler m_colorHandler;

    /** An optional format that defines how to display the column's values.*/
    private final ValueFormatHandler m_valueFormatHandler;

    /** Holds the FilterHandler if one was set or null. */
    private final FilterHandler m_filterHandler;

    private final DataColumnMetaDataManager m_metaDataManager;

    /** Config key for the column name. */
    private static final String CFG_COLUMN_NAME = "column_name";

    /** Config key for the element names. */
    private static final String CFG_ELEMENT_NAMES = "element_names";

    /** Config key for the column type. */
    private static final String CFG_COLUMN_TYPE = "column_type";

    /** Config key for the domain information. */
    private static final String CFG_COLUMN_DOMAIN = "column_domain";

    /** Config key for additional annotations. */
    private static final String CFG_COLUMN_PROPS = "column_properties";

    /** Config key for the ColorHandler. */
    private static final String CFG_COLORS = "color_handler";

    /** Config key for the SizeHandler. */
    private static final String CFG_SIZES = "size_handler";

    /** Config key for the ShapeHandler. */
    private static final String CFG_SHAPES = "shape_handler";

    /** Config key for the {@link ValueFormatHandler}. */
    private static final String CFG_VALUE_FORMAT = "value_format_handler";

    /** Config key for the FilterHandler. */
    private static final String CFG_FILTER = "filter_handler";

    /** Config key for the MetaData. */
    private static final String CFG_META_DATA = "meta_data";

    /**
     * Constructor taking all properties of this column spec as arguments. It
     * creates a read-only <code>DataColumnSpec</code> and should only be
     * called from the {@link DataColumnSpecCreator} in this package.
     *
     * @param name the name of the column, must not be <code>null</code>
     * @param elNames Names of sub elements (if any),
     * must not be <code>null</code>, nor contain <code>null</code> elements.
     * @param type the type of the column, must not be <code>null</code>
     * @param domain the domain, must not be <code>null</code>
     * @param props additional properties, must not be <code>null</code>
     * @param sizeHdl the <code>SizeHandler</code> or <code>null</code>
     * @param colorHdl the <code>ColorHandler</code> or <code>null</code>
     * @param shapeHdl the <code>ShapeHandler</code> or <code>null</code>
     * @throws IllegalArgumentException if either column name, type, domain, or properties are <code>null</code>
     */
    DataColumnSpec(final String name, final String[] elNames, final DataType type, final DataColumnDomain domain,
        final DataColumnProperties props, final SizeHandler sizeHdl, final ColorHandler colorHdl,
        final ShapeHandler shapeHdl, final FilterHandler filterHdl, final DataColumnMetaDataManager metaData) {
        this(name, elNames, type, domain, props, sizeHdl, colorHdl, shapeHdl, null, filterHdl, metaData);
    }

    /**
     *
     * @param name the name of the column, must not be <code>null</code>
     * @param elNames Names of sub elements (if any), must not be <code>null</code>, nor contain <code>null</code> elements.
     * @param type the type of the column, must not be <code>null</code>
     * @param domain the domain, must not be <code>null</code>
     * @param props additional properties, must not be <code>null</code>
     * @param sizeHdl the <code>SizeHandler</code> or <code>null</code>
     * @param colorHdl the <code>ColorHandler</code> or <code>null</code>
     * @param shapeHdl the <code>ShapeHandler</code> or <code>null</code>
     * @param formatHandler nullable
     * @param filterHdl nullable
     * @param metaData
     * @throws IllegalArgumentException if either column name, type, domain, or properties are <code>null</code>
     * @since 5.1
     */
    DataColumnSpec(final String name, final String[] elNames, final DataType type, final DataColumnDomain domain,
        final DataColumnProperties props, final SizeHandler sizeHdl, final ColorHandler colorHdl,
        final ShapeHandler shapeHdl, final ValueFormatHandler formatHandler, final FilterHandler filterHdl, final DataColumnMetaDataManager metaData) {

        final String nullError = "Do not init DataColumnSpec with null arguments!";
        List<String> elNamesAsList = Collections.unmodifiableList(
            Arrays.asList(CheckUtils.checkArgumentNotNull(elNames, nullError)));
        CheckUtils.checkArgument(!elNamesAsList.contains(null), "Element names must not contain null elements");

        m_name = CheckUtils.checkArgumentNotNull(name, nullError);
        m_type = CheckUtils.checkArgumentNotNull(type, nullError);
        m_domain = CheckUtils.checkArgumentNotNull(domain, nullError);
        m_properties = CheckUtils.checkArgumentNotNull(props, nullError);
        m_elementNames = elNamesAsList;
        m_sizeHandler = sizeHdl;
        m_colorHandler = colorHdl;
        m_shapeHandler = shapeHdl;
        m_valueFormatHandler = formatHandler;
        m_filterHandler = filterHdl;
        m_metaDataManager = metaData;
    }

    /**
     * Returns the name of this column.
     *
     * @return the column name
     */
    public String getName() {
        return m_name;
    }

    /**
     * Get names of sub elements such as bit vector positions or elements of
     * other vector data types. For non-vector types (most types are non-vector
     * types) this list is typically empty. For vector type columns (i.e. those
     * which contain vectors of <code>DataCell</code> or a
     * <code>BitVectorCell</code>) the elements of this list represent
     * identifiers for each of the different vector positions. There is,
     * however, no need that such a list is set.
     *
     * @return Names of the elements in a unmodifiable, random access list. The
     *         returned value will never be null, nor contain null elements. The
     *         length of the list may vary from 0 to any length.
     */
    public List<String> getElementNames() {
        return m_elementNames;
    }

    /**
     * Returns the column type which is a subclass of {@link DataType}.
     *
     * @return the <code>DataType</code> of this column; all data cells of
     *         this column are type-castable to its native type
     *
     * @see org.knime.core.data.DataType
     * @see org.knime.core.data.DataCell
     */
    public DataType getType() {
        return m_type;
    }

    /**
     * Returns the domain of this column spec including meta-information such as
     * bounds, possible values, etc.
     *
     * @return the domain of the column spec; can be empty, but never
     *         <code>null</code>
     */
    public DataColumnDomain getDomain() {
        return m_domain;
    }

    /**
     * Returns the properties assigned to this column spec. These properties can
     * be seen as some sort of annotations to this column.
     *
     * @return the column's annotation properties, never <code>null</code>
     */
    public DataColumnProperties getProperties() {
        return m_properties;
    }

    /**
     * Returns the <code>SizeHandler</code> defined on this column, if
     * available. Otherwise <code>null</code> will be returned.
     *
     * @return attached <code>SizeHandler</code> or <code>null</code>
     */
    public SizeHandler getSizeHandler() {
        return m_sizeHandler;
    }

    /**
     * Returns the <code>ShapeHandler</code> defined on this column, if
     * available. Otherwise <code>null</code> will be returned.
     *
     * @return attached <code>ShapeHandler</code> or <code>null</code>
     */
    public ShapeHandler getShapeHandler() {
        return m_shapeHandler;
    }

    /**
     * Returns the <code>ColorHandler</code> defined on this column, if
     * available. Otherwise <code>null</code> will be returned.
     *
     * @return attached <code>ColorHandler</code> or <code>null</code>
     */
    public ColorHandler getColorHandler() {
        return m_colorHandler;
    }

    /**
     * @return the wrapper for the {@link ValueFormatModel} if any is defined, otherwise <code>null</code>.
     * @since 5.1
     */
    public ValueFormatHandler getValueFormatHandler() {
        return m_valueFormatHandler;
    }


    /**
     * Returns the <code>FilterHandler</code> defined on this column, if available. (Note, this method was added
     * in KNIME 3.3 and therefore uses the java-8 Optional return type.)
     * @return attached <code>FilterHandler</code> or <code>null</code>
     * @since 3.3
     */
    public Optional<FilterHandler> getFilterHandler() {
        return Optional.ofNullable(m_filterHandler);
    }

    DataColumnMetaDataManager getMetaDataManager() {
        return m_metaDataManager;
    }

    /**
     * Retrieves the {@link DataColumnMetaData} of class <b>metaDataClass</b>.
     * An empty {@link Optional} is returned if no {@link DataColumnMetaData} of class <b>metaDataClass</b> is available.
     *
     * @param metaDataClass the type of {@link DataColumnMetaData} to be retrieved
     * @return the {@link DataColumnMetaData} for type <b>metaDataClass</b>
     * @since 4.1
     */
    public <M extends DataColumnMetaData> Optional<M> getMetaDataOfType(final Class<M> metaDataClass) {
        return m_metaDataManager.getMetaDataOfType(metaDataClass);
    }


    /**
     * The <code>DataColumnSpec</code> of this instance is compatible with the given spec if it has the same column name
     * and the {@link DataType} of the given type is a super type of the instance type.
     * Domain info, properties, and handlers are not considered during the comparison.
     *
     *
     * @param cspec another <code>DataColumnSpec</code> to compare this column to
     * @return <code>true</code> if both have the same column name and compatible types, otherwise <code>false</code>
     *
     * @see DataType#isASuperTypeOf(DataType)
     * @since 5.0
     */
    public boolean isCompatibleWith(final DataColumnSpec cspec) {
        if (cspec == this) {
            return true;
        }
        if (cspec == null) {
            return false;
        }
        return getName().equals(cspec.getName())
                && cspec.getType().isASuperTypeOf(getType());
    }

    /**
     * Two <code>DataColumnSpec</code>s are equal if they have the same
     * column name and type. Domain info, properties, and handlers are not
     * considered during the comparison.
     *
     * @param cspec another <code>DataColumnSpec</code> to compare this column
     *              to
     * @return <code>true</code> if both have the same column name and type,
     *          otherwise <code>false</code>
     *
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equalStructure(final DataColumnSpec cspec) {
        if (cspec == this) {
            return true;
        }
        if (cspec == null) {
            return false;
        }
        return getName().equals(cspec.getName())
                && getType().equals(cspec.getType());
    }

    /**
     * Two <code>DataColumnSpec</code>s are equal, if the name, type,
     * properties, domain, all property handlers, and element names are equal.
     *
     * @param o the <code>DataColumnSpec</code> to check equality
     * @return <code>true</code> if both objects are equal, otherwise
     *      <code>false</code>
     */
    @Override
    public boolean equals(final Object o) {
        if (o == this) {
            return true;
        }
        if (o == null || !(o instanceof DataColumnSpec)) {
            return false;
        }
        DataColumnSpec cspec = (DataColumnSpec) o;
        boolean areEqual =
            getName().equals(cspec.getName())
            && getType().equals(cspec.getType())
            && getDomain().equals(cspec.getDomain())
            && getProperties().equals(cspec.getProperties())
            && getElementNames().equals(cspec.getElementNames())
            && getMetaDataManager().equals(cspec.getMetaDataManager());
        return areEqual
                && Objects.equals(m_colorHandler, cspec.m_colorHandler)
                && Objects.equals(m_sizeHandler, cspec.m_sizeHandler)
                && Objects.equals(m_shapeHandler, cspec.m_shapeHandler)
                && Objects.equals(m_filterHandler, cspec.m_filterHandler);

    }

    /**
     * The hash code is computed based on the hash code of column name and type.
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return getName().hashCode() ^ getType().hashCode();
    }

    /**
     * Returns a string summary of this column spec including name and type.
     *
     * @return a string summary of this column spec with column name and type
     */
    @Override
    public String toString() {
        return getName() + " (" + getType() + ")";
    }

    /**
     * Saves name, type, domain, and properties and - if available - color,
     * size, and shape handler to the given <code>ConfigWO</code>.
     *
     * @param config write properties into
     * @throws NullPointerException if the config object is <code>null</code>
     */
    public void save(final ConfigWO config) {
        config.addString(CFG_COLUMN_NAME, m_name);
        if (m_elementNames.size() != 1 || !m_name.equals(m_elementNames.get(0))) {
            config.addStringArray(CFG_ELEMENT_NAMES, m_elementNames.toArray(new String[m_elementNames.size()]));
        }
        m_type.save(config.addConfig(CFG_COLUMN_TYPE));
        m_domain.save(config.addConfig(CFG_COLUMN_DOMAIN));
        m_properties.save(config.addConfig(CFG_COLUMN_PROPS));
        if (m_colorHandler != null) {
            m_colorHandler.save(config.addConfig(CFG_COLORS));
        }
        if (m_sizeHandler != null) {
            m_sizeHandler.save(config.addConfig(CFG_SIZES));
        }
        if (m_shapeHandler != null) {
            m_shapeHandler.save(config.addConfig(CFG_SHAPES));
        }
        if (m_valueFormatHandler != null) {
            m_valueFormatHandler.save(config.addConfig(CFG_VALUE_FORMAT));
        }
        if (m_filterHandler != null) {
            m_filterHandler.save(config.addConfig(CFG_FILTER));
        }
        m_metaDataManager.save(config.addConfig(CFG_META_DATA));
    }

    /**
     * Reads name, type, domain, and properties from the given
     * <code>ConfigRO</code> and - if available - size, shape, and color
     * handler. Returns a new <code>DataColumnSpec</code> object initialized
     * with the information read.
     *
     * @param config to read properties from
     * @return a new column spec object
     * @throws InvalidSettingsException if one of the non-optional properties is
     *             not available or can't be initialized
     * @throws NullPointerException if the config object is <code>null</code>
     */
    public static DataColumnSpec load(final ConfigRO config)
            throws InvalidSettingsException {
        String name = config.getString(CFG_COLUMN_NAME);
        String[] elNames = config.getStringArray(
                CFG_ELEMENT_NAMES, (String[])null);
        if (elNames == null) {
            elNames = new String[]{name};
        }
        DataType type = DataType.load(config.getConfig(CFG_COLUMN_TYPE));
        DataColumnDomain domain = DataColumnDomain.load(config.getConfig(CFG_COLUMN_DOMAIN));
        DataColumnProperties properties = DataColumnProperties.load(config.getConfig(CFG_COLUMN_PROPS));
        ColorHandler color = null;
        if (config.containsKey(CFG_COLORS)) {
            color = ColorHandler.load(config.getConfig(CFG_COLORS));
        }
        SizeHandler size = null;
        if (config.containsKey(CFG_SIZES)) {
            size = SizeHandler.load(config.getConfig(CFG_SIZES));
        }
        ShapeHandler shape = null;
        if (config.containsKey(CFG_SHAPES)) {
            shape = ShapeHandler.load(config.getConfig(CFG_SHAPES));
        }
        ValueFormatHandler valueFormatHandler = null;
        if(config.containsKey(CFG_VALUE_FORMAT)) {
            valueFormatHandler = ValueFormatHandler.load(config.getConfig(CFG_VALUE_FORMAT));
        }
        FilterHandler filter = null;
        if (config.containsKey(CFG_FILTER)) {
            filter = FilterHandler.load(config.getConfig(CFG_FILTER));
        }
        final DataColumnMetaDataManager metaDataManager;
        if (config.containsKey(CFG_META_DATA)) {
            metaDataManager = DataColumnMetaDataManager.load(config.getConfig(CFG_META_DATA));
        } else {
            // create an empty meta data object to avoid issues with NPEs
            metaDataManager = DataColumnMetaDataManager.EMPTY;
        }
        return new DataColumnSpec(name, elNames, type, domain, properties, size, color, shape, valueFormatHandler, filter, metaDataManager);
    }

} // DataColumnSpec
