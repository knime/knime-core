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
 *   Oct 19, 2021 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.core.data.v2;

import java.util.Optional;

import org.knime.core.data.v2.value.BooleanListValueFactory;
import org.knime.core.data.v2.value.BooleanValueFactory;
import org.knime.core.data.v2.value.DictEncodedStringValueFactory;
import org.knime.core.data.v2.value.DoubleListValueFactory;
import org.knime.core.data.v2.value.DoubleValueFactory;
import org.knime.core.data.v2.value.IntListValueFactory;
import org.knime.core.data.v2.value.IntValueFactory;
import org.knime.core.data.v2.value.ListValueFactory;
import org.knime.core.data.v2.value.LongListValueFactory;
import org.knime.core.data.v2.value.LongValueFactory;
import org.knime.core.data.v2.value.StringListValueFactory;
import org.knime.core.table.schema.BooleanDataSpec;
import org.knime.core.table.schema.ByteDataSpec;
import org.knime.core.table.schema.DataSpec;
import org.knime.core.table.schema.DoubleDataSpec;
import org.knime.core.table.schema.DurationDataSpec;
import org.knime.core.table.schema.FloatDataSpec;
import org.knime.core.table.schema.IntDataSpec;
import org.knime.core.table.schema.ListDataSpec;
import org.knime.core.table.schema.LocalDateDataSpec;
import org.knime.core.table.schema.LocalDateTimeDataSpec;
import org.knime.core.table.schema.LocalTimeDataSpec;
import org.knime.core.table.schema.LongDataSpec;
import org.knime.core.table.schema.PeriodDataSpec;
import org.knime.core.table.schema.StringDataSpec;
import org.knime.core.table.schema.StructDataSpec;
import org.knime.core.table.schema.VarBinaryDataSpec;
import org.knime.core.table.schema.VoidDataSpec;
import org.knime.core.table.schema.ZonedDateTimeDataSpec;

/**
 * Provides default ValueFactories for common DataSpecs.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @noreference This class is not intended to be referenced by clients.
 * @since 4.5
 */
public final class DefaultValueFactories {

    /**
     * Provides the default {@link ValueFactory} for a {@link DataSpec} if such a default exists (in org.knime.core).
     *
     * @param dataSpec for which the default {@link ValueFactory} is needed
     * @return an {@link Optional} containing the default {@link ValueFactory} for {@link DataSpec dataSpec} or
     *         {@link Optional#empty()}
     */
    public static Optional<ValueFactory<?, ?>> getDefaultValueFactory(final DataSpec dataSpec) {//NOSONAR
        // TODO what about dataspecs where a default ValueFactory exists but doesn't live in core e.g. date & time?
        return dataSpec.accept(DefaultValueFactoryMapper.INSTANCE);
    }

    private enum DefaultValueFactoryMapper implements DataSpec.Mapper<Optional<ValueFactory<?, ?>>> {
            INSTANCE;

        @Override
        public Optional<ValueFactory<?, ?>> visit(final BooleanDataSpec spec) {
            return Optional.of(new BooleanValueFactory());
        }

        @Override
        public Optional<ValueFactory<?, ?>> visit(final ByteDataSpec spec) {
            return Optional.empty();
        }

        @Override
        public Optional<ValueFactory<?, ?>> visit(final DoubleDataSpec spec) {
            return Optional.of(new DoubleValueFactory());
        }

        @Override
        public Optional<ValueFactory<?, ?>> visit(final DurationDataSpec spec) {
            return Optional.empty();
        }

        @Override
        public Optional<ValueFactory<?, ?>> visit(final FloatDataSpec spec) {
            return Optional.empty();
        }

        @Override
        public Optional<ValueFactory<?, ?>> visit(final IntDataSpec spec) {
            return Optional.of(new IntValueFactory());
        }

        @Override
        public Optional<ValueFactory<?, ?>> visit(final LocalDateDataSpec spec) {
            return Optional.empty();
        }

        @Override
        public Optional<ValueFactory<?, ?>> visit(final LocalDateTimeDataSpec spec) {
            return Optional.empty();
        }

        @Override
        public Optional<ValueFactory<?, ?>> visit(final LocalTimeDataSpec spec) {
            return Optional.empty();
        }

        @Override
        public Optional<ValueFactory<?, ?>> visit(final LongDataSpec spec) {
            return Optional.of(new LongValueFactory());
        }

        @Override
        public Optional<ValueFactory<?, ?>> visit(final PeriodDataSpec spec) {
            return Optional.empty();
        }

        @Override
        public Optional<ValueFactory<?, ?>> visit(final VarBinaryDataSpec spec) {
            return Optional.empty();
        }

        @Override
        public Optional<ValueFactory<?, ?>> visit(final VoidDataSpec spec) {
            return Optional.empty();
        }

        @Override
        public Optional<ValueFactory<?, ?>> visit(final StructDataSpec spec) {
            return Optional.empty();
        }

        @Override
        public Optional<ValueFactory<?, ?>> visit(final ListDataSpec listDataSpec) {
            var inner = listDataSpec.getInner();
            var specificList = getSpecificValueFactory(inner);
            if (specificList != null) {
                return Optional.of(specificList);
            } else {
                return inner.accept(this).map(DefaultValueFactoryMapper::createListValueFactory);
            }
        }

        private static ListValueFactory createListValueFactory(final ValueFactory<?, ?> innerFactory) {
            var listValueFactory = new ListValueFactory();
            var elementType = ValueFactoryUtils.getDataTypeForValueFactory(innerFactory);
            listValueFactory.initialize(innerFactory, elementType);
            return listValueFactory;
        }

        private static ValueFactory<?, ?> getSpecificValueFactory(final DataSpec inner) {// NOSONAR
            if (inner == DataSpec.doubleSpec()) {
                return DoubleListValueFactory.INSTANCE;
            } else if (inner == DataSpec.intSpec()) {
                return IntListValueFactory.INSTANCE;
            } else if (inner == DataSpec.longSpec()) {
                return LongListValueFactory.INSTANCE;
            } else if (inner == DataSpec.stringSpec()) {
                return StringListValueFactory.INSTANCE;
            } else if (inner == DataSpec.booleanSpec()) {
                return BooleanListValueFactory.INSTANCE;
            } else {
                return null;
            }
        }

        @Override
        public Optional<ValueFactory<?, ?>> visit(final ZonedDateTimeDataSpec spec) {
            return Optional.empty();
        }

        @Override
        public Optional<ValueFactory<?, ?>> visit(final StringDataSpec spec) {
            return Optional.of(new DictEncodedStringValueFactory());
        }

    }


    private DefaultValueFactories() {

    }
}
