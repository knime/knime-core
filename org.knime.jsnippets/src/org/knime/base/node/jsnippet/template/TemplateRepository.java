/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
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
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
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
 *  propagated with or for interoperation with KNIME. The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 *
 * History
 *   30.05.2012 (hofer): created
 */
package org.knime.base.node.jsnippet.template;

import java.util.Collection;
import java.util.UUID;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;

import org.knime.base.node.jsnippet.JSnippetTemplate;

/**
 *
 * @author Heiko Hofer
 */
public abstract class TemplateRepository {
    private EventListenerList m_listenerList = new EventListenerList();
    private ChangeEvent m_changeEvent;

    /**
     * Get the {@link JSnippetTemplate}s in the given meta category.
     * @param metaCategories only templates from these
     * meta categories will be returned.
     * @return the {@link JSnippetTemplate}s in the given meta category
     */
    @SuppressWarnings("rawtypes")
    public abstract Collection<JSnippetTemplate> getTemplates(
            final Collection<Class> metaCategories);

    /**
     * Test if a template can be removed. Returns only true if the template
     * is in this repository.
     * @param template the template
     * @return true when removeTemplate(template) could be successful
     */
    public abstract boolean isRemoveable(final JSnippetTemplate template);

    /**
     * Remove the given template.
     * @param template the template to be removed
     * @return when the template is successfully removed
     */
    public abstract boolean removeTemplate(final JSnippetTemplate template);

    /**
     * Get the template with the given id.
     * @param id the id
     * @return the template or null if a template with the id does not exist.
     * @throws NullPointerException if id is null.
     */
    public abstract JSnippetTemplate getTemplate(final UUID id);

    /**
     * Get a short descriptive string about the location of the template.
     * This should give the user an idea where the template comes from. It can
     * be a path to a file, or the name of a company with a template name like
     * "Fibonacci (KNIME)" for a template from KNIME that generates the
     * Fibonacci numbers.
     * @param template the template
     * @return the string describing the location of the template
     * @throws NullPointerException if template is null.
     */
    public abstract String getDisplayLocation(final JSnippetTemplate template);

    /**
     * Add listener to be notified when the list of templates changed.
     * @param l the listener
     */
    public void addChangeListener(final ChangeListener l) {
        m_listenerList.add(ChangeListener.class, l);
    }


    /**
     * Remove listener from the list of listeners.
     * @param l the listener to be removed
     */
    public void removeChangeListener(final ChangeListener l) {
        m_listenerList.remove(ChangeListener.class, l);
    }


    /**
     * Notify all listeners that have registered interest for
     * notification on this event type.
     */
    protected void fireStateChanged() {
        // Guaranteed to return a non-null array
        Object[] listeners = m_listenerList.getListenerList();
        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == ChangeListener.class) {
                // Lazily create the event:
                if (m_changeEvent == null) {
                    m_changeEvent = new ChangeEvent(this);
                }
                ((ChangeListener)listeners[i + 1]).stateChanged(m_changeEvent);
            }
        }
    }
}
