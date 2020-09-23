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
 *   Sep 23, 2020 (dietzc): created
 */
package org.knime.core.data.container;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.values.DoubleWriteValue;
import org.knime.core.data.values.WriteValue;

/**
 * TODO
 *
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 * @since 4.3
 *
 * @noreference This interface is not intended to be referenced by clients.
 * @noextend This interface is not intended to be extended by clients.
 */
final class BufferedWriteValueRegistry {

    private static BufferedWriteValueRegistry INSTANCE = new BufferedWriteValueRegistry();

    private Map<DataType, Function<Consumer<DataCell>, WriteValue<?>>> m_registry;

    private BufferedWriteValueRegistry() {
        m_registry.put(DoubleCell.TYPE, (c) -> new DoubleCellWriteValue(c));
    }

    public final static BufferedWriteValueRegistry getInstance() {
        return INSTANCE;
    }

    public WriteValue<?> getWriteValue(final DataType type, final Consumer<DataCell> consumer) {
        Function<Consumer<DataCell>, WriteValue<?>> factory = m_registry.get(type);

        /* TODO this is the best we can do in case we don't have another converter available.
        * Big question is how do we guarantee that we have WriteValues for all supported
        * DataTypes and how does the user know that he can cast on these. For reading the user knows it as DataType guarantees the read access.
        * Can we do something similar for writing? How to make sure our 'mapping' is 'complete'?
        */
        if (factory == null) {
            factory = (c) -> new DataCellWriteValue(consumer);
        }
        return factory.apply(consumer);
    }

    private final static class DoubleCellWriteValue implements DoubleWriteValue {
        private final Consumer<DataCell> m_consumer;

        DoubleCellWriteValue(final Consumer<DataCell> consumer) {
            m_consumer = consumer;
        }

        @Override
        public void setValue(final DoubleValue value) {
            if (value instanceof DataCell) {
                m_consumer.accept((DataCell)value);
            } else {
                m_consumer.accept(new DoubleCell(value.getDoubleValue()));
            }
        }

        @Override
        public void setDoubleValue(final double value) {
            m_consumer.accept(new DoubleCell(value));
        }
    }

    private final static class DataCellWriteValue implements WriteValue<DataCell> {
        private final Consumer<DataCell> m_consumer;

        DataCellWriteValue(final Consumer<DataCell> consumer) {
            m_consumer = consumer;
        }

        @Override
        public void setValue(final DataCell value) {
            m_consumer.accept(value);
        }
    }
}
