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
 * ------------------------------------------------------------------------
 *
 */
package org.knime.core.data;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Test;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.StringCell;

/**
 * Tests {@link DataTableSpec}.
 * @author Tobias Koetter, KNIME GmbH, Konstanz, Germany
 *
 */
@SuppressWarnings("static-method")
public final class DataTableSpecTest {

    private static DataColumnSpec[] createColumnSpecs(final int count, final String prefixName) {
        DataColumnSpec[] result = new DataColumnSpec[count];
        for (int i = 0; i < count; i++) {
            DataType type;
            switch (i % 3) {
                case 0: type = StringCell.TYPE; break;
                case 1: type = DoubleCell.TYPE; break;
                default: type = IntCell.TYPE; break;
            }
            String name = prefixName + i;
            result[i] = new DataColumnSpecCreator(name, type).createSpec();
        }
        return result;
    }

    /**
     * Tests the {@link DataTableSpec#isCompatibleWith(DataTableSpec)} method.
     */
    @Test
    public void testIsCompatibleWith() {
        DataColumnSpec[] cols = createColumnSpecs(5, "ColName");
        final DataTableSpec reference = new DataTableSpec(cols);

        //null parameter
        assertFalse(reference.isCompatibleWith(null));

        //same instance
        assertTrue(reference.isCompatibleWith(reference));

        //same structure
        final DataTableSpec same = new DataTableSpec(cols);
        assertTrue(reference.isCompatibleWith(same));

        //less columns
        final DataTableSpec lessCols = new DataTableSpec(Arrays.copyOfRange(cols, 0, cols.length -1));
        assertFalse(reference.isCompatibleWith(lessCols));

        //same structure but different name
        cols = createColumnSpecs(5, "DifferentColName");
        final DataTableSpec differentColNames = new DataTableSpec(cols);
        assertFalse(reference.isCompatibleWith(differentColNames));

        //compatible types
        int i = 0;
        final String prefix = "Col_";
        final DataColumnSpecCreator creator = new DataColumnSpecCreator(prefix + i, IntCell.TYPE);
        cols = new DataColumnSpec[1];
        cols[i] = creator.createSpec();
        final DataTableSpec intSpec = new DataTableSpec(cols);
        creator.setType(LongCell.TYPE);
        cols[i] = creator.createSpec();
        final DataTableSpec longSpec = new DataTableSpec(cols);
        assertTrue(intSpec.isCompatibleWith(longSpec));
        assertFalse(longSpec.isCompatibleWith(intSpec));
        creator.setType(DoubleCell.TYPE);
        cols[i] = creator.createSpec();
        final DataTableSpec doubleSpec = new DataTableSpec(cols);
        assertTrue(intSpec.isCompatibleWith(doubleSpec));
        assertTrue(longSpec.isCompatibleWith(doubleSpec));
        assertFalse(doubleSpec.isCompatibleWith(intSpec));
        assertFalse(doubleSpec.isCompatibleWith(longSpec));
    }

}
