/**
 * <h1>Java to DataCell Converters</h1>
 *
 * <h2>Introduction</h2>
 * <p>
 * This package contains classes to box Java types into KNIME types. An extension point is provided to allow
 * implementors of custom KNIME data types to provide compatibility with nodes like the Java Snippet node, which use the
 * KNIME Converter Framework.
 * </p>
 *
 * <h2>Extending
 * <h2>
 *
 * <h3>Annotations</h3>
 * <p>
 * In many cases conversion merely requires a call to a <code>create(Foo value)</code> method in your
 * <code>FooCellFactory</code> class. For this case, you just need to add the
 * {@link org.knime.core.data.convert.DataCellFactoryMethod} annotation to this method and the framework will
 * automatically create a converter from it.
 * </p>
 *
 * <p>
 * <b>Example:</b>
 *
 * <pre>
 * public class FooClassFactory extends ... {
 *
 *      // ...
 *
 *      &#64;DataCellFactoryMethod(name = "Foo")
 *      public static DataCell create(final Foo value) {
 *          return new FooCell(value);
 *      }
 *
 *      // ...
 * }
 * </pre>
 * </p>
 *
 * <h3>Extension Point</h3>
 * <p>
 * To enable conversion from your custom data type, you can also implement the Extension Point with the id defined in
 * {@link org.knime.core.data.convert.java.JavaToDataCellConverterRegistry#EXTENSION_POINT_ID}.
 * </p>
 * <p>
 * This usually involves implementing either
 * {@link org.knime.core.data.convert.java.SimpleDataCellToJavaConverterFactory} or
 * {@link org.knime.core.data.convert.java.DataCellToJavaConverterFactory} directly.
 * </p>
 *
 * @see org.knime.core.data.convert
 */
package org.knime.core.data.convert.java;
