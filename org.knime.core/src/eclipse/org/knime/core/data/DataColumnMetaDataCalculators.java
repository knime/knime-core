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
 *   Oct 11, 2019 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.core.data;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.knime.core.data.DataValue.UtilityFactory;
import org.knime.core.data.container.BlobWrapperDataCell;
import org.knime.core.data.meta.DataColumnMetaData;
import org.knime.core.data.meta.DataColumnMetaDataCreator;
import org.knime.core.data.meta.DataColumnMetaDataRegistry;
import org.knime.core.node.util.CheckUtils;

/**
 * Provides calculators that can calculate meta data from actual data.<br/>
 * This is done by retrieving the {@link DataColumnMetaDataCreator creators} for all {@link DataValue} interfaces the
 * {@link DataType} of the current column contains that declare that they have {@link DataColumnMetaData}. A
 * {@link DataValue} has {@link DataColumnMetaData} if its {@link UtilityFactory} returns {@code true} in
 * {@link UtilityFactory#hasMetaData()} in which case {@link UtilityFactory#getMetaDataCreator()} must return an
 * instance of {@link DataColumnMetaDataCreator}.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
final class DataColumnMetaDataCalculators {

    private DataColumnMetaDataCalculators() {
    }

    /**
     * Interface for objects that can be used to create {@link DataColumnMetaData} from actual data.
     *
     * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
     */
    interface MetaDataCalculator {

        /**
         * Updates the meta data with the contents in {@link DataCell cell} provided it is relevant for this meta data
         * calculator.<br>
         * Note that the calculator should ignore irrelevant cells but not fail!
         * Blob cells (discouraged nowadays) are handed in via {@link BlobWrapperDataCell}, so needs unwrapping via
         * instance check.
         *
         * @param cell the {@link DataCell} whose information should be incorporated into the meta data
         */
        void update(final DataCell cell);

        /**
         * Creates a {@link List} of {@link DataColumnMetaData} object corresponding to the information observed so
         * far.<br/>
         * The returned {@link DataColumnMetaData} must be independent of this {@link MetaDataCalculator} and subsequent
         * calls to {@link MetaDataCalculator#update(DataCell)} are allowed and must not change already created
         * {@link DataColumnMetaData}.
         *
         * @return the list of {@link DataColumnMetaData} corresponding to the information observed so far
         */
        List<DataColumnMetaData> createMetaData();
    }

    /**
     * Creates a {@link MetaDataCalculator} for {@link DataColumnSpec colSpec} and the provided options.
     *
     * @param colSpec the {@link DataColumnSpec} for which to create a {@link MetaDataCalculator}
     * @param dropMetaData whether the {@link DataColumnMetaData} stored in {@link DataColumnSpec colSpec} should be
     *            dropped
     * @param createMetaData whether new {@link DataColumnMetaData} should be created from the data
     * @return a {@link MetaDataCalculator} for {@link DataColumnSpec colSpec} that behaves according to the provided
     *         options
     */
    static MetaDataCalculator createCalculator(final DataColumnSpec colSpec, final boolean dropMetaData,
        final boolean createMetaData) {
        if (dropMetaData && !createMetaData) {
            return NullMetaDataCalculator.INSTANCE;
        }
        return new MetaDataCalculatorImpl(colSpec, !dropMetaData, createMetaData);
    }

    /**
     * Creates a copy of {@link MetaDataCalculator calculator} that contains the same information after the method
     * returns but is independent of {@link MetaDataCalculator calculator}.
     *
     * @param calculator the {@link MetaDataCalculator} to copy
     * @return a copy of {@link MetaDataCalculator calculator}
     */
    static MetaDataCalculator copy(final MetaDataCalculator calculator) {
        if (calculator == NullMetaDataCalculator.INSTANCE) {
            return NullMetaDataCalculator.INSTANCE;
        } else {
            assert calculator instanceof MetaDataCalculatorImpl : "Unknown MetaDataCalculator implementation "
                + calculator.getClass().getName();
            return new MetaDataCalculatorImpl((MetaDataCalculatorImpl)calculator);
        }
    }

    static void merge(final MetaDataCalculator first, final MetaDataCalculator second) {
        if (first == NullMetaDataCalculator.INSTANCE) {
            assert second == NullMetaDataCalculator.INSTANCE;
        } else {
            assert first instanceof MetaDataCalculatorImpl;
            assert second instanceof MetaDataCalculatorImpl;
            ((MetaDataCalculatorImpl)first).merge((MetaDataCalculatorImpl)second);
        }
    }

    /**
     * A dummy implementation of {@link MetaDataCalculator} that doesn't actually do any computation. Note that
     * attempting to merge the singleton with any object other than itself will cause an assertion error since this
     * indicates an implementation problem.
     *
     * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
     */
    private enum NullMetaDataCalculator implements MetaDataCalculator {
            INSTANCE;

        @Override
        public void update(final DataCell cell) {
            // do nothing
        }

        @Override
        public List<DataColumnMetaData> createMetaData() {
            return Collections.emptyList();
        }

    }

    /**
     * Implementation that actually creates {@link DataColumnMetaData}.
     *
     * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
     */
    private static class MetaDataCalculatorImpl implements MetaDataCalculator {

        private final Collection<DataColumnMetaDataCreator<?>> m_metaDataCreators;

        private final boolean m_updateMetaData;

        MetaDataCalculatorImpl(final DataColumnSpec spec, final boolean initializeWithSpec,
            final boolean updateMetaData) {
            m_metaDataCreators = DataColumnMetaDataRegistry.INSTANCE.getCreators(spec.getType());
            m_updateMetaData = updateMetaData;
            if (initializeWithSpec) {
                m_metaDataCreators
                    .forEach(m -> spec.getMetaDataOfType(m.getMetaDataClass()).ifPresent(o -> merge(m, o)));
            }
        }

        // the compatibility of creator and other is ensured at runtime
        @SuppressWarnings("unchecked")
        private static void merge(@SuppressWarnings("rawtypes") final DataColumnMetaDataCreator creator,
            final DataColumnMetaData other) {
            CheckUtils.checkState(creator.getMetaDataClass().isInstance(other),
                "Expected meta data of class '%s' but received meta data of class '%s'.",
                creator.getMetaDataClass().getName(), other.getClass().getName());
            creator.merge(other);
        }

        /**
         * Copies <b>toCopy</b> by also copying all {@link DataColumnMetaDataCreator DataValueMetaDataCreators} it
         * contains. This means that any later change to <b>toCopy</b> does NOT affect the newly created instance.
         *
         * @param toCopy the MetaDataCalculator to copy
         */
        MetaDataCalculatorImpl(final MetaDataCalculatorImpl toCopy) {
            m_metaDataCreators =
                toCopy.m_metaDataCreators.stream().map(DataColumnMetaDataCreator::copy).collect(Collectors.toList());
            m_updateMetaData = toCopy.m_updateMetaData;
        }

        @Override
        public void update(final DataCell cell) {
            if (m_updateMetaData && !m_metaDataCreators.isEmpty()) {
                DataCell unwrapped = cell instanceof BlobWrapperDataCell bwdc ? bwdc.getCell() : cell;
                m_metaDataCreators.forEach(c -> c.update(unwrapped));
            }
        }

        @Override
        public List<DataColumnMetaData> createMetaData() {
            return m_metaDataCreators.stream().map(DataColumnMetaDataCreator::create).collect(Collectors.toList());
        }

        @SuppressWarnings("unchecked")
        private void merge(final MetaDataCalculatorImpl other) {
            final Iterator<DataColumnMetaDataCreator<?>> otherCreators = other.m_metaDataCreators.iterator();
            for (DataColumnMetaDataCreator<?> creator : m_metaDataCreators) {
                assert otherCreators.hasNext();
                final DataColumnMetaDataCreator<?> otherCreator = otherCreators.next();
                assert creator.getClass().equals(otherCreator.getClass());
                creator.merge(creator.getClass().cast(otherCreator));
            }
        }
    }

}
