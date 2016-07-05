/**
 * <h1>KNIME Converter Framework</h1>
 *
 * <h2>Introduction</h2>
 * <p>
 * This package contains classes to dynamically box and unbox KNIME types from and to Java types. A extension point is
 * provided to allow implementors of custom KNIME data types to provide compatibility with nodes like the Java Snippet
 * node, which use the KNIME Converter Framework.
 * </p>
 *
 * <h2>Packages</h2>
 * <p>
 * <ul>
 * <li>{@link org.knime.core.data.convert.datacell} - For converting from DataCell to Java types.</li>
 * <li>{@link org.knime.core.data.convert.java} - For converting from Java types to DataCell.</li>
 * </ul>
 * </p>
 *
 * <h2>Structure</h2>
 * <p>
 * Each of the aforementioned packages contain a <code>*ConverterRegistry</code> class. All of the converters of the
 * respective types are registered and can be retrieved here.
 * </p>
 * <p>
 * For both of the converter types (<code>JavaToDataCell</code> and <code>DataCellToJava</code>) there are
 * <code>*ConverterFactory</code> classes which produce instances of <code>*Converter</code> classes respectively. This
 * is required since the converter instances may require information only available during node execution and therefore
 * cannot be instantiated before this point.
 * </p>
 *
 * <h2>Arrays/Collection Cells</h2>
 * <p>
 * If there is a Converter from e.g. <code>IntCell</code> to <code>Integer</code>, the framework can automatically
 * handle conversions form <code>ListCell(IntCell)</code> to <code>Integer[]</code> vice versa. Additionally there may
 * be explicit converters from a collection cell or array to a specific Java or DataCell type.
 * </p>
 *
 * @see org.knime.core.data.convert.datacell.JavaToDataCellConverterRegistry
 * @see org.knime.core.data.convert.datacell.DataCellToJavaConverterRegistry
 */
package org.knime.core.data.convert;
