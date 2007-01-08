/*
 * ------------------------------------------------------------------
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
 * -------------------------------------------------------------------
 * 
 * History
 *   02.02.2006 (mb): created
 *   25.10.2006 (tg): cleanup
 *   31.10.2006 (tm, cs): reviewed
 */
package org.knime.core.data;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
     * Initializes the creator with two DataColumnSpecs. If they have equal
     * structure, the domain information from both DataColumnSpecs is merged,
     * Color, Shape and Size-Handlers are compared (must be equal).
     * 
     * @param cspec1 the first {@link DataColumnSpec}.
     * @param cspec2 the second {@link DataColumnSpec}.
     * 
     * @see DataTableSpec#mergeDataTableSpecs(DataTableSpec, DataTableSpec)
     * @throws IllegalArgumentException if the structure (type and name) does
     * not match, if the domain can not be merged, if the Color-, Shape- or
     * SizeHandlers are different or if a property with different values exists.
     */
    public DataColumnSpecCreator(final DataColumnSpec cspec1,
            final DataColumnSpec cspec2) {
        if (!cspec1.equalStructure(cspec2)) {
            throw new IllegalArgumentException("Structures of DataColumnSpecs"
                    + " do not match.");
        }
        setName(cspec1.getName());
        setType(cspec1.getType());
        
        // Domain
        DataColumnDomain domain1 = cspec1.getDomain();
        DataColumnDomain domain2 = cspec2.getDomain();
        if (domain1.equals(domain2)) {
            m_domain = domain1;
        } else {
            // merge domain information.
            if ((domain1.hasValues() && (domain2.hasLowerBound() || domain2
                    .hasUpperBound()))
                    || (domain2.hasValues()
                    && (domain1.hasLowerBound() || domain1.hasUpperBound()))) {
                throw new IllegalArgumentException(
                        "Will not merge, one ColumnSpec has possible values"
                             + " and the other has upper and/or lower bounds");
            }
            if (domain1.hasValues() || domain2.hasValues()) {
                Set<DataCell> mergedvals = new HashSet<DataCell>();
                if (domain1.hasValues()) {
                    mergedvals.addAll(domain1.getValues());
                }
                if (domain2.hasValues()) {
                    mergedvals.addAll(domain2.getValues());
                }
                setDomain(new DataColumnDomain(null, null, mergedvals));
            } else if (domain1.hasLowerBound() || domain1.hasUpperBound()
                    || domain2.hasLowerBound() || domain2.hasUpperBound()) {
                DataValueComparator comparator =
                        cspec1.getType().getComparator();
                DataCell lowerBound = domain1.getLowerBound();
                if (comparator.compare(lowerBound,
                        domain2.getLowerBound()) > 0) {
                    lowerBound = domain2.getLowerBound();
                }
                DataCell upperBound = domain1.getUpperBound();
                if (comparator.compare(upperBound, 
                        domain2.getUpperBound()) < 0) {
                    upperBound = domain2.getUpperBound();
                }
                setDomain(new DataColumnDomain(lowerBound, upperBound, null));
            } else {
                setDomain(new DataColumnDomain(null, null, null));
            }
        }
     
        // ColorHandler
        ColorHandler color1 = cspec1.getColorHandler();
        ColorHandler color2 = cspec2.getColorHandler();
        if (color1 != null && color2 != null) {
            if (color1.equals(color2)) {
                setColorHandler(color1);
            } else {
                throw new IllegalArgumentException("Will not merge. " 
                       + "Different color handlers for column: "
                       + cspec1.getName());
            }
        } else {
            if (color1 != null) {
                setColorHandler(color1);
            }
            if (color2 != null) {
                setColorHandler(color2);
            }
        }
        
        // Properties
        DataColumnProperties prop1 = cspec1.getProperties();
        DataColumnProperties prop2 = cspec2.getProperties();
        Map<String, String> mergedProps = new HashMap<String, String>();
        Enumeration e = prop1.properties();
        while (e.hasMoreElements()) {
            String key = (String)e.nextElement();
            String value = prop1.getProperty(key);
            mergedProps.put(key, value);
        }
        e = prop2.properties();
        while (e.hasMoreElements()) {
            String key = (String)e.nextElement();
            String prop1value = prop1.getProperty(key);
            String prop2value = prop2.getProperty(key);
            if (prop1value != null) {
                if (!prop1value.equals(prop2value)) {
                    throw new IllegalArgumentException("Will not merge. "
                            + "Property with key " + key + " has different "
                            + "values: " + prop1value + ", " + prop2value);
                }
            }
            mergedProps.put(key, prop2value);
        }
        setProperties(new DataColumnProperties(mergedProps));
        
        // SizeHandler
        SizeHandler size1 = cspec1.getSizeHandler();
        SizeHandler size2 = cspec2.getSizeHandler();
        if (size1 != null && size2 != null) {
            if (size1.equals(size2)) {
                setSizeHandler(size1);
            } else {
                throw new IllegalArgumentException("Will not merge. " 
                       + "Different size handlers for column: "
                       + cspec1.getName());
            }
        } else {
            if (size1 != null) {
                setSizeHandler(size1);
            }
            if (size2 != null) {
                setSizeHandler(size2);
            }
        }
        
        // ShapeHandler
        ShapeHandler shape1 = cspec1.getShapeHandler();
        ShapeHandler shape2 = cspec2.getShapeHandler();
        if (shape1 != null && shape2 != null) {
            if (shape1.equals(shape2)) {
                setShapeHandler(shape1);
            } else {
                throw new IllegalArgumentException("Will not merge. " 
                       + "Different shape handlers for column: "
                       + cspec1.getName());
            }
        } else {
            if (shape1 != null) {
                setShapeHandler(shape1);
            }
            if (shape2 != null) {
                setShapeHandler(shape2);
            }
        }
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
