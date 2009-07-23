/* This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.ScopeVariable;
import org.knime.core.node.workflow.WorkflowManager;

/**
 * Dialog that let the user add, edit or remove workflow variables. Existing
 * variables are listed in a {@link WorkflowVariableTable} with name, type and
 * default (current) value. Workflow variables can be added or edited with the
 * {@link WorkflowVariablesEditDialog}.
 *
 * @author Fabian Dill, KNIME.com GmbH
 */
public class WorkflowVariablesDialog extends Dialog {

    private static final int SKIP_RESET_IDX = 0;
    private static final int RESET_IDX = 1;
    
    private WorkflowVariableTable m_table;

    private final WorkflowManager m_workflow;

    /**
     *
     * @param shell parent shell
     * @param workflow selected workflow to create the workflow variables for
     */
    public WorkflowVariablesDialog(final Shell shell,
            final WorkflowManager workflow) {
        super(shell);
        m_workflow = workflow;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    protected boolean isResizable() {
        return true;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    protected Control createDialogArea(final Composite parent) {
        parent.getShell().setText("Workflow Variable Administration");
        Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayout(new GridLayout(1, false));
        composite.setLayoutData(new GridData(GridData.FILL_BOTH));
        // composite contains:

        // second row (new composite):
        Composite tableAndBtnsComp = new Composite(composite, SWT.NONE);
        tableAndBtnsComp.setLayoutData(new GridData(GridData.FILL_BOTH));
        tableAndBtnsComp.setLayout(new GridLayout(2, false));
        // first column: table
        Composite tableComp = new Composite(tableAndBtnsComp, SWT.NONE);
        GridData gridData = new GridData(GridData.FILL_BOTH);
        gridData.grabExcessHorizontalSpace = true;
        tableComp.setLayout(new FillLayout());
        tableComp.setLayoutData(gridData);
        m_table = new WorkflowVariableTable(tableComp);
        for (ScopeVariable var : m_workflow.getWorkflowVariables()) {
            m_table.add(var);
        }
        m_table.getViewer().refresh();
        m_table.getViewer().addDoubleClickListener(new IDoubleClickListener() {
            /**
             *
             * {@inheritDoc}
             */
            public void doubleClick(final DoubleClickEvent event) {
                Table table = m_table.getViewer().getTable();
                int index = table.getSelectionIndex();
                // we only get a double-click event for existing items
                ScopeVariable var = m_table.get(index);
                editWorkflowVariable(var, index);
            }
        });

        // second column: 3 buttons
        Composite btnsComp = new Composite(tableAndBtnsComp, SWT.NONE);
        btnsComp.setLayout(new GridLayout(1, false));
        gridData = new GridData();
        gridData.verticalAlignment = GridData.VERTICAL_ALIGN_CENTER;
        btnsComp.setLayoutData(gridData);

        Button addBtn = new Button(btnsComp, SWT.PUSH);
        addBtn.setText("Add");
        addBtn.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetDefaultSelected(final SelectionEvent arg0) {
                widgetSelected(arg0);
            }

            @Override
            public void widgetSelected(final SelectionEvent arg0) {
                addWorkflowVariable();
            }

        });
        gridData = new GridData();
        gridData.widthHint = 80;
        gridData.heightHint = 20;
        addBtn.setLayoutData(gridData);

        Button editBtn = new Button(btnsComp, SWT.PUSH);
        editBtn.setText("Edit");
        editBtn.setLayoutData(gridData);
        editBtn.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetDefaultSelected(final SelectionEvent arg0) {
                widgetSelected(arg0);
            }

            @Override
            public void widgetSelected(final SelectionEvent arg0) {
                int selectionIdx = m_table.getViewer().getTable()
                    .getSelectionIndex();
                if (selectionIdx < 0) {
                    MessageDialog.openWarning(getShell(), "Empty selection",
                    "Please select the parameter you want to edit.");
                    return;
                }
                ScopeVariable selectedVar = m_table.get(selectionIdx);
                editWorkflowVariable(selectedVar, selectionIdx);
            }
        });

        Button removeBtn = new Button(btnsComp, SWT.PUSH);
        removeBtn.setText("Remove");
        removeBtn.setLayoutData(gridData);
        removeBtn.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetDefaultSelected(final SelectionEvent arg0) {
                widgetSelected(arg0);
            }

            @Override
            public void widgetSelected(final SelectionEvent arg0) {
                int idx = m_table.getViewer().getTable().getSelectionIndex();
                if (idx < 0) {
                    MessageDialog.openWarning(getShell(), "Empty selection", 
                            "Please select the parameter you want to remove.");
                    return;
                }
                ScopeVariable selectedParam =
                    (ScopeVariable)((IStructuredSelection)m_table
                        .getViewer().getSelection()).getFirstElement();
                removeWorkflowVariable(selectedParam);
            }
        });
        return composite;
    }

    private int openConfirmationDialog() {
        // if there are nodes to be reset -> ask for it
        if (containsExecutedNodes(m_workflow)
                && m_workflow.getParent().canResetNode(m_workflow.getID())) {
            MessageDialog dialog = new MessageDialog(getShell(),
                    "Add Workflow Variable Confirmation", getShell()
                            .getDisplay().getSystemImage(SWT.ICON_QUESTION),
                    "Workflow variables will only be available if the nodes are"
                            + " reset. You may skip reset if you are aware "
                            + "that the workflow variable will not be "
                            + "accessible then. Reset workflow now?",
                    MessageDialog.QUESTION, new String[]{"Skip reset", "Reset",
                            "Cancel"}, 1);
            return dialog.open();
        } else {
            // return "reset" -> anyway there are no executed nodes
            // (not isResetable)
            return 1;
        }
    }

    private boolean containsExecutedNodes(final WorkflowManager workflow) {
        for (NodeContainer node : workflow.getNodeContainers()) {
            if (node.getState().equals(NodeContainer.State.EXECUTED)) {
                // we only check for executed nodes
                // and not for canReset node (should be checked by the caller
                // of this method
                return true;
            }
        }
        return false;
    }

    private void addWorkflowVariable() {
        WorkflowVariablesEditDialog dialog = new WorkflowVariablesEditDialog();
        if (dialog.open() == Dialog.CANCEL) {
            // if the user has canceled the dialog there is nothing left to do
            return;
        }
        ScopeVariable var = dialog.getScopeVariable();
        // do not add it do WFM directly -> this is done when closing the dialog
        if (!m_table.add(var)) {
            MessageDialog.openWarning(getShell(), "Variable already exists", 
                    " A variable with the same name and type already exists. "
                    + "Edit this one if you want to change the value.");
        } else {
            m_table.getViewer().refresh();
        }
        getShell().forceFocus();
    }

    private void editWorkflowVariable(final ScopeVariable selectedVar,
            final int selectionIdx) {
        if (selectedVar.isGlobalConstant()) {
            MessageDialog.openError(getParentShell(), "Global Constant",
                    selectedVar.getName()
                    + " is a global constant " + "and can not be modified!");
            return;
        }
        WorkflowVariablesEditDialog dialog = new WorkflowVariablesEditDialog();
        dialog.create();
        dialog.loadFrom(selectedVar);
        if (dialog.open() == Dialog.CANCEL) {
            // if the user has canceled the dialog there is nothing left to do
            return;
        } // else replace it...
        ScopeVariable var = dialog.getScopeVariable();
        m_table.replace(selectionIdx, var);         
        m_table.getViewer().refresh();
    }
    
    private void removeWorkflowVariable(final ScopeVariable var) {
        if (var.isGlobalConstant()) {
            MessageDialog.openError(getParentShell(),
                    "Global Constant", var.getName()
                            + " is a global constant "
                            + "and can not be removed!");
            return;
        }
        // remember that something has changed 
        // -> will be deleted in replaceWorkflowVariable
        // remove it from the table anyway
        m_table.remove(var);
        m_table.getViewer().refresh();
    }
    
    private boolean hasValueChanged(final ScopeVariable newVar1, 
            final ScopeVariable newVar2) {
        if (newVar1.equals(newVar2)) {
            // type and name are the same -> ScopeVariable#equals
            assert newVar1.getType().equals(newVar2.getType());
            assert newVar1.getName().equals(newVar2.getName());
            // now check whether the value has changed 
            // without knowing the type of the WorkflowVariable
            ScopeVariable.Type type = newVar1.getType();
            if (ScopeVariable.Type.STRING.equals(type)) {
                if (newVar1.getStringValue().equals(newVar2.getStringValue())) {
                    return false;
                }
            } else if (ScopeVariable.Type.DOUBLE.equals(type)) {
                if (Double.compare(
                        newVar1.getDoubleValue(), 
                        newVar2.getDoubleValue()) == 0) {
                    return false;
                }
            } else if (ScopeVariable.Type.INTEGER.equals(type)) {
                if (newVar1.getIntValue() == newVar2.getIntValue()) {
                    return false;
                }
            } else {
                assert false : type.name() + " not yet supported";
            }
        }
        return true;
    }
    
    private boolean hasChanges() {
        List<ScopeVariable> wfmList = m_workflow.getWorkflowVariables();
        List<ScopeVariable> dialogList = m_table.getVariables();
        // different number of elements -> must contain changes
        if (wfmList.size() != dialogList.size()) {
            return true;
        }
        boolean hasChanges = false;
        for (ScopeVariable v : dialogList) {
            int idx = wfmList.indexOf(v);
            if (idx >= 0) {
                hasChanges |= hasValueChanged(v, wfmList.get(idx));
            } else {
                // if a variable is in the dialog but not in workflow
                // we have some changes
                return true;
            }
        }
        return hasChanges;
    }
    
    
    private void replaceWorkflowVariables(final boolean skipReset) {
        List<ScopeVariable> toBeRemoved = new ArrayList<ScopeVariable>();
        toBeRemoved.addAll(m_workflow.getWorkflowVariables()); 
        toBeRemoved.removeAll(m_table.getVariables());
        for (ScopeVariable v : toBeRemoved) {
            m_workflow.removeWorkflowVariable(v.getName());
        }
        // replace
        ScopeVariable[] vars = new ScopeVariable[m_table.getVariables().size()];
        m_workflow.addWorkflowVariables(skipReset, 
                m_table.getVariables().toArray(vars));
    }

    /**
     * 
     * {@inheritDoc}
     */
    @Override
    protected void okPressed() {
        // if one or more variables were added or edited
        // first ask flag -> if true do a closer investigation...
        if (hasChanges()) {
            // -> ask for reset confirmation
            int returnCode = openConfirmationDialog();
            // 1. skip reset -> add table content to WFM and do not reset
            // 2. reset -> add table content to WFM and do reset
            // 3. cancel -> do nothing
            if (returnCode == RESET_IDX || returnCode == SKIP_RESET_IDX) {
                replaceWorkflowVariables(returnCode == SKIP_RESET_IDX);
            } else {
                // CANCEL -> let dialog open
                return;
            }
        }
        // no changes -> close dialog
        super.okPressed();
    }
    
    /**
     * 
     * {@inheritDoc}
     */
    @Override
    protected void cancelPressed() {
        // if has changes -> open confirmation dialog "discard changes?"
        if (hasChanges()) {
            if (!MessageDialog.openConfirm(getShell(), 
                    "Discard Changes", 
                    "Do you really want to discard your changes?")) {
                // leave it open
                return;
            }
        }
        super.cancelPressed();
    }
}
