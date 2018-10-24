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
 *   Oct 24, 2018 (marcel): created
 */
package org.knime.core.data.convert.map;

import org.knime.core.data.convert.map.Destination.ConsumerParameters;

/**
 * Consumer factory that is typed on the (base) type of the consumers that it creates. The type is exposed via
 * {@link #getConsumerType()}. It is guaranteed that all created consumers are assignment-compatible with this type.
 * This allows clients to check with which specific consumer type they will be dealing.
 *
 * @param <D> Type of {@link Destination} to which consumers created by this factory write.
 * @param <ET> Type of the external type.
 * @param <T> The Java type of the values that consumers created by this factory accept.
 * @param <CP> Subtype of {@link ConsumerParameters}} that can be used to configure the consumers created by this
 *            factory.
 * @param <C> The (base) type of the consumers created by this factory.
 * @since 3.7
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @see CellValueConsumerFactory
 */
public interface TypedCellValueConsumerFactory<D extends Destination<?>, T, ET, CP extends ConsumerParameters<D>, //
        C extends CellValueConsumer<D, T, CP>>
    extends CellValueConsumerFactory<D, T, ET, CP> {

    /**
     * @return The (base) type of the consumers created by this consumer factory.
     */
    Class<C> getConsumerType();

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc} - It is guaranteed that the created consumer is assignment-compatible with the type
     *         returned by {@link #getConsumerType()}.
     */
    @Override
    C create();
}
