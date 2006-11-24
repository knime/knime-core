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
 *   02.02.2006 (mb): created
 *   25.10.2006 (tg): cleanup
 *   31.10.2006 (tm, cs): reviewed
 */
package org.knime.core.data;

import org.knime.core.data.property.ColorHandler;
import org.knime.core.data.property.ShapeHandler;
import org.knime.core.data.property.SizeHandler;
import org.knime.core.node.NodeLogger;

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
public class DataColumnSpecCreator {
    
    private static final NodeLogger LOGGER = 
        NodeLogger.getLogger(DataColumnSpec.class);

    /** Keeps the column name. */
    private String m_name;

    /** Keeps the column type. */
    private DataType m_type;

    /** Keeps the column domain with additional meta-info. */
    private DataColumnDomain m_domain;

    /** Contains column annotations, can not be null. */
    private DataColumnProperties m_properties;

    /** Holds the SizeHandler if one was set or null. */
    private SizeHandler m_sizeHandler = null;

    /** Holds the ShapeHandler if one was set or null. */
    private ShapeHandler m_shapeHandler = null;

    /** Holds the ColorHandler if one was set or null. */
    private ColorHandler m_colorHandler = null;
    
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
    }

    /**
     * Initializes the creator with a given {@link DataColumnSpec}.
     * 
     * @param cspec other spec
     */
    public DataColumnSpecCreator(final DataColumnSpec cspec) {
        m_name = cspec.getName();
        assert m_name != null : "Column name must not be null!";
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

    }

    /**
     * Set (new) column name. If the column name is empty or consists only
     * of whitespaces, a warning is logged and an artificial name is created.
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
     * Set (new) <code>ColorHandler</code> which can be <code>null</code>.
     * 
     * @param colorHdl the (new) <code>ColorHandler</code> or
     *            <code>null</code>
     */
    public void setColorHandler(final ColorHandler colorHdl) {
        m_colorHandler = colorHdl;
    }

    /**
     * Removes all handlers from this creator which are then set to
     * <code>null</code> for the next call of <code>#createSpec()</code>.
     */
    public void removeAllHandlers() {
        this.setSizeHandler(null);
        this.setColorHandler(null);
        this.setShapeHandler(null);
    }

    /**
     * Creates and returns a new <code>DataColumnSpec</code> using the
     * internal properties of this creator.
     * 
     * @return newly created <code>DataColumnSpec</code>
     */
    public DataColumnSpec createSpec() {
        return new DataColumnSpec(m_name, m_type, m_domain, m_properties,
                m_sizeHandler, m_colorHandler, m_shapeHandler);
    }
}
