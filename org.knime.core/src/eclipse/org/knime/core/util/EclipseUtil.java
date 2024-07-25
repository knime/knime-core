/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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
 * Created on 22.11.2013 by thor
 */
package org.knime.core.util;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.management.ManagementFactory;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Platform;
import org.eclipse.osgi.internal.loader.ModuleClassLoader;
import org.eclipse.osgi.internal.loader.classpath.ClasspathEntry;
import org.eclipse.osgi.internal.loader.classpath.ClasspathManager;
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.osgi.storage.bundlefile.BundleFile;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IPerspectiveDescriptor;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.knime.core.node.NodeLogger;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.service.application.ApplicationHandle;

/**
 * This class contains some misc utility methods around basic Eclipse funtionality.
 *
 * @author Thorsten Meinl, KNIME AG, Zurich, Switzerland
 * @since 2.9
 */
@SuppressWarnings("restriction")
public final class EclipseUtil {

    private static final String CLASSIC_PERSPECTIVE_ID = "org.knime.workbench.ui.ModellerPerspective";

    private static final String KNIME_APPLICATION_ID = "org.knime.product.KNIME_APPLICATION";

    /**
     * Interface for filtering classes in {@link EclipseUtil#findClasses(ClassFilter, ClassLoader)}.
     *
     * @since 2.12
     */
    public interface ClassFilter {
        /**
         * Returns whether the given class should be included in the resulting list.
         *
         * @param clazz a class
         * @return <code>true</code> if it should be included, <code>false</code> otherwise
         */
        boolean accept(Class<?> clazz);
    }

    /**
     * Class filter that checks for presence of certain annotations.
     *
     * @since 2.12
     */
    public static class AnnotationClassFilter implements ClassFilter {
        private final List<Class<? extends Annotation>> m_annotations;

        /**
         * Creates a new annotation filter.
         *
         * @param annotations a list of annotations classes
         */
        public AnnotationClassFilter(final Class<? extends Annotation>... annotations) {
            m_annotations = new ArrayList<>(annotations.length);
            for (Class<? extends Annotation> cl : annotations) {
                m_annotations.add(cl);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean accept(final Class<?> clazz) {
            for (Class<? extends Annotation> a : m_annotations) {
                if (clazz.getAnnotation(a) != null) {
                    return true;
                }
            }
            return false;
        }
    }

    private static final boolean RUN_FROM_SDK;
    private static final boolean RUN_IN_DEBUG;

    static {
        boolean b = false;
        try {
            b = checkSDK();
        } catch (RuntimeException ex) {
            NodeLogger.getLogger("org.knime.core.util.EclipseUtil").error(
                "Could not determine if we are run from the SDK: " + ex.getMessage(), ex);
            throw ex;
        } finally {
            RUN_FROM_SDK = b;
        }

        boolean d = false;
        try {
            d = checkDebug();
        } catch (RuntimeException ex) {
            NodeLogger.getLogger("org.knime.core.util.EclipseUtil").error(
                "Could not determine if we are running in debug mode: " + ex.getMessage(), ex);
            throw ex;
        } finally {
            RUN_IN_DEBUG = d;
        }
    }

    private EclipseUtil() {
    }

    /**
     * Returns the whether the current application has been started from an SDK or if it is a standalone instance. Note
     * that this method only guesses based on some indications and may be wrong (i.e. it returns <code>false</code> even
     * though the application has been started from the SDK) in some rare cases.
     *
     * @return <code>true</code> if the application has been started from an SDK, <code>false</code> otherwise
     */
    public static boolean isRunFromSDK() {
        return RUN_FROM_SDK;
    }

    /**
     * Returns the whether the current application has been started in debug or production mode. Note
     * that this method only guesses based on some indications and may be wrong (i.e. it returns <code>false</code> even
     * though the application has been started in debug) in some rare cases.
     *
     * @return <code>true</code> if the application has been started in debug mode, <code>false</code> otherwise
     * @since 2.12
     */
    public static boolean isRunInDebug() {
        return RUN_IN_DEBUG;
    }

    private static boolean checkSDK() {
        Location installLocation = Platform.getInstallLocation();
        if (installLocation == null) {
            return true;
        }
        Location configurationLocation = Platform.getConfigurationLocation();
        if (configurationLocation == null) {
            return true;
        }

        return configurationLocation.getURL().getPath().contains("/.metadata/.plugins/org.eclipse.pde.core/");
    }

    private static boolean checkDebug() {
        for (String arg : ManagementFactory.getRuntimeMXBean().getInputArguments()) {
            if (arg.startsWith("-agentlib:jdwp")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Searches the given class loader for classes that match the given class filter. This method is capable of
     * searching through Eclipse plug-ins but it will not recursivly search dependencies.
     *
     * @param filter a filter for classes
     * @param classLoader the class loader that should be used for searching
     * @return a collection with matching classes
     * @throws IOException if an I/O error occurs while scanning the class path
     * @since 2.12
     */
    public static Collection<Class<?>> findClasses(final ClassFilter filter, final ClassLoader classLoader)
        throws IOException {
        List<URL> classPathUrls = new ArrayList<>();

        if (classLoader instanceof ModuleClassLoader) {
            ModuleClassLoader cl = (ModuleClassLoader)classLoader;
            ClasspathManager cpm = cl.getClasspathManager();
            // String classPath = this.getClass().getName().replace(".", "/") + ".class";

            for (ClasspathEntry e : cpm.getHostClasspathEntries()) {
                BundleFile bf = e.getBundleFile();
                classPathUrls.add(bf.getEntry("").getLocalURL());
            }
        } else if (classLoader instanceof URLClassLoader) {
            URLClassLoader cl = (URLClassLoader)classLoader;
            for (URL u : cl.getURLs()) {
                classPathUrls.add(u);
            }
        } else {
            FileLocator.toFileURL(classLoader.getResource(""));
        }

        // filter classpath for nested entries
        for (Iterator<URL> it = classPathUrls.iterator(); it.hasNext();) {
            URL url1 = it.next();
            for (URL url2 : classPathUrls) {
                if ((url1 != url2) && url2.getPath().startsWith(url1.getPath())) {
                    it.remove();
                    break;
                }
            }
        }

        List<Class<?>> classes = new ArrayList<>();
        for (URL url : classPathUrls) {
            if ("file".equals(url.getProtocol())) {
                String path = url.getPath();
                // path = path.replaceFirst(Pattern.quote(classPath) + "$", "");
                collectInDirectory(new File(path), "", filter, classes, classLoader);
            } else if ("jar".equals(url.getProtocol())) {
                String path = url.getPath().replaceFirst("^file:", "").replaceFirst("\\!.+$", "");
                collectInJar(new JarFile(path), filter, classes, classLoader);
            } else {
                throw new IllegalStateException("Cannot read from protocol '" + url.getProtocol() + "'");
            }
        }

        return classes;
    }

    /**
     * Recursively Collects and returns all classes (excluding inner classes) that are in the specified directory.
     *
     * @param directory the directory
     * @param packageName the package name, initially the empty string
     * @param classNames a list that is filled with class names
     */
    private static void collectInDirectory(final File directory, final String packageName, final ClassFilter filter,
        final List<Class<?>> classes, final ClassLoader classLoader) {
        for (File f : directory.listFiles()) {
            if (f.isDirectory()) {
                collectInDirectory(f, packageName + f.getName() + ".", filter, classes, classLoader);
            } else if (f.getName().endsWith(".class")) {
                String className = packageName + f.getName().replaceFirst("\\.class$", "");
                try {
                    Class<?> cl = classLoader.loadClass(className);
                    if (filter.accept(cl)) {
                        classes.add(cl);
                    }
                } catch (ClassNotFoundException ex) {
                    NodeLogger.getLogger(EclipseUtil.class).error(
                        "Could not load class '" + className + "': " + ex.getMessage(), ex);
                }
            }
        }
    }

    /**
     * Collects and returns all classes inside the given JAR file (excluding inner classes).
     *
     * @param jar the jar file
     * @param classNames a list that is filled with class names
     * @throws IOException if an I/O error occurs
     */
    private static void collectInJar(final JarFile jar, final ClassFilter filter, final List<Class<?>> classes,
        final ClassLoader classLoader) throws IOException {
        Enumeration<JarEntry> entries = jar.entries();
        while (entries.hasMoreElements()) {
            JarEntry e = entries.nextElement();
            String className = e.getName();
            if (className.endsWith(".class")) {
                className = className.replaceFirst("\\.class$", "").replace('/', '.');
                try {
                    Class<?> cl = classLoader.loadClass(className);
                    if (filter.accept(cl)) {
                        classes.add(cl);
                    }
                } catch (ClassNotFoundException ex) {
                    NodeLogger.getLogger(EclipseUtil.class).error(
                        "Could not load class '" + className + "': " + ex.getMessage(), ex);
                }
            }
        }
        jar.close();
    }

    private static Optional<String> getApplicationID() {
        final var coreBundle = FrameworkUtil.getBundle(EclipseUtil.class);
        if (coreBundle != null) {
            final BundleContext ctx = coreBundle.getBundleContext();
            final ServiceReference<ApplicationHandle> ser = ctx.getServiceReference(ApplicationHandle.class);
            if (ser != null) {
                try {
                    final ApplicationHandle appHandle = ctx.getService(ser);
                    return Optional.ofNullable(appHandle == null ? null : appHandle.getInstanceId());
                } finally {
                    ctx.ungetService(ser);
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Checks whether this KNIME instance runs as an RMI application on the server.
     *
     * @return <code>true</code> if we are running on the server, <code>false</code> otherwise
     * @since 2.12
     */
    public static boolean determineServerUsage() {
        return getApplicationID().filter(id -> id.contains("KNIME_REMOTE_APPLICATION")).isPresent();
    }

    /**
     * Checks whether this KNIME instance runs as a local Analytics Platform.
     *
     * @return <code>true</code> if we are running as local AP, <code>false</code> otherwise
     * @since 5.4
     */
    public static boolean determineAPUsage() {
        return getApplicationID().filter(KNIME_APPLICATION_ID::equals).isPresent();
    }

    /**
     * Checks whether this KNIME instance is using the Classic UI perspective.
     *
     * @return <code>true</code> if the Classic UI is active, <code>false</code> otherwise
     * @since 5.4
     */
    public static boolean determineClassicUIUsage() {
        return PlatformUI.isWorkbenchRunning() && Display.getDefault().syncCall(() -> { // NOSONAR
            IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
            if (window != null) {
                final IWorkbenchPage page = window.getActivePage();
                if (page != null) {
                    final IPerspectiveDescriptor perspective = page.getPerspective();
                    return perspective != null && CLASSIC_PERSPECTIVE_ID.equals(perspective.getId());
                }
            }
            return false;
        });
    }

    /**
     * Determines which UI perspective is opened (if any).
     *
     * @return either {@code "classic"} or {@code "modern"} if called in a local AP, empty {@link Optional} otherwise
     * @since 5.4
     */
    public static Optional<String> currentUIPerspective() {
        return determineAPUsage() ? Optional.of(determineClassicUIUsage() ? "classic" : "modern") // NOSONAR
            : Optional.empty();
    }
}
