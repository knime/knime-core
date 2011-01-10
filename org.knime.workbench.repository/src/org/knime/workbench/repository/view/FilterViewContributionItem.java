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
    private TreeViewer m_viewer;

    private Combo m_combo;

    private RepositoryViewFilter m_filter;

    /**
     * Creates the contribution item.
     * 
     * @param viewer The viewer.
     */
    public FilterViewContributionItem(final TreeViewer viewer) {
        super("@knime.repository.view.filter");

        m_viewer = viewer;
        m_filter = new RepositoryViewFilter();
        m_viewer.addFilter(m_filter);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Control createControl(final Composite parent) {
        m_combo = new Combo(parent, SWT.DROP_DOWN);
        m_combo.addKeyListener(this);
        m_combo.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(final SelectionEvent e) {
                m_filter.setQueryString(m_combo.getText());
                m_viewer.refresh();
                if (m_combo.getText().length() > 0) {
                    m_viewer.expandAll();
                }

            }

        });
        
        return m_combo;
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
    public void keyPressed(final KeyEvent e) {

    }

    /**
     * {@inheritDoc}
     */
    public void keyReleased(final KeyEvent e) {
        String str = m_combo.getText();
        boolean shouldExpand = false;

        if (e.character == SWT.CR) {
            shouldExpand = true;
            if ((str.length() > 0)
                    && (!Arrays.asList(m_combo.getItems()).contains(str))) {
                m_combo.add(str, 0);
                m_combo.select(0);

            } else if (str.length() == 0) {
                m_viewer.collapseAll();
                shouldExpand = false;
            }

        } else if (e.character == SWT.ESC) {
            m_combo.setText("");
            str = "";
            m_viewer.collapseAll();
        }

        m_filter.setQueryString(str);

        m_viewer.refresh();

        if (shouldExpand) {
            m_viewer.expandAll();
        }
    }
}
