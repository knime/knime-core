/*
 * ------------------------------------------------------------------------
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
 * Created on 23.10.2013 by Christian Albrecht, KNIME.com AG, Zurich, Switzerland
 */
package org.knime.workbench.workflowcoach.prefs;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Widget;
import org.knime.workbench.explorer.ExplorerMountTable;
import org.knime.workbench.explorer.view.AbstractContentProvider;
import org.knime.workbench.explorer.view.AbstractContentProviderFactory;

/**
 *
 * @author Martin Horn, University of Konstanz
 */
public class ServerMountPointTable extends CheckboxTableViewer {
    private static final int MOUNT_TYPE_PROP = 2;

    private static final int MOUNT_ID_PROP = 1;

    private class MountPointTableLabelProvider implements ITableLabelProvider {

        @Override
        public Image getColumnImage(final Object element, final int columnIndex) {
            switch (columnIndex) {
                case MOUNT_TYPE_PROP:
                case MOUNT_ID_PROP:
                    String mountID = (String)element;
                    return ExplorerMountTable.getMountedContent().get(mountID).getFactory().getImage();

                default:
                    break;
            }

            return null;
        }

        @Override
        public String getColumnText(final Object element, final int columnIndex) {
            String mountID = (String) element;
            AbstractContentProviderFactory factory;

            switch (columnIndex) {
                case MOUNT_TYPE_PROP:
                    factory = ExplorerMountTable.getMountedContent().get(mountID).getFactory();
                    return factory.toString();

                case MOUNT_ID_PROP:
                    return ExplorerMountTable.getMountPoint(mountID).getMountID();

                default:
                    break;
            }

            return null;
        }

        @Override
        public void addListener(final ILabelProviderListener listener) {
        }

        @Override
        public void dispose() {
        }

        @Override
        public boolean isLabelProperty(final Object element, final String property) {
            return false;
        }

        @Override
        public void removeListener(final ILabelProviderListener listener) {
        }
    }

    private Table m_table;

    private SelectionListener m_selectionListener;

    private List<String> m_mountIDs;

    /**
     * Creates a new {@link ServerMountPointTable}.
     *
     * @param parent The parent component
     */
    public ServerMountPointTable(final Composite parent) {
        super(
            new Table(parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.SINGLE | SWT.BORDER | SWT.FULL_SELECTION | SWT.CHECK));

        m_table = getTable();
        m_table.setHeaderVisible(true);
        m_table.setLinesVisible(true);

        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.heightHint = getTableHeightHint(m_table, 3);
        gd.horizontalSpan = 1;
        gd.widthHint = new PixelConverter(parent).convertWidthInCharsToPixels(30);

        m_table.addSelectionListener(getSelectionListener());

        TableLayout tableLayout = new TableLayout();
        m_table.setLayout(tableLayout);
        m_table.setLayoutData(gd);

        setUseHashlookup(true);
        setContentProvider(ArrayContentProvider.getInstance());
        setLabelProvider(new MountPointTableLabelProvider());

        TableColumn checkCol = new TableColumn(m_table, SWT.NONE);
        checkCol.setText("Use");
        checkCol.setWidth(50);
        checkCol.setResizable(false);

        TableColumn mountCol = new TableColumn(m_table, SWT.NONE);
        mountCol.setText("MountID");
        mountCol.setWidth(250);
        mountCol.setResizable(true);

        mountCol = new TableColumn(m_table, SWT.NONE);
        mountCol.setText("Mounted Type");
        mountCol.setWidth(150);
        mountCol.setResizable(true);

        //filter only the mounted servers
        m_mountIDs = getServerMountIDs();

        setInput(m_mountIDs);

    }

    /**
     * @return the id's of all servers currently mounted
     */
    public static List<String> getServerMountIDs() {
        return ExplorerMountTable.getAllMountedIDs().stream().filter(s -> {
            AbstractContentProvider abstractContentProvider = ExplorerMountTable.getMountedContent().get(s);
            return abstractContentProvider == null ? false : abstractContentProvider.isRemote();
        }).collect(Collectors.toList());
    }

    /**
     * Sets the selection from a comma-separated list of mount-ids.
     *
     * @param selection
     */
    public void setSelectedServers(final String selection) {
        super.setCheckedElements(selection.split(","));
    }


    /**
     * Gets the currently selected servers as a comma-separated list of mount-ids.
     *
     * @return comma-separated list of mount-ids.
     */
    public String getSelectedServers() {
        Object[] objs = super.getCheckedElements();
        return Arrays.stream(objs).map(Object::toString).collect(Collectors.joining(","));
    }

    private int getTableHeightHint(final Table table, final int rows) {
        if (table.getFont().equals(JFaceResources.getDefaultFont())) {
            table.setFont(JFaceResources.getDialogFont());
        }
        int result = table.getItemHeight() * rows + table.getHeaderHeight();
        if (table.getLinesVisible()) {
            result += table.getGridLineWidth() * (rows - 1);
        }
        return result;
    }

    /**
     * Returns this field editor's selection listener. The listener is created if necessary.
     *
     * @return the selection listener
     */
    private SelectionListener getSelectionListener() {
        if (m_selectionListener == null) {
            createSelectionListener();
        }
        return m_selectionListener;
    }

    /**
     * Creates a selection listener.
     */
    private void createSelectionListener() {
        m_selectionListener = new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent event) {
                Widget widget = event.widget;
                if (widget == m_table) {
                    selectionChanged();
                }
            }
        };
    }

    private void selectionChanged() {
        // TODO what to do?
    }
}
