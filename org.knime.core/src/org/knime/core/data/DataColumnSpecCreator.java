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
 */
package org.knime.core.data;

import org.knime.core.data.property.ColorHandler;
import org.knime.core.data.property.SizeHandler;

/**
 * A Creator class that allows (as the only such way from outside this package)
 * to create a new DataColumnSpec. It can be created from an old Spec or by
 * specifying a name and type. Setter functions allow to overwrite all available
 * members in the (later) created DataColumnSpec.
 * 
 * @author M. Berthold, University of Konstanz
 */
public class DataColumnSpecCreator {

    /** Keeps the column name. */
    private String m_name;

    /** Keeps the column type. */
    private DataType m_type;

    /** Keeps the column domain with additional meta-info. */
    private DataColumnDomain m_domain = null;

    /** Contains column annotations. */
    private DataColumnProperties m_properties = null;

    /** Holds the SizeHandler if one was set. */
    private SizeHandler m_sizeHandler = null;

    /** Holds the ColorHandler if one was set. */
    private ColorHandler m_colorHandler = null;
    
    /**
     * Constructor - start creation of new DataColumnSpec only based on name (in
     * a String) and type.
     * 
     * @param name the new name
     * @param type the new type
     */
    public DataColumnSpecCreator(final String name, final DataType type) {
        if (name == null) {
            throw new NullPointerException("Name of column can't be null.");
        }
        if (type == null) {
            throw new NullPointerException("Type of column can't be null.");
        }
        m_name = name;
        m_type = type;
        m_domain = new DataColumnDomain(null, null, null);
        m_properties = new DataColumnProperties();
    }

    /**
     * Constructor - start creation of new DataColumnSpec based on an old
     * DataColumnSpec.
     * 
     * @param cspec old spec.
     */
    public DataColumnSpecCreator(final DataColumnSpec cspec) {
        m_name = cspec.getName();
        m_type = cspec.getType();
        m_domain = cspec.getDomain();
        assert m_domain != null; // correct column specs have non-null
        // copy properties
        m_properties = cspec.getProperties();
        assert m_properties != null; // correct specs have non-null
        // property size
        m_sizeHandler = cspec.getSizeHandler();
        // property color
        m_colorHandler = cspec.getColorHandler();

    }

    /**
     * Set new Name.
     * 
     * @param name the new name
     */
    public void setName(final String name) {
        if (name == null) {
            throw new NullPointerException("Name of DataColumnSpec can not"
                    + " be null!");
        }
        m_name = name;
    }

    /**
     * Set new Type.
     * 
     * @param type the new type
     */
    public void setType(final DataType type) {
        if (type == null) {
            throw new NullPointerException("Type of DataColumnSpec can not"
                    + " be null!");
        }
        m_type = type;
    }

    /**
     * Set new Domain. If a null domain is set, an empty domain will be created.
     * 
     * @param domain the new domain, if null an empty default domain will be
     *            created
     */
    public void setDomain(final DataColumnDomain domain) {
        if (domain == null) {
            m_domain = new DataColumnDomain(null, null, null);
        } else {
            m_domain = domain;
        }
    }

    /**
     * Set new Column Properties. If a null property object is passed, a new
     * empty default property object will be created.
     * 
     * @param props the new properties, if null an empty default props object is
     *            created.
     */
    public void setProperties(final DataColumnProperties props) {
        if (props == null) {
            m_properties = new DataColumnProperties();
        } else {
            m_properties = props;
        }
    }

    /**
     * Set new SizeHandler.
     * 
     * @param sizeHdl the new SizeHandler
     */
    public void setSizeHandler(final SizeHandler sizeHdl) {
        m_sizeHandler = sizeHdl;
    }
    
    /**
     * Set new ColorHandler.
     * 
     * @param colorHdl the new ColorHandler
     */
    public void setColorHandler(final ColorHandler colorHdl) {
        m_colorHandler = colorHdl;
    }
    
    /**
     * Removes all handler from this creator which are then <code>null</code>
     * for the next call of <code>#createSpec()</code>.
     */
    public void removeAllHandlers() {
        this.setSizeHandler(null);
        this.setColorHandler(null);
    }
    
    /**
     * @return newly created DataColumnSpec.
     */
    public DataColumnSpec createSpec() {
        return new DataColumnSpec(m_name, m_type, m_domain, m_properties,
                m_sizeHandler, m_colorHandler);
    }
}
