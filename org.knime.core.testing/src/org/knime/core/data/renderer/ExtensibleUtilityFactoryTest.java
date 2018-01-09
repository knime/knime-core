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
 * ---------------------------------------------------------------------
 *
 * History
 *   27.05.2013 (thor): created
 */
package org.knime.core.data.renderer;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.Collection;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.hamcrest.core.IsNull;
import org.junit.Test;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue.UtilityFactory;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.ExtensibleUtilityFactory;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.StringCell;
import org.osgi.framework.FrameworkUtil;

/**
 * Tests for the renderer extension point and its support in the framework.
 *
 * @author Thorsten Meinl, KNIME AG, Zurich, Switzerland
 */
public class ExtensibleUtilityFactoryTest {
    /**
     * Tests basic functionality of the extension point.
     */
    @Test
    public void testRegisteredRenderers() {
        ExtensibleUtilityFactory utilityFactory = TestDataValue.UTILITY;
        assertThat("Wrong utility factory retrieved via data type", DataType.getUtilityFor(TestDataValue.class),
                   is((UtilityFactory)utilityFactory));

        Collection<DataValueRendererFactory> availableRenderers = utilityFactory.getAvailableRenderers();
        assertThat("Wrong number of available renderers", availableRenderers.size(), is(2));

        assertTrue("TestValueRenderer not found in available renderers",
                   availableRenderers.contains(new TestValueRenderer.Factory()));
        assertTrue("StringValueRenderer not found in available renderers",
                   availableRenderers.contains(new StringValueRenderer.Factory()));

        assertThat("Wrong default renderer", new TestValueRenderer.Factory(), is(utilityFactory.getDefaultRenderer()));
        assertThat("Unexpected preference key", ExtensibleUtilityFactory.getPreferenceKey(TestDataValue.class),
                   is(utilityFactory.getPreferenceKey()));
    }

    /**
     * Checks if duplicate factories for one data type are detected.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testDuplicateUtilityFactory() {
        TestDataValue.UTILITY.getName(); // make sure the factory is loaded

        new ExtensibleUtilityFactory(TestDataValue.class) {
            @Override
            public String getName() {
                return "Second test value factory";
            }
        };
    }

    /**
     * Tests basic functionality of the extension point.
     */
    @Test
    public void testNoRegisteredRenderers() {
        ExtensibleUtilityFactory utilityFactory = Test2DataValue.UTILITY;
        assertThat("Wrong utility factory retrieved via data type", DataType.getUtilityFor(Test2DataValue.class),
                   is((UtilityFactory)utilityFactory));

        Collection<DataValueRendererFactory> availableRenderers = utilityFactory.getAvailableRenderers();
        assertThat("Wrong number of available renderers", availableRenderers.size(), is(0));

        assertThat("Unexpected preferred renderer", utilityFactory.getPreferredRenderer(), is(IsNull.nullValue()));
    }

    /**
     * Checks if the preferred renderer is correctly read from the preferences.
     */
    @Test
    public void testPreferredRenderer() {
        ExtensibleUtilityFactory utilityFactory = TestDataValue.UTILITY;

        IEclipsePreferences corePrefs =
                InstanceScope.INSTANCE.getNode(FrameworkUtil.getBundle(DataValueRendererFactory.class)
                        .getSymbolicName());
        DataValueRendererFactory rendererFactory = new TestValueRenderer.Factory();
        corePrefs.put(ExtensibleUtilityFactory.getPreferenceKey(TestDataValue.class), rendererFactory.getId());

        assertThat("Unexpected preferred renderer", utilityFactory.getPreferredRenderer(), is(rendererFactory));

        rendererFactory = new StringValueRenderer.Factory();
        corePrefs.put(ExtensibleUtilityFactory.getPreferenceKey(TestDataValue.class), rendererFactory.getId());
        assertThat("Unexpected preferred renderer after preference change", utilityFactory.getPreferredRenderer(),
                   is(rendererFactory));

        DataValueRendererFactory invalidRendererFactory = new MultiLineStringValueRenderer.Factory();
        corePrefs.put(ExtensibleUtilityFactory.getPreferenceKey(TestDataValue.class), invalidRendererFactory.getId());
        assertThat("Unexpected preferred renderer after trying to set invalid preferred renderer",
                   utilityFactory.getPreferredRenderer(), is(IsNull.nullValue()));
    }

    /**
     * Simple test for retrieving all existing extensible node factories.
     */
    @Test
    public void testGetAllFactories() {
        int factoryCount = ExtensibleUtilityFactory.getAllFactories().size();
        assertTrue("Strange number of utility factories, I expected at least 10", factoryCount >= 10);
    }

    /**
     * Checks if the creation of actual renderer (via the family) works correct.
     */
    @Test
    public void testGetRendererFamily() {
        DataValueRendererFamily family =
                StringCell.TYPE.getRenderer(new DataColumnSpecCreator("Test", StringCell.TYPE).createSpec());
        assertThat("Wrong number of renderers in family", family.getRendererDescriptions().length, is(2));

        IEclipsePreferences corePrefs =
                InstanceScope.INSTANCE.getNode(FrameworkUtil.getBundle(DataValueRendererFactory.class)
                        .getSymbolicName());
        DataValueRendererFactory rendererFactory = new DoubleGrayValueRenderer.Factory();
        corePrefs.put(ExtensibleUtilityFactory.getPreferenceKey(DoubleValue.class), rendererFactory.getId());
        DataColumnSpec colSpec = new DataColumnSpecCreator("Test", DoubleCell.TYPE).createSpec();
        family = DoubleCell.TYPE.getRenderer(colSpec);
        assertThat("Wrong preferred renderer in family list at position 0", family.getRendererDescriptions()[0], is(rendererFactory
                .createRenderer(colSpec).getDescription()));
    }
}
