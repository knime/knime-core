/*
 * ------------------------------------------------------------------------
 *
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
 *   Jul 1, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.core.data.convert.map;

import java.util.List;
import java.util.function.Supplier;

import org.knime.core.data.DataType;
import org.knime.core.data.convert.map.Source.ProducerParameters;

/**
 * A {@link CellValueProducerFactory} that uses a {@link Supplier} to create new {@link CellValueProducer} instances.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @param <S> Type of source this producer can read from
 * @param <ET> Type of external types
 * @param <T> Java type the created producer returns
 * @param <PP> Subclass of {@link ProducerParameters} for the given source type
 * @since 4.3
 */
public final class SupplierCellValueProducerFactory<S extends Source<ET>, ET, T, PP extends ProducerParameters<S>>
    extends AbstractCellValueProducerFactory<S, ET, T, PP>
    implements TypedCellValueProducerFactory<S, ET, T, PP, CellValueProducer<S, T, PP>> {

    private final ET m_externalType;

    private final Class<?> m_destType;

    private final Supplier<CellValueProducer<S, T, PP>> m_producerSupplier;

    /**
     * Constructor
     *
     * @param externalType Identifier of the external type this producer reads
     * @param destType Target Java type
     * @param producerSupplier a supplier of producer functions
     */
    public SupplierCellValueProducerFactory(final ET externalType, final Class<?> destType,
        final Supplier<CellValueProducer<S, T, PP>> producerSupplier) {
        m_externalType = externalType;
        m_destType = destType;
        m_producerSupplier = producerSupplier;
    }

    @Override
    public String getIdentifier() {
        if (m_externalType instanceof DataType dt) {
            return dt.getIdentifier() + "->" + m_destType.getName();
        } else {
            return m_externalType.toString() + "->" + m_destType.getName();
        }
    }

    @Override
    public Iterable<String> getIdentifierAliases() {
        if (m_externalType instanceof DataType dt) {
            return List.of(dt.toLegacyString() + "->" + m_destType.getName());
        } else {
            return List.of();
        }
    }

    @Override
    public Class<?> getDestinationType() {
        return m_destType;
    }

    @Override
    public ET getSourceType() {
        return m_externalType;
    }

    @Override
    public Class<CellValueProducer<S, T, PP>> getProducerType() {
        @SuppressWarnings("unchecked")
        Class<CellValueProducer<S, T, PP>> producerType =
            (Class<CellValueProducer<S, T, PP>>)m_producerSupplier.getClass();
        return producerType;
    }

    @Override
    public CellValueProducer<S, T, PP> create() {
        return m_producerSupplier.get();
    }

}
