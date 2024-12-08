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
 *   Dec 8, 2024 (wiswedel): created
 */
package org.knime.core.customization.ui;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import org.knime.core.customization.ui.UICustomization.WelcomeAPEndPointURLType;
import org.knime.core.customization.ui.actions.MenuEntry;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

/**
 * Deserializer for {@link UICustomization}. Needed because the <i>welcomeAPEndpointURL</i> string value in the yaml has
 * three states:
 * <ul>
 * <li>not present: default value</li>
 * <li>present but null: no welcome page</li>
 * <li>present and not null: custom URL</li>
 * </ul>
 *
 * @author Bernd Wiswedel, KNIME GmbH, Konstanz, Germany
 * @since 5.4
 */
@SuppressWarnings("serial")
final class UICustomizationDeserializer extends StdDeserializer<UICustomization> {

    UICustomizationDeserializer() {
        super(UICustomization.class);
    }

    @SuppressWarnings("unused") // URL constructor is called for validation
    @Override
    public UICustomization deserialize(final JsonParser jp, final DeserializationContext ctxt) throws IOException {
        JsonNode node = jp.getCodec().readTree(jp);
        List<MenuEntry> menuEntries;
        final JsonNode menuEntriesJsonNode = node.get("menuEntries");
        if (menuEntriesJsonNode != null) {
            try (JsonParser menuEntriesParser = menuEntriesJsonNode.traverse(jp.getCodec())) {
                menuEntries = jp.getCodec().readValue(menuEntriesParser, new TypeReference<List<MenuEntry>>() {});
            }
        } else {
            menuEntries = List.of();
        }

        final JsonNode welcomeAPEndpointUrlJsonNode = node.get("welcomeAPEndpointURL");
        final WelcomeAPEndPointURLType urlType;
        final String urlString;
        if (welcomeAPEndpointUrlJsonNode == null) {
            urlType = WelcomeAPEndPointURLType.DEFAULT;
            urlString = null;
        } else if (welcomeAPEndpointUrlJsonNode.isNull()) {
            urlType = WelcomeAPEndPointURLType.NONE;
            urlString = null;
        } else {
            urlType = WelcomeAPEndPointURLType.CUSTOM;
            try {
                urlString = welcomeAPEndpointUrlJsonNode.asText();
                new URL(urlString);
            } catch (MalformedURLException mfe) {
                throw new IOException(String.format("Invalid \"%s\": \"%s\"", "welcomeAPEndpointURL",
                    welcomeAPEndpointUrlJsonNode.asText()), mfe);
            }
        }
        return new UICustomization(menuEntries, urlType, urlString);
    }
}
