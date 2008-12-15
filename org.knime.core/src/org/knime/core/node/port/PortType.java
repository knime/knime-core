/*
 * ------------------------------------------------------------------ *
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
package org.knime.core.node.port;

import java.lang.reflect.Method;

import org.knime.core.eclipseUtil.GlobalClassCreator;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * Holds type information about node port types.
 * <p>
 * The documentation of this class is mostly missing and will be fixed in future
 * versions. For example implementations refer to one of the core
 * implementations in KNIME core.
 * 
 * <p>
 * Please also note that the general API for PortTypes (which is new in KNIME
 * 2.0) is not finalized and may (slightly) change in future versions as well
 * (meaning also that methods may be added to either this class or
 * {@link PortObject} or {@link PortObjectSpec}).
 * 
 * @since 2.0
 * @author M. Berthold & B. Wiswedel, University of Konstanz
 */
public final class PortType {
    private final Class<? extends PortObjectSpec> m_specClass;
    private final Class<? extends PortObject> m_objectClass;
    
    public PortType(final Class<? extends PortObject> objectClass) {
        m_objectClass = objectClass;
        m_specClass = getPortObjectSpecClass(objectClass);
    }
    
    public Class<? extends PortObjectSpec> getPortObjectSpecClass() {
        return m_specClass;
    }

    public Class<? extends PortObject> getPortObjectClass() {
        return m_objectClass;
    }
    
    /** Determines if the argument type is a sub type of this type.
     * @param subType The type to check
     * @return If this type is a super type.
     * @throws NullPointerException if the argument is null
     */
    public boolean isSuperTypeOf(final PortType subType) {
        boolean result = m_objectClass.isAssignableFrom(subType.m_objectClass);
        // if it's a super type, also the spec will be a super class
        // (can only narrow down the return type of the getSpec() method)
        assert !result || m_specClass.isAssignableFrom(subType.m_specClass);
        return result;
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
        return m_objectClass.hashCode();
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
        return m_objectClass.equals(other.m_objectClass);
    }
    
    public void save(final NodeSettingsWO settings) {
        settings.addString("object_class", m_objectClass.getName());
    }
    
    public boolean acceptsPortObjectSpec(final PortObjectSpec spec) {
        return spec == null || m_specClass.isAssignableFrom(spec.getClass());
    }
    
    public boolean acceptsPortObject(final PortObject obj) {
        return obj == null || m_objectClass.isAssignableFrom(obj.getClass());
    }

    @SuppressWarnings("unchecked")
    public static PortType load(final NodeSettingsRO settings) 
        throws InvalidSettingsException {
        String objectClassString = settings.getString("object_class");
        if (objectClassString == null) {
            throw new InvalidSettingsException(
                "No port object class found to create PortType object");
        }
        Class<?> obClass;
        try {
            obClass = GlobalClassCreator.createClass(objectClassString);
        } catch (ClassNotFoundException e) {
            throw new InvalidSettingsException("Unable to restore port type, " 
                    + "can't load class \"" + objectClassString + "\"");
        }
        if (!PortObject.class.isAssignableFrom(obClass)) {
            throw new InvalidSettingsException("Port object class \""
                    + objectClassString + "\" does not extend " 
                    + PortObject.class.getSimpleName());
        }
        return new PortType(obClass.asSubclass(PortObject.class));
    }
    
    public static Class<? extends PortObjectSpec> getPortObjectSpecClass(
            final Class<? extends PortObject> objectClass) {
        Method m;
        try {
            m = objectClass.getMethod("getSpec");
        } catch (SecurityException e) {
            NodeLogger.getLogger(PortObject.class).fatal(
                    "Security permissions does not allow " 
                    + "access of getSpec method", e);
            throw new RuntimeException(e);
        } catch (NoSuchMethodException e) {
            NodeLogger.getLogger(PortObject.class).fatal(
                    "getSpec() method is not implemented (though it is"
                    + " required by interface access of getSpec method", e);
            throw new RuntimeException(e);
        }
        return m.getReturnType().asSubclass(PortObjectSpec.class);
    }

    
}
