/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
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
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * -------------------------------------------------------------------
 *
 * History
 *   11.01.2006 (Florian Georg): created
 */
package org.knime.workbench.repository.view;

import java.util.Arrays;

import org.eclipse.jface.action.ControlContribution;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/**
 * Contribution Item in der RepositoryView. This registers a
 * <code>RepositoryViewFilter</code> on the viewer, that is able to filter
 * rquested nodes.
 *
 * @see RepositoryViewFilter
 *
 * @author Florian Georg, University of Konstanz
 */
public class FilterViewContributionItem extends ControlContribution implements
        KeyListener {
    private final TreeViewer m_viewer;

    private Combo m_combo;

    private final TextualViewFilter m_filter;

    private final boolean m_liveUpdate;

    /**
     * Creates the contribution item.
     *
     * @param viewer The viewer.
     * @param filter The filter to use.
     */
    public FilterViewContributionItem(final TreeViewer viewer,
            final TextualViewFilter filter) {
       this(viewer, filter, true);
    }

    /**
     * Creates the contribution item.
     *
     * @param viewer The viewer.
     * @param filter The filter to use.
     * @param liveUpdate Set to true if the filter should be updated on every
     *      key pressed. If false, it is only updated on pressing enter.
     *
     */
    public FilterViewContributionItem(final TreeViewer viewer,
            final TextualViewFilter filter, final boolean liveUpdate) {
        super("org.knime.workbench.repository.view.FilterViewContributionItem");
        m_viewer = viewer;
        m_filter = filter;
        m_viewer.addFilter(m_filter);
        m_liveUpdate = liveUpdate;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Control createControl(final Composite parent) {
        m_combo = new Combo(parent, SWT.DROP_DOWN);
        m_combo.addKeyListener(this);
        m_combo.addSelectionListener(createSelectionAdaptor());
        return m_combo;
    }

    /**
     * @return a selection adaptor that updates the filter
     */
    protected SelectionAdapter createSelectionAdaptor() {
        return new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                getFilter().setQueryString(getCombo().getText());
                getViewer().refresh();
                if (getCombo().getText().length() > 0) {
                    getViewer().expandAll();
                }
            }

        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected int computeWidth(final Control control) {
        return Math.max(super.computeWidth(control), 150);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void keyPressed(final KeyEvent e) {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void keyReleased(final KeyEvent e) {
        boolean shouldExpand = true;
        String str = m_combo.getText();
        boolean update = m_liveUpdate;

        if (e.character == SWT.CR) {
            if ((str.length() > 0)
                    && (!Arrays.asList(m_combo.getItems()).contains(str))) {
                m_combo.add(str, 0);
                m_combo.select(0);
            }
            update = true;
        } else if (e.character == SWT.ESC) {
            m_combo.setText("");
            str = "";
            update = true;
        }

        if (str.length() == 0) {
            m_viewer.collapseAll();
            shouldExpand = false;
            update = true;
        }
        m_filter.setQueryString(str);

        if (update) {
            m_viewer.refresh();
            if (shouldExpand) {
                m_viewer.expandAll();
            }
        }
    }

    /**
     * @return the combo
     */
    protected Combo getCombo() {
        return m_combo;
    }

    /**
     * @param combo the combo to set
     */
    protected void setCombo(final Combo combo) {
        m_combo = combo;
    }

    /**
     * @return the filter
     */
    protected TextualViewFilter getFilter() {
        return m_filter;
    }

    /**
     * @return the liveUpdate
     */
    protected boolean getLiveUpdate() {
        return m_liveUpdate;
    }

    /**
     * @return the viewer
     */
    protected TreeViewer getViewer() {
        return m_viewer;
    }
}
