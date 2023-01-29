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

import org.junit.Test;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.StringCell;

/**
 * Tests {@link DataColumnSpec}.
 * @author Tobias Koetter, KNIME GmbH, Konstanz, Germany
 *
 */
@SuppressWarnings("static-method")
public final class DataColumnSpecTest {

    /**
     * Tests the {@link DataColumnSpec#isCompatibleWith(DataColumnSpec)} method.
     */
    @Test
    public void testIsCompatibleWith() {

        final DataColumnSpecCreator creator = new DataColumnSpecCreator("Col", StringCell.TYPE);
        final DataColumnSpec stringSpec = creator.createSpec();

        //null parameter
        assertFalse(stringSpec.isCompatibleWith(null));

        //same instance
        assertTrue(stringSpec.isCompatibleWith(stringSpec));

        //same spec
        final DataColumnSpec stringSpec2 = creator.createSpec();
        assertTrue(stringSpec.isCompatibleWith(stringSpec2));
        assertTrue(stringSpec2.isCompatibleWith(stringSpec));

        //compatible types
        creator.setType(IntCell.TYPE);
        final DataColumnSpec intSpec = creator.createSpec();
        assertFalse(stringSpec.isCompatibleWith(intSpec));
        assertFalse(intSpec.isCompatibleWith(stringSpec));

        creator.setType(LongCell.TYPE);
        final DataColumnSpec longSpec = creator.createSpec();
        assertTrue(intSpec.isCompatibleWith(longSpec));
        assertFalse(longSpec.isCompatibleWith(intSpec));
        creator.setType(DoubleCell.TYPE);
        final DataColumnSpec doubleSpec = creator.createSpec();
        assertTrue(intSpec.isCompatibleWith(doubleSpec));
        assertTrue(longSpec.isCompatibleWith(doubleSpec));
        assertFalse(doubleSpec.isCompatibleWith(intSpec));
        assertFalse(doubleSpec.isCompatibleWith(longSpec));

        //different name but same type
        creator.setName(doubleSpec.getName() + "_other");
        final DataColumnSpec doubleDifferentNameSpec = creator.createSpec();
        assertFalse(doubleSpec.isCompatibleWith(doubleDifferentNameSpec));
        assertFalse(doubleDifferentNameSpec.isCompatibleWith(doubleSpec));
    }

}
