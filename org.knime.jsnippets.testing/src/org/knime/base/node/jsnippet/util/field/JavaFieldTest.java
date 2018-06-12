package org.knime.base.node.jsnippet.util.field;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.Field;
import java.util.Optional;

import org.junit.BeforeClass;
import org.junit.Test;
import org.knime.core.data.convert.datacell.JavaToDataCellConverterFactory;
import org.knime.core.data.convert.datacell.JavaToDataCellConverterRegistry;
import org.knime.core.data.convert.java.DataCellToJavaConverterFactory;
import org.knime.core.data.convert.java.DataCellToJavaConverterRegistry;
import org.knime.core.data.def.IntCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.config.Config;
import org.knime.core.node.workflow.FlowVariable;

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
 *   17.10.2016 (Jonathan Hale): created
 */

/**
 * Test for {@link JavaField}.
 *
 * @author Jonathan Hale
 */
public class JavaFieldTest {

    private static JavaToDataCellConverterFactory<?> m_javaToDataCellConverterFactory;

    private static DataCellToJavaConverterFactory<?, ?> m_dataCellToJavaConverterFactory;

    /**
     * Initialize test converter factories
     */
    @BeforeClass
    public static void beforeClass() {
        m_javaToDataCellConverterFactory = JavaToDataCellConverterRegistry.getInstance()
            .getConverterFactories(Integer.class, IntCell.TYPE).stream().findFirst().get();

        m_dataCellToJavaConverterFactory = DataCellToJavaConverterRegistry.getInstance()
            .getConverterFactories(IntCell.TYPE, Integer.class).stream().findFirst().get();
    }

    /**
     * Test setting and getting values of {@link InVar}, {@link OutVar}, {@link InCol} and {@link OutCol}.
     */
    @Test
    public void testGetSet() {
        {
            final InVar iv = new InVar();

            iv.setFlowVarType(FlowVariable.Type.DOUBLE);
            assertEquals(FlowVariable.Type.DOUBLE, iv.getFlowVarType());

            iv.setJavaName("KNIME");
            assertEquals("KNIME", iv.getJavaName());

            iv.setJavaType(Object.class);
            assertEquals(Object.class, iv.getJavaType());

            iv.setKnimeName("A");
            assertEquals("A", iv.getKnimeName());
        }

        {
            final OutVar ov = new OutVar();

            ov.setFlowVarType(FlowVariable.Type.DOUBLE);
            assertEquals(FlowVariable.Type.DOUBLE, ov.getFlowVarType());

            ov.setJavaName("K");
            assertEquals("K", ov.getJavaName());

            ov.setJavaType(Object.class);
            assertEquals(Object.class, ov.getJavaType());

            ov.setKnimeName("B");
            assertEquals("B", ov.getKnimeName());
        }

        {
            final OutCol oc = new OutCol();

            oc.setJavaName("K");
            assertEquals("K", oc.getJavaName());

            oc.setKnimeName("B");
            assertEquals("B", oc.getKnimeName());

            oc.setReplaceExisting(false);
            assertFalse(oc.getReplaceExisting());
            oc.setReplaceExisting(true);
            assertTrue(oc.getReplaceExisting());

            // Setting the data cell factory should update java and knime types.
            final String id = m_javaToDataCellConverterFactory.getIdentifier();
            oc.setConverterFactory(m_javaToDataCellConverterFactory);
            assertEquals(id, oc.getConverterFactoryId());
            assertEquals(m_javaToDataCellConverterFactory.getSourceType(), oc.getJavaType());
            assertEquals(m_javaToDataCellConverterFactory.getDestinationType(), oc.getDataType());
        }

        final InCol ic = new InCol();

        ic.setJavaName("K");
        assertEquals("K", ic.getJavaName());

        ic.setKnimeName("B");
        assertEquals("B", ic.getKnimeName());

        // Setting the data cell factory should update java and knime types.
        final String id = m_dataCellToJavaConverterFactory.getIdentifier();
        ic.setConverterFactory(IntCell.TYPE, m_dataCellToJavaConverterFactory);
        assertEquals(id, ic.getConverterFactoryId());
        assertEquals(m_dataCellToJavaConverterFactory.getDestinationType(), ic.getJavaType());
        assertEquals(IntCell.TYPE, ic.getDataType());
    }

    /**
     * Test loading and saving of {@link InVar}, {@link OutVar}, {@link InCol} and {@link OutCol}.
     *
     * @throws InvalidSettingsException
     * @throws IllegalArgumentException
     * @throws IllegalAccessException When something goes wrong while testing field values via reflection
     * @throws SecurityException When something goes wrong while testing field values via reflection
     * @throws NoSuchFieldException
     */
    @Test
    public void testLoadAndSave() throws InvalidSettingsException, IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
        {
            Config config = new NodeSettings("JavaFieldTestOutCol");

            final OutCol oc = new OutCol();
            oc.setConverterFactory(m_javaToDataCellConverterFactory);
            oc.setKnimeName("this");
            oc.setJavaName("that");

            oc.saveSettings(config);

            final OutCol loaded = new OutCol();
            loaded.loadSettings(config);

            /* Ensure via reflection that m_factory has *not* been set */
            final Field field = OutCol.class.getDeclaredField("m_factory");
            field.setAccessible(true);
            assertFalse(((Optional<?>)field.get(loaded)).isPresent());

            assertEquals(oc.getKnimeName(), loaded.getKnimeName());
            assertEquals(oc.getJavaName(), loaded.getJavaName());
            assertEquals(oc.getConverterFactoryId(), loaded.getConverterFactoryId());
            assertEquals(oc.getJavaType(), loaded.getJavaType());
            assertEquals(oc.getDataType(), loaded.getDataType());
        }
        {
            Config config = new NodeSettings("JavaFieldTestInCol");

            final InCol ic = new InCol();
            ic.setConverterFactory(IntCell.TYPE, m_dataCellToJavaConverterFactory);
            ic.setKnimeName("yes");
            ic.setJavaName("no");

            ic.saveSettings(config);

            final InCol loaded = new InCol();
            loaded.loadSettings(config);

            /* Ensure via reflection that m_factory has *not* been set */
            final Field field = InCol.class.getDeclaredField("m_factory");
            field.setAccessible(true);
            assertFalse(((Optional<?>)field.get(loaded)).isPresent());

            assertEquals(ic.getKnimeName(), loaded.getKnimeName());
            assertEquals(ic.getJavaName(), loaded.getJavaName());
            assertEquals(ic.getConverterFactoryId(), loaded.getConverterFactoryId());
            assertEquals(ic.getJavaType(), loaded.getJavaType());
            assertEquals(ic.getDataType(), loaded.getDataType());
        }
        {
            Config config = new NodeSettings("JavaFieldTestOutVar");

            final OutVar ov = new OutVar();
            ov.setKnimeName("yes");
            ov.setJavaName("no");
            ov.setJavaType(Integer.class);
            ov.setFlowVarType(FlowVariable.Type.INTEGER);

            ov.saveSettings(config);

            final OutVar loaded = new OutVar();
            loaded.loadSettings(config);

            assertEquals(ov.getKnimeName(), loaded.getKnimeName());
            assertEquals(ov.getJavaName(), loaded.getJavaName());
            assertEquals(ov.getJavaType(), loaded.getJavaType());
            assertEquals(ov.getFlowVarType(), loaded.getFlowVarType());
        }
        {
            Config config = new NodeSettings("JavaFieldTestInVar");

            final InVar iv = new InVar();
            iv.setKnimeName("yes");
            iv.setJavaName("no");
            iv.setJavaType(Integer.class);
            iv.setFlowVarType(FlowVariable.Type.INTEGER);

            iv.saveSettings(config);

            final InVar loaded = new InVar();
            loaded.loadSettings(config);

            assertEquals(iv.getKnimeName(), loaded.getKnimeName());
            assertEquals(iv.getJavaName(), loaded.getJavaName());
            assertEquals(iv.getJavaType(), loaded.getJavaType());
            assertEquals(iv.getFlowVarType(), loaded.getFlowVarType());
        }
    }

    /**
     * Test behavior with invalid converter factory id.
     */
    @Test
    public void testInvalidConverterFactoryId() {
        {
            Config config = new NodeSettings("JavaFieldTestOutCol");

            config.addString(JavaField.JAVA_NAME, "jn");
            config.addString(JavaField.KNIME_NAME, "kn");
            config.addBoolean(OutCol.REPLACE_EXISTING, false);
            config.addString(JavaField.CONV_FACTORY, "I DO NOT EXIST!");
            config.addString(JavaField.JAVA_TYPE, Integer.class.getName());
            config.addDataType(JavaField.KNIME_TYPE, IntCell.TYPE);

            // We are expecting the load to fail.
            final OutCol loaded = new OutCol();
            try {
                loaded.loadSettings(config);
            } catch (InvalidSettingsException e) {
                // Whether the converter factory is missing or not is checked in configure,
                // and not during load.
                fail("Loading a missing converter factory ID should not fail");
            }
        }
        {
            Config config = new NodeSettings("JavaFieldTestInCol");

            config.addString(JavaField.JAVA_NAME, "jn");
            config.addString(JavaField.KNIME_NAME, "kn");
            config.addString(JavaField.CONV_FACTORY, "I DO NOT EXIST!");
            config.addString(JavaField.JAVA_TYPE, Integer.class.getName());
            config.addDataType(JavaField.KNIME_TYPE, IntCell.TYPE);

            final InCol loaded = new InCol();
            try {
                loaded.loadSettings(config);
            } catch (InvalidSettingsException e) {
                // Whether the converter factory is missing or not is checked in configure,
                // and not during load.
                fail("Loading a missing converter factory ID should not fail");
            }
        }
    }

    @Test
    public void testLoadBackwardsCompatibility() throws InvalidSettingsException {
        {
            Config config = new NodeSettings("JavaFieldTestOutCol");

            config.addString(JavaField.JAVA_NAME, "java_name");
            config.addString(JavaField.KNIME_NAME, "knime_name");
            // no converter factory ID.
            config.addString(JavaField.JAVA_TYPE, Integer.class.getName());
            config.addDataType(JavaField.KNIME_TYPE, IntCell.TYPE);
            config.addBoolean(OutCol.REPLACE_EXISTING, true);

            final OutCol loaded = new OutCol();
            loaded.loadSettings(config);

            // The JavaField should silently find a replacement for the old settings
            assertEquals("JavaField did not find a replacement for old settings", m_javaToDataCellConverterFactory.getIdentifier(), loaded.getConverterFactoryId());
            assertEquals(Integer.class, loaded.getJavaType());
            assertEquals(IntCell.TYPE, loaded.getDataType());
        }
        {
            Config config = new NodeSettings("JavaFieldTestInCol");

            config.addString(JavaField.JAVA_NAME, "java_name");
            config.addString(JavaField.KNIME_NAME, "knime_name");
            // no converter factory ID.
            config.addString(JavaField.JAVA_TYPE, Integer.class.getName());
            config.addDataType(JavaField.KNIME_TYPE, IntCell.TYPE);

            final InCol loaded = new InCol();
            loaded.loadSettings(config);

            // The JavaField should silently find a replacement for the old settings
            assertEquals("JavaField did not find a replacement for old settings", m_dataCellToJavaConverterFactory.getIdentifier(), loaded.getConverterFactoryId());
            assertEquals(Integer.class, loaded.getJavaType());
            assertEquals(IntCell.TYPE, loaded.getDataType());
        }
    }
}
