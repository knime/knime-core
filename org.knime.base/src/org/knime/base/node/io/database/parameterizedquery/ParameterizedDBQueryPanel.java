/*
 * ------------------------------------------------------------------------
 *
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
 *   May 3, 2016 (budiyanto): created
 */
package org.knime.base.node.io.database.parameterizedquery;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collection;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.knime.base.util.flowvariable.FlowVariableResolver;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.database.DatabasePortObjectSpec;
import org.knime.core.node.port.database.DatabaseUtility;
import org.knime.core.node.port.database.StatementManipulator;
import org.knime.core.node.util.DataColumnSpecListCellRenderer;
import org.knime.core.node.util.FlowVariableListCellRenderer;
import org.knime.core.node.workflow.FlowVariable;

/**
 *
 * @author Budi Yanto, KNIME.com
 */
public class ParameterizedDBQueryPanel extends JPanel {

    /**
     * Automatically genearted Serial Version UID
     */
    private static final long serialVersionUID = 6975068112489198240L;

//    private static final NodeLogger LOGGER = NodeLogger.getLogger(DBLooperPanel.class);

    private final DefaultListModel<DataColumnSpec> m_knimeColumnsModel = new DefaultListModel<>();

    private final JList<DataColumnSpec> m_knimeColumnsList = new JList<>(m_knimeColumnsModel);

    private final DefaultListModel<DataColumnSpec> m_dbColumnsModel = new DefaultListModel<>();

    private final JList<DataColumnSpec> m_dbColumnsList = new JList<>(m_dbColumnsModel);

    private final DefaultListModel<FlowVariable> m_flowVariablesModel = new DefaultListModel<>();

    private final JList<FlowVariable> m_flowVariablesList = new JList<>(m_flowVariablesModel);

    private final RSyntaxTextArea m_editor = createEditor();

    private final SettingsModelBoolean m_appendInputColsModel = ParameterizedDBQueryNodeModel.createAppendInputColsModel();
    private final SettingsModelBoolean m_includeEmptyResultsModel = ParameterizedDBQueryNodeModel.createIncludeEmptyResultsModel();
    private final SettingsModelBoolean m_retainAllColumnsModel = ParameterizedDBQueryNodeModel.createRetainAllColumnsModel();
    private final SettingsModelBoolean m_failIfExceptionModel = ParameterizedDBQueryNodeModel.createFailIfExceptionModel();

    private StatementManipulator m_statementManipulator;

    /**
     *
     */
    ParameterizedDBQueryPanel() {
        setLayout(new BorderLayout());
        final JSplitPane mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        mainSplitPane.setLeftComponent(createColumnsAndVariablesPanel());
        mainSplitPane.setRightComponent(createEditorPanel());
        add(new JScrollPane(mainSplitPane), BorderLayout.CENTER);
        add(createOptionsPanel(), BorderLayout.SOUTH);
    }

    private JComponent createColumnsAndVariablesPanel() {

        /* Create database column list */
        final JPanel dbColumnsPanel = new JPanel(new BorderLayout());
        dbColumnsPanel.setPreferredSize(new Dimension(200, 150));
        final JLabel dbColumnsLabel = new JLabel("Database Column List");
        dbColumnsLabel.setBorder(new EmptyBorder(5, 5, 5, 5));
        dbColumnsPanel.add(dbColumnsLabel, BorderLayout.NORTH);
        dbColumnsPanel.add(new JScrollPane(m_dbColumnsList), BorderLayout.CENTER);
        m_dbColumnsList.setCellRenderer(new DataColumnSpecListCellRenderer());
        m_dbColumnsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        m_dbColumnsList.addKeyListener(new KeyAdapter() {
            /** {@inheritDoc} */
            @Override
            public void keyTyped(final KeyEvent evt) {
                if(evt.getKeyChar() == KeyEvent.VK_ENTER) {
                    onSelectionInDatabaseList();
                }
            }

        });
        m_dbColumnsList.addMouseListener(new MouseAdapter() {
            /** {@inheritDoc} */
            @Override
            public void mouseClicked(final MouseEvent evt) {
                if(evt.getClickCount() == 2){
                    onSelectionInDatabaseList();
                }
            }
        });

        /* Create Knime column list */
        final JPanel knimeColumnsPanel = new JPanel(new BorderLayout());
        knimeColumnsPanel.setPreferredSize(new Dimension(200, 150));
        final JLabel knimeColumnsLabel = new JLabel("Column List");
        knimeColumnsLabel.setBorder(new EmptyBorder(5, 5, 5, 5));
        knimeColumnsPanel.add(knimeColumnsLabel, BorderLayout.NORTH);
        knimeColumnsPanel.add(new JScrollPane(m_knimeColumnsList), BorderLayout.CENTER);
        m_knimeColumnsList.setCellRenderer(new DataColumnSpecListCellRenderer());
        m_knimeColumnsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        m_knimeColumnsList.addKeyListener(new KeyAdapter() {
            /** {@inheritDoc} */
            @Override
            public void keyTyped(final KeyEvent evt) {
                if(evt.getKeyChar() == KeyEvent.VK_ENTER) {
                    onSelectionInColumnList();
                }
            }
        });

        m_knimeColumnsList.addMouseListener(new MouseAdapter() {
            /** {@inheritDoc} */
            @Override
            public void mouseClicked(final MouseEvent evt) {
                if(evt.getClickCount() == 2){
                    onSelectionInColumnList();
                }
            }
        });

        final JSplitPane columnsSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        columnsSplitPane.setResizeWeight(0.5);
        columnsSplitPane.setOneTouchExpandable(true);
        columnsSplitPane.setDividerSize(8);
        columnsSplitPane.setTopComponent(dbColumnsPanel);
        columnsSplitPane.setBottomComponent(knimeColumnsPanel);
        columnsSplitPane.setDividerLocation(150);

        /* Create flow variables list*/
        final JPanel variablesPanel = new JPanel(new BorderLayout());
        variablesPanel.setPreferredSize(new Dimension(200, 150));
        final JLabel variablesLabel = new JLabel("Flow Variable List");
        variablesLabel.setBorder(new EmptyBorder(5, 5, 5, 5));
        variablesPanel.add(variablesLabel, BorderLayout.NORTH);
        variablesPanel.add(new JScrollPane(m_flowVariablesList), BorderLayout.CENTER);
        m_flowVariablesList.setCellRenderer(new FlowVariableListCellRenderer());
        m_flowVariablesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        m_flowVariablesList.addKeyListener(new KeyAdapter() {
            /** {@inheritDoc} */
            @Override
            public void keyTyped(final KeyEvent evt) {
                if(evt.getKeyChar() == KeyEvent.VK_ENTER) {
                    onSelectionInFlowVariableList();
                }
            }
        });

        m_flowVariablesList.addMouseListener(new MouseAdapter() {
            /**
             * {@inheritDoc}
             */
            @Override
            public void mouseClicked(final MouseEvent evt) {
                if(evt.getClickCount() == 2){
                    onSelectionInFlowVariableList();
                }
            }
        });

        final JSplitPane columnsVariableSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        columnsVariableSplitPane.setResizeWeight(0.5);
        columnsVariableSplitPane.setOneTouchExpandable(true);
        columnsVariableSplitPane.setDividerSize(8);
        columnsVariableSplitPane.setTopComponent(columnsSplitPane);
        columnsVariableSplitPane.setBottomComponent(variablesPanel);
        columnsVariableSplitPane.setDividerLocation(300);

        return columnsVariableSplitPane;
    }

    private void onSelectionInFlowVariableList() {
        final FlowVariable selected = m_flowVariablesList.getSelectedValue();
        if(selected != null) {
            m_editor.replaceSelection(FlowVariableResolver
                .getPlaceHolderForVariable(selected));
            m_flowVariablesList.clearSelection();
            m_editor.requestFocus();
        }
    }

    private void onSelectionInColumnList() {
        final DataColumnSpec selected = m_knimeColumnsList.getSelectedValue();
        if(selected != null) {
            String name = selected.getName().replace("$", "\\$");
            name = "$" + name + "$";
            m_editor.replaceSelection(name);
            m_knimeColumnsList.clearSelection();
            m_editor.requestFocus();
        }
    }

    private void onSelectionInDatabaseList() {
        final DataColumnSpec selected = m_dbColumnsList.getSelectedValue();
        if(selected != null) {
            String var = selected.getName();
            if(m_statementManipulator != null) {
                var = m_statementManipulator.quoteIdentifier(var);
            }
            m_editor.replaceSelection(var);
            m_dbColumnsList.clearSelection();
            m_editor.requestFocus();
        }
    }

    private JPanel createOptionsPanel() {
        final JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLoweredBevelBorder(),
            BorderFactory.createTitledBorder("Options")));

        final Box optionsBox = Box.createHorizontalBox();
        optionsBox.add(new DialogComponentBoolean(m_appendInputColsModel,
                "Append input columns").getComponentPanel());
        optionsBox.add(new DialogComponentBoolean(m_includeEmptyResultsModel,
                "Include empty results").getComponentPanel());
        optionsBox.add(new DialogComponentBoolean(m_retainAllColumnsModel,
                "Retain all columns").getComponentPanel());
        optionsBox.add(new DialogComponentBoolean(m_failIfExceptionModel,
                "Fail on error").getComponentPanel());

        panel.add(optionsBox, BorderLayout.CENTER);
        m_appendInputColsModel.addChangeListener(
            l -> {
                m_includeEmptyResultsModel.setEnabled(m_appendInputColsModel.getBooleanValue());
                m_retainAllColumnsModel.setEnabled(m_appendInputColsModel.getBooleanValue());
            });

        return panel;
    }

    private JPanel createEditorPanel() {
        final JPanel panel = new JPanel(new BorderLayout());
        final JLabel editorLabel = new JLabel("SQL Statement");
        editorLabel.setBorder(new EmptyBorder(5, 5, 5, 5));

        final RTextScrollPane editorScrollPane = new RTextScrollPane(m_editor);
        editorScrollPane.setPreferredSize(new Dimension(850, 400));
        editorScrollPane.setFoldIndicatorEnabled(true);
        panel.add(editorLabel, BorderLayout.NORTH);
        panel.add(editorScrollPane, BorderLayout.CENTER);
        return panel;
    }

    void loadSettingsFrom(final NodeSettingsRO settings,
            final PortObjectSpec[] specs, final Collection<FlowVariable> flowVariables) {
        try{
            m_appendInputColsModel.loadSettingsFrom(settings);
            m_includeEmptyResultsModel.loadSettingsFrom(settings);
            m_retainAllColumnsModel.loadSettingsFrom(settings);
            m_failIfExceptionModel.loadSettingsFrom(settings);
            m_editor.setText(settings.getString(
                ParameterizedDBQueryNodeModel.CFG_SQL_STATEMENT,
                ParameterizedDBQueryNodeModel.getDefaultSQLStatement()));
        } catch(InvalidSettingsException ex){
            m_appendInputColsModel.setBooleanValue(ParameterizedDBQueryNodeModel.DEF_APPEND_INPUT_COL);
            m_includeEmptyResultsModel.setBooleanValue(ParameterizedDBQueryNodeModel.DEF_INCLUDE_EMPTY_RESULTS);
            m_retainAllColumnsModel.setBooleanValue(ParameterizedDBQueryNodeModel.DEF_RETAIN_ALL_COLUMNS);
            m_failIfExceptionModel.setBooleanValue(ParameterizedDBQueryNodeModel.DEF_FAIL_IF_EXCEPTION);
        }

        updateKnimeColumns((DataTableSpec) specs[0]);
        updateDBColumns(((DatabasePortObjectSpec) specs[1]).getDataTableSpec());
        updateFlowVariables(flowVariables.toArray(
            new FlowVariable[flowVariables.size()]));

        if(specs[1] != null && specs[1] instanceof DatabasePortObjectSpec) {
            final DatabasePortObjectSpec dbSpec = (DatabasePortObjectSpec) specs[1];
            final DatabaseUtility dbUtility = DatabaseUtility.getUtility(dbSpec.getDatabaseIdentifier());
            m_statementManipulator = dbUtility.getStatementManipulator();
        }

    }

    void saveSettingsTo(final NodeSettingsWO settings){
        m_appendInputColsModel.saveSettingsTo(settings);
        m_includeEmptyResultsModel.saveSettingsTo(settings);
        m_retainAllColumnsModel.saveSettingsTo(settings);
        m_failIfExceptionModel.saveSettingsTo(settings);
        settings.addString(ParameterizedDBQueryNodeModel.CFG_SQL_STATEMENT, m_editor.getText());
    }

    void updateFlowVariables(final FlowVariable[] flowVariables) {
        m_flowVariablesModel.clear();
        for (final FlowVariable var : flowVariables) {
            m_flowVariablesModel.addElement(var);
        }
    }

    void updateKnimeColumns(final DataTableSpec spec) {
        m_knimeColumnsModel.clear();
        for (DataColumnSpec colSpec : spec) {
            m_knimeColumnsModel.addElement(colSpec);
        }
    }

    void updateDBColumns(final DataTableSpec dbSpec) {
        m_dbColumnsModel.clear();
        for (DataColumnSpec colSpec : dbSpec) {
            m_dbColumnsModel.addElement(colSpec);
        }
    }

    private static RSyntaxTextArea createEditor(){
        final RSyntaxTextArea editor = new RSyntaxTextArea();
        editor.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_SQL);
        editor.setCodeFoldingEnabled(true);
        editor.setAntiAliasingEnabled(true);
        editor.setAutoIndentEnabled(true);
        editor.setFadeCurrentLineHighlight(true);
        editor.setHighlightCurrentLine(true);
        editor.setLineWrap(false);
        editor.setRoundedSelectionEdges(true);
        editor.setBorder(new EtchedBorder());
        editor.setTabSize(4);
        return editor;
    }

}
