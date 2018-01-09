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
 * ------------------------------------------------------------------------
 *
 * History
 *   24.11.2011 (hofer): created
 */
package org.knime.base.node.jsnippet;

import java.awt.FlowLayout;
import java.util.Map;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.knime.base.node.jsnippet.template.JavaSnippetTemplate;
import org.knime.base.node.jsnippet.ui.ColumnList;
import org.knime.base.node.jsnippet.ui.FieldsTableModel;
import org.knime.base.node.jsnippet.ui.FieldsTableModel.Column;
import org.knime.base.node.jsnippet.ui.FlowVariableList;
import org.knime.base.node.jsnippet.ui.InFieldsTable;
import org.knime.base.node.jsnippet.ui.OutFieldsTable;
import org.knime.base.node.jsnippet.util.JavaSnippetSettings;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.workflow.FlowVariable;

/**
 * The dialog of the java edit variable node.
 *
 * @author Heiko Hofer
 */
public class JavaEditVarNodeDialog extends JavaSnippetNodeDialog {

    private JCheckBox m_runOnExecuteChecker;

    /**
     * Create a new Dialog.
     * @param templateMetaCategory the meta category used in the templates
     * tab or to create templates
     */
    @SuppressWarnings("rawtypes")
    public JavaEditVarNodeDialog(final Class templateMetaCategory) {
        super(templateMetaCategory);
    }

    /**
     * Create a new Dialog.
     * @param templateMetaCategory the meta category used in the templates
     * tab or to create templates
     * @param isPreview if this is a preview used for showing templates.
     */
    @SuppressWarnings("rawtypes")
    protected JavaEditVarNodeDialog(final Class templateMetaCategory,
                                 final boolean isPreview) {
        super(templateMetaCategory, isPreview);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setEnabled(final boolean enabled) {
        if (isEnabled() != enabled) {
            m_runOnExecuteChecker.setEnabled(enabled);
        }
        super.setEnabled(enabled);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected InFieldsTable createInFieldsTable() {
        InFieldsTable table = new InFieldsTable();
        FieldsTableModel model = (FieldsTableModel)table.getTable().getModel();
        model.setColumnName(model.getIndex(Column.COLUMN), "Flow Variable");
        return table;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected OutFieldsTable createOutFieldsTable() {
        OutFieldsTable table = new OutFieldsTable(true);
        FieldsTableModel model = (FieldsTableModel)table.getTable().getModel();
        model.setColumnName(model.getIndex(Column.COLUMN), "Flow Variable");
        table.getTable().getColumnModel().getColumn(model.getIndex(
                Column.REPLACE_EXISTING)).setPreferredWidth(15);
        return table;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected JComponent createColsAndVarsPanel() {
        m_colList = new ColumnList();
        // set variable panel
        m_flowVarsList = new FlowVariableList();
        JScrollPane flowVarScroller = new JScrollPane(m_flowVarsList);
        flowVarScroller.setBorder(
                createEmptyTitledBorder("Flow Variable List"));

        return flowVarScroller;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected JPanel createOptionsPanel() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEADING));
        m_runOnExecuteChecker = new JCheckBox("Run only on execution");
        m_runOnExecuteChecker.setToolTipText("If selected, the snippet is run only when the node is executed.");
        p.add(m_runOnExecuteChecker);
        return p;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected JavaSnippetNodeDialog createPreview() {
        return new JavaEditVarNodeDialog(m_templateMetaCategory, true);
    }

    /** {@inheritDoc} */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final PortObjectSpec[] specs) throws NotConfigurableException {
        loadSettingsFrom(settings, new DataTableSpec[]{new DataTableSpec()});
        m_runOnExecuteChecker.setSelected(m_settings.isRunOnExecute());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void applyTemplate(final JavaSnippetTemplate template,
                              final DataTableSpec spec,
                              final Map<String, FlowVariable> flowVariables) {
        super.applyTemplate(template, spec, flowVariables);
        m_runOnExecuteChecker.setSelected(m_settings.isRunOnExecute());

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void preSaveSettings(final JavaSnippetSettings s) {
        s.setRunOnExecute(m_runOnExecuteChecker.isSelected());
    }
}
