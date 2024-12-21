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

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

import org.apache.commons.lang3.StringUtils;
import org.knime.core.customization.ui.actions.MenuEntry;
import org.knime.core.util.User;
import org.knime.core.node.util.CheckUtils;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents the UI customization settings that include a list of menu entries.
 *
 * @since 5.3
 * @noreference This class is not intended to be referenced by clients.
 * @author Bernd Wiswedel
 */
public final class UICustomization {

    /** No customization, no additional entries in the menu etc.
     * @noreference This field is not intended to be referenced by clients. */
    public static final UICustomization DEFAULT = new UICustomization();

    private static final String USER_PLACEHOLDER = "{user}";

    private final List<MenuEntry> m_menuEntries;

    private final boolean m_hideWelcomeAPTiles;

    private final String m_welcomeAPEndpointURL;

    private UICustomization() {
        m_menuEntries = List.of();
        m_hideWelcomeAPTiles = false;
        m_welcomeAPEndpointURL = null;
    }

    @SuppressWarnings("unused") // URL constructor
    @JsonCreator
    UICustomization(@JsonProperty("menuEntries") final List<MenuEntry> menuEntries, //
        @JsonProperty("hideWelcomeAPTiles") final boolean hideWelcomeAPTiles, // added in 5.5 (and 5.4.1)
        @JsonProperty("welcomeAPEndpointURL") final String welcomeAPEndpointURL) throws IOException {
        m_menuEntries = Objects.requireNonNullElse(menuEntries, List.of());
        m_hideWelcomeAPTiles = hideWelcomeAPTiles;
        if (welcomeAPEndpointURL != null) {
            try {
                new URL(welcomeAPEndpointURL);
            } catch (MalformedURLException mfe) {
                throw new IOException(
                    String.format("Invalid \"%s\": \"%s\"", "welcomeAPEndpointURL", welcomeAPEndpointURL), mfe);
            }
        }
        m_welcomeAPEndpointURL = welcomeAPEndpointURL;
    }

    /**
     * @return the list of menu entries, not null.
     */
    public List<MenuEntry> getMenuEntries() {
        return m_menuEntries;
    }

    /**
     * @return whether to hide the 'welcome-AP-tile(s)' or not
     * @since 5.5
     */
    public boolean isHideWelcomeAPTiles() {
        return m_hideWelcomeAPTiles;
    }

    /**
     * @param userSupplier a user-supplier to replace the placeholder "{user}" in the endpoint URL - only called if
     *            there is a "{user}"-placeholder
     * @return the welcomeAPEndpointURL
     * @since 5.5
     * @throws IllegalArgumentException if the URL contains the placeholder "{user}" but no user name is given
     */
    public Optional<String> getWelcomeAPEndpointURL(final Supplier<String> userSupplier) {
        if (m_welcomeAPEndpointURL == null) {
            Optional.empty();
        } else {
            return Optional.of(replaceUserFieldInEndpointURLIfPresent(m_welcomeAPEndpointURL, userSupplier));
        }
        return Optional.of(m_welcomeAPEndpointURL);
    }

    /**
     * For user-defined endpoints, replace the placeholder "{user}" with the actual user name. In most cases (99.9%+)
     * this method does nothing as the default endpoint in the public KNIME distribution is used (see
     * {@link #KNIME_COM_ENDPOINT}). Custom Business-Hubs might deliver AP customizations with custom endpoints having
     * place holders for the user name. The user name is determined by {@link User#getUsername()}.
     *
     * @return The modified endpoint URL in case it contains the placeholder "{user}". Otherwise the input is returned.
     */
    private static String replaceUserFieldInEndpointURLIfPresent(final String rawEndpointURLAsString,
        final Supplier<String> userSupplier) {
        if (rawEndpointURLAsString.contains(USER_PLACEHOLDER)) {
            var user = CheckUtils.checkArgumentNotNull(userSupplier.get(),
                "No 'user' parameter given, but required to resolve the URL template: \"%s\"", rawEndpointURLAsString);
            return StringUtils.replace(rawEndpointURLAsString, USER_PLACEHOLDER, user);
        }
        return rawEndpointURLAsString;
    }

    @Override
    public String toString() {
        return String.format("UI{menuEntries=%s, hideWelcomeAPTiles=%s, welcomeAPEndpointURL=%s}", m_menuEntries,
            m_hideWelcomeAPTiles, m_welcomeAPEndpointURL);
    }
}
