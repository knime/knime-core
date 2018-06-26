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
 *   27.05.2016 (Jonathan Hale): created
 */
package org.knime.core.data.convert;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.util.Optional;

import org.junit.Test;
import org.knime.core.data.DataValue;
import org.knime.core.data.MissingValue;
import org.knime.core.data.convert.datacell.JavaToDataCellConverterFactory;
import org.knime.core.data.convert.datacell.JavaToDataCellConverterRegistry;
import org.knime.core.data.convert.java.DataCellToJavaConverterFactory;
import org.knime.core.data.convert.java.DataCellToJavaConverterRegistry;
import org.knime.core.data.convert.java.SimpleDataCellToJavaConverterFactory;
import org.knime.core.data.convert.util.SerializeUtil;
import org.knime.core.data.convert.util.SerializeUtil.FactoryPlaceholder;
import org.knime.core.data.def.IntCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;

/**
 * Tests for {@link SerializeUtil}.
 *
 * @author Jonathan Hale
 */
public class SerializeUtilTest {

    private static class DefinitelyNotRegisteredFactory
        extends SimpleDataCellToJavaConverterFactory<MissingValue, Void> {

        public DefinitelyNotRegisteredFactory() {
            super(MissingValue.class, Void.class, (value) -> null);
        }
    }

    @Test
    public void testStoreAndLoadDataCellToJava() throws InvalidSettingsException {
        final Optional<? extends DataCellToJavaConverterFactory<? extends DataValue, Integer>> factory =
            DataCellToJavaConverterRegistry.getInstance().getConverterFactories(IntCell.TYPE, Integer.class).stream()
                .findFirst();
        assumeTrue(factory.isPresent());

        final NodeSettings testSettings = new NodeSettings(getClass().getName());
        SerializeUtil.storeConverterFactory(factory.get(), testSettings, "the-factory");
        final Optional<DataCellToJavaConverterFactory<?, ?>> loadedFactory =
            SerializeUtil.loadDataCellToJavaConverterFactory(testSettings, "the-factory");

        assertTrue(loadedFactory.isPresent());
        assertEquals(factory.get(), loadedFactory.get());
    }

    @Test
    public void testStoreAndLoadJavaToDataCell() throws InvalidSettingsException {
        Optional<JavaToDataCellConverterFactory<Integer>> factory =
            JavaToDataCellConverterRegistry.getInstance().getConverterFactories(Integer.class, IntCell.TYPE).stream().findFirst();
        assumeTrue(factory.isPresent());

        final NodeSettings testSettings = new NodeSettings(getClass().getName());
        SerializeUtil.storeConverterFactory(factory.get(), testSettings, "the-factory2");
        final Optional<JavaToDataCellConverterFactory<?>> loadedFactory =
            SerializeUtil.loadJavaToDataCellConverterFactory(testSettings, "the-factory2");

        assertTrue(loadedFactory.isPresent());
        assertEquals(factory.get(), loadedFactory.get());
    }

    @Test
    public void testPlaceholders() throws InvalidSettingsException {
        final NodeSettings testSettings = new NodeSettings(getClass().getName());

        final DefinitelyNotRegisteredFactory theMissingFactory = new DefinitelyNotRegisteredFactory();
        SerializeUtil.storeConverterFactory(theMissingFactory, testSettings, "missing-factory");
        Optional<DataCellToJavaConverterFactory<?, ?>> missingFactory =
            SerializeUtil.loadDataCellToJavaConverterFactory(testSettings, "missing-factory");
        assertFalse(missingFactory.isPresent());

        final FactoryPlaceholder placeholder = SerializeUtil.getPlaceholder(testSettings, "missing-factory");
        assertEquals(theMissingFactory.getName(), placeholder.getName());
        assertEquals(theMissingFactory.getSourceType().getName(), placeholder.getSourceTypeName());
        assertEquals(theMissingFactory.getDestinationType().getName(), placeholder.getDestinationTypeName());
        assertEquals(theMissingFactory.getIdentifier(), placeholder.getIdentifier());
    }
}
