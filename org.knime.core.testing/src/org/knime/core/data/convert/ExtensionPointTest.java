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
 *   23.05.2016 (Jonathan Hale): created
 */
package org.knime.core.data.convert;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Optional;

import org.junit.Ignore;
import org.junit.Test;
import org.knime.core.data.DataValue;
import org.knime.core.data.StringValue;
import org.knime.core.data.convert.datacell.JavaToDataCellConverter;
import org.knime.core.data.convert.datacell.JavaToDataCellConverterFactory;
import org.knime.core.data.convert.datacell.JavaToDataCellConverterRegistry;
import org.knime.core.data.convert.java.DataCellToJavaConverter;
import org.knime.core.data.convert.java.DataCellToJavaConverterFactory;
import org.knime.core.data.convert.java.DataCellToJavaConverterRegistry;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;

/**
 * Test for the "org.knime.core.JavaToDataCellConverter" and "org.knime.core.DataCellToJavaConverter" extension points,
 * as well as the factory names.
 *
 * @author Jonathan Hale
 */
@Ignore("Requires StringCellToIntTestConverterFactory and StringToIntCellTestConverterFactory to be registered in the MANIFEST.MF!"
    + "This would currently expose them in the JavaSnippet.")
public class ExtensionPointTest {

    /**
     * Test whether {@link StringToIntCellTestConverterFactory} was correctly registered at the
     * "org.knime.core.JavaToDataCellConverter" extension point and can be used for conversion.
     *
     * @throws Exception When something went wrong
     */
    @Test
    public void testJavaToDataCell() throws Exception {
        final Optional<JavaToDataCellConverterFactory<String>> factory = JavaToDataCellConverterRegistry.getInstance()
            .getConverterFactories(String.class, IntCell.TYPE).stream().findFirst();
        assertTrue(factory.isPresent());

        final JavaToDataCellConverter<String> converter = factory.get().create(null);
        assertNotNull(converter);

        final IntCell convert = (IntCell)converter.convert("Answer to Life, the Universe, and Everything");
        assertEquals(convert.getIntValue(), 42);
    }

    /**
     * Test whether {@link StringCellToIntTestConverterFactory} was correctly registered at the
     * "org.knime.core.DataCellToJavaConverter" extension point and can be used for conversion.
     *
     * @throws Exception
     */
    @Test
    public void testDataCellToJava() throws Exception {
        final Optional<? extends DataCellToJavaConverterFactory<? extends DataValue, Integer>> factory =
            DataCellToJavaConverterRegistry.getInstance().getConverterFactories(StringCell.TYPE, Integer.class).stream()
                .findFirst();

        assertTrue(factory.isPresent());

        final DataCellToJavaConverter<StringValue, Integer> converter =
            (DataCellToJavaConverter<StringValue, Integer>)factory.get().create();
        assertNotNull(converter);

        final Integer convert = converter.convert(new StringCell("Answer to Life, the Universe, and Everything"));
        assertEquals(convert, new Integer(42));
    }
}
