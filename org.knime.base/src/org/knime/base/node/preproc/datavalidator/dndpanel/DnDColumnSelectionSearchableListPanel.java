/*
 * ------------------------------------------------------------------------
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
 * Created on 29.10.2013 by NanoTec
 */
package org.knime.base.node.preproc.datavalidator.dndpanel;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.TransferHandler;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.util.ColumnSelectionList;
import org.knime.core.node.util.ColumnSelectionSearchableListPanel;
import org.knime.core.node.util.ColumnSelectionSearchableListPanel.ConfigurationRequestEvent.Type;

/**
 * A panel comprising a column list, search field and some search customizers for the user. The list should be
 * initialized using {@link #update(DataTableSpec)} afterwards the returned {@link ListModifier} can be used to
 * add/remove additional {@link DataColumnSpec}s. Usually additional columns are used to add 'invalid' columns, for
 * which a model specific configuration does exist but which does actually not exist any more in the input table. See
 * {@link ListModifier} to get more information about additional columns.
 *
 * @author Marcel Hanser
 * @since 2.10
 */
@SuppressWarnings("serial")
public final class DnDColumnSelectionSearchableListPanel extends ColumnSelectionSearchableListPanel {

    private final ConfiguredColumnDeterminer m_configuredColumnDeterminer;

    /**
     * @param searchedItemsSelectionMode mode
     * @param configuredColumnDeterminer determiner
     */
    public DnDColumnSelectionSearchableListPanel(final SearchedItemsSelectionMode searchedItemsSelectionMode,
        final ConfiguredColumnDeterminer configuredColumnDeterminer) {
        super(searchedItemsSelectionMode, configuredColumnDeterminer);
        m_configuredColumnDeterminer = configuredColumnDeterminer;
    }

    /**
     * Convenient method to enable the drag and drop support of the list view at the left side.
     *
     * @param dndStateListener notified if drag is started and finished
     * @since 2.11
     */
    public void enableDragAndDropSupport(final DnDStateListener dndStateListener) {
        ColumnSelectionList columnList = getColumnList();
        columnList.setDragEnabled(true);
        JPopupMenu popup = new JPopupMenu();
        final JMenuItem jMenuItem = new JMenuItem("New Configuration", DnDConfigurationPanel.ADD_ICON_16);
        popup.add(jMenuItem);
        popup.addPopupMenuListener(new PopupMenuListener() {

            @Override
            public void popupMenuWillBecomeVisible(final PopupMenuEvent e) {
                boolean enable = true;
                for (DataColumnSpec col : getSelectedColumns()) {
                    if (m_configuredColumnDeterminer.isConfiguredColumn(col)) {
                        enable = false;
                        break;
                    }
                }
                jMenuItem.setEnabled(enable);
            }

            @Override
            public void popupMenuWillBecomeInvisible(final PopupMenuEvent e) {
            }

            @Override
            public void popupMenuCanceled(final PopupMenuEvent e) {
            }
        });
        jMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                fireConfigurationRequested(Type.CREATION);
            }
        });
        columnList.setComponentPopupMenu(popup);
        final TransferHandler handler = columnList.getTransferHandler();
        columnList.setTransferHandler(new DnDColumnSpecSourceTransferHandler(handler, dndStateListener) {

            @Override
            protected List<DataColumnSpec> getColumnsSpecsToDrag() {
                return getSelectedColumns();
            }
        });

    }
}
