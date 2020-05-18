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
 *   May 18, 2020 (marcel): created
 */
package org.knime.core.data.convert.map.experimental;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

import org.knime.core.columnar.table.NullableWriteValue;
import org.knime.core.columnar.table.WriteValue;
import org.knime.core.columnar.table.type.DoubleType.DoubleWriteValue;
import org.knime.core.columnar.table.type.IntType.IntWriteValue;
import org.knime.core.columnar.table.type.StringType.StringWriteValue;
import org.knime.core.data.convert.map.MappingException;
import org.knime.core.util.Pair;

/**
 * @since 4.2
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 */
public final class FastTableValueConsumers {

    private FastTableValueConsumers() {}

    // Registry:

    public static final Map<Class<?>, Pair<Class<? extends WriteValue>, //
            Function<Class<? extends WriteValue>, CellValueConsumerNoDestination<?>>>> CONSUMER_REGISTRY;
    static {
        final Map<Class<?>, Pair<Class<? extends WriteValue>, //
                Function<Class<? extends WriteValue>, CellValueConsumerNoDestination<?>>>> consumerRegistry =
                    new LinkedHashMap<>();
        addConsumer(consumerRegistry, double.class, DoubleWriteValue.class, FastTableDoubleConsumer::new);
        addConsumer(consumerRegistry, int.class, IntWriteValue.class, FastTableIntConsumer::new);
        addConsumer(consumerRegistry, String.class, StringWriteValue.class, FastTableStringConsumer::new);
        CONSUMER_REGISTRY = Collections.unmodifiableMap(consumerRegistry);
    }

    private static <S, D extends WriteValue> void addConsumer(final Map<Class<?>, Pair<Class<? extends WriteValue>, //
            Function<Class<? extends WriteValue>, CellValueConsumerNoDestination<?>>>> consumerRegistry,
        final Class<S> sourceElementType, //
        final Class<D> destinationContainerType, //
        final Function<D, CellValueConsumerNoDestination<S>> consumerConstructor) {
        consumerRegistry.put(sourceElementType, new Pair(destinationContainerType, consumerConstructor));
    }

    // Consumer implementations:

    public static final class FastTableDoubleConsumer extends AbstractFastTableValueConsumer<Double, DoubleWriteValue>
        implements DoubleCellValueConsumerNoDestination {

        public FastTableDoubleConsumer(final DoubleWriteValue destination) {
            super(destination);
        }

        @Override
        public void consumeDoubleCellValue(final double value) throws MappingException {
            m_destination.setDouble(value);
        }
    }

    public static final class FastTableIntConsumer extends AbstractFastTableValueConsumer<Integer, IntWriteValue>
        implements IntCellValueConsumerNoDestination {

        public FastTableIntConsumer(final IntWriteValue destination) {
            super(destination);
        }

        @Override
        public void consumeIntCellValue(final int value) throws MappingException {
            m_destination.setInt(value);
        }
    }

    public static final class FastTableStringConsumer extends AbstractFastTableValueConsumer<String, StringWriteValue> {

        public FastTableStringConsumer(final StringWriteValue destination) {
            super(destination);
        }

        @Override
        public void consumeCellValue(final String value) throws MappingException {
            m_destination.setStringValue(value);
        }
    }

    private abstract static class AbstractFastTableValueConsumer<T, D extends NullableWriteValue>
        implements PrimitiveCellValueConsumerNoDestination<T> {

        protected final D m_destination;

        public AbstractFastTableValueConsumer(final D destination) {
            m_destination = destination;
        }

        @Override
        public final void consumeMissingCellValue() throws MappingException {
            m_destination.setMissing();
        }
    }
}
