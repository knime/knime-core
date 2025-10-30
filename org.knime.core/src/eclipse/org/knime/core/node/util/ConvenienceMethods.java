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
 * History
 *   Mar 1, 2008 (wiswedel): created
 */
package org.knime.core.node.util;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;

import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.NodeLogger;
import org.knime.core.util.Pair;

/**
 * Collection of methods that are useful in different contexts.
 * @author Bernd Wiswedel, University of Konstanz
 */
public final class ConvenienceMethods {

    private static final NodeLogger LOGGER =
        NodeLogger.getLogger(ConvenienceMethods.class);

    private ConvenienceMethods() {
        // no op
    }

    /** Determines if both arguments are equal according to their equals
     * method (assumed to be symmetric). This method handles null arguments.
     * @param o1 First object for comparison, may be <code>null</code>.
     * @param o2 Second object for comparison, may be <code>null</code>.
     * @return If both arguments are equal
     * (if either one is null, so must be the other one)
     * @deprecated use {@link Objects#equals(Object, Object)} instead
     */
    @Deprecated
    public static boolean areEqual(final Object o1, final Object o2) {
        if (o1 == o2) {
            // same object or both are null
            return true;
        }
        if (o1 != null) {
            return o1.equals(o2);
        } else {
            // o2 != null && o1 == null
            return false;
        }
    }

    /** Read system property "line.separator", returns '\n' if that fails
     * (whereby a LOG message is reported).
     * @return The system line separator, never null.
     */
    public static String getLineSeparator() {
        String lineSep;
        try {
            lineSep = System.getProperty("line.separator");
            if (lineSep == null || lineSep.isEmpty()) {
                throw new RuntimeException("line separator must not be empty");
            }
        } catch (Throwable e) {
            LOGGER.error("Unable to get \"line.separator\" from system, "
                    + "using \"\\n\"", e);
            lineSep = "\n";
        }
        return lineSep;
    }

    /** Read system property <code>envVar</code> that is supposed to be of
     * a format such as '1024' (1024 bytes), '5K' (5 kilobyte),
     * '2M' (2 megabyte). If that fails (not parsable or negative), it will
     * return the the default value and log an error message.
     * @param envVar The name of the variable, must not be null (NPE)
     * @param defaultValue The default value (in bytes)
     * @return The size parameters in bytes or the defaultValue if the value
     *         can't be parsed.
     */
    public static long readSizeSystemProperty(
            final String envVar, final long defaultValue) {
        long size = defaultValue;
        String property = System.getProperty(envVar);
        if (property != null) {
            String s = property.trim();
            int multiplier = 1;
            if (s.endsWith("m") || s.endsWith("M")) {
                s = s.substring(0, s.length() - 1);
                multiplier = 1024 * 1024;
            } else if (s.endsWith("k") || s.endsWith("K")) {
                s = s.substring(0, s.length() - 1);
                multiplier = 1024;
            }
            try {
                int newSize = Integer.parseInt(s);
                if (newSize < 0) {
                    throw new NumberFormatException("Size < 0" + newSize);
                }
                size = newSize * multiplier;
                LOGGER.debug("Setting min blob size for SDF cells to "
                        + size + " bytes");
            } catch (NumberFormatException e) {
                LOGGER.warn("Unable to parse property " + envVar
                        + ", using default", e);
            }
        }
        return size;
    }

    /** Get string summary from argument collection for printing in warning messages,
     * see {@link #getShortStringFrom(Iterator, int, int)} for details.
     * @param objs The non null array to summarize
     * @param maxToPrint length to print, rest will be cut.
     * @return Such a short string summary.
     * @since 2.7*/
    public static String getShortStringFrom(final Collection<?> objs, final int maxToPrint) {
        return getShortStringFrom(objs.iterator(), objs.size(), maxToPrint);
    }

    /** Get string summary from argument iterator for printing in warning messages,
     * see {@link #getShortStringFrom(Iterator, int, int, Pair)} for details.
     * @param it The non null (0-position) iterator
     * @param length Length of the underlying collection
     * @param maxToPrint length to print, rest will be cut.
     * @return Such a short string summary.
     * @since 3.2 */
    public static String getShortStringFrom(final Iterator<?> it, final int length, final int maxToPrint) {
        return getShortStringFrom(it, length, maxToPrint, Pair.create("\"", "\""));
    }

    /**
     * Get string summary from argument iterator for printing in warning messages.
     * Here are some examples with maxToPrint = 3:
     * <pre>
     * [foo, bar, foobar, barfoo, barfuss] -&gt; "foo", "bar", "foobar", ... &lt;1 more&gt;
     * [foo, bar] -&gt; "foo", "bar"
     * </pre>
     * The delimiters can be added for the left and right enclosure of each printed object.
     * The other `getShortString` methods will default to quotes.
     *
     * @param it the non null (0-position) iterator
     * @param length Length of the underlying collection
     * @param maxToPrint length to print, rest will be cut
     * @param enclosures the left and right enclosure for each printed object
     * @return a short string summary
     * @since 5.9
     */
    public static String getShortStringFrom(final Iterator<?> it, final int length, final int maxToPrint,
        final Pair<String, String> enclosures) {
        final var b = new StringBuilder();
        var l = 0;
        while (it.hasNext()) {
            Object o = it.next();
            if (l > 0) {
                b.append(", ");
            }
            if (l < maxToPrint) {
                b.append(enclosures.getFirst());
                b.append(o == null ? "<null>" : o);
                b.append(enclosures.getSecond());
            } else {
                b.append(" ... <").append(length - maxToPrint).append(" more>");
                break;
            }
            l++;
        }
        return b.toString();
    }

    /**
     * Returns all generic superclasses extended by the given class.
     *
     * @param clazz any class object, must not be <code>null</code>
     * @return a (possibly empty) collection with generic classes
     * @since 3.0
     */
    public static Collection<Type> getAllGenericSuperclasses(final Class<?> clazz) {
        List<Type> l = new ArrayList<>();
        getAllGenericSuperclasses(clazz, l);
        return l;
    }

    private static void getAllGenericSuperclasses(final Class<?> clazz, final List<Type> types) {
        if (clazz == null) {
            return;
        }

        types.add(clazz.getGenericSuperclass());
        if (clazz.getGenericSuperclass() instanceof Class) {
            getAllGenericSuperclasses((Class<?>) clazz.getGenericSuperclass(), types);
        }
    }

    /**
     * Returns all generic interfaces implemented by the given class and its superclasses.
     *
     * @param clazz any class object, must not be <code>null</code>
     * @return a (possibly empty) collection with generic interfaces
     * @since 3.0
     */
    public static Collection<Type> getAllGenericInterfaces(final Class<?> clazz) {
        Set<Type> l = new LinkedHashSet<>();
        getAllGenericInterfaces(clazz, l);
        return l;
    }

    private static void getAllGenericInterfaces(final Class<?> clazz, final Set<Type> types) {
        if (clazz == null) {
            return;
        }

        for (Type t : clazz.getGenericInterfaces()) {
            types.add(t);
        }

        for (Class<?> iface : clazz.getInterfaces()) {
            getAllGenericInterfaces(iface, types);
        }

        getAllGenericInterfaces(clazz.getSuperclass(), types);
    }

    /**
     * Checks if the number of rows is greater than {@link Integer#MAX_VALUE} and throws an exception in this case.
     *
     * @param table any buffered data table
     * @throws UnsupportedOperationException if the number of rows is greater than {@link Integer#MAX_VALUE}
     * @since 3.0
     */
    public static void checkTableSize(final BufferedDataTable table) {
        checkTableSize(table.size());
    }

    /**
     * Checks if the number of rows is greater than {@link Integer#MAX_VALUE} and throws an exception in this case.
     *
     * @param rowCount the row count
     * @return the row count as int (if below {@link Integer#MAX_VALUE})
     * @throws UnsupportedOperationException if the number of rows is greater than {@link Integer#MAX_VALUE}
     * @since 3.0
     */
    public static int checkTableSize(final long rowCount) {
        if (rowCount > Integer.MAX_VALUE) {
            throw new UnsupportedOperationException(
                "This node does not support more than " + Integer.MAX_VALUE + " rows.");
        }
        return (int) rowCount;
    }

    /**
     * This methods creates a predicate for use in a stream's filter operation (e.g.). It will filter duplicates based
     * on the given function.<br />
     * Example usage:
     * <code>
     * List<Person> persons = ...;
     * List<Person> distinctByFirstName = persons.stream()
     *    filter(distinctByKey(p -> p.getFirstName())).collect(Collectors.toList());
     * </code>
     *
     * @param keyExtractor a function that extracts the unqiue key from the object
     * @return a predicate
     * @since 3.2
     */
    public static <T> Predicate<T> distinctByKey(final Function<? super T, Object> keyExtractor) {
        Map<Object, Boolean> seen = new ConcurrentHashMap<>();
        return t -> seen.putIfAbsent(keyExtractor.apply(t), Boolean.TRUE) == null;
    }
}
