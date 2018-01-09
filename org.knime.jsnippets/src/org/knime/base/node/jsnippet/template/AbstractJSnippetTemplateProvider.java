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
 *   26.07.2015 (koetter): created
 */
package org.knime.base.node.jsnippet.template;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.base.node.jsnippet.util.JSnippetTemplate;

/**
 *
 * <p>This class might change and is not meant as public API.
 * @author Tobias Koetter, KNIME.com
 * @param <T> the {@link JSnippetTemplate} implementation
 * @since 2.12
 * @noextend This class is not intended to be subclassed by clients.
 * @noinstantiate This class is not intended to be instantiated by clients.
 * @noreference This class is not intended to be referenced by clients.
 */
public abstract class AbstractJSnippetTemplateProvider<T extends JSnippetTemplate> extends TemplateRepository<T>
    implements ChangeListener, TemplateProvider<T>{

    private Map<Class<?>, Map<String, Collection<T>>> m_templates;
    private List<TemplateRepository<T>> m_repos;
    private TemplateRepository<T> m_defaultRepo;

    /**
     *
     * @param defaultRepository the default {@link TemplateRepository}
     */
    public AbstractJSnippetTemplateProvider(final TemplateRepository<T> defaultRepository) {
        m_defaultRepo = defaultRepository;
        m_repos = new LinkedList<>();
        m_templates = new LinkedHashMap<>();
    }

    /**
     * @param repository the repositories
     */
    protected void setRepositories(final List<TemplateRepository<T>> repository) {
        m_repos.addAll(repository);
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("rawtypes")
    @Override
    public Set<String> getCategories(final Collection<Class> metaCategories) {
        initTemplates(metaCategories);
        Set<String> categories = new LinkedHashSet<>();
        for (Class<?> c : metaCategories) {
            if (m_templates.containsKey(c)) {
                categories.addAll(m_templates.get(c).keySet());
            }
        }
        return categories;
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("rawtypes")
    @Override
    public Collection<T> getTemplates(final Collection<Class> metaCategories) {
        initTemplates(metaCategories);
        Set<T> templates = new LinkedHashSet<>();
        for (Class<?> c : metaCategories) {
            if (m_templates.containsKey(c)) {
                templates.addAll(m_templates.get(c).get(TemplateProvider.ALL_CATEGORY));
            }
        }
        return templates;
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("rawtypes")
    @Override
    public Collection<T> getTemplates(final Collection<Class> metaCategories, final String category) {
        initTemplates(metaCategories);
        Set<T> templates = new LinkedHashSet<>();
        for (Class<?> c : metaCategories) {
            if (m_templates.containsKey(c)) {
                templates.addAll(m_templates.get(c).get(category));
            }
        }
        return templates;
    }

    /** Load templates for the given meta categories.
     * @param metaCategories the meta categories
     */
    @SuppressWarnings("rawtypes")
    private void initTemplates(final Collection<Class> metaCategories) {
        // reset data
        for (Class key : metaCategories) {
            if (m_templates.containsKey(key)) {
                m_templates.remove(key);
            }
            Map<String, Collection<T>> templates = new LinkedHashMap<>();
            templates.put(TemplateProvider.ALL_CATEGORY, new ArrayList<T>());
            m_templates.put(key, templates);
        }
        for (TemplateRepository<T> repo : m_repos) {
            appendTemplates(repo.getTemplates(metaCategories));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addTemplate(final T template) {
        m_defaultRepo.removeChangeListener(this);
        m_defaultRepo.addTemplate(template);
        m_defaultRepo.addChangeListener(this);
        appendTemplates(Collections.singletonList(template));
        // notify listeners
        fireStateChanged();
    }

    /**
     * Append to given list of templates.
     * @param templates the templates
     */
    private void appendTemplates(final Collection<T> templates) {
        if (null == templates) {
            return;
        }
        for (T template : templates) {
            Class<?> key = template.getMetaCategory();
            appendTemplateTo(m_templates.get(key), template);
        }
    }

    /**
     * Append the template to the given map.
     * @param map the map
     * @param template the template
     */
    private void appendTemplateTo(final Map<String, Collection<T>> map, final T template) {
        map.get(TemplateProvider.ALL_CATEGORY).add(template);
        String key = template.getCategory();
        if (!map.containsKey(key)) {
            map.put(key, new ArrayList<T>());
        }
        map.get(key).add(template);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isRemoveable(final T template) {
        for (TemplateRepository<T> repo : m_repos) {
            if (repo.isRemoveable(template)) {
                return true;
            }
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean removeTemplate(final T template) {
        if (isRemoveable(template)) {
            boolean success;
            for (TemplateRepository<T> repo : m_repos) {
                repo.removeChangeListener(this);
                success = repo.removeTemplate(template);
                if (success) {
                    break;
                }
                repo.addChangeListener(this);
            }
            fireStateChanged();
            return true;
        } else {
            return false;
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stateChanged(final ChangeEvent e) {
        // A template changed unexpectedly, reset temporary data
        m_templates.clear();
        // notify listeners
        fireStateChanged();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public T getTemplate(final UUID id) {
        if (null == id) {
            throw new NullPointerException("UUID is null.");
        }
        for (TemplateRepository<T> repo : m_repos) {
            T template = repo.getTemplate(id);
            if (null != template) {
                return template;
            }
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDisplayLocation(final T template) {
        if (null == template) {
            throw new NullPointerException("template is null.");
        }
        for (TemplateRepository<T> repo : m_repos) {
            String loc = repo.getDisplayLocation(template);
            if (null != loc) {
                return loc;
            }
        }
        return null;
    }

}