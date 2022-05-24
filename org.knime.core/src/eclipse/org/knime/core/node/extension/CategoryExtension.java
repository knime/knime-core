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
 *   Mar 19, 2021 (hornm): created
 */
package org.knime.core.node.extension;

import java.util.regex.Pattern;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.runtime.IConfigurationElement;
import org.knime.core.node.util.CheckUtils;

/**
 * A node category contributed via the respective extension point. This class encapsulates everything that is defined
 * via the extension point.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 * @author Benjamin Wilhelm, KNIME GmbH, Konstanz, Germany
 * @noreference This class is not intended to be referenced by clients.
 * @since 4.5
 */
public final class CategoryExtension {

    private static final Pattern KNIME_PLUGIN_ID_PATTERN = Pattern.compile("^(?:org|com)\\.knime\\..+");

    private final String m_name;

    private final String m_path;

    private final String m_levelId;

    private final String m_afterId;

    private final String m_description;

    private final String m_icon;

    private final boolean m_locked;

    private final String m_contributingPlugin;

    private CategoryExtension(final String name, final String path, final String levelId, final String afterId,
        final String description, final String icon, final boolean locked, final String contributingPlugin) {
        m_name = name;
        m_levelId = levelId;
        m_path = path;
        m_afterId = afterId;
        m_description = description;
        m_icon = icon;
        m_locked = locked;
        m_contributingPlugin = contributingPlugin;
    }

    /**
     * Create a {@link CategoryExtension} for a category registered at the "categories" extension point.
     *
     * @param configurationElement the configuration element of the "categories" extension point.
     * @return a new {@link CategoryExtension} with date from the extension
     */
    public static CategoryExtension fromConfigurationElement(final IConfigurationElement configurationElement) {
        var contributingPlugin = configurationElement.getDeclaringExtension().getNamespaceIdentifier();

        var name = configurationElement.getAttribute("name");
        CheckUtils.checkArgument(StringUtils.isNotBlank(name),
            "Category name must not be blank (contributing plug-in %s)",
            configurationElement.getContributor().getName());

        var path = configurationElement.getAttribute("path");
        CheckUtils.checkArgument(StringUtils.isNotBlank(path),
            "Category path must not be blank (contributing plug-in %s)",
            configurationElement.getContributor().getName());

        var levelId = configurationElement.getAttribute("level-id");
        CheckUtils.checkArgument(StringUtils.isNotBlank(levelId),
            "Category level-id must not be blank (contributing plug-in %s)",
            configurationElement.getContributor().getName());

        var afterId = ObjectUtils.defaultIfNull(configurationElement.getAttribute("after"), "");
        var description = ObjectUtils.defaultIfNull(configurationElement.getAttribute("description"), "");
        var icon = cleanupRelativeIconPath(configurationElement.getAttribute("icon"));

        var lockedAttribute = configurationElement.getAttribute("locked");
        var locked = Boolean.parseBoolean(lockedAttribute)
            || ((lockedAttribute == null) && KNIME_PLUGIN_ID_PATTERN.matcher(contributingPlugin).matches());

        return new CategoryExtension(name, normalizePath(path), levelId, afterId, description, icon, locked,
            contributingPlugin);
    }

    /**
     * @param name name the name of this category e.g. "File readers"
     * @param levelId the category level-id. This is used as a path-segment and must be unique at the level specified by
     *            "path".
     * @return a builder to create a {@link CategoryExtension}.
     */
    public static Builder builder(final String name, final String levelId) {
        return new Builder(name, levelId);
    }

    /** Add a / before and after the path if it isn't there yet */
    private static String normalizePath(String path) {
        if (!path.startsWith("/")) {
            path = "/" + path; // NOSONAR
        }
        if (!path.endsWith("/")) {
            path = path + "/"; // NOSONAR
        }
        return path;
    }

    /** Make icon paths relative if they are absolute */
    private static String cleanupRelativeIconPath(String icon) {
        while (icon != null && icon.startsWith("/")) {
            icon = icon.substring(1);
        }
        return icon;
    }

    /**
     * @return the category name
     */
    public String getName() {
        return m_name;
    }

    /**
     * @return the path this category is located at
     */
    public String getPath() {
        return m_path;
    }

    /**
     * Composes and returns the complete path which is made of the path and the level-id.
     *
     * @return the complete path, i.e. the path concatenated with the level-id
     */
    public String getCompletePath() {
        return m_path + m_levelId;
    }

    /**
     * @return the category id
     */
    public String getLevelId() {
        return m_levelId;
    }

    /** @return the "after" field in the extension point or an empty string. */
    public String getAfterID() {
        return m_afterId;
    }

    /**
     * @return the description of the category or an empty string
     * @since 4.6
     */
    public String getDescription() {
        return m_description;
    }

    /**
     * @return the path to the icon of the category
     * @since 4.6
     */
    public String getIcon() {
        return m_icon;
    }

    /**
     * @return if the category is locked
     * @since 4.6
     */
    public boolean isLocked() {
        return m_locked;
    }

    /** @return symbolic name of the plug-in contributing the category/extension. */
    public String getContributingPlugin() {
        return m_contributingPlugin;
    }

    /**
     * A builder for {@link CategoryExtension}
     *
     * @noreference This class is not intended to be referenced by clients.
     * @since 4.6
     */
    public static final class Builder {

        private final String m_name;

        private final String m_levelId;

        private String m_path = "/";

        private String m_after = "";

        private String m_description = "";

        private String m_icon = null;

        private boolean m_locked = false;

        private String m_pluginId = null;

        private Builder(final String name, final String levelId) {
            CheckUtils.checkArgumentNotNull(name, "The name must not be null.");
            CheckUtils.checkArgument(!name.isBlank(), "The name must not be blank.");
            m_name = name;

            CheckUtils.checkArgumentNotNull(levelId);
            m_levelId = levelId;
        }

        /** @return the constructed {@link CategoryExtension} with all the attributes */
        public CategoryExtension build() {
            return new CategoryExtension(m_name, m_path, m_levelId, m_after, m_description, m_icon, m_locked,
                m_pluginId);
        }

        /**
         * @param path the absolute "path" that lead to this category e.g. "/io/read". The segments are the category
         *            level-IDs, separated by a slash ("/").
         * @return the builder
         */
        public Builder withPath(String path) {
            CheckUtils.checkArgumentNotNull(path, "The path must not be null.");
            // NOTE:
            // We make sure all paths start and end with a "/"
            if (!path.startsWith("/")) {
                path = "/" + path; // NOSONAR: special path delimiter for category paths
            }
            if (!path.endsWith("/")) {
                path = path + "/"; // NOSONAR: special path delimiter for category paths
            }
            m_path = path;
            return this;
        }

        /**
         * @param after specifies the level-id of the category after which this category should be sorted in.
         * @return the builder
         */
        public Builder withAfter(final String after) {
            CheckUtils.checkArgumentNotNull(after, "The after must not be null.");
            m_after = after;
            return this;
        }

        /**
         * @param description a short description of the category
         * @return the builder
         */
        public Builder withDescription(final String description) {
            CheckUtils.checkArgumentNotNull(description, "The description must not be null.");
            m_description = description;
            return this;
        }

        /**
         * @param icon the path to an icon (16x16 pixel) for this category
         * @return the builder
         */
        public Builder withIcon(final String icon) {
            m_icon = icon;
            return this;
        }

        /**
         * @param locked if true, only nodes or sub-categories from the same vendor or KNIME may be added to this
         *            category. If false every plug-in may add nodes and sub-categories.
         * @return the builder
         */
        public Builder withLocked(final boolean locked) {
            m_locked = locked;
            return this;
        }

        /**
         * @param pluginId the id of the plugin which provides this category. This must be from the same vendor as the
         *            categoryset.
         * @return the builder
         */
        public Builder withPluginId(final String pluginId) {
            m_pluginId = pluginId;
            return this;
        }
    }
}
