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

package org.knime.core.data.convert;

import java.util.Objects;

import org.knime.core.data.convert.datacell.JavaToDataCellConverterFactory;
import org.knime.core.data.convert.java.DataCellToJavaConverterFactory;

/**
 * Class which contains information to reference a {@link DataCellToJavaConverterFactory} or
 * {@link JavaToDataCellConverterFactory} by source and destination types. This class is for internal usage only and
 * should not be used by other plug-ins.
 *
 * @author Jonathan Hale, KNIME, Konstanz, Germany
 * @since 3.2
 *
 * @noreference This class is not intended to be referenced by clients.
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
public final class ConversionKey {

    private final int m_hashCode;

    private final Class<?> m_sourceType;

    private final Object m_destType;

    /**
     * Create from source and destination type.
     *
     * @param sourceType Source type the referenced factory should be able to handle; must not be <code>null</code>
     * @param destType Destination type the referenced factory should be able to handle; must not be <code>null</code>
     */
    public ConversionKey(final Class<?> sourceType, final Object destType) {
        m_sourceType = sourceType;
        m_destType = destType;

        // precompute hashCode, since general use cases involve at least one call to hashCode()
        final int prime = 31;
        m_hashCode = prime * (prime + sourceType.hashCode()) + destType.hashCode();
    }

    /**
     * Create from an existing factory.
     *
     * @param factory The existing factory which should be referenced by this key
     */
    public ConversionKey(final DataCellToJavaConverterFactory<?, ?> factory) {
        this(factory.getSourceType(), factory.getDestinationType());
    }

    /**
     * Create from an existing factory.
     *
     * @param factory The existing factory which should be referenced by this key
     */
    public ConversionKey(final JavaToDataCellConverterFactory<?> factory) {
        this(factory.getSourceType(), factory.getDestinationType());
    }

    @Override
    public int hashCode() {
        return m_hashCode;
    }

    /**
     * Get the source type of the factory this key refers to.
     *
     * @return the source type of the factory
     */
    public Class<?> getSourceType() {
        return m_sourceType;
    }

    /**
     * Get the destination type of the factory this key refers to.
     *
     * @return the destination type of the factory
     */
    public Object getDestType() {
        return m_destType;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        ConversionKey other = (ConversionKey)obj;
        return Objects.equals(this.m_destType, other.m_destType)
                && Objects.equals(this.m_sourceType, other.m_sourceType);
    }
}
