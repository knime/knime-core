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
 *   11.02.2015 (tibuch): created
 */
package org.knime.base.node.preproc.draganddroppanel.transferhandler;

import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JList;

/**
 * Transfer handler to move list items around.
 *
 * @author tibuch
 */
public class ListTransferHandler extends StringTransferHandler {

    private int[] m_indices = null;
    private int m_addIndex = -1;
    private int m_addCount = 0;

    @Override
    protected String exportString(final JComponent c) {
        JList<String> list = (JList<String>)c;
        m_indices = list.getSelectedIndices();
        List<String> values = list.getSelectedValuesList();

        StringBuffer buff = new StringBuffer();

        for (int i = 0; i < values.size(); i++) {
            String val = values.get(i);
            if (val.endsWith(" ")) {
                continue;
            }
            buff.append(val);
            if (i != values.size() - 1) {
                buff.append("\n");
            }
        }

        return buff.toString();
    }

    @Override
    protected void importString(final JComponent c, final String str) {
        JList<String> target = (JList<String>)c;
        DefaultListModel<String> listModel = (DefaultListModel<String>)target.getModel();
        int index = target.getSelectedIndex();

        //Prevent the user from dropping data back on itself.
        //For example, if the user is moving items #4,#5,#6 and #7 and
        //attempts to insert the items after item #5, this would
        //be problematic when removing the original items.
        //So this is not allowed.
        if (m_indices != null && index >= m_indices[0] - 1
                && index <= m_indices[m_indices.length - 1]) {
            m_indices = null;
            return;
        }

        int max = listModel.getSize();
        if (index < 0) {
            index = max;
        } else {
            index++;
            if (index > max) {
                index = max;
            }
        }
        m_addIndex = index;
        String[] values = str.split("\n");
        m_addCount = values.length;
        for (int i = 0; i < values.length; i++) {
            listModel.add(index++, values[i]);
        }
    }

    @Override
    protected void cleanup(final JComponent c, final boolean remove) {
        if (remove && m_indices != null) {
            JList<?> source = (JList<?>)c;
            DefaultListModel<?> model  = (DefaultListModel<?>)source.getModel();
            // adjust indices if we only moved listitems
            if (m_addCount > 0) {
                for (int i = 0; i < m_indices.length; i++) {
                    if (m_indices[i] > m_addIndex) {
                        m_indices[i] += m_addCount;
                    }
                }
            }
            for (int i = m_indices.length - 1; i >= 0; i--) {
                model.remove(m_indices[i]);
            }
        }
        m_indices = null;
        m_addCount = 0;
        m_addIndex = -1;
    }
}
