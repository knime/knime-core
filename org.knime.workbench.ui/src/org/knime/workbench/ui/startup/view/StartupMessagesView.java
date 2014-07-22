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
 *   21.07.2014 (thor): created
 */
package org.knime.workbench.ui.startup.view;

import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.part.ViewPart;
import org.knime.workbench.core.util.ImageRepository;
import org.knime.workbench.core.util.ImageRepository.SharedImages;
import org.knime.workbench.ui.startup.StartupMessage;

/**
 * View that shows startup messages provided by other plug-ins.
 *
 * @author Thorsten Meinl, KNIME.com, Zurich, Switzerland
 */
public class StartupMessagesView extends ViewPart {
    private TableViewer m_tableViewer;

    /**
     * Creates a new view.
     */
    public StartupMessagesView() {
        setPartName("Startup Messages");
        setTitleImage(ImageRepository.getImage(SharedImages.StartupMessages));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void createPartControl(final Composite parent) {
        Table table = new Table(parent, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
        m_tableViewer = new TableViewer(table);
        table.setHeaderVisible(true);
        table.setLinesVisible(true);
        m_tableViewer.setContentProvider(new StartupMessageContentProvider());
        m_tableViewer.setLabelProvider(new StartupMessageLabelProvider());
        m_tableViewer.setComparator(new ViewerComparator() {
            /**
             * {@inheritDoc}
             */
            @Override
            public int compare(final Viewer viewer, final Object e1, final Object e2) {
                if ((e1 instanceof StartupMessage) && (e2 instanceof StartupMessage)) {
                    StartupMessage m1 = (StartupMessage)e1;
                    StartupMessage m2 = (StartupMessage)e2;

                    // errors first, then warnings, then infos
                    return -m1.getType() + m2.getType();
                } else {
                    return e1.toString().compareTo(e2.toString());
                }
            }
        });

        createTableColumns(table);
        m_tableViewer.setInput(StartupMessage.getAllStartupMessages());
        table.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetSelected(final SelectionEvent event) {
                // nothing to do
            }

            @Override
            public void widgetDefaultSelected(final SelectionEvent event) {
                TableItem tableCell = (TableItem)event.item;
                StartupMessage msg = (StartupMessage)tableCell.getData();
                StartupMessageDialog dlg = new StartupMessageDialog(event.display.getActiveShell(), msg);
                dlg.open();
            }
        });
    }

    private void createTableColumns(final Table tree) {
        TableColumn messageColumn = new TableColumn(tree, SWT.LEFT);
        messageColumn.setText("Message");
        messageColumn.setWidth(520);
        messageColumn.setResizable(true);
        messageColumn.setMoveable(true);

        TableColumn pluginColumn = new TableColumn(tree, SWT.LEFT);
        pluginColumn.setText("Plug-in");
        pluginColumn.setWidth(8);
        pluginColumn.setResizable(true);
        pluginColumn.setMoveable(true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setFocus() {
        m_tableViewer.getTable().setFocus();
    }
}
