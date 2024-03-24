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
 * -------------------------------------------------------------------
 *
 * History
 *   Mar 24, 2024 (wiswedel): created
 */
package org.knime.core.customization;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;

import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.knime.core.customization.repository.NodesFilter;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

/**
 * Tests for {@link APCustomizationProviderServiceImpl}.
 */
public class APCustomizationProviderServiceImplTest {

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() throws Exception {
        Path testYaml = tempDir.resolve("test.yml");
        String yamlContent = """
                nodesFilter:
                  - scope: view
                    rule: allow
                    predicate:
                      type: pattern
                      patterns:
                        - org\\.knime\\.base\\..+
                      isRegex: true
                """;
        Files.writeString(testYaml, yamlContent);

        // No need to mock Bundle; assuming this test runs as "JUnit Plug-in Test" under OSGi
        BundleContext context = FrameworkUtil.getBundle(this.getClass()).getBundleContext();
        IEclipsePreferences prefs = DefaultScope.INSTANCE.getNode(context.getBundle().getSymbolicName());

        prefs.put(APCustomizationProviderServiceImpl.PREF_KEY_CUSTOMIZATION_CONFIG_PATH, testYaml.toString());
    }

    @Test
    void testGetCustomization() {
        APCustomizationProviderServiceImpl serviceImpl = new APCustomizationProviderServiceImpl();
        APCustomization customization = serviceImpl.getCustomization();
        assertEquals(1, customization.getNodesCustomization().size());
        NodesFilter nodesFilter = customization.getNodesCustomization().get(0);
        // can't check internals because class have package scope
        assertThat("debug description of customization", nodesFilter.toString(), containsString("ALLOW"));
        assertThat("debug description of customization", nodesFilter.toString(), containsString("VIEW"));
        assertThat("debug description of customization", nodesFilter.toString(),
            containsString("org\\.knime\\.base"));
    }
}
