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
 *   19 Sep 2022 (Carsten Haubold): created
 */
package org.knime.core.data.v2.value.cell;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.Test;
import org.knime.core.data.DataCell;
import org.knime.core.data.v2.ReadValue;
import org.knime.core.data.v2.ValueFactory;
import org.knime.core.data.v2.value.DenseBitVectorValueFactory;
import org.knime.core.data.vector.bitvector.DenseBitVectorCellFactory;

/**
 *
 * @author Carsten Haubold, KNIME GmbH, Konstanz, Germany
 */
public final class DenseBitVectorValueFactoryTest extends AbstractValueFactoryTest {
    @Override
    protected ValueFactory<?, ?> createFactory() {
        return DenseBitVectorValueFactory.INSTANCE;
    }

    @Override
    protected DataCell createNewTestCell() {
        return new DenseBitVectorCellFactory("DEADBEEF01234").createDataCell();
    }

    /**
     * Tests that changing the access leads to correctly read values, because the Accesses and {@link ReadValue}s are
     * re-used throughout the columnar backend.
     */
    @Test
    public void testReadValueUpdates() {
        var value = new BufferedValue(createFactory());
        var testCellA = createNewTestCell();
        var testCellB = new DenseBitVectorCellFactory("ABBA666ACDC").createDataCell();
        value.getWriteValue().setValue(testCellA);
        assertEquals(testCellA, value.getReadValue().getDataCell());
        value.getWriteValue().setValue(testCellB);
        assertEquals(testCellB, value.getReadValue().getDataCell());
    }
}
