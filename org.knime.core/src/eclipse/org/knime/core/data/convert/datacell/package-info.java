/**
 * <h1>DataCell to Java Converters</h1>
 *
 * <h2>Introduction</h2>
 * <p>
 * This package contains classes to unbox KNIME types to Java types. An extension point is provided to allow
 * implementors of custom KNIME data types to provide compatibility with nodes like the Java Snippet node, which use the
 * KNIME Converter Framework.
 * </p>
 *
 * <h2>Extending
 * <h2>
 *
 * <h3>Annotations</h3>
 * <p>
 * In many cases conversion merely requires a call to a <code>getFooValue()</code> method in your <code>FooValue</code>
 * interface. For this case, you just need to add the {@link org.knime.core.data.convert.DataValueAccessMethod}
 * annotation to this method and the framework will automatically create a converter from it.
 * </p>
 *
 * <p>
 * <b>Example:</b>
 *
 * <pre>
 * public interface FooValue extends ... {
 *
 *      // ...
 *
 *      &#64;DataValueAccessMethod(name = "Foo")
 *      public Foo getFooValue();
 *
 *      // ...
 * }
 * </pre>
 * </p>
 *
 * <h3>Extension Point</h3>
 * <p>
 * To enable conversion from your custom data type, you can also implement the Extension Point with the id defined in
 * {@link org.knime.core.data.convert.java.DataCellToJavaConverterRegistry#EXTENSION_POINT_ID}.
 * </p>
 * <p>
 * This usually involves implementing either
 * {@link org.knime.core.data.convert.java.SimpleDataCellToJavaConverterFactory} or
 * {@link org.knime.core.data.convert.java.DataCellToJavaConverterFactory} directly.
 * </p>
 *
 * @see org.knime.core.data.convert
 */
package org.knime.core.data.convert.datacell;