/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ---------------------------------------------------------------------
 *
 * Created on Dec 6, 2013 by Patrick Winter, KNIME.com AG, Zurich, Switzerland
 */
package org.knime.core.node.util.filter;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.border.Border;

/**
 * Preview twin list that shows the included and excluded elements.
 *
 * @author Patrick Winter, KNIME.com AG, Zurich, Switzerland
 * @param <T> The type of elements that are in- or excluded
 * @since 2.10
 */
@SuppressWarnings("serial")
public final class FilterIncludeExcludePreview<T> extends JPanel {

    private Border m_borderIncludeEnabled;

    private Border m_borderIncludeDisabled;

    private Border m_borderExcludeEnabled;

    private Border m_borderExcludeDisabled;

    private JList<T> m_includeList;

    private DefaultListModel<T> m_includeListModel;

    private JScrollPane m_includePane;

    private JList<T> m_excludeList;

    private DefaultListModel<T> m_excludeListModel;

    private JScrollPane m_excludePane;

    /**
     * Creates a preview with the default titles "Included" and "Excluded".
     *
     * @param renderer The renderer that is used to render the elements in the list
     */
    public FilterIncludeExcludePreview(final ListCellRenderer<T> renderer) {
        this("Included", "Excluded", renderer);
    }

    /**
     * Creates a preview.
     *
     * @param includeTitle The title for the include list
     * @param excludeTitle The title for the exclude list
     * @param renderer The renderer that is used to render the elements in the list
     */
    public FilterIncludeExcludePreview(final String includeTitle, final String excludeTitle,
        final ListCellRenderer<T> renderer) {
        m_borderIncludeEnabled =
            BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(0, 221, 0), 2), includeTitle);
        m_borderIncludeDisabled =
            BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.GRAY, 2), includeTitle);
        m_borderExcludeEnabled =
            BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(240, 0, 0), 2), excludeTitle);
        m_borderExcludeDisabled =
            BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.GRAY, 2), excludeTitle);
        setLayout(new GridLayout(1, 2));
        m_includeListModel = new DefaultListModel<T>();
        m_includeList = new JList<T>(m_includeListModel);
        m_includeList.setSelectionBackground(super.getBackground());
        m_includeList.setBackground(super.getBackground());
        m_excludeListModel = new DefaultListModel<T>();
        m_excludeList = new JList<T>(m_excludeListModel);
        m_excludeList.setSelectionBackground(super.getBackground());
        m_excludeList.setBackground(super.getBackground());
        m_includeList.setCellRenderer(renderer);
        m_excludeList.setCellRenderer(renderer);
        m_includePane = new JScrollPane(m_includeList);
        m_excludePane = new JScrollPane(m_excludeList);
        setEnabled(true);
        add(m_excludePane);
        add(m_includePane);
    }

    /**
     * Updates the preview lists.
     *
     * @param includedTs The Ts that are included
     * @param excludedTs The Ts that are excluded
     */
    public void update(final List<T> includedTs, final List<T> excludedTs) {
        // Clear the lists
        m_includeListModel.clear();
        m_excludeListModel.clear();
        for (T t : includedTs) {
            m_includeListModel.addElement(t);
        }
        for (T t : excludedTs) {
            m_excludeListModel.addElement(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setEnabled(final boolean enabled) {
        super.setEnabled(enabled);
        m_includeList.setEnabled(enabled);
        m_includePane.setEnabled(enabled);
        m_excludeList.setEnabled(enabled);
        m_excludePane.setEnabled(enabled);
        if (enabled) {
            m_includePane.setBorder(m_borderIncludeEnabled);
            m_excludePane.setBorder(m_borderExcludeEnabled);
        } else {
            m_includePane.setBorder(m_borderIncludeDisabled);
            m_excludePane.setBorder(m_borderExcludeDisabled);
        }
    }

    /**
     * Set the size of a list.
     *
     * @param size The size of one list
     */
    public void setListSize(final Dimension size) {
        m_includePane.setPreferredSize(size);
        m_excludePane.setPreferredSize(size);
    }

}
