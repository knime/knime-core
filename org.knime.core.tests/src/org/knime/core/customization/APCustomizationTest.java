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

import static org.junit.Assert.assertSame;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.knime.core.customization.ui.UICustomization;
import org.knime.core.customization.ui.actions.MenuEntry;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

/**
 * Tests for {@link APCustomization} functionalities.
 */
public class APCustomizationTest {

    private static final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    @Test
    void testViewAllow() throws Exception {
        String ymlInput = """
                nodesFilter:
                  - scope: view
                    rule: allow
                    predicate:
                      type: pattern
                      patterns:
                        - org\\.knime\\..+
                        - com\\.knime\\..+
                      isRegex: true
                """;

        APCustomization.Nodes nodesCustomization = mapper.readValue(ymlInput, APCustomization.class).nodes();
        assertTrue(
            nodesCustomization.isViewAllowed("org.knime.base.node.io.filehandling.csv.reader.FileReaderNodeFactory"));
        assertTrue(nodesCustomization.isViewAllowed("com.knime.extension.package.KNIMENodeFactory"));
        assertFalse(nodesCustomization.isViewAllowed("org.community.somepackage.BetterNodeFactory"));

        assertTrue(
            nodesCustomization.isUsageAllowed("org.knime.base.node.io.filehandling.csv.reader.FileReaderNodeFactory"));
        assertTrue(nodesCustomization.isUsageAllowed("com.knime.extension.package.KNIMENodeFactory"));
        assertTrue(nodesCustomization.isUsageAllowed("org.community.somepackage.BetterNodeFactory"));
    }

    @Test
    void testViewDeny() throws Exception {
        String ymlInput = """
                nodesFilter:
                  - scope: view
                    rule: deny
                    predicate:
                      type: pattern
                      patterns:
                        - org\\.community\\.somepackage\\..+
                      isRegex: true
                """;

        APCustomization.Nodes nodesCustomization = mapper.readValue(ymlInput, APCustomization.class).nodes();
        assertTrue(
            nodesCustomization.isViewAllowed("org.knime.base.node.io.filehandling.csv.reader.FileReaderNodeFactory"));
        assertFalse(nodesCustomization.isViewAllowed("org.community.somepackage.BetterNodeFactory"));

        assertTrue(
            nodesCustomization.isUsageAllowed("org.knime.base.node.io.filehandling.csv.reader.FileReaderNodeFactory"));
        assertTrue(nodesCustomization.isUsageAllowed("org.community.somepackage.BetterNodeFactory"));
    }

    @Test
    void testUseDeny() throws Exception {
        String ymlInput = """
                nodesFilter:
                  - scope: use
                    rule: deny
                    predicate:
                      type: pattern
                      patterns:
                        - org\\.community\\.somepackage\\..+
                      isRegex: true
                """;

        APCustomization customization = mapper.readValue(ymlInput, APCustomization.class);
        assertTrue(customization.nodes()
            .isViewAllowed("org.knime.base.node.io.filehandling.csv.reader.FileReaderNodeFactory"));
        assertFalse(customization.nodes().isViewAllowed("org.community.somepackage.BetterNodeFactory"));

        assertTrue(customization.nodes()
            .isUsageAllowed("org.knime.base.node.io.filehandling.csv.reader.FileReaderNodeFactory"));
        assertFalse(customization.nodes().isUsageAllowed("org.community.somepackage.BetterNodeFactory"));
    }

    @Test
    void testUseAllow() throws Exception {
        String ymlInput = """
                nodesFilter:
                  - scope: use
                    rule: allow
                    predicate:
                      type: pattern
                      patterns:
                        - org\\.knime\\..+
                        - com\\.knime\\..+
                      isRegex: true
                """;

        APCustomization customization = mapper.readValue(ymlInput, APCustomization.class);
        APCustomization.Nodes nodesCustomization = customization.nodes();
        assertTrue(
            nodesCustomization.isViewAllowed("org.knime.base.node.io.filehandling.csv.reader.FileReaderNodeFactory"));
        assertFalse(nodesCustomization.isViewAllowed("org.community.somepackage.BetterNodeFactory"));

        assertTrue(
            nodesCustomization.isUsageAllowed("org.knime.base.node.io.filehandling.csv.reader.FileReaderNodeFactory"));
        assertFalse(nodesCustomization.isUsageAllowed("org.community.somepackage.BetterNodeFactory"));

        // Assert that the loaded UICustomization is identical to NO_UI_CUSTOMIZATION
        assertEquals(UICustomization.NO_UI_CUSTOMIZATION, customization.getUICustomization());
    }


    @Test
    void testUICustomization() throws Exception {
        String ymlInput = """
                nodesFilter:
                  - scope: view
                    rule: allow
                    predicate:
                      type: pattern
                      patterns:
                        - org\\.knime\\..+
                        - com\\.knime\\..+
                      isRegex: true
                ui:
                  menuEntries:
                    - name: "Company Help Portal"
                      link: "https://help.company.com/knime"
                """;

        APCustomization customization = mapper.readValue(ymlInput, APCustomization.class);

        assertNotNull(customization);
        assertNotNull(customization.getUICustomization());
        final List<MenuEntry> menuEntries = customization.getUICustomization().getMenuEntries();

        assertSame(menuEntries, customization.ui().getMenuEntries());

        assertEquals(1, menuEntries.size());

        MenuEntry entry = menuEntries.get(0);
        assertEquals("Company Help Portal", entry.getName());
        assertEquals("https://help.company.com/knime", entry.getLink());

    }
}
