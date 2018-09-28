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
 *   30.09.2011 (hofer): created
 */
package org.knime.base.node.preproc.stringmanipulation;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;

import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.border.Border;

import org.apache.commons.lang3.text.WordUtils;
import org.fife.ui.autocomplete.BasicCompletion;
import org.knime.base.node.preproc.stringmanipulation.manipulator.Manipulator;
import org.knime.base.node.util.JSnippetPanel;
import org.knime.base.node.util.JavaScriptingCompletionProvider;
import org.knime.base.node.util.ManipulatorProvider;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.ColumnSelectionPanel;
import org.knime.core.node.util.FlowVariableListCellRenderer;
import org.knime.core.node.util.rsyntaxtextarea.KnimeCompletionProvider;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.ext.sun.nodes.script.compile.CompilationFailedException;
import org.knime.ext.sun.nodes.script.expression.Expression;

/**
 * The node dialog of the string manipulation node and string manipulation (variable) node.
 *
 * @author Heiko Hofer
 * @author Thorsten Meinl, University of Konstanz
 * @author Simon Schmid
 */
public class StringManipulationNodeDialog extends NodeDialogPane {
    private boolean m_isOnlyVariables;

    private String m_columnOrVariable;

    private JSnippetPanel m_snippetPanel;

    private JRadioButton m_appendRadio;

    private JTextField m_newNameField;

    private JRadioButton m_replaceRadio;

    private ColumnSelectionPanel m_replaceColumnCombo;

    private JComboBox<FlowVariable> m_replaceVariableCombo;

    private JCheckBox m_compileOnCloseChecker;

    private JCheckBox m_insertMissingAsNullChecker;

    private DataTableSpec m_currentSpec = null;

    private KnimeCompletionProvider m_completionProvider;

    /**
     * Create new instance.
     *
     * @param isOnlyVariables defines if only variables should be choosable in the dialog
     * @since 3.3
     */
    public StringManipulationNodeDialog(final boolean isOnlyVariables) {
        m_isOnlyVariables = isOnlyVariables;
        if (isOnlyVariables) {
            m_columnOrVariable = "variable";
        } else {
            m_columnOrVariable = "column";
        }
        addTab("String Manipulation", createStringManipulationPanel());
    }

    /**
     * @return the controls for the string manipulation node
     * @since 3.3
     */
    public Component createStringManipulationPanel() {
        m_snippetPanel =
            new JSnippetPanel(StringManipulatorProvider.getDefault(), createCompletionProvider(), !m_isOnlyVariables);

        m_newNameField = new JTextField(10);
        String radioButtonName;
        String radioButtonToolTip;

        radioButtonName = "Append " + WordUtils.capitalize(m_columnOrVariable) + ": ";
        radioButtonToolTip = "Appends a new " + m_columnOrVariable + " to the input with a given name.";
        m_appendRadio = new JRadioButton(radioButtonName);
        m_appendRadio.setToolTipText(radioButtonToolTip);

        radioButtonName = "Replace " + WordUtils.capitalize(m_columnOrVariable) + ": ";
        if (m_isOnlyVariables) {
            radioButtonToolTip = "Replaces the " + m_columnOrVariable + " if the type stays the same.";
        } else {
            radioButtonToolTip = "Replaces the " + m_columnOrVariable + " and changes the " + m_columnOrVariable
                + " type accordingly.";
        }
        m_replaceRadio = new JRadioButton(radioButtonName);
        m_replaceRadio.setToolTipText(radioButtonToolTip);

        if (m_isOnlyVariables) {
            // show all variables
            m_replaceVariableCombo = new JComboBox<FlowVariable>(new DefaultComboBoxModel<FlowVariable>());
            m_replaceVariableCombo.setRenderer(new FlowVariableListCellRenderer());
        } else {
            // show all columns
            m_replaceColumnCombo = new ColumnSelectionPanel((Border)null, DataValue.class);
            m_replaceColumnCombo.setRequired(false);
        }

        ButtonGroup buttonGroup = new ButtonGroup();
        buttonGroup.add(m_appendRadio);
        buttonGroup.add(m_replaceRadio);
        ActionListener actionListener = new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                if (m_isOnlyVariables) {
                    m_replaceVariableCombo.setEnabled(m_replaceRadio.isSelected());
                } else {
                    m_replaceColumnCombo.setEnabled(m_replaceRadio.isSelected());
                }
                m_newNameField.setEnabled(m_appendRadio.isSelected());
            }
        };
        m_appendRadio.addActionListener(actionListener);
        m_replaceRadio.addActionListener(actionListener);

        m_compileOnCloseChecker = new JCheckBox("Syntax check on close");
        m_compileOnCloseChecker.setToolTipText("Checks the syntax of the expression on close.");

        m_insertMissingAsNullChecker = new JCheckBox("Insert Missing As Null");
        m_insertMissingAsNullChecker
            .setToolTipText("If unselected, missing values in the input will produce a missing cell result");
        return createPanel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean closeOnESC() {
        // do not close the dialog when ESC is pressed.
        // Allows the table to use ESC to cancel the edit mode of a cell.
        return false;
    }

    /**
     * Create a simple provider that adds some Java-related completions.
     *
     * @return The completion provider.
     * @since 3.7
     */
    protected KnimeCompletionProvider createCompletionProvider() {
        m_completionProvider = new JavaScriptingCompletionProvider();

        Collection<Manipulator> manipulators =
            StringManipulatorProvider.getDefault().getManipulators(ManipulatorProvider.ALL_CATEGORY);
        for (Manipulator m : manipulators) {
            // A BasicCompletion is just a straightforward word completion.
            m_completionProvider.addCompletion(
                new BasicCompletion(m_completionProvider, m.getName(), m.getDisplayName(), m.getDescription()));
        }

        return m_completionProvider;
    }

    /**
     * @since 3.3
     */
    protected JPanel createPanel() {
        JPanel southPanel = new JPanel(new GridLayout(0, 2));
        JPanel replaceOrAppend = createAndOrReplacePanel();
        southPanel.add(replaceOrAppend);

        JPanel returnTypeAndCompilation = createReturnTypeAndCompilationPanel();
        southPanel.add(returnTypeAndCompilation);

        JPanel p = new JPanel(new BorderLayout());
        p.add(m_snippetPanel, BorderLayout.CENTER);
        p.add(southPanel, BorderLayout.SOUTH);
        p.setPreferredSize(new Dimension(820, 420));
        return p;
    }

    private JPanel createAndOrReplacePanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();

        m_appendRadio.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                m_newNameField.requestFocus();
                m_newNameField.selectAll();
            }
        });

        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(6, 6, 2, 6);
        c.gridx = 0;
        c.gridy = 0;
        panel.add(m_appendRadio, c);

        c.gridx += 1;
        c.insets = new Insets(6, 0, 2, 6);
        panel.add(m_newNameField, c);

        c.gridy += 1;
        c.gridx = 0;
        c.insets = new Insets(2, 6, 4, 6);
        panel.add(m_replaceRadio, c);

        c.gridx += 1;
        c.insets = new Insets(2, 0, 4, 6);
        if (m_isOnlyVariables) {
            panel.add(m_replaceVariableCombo, c);
        } else {
            panel.add(m_replaceColumnCombo, c);
        }

        return panel;
    }

    private JPanel createReturnTypeAndCompilationPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();

        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(6, 6, 4, 6);
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 1;
        c.weightx = 0;
        c.weighty = 0;

        if (!m_isOnlyVariables) {
            p.add(m_insertMissingAsNullChecker, c);
        }
        c.gridy++;
        c.insets = new Insets(2, 6, 4, 6);
        p.add(m_compileOnCloseChecker, c);

        return p;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {
        DataTableSpec spec;
        if (m_isOnlyVariables) {
            spec = new DataTableSpec();
        } else {
            spec = (DataTableSpec)specs[0];
        }
        StringManipulationSettings s = new StringManipulationSettings();
        s.loadSettingsInDialog(settings, spec);

        String exp = s.getExpression();

        String defaultNewName = "new " + m_columnOrVariable;
        String newName = s.getColName();
        boolean isReplace = s.isReplace();
        boolean isTestCompilation = s.isTestCompilationOnDialogClose();
        boolean isInsertMissingAsNull = s.isInsertMissingAsNull();

        m_newNameField.setText("");

        final Map<String, FlowVariable> availableFlowVariables = getAvailableFlowVariables();
        if (m_isOnlyVariables) {
            DefaultComboBoxModel<FlowVariable> cmbModel =
                (DefaultComboBoxModel<FlowVariable>)m_replaceVariableCombo.getModel();
            cmbModel.removeAllElements();
            for (FlowVariable v : availableFlowVariables.values()) {
                switch (v.getScope()) {
                    case Flow:
                        cmbModel.addElement(v);
                        break;
                    default:
                        // ignore
                }
            }
            if (availableFlowVariables.containsValue(newName)){
                m_replaceVariableCombo.setSelectedItem(availableFlowVariables.get(newName));
            }
        } else {
            // will select newColName only if it is in the spec list
            try {
                m_replaceColumnCombo.update(spec, newName);
            } catch (NotConfigurableException e1) {
                NodeLogger.getLogger(getClass())
                    .coding("Combo box throws exception although content is not required", e1);
            }
        }

        m_currentSpec = spec;
        // whether there are variables or columns available
        // -- which of two depends on the customizer
        boolean fieldsAvailable;
        if (m_isOnlyVariables) {
            fieldsAvailable = m_replaceVariableCombo.getItemCount() > 0;
        } else {
            fieldsAvailable = m_replaceColumnCombo.getNrItemsInList() > 0;
        }

        m_replaceRadio.setEnabled(fieldsAvailable);
        if (isReplace && fieldsAvailable) {
            m_replaceRadio.doClick();
        } else {
            m_appendRadio.doClick();
            String newNameString = (newName != null ? newName : defaultNewName);
            m_newNameField.setText(newNameString);
        }
        m_snippetPanel.update(exp, spec, availableFlowVariables);

        m_compileOnCloseChecker.setSelected(isTestCompilation);
        m_insertMissingAsNullChecker.setSelected(isInsertMissingAsNull);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        StringManipulationSettings s = new StringManipulationSettings();

        String newColName = null;
        boolean isReplace = m_replaceRadio.isSelected();
        if (isReplace) {
            if (m_isOnlyVariables) {
                newColName = ((FlowVariable)m_replaceVariableCombo.getModel().getSelectedItem()).getName();
            } else {
                newColName = m_replaceColumnCombo.getSelectedColumn();
            }
        } else {
            newColName = m_newNameField.getText();
        }
        s.setReplace(isReplace);
        s.setColName(newColName);
        String exp = m_snippetPanel.getExpression();
        s.setExpression(exp);
        boolean isTestCompilation = m_compileOnCloseChecker.isSelected();
        s.setTestCompilationOnDialogClose(isTestCompilation);
        if (isTestCompilation && m_currentSpec != null) {
            try (Expression tempExp = Expression.compile(s.getJavaScriptingSettings(), m_currentSpec);) {
            } catch (CompilationFailedException cfe) {
                throw new InvalidSettingsException(cfe.getMessage(), cfe);
            } catch (IOException e) {
                NodeLogger.getLogger(getClass()).warn("Unable to clean up Expression object: " + e.getMessage(), e);
            }
        }
        s.setInsertMissingAsNull(m_insertMissingAsNullChecker.isSelected());
        s.saveSettingsTo(settings);
        s.discard();
    }
}
