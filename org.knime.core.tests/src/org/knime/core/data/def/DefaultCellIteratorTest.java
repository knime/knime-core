/*
 * -------------------------------------------------------------------
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
 * -------------------------------------------------------------------
 *
 * History
 *   Nov 23, 2005 (wiswedel): created
 *   21.06.06 (bw & po): reviewed
 */
package org.knime.core.data.def;

import java.util.Iterator;
import java.util.NoSuchElementException;

import junit.framework.TestCase;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DoubleValue;
import org.knime.core.node.NodeLogger;

/**
 * Test the default cell iterator class.
 *
 * @author Bernd Wiswedel, University of Konstanz
 */
public class DefaultCellIteratorTest extends TestCase {

    /** Test all methods in iterator. */
    public void testIterator() {
        double[] d = new double[]{1.0, 2.0, 3.0};
        DataRow row = new DefaultRow("Key", d);
        Iterator<DataCell> it = row.iterator();
        int i = 0;
        while (it.hasNext()) {
            DataCell cell = it.next();
            DoubleValue dCell = (DoubleValue)cell;
            assertEquals(dCell.getDoubleValue(), d[i]);
            try {
                it.remove();
                fail("Expected " + UnsupportedOperationException.class + " not thrown");
            } catch (UnsupportedOperationException e) {
                NodeLogger.getLogger(getClass()).debug("Got expected exception: " + e.getClass(), e);
            }
            i++;
        }
        assertFalse(it.hasNext());
        assertEquals(i, d.length);
        try {
            it.next();
            fail("Expected " + NoSuchElementException.class + " not thrown");
        } catch (NoSuchElementException e) {
            NodeLogger.getLogger(getClass()).debug("Got expected exception: " + e.getClass(), e);
        }
    }
}
