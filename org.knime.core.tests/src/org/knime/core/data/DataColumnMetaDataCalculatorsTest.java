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
 *   Oct 31, 2019 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.core.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.knime.core.data.DataColumnMetaDataCalculators.MetaDataCalculator;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.meta.DataColumnMetaData;
import org.knime.core.data.meta.TestDataColumnMetaData;

/**
 * Contains unit tests for the MetaDataCalculators class and its MetaDataCalculator implementations.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public class DataColumnMetaDataCalculatorsTest {

    private static final List<String> INITIAL_VALUES = Arrays.asList("X", "Y", "Z");

    private static final List<String> TEST_VALUES = Arrays.asList("A", "B", "C");

    private static final DataCell[] CELLS = TEST_VALUES.stream().map(StringCell::new).toArray(DataCell[]::new);

    private static final TestDataColumnMetaData INITIAL_META_DATA = new TestDataColumnMetaData(INITIAL_VALUES);

    private static DataColumnSpec createColSpec(final String name, final DataType type) {
        return new DataColumnSpecCreator(name, type).createSpec();
    }

    /**
     * Ensures that the MetaDataCalculator that drops the meta data and doesn't create new meta data is always the same
     * object.
     */
    public void testDropWithoutCreateSingleton() {
        MetaDataCalculator first =
            DataColumnMetaDataCalculators.createCalculator(createColSpec("first", StringCell.TYPE), true, false);
        MetaDataCalculator second =
            DataColumnMetaDataCalculators.createCalculator(createColSpec("second", StringCell.TYPE), true, false);
        assertTrue(first == second);
    }

    private static MetaDataCalculator createMdc(final boolean drop, final boolean create) {
        final DataColumnSpecCreator csc = new DataColumnSpecCreator("test", StringCell.TYPE);
        csc.addMetaData(INITIAL_META_DATA, true);
        return DataColumnMetaDataCalculators.createCalculator(csc.createSpec(), drop, create);
    }

    /**
     * Tests a MetaDataCalculator that drops the existing meta data and doesn't create new meta data.
     */
    public void testDropNoCreate() {
        final MetaDataCalculator mdc = createMdc(true, false);
        testMetaDataCalculator(mdc, Collections.emptyList());
    }

    /**
     * Tests a MetaDataCalculator that drops the existing meta data and creates new meta data.
     */
    public void testDropCreate() {
        final MetaDataCalculator mdc = createMdc(true, true);
        testMetaDataCalculator(mdc, Collections.singletonList(new TestDataColumnMetaData(TEST_VALUES)));
    }

    /**
     * Tests a MetaDataCalculator that is initialized with the existing meta data.
     */
    public void testNoDropCreate() {
        final MetaDataCalculator mdc = createMdc(false, true);
        final List<String> concat = new ArrayList<>(INITIAL_VALUES);
        concat.addAll(TEST_VALUES);
        testMetaDataCalculator(mdc, Collections.singletonList(new TestDataColumnMetaData(concat)));
    }

    /**
     * Tests a MetaDataCalculator that is initialized with the existing meta data but doesn't
     * update it.
     */
    public void testNoDropNoCreate() {
        final MetaDataCalculator mdc = createMdc(false, false);
        testMetaDataCalculator(mdc, Collections.singletonList(INITIAL_META_DATA));
    }

    /**
     * Test copying MetaDataCalculator objects.
     */
    public void testCopy() {
        // test the singleton property for the case where meta data is dropped and not recreated
        final MetaDataCalculator dropNoCreate = createMdc(true, false);
        assertEquals(dropNoCreate, DataColumnMetaDataCalculators.copy(dropNoCreate));

        final MetaDataCalculator noDropCreate = createMdc(false, true);
        final MetaDataCalculator copy = DataColumnMetaDataCalculators.copy(noDropCreate);
        assertEquals(noDropCreate.createMetaData(), copy.createMetaData());
        // test that the copy is independent of its origin
        noDropCreate.update(new StringCell("foo"));
        assertNotEquals(noDropCreate.createMetaData(), copy.createMetaData());
    }

    /**
     * Tests merging MetaDataCalculator objects.
     */
    public void testMerge() {
        final MetaDataCalculator noDropNoCreate = createMdc(false, false);
        final MetaDataCalculator dropCreate = createMdc(true, true);
        feed(dropCreate);
        final List<String> concat = new ArrayList<>(TEST_VALUES);
        concat.addAll(INITIAL_VALUES);
        DataColumnMetaDataCalculators.merge(dropCreate, noDropNoCreate);
        assertEquals(Collections.singletonList(new TestDataColumnMetaData(concat)), dropCreate.createMetaData());
    }


    private static void feed(final MetaDataCalculator mdc) {
        feed(mdc, CELLS);
    }

    private static void feed(final MetaDataCalculator mdc, final DataCell[] cells) {
        for (DataCell cell : cells) {
            mdc.update(cell);
        }
    }

    private static void testMetaDataCalculator(final MetaDataCalculator mdc, final List<DataColumnMetaData> expected) {
        feed(mdc);
        assertEquals(expected, mdc.createMetaData());
    }
}
