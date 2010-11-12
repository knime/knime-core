/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2009
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
 *   03.08.2010 (hofer): created
 */
package org.knime.base.node.io.tablecreator.table;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedMap;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.knime.base.node.io.filereader.ColProperty;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.def.StringCell;

/**
 * A swing action to adjust the properties of a column.
 *
 * @author Heiko Hofer
 */
class PropertyColumnsAction extends AbstractAction {
    private static final long serialVersionUID = 5600265508614151001L;
    private JTable m_table;

    /**
     * Creates a new instance.
     *
     * @param table the 'model' for this action
     */
    PropertyColumnsAction(final JTable table) {
        super("Column Properties...");
        m_table = table;
        m_table.getColumnModel().getSelectionModel().addListSelectionListener(new ListSelectionListener() {

            public void valueChanged(final ListSelectionEvent e) {
                ListSelectionModel sel =
                    m_table.getColumnModel().getSelectionModel();
                setEnabled(!sel.isSelectionEmpty()
                        && sel.getMinSelectionIndex()
                            == sel.getMaxSelectionIndex());
            }
        });
    }



    /**
     * {@inheritDoc}
     */
    public void actionPerformed(final ActionEvent e) {
        int colIdx = m_table.getColumnModel().getSelectionModel()
                        .getMinSelectionIndex();
        SortedMap<Integer, ColProperty> props =
            ((SpreadsheetTableModel)m_table.getModel()).getColumnProperties();

        ColProperty target = props.get(colIdx);
        if (null == target) {
            target = createDefaultColumnProperty(props);
        }
        Vector<ColProperty> colProps = new Vector<ColProperty>();
        for(Integer idx : props.keySet()) {
            if (idx != colIdx) {
                colProps.add(props.get(idx));
            }
        }
        colProps.add(target);
        Frame parent = (Frame)SwingUtilities.getAncestorOfClass(
                Frame.class, m_table);
//
//        Vector<ColProperty> result =
//            org.knime.base.node.io.filereader.ColPropertyDialog.openUserDialog(
//                    parent,
//                colProps.size() - 1,
//                colProps);
        Vector<ColProperty> result =
            org.knime.base.node.io.tablecreator.prop.ColPropertyDialog.openUserDialog(
                    parent,
                colProps.size() - 1,
                colProps);
        if (null != result) {
            //props.put(colIdx, result.get(colProps.size() - 1));
            ((SpreadsheetTableModel)m_table.getModel()).setColProperty(colIdx,
                    result.get(colProps.size() - 1));
        }
    }

    /**
     * Generates a {@link ColProperty} object with default settings.
     * @param props Properties of the current columns of the table
     * @return a {@link ColProperty} object with default settings
     */
    static ColProperty createDefaultColumnProperty(
            final SortedMap<Integer, ColProperty> props) {
        DataColumnSpec firstColSpec =
            new DataColumnSpecCreator(getUniqueName(props), StringCell.TYPE)
                        .createSpec();
        ColProperty target = new ColProperty();
        target.setColumnSpec(firstColSpec);
        // this will cause it to be ignored when re-analyzing:
        target.setUserSettings(false);

        target.setMissingValuePattern("");
        return target;
    }

    /**
     * Generates a name following the pattern: "column <integer>"
     */
    private static String getUniqueName(final SortedMap<Integer,
            ColProperty> props) {
        Set<String> names = new HashSet<String>();
        for(ColProperty prop : props.values()) {
            names.add(prop.getColumnSpec().getName());
        }
        int i = 1;
        String prefix = "column";
        String newName = prefix + i;
        while(names.contains(newName)) {
            i++;
            newName = prefix + i;
        }
        return newName;
    }

}
