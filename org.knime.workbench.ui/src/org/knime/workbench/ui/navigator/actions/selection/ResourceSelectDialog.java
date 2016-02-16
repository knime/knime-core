/*
 * ------------------------------------------------------------------------
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
 * ------------------------------------------------------------------------
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
 * 
 * @deprecated since AP 3.0
 */
@Deprecated
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
