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
 *   25.07.2015 (koetter): created
 */
package org.knime.base.node.jsnippet.template;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;

import javax.swing.event.ChangeListener;

import org.knime.base.node.jsnippet.util.JSnippetTemplate;

/**
 *
 * <p>This class might change and is not meant as public API.
 * @author Tobias Koetter, KNIME.com
 * @param <T> {@link JSnippetTemplate} implementation
 * @since 2.12
 * @noextend This interface is not intended to be extended by clients.
 * @noimplement This interface is not intended to be implemented by clients.
 * @noreference This interface is not intended to be referenced by clients.
 */
public interface TemplateProvider<T extends JSnippetTemplate> {

    /**
     * The display name of the category with all templates.
     */
    public static final String ALL_CATEGORY = "All";

    /**
     * Get all categories.
     * @param metaCategories only categories that hold templates in this
     * meta categories will be displayed.
     * @return the categories
     */
    @SuppressWarnings("rawtypes")
    public Set<String> getCategories(Collection<Class> metaCategories);

    /**
     * Get the {@link JSnippetTemplate}s in the given meta category.
     * @param metaCategories only templates from these
     * meta categories will be returned.
     * @return the {@link JSnippetTemplate}s in the given meta category
     */
    @SuppressWarnings("rawtypes")
    public Collection<T> getTemplates(Collection<Class> metaCategories);

    /**
     * Get the {@link JavaSnippetTemplate}s in the given category.
     * @param metaCategories only templates from these
     * meta categories will be returned.
     * @param category a category as given by getCategories()
     * @return the {@link JavaSnippetTemplate}s in the given category
     */
    @SuppressWarnings("rawtypes")
    public abstract Collection<T> getTemplates(Collection<Class> metaCategories, String category);

    /**
     * Add a template to the default location.
     * @param template the template
     */
    public abstract void addTemplate(T template);

    /**
     * Test if a template can be removed.
     * @param template the template
     * @return true when removeTemplate(template) could be successful
     */
    public abstract boolean isRemoveable(T template);

    /**
     * Remove the given template.
     * @param template the template to be removed
     * @return when the template is successfully removed
     */
    public boolean removeTemplate(T template);

    /**
     * Get the template with the given id.
     * @param id the id
     * @return the template or null if a template with the id does not exist.
     * @throws NullPointerException if id is null.
     */
    public T getTemplate(UUID id);

    /**
     * Get a short descriptive string about the location of the template.
     * This should give the user an idea where the template comes from. It can
     * be a path to a file, or the name of a company with a template name like
     * "Fibonacci (KNIME)" for a template from KNIME that generates the
     * Fibonacci numbers.
     * @param template the template
     * @return the string describing the location of the template or null if
     * no string could be generated.
     * @throws NullPointerException if template is null.
     */
    public String getDisplayLocation(T template);

    /**
     * Add listener to be notified when the list of templates changed.
     * @param l the listener
     */
    public void addChangeListener(ChangeListener l);

}