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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.util.Map;

import org.junit.Test;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.property.ColorHandler;
import org.knime.core.data.property.ColorModelRange;
import org.knime.core.data.property.ShapeFactory;
import org.knime.core.data.property.ShapeHandler;
import org.knime.core.data.property.ShapeModelNominal;
import org.knime.core.data.property.SizeHandler;
import org.knime.core.data.property.SizeModelDouble;
import org.knime.core.data.property.ValueFormatHandler;
import org.knime.core.data.property.ValueFormatModelNumber;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.util.valueformat.NumberFormatter;

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
        assertFalse(stringSpec.isCompatibleWith(null), "spec is not compatible with null");

        //same instance
        assertTrue(stringSpec.isCompatibleWith(stringSpec), "spec is compatible with itself");

        //same spec
        final DataColumnSpec stringSpec2 = creator.createSpec();
        assertTrue(stringSpec.isCompatibleWith(stringSpec2), "spec is compatible with identical spec");
        assertTrue(stringSpec2.isCompatibleWith(stringSpec), "spec is compatible with identical spec");

        //compatible types
        creator.setType(IntCell.TYPE);
        final DataColumnSpec intSpec = creator.createSpec();
        assertFalse(stringSpec.isCompatibleWith(intSpec), "string is not compatible with int");
        assertFalse(intSpec.isCompatibleWith(stringSpec), "int is not compatible with string");

        creator.setType(LongCell.TYPE);
        final DataColumnSpec longSpec = creator.createSpec();
        assertTrue(intSpec.isCompatibleWith(longSpec), "int is compatible with long");
        assertFalse(longSpec.isCompatibleWith(intSpec), "long is not compatible with int");
        creator.setType(DoubleCell.TYPE);
        final DataColumnSpec doubleSpec = creator.createSpec();
        assertTrue(intSpec.isCompatibleWith(doubleSpec), "int is compatible with double");
        assertTrue(longSpec.isCompatibleWith(doubleSpec), "long is compatible with double");
        assertFalse(doubleSpec.isCompatibleWith(intSpec), "double is not compatible with int");
        assertFalse(doubleSpec.isCompatibleWith(longSpec), "double is not compatible with long");

        //different name but same type
        creator.setName(doubleSpec.getName() + "_other");
        final DataColumnSpec doubleDifferentNameSpec = creator.createSpec();
        assertFalse(doubleSpec.isCompatibleWith(doubleDifferentNameSpec),
            "double is not compatible with double with " + "different name");
        assertFalse(doubleDifferentNameSpec.isCompatibleWith(doubleSpec),
            "double with different name is not " + "compatible with double");
    }

    /** When creating a spec without specifying handlers, no handlers should be present. */
    @Test
    public void testDefaultHandlersAreNull() {
        // given
        final DataColumnSpecCreator creator = new DataColumnSpecCreator("Col", StringCell.TYPE);
        final DataColumnSpec stringSpec = creator.createSpec();

        // when checking default handlers
        // then null is returned
        assertNull(stringSpec.getColorHandler(), "Default color handler should be null");
        assertNull(stringSpec.getShapeHandler(), "Default shape handler should be null");
        assertNull(stringSpec.getSizeHandler(), "Default size handler should be null");
        assertNull(stringSpec.getValueFormatHandler(), "Default value format handler should be null");
    }

    /** Test loading and saving visual attribute handlers (color, shape, value format)
     * @throws InvalidSettingsException */
    @Test
    public void testSaveLoadHandlers() throws InvalidSettingsException {
        // given spec with handlers
        final var colorHandler = new ColorHandler(new ColorModelRange(0, Color.BLACK, 1, Color.RED));
        final var shapeHandler = new ShapeHandler(new ShapeModelNominal(Map.of(new StringCell("A"), ShapeFactory.getShape(ShapeFactory.CIRCLE))));
        final var sizeHandler = new SizeHandler(new SizeModelDouble(0, 1));
        final var valueFormatHandler = new ValueFormatHandler(new ValueFormatModelNumber(NumberFormatter.builder().setMaximumDecimals(17).build()));

        final DataColumnSpecCreator creator = new DataColumnSpecCreator("Col", StringCell.TYPE);
        creator.setColorHandler(colorHandler);
        creator.setShapeHandler(shapeHandler);
        creator.setSizeHandler(sizeHandler);
        creator.setValueFormatHandler(valueFormatHandler);
        final DataColumnSpec stringSpec = creator.createSpec();

        // when saving and loading
        final var config = new NodeSettings("root");
        stringSpec.save(config);
        final var stringSpec2 = DataColumnSpec.load(config);

        // then handlers are the same
        assertEquals(colorHandler, stringSpec2.getColorHandler(), "Loaded color handler is not the same as saved one.");
        assertEquals(shapeHandler, stringSpec2.getShapeHandler(), "Loaded shape handler is not the same as saved one.");
        assertEquals(sizeHandler, stringSpec2.getSizeHandler(), "Loaded size handler is not the same as saved one.");
        assertEquals(valueFormatHandler, stringSpec2.getValueFormatHandler(),
            "Loaded value format handler is not the same as saved one.");
    }

    /**
     * Tests the {@link DataColumnSpec#equals(Object)} method.
     */
    @Test
    public void testEquals() {
        // given
        final DataColumnSpecCreator creator = new DataColumnSpecCreator("Col", StringCell.TYPE);
        final DataColumnSpec stringSpec = creator.createSpec();
        final DataColumnSpec stringSpec2 = creator.createSpec();

        //null parameter
        assertFalse(stringSpec.equals(null), "Spec is not unequal to null"); //NOSONAR assertNotNull is not what we want

        //same instance
        assertEquals(stringSpec, stringSpec, "Spec is not equal to itself");

        //same spec
        assertEquals(stringSpec, stringSpec2, "Spec is not equal to identical spec");
    }

    /**
     * Test unequals. Different name, different type, different handlers.
     * @throws InvalidSettingsException
     */
    @Test
    public void testUnequals() throws InvalidSettingsException {
        // given
        final DataColumnSpecCreator creator = new DataColumnSpecCreator("Col", StringCell.TYPE);
        final DataColumnSpec stringSpec = creator.createSpec();
        // different name
        creator.setName(stringSpec.getName() + "_other");
        final DataColumnSpec stringSpec2 = creator.createSpec();
        // different type
        creator.setType(IntCell.TYPE);
        final DataColumnSpec intSpec = creator.createSpec();
        // different color handler
        creator.setColorHandler(new ColorHandler(new ColorModelRange(0, Color.BLACK, 1, Color.RED)));
        final DataColumnSpec colorSpec = creator.createSpec();
        // different value format handler
        creator.setValueFormatHandler(new ValueFormatHandler(
            new ValueFormatModelNumber(NumberFormatter.builder().setMaximumDecimals(17).build())));
        final DataColumnSpec valueFormatSpec = creator.createSpec();
        // and another value format handler
        creator.setValueFormatHandler(new ValueFormatHandler(
            new ValueFormatModelNumber(NumberFormatter.builder().setMaximumDecimals(18).build())));
        final DataColumnSpec valueFormatSpec2 = creator.createSpec();

        // when checking equals, all should be false
        assertNotEquals(stringSpec, stringSpec2, "Different column names not detected");
        assertNotEquals(stringSpec, intSpec, "Different column types not detected");
        assertNotEquals(stringSpec, colorSpec, "Different color handlers not detected");
        assertNotEquals(stringSpec, valueFormatSpec, "Different value format handlers not detected");
        assertNotEquals(valueFormatSpec, valueFormatSpec2, "Different value format handler parameters not detected");
    }

}
