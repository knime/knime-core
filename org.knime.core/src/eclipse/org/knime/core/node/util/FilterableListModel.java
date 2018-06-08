package org.knime.core.node.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.AbstractListModel;

/**
 * List model which filters its items according to a search string and considers a list of excluded items.
 *
 * @author Jonathan Hale, KNIME GmbH, Konstanz, Germany
 * @since 3.6
 */
public final class FilterableListModel extends AbstractListModel<String> {

    private static final long serialVersionUID = 1L;

    private final List<String> m_unfiltered;

    private List<String> m_filtered;

    private Collection<String> m_excluded = Collections.emptyList();

    /* Filter string, only ever null to force refiltering */
    private String m_filter = "";

    /**
     * Constructor
     *
     * @param unfiltered Unfiltered list of elements
     */
    public FilterableListModel(final List<String> unfiltered) {
        m_filtered = m_unfiltered = unfiltered;
    }

    @Override
    public int getSize() {
        return m_filtered.size();
    }

    @Override
    public String getElementAt(final int index) {
        return m_filtered.get(index);
    }

    /**
     * Set the string with which to filter the elements of this list.
     *
     * @param filter Filter string
     */
    public synchronized void setFilter(final String filter) {
        if (m_filter != null && m_filter.equals(filter)) {
            return;
        }
        if (m_filter != null && filter.startsWith(m_filter)) {
            // the most common use case will be a list gradually refined by user typing more characters
            m_filtered = m_filtered.stream().filter(s -> s.contains(filter)).collect(Collectors.toList());
        } else if (filter.isEmpty()) {
            // copy the full list
            m_filtered = new ArrayList<>(m_unfiltered);
        } else {
            m_filtered = m_unfiltered.stream().filter(s -> s.contains(filter)).collect(Collectors.toList());
        }
        m_filter = filter;
        m_filtered.removeAll(m_excluded);
        this.fireContentsChanged(this, 0, m_unfiltered.size());
    }

    /**
     * Set list of excluded elements. Will cause the list to be refiltered.
     *
     * @param list Collection of elements to exclude.
     */
    public synchronized void setExcluded(final String[] list) {
        setExcluded(Arrays.asList(list));
    }

    /**
     * Set list of excluded elements. Will cause the list to be refiltered.
     *
     * @param list Collection of elements to exclude.
     */
    public synchronized void setExcluded(final Collection<String> list) {
        m_excluded = list;
        final String filter = m_filter;
        m_filter = null;
        setFilter(filter);
    }

    /**
     * @return Currently excluded elements
     */
    public Collection<String> getExcluded() {
        return Collections.unmodifiableCollection(m_excluded);
    }
}
