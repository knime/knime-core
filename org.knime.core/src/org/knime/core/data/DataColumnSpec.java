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
 * --------------------------------------------------------------------- *
 * 
 * History
 *    25.10.2006 (tg): cleanup
 *    31.10.2006 (tm, cs): reviewed
 */
package org.knime.core.data;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.knime.core.data.property.ColorHandler;
import org.knime.core.data.property.PropertyHandler;
import org.knime.core.data.property.ShapeHandler;
import org.knime.core.data.property.SizeHandler;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.config.ConfigRO;
import org.knime.core.node.config.ConfigWO;

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
     * @throws NullPointerException if either column name, type, domain, or
     *             properties are <code>null</code>
     */
    DataColumnSpec(final String name, final String[] elNames,
            final DataType type, final DataColumnDomain domain,
            final DataColumnProperties props, final SizeHandler sizeHdl,
            final ColorHandler colorHdl, final ShapeHandler shapeHdl) {
        if (name == null || type == null || domain == null || props == null
                || elNames == null) {
            throw new NullPointerException("Do not init DataColumnSpec with"
                    + " null arguments!");
        }
        List<String> elNamesAsList = 
            Collections.unmodifiableList(Arrays.asList(elNames));
        if (elNamesAsList.contains(null)) {
            throw new NullPointerException(
                    "Element names must not contain null elements");
        }
        m_name = name;
        m_elementNames = elNamesAsList;
        m_type = type;
        m_domain = domain;
        m_properties = props;
        m_sizeHandler = sizeHdl;
        m_colorHandler = colorHdl;
        m_shapeHandler = shapeHdl;
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
     * @return atached <code>ShapeHandler</code> or <code>null</code>
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
            && getElementNames().equals(cspec.getElementNames());
        return areEqual && equalsHandlers(m_colorHandler, cspec.m_colorHandler)
            && equalsHandlers(m_sizeHandler, cspec.m_sizeHandler)
            && equalsHandlers(m_shapeHandler, cspec.m_shapeHandler);
    }
    
    /** @return helper method that compares two <code>PropertyHandler</code>s */
    private boolean equalsHandlers(
            final PropertyHandler h1, final PropertyHandler h2) {
        if (h1 == null) {
            return (h2 == null);
        }
        return h1.equals(h2);
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
        return "name=" + getName() + ",type=" + getType();
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
        if (m_elementNames.size() != 1 
                || !m_name.equals(m_elementNames.get(0))) {
            config.addStringArray(CFG_ELEMENT_NAMES, 
                    m_elementNames.toArray(new String[m_elementNames.size()]));
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
        DataColumnDomain domain =
                DataColumnDomain.load(config.getConfig(CFG_COLUMN_DOMAIN));
        DataColumnProperties properties =
                DataColumnProperties.load(config.getConfig(CFG_COLUMN_PROPS));
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
        return new DataColumnSpec(name, elNames, type, domain, properties,
                size, color, shape);
    }

} // DataColumnSpec
