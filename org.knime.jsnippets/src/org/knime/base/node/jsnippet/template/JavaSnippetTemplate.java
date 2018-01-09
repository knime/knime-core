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
 * ------------------------------------------------------------------------
 *
 * History
 *   02.04.2012 (hofer): created
 */
package org.knime.base.node.jsnippet.template;

import java.util.UUID;

import org.knime.base.node.jsnippet.util.JSnippetTemplate;
import org.knime.base.node.jsnippet.util.JavaSnippetSettings;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * A settings class for Java Snippet templates.
 * <p>This class might change and is not meant as public API.
 *
 * @author Heiko Hofer
 * @since 2.12
 * @noextend This class is not intended to be subclassed by clients.
 * @noinstantiate This class is not intended to be instantiated by clients.
 * @noreference This class is not intended to be referenced by clients.
 */
@SuppressWarnings("rawtypes")
public class JavaSnippetTemplate implements JSnippetTemplate {
    private static final String SNIPPET = "Snippet";
    private static final String META_CATEGORY = "Meta category";
    private static final String CATEGORY = "Category";
    private static final String NAME = "Name";
    private static final String DESCRIPTION = "Description";
    private static final String VERSION = "Version";

    /** The first version of templates. */
    public static final String VERSION_1_X = "version 1.x";

    private JavaSnippetSettings m_snippetSettings;
    /** The meta category which typically is the dialog class this template
     * comes from.
     */
    private Class m_metaCategory;
    /** The category this template falls into. */
    private String m_category;
    /** A short (one sentence) descriptive name. It must not  necessarily be
     * unique.
     */
    private String m_name;
    /** The description of the template. */
    private String m_description;
    /** The version of the template. */
    private String m_version;
    /** The uuid of the template. */
    private String m_uuid;


    /**
     * Create a template and read parameters from the settings object.
     * @param settings the settings
     * @return a new instance
     */
    public static JavaSnippetTemplate create(final NodeSettingsRO settings) {
        JavaSnippetTemplate template = new JavaSnippetTemplate();
        template.loadSettings(settings);
        return template;
    }
    /**
     * Create instance with default values.
     * @param metaCategory the meta category of the template
     * @param snippetSettings the settings
     */
    public JavaSnippetTemplate(final Class metaCategory,
            final JavaSnippetSettings snippetSettings) {
            this(metaCategory, snippetSettings, JavaSnippetTemplate.VERSION_1_X);
    }
    /**
     * Create instance with default values.
     * @param metaCategory the meta category of the template
     * @param snippetSettings the settings
     * @param version the version
     */
    protected JavaSnippetTemplate(final Class metaCategory,
            final JavaSnippetSettings snippetSettings, final String version) {
        m_metaCategory = metaCategory;
        m_category = "default";
        m_description = "";
        m_version = version;
        m_snippetSettings = snippetSettings;
        m_uuid = UUID.randomUUID().toString();
        m_snippetSettings.setTemplateUUID(m_uuid);
    }

    /**
     * Create an empty instance used for persistence.
     */
    protected JavaSnippetTemplate() {
        // fields will be set with loadSettingsFor...
    }

    /**
     * @return the snippetSettings
     */
    public JavaSnippetSettings getSnippetSettings() {
        return m_snippetSettings;
    }

    /**
     * @return the category
     */
    @Override
    public String getCategory() {
        return m_category;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setCategory(final String category) {
        m_category = category;
    }

    /**
     * @return the name
     */
    @Override
    public String getName() {
        return m_name;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setName(final String name) {
        m_name = name;
    }

    /**
     * @return the description
     */
    @Override
    public String getDescription() {
        return m_description;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setDescription(final String description) {
        m_description = description;
    }

    /**
     * @return the metaCategory
     */
    @Override
    public Class getMetaCategory() {
        return m_metaCategory;
    }


    /**
     * @return the uuid
     */
    @Override
    public String getUUID() {
        return m_uuid;
    }

    /** Saves current parameters to settings object.
     * @param settings To save to.
     */
    @Override
    public void saveSettings(final NodeSettingsWO settings) {
        settings.addString(META_CATEGORY, m_metaCategory.getName());
        settings.addString(CATEGORY, m_category);
        settings.addString(NAME, m_name);
        settings.addString(DESCRIPTION, m_description);
        settings.addString(VERSION, m_version);
        NodeSettingsWO snippet = settings.addNodeSettings(SNIPPET);
        m_snippetSettings.saveSettings(snippet);
    }

    /** Loads parameters.
     * @param settings to load from
     */
    @Override
    public void loadSettings(final NodeSettingsRO settings) {
        try {
            String metaCategory = settings.getString(META_CATEGORY, null);
            m_metaCategory = getMetaCategoryClass(metaCategory);
            m_category = settings.getString(CATEGORY, "default");
            m_name = settings.getString(NAME, "?");
            m_description = settings.getString(DESCRIPTION, "");
            m_version = settings.getString(m_version,
                    JavaSnippetTemplate.VERSION_1_X);
            NodeSettingsRO snippet = settings.getNodeSettings(SNIPPET);
            m_snippetSettings = new JavaSnippetSettings();
            m_snippetSettings.loadSettingsForDialog(snippet);
            m_uuid = m_snippetSettings.getTemplateUUID();
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        } catch (InvalidSettingsException e) {
            throw new IllegalStateException(e);
        }
    }


    /**
     * @param metaCategory the class name
     * @return the {@link Class} or <code>null</code>
     * @throws ClassNotFoundException if the class cannot be found
     */
    protected Class<? extends Object> getMetaCategoryClass(final String metaCategory) throws ClassNotFoundException {
        return metaCategory != null
            ? Class.forName(metaCategory) : JavaSnippetTemplate.class;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((m_uuid == null) ? 0 : m_uuid.hashCode());
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        JavaSnippetTemplate other = (JavaSnippetTemplate)obj;
        if (m_uuid == null) {
            if (other.m_uuid != null) {
                return false;
            }
        } else if (!m_uuid.equals(other.m_uuid)) {
            return false;
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return getName();
    }

}
