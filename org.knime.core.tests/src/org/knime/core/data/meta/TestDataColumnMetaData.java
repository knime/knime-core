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
 *   Oct 29, 2019 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.core.data.meta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataValue;
import org.knime.core.data.StringValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.config.ConfigRO;
import org.knime.core.node.config.ConfigWO;

/**
 * A dummy implementation of {@link DataColumnMetaData} for testing purposes.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public final class TestDataColumnMetaData implements DataColumnMetaData {

    private final List<String> m_metaData;

    /**
     * Creates a {@link TestDataColumnMetaData} object that holds a copy of {@link List metaData}.
     *
     * @param metaData the meta data to encapsulate
     */
    public TestDataColumnMetaData(final List<String> metaData) {
        m_metaData = new ArrayList<>(metaData);
    }

    /**
     * Creates a {@link TestDataColumnMetaData} object that holds the provided <b>metaData</b>.
     *
     * @param metaData the meta data to encapsulate
     */
    public TestDataColumnMetaData(final String... metaData) {
        m_metaData = Arrays.asList(metaData);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (obj instanceof TestDataColumnMetaData) {
            return m_metaData.equals(((TestDataColumnMetaData)obj).m_metaData);
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return m_metaData.hashCode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return m_metaData.stream().collect(Collectors.joining(", "));
    }

    /**
     * Serializer for {@link TestDataColumnMetaData}.
     *
     * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
     */
    public static final class TestMetaDataSerializer implements DataColumnMetaDataSerializer<TestDataColumnMetaData> {

        /**
         * Config key used for the values of {@link TestDataColumnMetaData} when serialized to a {@link ConfigWO}.
         */
        public static final String CFG_TEST_SETTING = "test";

        /**
         * {@inheritDoc}
         */
        @Override
        public void save(final TestDataColumnMetaData metaData, final ConfigWO config) {
            config.addStringArray(CFG_TEST_SETTING, metaData.m_metaData.toArray(new String[0]));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public TestDataColumnMetaData load(final ConfigRO config) throws InvalidSettingsException {
            final String[] testSetting = config.getStringArray(CFG_TEST_SETTING);
            return new TestDataColumnMetaData(Arrays.asList(testSetting));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Class<TestDataColumnMetaData> getMetaDataClass() {
            return TestDataColumnMetaData.class;
        }

    }

    /**
     * {@link DataColumnMetaDataCreator} that creates {@link TestDataColumnMetaData} for testing purposes.
     *
     * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
     */
    public static final class TestMetaDataCreator implements DataColumnMetaDataCreator<TestDataColumnMetaData> {

        private List<String> m_metaData = new ArrayList<>();

        /**
         * {@inheritDoc}
         */
        @Override
        public Class<TestDataColumnMetaData> getMetaDataClass() {
            return TestDataColumnMetaData.class;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void update(final DataCell cell) {
            if (cell instanceof StringValue) {
                m_metaData.add(cell.toString());
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public TestDataColumnMetaData create() {
            return new TestDataColumnMetaData(m_metaData);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public DataColumnMetaDataCreator<TestDataColumnMetaData> copy() {
            final TestMetaDataCreator copy = new TestMetaDataCreator();
            copy.m_metaData = new ArrayList<>(m_metaData);
            return copy;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public TestMetaDataCreator merge(final TestDataColumnMetaData other) {
            m_metaData.addAll(other.m_metaData);
            return this;
        }

    }

    /**
     * Bundles {@link TestDataColumnMetaData} with {@link TestMetaDataSerializer} and {@link TestMetaDataCreator} for
     * testing purposes.
     *
     * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
     */
    public static final class TestMetaDataExtension implements DataColumnMetaDataExtension<TestDataColumnMetaData> {

        /**
         * {@inheritDoc}
         */
        @Override
        public Class<? extends DataValue> getDataValueClass() {
            return StringValue.class;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public DataColumnMetaDataCreator<TestDataColumnMetaData> create() {
            return new TestMetaDataCreator();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Class<TestDataColumnMetaData> getMetaDataClass() {
            return TestDataColumnMetaData.class;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public DataColumnMetaDataSerializer<TestDataColumnMetaData> createSerializer() {
            return new TestMetaDataSerializer();
        }

    }

}
