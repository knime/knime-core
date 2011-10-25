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
 */
package org.knime.base.node.io.database;

import java.awt.BorderLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import org.knime.core.node.NodeLogger;

/**
 * Class implements a tree that shows all available tables grouped by table
 * types together with their table names and column names (requested on 
 * demand). 
 * 
 * @author Thomas Gabriel, KNIME.com AG, Zurich, Switzerland
 */
public class DBTreeBrowser extends JPanel implements TreeSelectionListener {
    
    private final JTree m_tree;
    private DatabaseMetaData m_meta;
    
    private final DefaultMutableTreeNode m_root = 
            new DefaultMutableTreeNode("ROOT");
    
    private static final NodeLogger LOGGER = 
            NodeLogger.getLogger(DBTreeBrowser.class);
    
    /**
     * Create a new database browser.
     * @param editor to which table and table column names are added
     */
    public DBTreeBrowser(final JEditorPane editor) {
        super(new BorderLayout());
        m_tree = new JTree(m_root);
        m_tree.setRootVisible(false);
        m_tree.setToggleClickCount(1);
        m_tree.getSelectionModel().setSelectionMode(
                TreeSelectionModel.SINGLE_TREE_SELECTION);
        m_tree.addMouseListener(new MouseAdapter() {
            /** {@inheritDoc} */
            @Override
            public final void mouseClicked(final MouseEvent me) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode)
                        m_tree.getLastSelectedPathComponent();
                if (node == null) {
                    return;
                }
                final String nodeInfo = node.toString();
                if (node.getLevel() < 2) {
                    return;
                }
                if (me.getClickCount() == 2) {
                    if (node.getLevel() == 3) { // column name
                        TreeNode tableNode = node.getParent();
                        editor.replaceSelection(tableNode.toString() 
                                + "." +  nodeInfo);
                    } else { // table name
                        editor.replaceSelection(nodeInfo);
                    }
                    editor.requestFocus();
                }
            }
        });
        m_tree.addTreeSelectionListener(this);
        final JScrollPane jsp = new JScrollPane(m_tree);
        super.add(jsp, BorderLayout.CENTER);
    }
    
    /**
     * Update this tree and metadata from database.
     * @param meta <code>DatabaseMetaData</code> used to retrieve table names 
     *             and column names from.
     */
    final synchronized void update(final DatabaseMetaData meta) {
        m_meta = meta;
        m_tree.collapsePath(new TreePath(m_root));
        m_root.removeAllChildren();
        if (meta != null) {
            ArrayList<String> tableTypes = new ArrayList<String>();
            try {
                ResultSet rsTableTypes = meta.getTableTypes();
                while (rsTableTypes.next()) {
                    final String tableName = rsTableTypes.getString(1);
                    tableTypes.add(tableName);

                }
                rsTableTypes.close();
            } catch (SQLException sqle) {
                LOGGER.warn("Could not get table types from database, reason: "
                        + sqle.getMessage(), sqle);
            }
            LOGGER.debug("Fetching table types: " + tableTypes);
            for (String type : tableTypes) {
                String[] tableNames = null;
                try {
                     tableNames = getTableNames(type);
                } catch (Exception e) {
                     LOGGER.debug("Could fetch database metadata of type '" 
                             + type + "', reason: " + e.getMessage());
                }
                if (tableNames == null || tableNames.length == 0) {
                    LOGGER.info("No database metainfo on type '" + type + "'.");
                    continue;
                }
                final DefaultMutableTreeNode typeNode = 
                        new DefaultMutableTreeNode(type);
                typeNode.setAllowsChildren(true);
                for (final String table : tableNames) {
                     final DefaultMutableTreeNode tableNode = 
                                new DefaultMutableTreeNode(table);
                     tableNode.setAllowsChildren(true);
                     typeNode.add(tableNode);
                }
                m_root.add(typeNode);
            }
        }
        m_tree.expandPath(new TreePath(m_root));
        m_tree.repaint();
    }
    
    /** {@inheritDoc} */
    @Override
    public void valueChanged(final TreeSelectionEvent event) {
        final DefaultMutableTreeNode node = (DefaultMutableTreeNode)
                m_tree.getLastSelectedPathComponent();
        if (node == null) {
            return;
        }
        final String nodeInfo = node.toString();
        try {
            String[] columnNames = getColumnNames(nodeInfo);
            for (String colName : columnNames) {
                final DefaultMutableTreeNode child = 
                        new DefaultMutableTreeNode(colName);
                child.setAllowsChildren(false);
                node.add(child);
            } 
        } catch (SQLException sqle) {
            LOGGER.debug(sqle);
        }
    }
    
    private String[] getTableNames(final String type) throws SQLException {
        final ResultSet rs = m_meta.getTables(null, null, "%", 
                new String[]{type});
        final ArrayList<String> tableNames = new ArrayList<String>();
        while (rs.next()) {
            final String tableName = rs.getString("TABLE_NAME");
            tableNames.add(tableName);

        }
        rs.close();
        return tableNames.toArray(new String[tableNames.size()]);
    }

    private String[] getColumnNames(final String tableName) 
            throws SQLException {
        final ArrayList<String> columnNames = new ArrayList<String>();
        ResultSet rsc = m_meta.getColumns(null, null, tableName, null);
        while (rsc.next()) {
            final String columnName = rsc.getString("COLUMN_NAME");
            columnNames.add(columnName);
        }
        rsc.close();
        return columnNames.toArray(new String[columnNames.size()]);
    }

}
