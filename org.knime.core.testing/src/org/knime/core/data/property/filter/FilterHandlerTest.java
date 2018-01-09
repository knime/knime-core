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
 *   Oct 26, 2016 (wiswedel): created
 */
package org.knime.core.data.property.filter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.knime.core.data.DataCell;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;

/**
 *
 * @author wiswedel
 */
public class FilterHandlerTest {

    @Test(expected=IllegalArgumentException.class)
    public void testNominalFailNullArgument() {
        FilterModel.newNominalModel(Arrays.asList((DataCell)null));
    }

    @Test(expected=IllegalArgumentException.class)
    public void testNominalFailNullInCollection() {
        FilterModel.newNominalModel(null);
    }

    @Test
    public void testNominal() throws InvalidSettingsException {
        final List<DataCell> list = Arrays.asList(new StringCell("A"), new StringCell("B"));
        FilterModelNominal modelOrig = FilterModel.newNominalModel(list);
        modelOrig.toString(); // coverage
        assertTrue(modelOrig.isInFilter(new StringCell("A")));
        assertFalse(modelOrig.isInFilter(new StringCell("C")));

        assertEquals(list, modelOrig.getValues());
        assertNotEquals(modelOrig, null);
        FilterHandler filterHandlerOrig = FilterHandler.from(modelOrig);
        filterHandlerOrig.toString();
        assertEquals(filterHandlerOrig, filterHandlerOrig);
        assertNotEquals(filterHandlerOrig, null);
        assertTrue(filterHandlerOrig.isInFilter(new StringCell("A")));
        assertFalse(filterHandlerOrig.isInFilter(new StringCell("C")));

        NodeSettings s = new NodeSettings("handler");
        filterHandlerOrig.save(s);
        FilterHandler filterHandlerLoaded = FilterHandler.load(s);
        assertEquals(filterHandlerOrig.hashCode(), filterHandlerLoaded.hashCode());
        assertEquals(filterHandlerOrig, filterHandlerLoaded);
        assertNotSame(filterHandlerOrig, filterHandlerLoaded);

        // model is internalized, so should be the same
        FilterModel modelLoaded = filterHandlerLoaded.getModel();
        assertSame(modelOrig, modelLoaded);
    }

    @Test(expected=IllegalArgumentException.class)
    public void testRangeFailInvalidArgs1() {
        FilterModel.newRangeModel(2, 1, true, false);
    }

    @Test(expected=IllegalArgumentException.class)
    public void testRangeFailInvalidArgs2() {
        FilterModel.newRangeModel(Double.NaN, Double.NaN, true, true);
    }

    @Test
    public void testRangeMinUnbounded() {
        FilterModelRange m = FilterModel.newRangeModel(Double.NaN, 17.0, true, true);
        m.toString(); // coverage
        assertNotEquals(m, null); // coverage
        assertFalse(m.getMinimum().isPresent());
        assertEquals(m.getMaximum().getAsDouble(), 17.0, 0.0);
        assertEquals(m.isMinimumInclusive(), true);
        assertEquals(m.isMaximumInclusive(), true);
        assertTrue(m.isInFilter(new DoubleCell(-3039.3)));
        assertTrue(m.isInFilter(new DoubleCell(16.3)));
        assertTrue(m.isInFilter(new DoubleCell(17.0)));
        assertFalse(m.isInFilter(new DoubleCell(20.0)));
    }

    @Test
    public void testRangeMaxUnbounded() {
        FilterModelRange m = FilterModel.newRangeModel(15.0, Double.POSITIVE_INFINITY, false, false);
        m.toString();
        assertFalse(m.getMaximum().isPresent());
        assertEquals(m.getMinimum().getAsDouble(), 15.0, 0.0);
        assertEquals(m.isMinimumInclusive(), false);
        assertEquals(m.isMaximumInclusive(), false);
        assertTrue(m.isInFilter(new DoubleCell(3039.3)));
        assertTrue(m.isInFilter(new DoubleCell(16.3)));
        assertFalse(m.isInFilter(new DoubleCell(15.0)));
        assertFalse(m.isInFilter(new DoubleCell(12.0)));
    }

    @Test
    public void testRange() throws InvalidSettingsException {
        FilterModelRange modelOrig = FilterModel.newRangeModel(15.0, 17.0, true, false);
        modelOrig.toString(); // coverage
        assertNotEquals(modelOrig, null);
        FilterHandler filterHandlerOrig = FilterHandler.from(modelOrig);
        filterHandlerOrig.toString();
        NodeSettings s = new NodeSettings("handler");
        filterHandlerOrig.save(s);
        FilterHandler filterHandlerLoaded = FilterHandler.load(s);
        assertEquals(filterHandlerOrig.hashCode(), filterHandlerLoaded.hashCode());
        assertEquals(filterHandlerOrig, filterHandlerLoaded);
        assertNotSame(filterHandlerOrig, filterHandlerLoaded);
        assertFalse(modelOrig.isInFilter(new StringCell("Not a double")));
        assertTrue(modelOrig.isInFilter(new DoubleCell(15.0)));
        assertFalse(modelOrig.isInFilter(new DoubleCell(17.0)));
        assertTrue(modelOrig.isInFilter(new DoubleCell(15.2)));
        assertFalse(modelOrig.isInFilter(new DoubleCell(27.0)));
        assertFalse(modelOrig.isInFilter(new DoubleCell(7.0)));

        // model is internalized, so should be the same
        FilterModel modelLoaded = filterHandlerLoaded.getModel();
        assertSame(modelOrig, modelLoaded);
    }

}
