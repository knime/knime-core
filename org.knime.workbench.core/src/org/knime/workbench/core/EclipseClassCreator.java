/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2010
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
 * -------------------------------------------------------------------
 *
 * History
 *   04.07.2005 (Florian Georg): created
 */
package org.knime.workbench.core;

import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.knime.core.eclipseUtil.ClassCreator;
import org.osgi.framework.Bundle;
/**
 * Class creator, used inside Eclipse to load classes for the KNIME core. We
 * need this to lookup classes from the contributing plugins.
 *
 * This class is kinda hack ... it first tries to load classes from the core and
 * editor plugins. If this fails, the requested class is tried to be loaded from
 * all plugins that contribute extensions to a given extension point ID. In our
 * case, this is normally the "knime.nodes" extension point, so that we can load
 * <code>NodeFactory</code> classes from contributing plugins.
 *
 * @author Florian Georg, University of Konstanz
 */
public class EclipseClassCreator implements ClassCreator {
    private IExtension[] m_extensions;

    /**
     * Constructor.
     *
     * @param pointID The extension point to process
     */
    public EclipseClassCreator(final String pointID) {
        IExtensionRegistry registry = Platform.getExtensionRegistry();
        IExtensionPoint point = registry.getExtensionPoint(pointID);
        if (point == null) {
            throw new IllegalStateException("Invalid extension point : "
                    + pointID);

        }
        m_extensions = point.getExtensions();
    }

    /**
     * Tries to resolve the class by asking all plugins that contribute to our
     * extension point.
     *
     * @param className The class to lookup
     * @return class, or <code>null</code>
     */
    public Class<?> createClass(final String className) {
        Class<?> clazz = null;

//        // first, try the core and editor
//        try {
//            Bundle p = KNIMECorePlugin.getDefault().getBundle();
//            clazz = p.loadClass(className);
//            return clazz;
//        } catch (Exception ex) {
//            try {
//                Bundle p = Platform.getBundle("org.knime.workbench.editor");
//                clazz = p.loadClass(className);
//                return clazz;
//            } catch (Exception e) {
//                // ignore
//            }
//        }

        /**
         * Look at all extensions, and try to load class from the plugin
         */
        for (int i = 0; i < m_extensions.length && clazz == null; i++) {
            IExtension ext = m_extensions[i];
            String pluginID = ext.getNamespaceIdentifier();
            try {
                // workaround for a bug that occurs when deserializing array
                // objects. The bundle class loader is not able to resolve
                // arrays, e.g. "[Lorg.openscience.cdk.AtomContainer;". The
                // eclipse bug is resolved as WONTFIX
                // (https://bugs.eclipse.org/bugs/show_bug.cgi?id=129550)
                // Their solution: Developers shall use the Class.forName()
                // method, which - however - does not work out here as we have
                // no chance to get a proper class loader from the Bundle.
                // NOTE: all that is obsolete. It will be removed by the buddy
                // class loading concept in later versions.
                Bundle bundle = Platform.getBundle(pluginID);
                if (bundle != null) {
                    clazz = bundle.loadClass(className);
                }
            } catch (Exception e) {
                // ignore
            }
        }
        return clazz;
    }

    /**
     * Exception that is thrown if a plugin cannot be loaded because it does not
     * exist.
     *
     * @author Thorsten Meinl, University of Konstanz
     */
    public static class NoSuchPluginException extends Exception {
        /**
         * Constructs a new exception with the specified detail message and
         * cause.
         * <p>
         * Note that the detail message associated with <code>cause</code> is
         * <i>not</i> automatically incorporated in this exception's detail
         * message.
         *
         * @param pluginID the ID of the non-existing plugin
         * @param cause the cause (which is saved for later retrieval by the
         *            {@link #getCause()} method). (A <tt>null</tt> value is
         *            permitted, and indicates that the cause is nonexistent or
         *            unknown.)
         */
        public NoSuchPluginException(final String pluginID,
                final Throwable cause) {
            super("Plugin " + pluginID + " not found", cause);
        }

        /**
         * Constructs a new exception with the specified detail message. The
         * cause is not initialized, and may subsequently be initialized by a
         * call to {@link #initCause(Throwable)}.
         *
         * @param pluginID the ID of the non-existing plugin
         */
        public NoSuchPluginException(final String pluginID) {
            super("Plugin " + pluginID + " not found");
        }

        /**
         * Constructs a new exception with the specified cause and a detail
         * message of <tt>(cause==null ? null : cause.toString())</tt> (which
         * typically contains the class and detail message of <tt>cause</tt>).
         * This constructor is useful for exceptions that are little more than
         * wrappers for other throwables (for example, {@link
         * java.security.PrivilegedActionException}).
         *
         * @param cause the cause (which is saved for later retrieval by the
         *            {@link #getCause()} method). (A <tt>null</tt> value is
         *            permitted, and indicates that the cause is nonexistent or
         *            unknown.)
         */
        public NoSuchPluginException(final Throwable cause) {
            super(cause);
        }
    }
}
