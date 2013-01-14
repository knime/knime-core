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
 *  propagated with or for interoperation with KNIME. The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 *
 * History
 *   24.11.2011 (hofer): created
 */
package org.knime.base.node.jsnippet.ui;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collection;

import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.ListSelectionModel;

import org.knime.core.node.util.FlowVariableListCellRenderer;
import org.knime.core.node.workflow.FlowVariable;

/**
 * A component that presents a list of flow variables for the snippet dialogs.
 *
 * @author Heiko Hofer
 */
@SuppressWarnings("serial")
public class FlowVariableList extends JList {
    private JSnippetTextArea m_snippet;
    private JSnippetFieldsController m_fields;

    /**
     *
     */
    public FlowVariableList() {
        super(new DefaultListModel());
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        setToolTipText(""); // enable tooltip
        addKeyListener(new KeyAdapter() {
            /** {@inheritDoc} */
            @Override
            public void keyTyped(final KeyEvent e) {
                if (e.getKeyChar() == KeyEvent.VK_ENTER) {
                    Object selected = getSelectedValue();
                    if (selected != null) {
                        onSelectionInVariableList(selected);
                    }
                }
            }
        });
        addMouseListener(new MouseAdapter() {
            /** {@inheritDoc} */
            @Override
            public void mouseClicked(final MouseEvent e) {
                if (e.getClickCount() == 2) {
                    Object selected = getSelectedValue();
                    if (selected != null) {
                        onSelectionInVariableList(selected);
                    }
                }
            }
        });
        setCellRenderer(new FlowVariableListCellRenderer());
    }

    private void onSelectionInVariableList(final Object selected) {
        if (selected != null && selected instanceof FlowVariable) {

            if (null == m_fields) {
                return;
            }
            FlowVariable v = (FlowVariable) selected;
            String enter = m_fields.getFieldReadStatement(v);
            clearSelection();
            if (null != m_snippet) {
                m_snippet.replaceSelection(enter);
                m_snippet.requestFocus();
            }
        }
    }

    /**
     * A double click on an element will perform an insection to this text area.
     *
     * @param snippet the text area
     */
    public void install(final JSnippetTextArea snippet) {
        m_snippet = snippet;
    }


    /**
     * The JSnippetFieldsController will create a statement when a element is
     * double clicked. The statement is then inserted in the textarea set by
     * install(JSnippetTextArea snippet).
     *
     * @param fields the field controller
     */
    public void install(final JSnippetFieldsController fields) {
        m_fields = fields;
    }

    /**
     * Set the flow variables to display.
     * @param flowVars the flow variables.
     */
    public void setFlowVariables(final Collection<FlowVariable> flowVars) {
        DefaultListModel fvListModel =
            (DefaultListModel)getModel();
        fvListModel.removeAllElements();
        for (FlowVariable v : flowVars) {
            fvListModel.addElement(v);
        }

    }

}
