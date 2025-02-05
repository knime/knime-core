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
 *   04.06.2018 (Jonathan Hale): created
 */
package org.knime.core.data.convert.map;

import java.util.List;

import org.knime.core.data.DataType;
import org.knime.core.data.convert.map.Destination.ConsumerParameters;

/**
 * Simple implementation of {@link CellValueConsumerFactory} that allows passing the consumption procedure as a lambda
 * expression.
 *
 * @author Jonathan Hale, KNIME, Konstanz, Germany
 * @param <D> Type of destination the consumer can write to
 * @param <T> Java type the created consumer is able to accept
 * @param <ET> Type of external types
 * @param <CP> Subclass of {@link ConsumerParameters} for given destination type
 *
 * @since 3.6
 */
public class SimpleCellValueConsumerFactory<D extends Destination<ET>, T, ET, CP extends ConsumerParameters<D>>
    extends AbstractCellValueConsumerFactory<D, T, ET, CP>
    implements TypedCellValueConsumerFactory<D, T, ET, CP, CellValueConsumer<D, T, CP>> {

    final ET m_externalType;

    final Class<?> m_sourceType;

    final CellValueConsumer<D, T, CP> m_consumer;

    /**
     * Constructor
     *
     * @param sourceType Class of the type the created consumer accepts
     * @param externalType Identifier of the external type this consumer writes as
     * @param consumer The consumer function (e.g. as lambda expression)
     */
    public SimpleCellValueConsumerFactory(final Class<?> sourceType, final ET externalType,
        final CellValueConsumer<D, T, CP> consumer) {
        m_sourceType = sourceType;
        m_externalType = externalType;
        m_consumer = consumer;
    }

    @Override
    public String getIdentifier() {
        if (m_externalType instanceof IdentifiableType it) {
            return m_sourceType.getName() + "->" + it.getIdentifier();
        } else {
            return m_sourceType.getName() + "->" + m_externalType.toString();
        }
    }

    @Override
    public Iterable<String> getIdentifierAliases() {
        if (m_externalType instanceof DataType dt) {
            return List.of(m_sourceType.getName() + "->" + dt.toLegacyString());
        } else {
            return List.of();
        }
    }

    @Override
    public CellValueConsumer<D, T, CP> create() {
        return m_consumer;
    }

    @Override
    public ET getDestinationType() {
        return m_externalType;
    }

    @Override
    public Class<?> getSourceType() {
        return m_sourceType;
    }

    @Override
    public Class<CellValueConsumer<D, T, CP>> getConsumerType() {
        @SuppressWarnings("unchecked")
        final Class<CellValueConsumer<D, T, CP>> consumerType =
            (Class<CellValueConsumer<D, T, CP>>)m_consumer.getClass();
        return consumerType;
    }
}