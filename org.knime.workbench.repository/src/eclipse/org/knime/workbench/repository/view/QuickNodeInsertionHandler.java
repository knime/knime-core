/*
 * ------------------------------------------------------------------------
 *
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
 * ---------------------------------------------------------------------
 *
 * History
 *   29.07.2014 (Marcel Hanser): created
 */
package org.knime.workbench.repository.view;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.util.KNIMEJob;
import org.knime.workbench.core.nodeprovider.NodeProvider;
import org.knime.workbench.repository.NodeUsageRegistry;
import org.knime.workbench.repository.RepositoryFactory;
import org.knime.workbench.repository.RepositoryManager;
import org.knime.workbench.repository.model.MetaNodeTemplate;
import org.knime.workbench.repository.model.NodeTemplate;
import org.knime.workbench.repository.model.Root;
import org.osgi.framework.FrameworkUtil;

/**
 * Opens the Quick Node Insertion dialog.
 *
 * @author Marcel Hanser
 */
public class QuickNodeInsertionHandler extends AbstractHandler {
    private static final NodeLogger LOGGER = NodeLogger.getLogger(QuickNodeInsertionHandler.class);

    /**
     * The eclipse command id.
     */
    public static final String COMMAND_ID = "org.knime.workbench.repository.view.QuickNodeInsertionHandler";

    @Override
    public Object execute(final ExecutionEvent event) throws ExecutionException {

        // open the dialog
        TitleAreaDialog titleAreaDialog =
            new TitleAreaDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell()) {

                private TanimotoTextualViewFilter m_repositoryViewFilter;

                private Text m_text;

                private TableViewer m_viewer;

                /**
                 * {@inheritDoc}
                 */
                @Override
                public void create() {
                    setShellStyle(getShellStyle() | SWT.RESIZE);
                    super.create();
                    setTitle("Quick Node Insertion");
                    setMessage("Type in the node name", IMessageProvider.INFORMATION);
                }

                @Override
                protected void buttonPressed(final int buttonId) {
                    if (IDialogConstants.OK_ID == buttonId) {
                        createNode(((IStructuredSelection)m_viewer.getSelection()).getFirstElement());
                        Display.getDefault().asyncExec(new Runnable() {
                            @Override
                            public void run() {
                                m_text.setFocus();
                            }
                        });
                    } else if (IDialogConstants.CANCEL_ID == buttonId) {
                        cancelPressed();
                    }
                }

                @Override
                protected Control createDialogArea(final Composite parent) {
                    Composite comp = (Composite)super.createDialogArea(parent);

                    m_text = new Text(comp, SWT.SINGLE | SWT.BORDER | SWT.BORDER_SOLID);
                    m_text.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

                    Label labels = new Label(comp, 0);
                    labels.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

                    m_viewer =
                        new TableViewer(comp, SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER | SWT.BORDER_SOLID);

                    ColumnViewerToolTipSupport.enableFor(m_viewer);
                    m_viewer.setContentProvider(new ListRepositoryContentProvider());
                    m_viewer.setLabelProvider(new RepositoryStyledLabelProvider(new RepositoryLabelProvider()));
                    m_viewer.setInput("Loading node repository...");
                    m_viewer.getTable().setLayoutData(new GridData(GridData.FILL_BOTH));
                    m_repositoryViewFilter = new TanimotoTextualViewFilter();
                    m_viewer.setFilters(new ViewerFilter[]{m_repositoryViewFilter});

                    m_viewer.addDoubleClickListener(new IDoubleClickListener() {
                        @Override
                        public void doubleClick(final DoubleClickEvent myEvent) {
                            createNode(((IStructuredSelection)myEvent.getSelection()).getFirstElement());
                        }

                    });

                    // just to remove the warning, that this ist not used.
                    //                    m_tooltipSupport.toString();

                    m_text.addKeyListener(new KeyAdapter() {

                        private static final int KEY_UP = 16777217;

                        private static final int KEY_DOWN = 16777218;

                        private String m_lastString;

                        /**
                         * {@inheritDoc}
                         */
                        @Override
                        public void keyPressed(final KeyEvent e) {
                            // set the selection down or up if the key is up or down
                            if (e.keyCode == KEY_UP || e.keyCode == KEY_DOWN) {
                                TableItem[] items = m_viewer.getTable().getItems();
                                IStructuredSelection hu = (IStructuredSelection)m_viewer.getSelection();

                                int i = -1;
                                if (!hu.isEmpty()) {
                                    Object selected = ((IStructuredSelection)m_viewer.getSelection()).getFirstElement();
                                    for (i = 0; i < items.length; i++) {
                                        if (items[i].getData() == selected) {
                                            break;
                                        }
                                    }
                                }

                                int index = (i == -1 ? 0 : (e.keyCode == KEY_UP ? i - 1 : i + 1));
                                if (index >= 0 && index < items.length) {
                                    m_viewer.setSelection(new StructuredSelection(items[index].getData()));
                                }
                            }
                        }

                        @Override
                        public void keyReleased(final KeyEvent e) {
                            String str = m_text.getText();

                            if (m_lastString == null || !m_lastString.equals(str)) {
                                m_repositoryViewFilter.setQueryString(str);
                                m_viewer.setComparator(new ViewerComparator(m_repositoryViewFilter.createComparator()));
                                m_viewer.getControl().setRedraw(true);
                                m_viewer.refresh();
                            }
                        }

                    });

                    readNodeRepositoryAsync(m_viewer);
                    return comp;
                }

                /**
                 * {@inheritDoc}
                 */
                @Override
                protected Point getInitialLocation(final Point initialSize) {
                    // move the window a bit to the left from the knime editor
                    Shell parentShell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
                    Rectangle shellBounds = parentShell.getBounds();
                    Point newLocation = new Point((int)((shellBounds.x) + (shellBounds.width) * 0.08), initialSize.y);
                    return newLocation;
                }

                /**
                 * {@inheritDoc}
                 */
                @Override
                protected Point getInitialSize() {
                    return new Point(300, 400);
                }
            };
        titleAreaDialog.open();
        return null;
    }

    /**
     *
     */
    private void readNodeRepositoryAsync(final TableViewer viewer) {
        final Job treeUpdater =
            new KNIMEJob("Node Repository Loader - Fast Selection Dialog", FrameworkUtil.getBundle(getClass())) {
                @Override
                protected IStatus run(final IProgressMonitor monitor) {
                    final Root root = RepositoryManager.INSTANCE.getRoot(monitor);
                    Display.getDefault().asyncExec(new Runnable() {
                        @Override
                        public void run() {
                            viewer.setInput(root);
                        }
                    });
                    return Status.OK_STATUS;
                }
            };
        treeUpdater.setSystem(true);
        treeUpdater.schedule();
    }

    /**
     * @param event
     */
    private static void createNode(final Object o) {
        if (o instanceof NodeTemplate) {
            NodeTemplate tmplt = (NodeTemplate)o;
            NodeFactory<? extends NodeModel> nodeFact;
            try {
                nodeFact = tmplt.createFactoryInstance();
            } catch (Exception e) {
                LOGGER.error("Unable to instantiate the selected node " + tmplt.getFactory().getName(), e);
                return;
            }
            boolean added = NodeProvider.INSTANCE.addNode(nodeFact);
            if (added) {
                NodeUsageRegistry.addNode(tmplt);
            }
        }
        if (o instanceof MetaNodeTemplate) {
            MetaNodeTemplate mnt = (MetaNodeTemplate)o;
            NodeID metaNode = mnt.getManager().getID();
            NodeProvider.INSTANCE.addMetaNode(RepositoryFactory.META_NODE_ROOT, metaNode);
        }
    }
}
