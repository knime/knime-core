/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2008 - 2011
 * KNIME.com, Zurich, Switzerland
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.com
 * email: contact@knime.com
 * ---------------------------------------------------------------------
 *
 * History
 *   14.08.2009 (ohl): created
 */
package org.knime.workbench.ui.navigator.actions.selection;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.ISelectionValidator;
import org.knime.workbench.ui.navigator.actions.selection.TreeSelectionControl.TreeSelectionChangeListener;
import org.knime.workbench.ui.wizards.workflowgroup.KnimeResourceContentProviderWithRoot;
import org.knime.workbench.ui.wizards.workflowgroup.KnimeResourceLabelProviderWithRoot;

/**
 *
 * @author ohl, KNIME.com, Zurich, Switzerland
 */
public class ResourceSelectDialog extends Dialog {

    private final IContainer m_root;

    private IContainer m_selectedContainer;

    private final ISelection m_initialSelection;

    private ISelectionValidator m_validator = null;

    private String m_title = null;

    private String m_message = "";

    private boolean m_valid = true;

    public ResourceSelectDialog(final Shell parentShell,
            final IContainer localSelectionRoot,
            final ISelection initialSelection, final String message) {
        super(parentShell);
        m_message = message;
        m_root = localSelectionRoot;
        m_initialSelection = initialSelection;
        m_selectedContainer = findSelectedContainer(initialSelection);
    }

    public void setTitle(final String title) {
        m_title = title;
    }

    @Override
    protected void configureShell(final Shell newShell) {
        super.configureShell(newShell);
        if (m_title != null) {
            newShell.setText(m_title);
        }
    }

    public IContainer getSelection() {
        return m_selectedContainer;
    }

    @Override
    protected Control createDialogArea(final Composite parent) {

        TreeSelectionControl tree = new TreeSelectionControl();
        tree.setContentProvider(new KnimeResourceContentProviderWithRoot());
        tree.setLabelProvider(new KnimeResourceLabelProviderWithRoot());
        tree.setInitialSelection(m_initialSelection);
        tree.setInput(m_root);
        tree.setMessage(m_message);
        tree.setValidator(new ISelectionValidator() {

            public String isValid(final Object selection) {
                if (m_validator != null) {
                    String result = m_validator.isValid(selection);
                    Button b = getButton(IDialogConstants.OK_ID);
                    // store it in case button is not created yet
                    m_valid = result == null;
                    if (b != null) {
                        b.setEnabled(m_valid);
                    }
                    return result;
                }
                return null;
            }
        });
        tree.setChangeListener(new TreeSelectionChangeListener() {
            public void treeSelectionChanged(final Object newSelection,
                    final boolean valid) {
                m_selectedContainer = findSelectedContainer(newSelection);
            }
        });
        return tree.createTreeControl(parent);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Button createButton(final Composite parent, final int id, final String label,
            final boolean defaultButton) {
        Button b = super.createButton(parent, id, label, defaultButton);
        if (id == Dialog.OK && m_validator != null) {
            // sometimes the validator gets called before the button is created
            b.setEnabled(m_valid);
        }
        return b;
    }


    public void setValidator(final ISelectionValidator validator) {
        m_validator = validator;
    }

    private IContainer findSelectedContainer(final Object o) {
        if (o instanceof IWorkspace) {
            return ((IWorkspace)o).getRoot();
        } else if (o instanceof IWorkspaceRoot) {
            return (IContainer)o;
        } else if (o instanceof IContainer) {
            return (IContainer)o;
        } else if (o instanceof IPath) {
            IResource res = m_root.findMember((IPath)o);
            if (res instanceof IContainer) {
                return (IContainer)res;
            }
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isResizable() {
        return true;
    }
}
