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
 * History
 *   01.06.2016 (thor): created
 */
package org.knime.workbench.workflowcoach.ui;

import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.knime.workbench.workflowcoach.NodeRecommendationManager.NodeRecommendation;

/**
 * Sorter for the workflow coach view.
 *
 * @author Thorsten Meinl, KNIME.com, Zurich, Switzerland
 */
class TableColumnSorter extends ViewerComparator implements SelectionListener {
    public static final int ASC = 1;

    public static final int NONE = 0;

    public static final int DESC = -1;

    private int m_direction = 0;

    private TableColumn m_selectedColumn = null;

    private int m_columnIndex = 0;

    private final TableViewer m_viewer;

    public TableColumnSorter(final TableViewer viewer) {
        m_viewer = viewer;
    }

    public void setColumn(final TableColumn selectedColumn) {
        if (m_selectedColumn == selectedColumn) {
            switch (m_direction) {
                case ASC:
                    m_direction = DESC;
                    break;
                case DESC:
                    m_direction = ASC;
                    break;
                default:
                    m_direction = ASC;
                    break;
            }
        } else {
            m_selectedColumn = selectedColumn;
            m_direction = ASC;
        }

        Table table = m_viewer.getTable();
        switch (m_direction) {
            case ASC:
                table.setSortColumn(selectedColumn);
                table.setSortDirection(SWT.UP);
                break;
            case DESC:
                table.setSortColumn(selectedColumn);
                table.setSortDirection(SWT.DOWN);
                break;
            default:
                table.setSortColumn(null);
                table.setSortDirection(SWT.NONE);
                break;
        }

        TableColumn[] columns = table.getColumns();
        for (int i = 0; i < columns.length; i++) {
            TableColumn theColumn = columns[i];
            if (theColumn == m_selectedColumn) {
                m_columnIndex = i;
                break;
            }
        }
        m_viewer.refresh();
    }

    @Override
    public int compare(final Viewer viewer, final Object e1, final Object e2) {
        return m_direction * compare(e1, e2);
    }

    @SuppressWarnings("null")
    private int compare(final Object e1, final Object e2) {
        if (!(e1 instanceof NodeRecommendation[]) || !(e2 instanceof NodeRecommendation[]) || (m_columnIndex == 0)) {
            return 0;
        }
        NodeRecommendation r1 = ((NodeRecommendation[])e1)[m_columnIndex - 1];
        NodeRecommendation r2 = ((NodeRecommendation[])e2)[m_columnIndex - 1];

        if ((r1 == null) && (r2 == null)) {
            return 0;
        } else if ((r1 != null) && (r2 == null)) {
            return -1 * m_direction;
        } else if ((r1 == null) && (r2 != null)) {
            return 1 * m_direction;
        } else {
            return r1.getFrequency() - r2.getFrequency();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void widgetSelected(final SelectionEvent e) {
        setColumn((TableColumn)e.widget);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void widgetDefaultSelected(final SelectionEvent e) {
        // nothing to do
    }
}
