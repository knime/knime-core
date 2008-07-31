/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
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
 * -------------------------------------------------------------------
 * 
 * History
 *   11.07.2006 (sieb): created
 */
package org.knime.workbench.ui.wizards.export;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.resource.JFaceColors;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ISelectionValidator;
import org.eclipse.ui.dialogs.SelectionDialog;
import org.eclipse.ui.internal.ide.IDEWorkbenchMessages;
import org.eclipse.ui.internal.ide.IIDEHelpContextIds;

/**
 * 
 * This is a complete copy of the Eclipse implementation, just to change the
 * ContainerSelectionGroup.
 * 
 * @author Christoph Sieb - University of Konstanz
 * 
 * A standard selection dialog which solicits a container resource from the
 * user. The <code>getResult</code> method returns the selected container
 * resource.
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 * <p>
 * Example:
 * 
 * <pre>
 * ContainerSelectionDialog dialog = new ContainerSelectionDialog(getShell(),
 *         initialSelection, allowNewContainerName(), msg);
 * dialog.open();
 * Object[] result = dialog.getResult();
 * </pre>
 * 
 * </p>
 */
public class ContainerSelectionDialog extends SelectionDialog {
    // the widget group;
    ContainerSelectionGroup m_group;

    // the root resource to populate the viewer with
    private IContainer m_initialSelection;

    // allow the user to type in a new container name
    private boolean m_allowNewContainerName = true;

    // the validation message
    Label m_statusMessage;

    // for validating the selection
    ISelectionValidator m_validator;

    // show closed projects by default
    private boolean m_showClosedProjects = true;

    /**
     * Creates a resource container selection dialog rooted at the given
     * resource. All selections are considered valid.
     * 
     * @param parentShell the parent shell
     * @param initialRoot the initial selection in the tree
     * @param allowNewContainerName <code>true</code> to enable the user to
     *            type in a new container name, and <code>false</code> to
     *            restrict the user to just selecting from existing ones
     * @param message the message to be displayed at the top of this dialog, or
     *            <code>null</code> to display a default message
     */
    public ContainerSelectionDialog(final Shell parentShell,
            final IContainer initialRoot, final boolean allowNewContainerName,
            final String message) {
        super(parentShell);
        setTitle(IDEWorkbenchMessages.ContainerSelectionDialog_title);
        this.m_initialSelection = initialRoot;
        this.m_allowNewContainerName = allowNewContainerName;
        if (message != null) {
            setMessage(message);
        } else {
            setMessage(IDEWorkbenchMessages.ContainerSelectionDialog_message);
        }
        setShellStyle(getShellStyle() | SWT.RESIZE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void configureShell(final Shell shell) {
        super.configureShell(shell);
        PlatformUI.getWorkbench().getHelpSystem().setHelp(shell,
                IIDEHelpContextIds.CONTAINER_SELECTION_DIALOG);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Control createDialogArea(final Composite parent) {
        // create composite
        Composite area = (Composite)super.createDialogArea(parent);

        Listener listener = new Listener() {
            public void handleEvent(final Event event) {
                if (m_statusMessage != null && m_validator != null) {
                    String errorMsg = m_validator.isValid(m_group
                            .getContainerFullPath());
                    if (errorMsg == null || errorMsg.equals("")) { //$NON-NLS-1$
                        m_statusMessage.setText(""); //$NON-NLS-1$
                        getOkButton().setEnabled(true);
                    } else {
                        m_statusMessage.setForeground(JFaceColors
                                .getErrorText(m_statusMessage.getDisplay()));
                        m_statusMessage.setText(errorMsg);
                        getOkButton().setEnabled(false);
                    }
                }
            }
        };

        // container selection group
        m_group = new ContainerSelectionGroup(area, listener,
                m_allowNewContainerName, getMessage(), m_showClosedProjects);
        if (m_initialSelection != null) {
            m_group.setSelectedContainer(m_initialSelection);
        }

        m_statusMessage = new Label(parent, SWT.NONE);
        m_statusMessage.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        m_statusMessage.setFont(parent.getFont());

        return dialogArea;
    }

    /**
     * The <code>ContainerSelectionDialog</code> implementation of this
     * <code>Dialog</code> method builds a list of the selected resource
     * containers for later retrieval by the client and closes this dialog.
     */
    @Override
    protected void okPressed() {

        List chosenContainerPathList = new ArrayList();
        IPath returnValue = m_group.getContainerFullPath();
        if (returnValue != null) {
            chosenContainerPathList.add(returnValue);
        }
        setResult(chosenContainerPathList);
        super.okPressed();
    }

    /**
     * Sets the validator to use.
     * 
     * @param validator A selection validator
     */
    public void setValidator(final ISelectionValidator validator) {
        this.m_validator = validator;
    }

    /**
     * Set whether or not closed projects should be shown in the selection
     * dialog.
     * 
     * @param show Whether or not to show closed projects.
     */
    public void showClosedProjects(final boolean show) {
        this.m_showClosedProjects = show;
    }
}
