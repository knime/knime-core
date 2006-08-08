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
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * --------------------------------------------------------------------- *
 */
package org.knime.core.data;

import org.knime.core.data.property.ColorHandler;
import org.knime.core.data.property.SizeHandler;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.config.ConfigRO;
import org.knime.core.node.config.ConfigWO;

/**
 * Interface describing the makeup of one column in a <code>DataTable</code>
 * containing information regarding type, name, all used values (if available),
 * and possibly other information in the future.
 * 
 * <p>
 * Each column spec should be able to make a clone of itself by copying the
 * internal structure into a new instance.
 * 
 * <p>
 * In addition, a SizeHandler and/or ColorHandler can be set to retrieved 
 * viewing properties for certain attribute values.
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

    /** Keeps the column type. */
    private final DataType m_type;

    /** Keeps the column domain with additional meta-info. */
    private final DataColumnDomain m_domain;

    /** Contains column annotations. */
    private final DataColumnProperties m_properties;

    /** Holds the SizeHandler if one was set. */
    private final SizeHandler m_sizeHandler;

    /** Holds the ColorHandler if one was set. */
    private final ColorHandler m_colorHandler;
    
    /**
     * Constructor taking all properties of this column spec as an argument. It
     * creates a "read-only" DataColumnSpec and should only be called from the
     * DataColumnSpecCreator in this package.
     * 
     * @param name the name of the column, must not be null
     * @param type the type of the column, must not be null
     * @param domain the domain, must not be null
     * @param props additional properties, must not be null
     * @param sizeHdl the SizeHandler or null
     * @param colorHdl the ColorHandler or null
     */
    DataColumnSpec(final String name, final DataType type,
            final DataColumnDomain domain, final DataColumnProperties props,
            final SizeHandler sizeHdl, final ColorHandler colorHdl) {
        assert name != null;
        assert type != null;
        assert domain != null;
        assert props != null;
        m_name = name;
        m_type = type;
        m_domain = domain;
        m_properties = props;
        m_sizeHandler = sizeHdl;
        m_colorHandler = colorHdl;
    }

    /**
     * Returns the name of this column.
     * 
     * @return The column name.
     */
    public String getName() {
        return m_name;
    }

    /**
     * Returns column type as which is a subclass of <code>DataType</code>.
     * 
     * @return The DataType of this column. All data cells of this column are
     *         typecastable to the native type of it.
     * @see org.knime.core.data.DataType
     * @see org.knime.core.data.DataCell
     */
    public DataType getType() {
        return m_type;
    }

    /**
     * Returns the domain of this column spec which includes meta-information
     * such as bounds, possible values, etc.
     * 
     * @return The domain of the column spec. Could be empty, but never null.
     */
    public DataColumnDomain getDomain() {
        return m_domain;
    }

    /**
     * Returns the properties assigned to this column spec. These properties can
     * be seen as some sort of annotations to identify particular column
     * properties.
     * 
     * @return The column's annotation properties, never null.
     */
    public DataColumnProperties getProperties() {
        return m_properties;
    }

    /**
     * Returns the SizeHandler defined on this column, if available (otherwise
     * null will be returned).
     * 
     * @return attached SizeHandler or null.
     */
    public SizeHandler getSizeHandler() {
        return m_sizeHandler;
    }
    
    /**
     * Returns the ColorHandler defined on this column, if available (otherwise
     * null will be returned).
     * 
     * @return attached ColorHandler or null.
     */
    public ColorHandler getColorHandler() {
        return m_colorHandler;
    }

    /**
     * Two column specs are equal if their name and type match. For a test
     * including the domain, see the next method.
     * 
     * @param obj another column spec to compare this to
     * @return true if this equals the argument
     * @see #equalsWithDomain(DataColumnSpec)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof DataColumnSpec)) {
            return false;
        }
        final DataColumnSpec spec = (DataColumnSpec)obj;
        return getName().equals(spec.getName())
                && getType().equals(spec.getType());
    }

    /**
     * @see java.lang.Object#equals(java.lang.Object)
     * @param spec The spec to check equality.
     * @return <code>true</code> if name, type, and domain are equal.
     */
    public boolean equalsWithDomain(final DataColumnSpec spec) {
        if (spec == this) {
            return true;
        }
        if (spec == null) {
            return false;
        }
        return m_domain.equals(spec.getDomain())
                && getProperties().equals(spec.getProperties())
                && getName().equals(spec.getName())
                && getType().equals(spec.getType());
    }

    /**
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        int tempHash = getName().hashCode();
        tempHash ^= getType().hashCode();
        return tempHash;
    }

    /**
     * Returns a String summary of this column spec including name and type.
     * 
     * @return A String summary of this column spec.
     */
    @Override
    public String toString() {
        return "name=" + getName() + ",type=" + getType();
    }
    
    private static final String CFG_COLUMN_NAME   = "column_name";
    private static final String CFG_COLUMN_TYPE   = "column_type";
    private static final String CFG_COLUMN_DOMAIN = "column_domain";
    private static final String CFG_COLUMN_PROPS  = "column_properties";
    private static final String CFG_COLORS        = "color_handler";
    private static final String CFG_SIZES         = "size_handler";
    
    /**
     * Saves name, type, domain and properties and - if available - color and 
     * size property to the given <code>Config</code>. 
     * @param config Write properties into.
     */
    public void save(final ConfigWO config) {
        config.addString(CFG_COLUMN_NAME, m_name);
        m_type.save(config.addConfig(CFG_COLUMN_TYPE));
        m_domain.save(config.addConfig(CFG_COLUMN_DOMAIN));
        m_properties.save(config.addConfig(CFG_COLUMN_PROPS));
        if (m_colorHandler != null) {
            m_colorHandler.save(config.addConfig(CFG_COLORS));
        }
        if (m_sizeHandler != null) {
            m_sizeHandler.save(config.addConfig(CFG_SIZES));
        }   
    }
    
    /**
     * Reads name, type, domain, and properties from the given 
     * <code>Config</code> and - if available - size and color property; return 
     * a new <code>DataColumnSpec</code> object.
     * @param config To read properties from.
     * @return A new column spec object.
     * @throws InvalidSettingsException If one of the non-optinal properties is
     *         not available or can't be initialized.
     */
    public static DataColumnSpec load(final ConfigRO config) 
            throws InvalidSettingsException {
        String name = config.getString(CFG_COLUMN_NAME);
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
        return new DataColumnSpec(name, type, domain, properties, size, color);
    }

} // DataColumnSpec
