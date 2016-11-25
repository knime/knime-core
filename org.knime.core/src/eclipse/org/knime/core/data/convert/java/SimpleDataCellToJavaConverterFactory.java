/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 */

package org.knime.core.data.convert.java;

import org.knime.core.data.DataValue;

/**
 * Easy to use implementation of {@link DataCellToJavaConverterFactory} for simple Converters. All conversion processes
 * which do not require a state (execution context and/or DataCellFactory instance) should be considerable as simple,
 * all other should implement the extension point described in {@link org.knime.core.data.convert}.
 *
 * <p>
 * <b>Example:</b><br>
 *
 * <pre>
 * public class FooConverterFactory
 *          extends SimpleDataCellToJavaConverterFactory&lt;FooDataValue, Foo> {
 *
 *      // Constructor
 *      public FooConverterFactory() {
 *          super(FooDataValue.class, // class of source type
 *                Foo.class,          // class of destination type
 *                (fooVal) -> fooVal.getFooValue() // conversion function
 *          );
 *      )
 * }
 * </pre>
 * </p>
 *
 * @author Jonathan Hale, KNIME, Konstanz, Germany
 * @param <S> Source type which can be converted by converters created by this factory
 * @param <D> Destination type which can be converted by converters created by this factory
 * @since 3.2
 */
public class SimpleDataCellToJavaConverterFactory<S extends DataValue, D> implements DataCellToJavaConverterFactory<S, D> {

    private final Class<S> m_sourceType;

    private final Class<D> m_destType;

    private final DataCellToJavaConverter<S, D> m_converter;

    private final String m_name;

    /**
     * Creates a new converter factory.
     *
     * @param sourceType Type the created {@link DataCellToJavaConverter}s convert from
     * @param destType Type the created {@link DataCellToJavaConverter}s convert to
     * @param converter Implementation (possibly a Lambda expression) of the {@link DataCellToJavaConverter} defining
     *            the single instance returned by {@link #create()}.
     */
    public SimpleDataCellToJavaConverterFactory(final Class<S> sourceType, final Class<D> destType,
        final DataCellToJavaConverter<S, D> converter) {
        this(sourceType, destType, converter, "");
    }

    /**
     * Creates a new converter factory.
     *
     * @param sourceType Type the created {@link DataCellToJavaConverter}s convert from
     * @param destType Type the created {@link DataCellToJavaConverter}s convert to
     * @param converter Implementation (possibly a Lambda expression) of the {@link DataCellToJavaConverter} defining
     *            the single instance returned by {@link #create()}.
     * @param name Name for this factory
     */
    public SimpleDataCellToJavaConverterFactory(final Class<S> sourceType, final Class<D> destType,
        final DataCellToJavaConverter<S, D> converter, final String name) {
        m_sourceType = sourceType;
        m_destType = destType;
        m_converter = converter;
        m_name = name;
    }

    @Override
    public DataCellToJavaConverter<S, D> create() {
        return m_converter;
    }

    @Override
    public Class<S> getSourceType() {
        return m_sourceType;
    }

    @Override
    public Class<D> getDestinationType() {
        return m_destType;
    }

    @Override
    public String getName() {
        return m_name;
    }

    @Override
    public String getIdentifier() {
        return getClass().getName() + "(" + m_sourceType.getSimpleName() + "," + m_destType.toString() + "," + m_name
            + ")";
    }
}
