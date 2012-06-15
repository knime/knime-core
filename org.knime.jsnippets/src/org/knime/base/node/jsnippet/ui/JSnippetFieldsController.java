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
 *  propagated with or for interoperation with KNIME. The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 *
 * History
 *   16.01.2012 (hofer): created
 */
package org.knime.base.node.jsnippet.ui;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Map;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import org.knime.base.node.jsnippet.JavaSnippet;
import org.knime.base.node.jsnippet.JavaSnippetFields;
import org.knime.base.node.jsnippet.JavaSnippetSettings;
import org.knime.base.node.jsnippet.expression.Type;
import org.knime.base.node.jsnippet.type.TypeProvider;
import org.knime.base.node.jsnippet.type.data.DataValueToJava;
import org.knime.base.node.jsnippet.ui.FieldsTableModel.Column;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.node.workflow.FlowVariable;


/**
 * This class is the clue between the fields tables and the java snippet and
 * its text area.
 *
 * @author Heiko Hofer
 */
public class JSnippetFieldsController {
    private final JavaSnippet m_snippet;
    private final InFieldsTable m_inFieldsTable;
    private final OutFieldsTable m_outFieldsTable;
    /** flag to recreate snippets system fields only if needed. */
    private boolean m_inListenerArmed;
    /** flag to recreate snippets system fields only if needed. */
    private boolean m_outListenerArmed;

    /**
     * Create a new instance.
     * @param snippet code snippets will be entered in the document of this
     * snippet.
     * @param inFieldsTable java fields representing inputs
     * @param outFieldsTable java fields representing java snippet outputs
     */
    public JSnippetFieldsController(final JavaSnippet snippet,
            final InFieldsTable inFieldsTable,
            final OutFieldsTable outFieldsTable) {
        m_snippet = snippet;
        m_inFieldsTable = inFieldsTable;
        m_outFieldsTable = outFieldsTable;
        m_inListenerArmed = false;
        m_outListenerArmed = false;

        m_inFieldsTable.getTable().getModel().addTableModelListener(
                new TableModelListener() {
            @Override
            public void tableChanged(final TableModelEvent e) {
                if (e.getType() == TableModelEvent.INSERT) {
                    m_inListenerArmed = false;
                }
                if (m_inListenerArmed) {
                    // update snippet when table changes.
                    m_snippet.setJavaSnippetFields(new JavaSnippetFields(
                            m_inFieldsTable.getInColFields(),
                            m_inFieldsTable.getInVarFields(),
                            m_outFieldsTable.getOutColFields(),
                            m_outFieldsTable.getOutVarFields()));
                }
            }
        });
        m_outFieldsTable.getTable().getModel().addTableModelListener(
                new TableModelListener() {
            @Override
            public void tableChanged(final TableModelEvent e) {
                if (e.getType() == TableModelEvent.INSERT) {
                    m_outListenerArmed = false;
                }
                if (m_outListenerArmed) {
                    // update snippet when table changes.
                    m_snippet.setJavaSnippetFields(new JavaSnippetFields(
                            m_inFieldsTable.getInColFields(),
                            m_inFieldsTable.getInVarFields(),
                            m_outFieldsTable.getOutColFields(),
                            m_outFieldsTable.getOutVarFields()));
                }
            }
        });
        m_inFieldsTable.addPropertyChangeListener(
                InFieldsTable.PROP_FIELD_ADDED, new PropertyChangeListener() {

            @Override
            public void propertyChange(final PropertyChangeEvent evt) {
                m_inListenerArmed = true;
                // update snippet when table changes.
                m_snippet.setJavaSnippetFields(new JavaSnippetFields(
                        m_inFieldsTable.getInColFields(),
                        m_inFieldsTable.getInVarFields(),
                        m_outFieldsTable.getOutColFields(),
                        m_outFieldsTable.getOutVarFields()));
            }
        });
        m_outFieldsTable.addPropertyChangeListener(
                OutFieldsTable.PROP_FIELD_ADDED, new PropertyChangeListener() {

            @Override
            public void propertyChange(final PropertyChangeEvent evt) {
                m_outListenerArmed = true;
                // update snippet when table changes.
                m_snippet.setJavaSnippetFields(new JavaSnippetFields(
                        m_inFieldsTable.getInColFields(),
                        m_inFieldsTable.getInVarFields(),
                        m_outFieldsTable.getOutColFields(),
                        m_outFieldsTable.getOutVarFields()));
            }
        });
    }

    /**
     * Get a statement to inserted in the document of a java snippet to read
     * the given column.
     * @param colSpec the column to be read
     * @return the statement
     */
    @SuppressWarnings("rawtypes")
    public String getFieldReadStatement(final DataColumnSpec colSpec) {
        FieldsTableModel model =
            (FieldsTableModel) m_inFieldsTable.getTable().getModel();
        int index = -1;
        for (int r = 0; r < model.getRowCount(); r++) {
            Object value = model.getValueAt(r, Column.COLUMN);
            if (value instanceof DataColumnSpec) {
                DataColumnSpec foo = (DataColumnSpec)value;
                if (foo.getName().equals(colSpec.getName())) {
                    index = r;
                    break;
                }
            }
        }

        if (index >= 0) {
            // return java field name
            return (String)model.getValueAt(index, Column.JAVA_FIELD);
        } else {
            // try to add a row for the colSpec
            boolean success = m_inFieldsTable.addRow(colSpec);
            if (success) {
                // return java field name
                return (String)model.getValueAt(model.getRowCount() - 1,
                        Column.JAVA_FIELD);
            } else {
                // return generic code
                String name = colSpec.getName();
                DataType elemType = colSpec.getType().isCollectionType()
                    ? colSpec.getType().getCollectionElementType()
                    : colSpec.getType();
                DataValueToJava dvToJava =
                    TypeProvider.getDefault().getDataValueToJava(elemType,
                            colSpec.getType().isCollectionType());
                Class javaType = dvToJava.getPreferredJavaType();
                return "getCell(\"" + name + "\", "
                    + Type.getIdentifierFor(javaType) + ")";
            }
        }
    }

    /**
     * Get a statement to inserted in the document of a java snippet to read
     * the given flow variable.
     * @param v the flow variable to be read
     * @return the statement
     */
    @SuppressWarnings("rawtypes")
    public String getFieldReadStatement(final FlowVariable v) {
        FieldsTableModel model =
            (FieldsTableModel) m_inFieldsTable.getTable().getModel();
        int index = -1;
        for (int r = 0; r < model.getRowCount(); r++) {
            Object value = model.getValueAt(r, Column.COLUMN);
            if (value instanceof FlowVariable) {
                FlowVariable foo = (FlowVariable)value;
                if (foo.getName().equals(v.getName())) {
                    index = r;
                    break;
                }
            }
        }

        if (index >= 0) {
            // return java field name
            return (String)model.getValueAt(index,
                    Column.COLUMN);
        } else {
            // try to add a row for the flow variable
            boolean success = m_inFieldsTable.addRow(v);
            if (success) {
                // return java field name
                return (String)model.getValueAt(model.getRowCount() - 1,
                        Column.JAVA_FIELD);
            } else {
                // return generic code
                String name = v.getName();
                FlowVariable.Type type = v.getType();
                Class javaType = TypeProvider.getDefault().
                    getTypeConverter(type).getPreferredJavaType();
                return "getFlowVariable(\"" + name + "\", "
                    + Type.getIdentifierFor(javaType) + ")";
            }
        }
    }

    /**
     * Update the settings and input spec.
     * @param settings the settings
     * @param spec the input spec
     * @param flowVars the available flow variables
     */
    public void updateData(final JavaSnippetSettings settings,
            final DataTableSpec spec,
            final Map<String, FlowVariable> flowVars) {
        m_inListenerArmed = false;
        m_outListenerArmed = false;
        m_inFieldsTable.updateData(settings.getJavaSnippetFields(),
                spec, flowVars);
        m_outFieldsTable.updateData(settings.getJavaSnippetFields(),
                spec, flowVars);
        // update snippet.
        m_snippet.setJavaSnippetFields(new JavaSnippetFields(
                m_inFieldsTable.getInColFields(),
                m_inFieldsTable.getInVarFields(),
                m_outFieldsTable.getOutColFields(),
                m_outFieldsTable.getOutVarFields()));
        m_inListenerArmed = true;
        m_outListenerArmed = true;
    }

}
