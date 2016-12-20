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
 * -------------------------------------------------------------------
 *
 * History
 *   16.03.2005 (georg): created
 */
package org.knime.workbench.repository.view;

import java.lang.reflect.Method;
import java.util.ConcurrentModificationException;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.views.properties.IPropertySource;
import org.eclipse.ui.views.properties.IPropertySourceProvider;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.streamable.PartitionInfo;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.util.KNIMEJob;
import org.knime.workbench.core.nodeprovider.NodeProvider;
import org.knime.workbench.repository.NodeUsageRegistry;
import org.knime.workbench.repository.RepositoryManager;
import org.knime.workbench.repository.model.AbstractRepositoryObject;
import org.knime.workbench.repository.model.Category;
import org.knime.workbench.repository.model.IContainerObject;
import org.knime.workbench.repository.model.IRepositoryObject;
import org.knime.workbench.repository.model.MetaNodeTemplate;
import org.knime.workbench.repository.model.NodeTemplate;
import org.knime.workbench.repository.model.Root;
import org.osgi.framework.FrameworkUtil;

/**
 * This view shows the content of the repository that was loaded from the contributing extensions. It mainly includes a
 * tree viewer which shows the hierarchy of categories / nodes.
 *
 * @author Florian Georg, University of Konstanz
 * @author Thorsten Meinl, University of Konstanz
 * @author Martin Horn, University of Konstanz
 */
public abstract class AbstractRepositoryView extends ViewPart implements RepositoryManager.Listener {
    private static final NodeLogger LOGGER = NodeLogger.getLogger(AbstractRepositoryView.class);

    /**
     * The key to store/access the additional information about the streaming-ability of a node, as stored optionally
     * with an {@link AbstractRepositoryObject}.
     */
    final static String KEY_INFO_STREAMABLE = "info_streamable";

    /**
     * The tree component for showing the repository contents. It will be initialized in
     * {@link #createPartControl(Composite)}, thus do not try to use if before.
     */
    protected TreeViewer m_viewer;

    private final IPropertySourceProvider m_propertyProvider = new PropertyProvider();

    private SearchQueryContributionItem m_toolbarSearchText;

    private FilterStreamableNodesAction m_filterStreamNodesButton;

    private ShowAdditionalInfoAction m_showAddInfoButton;

    private FuzzySearchAction m_fuzzySearchButton;

    private static final Boolean NON_INSTANT_SEARCH = Boolean
        .getBoolean(KNIMEConstants.PROPERTY_REPOSITORY_NON_INSTANT_SEARCH);

    private int m_nodeCounter = 0;

    private long m_lastViewUpdate = 0;

    /* indicator whether the additional information for each repository object already has been determined */
    private boolean m_additionalInfoAvailable = false;

    /*text filter combined with 'additional info' filter (e.g. streaming) */
    private AdditionalInfoViewFilter m_textInfoFilter;

    /* fuzzy text filter combined with 'additional info' filter (e.g. streaming) */
    private AdditionalInfoViewFilter m_fuzzyTextInfoFilter;


    /**
     * The constructor.
     */
    public AbstractRepositoryView() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void init(final IViewSite site) throws PartInitException {
        super.init(site);
        // Bug#5807 set the initial focus on the search field.
        site.getPage().addPartListener(new IPartListener() {

            @Override
            public void partOpened(final IWorkbenchPart part) {
            }

            @Override
            public void partDeactivated(final IWorkbenchPart part) {
            }

            @Override
            public void partClosed(final IWorkbenchPart part) {
            }

            @Override
            public void partBroughtToTop(final IWorkbenchPart part) {
            }

            @Override
            public void partActivated(final IWorkbenchPart part) {
                if (part == AbstractRepositoryView.this) {
                    m_toolbarSearchText.getText().setFocus();
                }
            }
        });
    }

    /**
     * This callback creates the content of the view. The TreeViewer is initialized.
     *
     * @see org.eclipse.ui.IWorkbenchPart #createPartControl(org.eclipse.swt.widgets.Composite)
     * @param parent the parent composite
     */
    @Override
    public void createPartControl(final Composite parent) {
        parent.setCursor(new Cursor(Display.getDefault(), SWT.CURSOR_WAIT));

        m_viewer = new TreeViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
        m_viewer.getControl().setToolTipText("Loading node repository...");
        m_viewer.setContentProvider(new RepositoryContentProvider());
        m_viewer.setLabelProvider(new RepositoryStyledLabelProvider(new RepositoryLabelProvider(), false));
        m_viewer.setInput("Loading node repository...");
        contributeToActionBars();
        hookContextMenu();
        hookDoubleClickAction();
        // The viewer provides the selection to the workbench.
        this.getSite().setSelectionProvider(m_viewer);
        // The viewer supports drag&drop
        // (well, actually only drag - objects are dropped into the editor ;-)
        Transfer[] transfers = new Transfer[]{LocalSelectionTransfer.getTransfer()};
        m_viewer.addDragSupport(DND.DROP_COPY | DND.DROP_MOVE, transfers, new NodeTemplateDragListener(m_viewer));
        PlatformUI.getWorkbench().getHelpSystem()
            .setHelp(m_viewer.getControl(), "org.knime.workbench.help.repository_view_context");

        final Job treeUpdater = new KNIMEJob("Node Repository Loader", FrameworkUtil.getBundle(getClass())) {
            @Override
            protected IStatus run(final IProgressMonitor monitor) {
                readRepository(parent, monitor);
                return Status.OK_STATUS;
            }
        };
        treeUpdater.setSystem(true);
        treeUpdater.schedule();
    }

    /**
     * This method reads the repository contents and sets them as model into the {@link #m_viewer tree view}.
     *
     * @param parent the parent component of this view
     * @param monitor a progress monitor, must not be <code>null</code>
     */
    protected void readRepository(final Composite parent, final IProgressMonitor monitor) {
        RepositoryManager.INSTANCE.addLoadListener(this);
        Root repository = RepositoryManager.INSTANCE.getRoot(monitor);

        updateRepositoryView(repository);

        Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run() {
                if (!m_viewer.getControl().isDisposed()) {
                    parent.setCursor(null);
                    m_viewer.getControl().setToolTipText(null);
                }
            }
        });
        onReadingRepositoryDone();
    }

    /* called as soon as the repository has been read entirely */
    private void onReadingRepositoryDone() {
        m_filterStreamNodesButton.setEnabled(true);
        m_showAddInfoButton.setEnabled(true);
    }

    /**
     * This methods recursively retrieves and enriches the repository objects with additional information,
     * e.g. number of ports, whether the node is streamable and/or distributable, etc.
     * Should be called only after the repository content was already loaded with {@link #readRepository(Composite, IProgressMonitor)}.
     */
    protected void enrichWithAdditionalInfo(final IRepositoryObject parent, final IProgressMonitor monitor, final boolean updateTreeStructure) {
        if(monitor.isCanceled()) {
            return;
        }
        if (!m_additionalInfoAvailable) {
            if (parent instanceof IContainerObject) {
                IRepositoryObject[] children = ((IContainerObject)parent).getChildren();
                for (IRepositoryObject child : children) {
                    enrichWithAdditionalInfo(child, monitor, updateTreeStructure);
                }
            } else if (parent instanceof NodeTemplate) {
                NodeTemplate nodeTemplate = (NodeTemplate)parent;
                try {
                    NodeFactory<? extends NodeModel> nf = nodeTemplate.createFactoryInstance();
                    NodeModel nm = nf.createNodeModel();
                    //check whether the current node model overrides the #createStreamableOperator-method
                    Method m = nm.getClass().getMethod("createStreamableOperator", PartitionInfo.class,
                        PortObjectSpec[].class);
                    if (m.getDeclaringClass() != NodeModel.class) {
                        //method has been overriden -> node is probably streamable or distributable
                        nodeTemplate.addAdditionalInfo(KEY_INFO_STREAMABLE, "streamable");
                    }

                    //possible TODO: parse xml description and get some more additional information (e.g. short description, ...)
                    //                    nodeTemplate.addAdditionalInfo(KEY_INFO_SHORT_DESCRIPTION,
                    //                        "this could be the short description, number of ports etc.");
                } catch (Throwable t) {
                    LOGGER.error("Unable to instantiate the node " + nodeTemplate.getFactory().getName(), t);
                    return;
                }

                if (!updateTreeStructure) {
                    Display.getDefault().asyncExec(new Runnable() {
                        @Override
                        public void run() {
                            if (!m_viewer.getControl().isDisposed()) {
                                m_viewer.update(parent, null);
                            }
                        }
                    });
                } else {
                    Display.getDefault().asyncExec(new Runnable() {
                        @Override
                        public void run() {
                            if (!m_viewer.getControl().isDisposed()) {
                                m_viewer.update(parent, null);
                                TreeViewerUpdater.update(m_viewer, true, false);
                            }
                        }
                    });
                }
            }
        }
    }

    private void hookDoubleClickAction() {
        m_viewer.addDoubleClickListener(new IDoubleClickListener() {
            @Override
            public void doubleClick(final DoubleClickEvent event) {
                Object o = ((IStructuredSelection)event.getSelection()).getFirstElement();
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
                } else if (o instanceof MetaNodeTemplate) {
                    MetaNodeTemplate mnt = (MetaNodeTemplate)o;
                    NodeID metaNode = mnt.getManager().getID();
                    NodeProvider.INSTANCE.addMetaNode(WorkflowManager.META_NODE_ROOT, metaNode);
                } else if (o instanceof Category) {
                    m_viewer.setExpandedState(o, !m_viewer.getExpandedState(o));
                }
            }
        });
    }

    private void hookContextMenu() {
        MenuManager menuMgr = new MenuManager("#PopupMenu");
        menuMgr.setRemoveAllWhenShown(true);
        menuMgr.addMenuListener(new IMenuListener() {
            @Override
            public void menuAboutToShow(final IMenuManager manager) {
                AbstractRepositoryView.this.fillContextMenu(manager);
            }
        });
        Menu menu = menuMgr.createContextMenu(m_viewer.getControl());

        m_viewer.getControl().setMenu(menu);
        getSite().registerContextMenu(menuMgr, m_viewer);
    }

    private void contributeToActionBars() {
        initializeFilters();

        // Create drill down adapter
        IActionBars bars = getViewSite().getActionBars();
        fillLocalPullDown(bars.getMenuManager());
        fillLocalToolBar(bars.getToolBarManager());

    }

    private void fillLocalPullDown(final IMenuManager manager) {
        // register drill down actions

        //button that activates/deactivates the filter for streamable nodes
        m_filterStreamNodesButton = new FilterStreamableNodesAction(() -> {
            onFilterStreamableNodesClicked(m_textInfoFilter, m_fuzzyTextInfoFilter);
        });
        m_filterStreamNodesButton.setEnabled(false);

        //toggles whether the additional information should be displayed
        m_showAddInfoButton = new ShowAdditionalInfoAction(() -> {
            onShowAdditionalInfoClicked();
        });
        m_showAddInfoButton.setEnabled(false);

        manager.add(m_showAddInfoButton);
        manager.add(m_filterStreamNodesButton);

        manager.add(new Separator());
    }

    /**
     * Fills the context menu for the repository view. Subclasses can add additional entries.
     *
     * @param manager the menu manager for the context menu
     */
    protected void fillContextMenu(final IMenuManager manager) {
        // manager.add(m_action1);
        // manager.add(m_action2);
        // manager.add(new Separator());

        // register drill down actions

        // Other plug-ins can contribute there actions here
        manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
    }

    /**
     * Fills the tool bar of the repository view. Subclasses can add additional buttons to the toolbar.
     *
     * @param manager the toolbar manager
     */
    protected void fillLocalToolBar(final IToolBarManager manager) {


        //whether the fuzzy search or standard search is to be used
        m_fuzzySearchButton = new FuzzySearchAction(() -> {
            onFuzzySearchButtonClicked(m_fuzzyTextInfoFilter, m_textInfoFilter);
        });

        manager.add(m_fuzzySearchButton);

        manager.add(new Separator());

        // create the combo contribution item that provides the query string
        m_toolbarSearchText = new SearchQueryContributionItem(m_viewer, m_fuzzySearchButton.isChecked()
            ? m_fuzzyTextInfoFilter.getDelegateFilter() : m_textInfoFilter.getDelegateFilter(), !NON_INSTANT_SEARCH);
        //set the streamable-node filter (that wraps the other one)
        m_viewer
            .setFilters(new ViewerFilter[]{m_fuzzySearchButton.isChecked() ? m_fuzzyTextInfoFilter : m_textInfoFilter});
        m_toolbarSearchText.setQueryChangedCallback(() -> {
            onSearchQueryChanged();
        });
        manager.add(m_toolbarSearchText);
        manager.add(new Separator());
    }


    private void initializeFilters() {
        //prepare the filters to be shared between different items, or combined

        //text only filter
        final RepositoryViewFilter textFilter = new RepositoryViewFilter();

        //fuzzy text filter
        final TanimotoTextualViewFilter fuzzyFilter = new TanimotoTextualViewFilter();

        //text filter combined with 'additional info' filter (e.g. streaming)
        m_textInfoFilter = new AdditionalInfoViewFilter(textFilter, KEY_INFO_STREAMABLE);

        //fuzzy text filter combinded with the 'additional info' filter (e.g. streaming)
        m_fuzzyTextInfoFilter = new AdditionalInfoViewFilter(fuzzyFilter, KEY_INFO_STREAMABLE);
    }

    /* called whenever the search query changes (no matter in what search mode, i.e. fuzzy or text) */
    private void onSearchQueryChanged() {
        if (m_fuzzySearchButton.isChecked()) {
            // if the query string is empty, use the category tree, otherwise show the node list (in case fuzzy search is activated)
            if (m_fuzzyTextInfoFilter.getDelegateFilter().hasNonEmptyQuery()) {
                if (!(m_viewer.getContentProvider() instanceof ListRepositoryContentProvider)) {
                    //only change the content provider if its not a list content provider already
                    m_viewer.setContentProvider(new ListRepositoryContentProvider());

                    //sync the additional info to be shown
                    onShowAdditionalInfoClicked();
                }
                m_viewer.setComparator(
                    new ViewerComparator(m_fuzzyTextInfoFilter.getDelegateFilter().createComparator()));
            } else {
                if (!(m_viewer.getContentProvider() instanceof RepositoryContentProvider)) {
                    //only change the content provider if its not a tree content provider already
                    m_viewer.setContentProvider(new RepositoryContentProvider());
                    m_viewer.setComparator(null);

                    //sync the additional info to be shown
                    onShowAdditionalInfoClicked();
                }
            }

        }
    }

    /* action to be performed if the "Fuzzy Search" button is clicked */
    private void onFuzzySearchButtonClicked(final AdditionalInfoViewFilter extFuzzyFilter, final AdditionalInfoViewFilter extTextFilter) {
        if (m_fuzzySearchButton.isChecked()) {
            m_viewer.setFilters(new ViewerFilter[]{extFuzzyFilter});
            m_toolbarSearchText.setFilter(extFuzzyFilter.getDelegateFilter());

            //sync streamable filter settings
            extTextFilter.setDoFilter(m_filterStreamNodesButton.isChecked());
            extFuzzyFilter.setDoFilter(m_filterStreamNodesButton.isChecked());

            //transfer the search query
            extFuzzyFilter.getDelegateFilter().setQueryString(extTextFilter.getDelegateFilter().getQueryString());

            //set the content provider. If search query is empty, show the tree, otherwise the list
            if(extTextFilter.getDelegateFilter().hasNonEmptyQuery()) {
                m_viewer.setContentProvider(new ListRepositoryContentProvider());
                m_viewer.setComparator(new ViewerComparator(extFuzzyFilter.getDelegateFilter().createComparator()));
            } else {
                m_viewer.setContentProvider(new RepositoryContentProvider());
                m_viewer.setComparator(null);
            }
        } else {
            m_viewer.setContentProvider(new RepositoryContentProvider());
            m_viewer.setFilters(new ViewerFilter[]{extTextFilter});
            m_toolbarSearchText.setFilter(extTextFilter.getDelegateFilter());

            //sync streamable filter settings
            extTextFilter.setDoFilter(m_filterStreamNodesButton.isChecked());
            extFuzzyFilter.setDoFilter(m_filterStreamNodesButton.isChecked());

            //transfer the search query
            extTextFilter.getDelegateFilter().setQueryString(extFuzzyFilter.getDelegateFilter().getQueryString());
            m_viewer.setComparator(null);
        }

        //sync the additional info info
        //mainly to set the right label provider
        onShowAdditionalInfoClicked();

        if (extTextFilter.getDelegateFilter().hasNonEmptyQuery()) {
            TreeViewerUpdater.update(m_viewer, true, true);
        }
    }

    /* action to be performed if the "Show Additional Info" button is clicked */
    private void onShowAdditionalInfoClicked() {
        boolean showCategory =
                m_fuzzySearchButton.isChecked() && m_fuzzyTextInfoFilter.getDelegateFilter().hasNonEmptyQuery();
        if (m_showAddInfoButton.isChecked()) {
            m_viewer.setLabelProvider(
                new RepositoryStyledLabelProvider(new RepositoryLabelProvider(), showCategory, KEY_INFO_STREAMABLE));
        } else {
            m_viewer.setLabelProvider(
                new RepositoryStyledLabelProvider(new RepositoryLabelProvider(), showCategory));
        }
        //ensure that the additional information is available and load it lazily if not
        if (!m_additionalInfoAvailable && m_showAddInfoButton.isChecked()) {
            m_showAddInfoButton.setEnabled(false);
            m_filterStreamNodesButton.setEnabled(false);
            final Job nodeInfoUpdater =
                new KNIMEJob("Additional Node Info Repository Loader", FrameworkUtil.getBundle(getClass())) {
                    @Override
                    protected IStatus run(final IProgressMonitor monitor) {
                        enrichWithAdditionalInfo(RepositoryManager.INSTANCE.getRoot(), monitor, false);
                        m_additionalInfoAvailable = true;
                        m_showAddInfoButton.setEnabled(true);
                        m_filterStreamNodesButton.setEnabled(true);
                        return Status.OK_STATUS;
                    }
                };
            nodeInfoUpdater.setSystem(true);
            nodeInfoUpdater.schedule();
        }
    }

    /* action to be performed if the "Filter Streamable Nodes" button is clicked */
    private void onFilterStreamableNodesClicked(final AdditionalInfoViewFilter infoTextFilter,
        final AdditionalInfoViewFilter infoFuzzyFilter) {
        //ensure that the additional information is available and load it lazily if not
        if (!m_additionalInfoAvailable && m_filterStreamNodesButton.isChecked()) {
            m_filterStreamNodesButton.setEnabled(false);
            m_showAddInfoButton.setEnabled(false);
            final Job nodeInfoUpdater =
                new KNIMEJob("Additional Node Info Repository Loader", FrameworkUtil.getBundle(getClass())) {
                @Override
                protected IStatus run(final IProgressMonitor monitor) {
                    m_nodeCounter = 0;
                    //set filters
                    infoTextFilter.setDoFilter(m_filterStreamNodesButton.isChecked());
                    infoFuzzyFilter.setDoFilter(m_filterStreamNodesButton.isChecked());
                    enrichWithAdditionalInfo(RepositoryManager.INSTANCE.getRoot(), monitor, true);
                    m_additionalInfoAvailable = true;
                    m_filterStreamNodesButton.setEnabled(true);
                    m_showAddInfoButton.setEnabled(true);
                    Display.getDefault().asyncExec(new Runnable() {
                        @Override
                        public void run() {
                            if (!m_viewer.getControl().isDisposed()) {
                                //update view
                                TreeViewerUpdater.update(m_viewer, true, false);
                            }
                        }
                    });
                    return Status.OK_STATUS;
                }
            };
            nodeInfoUpdater.setSystem(true);
            nodeInfoUpdater.schedule();
        } else {
            //set filter
            infoTextFilter.setDoFilter(m_filterStreamNodesButton.isChecked());
            infoFuzzyFilter.setDoFilter(m_filterStreamNodesButton.isChecked());
            //update view
            TreeViewerUpdater.update(m_viewer, true, true);
        }
    }

    /**
     * Passing the focus request to the m_viewer's control.
     */
    @Override
    public void setFocus() {
        m_viewer.getControl().setFocus();
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T> T getAdapter(final Class<T> adapter) {
        if (adapter == IPropertySourceProvider.class) {
            return (T)m_propertyProvider;
        }
        return super.getAdapter(adapter);
    }

    /**
     * Property source provider.
     *
     * @author Florian Georg, University of Konstanz
     */
    private static class PropertyProvider implements IPropertySourceProvider {
        /**
         * Delegates the request, if the object is an IAdaptable.
         *
         * @see org.eclipse.ui.views.properties.IPropertySourceProvider# getPropertySource(java.lang.Object)
         */
        @Override
        public IPropertySource getPropertySource(final Object object) {
            // Look if we can get an adapter to IPropertySource....
            if (object instanceof IAdaptable) {
                IAdaptable adaptable = (IAdaptable)object;

                return adaptable.getAdapter(IPropertySource.class);
            }

            // well, no :-(
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void newCategory(final Root root, final Category category) {
        // do nothing yet (this is quite fast)
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void newNode(final Root root, final NodeTemplate node) {
        m_nodeCounter++;
        if (System.currentTimeMillis() - m_lastViewUpdate < 500) {
            return;
        }
        updateRepositoryView(root);
        m_lastViewUpdate = System.currentTimeMillis();

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void newMetanode(final Root root, final MetaNodeTemplate metanode) {
        m_nodeCounter++;
        if (System.currentTimeMillis() - m_lastViewUpdate < 500) {
            return;
        }
        updateRepositoryView(root);
        m_lastViewUpdate = System.currentTimeMillis();
    }

    private void updateRepositoryView(final Root root) {
        final Root transformedRepository = transformRepository(root);

        Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run() {
                if (!m_viewer.getControl().isDisposed()) {
                    m_viewer.getControl()
                        .setToolTipText("Loading node repository... " + m_nodeCounter + " nodes found");
                    if (m_viewer.getInput() != transformedRepository) {
                        m_viewer.setInput(transformedRepository);
                    } else {
                        try {
                            m_viewer.refresh(transformedRepository);
                        } catch (ConcurrentModificationException ex) {
                            // ignore, this may happen if new nodes
                            // are added while the viewer is updating
                        }
                    }
                }
            }
        });
    }

    /**
     * This method may be overridden by subclasses in order to transform the current repository into a different
     * structure. The method gets the current repository (its root) as argument and should return a new repository (its
     * root). The default implementation does not change the repository.
     *
     * @param originalRoot the root if the original repository
     * @return the root of a potentially transformed repository
     */
    protected Root transformRepository(final Root originalRoot) {
        return originalRoot;
    }
}
