/*
 * ------------------------------------------------------------------------
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
 *   06.01.2014 (thor): created
 */
package org.knime.core.data;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.NodeSettings;

import junit.framework.Assert;

/**
 * Testcases for {@link AdapterCell}.
 *
 * @author Thorsten Meinl, KNIME AG, Zurich, Switzerland
 */
public class AdapterCellTest {
    public static class MyAdapterCell extends AdapterCell {
        public MyAdapterCell(final DataCell valueCell, final AdapterValue predefinedAdapters,
                             final Class<? extends DataValue>... valueClasses) {
            super(valueCell, predefinedAdapters, valueClasses);
        }

        public MyAdapterCell(final DataCell valueCell, final Class<? extends DataValue>... valueClasses) {
            super(valueCell, valueClasses);
        }

        public MyAdapterCell(final DataCellDataInput input) throws IOException {
            super(input);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected boolean equalsDataCell(final DataCell dc) {
            return getAdapterMap().equals(((MyAdapterCell) dc).getAdapterMap());
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int hashCode() {
            return 0;
        }
    }

    /**
     * Checks whether we can add missing values to an adapter via the constructor.
     *
     * @throws Exception if an error occurs
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testAddMissingValuesViaConstructor() throws Exception {
        MyAdapterCell c1 = new MyAdapterCell(new MissingCell("Something went wrong"), StringValue.class);

        MissingValue m1 = c1.getAdapterError(StringValue.class);
        assertThat("No missing value found for StringValue", m1, is(not(nullValue())));
        assertThat("Unexpected error message in missing value", m1.getError(), is("Something went wrong"));
    }

    /**
     * Checks whether we can add missing values to an adapter via
     * {@link AdapterCell#cloneAndAddAdapter(DataCell, Class...)}.
     *
     * @throws Exception if an error occurs
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testAddMissingValuesViaClone() throws Exception {
        MyAdapterCell c1 = new MyAdapterCell(new StringCell("Test"), StringValue.class);

        AdapterCell c2 = c1.cloneAndAddAdapter(new MissingCell("Something went wrong"), DoubleValue.class);
        MissingValue m2 = c2.getAdapterError(DoubleValue.class);
        assertThat("No missing value found for StringValue", m2, is(not(nullValue())));
        assertThat("Unexpected error message in missing value", m2.getError(), is("Something went wrong"));
    }

    /** Test for bug 5061. */
    @Test
    public void testAdapterTypeSaveLoad_Bug5061() throws Exception {
        List<Class<? extends DataValue>> adapterList = new ArrayList<Class<? extends DataValue>>();
        adapterList.add(StringValue.class);
        DataType type = DataType.getType(MyAdapterCell.class, null, adapterList);
        final NodeSettings config = new NodeSettings("temp");
        type.save(config);
        DataType loadType = DataType.load(config);
        Assert.assertEquals(type, loadType);
        Assert.assertTrue(loadType.isAdaptable(StringValue.class));
    }

}
