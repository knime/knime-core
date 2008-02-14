/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
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
 * ---------------------------------------------------------------------
 * 
 * History
 *   11.09.2007 (mb): created
 */
package org.knime.core.node;

import org.knime.core.eclipseUtil.GlobalClassCreator;

/** Holds type information about node port types.
 * 
 * @author M. Berthold & B. Wiswedel, University of Konstanz
 */
public final class PortType {
    private final Class<? extends PortObjectSpec> m_specClass;
    private final Class<? extends PortObject> m_objectClass;
    
    // made package visible to avoid others from creating new types.
    PortType(final Class<? extends PortObjectSpec> specClass,
            final  Class<? extends PortObject> objectClass) {
        if ((specClass == null) || (objectClass == null)) {
            throw new NullPointerException("PortType args must not be null!");
        }
        m_specClass = specClass;
        m_objectClass = objectClass;
    }
    
    public Class<? extends PortObjectSpec> getPortObjectSpecClass() {
        return m_specClass;
    }

    public Class<? extends PortObject> getPortObjectClass() {
        return m_objectClass;
    }
    
    /** Returns string comprising spec and object class.
     * {@inheritDoc} */
    @Override
    public String toString() {
        return "PortType(" + m_specClass.getSimpleName() + "," 
            + m_objectClass.getSimpleName() + ")";
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return m_specClass.hashCode() ^ m_objectClass.hashCode();
    }
    
    /** {@inheritDoc} */
    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof PortType)) {
            return false;
        }
        PortType other = (PortType)obj;
        return m_specClass.equals(other.m_specClass)
            && m_objectClass.equals(other.m_objectClass);
    }
    
    public void save(final NodeSettingsWO settings) {
        settings.addString("spec_class", m_specClass.getName());
        settings.addString("object_class", m_objectClass.getName());
    }

    @SuppressWarnings("unchecked")
    public static PortType load(final NodeSettingsRO settings) 
        throws InvalidSettingsException {
        String specClassString = settings.getString("spec_class");
        String objectClassString = settings.getString("object_class");
        if (specClassString == null) {
            throw new InvalidSettingsException(
                "No port specification class found to create PortType object");
        }
        if (objectClassString == null) {
            throw new InvalidSettingsException(
                "No port object class found to create PortType object");
        }
        Class<?> spClass = null;
        Class<?> obClass;
        try {
            spClass = GlobalClassCreator.createClass(specClassString);
            obClass = GlobalClassCreator.createClass(objectClassString);
        } catch (ClassNotFoundException e) {
            String eSource, eClass;
            if (spClass != null) {
                eSource = "object";
                eClass = objectClassString;
            } else {
                eSource = "specification";
                eClass = specClassString;
            }
            throw new InvalidSettingsException("Can't load class \""
                    + eClass + "\" for port " + eSource);
        }
        if (!PortObjectSpec.class.isAssignableFrom(spClass)) {
            throw new InvalidSettingsException("Port specification class \""
                    + specClassString + "\" does not extend " 
                    + PortObjectSpec.class.getSimpleName());
        }
        if (!PortObject.class.isAssignableFrom(obClass)) {
            throw new InvalidSettingsException("Port object class \""
                    + objectClassString + "\" does not extend " 
                    + PortObject.class.getSimpleName());
        }
        return new PortType((Class<? extends PortObjectSpec>)spClass,
                (Class<? extends PortObject>)obClass);
    }
    
}
