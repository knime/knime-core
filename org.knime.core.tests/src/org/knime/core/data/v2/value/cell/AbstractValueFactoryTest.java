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
 *   Nov 18, 2021 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.core.data.v2.value.cell;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataValue;
import org.knime.core.data.v2.ReadValue;
import org.knime.core.data.v2.ValueFactory;
import org.knime.core.data.v2.WriteValue;
import org.knime.core.table.access.BufferedAccesses;
import org.knime.core.table.access.ReadAccess;
import org.knime.core.table.access.WriteAccess;

/**
 * Tests that the Read and WriteValues of a ValueFactory are compatible, i.e. the latter can be set from the former via
 * {@link WriteValue#setValue(DataValue)} and can also be set by a compatible DataCell.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public abstract class AbstractValueFactoryTest {

    /**
     * @return a new instance of the factory to test
     */
    protected abstract ValueFactory<?, ?> createFactory();

    /**
     * @return a cell compatible with the factory
     */
    protected abstract DataCell createNewTestCell();

    private ValueFactory<?, ?> m_testInstance;

    /**
     * Sets up the test instance before each test.
     */
    @Before
    public void before() {
        m_testInstance = createFactory();
    }

    /**
     * Tests that the {@link WriteValue} produced by a {@link ValueFactory} can be set with a {@link ReadValue} from the
     * same factory via {@link WriteValue#setValue(DataValue)}.
     */
    @Test
    public void testSetWriteValueFromReadValue() {
        var first = new BufferedValue(m_testInstance);
        var second = new BufferedValue(m_testInstance);
        first.getWriteValue().setValue(createNewTestCell());
        assertEquals(createNewTestCell(), first.getReadValue().getDataCell());
        second.getWriteValue().setValue(first.getReadValue());
        assertEquals(createNewTestCell(), second.getReadValue().getDataCell());
    }

    /**
     * Tests setting the {@link WriteValue} from a compatible {@link DataCell}.
     */
    @Test
    public void testSetWriteValueFromDataCell() {
        var bufferedValue = new BufferedValue(m_testInstance);
        bufferedValue.getWriteValue().setValue(createNewTestCell());
        assertEquals(createNewTestCell(), bufferedValue.getReadValue().getDataCell());
    }

    /**
     * Class that holds read and write value which are coupled through a buffered access
     */
    protected static final class BufferedValue {
        private final WriteValue<DataValue> m_write;

        private final ReadValue m_read;

        @SuppressWarnings("unchecked")
        <R extends ReadAccess, W extends WriteAccess> BufferedValue(final ValueFactory<R, W> factory) {
            var buffer = BufferedAccesses.createBufferedAccess(factory.getSpec());
            m_write = (WriteValue<DataValue>)factory.createWriteValue((W)buffer);
            m_read = factory.createReadValue((R)buffer);
        }

        WriteValue<DataValue> getWriteValue() {
            return m_write;
        }

        ReadValue getReadValue() {
            return m_read;
        }
    }
}
