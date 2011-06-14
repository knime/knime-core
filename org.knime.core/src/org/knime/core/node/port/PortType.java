/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
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
 * ---------------------------------------------------------------------
 *
 * History
 *   11.09.2007 (mb): created
 */
package org.knime.core.node.port;

import java.lang.reflect.Method;

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
    private boolean m_isOptional;

    /**
     * @param objectClass compatible port objects
     * @param isOptional indicates that this port does not need to be connected
     */
    public PortType(final Class<? extends PortObject> objectClass,
            final boolean isOptional) {
        m_objectClass = objectClass;
        m_specClass = getPortObjectSpecClass(objectClass);
        m_isOptional = isOptional;
    }

    /**
     * @param objectClass compatible port objects. Non-optional port!
     */
    public PortType(final Class<? extends PortObject> objectClass) {
        this(objectClass, false);
    }

    public Class<? extends PortObjectSpec> getPortObjectSpecClass() {
        return m_specClass;
    }

    public Class<? extends PortObject> getPortObjectClass() {
        return m_objectClass;
    }

    /**
     * @return true if this port does not need to be connected.
     */
    public boolean isOptional() {
        return m_isOptional;
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
            obClass = Class.forName(objectClassString);
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
