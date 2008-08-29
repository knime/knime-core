/* @(#)$RCSfile$
 * $Revision$ $Date$ $Author$
 *
 * -------------------------------------------------------------------
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
 * -------------------------------------------------------------------
 *
 * History
 *   04.07.2005 (Florian Georg): created
 */
package org.knime.workbench.core;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IPluginDescriptor;
import org.eclipse.core.runtime.Platform;
import org.knime.core.eclipseUtil.ClassCreator;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
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
//    private static final NodeLogger LOGGER =
//            NodeLogger.getLogger(EclipseClassCreator.class);

    private static final List<Bundle> DEPRECATED_PLUGINS =
            new ArrayList<Bundle>();

    /**
     * Try to load a deprecated plugin, so that its classes are loadable.
     *
     * @param pluginID the plugin's ID
     *
     * @throws NoSuchPluginException if the plugin cannot be found
     * @throws BundleException if an error
     */
    public static void addDeprecatedPlugin(final String pluginID)
            throws NoSuchPluginException, BundleException {
        Bundle b = Platform.getBundle(pluginID);
        if (b != null) {
            b.start();
            DEPRECATED_PLUGINS.add(b);
        } else {
            throw new NoSuchPluginException(pluginID);
        }
    }

//    static {
//        try {
//            addDeprecatedPlugin("org.knime.deprecated");
//        } catch (NoSuchPluginException ex) {
//            LOGGER.warn("Plugin org.knime.deprecated not found, the "
//                    + "old nodes will not be available", ex);
//        } catch (BundleException ex) {
//            LOGGER.warn("Plugin org.knime.deprecated could not be "
//                    + "started, the old nodes will not be available", ex);
//        }
//    }

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
    public Class createClass(final String className) {

        Class clazz = null;

        // first, try the core and editor
        try {
            Bundle p = KNIMECorePlugin.getDefault().getBundle();
            clazz = p.loadClass(className);
            return clazz;
        } catch (Exception ex) {
            try {
                Bundle p = Platform.getBundle("org.knime.workbench.editor");
                clazz = p.loadClass(className);
                return clazz;
            } catch (Exception e) {
                // ignore
            }
        }

        /**
         * Look at all extensions, and try to load class from the plugin
         */
        for (int i = 0; i < m_extensions.length && clazz == null; i++) {
            IExtension ext = m_extensions[i];
            String pluginID = ext.getNamespace();
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
                IPluginDescriptor pluginDesc =
                        ext.getDeclaringPluginDescriptor();
                if (pluginDesc != null) {
                    ClassLoader l = pluginDesc.getPluginClassLoader();
                    clazz = Class.forName(className, false, l);
                } else {
                    Bundle p = Platform.getBundle(pluginID);
                    clazz = p.loadClass(className);
                }
            } catch (Exception e) {
                // ignore
            }
        }

        if (clazz == null) {
            for (Bundle b : DEPRECATED_PLUGINS) {
                // also check the deprecated plugin; this is not contained
                // in the extensions and thus not covered by the above loop
                try {
                    clazz = b.loadClass(className);
                    return clazz;

                } catch (ClassNotFoundException cnfe) {
                    // clazz just stays null
                }
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
