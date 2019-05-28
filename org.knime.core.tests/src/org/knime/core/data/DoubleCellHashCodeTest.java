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
 *   28.08.2015 (Benjamin Wilhelm): created
 */
package org.knime.core.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.knime.core.data.def.DoubleCell;

/**
 * Some tests for edge cases of {@link DoubleCell#equals(Object)} and {@link DoubleCell#hashCode()}.
 *
 * @author Benjamin Wilhelm, KNIME GmbH, Konstanz, Germany
 */
public class DoubleCellHashCodeTest {

    /**
     * Tests if two NaN double cells with different bit representations are equal and if their hash code is equal.
     */
    @Test
    public void testNaNHashCode() {
        final double nanDouble1 = Double.longBitsToDouble(0x7FFFFFFFFFFFFFFFl);
        final double nanDouble2 = Double.longBitsToDouble(0x7FF2FFFFFFFFFFFFl);
        final DoubleCell doubleCell1 = new DoubleCell(nanDouble1);
        final DoubleCell doubleCell2 = new DoubleCell(nanDouble2);

        assertTrue(doubleCell1.equals(doubleCell2));
        assertTrue(doubleCell2.equals(doubleCell1));
        assertEquals(doubleCell1.hashCode(), doubleCell2.hashCode());
    }

    /**
     * Tests if two Inf double cells are equal and if their hash code is equal.
     */
    @Test
    public void testInfHashCode() {
        final double infinity1 = Double.POSITIVE_INFINITY;
        final double infinity2 = Double.POSITIVE_INFINITY;
        final DoubleCell doubleCell1 = new DoubleCell(infinity1);
        final DoubleCell doubleCell2 = new DoubleCell(infinity2);

        assertTrue(doubleCell1.equals(doubleCell2));
        assertTrue(doubleCell2.equals(doubleCell1));
        assertEquals(doubleCell1.hashCode(), doubleCell2.hashCode());
    }

    /**
     * Tests if 0.0 and -0.0 double cells are equal. They should not be equal.
     */
    @Test
    public void testZeroHashCode() {
        final double zero1 = 0.0;
        final double zero2 = -0.0;
        final DoubleCell doubleCell1 = new DoubleCell(zero1);
        final DoubleCell doubleCell2 = new DoubleCell(zero2);

        assertFalse(doubleCell1.equals(doubleCell2));
        assertFalse(doubleCell2.equals(doubleCell1));
    }
}
