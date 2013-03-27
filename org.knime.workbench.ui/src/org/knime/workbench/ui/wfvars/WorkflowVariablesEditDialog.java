/* This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2013
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 */
package org.knime.workbench.ui.wfvars;

import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.node.workflow.FlowVariable.Scope;

/**
 * Let the user add or edit a workflow variable with name, type and default
 * value. The ScopeVariable is created here, but actually added to the workflow
 * in the {@link WorkflowVariablesDialog}.
 *
 * @author Fabian Dill, KNIME.com AG
 */
public class WorkflowVariablesEditDialog extends Dialog {

    private Text m_varNameCtrl;
    private Combo m_typeSelectionCtrl;
    private Text m_varDefaultValueCtrl;

    private FlowVariable m_variable;
    private FlowVariable.Type m_type;

    /**
     *
     */
    public WorkflowVariablesEditDialog() {
        super(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell());
    }


    /**
     * It is resizable.
     *
     * {@inheritDoc}
     */
    @Override
    protected boolean isResizable() {
        return true;
    }

    /**
     *
     * @param var {@link FlowVariable} to fill the fields of the dialog from
     */
    public void loadFrom(final FlowVariable var) {
        m_varNameCtrl.setText(var.getName());
        m_varNameCtrl.setEditable(false);
        m_varNameCtrl.setEnabled(false);
        String typeString = var.getType().name();
        if (typeString == null) {
            throw new IllegalArgumentException("Type of variable "
                    + var.getName() + " must not be null!");
        }
        for (int i = 0; i < m_typeSelectionCtrl.getItemCount(); i++) {
            if (typeString.equals(m_typeSelectionCtrl.getItem(i))) {
                m_typeSelectionCtrl.select(i);
                break;
            }
        }
        String value = WorkflowVariableTable.getValueFrom(var);
        m_varDefaultValueCtrl.setText(value);
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    protected Control createDialogArea(final Composite parent) {
        parent.getShell().setText("Add/Edit Workflow Variable");
        Composite twoColComp = new Composite(parent, SWT.NONE);
        twoColComp.setLayout(new GridLayout(3, true));

        GridData horizontalFill = 
            new GridData(SWT.FILL, SWT.CENTER, true, false);
        horizontalFill.horizontalSpan = 2;

        // first row: node settings name
        Label varNameLabel = new Label(twoColComp, SWT.NONE);
        varNameLabel.setText("Variable name: ");
        m_varNameCtrl = new Text(twoColComp, SWT.BORDER);
        m_varNameCtrl.setLayoutData(horizontalFill);
        // add validation (at least some basic "text length > 0)
        m_varNameCtrl.addModifyListener(new ModifyListener() {

            @Override
            public void modifyText(final ModifyEvent arg0) {
                String text = m_varNameCtrl.getText();
                for (Scope s : FlowVariable.Scope.values()) {
                    if (!Scope.Flow.equals(s)) {
                        if (text.startsWith(s.getPrefix())) {
                            showError("Flow variables must not start with \""
                                    + s.getPrefix() + "\"!");
                            m_varNameCtrl.setText("");
                        }
                    }
                }
            }

        });
        // second row: parameter type
        Label typeLabel = new Label(twoColComp, SWT.NONE);
        typeLabel.setText("Variable Type:");

        m_typeSelectionCtrl = new Combo(twoColComp,
                SWT.DROP_DOWN | SWT.READ_ONLY);
        for (FlowVariable.Type t : FlowVariable.Type.values()) {
            m_typeSelectionCtrl.add(t.name());
        }
        m_typeSelectionCtrl.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetDefaultSelected(final SelectionEvent arg0) {
                widgetSelected(arg0);
            }
            @Override
            public void widgetSelected(final SelectionEvent arg0) {
                m_type = FlowVariable.Type.valueOf(m_typeSelectionCtrl.getItem(
                        m_typeSelectionCtrl.getSelectionIndex()));
            }
        });
        m_typeSelectionCtrl.setLayoutData(horizontalFill);
        m_typeSelectionCtrl.select(0);
        // third row: data set parameter name
        Label defaultValueLabel = new Label(twoColComp, SWT.NONE);
        defaultValueLabel.setText("Default value: ");

        m_varDefaultValueCtrl = new Text(twoColComp, SWT.BORDER);
        m_varDefaultValueCtrl.setLayoutData(horizontalFill);
        return twoColComp;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    protected void okPressed() {
        String varName = m_varNameCtrl.getText().trim();
        if (varName.isEmpty()) {
            String msg = "Variable name must not be empty!";
            showError(msg);
            throw new OperationCanceledException(msg);
        }
        String value = m_varDefaultValueCtrl.getText().trim();
        if (value.isEmpty()) {
            String msg = "A default value must be entered!";
            showError(msg);
            throw new OperationCanceledException(msg);
        }
        int selectionIdx = m_typeSelectionCtrl.getSelectionIndex();
        if (selectionIdx < 0) {
            String msg = "No type selected for variable "
                + varName;
            showError(msg);
            throw new IllegalArgumentException(msg);
        }
        String typeString = m_typeSelectionCtrl.getItem(selectionIdx);
        m_type = FlowVariable.Type.valueOf(typeString);
        if (FlowVariable.Type.DOUBLE.equals(m_type)) {
            if (value != null && value.length() > 0) {
                try {
                   m_variable = new FlowVariable(varName,
                           Double.parseDouble(value));
                } catch (NumberFormatException nfe) {
                    m_variable = null;
                    String msg = "Invalid default value " + value
                        + " for variable " + varName + "!";
                    showError(msg);
                    throw new OperationCanceledException(msg);
                }
            }
        } else if (FlowVariable.Type.STRING.equals(m_type)) {
            if (value != null && value.length() > 0) {
                   m_variable = new FlowVariable(varName, value);
            } else {
                m_variable = null;
                String msg = "Invalid default value " + value
                    + " for variable " + varName + "!";
                showError(msg);
                throw new OperationCanceledException(msg);
            }
        } else if (FlowVariable.Type.INTEGER.equals(m_type)) {
            if (value != null && value.length() > 0) {
                try {
                   m_variable = new FlowVariable(varName, Integer.parseInt(
                           value));
                } catch (NumberFormatException nfe) {
                    m_variable = null;
                    String msg = "Invalid default value " + value
                        + " for variable " + varName + "!";
                    showError(msg);
                    throw new OperationCanceledException(msg);
                }
            }
        }
        super.okPressed();
    }

    private void showError(final String message) {
        MessageDialog.openError(getParentShell(), "Error", message);
    }

    /**
     *
     * @return the created {@link FlowVariable} or <code>null</code>
     */
    FlowVariable getScopeVariable() {
        return m_variable;
    }
}
