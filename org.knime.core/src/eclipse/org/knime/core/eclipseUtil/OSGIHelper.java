package org.knime.core.eclipseUtil;

import org.eclipse.core.runtime.IProduct;
import org.eclipse.core.runtime.Platform;
import org.eclipse.equinox.internal.app.CommandLineArgs;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

/**
 * This class contains some methods to access OSGI information that may not be
 * available if the program is not running in a complete OSGI framework.
 *
 * @author Robert Cauble, Pervasive
 * @author Thorsten Meinl, University of Konstanz
 * @since 2.6
 */
@SuppressWarnings("restriction")
public final class OSGIHelper {
    /**
     * Returns the bundle that contains the given class.
     *
     * @param cls a class
     * @return a bundle or <code>null</code> if the bundle can not be determined
     */
    public static Bundle getBundle(final Class<?> cls) {
        if (hasOSGIFramework()) {
            return BundleGetter.get(cls);
        } else {
            return null;
        }
    }

    /**
     * Returns the name of the current Eclipse application.
     *
     * @return the application name
     */
    public static String getApplicationName() {
        if (hasOSGIFramework() && hasEclipsePlatform()) {
            return ProductNameGetter.get();
        } else {
            return "<unknown>";
        }
    }

    private static boolean hasOSGIFramework() {
        try {
            Class.forName("org.osgi.framework.FrameworkUtil");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private static boolean hasEclipsePlatform() {
        try {
            Class.forName("org.eclipse.core.runtime.Platform");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private static class BundleGetter {
        public static Bundle get(final Class<?> cls) {
            return FrameworkUtil.getBundle(cls);
        }
    }

    private static class ProductNameGetter {
        public static String get() {
            IProduct product = Platform.getProduct();
            if (product != null) {
                return product.getApplication();
            } else {
                String[] args = CommandLineArgs.getAllArgs();
                for (int i = 0; i < args.length; i++) {
                    if ("-application".equals(args[i])) {
                        if (args.length > (i + 1)) {
                            return args[i + 1];
                        } else {
                            return "<unknown>";
                        }
                    }
                }
                return "<unknown>";
            }
        }
    }
}
