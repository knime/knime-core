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

package org.knime.core.data.convert.datacell;

import java.io.InputStream;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.blob.BinaryObjectCellFactory;
import org.knime.core.data.blob.BinaryObjectDataCell;
import org.knime.core.data.convert.java.DataCellToJavaConverter;
import org.knime.core.node.ExecutionContext;

/**
 * Default implementation of {@link JavaToDataCellConverterFactory}. Supports conversions from byte[], InputStream and
 * Byte to {@link BinaryObjectDataCell} .
 *
 * @author Jonathan Hale
 *
 * @param <T> {@link JavaToDataCellConverter} subclass which can be created by this factory.
 * @since 3.2
 * @noreference This class is not intended to be referenced by clients.
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
public final class JavaToBinaryObjectCellConverterFactory<T> implements JavaToDataCellConverterFactory<T> {

    private final Class<T> m_sourceType;

    /**
     * Constructor
     *
     * @param sourceType class of <T> which can be converted to {@link BinaryObjectDataCell} by created
     *            {@link DataCellToJavaConverter Converters}
     */
    protected JavaToBinaryObjectCellConverterFactory(final Class<T> sourceType) {
        m_sourceType = sourceType;

        if (!sourceType.equals(byte[].class) && !sourceType.equals(InputStream.class)
            && !sourceType.equals(Byte.class)) {
            // source type can only be byte[] or InputStream
            throw new IllegalArgumentException("Invalid source type.");
        }
    }

    @SuppressWarnings("unchecked")
    // m_sourceType has to be Class<T>, therefore we know what T is at Runtime
    @Override
    public JavaToDataCellConverter<T> create(final ExecutionContext context) {
        if (m_sourceType.isAssignableFrom(InputStream.class)) {
            return (JavaToDataCellConverter<T>)new JavaToDataCellConverter<InputStream>() {

                private final BinaryObjectCellFactory m_factory = new BinaryObjectCellFactory(context);

                @Override
                public DataCell convert(final InputStream source) throws Exception {
                    return m_factory.create(source);
                }
            };
        } else if (m_sourceType.isAssignableFrom(byte[].class)) {
            return (JavaToDataCellConverter<T>)new JavaToDataCellConverter<byte[]>() {

                private final BinaryObjectCellFactory m_factory = new BinaryObjectCellFactory(context);

                @Override
                public DataCell convert(final byte[] source) throws Exception {
                    return m_factory.create(source);
                }
            };
        } else if (m_sourceType.isAssignableFrom(Byte.class)) {
            return (JavaToDataCellConverter<T>)new JavaToDataCellConverter<Byte>() {

                private final BinaryObjectCellFactory m_factory = new BinaryObjectCellFactory(context);

                @Override
                public DataCell convert(final Byte source) throws Exception {
                    return m_factory.create(new byte[]{source});
                }
            };
        }

        throw new IllegalArgumentException("Invalid source type.");
    }

    @Override
    public Class<T> getSourceType() {
        return m_sourceType;
    }

    @Override
    public DataType getDestinationType() {
        return BinaryObjectDataCell.TYPE;
    }

    @Override
    public String getIdentifier() {
        return getClass().getName() + "(" + m_sourceType.getName() + ")";
    }
}