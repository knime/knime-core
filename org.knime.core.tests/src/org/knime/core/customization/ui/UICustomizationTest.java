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
 *   May 23, 2024 (wiswedel): created
 */
package org.knime.core.customization.ui;

import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.knime.core.customization.ui.actions.MenuEntry;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

/**
 * Tests for {@link UICustomization}.
 */
public class UICustomizationTest {

    private static final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    @Test
    void testDeserialization() throws Exception {
        String ymlInput = """
                menuEntries:
                  - name: "Company Help Portal"
                    link: "https://help.company.com/knime"
                """;

        UICustomization uiCustomization = mapper.readValue(ymlInput, UICustomization.class);

        assertNotNull(uiCustomization);
        List<MenuEntry> menuEntries = uiCustomization.getMenuEntries();
        assertNotNull(menuEntries);
        assertEquals(1, menuEntries.size());

        MenuEntry entry = menuEntries.get(0);
        assertEquals("Company Help Portal", entry.getName());
        assertEquals("https://help.company.com/knime", entry.getLink());
        assertNotNull(uiCustomization.toString());
    }

    @Test
    void testEmptyDeserialization() throws Exception {
        String ymlInput = """
                menuEntries: []
                """;

        UICustomization uiCustomization = mapper.readValue(ymlInput, UICustomization.class);

        assertNotNull(uiCustomization);
        List<MenuEntry> menuEntries = uiCustomization.getMenuEntries();
        assertNotNull(menuEntries);
        assertEquals(0, menuEntries.size());
    }

    @Test
    void testDeserializationMissingFields() {
        String ymlInputMissingName = """
                menuEntries:
                  - link: "https://help.company.com/knime"
                """;
        String ymlInputMissingLink = """
                menuEntries:
                  - name: "Company Help Portal"
                """;

        assertThrows(JsonProcessingException.class, () -> mapper.readValue(ymlInputMissingName, UICustomization.class));

        assertThrows(JsonProcessingException.class, () -> mapper.readValue(ymlInputMissingLink, UICustomization.class));
    }

}