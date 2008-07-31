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
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.internal.ide.IDEWorkbenchMessages;
import org.eclipse.ui.internal.ide.misc.ContainerContentProvider;
import org.eclipse.ui.part.DrillDownComposite;
import org.knime.workbench.ui.navigator.KnimeResourceLableProvider;
import org.knime.workbench.ui.navigator.KnimeResourcePatternFilter;

/**
 * Workbench-level composite for choosing a container. This is a complete copy
 * of the Eclipse implementation, just to change the filter of the viewer. So,
 * only projects are shown.
 * 
 * @author Christoph Sieb - University of Konstanz
 */
public class ContainerSelectionGroup extends Composite {

    // The listener to notify of events
    private Listener m_listener;

    // Enable user to type in new container name
    private boolean m_allowNewContainerName = true;

    // show all projects by default
    private boolean m_showClosedProjects = true;

    // Last selection made by user
    private IContainer m_selectedContainer;

    // handle on parts
    private Text m_containerNameField;

    TreeViewer m_treeViewer;

    // the message to display at the top of this dialog
    private static final String DEFAULT_MSG_NEW_ALLOWED =
            IDEWorkbenchMessages.ContainerGroup_message;

    private static final String DEFAULT_MSG_SELECT_ONLY =
            IDEWorkbenchMessages.ContainerGroup_selectFolder;

    // sizing constants
    private static final int SIZING_SELECTION_PANE_WIDTH = 320;

    private static final int SIZING_SELECTION_PANE_HEIGHT = 300;

    /**
     * Creates a new instance of the widget.
     * 
     * @param parent The parent widget of the group.
     * @param listener A listener to forward events to. Can be null if no
     *            listener is required.
     * @param allowNewContainerName Enable the user to type in a new container
     *            name instead of just selecting from the existing ones.
     */
    public ContainerSelectionGroup(final Composite parent,
            final Listener listener, final boolean allowNewContainerName) {
        this(parent, listener, allowNewContainerName, null);
    }

    /**
     * Creates a new instance of the widget.
     * 
     * @param parent The parent widget of the group.
     * @param listener A listener to forward events to. Can be null if no
     *            listener is required.
     * @param allowNewContainerName Enable the user to type in a new container
     *            name instead of just selecting from the existing ones.
     * @param message The text to present to the user.
     */
    public ContainerSelectionGroup(final Composite parent,
            final Listener listener, final boolean allowNewContainerName,
            final String message) {
        this(parent, listener, allowNewContainerName, message, true);
    }

    /**
     * Creates a new instance of the widget.
     * 
     * @param parent The parent widget of the group.
     * @param listener A listener to forward events to. Can be null if no
     *            listener is required.
     * @param allowNewContainerName Enable the user to type in a new container
     *            name instead of just selecting from the existing ones.
     * @param message The text to present to the user.
     * @param showClosedProjects Whether or not to show closed projects.
     */
    public ContainerSelectionGroup(final Composite parent,
            final Listener listener, final boolean allowNewContainerName,
            final String message, final boolean showClosedProjects) {
        this(parent, listener, allowNewContainerName, message,
                showClosedProjects, SIZING_SELECTION_PANE_HEIGHT);
    }

    /**
     * Creates a new instance of the widget.
     * 
     * @param parent The parent widget of the group.
     * @param listener A listener to forward events to. Can be null if no
     *            listener is required.
     * @param allowNewContainerName Enable the user to type in a new container
     *            name instead of just selecting from the existing ones.
     * @param message The text to present to the user.
     * @param showClosedProjects Whether or not to show closed projects.
     * @param heightHint height hint for the drill down composite
     */
    public ContainerSelectionGroup(final Composite parent,
            final Listener listener, final boolean allowNewContainerName,
            final String message, final boolean showClosedProjects,
            final int heightHint) {
        super(parent, SWT.NONE);
        this.m_listener = listener;
        this.m_allowNewContainerName = allowNewContainerName;
        this.m_showClosedProjects = showClosedProjects;
        if (message != null) {
            createContents(message, heightHint);
        } else if (allowNewContainerName) {
            createContents(DEFAULT_MSG_NEW_ALLOWED, heightHint);
        } else {
            createContents(DEFAULT_MSG_SELECT_ONLY, heightHint);
        }
    }

    /**
     * The container selection has changed in the tree view. Update the
     * container name field value and notify all listeners.
     */
    public void containerSelectionChanged(final IContainer container) {
        m_selectedContainer = container;

        if (m_allowNewContainerName) {
            if (container == null) {
                m_containerNameField.setText("");
            } else {
                m_containerNameField.setText(container.getFullPath()
                        .makeRelative().toString());
            }
        }

        // fire an event so the parent can update its controls
        if (m_listener != null) {
            Event changeEvent = new Event();
            changeEvent.type = SWT.Selection;
            changeEvent.widget = this;
            m_listener.handleEvent(changeEvent);
        }

        m_treeViewer.expandAll();
    }

    /**
     * Creates the contents of the composite.
     */
    public void createContents(final String message) {
        createContents(message, SIZING_SELECTION_PANE_HEIGHT);
    }

    /**
     * Creates the contents of the composite.
     * 
     * @param heightHint height hint for the drill down composite
     */
    public void createContents(final String message, final int heightHint) {
        GridLayout layout = new GridLayout();
        layout.marginWidth = 0;
        setLayout(layout);
        setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        Label label = new Label(this, SWT.WRAP);
        label.setText(message);
        label.setFont(this.getFont());

        if (m_allowNewContainerName) {
            m_containerNameField = new Text(this, SWT.SINGLE | SWT.BORDER);
            m_containerNameField.setLayoutData(new GridData(
                    GridData.FILL_HORIZONTAL));
            m_containerNameField.addListener(SWT.Modify, m_listener);
            m_containerNameField.setFont(this.getFont());
        } else {
            // filler...
            new Label(this, SWT.NONE);
        }

        createTreeViewer(heightHint);
        Dialog.applyDialogFont(this);
    }

    /**
     * Returns a new drill down viewer for this dialog.
     * 
     * @param heightHint height hint for the drill down composite
     * @return a new drill down viewer
     */
    protected void createTreeViewer(final int heightHint) {
        // Create drill down.
        DrillDownComposite drillDown = new DrillDownComposite(this, SWT.BORDER);
        GridData spec = new GridData(SWT.FILL, SWT.FILL, true, true);
        spec.widthHint = SIZING_SELECTION_PANE_WIDTH;
        spec.heightHint = heightHint;
        drillDown.setLayoutData(spec);

        // Create tree viewer inside drill down.
        m_treeViewer = new TreeViewer(drillDown, SWT.NONE);
        m_treeViewer
                .addFilter(new KnimeResourcePatternFilter());
        m_treeViewer.setLabelProvider(new KnimeResourceLableProvider());
        drillDown.setChildTree(m_treeViewer);
        ContainerContentProvider cp = new ContainerContentProvider();
        cp.showClosedProjects(m_showClosedProjects);
        m_treeViewer.setContentProvider(cp);
        // treeViewer.setLabelProvider(WorkbenchLabelProvider
        // .getDecoratingWorkbenchLabelProvider());
        m_treeViewer.setSorter(new ViewerSorter());
        m_treeViewer
                .addSelectionChangedListener(new ISelectionChangedListener() {
                    public void selectionChanged(
                            final SelectionChangedEvent event) {
                        IStructuredSelection selection =
                                (IStructuredSelection)event.getSelection();
                        containerSelectionChanged((IContainer)selection
                                .getFirstElement()); // allow null
                    }
                });
        m_treeViewer.addDoubleClickListener(new IDoubleClickListener() {
            public void doubleClick(final DoubleClickEvent event) {
                ISelection selection = event.getSelection();
                if (selection instanceof IStructuredSelection) {
                    Object item =
                            ((IStructuredSelection)selection).getFirstElement();
                    if (m_treeViewer.getExpandedState(item)) {
                        m_treeViewer.collapseToLevel(item, 1);
                    } else {
                        m_treeViewer.expandToLevel(item, 1);
                    }
                }
            }
        });

        // This has to be done after the viewer has been laid out
        m_treeViewer.setInput(ResourcesPlugin.getWorkspace());
    }

    /**
     * Returns the currently entered container name. Null if the field is empty.
     * Note that the container may not exist yet if the user entered a new
     * container name in the field.
     */
    public IPath getContainerFullPath() {
        if (m_allowNewContainerName) {
            String pathName = m_containerNameField.getText();
            if (pathName == null || pathName.length() < 1) {
                return null;
            } else {
                // The user may not have made this absolute so do it for them
                return (new Path(pathName)).makeAbsolute();
            }
        } else {
            if (m_selectedContainer == null) {
                return null;
            } else {
                return m_selectedContainer.getFullPath();
            }
        }
    }

    /**
     * Gives focus to one of the widgets in the group, as determined by the
     * group.
     */
    public void setInitialFocus() {
        if (m_allowNewContainerName) {
            m_containerNameField.setFocus();
        } else {
            m_treeViewer.getTree().setFocus();
        }
    }

    /**
     * Sets the selected existing container.
     */
    public void setSelectedContainer(final IContainer container) {
        m_selectedContainer = container;

        // expand to and select the specified container
        List itemsToExpand = new ArrayList();
        IContainer parent = container.getParent();
        while (parent != null) {
            itemsToExpand.add(0, parent);
            parent = parent.getParent();
        }
        m_treeViewer.setExpandedElements(itemsToExpand.toArray());
        m_treeViewer.setSelection(new StructuredSelection(container), true);
    }
}
