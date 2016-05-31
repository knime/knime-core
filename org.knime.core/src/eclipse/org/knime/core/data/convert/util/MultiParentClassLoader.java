package org.knime.core.data.convert.util;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * ClassLoader which searches through multiple ClassLoaders. Important for dynamic uses of the converter framework.
 * Allows creating a class loader which knows classes of org.knime.core.data.convert and the user bundle.
 *
 * <p>
 * <b>Example:</b>
 *
 * <pre>
 * final ClassLoader loader =
 *     new ClassLoader(getClass().getClassLoader(), DataCellToJavaConverterRegistry.class.getClassLoader());
 * </pre>
 * <p>
 *
 * A call to {@link ClassLoader#loadClass(String)} will now first check the <code>getClass().getClassLoader()</code> and
 * if that does not contain the class we are trying to load, it will check
 * <code>DataCellToJavaConverterRegistry.class.getClassLoader()</code>.
 *
 * @author Jonathan Hale
 * @since 3.2
 */
public class MultiParentClassLoader extends ClassLoader {

    /*
     * List of class loaders which will be searched
     */
    private final ArrayList<ClassLoader> m_classLoaders;

    /**
     * @param classLoaders ClassLoaders to search in the given order when trying to find a class
     */
    public MultiParentClassLoader(final ClassLoader... classLoaders) {
        m_classLoaders = new ArrayList<>(Arrays.asList(classLoaders));
    }

    @Override
    public URL findResource(final String name) {
        for (final ClassLoader loader : m_classLoaders) {
            final URL resource = loader.getResource(name);

            if (resource != null) {
                return resource;
            }
        }
        return null;
    }

    @Override
    protected Class<?> findClass(final String name) throws ClassNotFoundException {
        for (final ClassLoader loader : m_classLoaders) {
            try {
                final Class<?> clazz = loader.loadClass(name);
                return clazz;
            } catch (final ClassNotFoundException e) {
                // thrown when loader cannot find the class.
            }
        }
        return super.findClass(name); // just throws ClassNotFoundException
    }

}