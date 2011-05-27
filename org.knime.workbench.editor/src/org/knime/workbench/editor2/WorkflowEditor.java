/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
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
 */
package org.knime.workbench.editor2;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.draw2d.FigureCanvas;
import org.eclipse.draw2d.Viewport;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.PrecisionPoint;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.DefaultEditDomain;
import org.eclipse.gef.EditDomain;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPartViewer;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.KeyHandler;
import org.eclipse.gef.RootEditPart;
import org.eclipse.gef.SnapToGeometry;
import org.eclipse.gef.SnapToGrid;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.commands.CommandStack;
import org.eclipse.gef.commands.CommandStackListener;
import org.eclipse.gef.editparts.ScalableFreeformRootEditPart;
import org.eclipse.gef.editparts.ZoomManager;
import org.eclipse.gef.ui.actions.ActionRegistry;
import org.eclipse.gef.ui.actions.PrintAction;
import org.eclipse.gef.ui.actions.RedoAction;
import org.eclipse.gef.ui.actions.SaveAction;
import org.eclipse.gef.ui.actions.StackAction;
import org.eclipse.gef.ui.actions.UndoAction;
import org.eclipse.gef.ui.actions.WorkbenchPartAction;
import org.eclipse.gef.ui.parts.GraphicalEditor;
import org.eclipse.gef.ui.properties.UndoablePropertySheetEntry;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.ISaveablePart2;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IURIEditorInput;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.EditorHistory;
import org.eclipse.ui.internal.Workbench;
import org.eclipse.ui.internal.WorkbenchMessages;
import org.eclipse.ui.internal.help.WorkbenchHelpSystem;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.progress.IProgressService;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import org.eclipse.ui.views.properties.IPropertySheetPage;
import org.eclipse.ui.views.properties.PropertySheetPage;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeContainer.State;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.NodePropertyChangedEvent;
import org.knime.core.node.workflow.NodePropertyChangedListener;
import org.knime.core.node.workflow.NodeStateChangeListener;
import org.knime.core.node.workflow.NodeStateEvent;
import org.knime.core.node.workflow.NodeUIInformation;
import org.knime.core.node.workflow.NodeUIInformationEvent;
import org.knime.core.node.workflow.NodeUIInformationListener;
import org.knime.core.node.workflow.SingleNodeContainer;
import org.knime.core.node.workflow.WorkflowEvent;
import org.knime.core.node.workflow.WorkflowListener;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.node.workflow.WorkflowPersistor;
import org.knime.core.util.Pointer;
import org.knime.workbench.core.nodeprovider.NodeProvider;
import org.knime.workbench.core.nodeprovider.NodeProvider.EventListener;
import org.knime.workbench.editor2.actions.AbstractNodeAction;
import org.knime.workbench.editor2.actions.AddAnnotationAction;
import org.knime.workbench.editor2.actions.CancelAction;
import org.knime.workbench.editor2.actions.CancelAllAction;
import org.knime.workbench.editor2.actions.CheckUpdateMetaNodeLinkAction;
import org.knime.workbench.editor2.actions.CollapseMetaNodeAction;
import org.knime.workbench.editor2.actions.CopyAction;
import org.knime.workbench.editor2.actions.CutAction;
import org.knime.workbench.editor2.actions.DefaultOpenViewAction;
import org.knime.workbench.editor2.actions.DisconnectMetaNodeLinkAction;
import org.knime.workbench.editor2.actions.ExecuteAction;
import org.knime.workbench.editor2.actions.ExecuteAllAction;
import org.knime.workbench.editor2.actions.ExecuteAndOpenViewAction;
import org.knime.workbench.editor2.actions.ExpandMetaNodeAction;
import org.knime.workbench.editor2.actions.HideNodeNamesAction;
import org.knime.workbench.editor2.actions.MetaNodeSetNameAction;
import org.knime.workbench.editor2.actions.NodeConnectionContainerDeleteAction;
import org.knime.workbench.editor2.actions.OpenDialogAction;
import org.knime.workbench.editor2.actions.PasteAction;
import org.knime.workbench.editor2.actions.PasteActionContextMenu;
import org.knime.workbench.editor2.actions.PauseLoopExecutionAction;
import org.knime.workbench.editor2.actions.ResetAction;
import org.knime.workbench.editor2.actions.ResumeLoopAction;
import org.knime.workbench.editor2.actions.SetNameAndDescriptionAction;
import org.knime.workbench.editor2.actions.StepLoopAction;
import org.knime.workbench.editor2.actions.UpdateMetaNodeLinkAction;
import org.knime.workbench.editor2.commands.CreateNewConnectedMetaNodeCommand;
import org.knime.workbench.editor2.commands.CreateNewConnectedNodeCommand;
import org.knime.workbench.editor2.commands.CreateNodeCommand;
import org.knime.workbench.editor2.editparts.NodeContainerEditPart;
import org.knime.workbench.editor2.editparts.WorkflowRootEditPart;
import org.knime.workbench.repository.RepositoryManager;
import org.knime.workbench.ui.KNIMEUIPlugin;
import org.knime.workbench.ui.SyncExecQueueDispatcher;
import org.knime.workbench.ui.navigator.KnimeResourceUtil;
import org.knime.workbench.ui.navigator.ProjectWorkflowMap;
import org.knime.workbench.ui.navigator.WorkflowEditorAdapter;
import org.knime.workbench.ui.preferences.PreferenceConstants;

/**
 * This is the implementation of the Eclipse Editor used for editing a
 * <code>WorkflowManager</code> object. This also handles the basic GEF stuff
 * (command stack) and hooks into the workbench to provide actions etc. ...
 *
 * @author Florian Georg, University of Konstanz
 * @author Christoph Sieb, University of Konstanz
 */
public class WorkflowEditor extends GraphicalEditor implements
        CommandStackListener, ISelectionListener, WorkflowListener,
        IResourceChangeListener, NodeStateChangeListener,
        NodePropertyChangedListener, ISaveablePart2, NodeUIInformationListener,
        EventListener {

    /** Id as defined in plugin.xml. */
    public static final String ID = "org.knime.workbench.editor.WorkflowEditor";

    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(WorkflowEditor.class);

    /** Clipboard name. */
    public static final String CLIPBOARD_ROOT_NAME = "clipboard";

    /**
     * The static clipboard for copy/cut/paste.
     */
    private static ClipboardObject clipboard;

    private static final Color BG_COLOR_WRITE_LOCK =
        new Color(null, 235, 235, 235);

    private static final Color BG_COLOR_DEFAULT =
        Display.getDefault().getSystemColor(SWT.COLOR_WHITE);

    /** root model object (=editor input) that is handled by the editor. * */
    private WorkflowManager m_manager;

    /** the editor's action registry. */
    private ActionRegistry m_actionRegistry;

    /** the <code>EditDomain</code>. */
    private final DefaultEditDomain m_editDomain;

    /** the dirty state. */
    private boolean m_isDirty;

    /** List with the action ids that are associated to this editor. * */
    private List<String> m_editorActions;

    private GraphicalViewer m_graphicalViewer;

    // path to the workflow directory (that contains the workflow.knime file)
    private URI m_fileResource;

    /** If subworkflow editor, store the parent for saving. */
    private WorkflowEditor m_parentEditor;

    private NewOverviewOutlinePage m_overviewOutlinePage;

    private PropertySheetPage m_undoablePropertySheetPage;

    private final WorkflowSelectionTool m_selectionTool;

    /** whether the afterOpen method has been run already (disallow queuing
     * another runnable). */
    private boolean m_hasAfterOpenRun = false;
    /** A list of runnable to be run after the editor is initialized.
     * See also {@link #addAfterOpenRunnable(Runnable)} for details. */
    private List<Runnable> m_afterOpenRunnables;

    /** Indicates if this editor has been closed. */
    private boolean m_closed;

    private String m_manuallySetToolTip;

    /**
     * No arg constructor, creates the edit domain for this editor.
     */
    public WorkflowEditor() {
        super();

        LOGGER.debug("Creating WorkflowEditor...");

        m_closed = false;

        // create an edit domain for this editor (handles the command stack)
        m_editDomain = new DefaultEditDomain(this);
        m_selectionTool = new WorkflowSelectionTool();
        m_editDomain.setActiveTool(m_selectionTool);
        m_editDomain.setDefaultTool(m_selectionTool);

        setEditDomain(m_editDomain);

        // initialize actions (can't be in init(), as setInput is called before)
        createActions();
    }

    /**
     * @return the graphical viewer of this editor.
     */
    public GraphicalViewer getViewer() {
        return getGraphicalViewer();
    }

    /**
     * Returns the clipboard content for this editor.
     *
     * @return the clipboard for this editor
     */
    public ClipboardObject getClipboardContent() {
        return clipboard;
    }

    /**
     * Sets the clipboard content for this editor.
     *
     * @param content the content to set into the clipboard
     *
     */
    public void setClipboardContent(final ClipboardObject content) {
        clipboard = content;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void init(final IEditorSite site, final IEditorInput input)
            throws PartInitException {

        LOGGER.debug("Initializing editor UI...");

        // TODO FIXME: Colors need to be assigned to different debugging levels
        NodeLogger.getLogger(WorkflowEditor.class).debug(
                "Opening workflow Editor on " + input.getName());

        // store site and input
        setSite(site);
        setInput(input);

        // add this as a CommandStackListener
        getCommandStack().addCommandStackListener(this);

        // add this as a selection change listener
        getSite().getWorkbenchWindow().getSelectionService()
                .addSelectionListener(this);

        // add this editor as a listener to WorkflowEvents
        m_manager.addListener(this);
        m_manager.addNodePropertyChangedListener(this);

        queueAfterOpen();
    }

    /** Add an action that is run in the SWT main thread after the editor
     * is initialized. It's used to prompt for additional actions (update links)
     * or to display notifications.
     * @param action The action to queue.
     * @throws IllegalStateException If editor is already initialized and the
     * after-open method has already been called. */
    public void addAfterOpenRunnable(final Runnable action) {
        if (m_hasAfterOpenRun) {
            throw new IllegalStateException("Can't queue afterOpen-runner - "
                    + "method has already been run");
        }
        if (m_afterOpenRunnables == null) {
            m_afterOpenRunnables = new ArrayList<Runnable>();
        }
        m_afterOpenRunnables.add(action);
    }

    /** Queues all {@link Runnable} in the after-open-runnable list in the
     * SWT main thread. */
    private void queueAfterOpen() {
        m_hasAfterOpenRun = true;
        if (m_afterOpenRunnables != null) {
            final Display d = Display.getDefault();
            for (final Runnable r : m_afterOpenRunnables) {
                d.asyncExec(r);
            }
        }
        m_afterOpenRunnables = null;
    }

    private List<IEditorPart> getSubEditors() {
        List<IEditorPart> editors = new ArrayList<IEditorPart>();
        if (m_manager == null) {
            // no workflow, no sub editors
            return editors;
        }
        for (NodeContainer child : m_manager.getNodeContainers()) {
            if (child instanceof SingleNodeContainer) {
                continue;
            }
            WorkflowManagerInput in =
                    new WorkflowManagerInput((WorkflowManager)child, this);
            if (PlatformUI.getWorkbench().getActiveWorkbenchWindow() != null) {
                for (IWorkbenchPage p : PlatformUI.getWorkbench()
                        .getActiveWorkbenchWindow().getPages()) {
                    IEditorPart editor = p.findEditor(in);
                    if (editor != null) {
                        editors.add(editor);
                        if (editor instanceof WorkflowEditor) {
                            /*
                             * Recursively get all subeditors. This is necessary
                             * for meta nodes in meta nodes.
                             */
                            editors.addAll(((WorkflowEditor)editor)
                                    .getSubEditors());
                        }
                    }
                }
            }
        }
        return editors;
    }

    /**
     * Deregisters all listeners when the editor is disposed.
     *
     * @see org.eclipse.ui.IWorkbenchPart#dispose()
     */
    @Override
    public void dispose() {
        NodeLogger.getLogger(WorkflowEditor.class).debug("Disposing editor...");
        if (m_fileResource != null && m_manager != null) {
            // disposed is also called when workflow load fails or is canceled
            ProjectWorkflowMap.unregisterClientFrom(m_fileResource, this);
            ProjectWorkflowMap.remove(m_fileResource);
            m_manager.removeListener(this);
            m_manager.removeNodeStateChangeListener(this);
            m_manager.removeUIInformationListener(this);
        }
        // remember that this editor has been closed
        m_closed = true;
        for (IEditorPart child : getSubEditors()) {
            child.getEditorSite().getPage().closeEditor(child, false);
        }
        NodeProvider.INSTANCE.removeListener(this);
        m_manager.removeNodePropertyChangedListener(this);
        getSite().getWorkbenchWindow().getSelectionService()
                .removeSelectionListener(this);
        // remove resource listener..
        if (m_fileResource != null
                && KnimeResourceUtil.getResourceForURI(m_fileResource)
                        != null) {
            ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
        }
        if (m_parentEditor != null && m_manager != null) {
            // bug fix 2051: Possible memory leak related to sub-flow editor.
            // meta node editors were still referenced by the EditorHistory
            IWorkbench workbench = PlatformUI.getWorkbench();
            if (workbench instanceof Workbench) {
                EditorHistory hist = ((Workbench)workbench).getEditorHistory();
                WorkflowManagerInput wfmInput =
                        new WorkflowManagerInput(m_manager, m_parentEditor);
                hist.remove(wfmInput);
            }
        }

        getCommandStack().removeCommandStackListener(this);
        super.dispose();
    }

    /**
     * Creates the editor actions.
     *
     * @see org.eclipse.gef.ui.parts.GraphicalEditor#createActions()
     */
    @Override
    protected void createActions() {
        LOGGER.debug("creating editor actions...");

        // super already does something for us...
        super.createActions();

        // Stack actions
        StackAction undo = new UndoAction(this);
        StackAction redo = new RedoAction(this);

        // Editor Actions
        WorkbenchPartAction delete =
                new NodeConnectionContainerDeleteAction(this);
        WorkbenchPartAction save = new SaveAction(this);
        WorkbenchPartAction print = new PrintAction(this);
        WorkbenchPartAction hideNodeName = new HideNodeNamesAction(this);

        // node actions
        //
        AbstractNodeAction openDialog = new OpenDialogAction(this);

        AbstractNodeAction execute = new ExecuteAction(this);
        AbstractNodeAction executeAll = new ExecuteAllAction(this);
        AbstractNodeAction cancelAll = new CancelAllAction(this);
        AbstractNodeAction cancel = new CancelAction(this);
        AbstractNodeAction pause = new PauseLoopExecutionAction(this);
        AbstractNodeAction step = new StepLoopAction(this);
        AbstractNodeAction resume = new ResumeLoopAction(this);
        AbstractNodeAction executeAndView = new ExecuteAndOpenViewAction(this);
        AbstractNodeAction reset = new ResetAction(this);
        AbstractNodeAction setNameAndDescription =
                new SetNameAndDescriptionAction(this);
        AbstractNodeAction defaultOpenView = new DefaultOpenViewAction(this);

        AbstractNodeAction metaNodeSetName =
            new MetaNodeSetNameAction(this);
        AbstractNodeAction checkUpdateMetaNodeLink =
            new CheckUpdateMetaNodeLinkAction(this);
        AbstractNodeAction updateMetaNodeLink =
            new UpdateMetaNodeLinkAction(this);
        AbstractNodeAction disconnectMetaNodeLink =
            new DisconnectMetaNodeLinkAction(this);

        // new annotation action
        AddAnnotationAction annotation = new AddAnnotationAction(this);

        // copy / cut / paste action
        CopyAction copy = new CopyAction(this);
        CutAction cut = new CutAction(this);
        PasteAction paste = new PasteAction(this);
        PasteActionContextMenu pasteContext = new PasteActionContextMenu(this);
        CollapseMetaNodeAction collapse = new CollapseMetaNodeAction(this);
        ExpandMetaNodeAction expand = new ExpandMetaNodeAction(this);

        // register the actions
        m_actionRegistry.registerAction(undo);
        m_actionRegistry.registerAction(redo);
        m_actionRegistry.registerAction(delete);
        m_actionRegistry.registerAction(save);
        m_actionRegistry.registerAction(print);

        m_actionRegistry.registerAction(openDialog);
        m_actionRegistry.registerAction(execute);
        m_actionRegistry.registerAction(executeAll);
        m_actionRegistry.registerAction(cancelAll);
        m_actionRegistry.registerAction(cancel);
        m_actionRegistry.registerAction(pause);
        m_actionRegistry.registerAction(step);
        m_actionRegistry.registerAction(resume);
        m_actionRegistry.registerAction(executeAndView);
        m_actionRegistry.registerAction(reset);
        m_actionRegistry.registerAction(setNameAndDescription);
        m_actionRegistry.registerAction(defaultOpenView);

        m_actionRegistry.registerAction(copy);
        m_actionRegistry.registerAction(cut);
        m_actionRegistry.registerAction(paste);
        m_actionRegistry.registerAction(pasteContext);
        m_actionRegistry.registerAction(hideNodeName);
        m_actionRegistry.registerAction(collapse);
        m_actionRegistry.registerAction(expand);

        m_actionRegistry.registerAction(metaNodeSetName);
        m_actionRegistry.registerAction(checkUpdateMetaNodeLink);
        m_actionRegistry.registerAction(updateMetaNodeLink);
        m_actionRegistry.registerAction(disconnectMetaNodeLink);
        m_actionRegistry.registerAction(annotation);

        // remember ids for later updates via 'updateActions'
        m_editorActions = new ArrayList<String>();
        m_editorActions.add(undo.getId());
        m_editorActions.add(redo.getId());
        m_editorActions.add(delete.getId());
        m_editorActions.add(save.getId());

        m_editorActions.add(openDialog.getId());
        m_editorActions.add(execute.getId());
        m_editorActions.add(executeAll.getId());
        m_editorActions.add(cancelAll.getId());
        m_editorActions.add(executeAndView.getId());
        m_editorActions.add(reset.getId());
        m_editorActions.add(setNameAndDescription.getId());
        m_editorActions.add(defaultOpenView.getId());
        m_editorActions.add(hideNodeName.getId());
        m_editorActions.add(collapse.getId());
        m_editorActions.add(expand.getId());

        m_editorActions.add(copy.getId());
        m_editorActions.add(cut.getId());
        m_editorActions.add(paste.getId());
        m_editorActions.add(metaNodeSetName.getId());
        m_editorActions.add(checkUpdateMetaNodeLink.getId());
        m_editorActions.add(updateMetaNodeLink.getId());
        m_editorActions.add(annotation.getId());


    }

    /**
     * This hooks keys like F2 for editing inside the editor.
     *
     * @return The common (shared) key handler.
     */
    protected KeyHandler getCommonKeyHandler() {
        return new KeyHandler() {
            @Override
            public boolean keyPressed(final org.eclipse.swt.events.KeyEvent e) {
                if (e.keyCode == SWT.F2) {
                    NodeContainerEditPart[] parts = getNodeParts();
                    if (parts.length == 1) {
                        parts[0].performDirectEdit();
                    }
                }
                return super.keyPressed(e);
            }
        };
    }

    /**
     * Returns a list of selected NodeContainerEditPart objects.
     *
     * @return list of node containers
     */
    private NodeContainerEditPart[] getNodeParts() {
        ISelectionProvider provider = getEditorSite().getSelectionProvider();
        if (provider == null) {
            return new NodeContainerEditPart[0];
        }
        ISelection sel = provider.getSelection();
        if (!(sel instanceof IStructuredSelection)) {
            return new NodeContainerEditPart[0];
        }

        ArrayList<NodeContainerEditPart> objects =
                new ArrayList<NodeContainerEditPart>(
                        ((IStructuredSelection)sel).toList());
        // remove all objects that are not instance of NodeContainerEditPart
        for (Iterator iter = objects.iterator(); iter.hasNext();) {
            Object element = iter.next();
            if (!(element instanceof NodeContainerEditPart)) {
                iter.remove();
                continue;
            }
        }
        final NodeContainerEditPart[] parts =
                objects.toArray(new NodeContainerEditPart[objects.size()]);

        return parts;
    }

    /**
     * Returns the action registry for this editor. It is "lazy" created on
     * first invocation.
     *
     * @see org.eclipse.gef.ui.parts.GraphicalEditor#getActionRegistry()
     */
    @Override
    public ActionRegistry getActionRegistry() {
        if (m_actionRegistry == null) {
            m_actionRegistry = new ActionRegistry();
        }
        return m_actionRegistry;
    }

    /**
     * Creates the graphical viewer that is hosted in this editor and hooks
     * keyhandler and edit domain.
     *
     * @see org.eclipse.gef.ui.parts.GraphicalEditor
     *      #createGraphicalViewer(org.eclipse.swt.widgets.Composite)
     */
    @Override
    protected void createGraphicalViewer(final Composite parent) {
        IEditorSite editorSite = getEditorSite();
        GraphicalViewer viewer = null;
        viewer =
                new WorkflowGraphicalViewerCreator(editorSite,
                        this.getActionRegistry()).createViewer(parent);

        // Add a listener to the static node provider
        NodeProvider.INSTANCE.addListener(this);

        // Configure the key handler
        ModifierKeyHandler keyHandler =
                new ModifierKeyHandler(viewer);

        KeyHandler parentKeyHandler =
                keyHandler.setParent(getCommonKeyHandler());
        viewer.setKeyHandler(parentKeyHandler);

        // hook the viewer into the EditDomain
        getEditDomain().addViewer(viewer);

        // activate the viewer as selection provider for Eclipse
        getSite().setSelectionProvider(viewer);

        // remember this viewer
        m_graphicalViewer = viewer;

        // We already have the model - set it into the viewer
        getGraphicalViewer().setContents(m_manager);

        // add Help context
        WorkbenchHelpSystem.getInstance().setHelp(
                m_graphicalViewer.getControl(),
                "org.knime.workbench.help.flow_editor_context");

        loadProperties();
        updateEditorBackgroundColor();
        ((WorkflowRootEditPart)getGraphicalViewer().getRootEditPart()
                .getChildren().get(0))
                .createToolTipHelper(getSite().getShell());

    }

    /**
     * This does nothing by now, as all is handled by
     * <code>createGraphicalViewer</code>.
     *
     * @see org.eclipse.gef.ui.parts.GraphicalEditor
     *      #initializeGraphicalViewer()
     */
    @Override
    protected void initializeGraphicalViewer() {
        // nothing
    }

    /**
     * Configures the graphical viewer.
     *
     * @see org.eclipse.gef.ui.parts.GraphicalEditor#configureGraphicalViewer()
     */
    @Override
    protected void configureGraphicalViewer() {
        super.configureGraphicalViewer();

    }

    /**
     * @return The graphical viewer in this editor
     * @see org.eclipse.gef.ui.parts.GraphicalEditor#getGraphicalViewer()
     */
    @Override
    protected GraphicalViewer getGraphicalViewer() {
        return m_graphicalViewer;
    }

    /**
     * Sets the editor input, that is, the file that contains the serialized
     * workflow manager.
     *
     * {@inheritDoc}
     */
    @Override
    protected void setInput(final IEditorInput input) {
        LOGGER.debug("Setting input into editor...");

        super.setInput(input);

        if (input instanceof WorkflowManagerInput) {
            setWorkflowManagerInput((WorkflowManagerInput)input);
        } else {

            // input is the (full, absolute) path to the workflow.knime file
            File wfFile = new File(((IURIEditorInput)input).getURI());
            try {
                // store the workflow directory
                m_fileResource = wfFile.getParentFile().toURI();
                String path = m_fileResource.getPath();
                if (path.endsWith("/")) {
                    // remove trailing slashes                }
                    path = path.substring(0, path.length() - 1);
                    m_fileResource =
                        new URI(m_fileResource.getScheme(), null, path, null);
                }
            } catch (URISyntaxException e1) {
                // then leave the slash...
            }

            LOGGER.debug("Resource File's project: " + m_fileResource);

            // register listener to check whether the underlying knime file
            // (input) has been deleted or renamed (in case it is a resource)
            if (KnimeResourceUtil.getResourceForURI(m_fileResource) != null) {
                ResourcesPlugin.getWorkspace().addResourceChangeListener(this,
                        IResourceChangeEvent.POST_CHANGE);
            }

            try {
                // FIXME:
                // setInput is called before the entire repository is loaded,
                // need to figure out how to do it the other way around
                // the static block needs to be executed, access
                // RepositoryManager.INSTANCE
                if (RepositoryManager.INSTANCE == null) {
                    LOGGER.fatal(
                            "Repository Manager Instance must not be null!");
                }
                assert m_manager == null;

                m_manager =
                        (WorkflowManager)ProjectWorkflowMap
                                .getWorkflow(m_fileResource);
                if (m_manager != null) {
                    // in case the workflow manager was edited somewhere else
                    // ...
                    if (m_manager.isDirty()) {
                        // ... make sure to inform the user about it
                        markDirty();
                    }
                } else {
                    IWorkbench wb = PlatformUI.getWorkbench();
                    IProgressService ps = wb.getProgressService();
                    // this one sets the workflow manager in the editor
                    LoadWorkflowRunnable loadWorflowRunnable =
                            new LoadWorkflowRunnable(this, wfFile);
                    ps.busyCursorWhile(loadWorflowRunnable);
                    // check if the editor should be disposed
                    if (m_manager == null) {
                        if (loadWorflowRunnable.hasLoadingBeenCanceled()) {
                            final String cancelError =
                                loadWorflowRunnable.getLoadingCanceledMessage();
                            Display.getDefault().asyncExec(new Runnable() {
                                @Override
                                public void run() {
                                    getEditorSite().getPage().closeEditor(
                                            WorkflowEditor.this, false);
                                    MessageBox mb =
                                        new MessageBox(Display
                                                .getDefault()
                                                .getActiveShell(),
                                                SWT.ICON_INFORMATION
                                                | SWT.OK);
                                    mb.setText("Editor could not be opened");
                                    mb.setMessage(cancelError);
                                    mb.open();
                                }

                            });
                            throw new OperationCanceledException(cancelError);
                        } else if (loadWorflowRunnable.getThrowable() != null) {
                            throw new RuntimeException(
                                    loadWorflowRunnable.getThrowable());
                        }
                    }
                    ProjectWorkflowMap.putWorkflow(m_fileResource, m_manager);
                }
                // in any case register as client (also if the workflow was
                // already
                // loaded by another client
                ProjectWorkflowMap.registerClientTo(m_fileResource, this);
                m_manager.addListener(this);
                m_manager.addNodeStateChangeListener(this);
            } catch (InterruptedException ie) {
                LOGGER.fatal("Workflow loading thread interrupted", ie);
            } catch (InvocationTargetException e) {
                LOGGER.fatal("Workflow could not be loaded.", e);
            }

            updatePartName();

            if (getGraphicalViewer() != null) {
                loadProperties();
            }

            // update Actions, as now there's everything available
            updateActions();
        }
    }

    private void updatePartName() {
        // Editor name (title)
        setPartName(getTitleToolTip());
        setTitleToolTip(getTitleToolTip());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setTitleToolTip(final String toolTip) {
        m_manuallySetToolTip = toolTip;
        super.setTitleToolTip(toolTip);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getTitleToolTip() {
        // only for projects -> they have file resources in title
        // when renamed, they cannot reflect the changes, thus, the manually
        // set title is returned...
        // meta nodes can do not have file resources in title
        if (m_manuallySetToolTip != null && m_parentEditor == null) {
            return m_manuallySetToolTip;
        }
        // if this is a project
        if (m_parentEditor == null) {
            return m_manager.getID().getIDWithoutRoot() + ": "
                    + new Path(m_fileResource.getPath()).lastSegment();
        } else {
            // we are a meta node editor
            // return id and node name (custom name)
            String name =
                    m_manager.getID().getIDWithoutRoot() + ": "
                            + m_manager.getName();
            if (m_manager.getCustomName() != null) {
                name += " (" + m_manager.getCustomName() + ")";
            }
            return name;
        }
    }

    private void setWorkflowManagerInput(final WorkflowManagerInput input) {
        m_parentEditor = input.getParentEditor();
        WorkflowManager wfm = (input).getWorkflowManager();
        setWorkflowManager(wfm);
        setPartName(input.getName());
        wfm.addListener(this);
        if (getGraphicalViewer() != null) {
            loadProperties();
        }

        // update Actions, as now there's everything available
        updateActions();
        updatePartName();
        m_manager.addUIInformationListener(this);
        return;
    }

    /**
     * Updates the actions of this workflow editor. Can be used from subclassing
     * objects to update the actions.
     */
    public void updateActions() {
        // TODO: update here the actions in the action bar
        // based on current selection
        // -> maybe solves the execute all enabled/disabled problem
        updateActions(m_editorActions);
    }

    /**
     * Returns the overview for the outline view.
     *
     * @return the overview
     */
    protected NewOverviewOutlinePage getOverviewOutlinePage() {
        if ((m_overviewOutlinePage == null) && (getGraphicalViewer() != null)) {
            RootEditPart rootEditPart = getGraphicalViewer().getRootEditPart();

            if (rootEditPart instanceof ScalableFreeformRootEditPart) {
                m_overviewOutlinePage =
                        new NewOverviewOutlinePage(
                                (ScalableFreeformRootEditPart)rootEditPart);
            }
        }

        return m_overviewOutlinePage;
    }

    /**
     * Returns the undoable <code>PropertySheetPage</code> for this editor.
     *
     * @return the undoable <code>PropertySheetPage</code>
     */
    protected PropertySheetPage getPropertySheetPage() {
        if (m_undoablePropertySheetPage == null) {
            m_undoablePropertySheetPage = new PropertySheetPage();

            m_undoablePropertySheetPage
                    .setRootEntry(new UndoablePropertySheetEntry(
                            getCommandStack()));
        }

        return m_undoablePropertySheetPage;
    }

    /**
     * @return The WFM that is edited by this editor ("root model object")
     */
    public WorkflowManager getWorkflowManager() {
        return m_manager;
    }

    /**
     * Adaptable implementation for Editor, returns the objects used in this
     * editor, if asked for.
     *
     * @see org.eclipse.gef.ui.parts.GraphicalEditor
     *      #getAdapter(java.lang.Class)
     */
    @Override
    public Object getAdapter(final Class adapter) {
        // we need to handle common GEF elements we created
        if (adapter == GraphicalViewer.class) {
            return this.getGraphicalViewer();
        } else if (adapter == EditPartViewer.class) {
            return this.getGraphicalViewer();
        } else if (adapter == CommandStack.class) {
            return this.getCommandStack();
        } else if (adapter == EditDomain.class) {
            return this.getEditDomain();
        } else if (adapter == ActionRegistry.class) {
            return this.getActionRegistry();
        } else if (adapter == IPropertySheetPage.class) {
            return this.getPropertySheetPage();
        } else if (adapter == IContentOutlinePage.class) {
            return this.getOverviewOutlinePage();
        } else if (adapter == ZoomManager.class) {
            return getGraphicalViewer().getProperty(
                    ZoomManager.class.toString());
        }

        if (adapter == WorkflowEditorAdapter.class) {
            // hackaround to deliver the wfm to the navigator
            return new WorkflowEditorAdapter(m_manager);
        }

        // the super implementation handles the rest
        return super.getAdapter(adapter);
    }

    /**
     * Sets the snap functionality.
     */
    private void loadProperties() {
        // Snap to Geometry property
        GraphicalViewer graphicalViewer = getGraphicalViewer();
        graphicalViewer.setProperty(SnapToGeometry.PROPERTY_SNAP_ENABLED,
                new Boolean(true));

        // Grid properties
        graphicalViewer.setProperty(SnapToGrid.PROPERTY_GRID_ENABLED,
                new Boolean(false));
        // Grid properties
        graphicalViewer.setProperty(SnapToGrid.PROPERTY_GRID_SPACING,
                new Dimension(6, 6));
        // We keep grid visibility and enablement in sync
        graphicalViewer.setProperty(SnapToGrid.PROPERTY_GRID_VISIBLE,
                new Boolean(false));
    }

    /** Sets background color according to edit mode (see
     * {@link WorkflowManager#isWriteProtected()}. */
    private void updateEditorBackgroundColor() {
        GraphicalViewer gv = getGraphicalViewer();
        Control control = gv.getControl();
        if (m_manager.isWriteProtected()) {
            control.setBackground(BG_COLOR_WRITE_LOCK);
        } else {
            control.setBackground(BG_COLOR_DEFAULT);
        }
    }

    /**
     * Whether the dialog has passed the {@link #promptToSaveOnClose()} method.
     * This helps us to distinguish whether we have to offer an exit dialog when
     * executing nodes are not saveable. This flag is only true if the user
     * closes the editor.
     */
    private boolean m_isClosing;

    /**
     * Brings up the Save-Dialog and sets the m_isClosing flag. {@inheritDoc}
     */
    @Override
    public int promptToSaveOnClose() {
        /*
         * Ideally we would just set the m_isClosing flag and return
         * ISaveablePart2.DEFAULT which will bring up a separate dialog. This
         * does not work as we have to set the m_isClosing only if the user
         * presses YES (no means to figure out what button was pressed when
         * eclipse opens the dialog).
         */
        if (m_parentEditor != null) {
            // ignore closing meta node editors.
            return ISaveablePart2.NO;
        }
        String message =
                NLS.bind(WorkbenchMessages.EditorManager_saveChangesQuestion,
                        getTitle());
        // Show a dialog.
        Shell sh = Display.getDefault().getActiveShell();
        String[] buttons =
                new String[]{IDialogConstants.YES_LABEL,
                        IDialogConstants.NO_LABEL,
                        IDialogConstants.CANCEL_LABEL};
        MessageDialog d =
                new MessageDialog(sh, WorkbenchMessages.Save_Resource, null,
                        message, MessageDialog.QUESTION, buttons, 0);
        switch (d.open()) { // returns index in buttons[] array
        case 0: // YES
            m_isClosing = true;
            return ISaveablePart2.YES;
        case 1: // NO
            return ISaveablePart2.NO;
        default: // CANCEL button or window 'x'
            return ISaveablePart2.CANCEL;
        }
    }

    /** {@inheritDoc} */
    @Override
    public void doSave(final IProgressMonitor monitor) {
        LOGGER.debug("Saving workflow ...");

        // Exception messages from the inner thread
        final StringBuffer exceptionMessage = new StringBuffer();

        if (m_fileResource == null && m_parentEditor != null) {
            m_parentEditor.doSave(monitor);
            m_isDirty = false;
            Display.getDefault().asyncExec(new Runnable() {
                @Override
                public void run() {
                    firePropertyChange(IEditorPart.PROP_DIRTY);
                }
            });
            return;
        }

        // to be sure to mark dirty and inform the user about running nodes
        // we ask for the state BEFORE saving
        // this flag is evaluated at the end of this method
        boolean wasInProgress = false;
        try {
            final File file = new File(new File(m_fileResource),
                    WorkflowPersistor.WORKFLOW_FILE);

            // If something fails an empty workflow is created
            // except when cancellation occurred
            IWorkbench wb = PlatformUI.getWorkbench();
            IProgressService ps = wb.getProgressService();
            SaveWorkflowRunnable saveWorflowRunnable =
                    new SaveWorkflowRunnable(this, file, exceptionMessage,
                            monitor);

            State state = m_manager.getState();
            wasInProgress =
                    state.executionInProgress()
                            && !state.equals(State.EXECUTINGREMOTELY);

            ps.run(true, false, saveWorflowRunnable);
            // after saving the workflow, check for the import marker
            // and delete it

            // mark command stack (no undo beyond this point)
            getCommandStack().markSaveLocation();

        } catch (Exception e) {
            LOGGER.error("Could not save workflow: " + exceptionMessage, e);

            // inform the user
            if (exceptionMessage.toString().trim().length() > 0) {
                showInfoMessage("Workflow could not be saved ...",
                        exceptionMessage.toString());
            }

            throw new OperationCanceledException("Workflow was not saved: "
                    + exceptionMessage.toString());
        }

        Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run() {
                try {
                    IResource r =
                            KnimeResourceUtil.getResourceForURI(m_fileResource);
                    if (r != null) {
                        String pName = r.getProject().getName();
                        monitor.setTaskName("Refreshing " + pName + "...");
                        r.getProject().refreshLocal(IResource.DEPTH_INFINITE,
                                monitor);
                    }
                } catch (CoreException ce) {
                    OperationCanceledException oce =
                            new OperationCanceledException(
                                    "Workflow was not saved: " + ce.toString());
                    oce.initCause(ce);
                    throw oce;
                }
            }
        });

        // mark all sub editors as saved
        for (IEditorPart subEditor : getSubEditors()) {
            final WorkflowEditor editor = (WorkflowEditor)subEditor;
            ((WorkflowEditor)subEditor).setIsDirty(false);
            Display.getDefault().asyncExec(new Runnable() {
                @Override
                public void run() {
                    editor.firePropertyChange(IEditorPart.PROP_DIRTY);
                }
            });
        }

        monitor.done();

        // bugfix 799 (partly)
        // check if the workflow manager is in execution
        // this happens if the user pressed "Yes" on save confirmation dialog
        // or simply saves (Ctrl+S)
        if (wasInProgress) {
            markDirty();
            final Pointer<Boolean> abortPointer = new Pointer<Boolean>();
            abortPointer.set(Boolean.FALSE);
            Display.getDefault().syncExec(new Runnable() {
                @Override
                public void run() {
                    boolean abort = false;
                    Shell sh = Display.getDefault().getActiveShell();
                    String title = "Workflow in execution";
                    String message = "Executing nodes are not saved!";
                    if (m_isClosing) {
                        abort =
                                !MessageDialog.openQuestion(sh, title, message
                                        + " Exit anyway?");
                        m_isClosing = !abort; // user canceled close
                    } else {
                        IPreferenceStore prefStore =
                                KNIMEUIPlugin.getDefault().getPreferenceStore();
                        String toogleMessage = "Don't warn me again";
                        if (prefStore.getBoolean(
                                PreferenceConstants.P_CONFIRM_EXEC_NODES_NOT_SAVED)) {
                            MessageDialogWithToggle
                                    .openInformation(
                                            sh,
                                            title,
                                            message,
                                            toogleMessage,
                                            false,
                                            prefStore,
                                            PreferenceConstants.P_CONFIRM_EXEC_NODES_NOT_SAVED);
                        }
                    }
                    abortPointer.set(Boolean.valueOf(abort));
                }
            });
            if (abortPointer.get()) {
                throw new OperationCanceledException(
                        "Closing workflow canceled on user request.");
            }
        }
    }

    /**
     * Shows a simple information message.
     *
     * @param message the info message to display
     */
    private void showInfoMessage(final String header, final String message) {
        // inform the user

        MessageBox mb =
                new MessageBox(this.getSite().getShell(), SWT.ICON_INFORMATION
                        | SWT.OK);
        mb.setText(header);
        mb.setMessage(message);
        mb.open();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void doSaveAs() {
        throw new UnsupportedOperationException("saveAs not implemented");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isDirty() {
        // if we are a subworkflow editor we are never dirty
        if (m_parentEditor != null) {
            return m_parentEditor.isDirty();
        }
        return m_isDirty;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isSaveAsAllowed() {
        return false;
    }

    /**
     * Notifies property listeners on the editor about changes (e.g. dirty state
     * has changed). Updates the available actions afterwards
     *
     * @see org.eclipse.ui.part.WorkbenchPart#firePropertyChange(int)
     */
    @Override
    protected void firePropertyChange(final int property) {

        super.firePropertyChange(property);

        // updates the editor actions
        updateActions(m_editorActions);
    }

    /**
     * Called when the editors selection has changed. Updates the list of
     * available actions for the new selection in the editor.
     *
     * @see org.eclipse.ui.ISelectionListener#selectionChanged
     *      (org.eclipse.ui.IWorkbenchPart,
     *      org.eclipse.jface.viewers.ISelection)
     */
    @Override
    public void selectionChanged(final IWorkbenchPart part,
            final ISelection selection) {
        // update available actions
        updateActions();
    }

    /**
     * Called when the command stack has changed, that is, a GEF command was
     * executed (Add,Remove,....). This keeps track of the dirty state of the
     * editor. {@inheritDoc}
     */
    @Override
    public void commandStackChanged(final EventObject event) {

        // update the actions (should enable undo/redo accordingly)
        updateActions(m_editorActions);

        // track the dirty state of the edit domain
        boolean b = m_editDomain.getCommandStack().isDirty();
        if (b || getWorkflowManager().isDirty()) {
            markDirty();
        } else {
            m_isDirty = false;
            firePropertyChange(IEditorPart.PROP_DIRTY);
        }
    }

    /*
     * --------- methods for adding a auto-placed and auto-connected node -----
     */

    /**
     * {@inheritDoc}
     * Listener interface method of the {@link NodeProvider}.
     * Called when other instances want to add a meta node to the workflow in
     * the editor. <br>
     * The implementation only adds it if the editor is active. If one other
     * node is selected in the editor, to which the new node will then be
     * connected to.
     *
     * @param sourceManager wfm to copy the meta node from
     * @param id the id of the meta node in the source manager
     * @return if the meta node was actually added
     */
    @Override
    public boolean addMetaNode(final WorkflowManager sourceManager,
            final NodeID id) {
        if (id == null || sourceManager == null) {
            return false;
        }
        if (!isEditorActive()) {
            return false;
        }

        NodeContainerEditPart preNode = getTheOneSelectedNode();
        NodeID preID = null;
        Point nodeLoc = null;
        if (preNode == null) {
            nodeLoc = getViewportCenterLocation();
            nodeLoc = toAbsolute(nodeLoc);
        } else {
            nodeLoc = getLocationRightOf(preNode);
            preID = preNode.getNodeContainer().getID();
        }
        Command newNodeCmd =
                new CreateNewConnectedMetaNodeCommand(getViewer(), m_manager,
                        sourceManager, id, nodeLoc, preID);
        getCommandStack().execute(newNodeCmd);
        // after adding a node the editor should get the focus
        setFocus();
        return true;
    }

    /**
     * Listener interface method of the {@link NodeProvider}. Called when other
     * instances want to add a node to the workflow in the editor. <br>
     * The implementation only adds it if the editor is active and one node is
     * selected in the editor, to which the new node will then be connected to.
     * {@inheritDoc}
     */
    @Override
    public boolean addNode(final NodeFactory<? extends NodeModel> nodeFactory) {

        if (!isEditorActive()) {
            return false;
        }

        NodeContainerEditPart preNode = getTheOneSelectedNode();
        Point nodeLoc = null;
        Command newNodeCmd = null;
        if (preNode == null) {
            nodeLoc = getViewportCenterLocation();
            // this command accepts/requires relative coordinates
            newNodeCmd = new CreateNodeCommand(m_manager, nodeFactory, nodeLoc);
        } else {
            nodeLoc = getLocationRightOf(preNode);
            newNodeCmd =
                    new CreateNewConnectedNodeCommand(getViewer(), m_manager,
                            nodeFactory, nodeLoc, preNode.getNodeContainer()
                                    .getID());
        }

        getCommandStack().execute(newNodeCmd);
        // after adding a node the editor should get the focus
        setFocus();
        return true;
    }

    private boolean isEditorActive() {
        // find out if we are the active editor (any easier way than that???)
        IWorkbenchWindow window =
                PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        if (window == null) {
            return false;
        }
        IWorkbenchPage page = window.getActivePage();
        if (page == null) {
            return false;
        }
        IEditorPart editor = page.getActiveEditor();
        return editor == this;
    }

    /**
     * @return a node if and only if exactly one node in the editor is selected
     */
    private NodeContainerEditPart getTheOneSelectedNode() {
        IStructuredSelection edSel =
                (IStructuredSelection)getSite().getSelectionProvider()
                        .getSelection();
        if (edSel.size() == 1) {
            Object o = edSel.getFirstElement();
            if (o instanceof NodeContainerEditPart) {
                // the one and only node that is selected
                return (NodeContainerEditPart)o;
            }
        }
        return null;
    }

    /**
     * @return a location in the middle of the visible part of the editor. These
     *         are relative coordinates (to the current scrolling position)
     */
    private Point getViewportCenterLocation() {
        FigureCanvas ctrl = ((FigureCanvas)getViewer().getControl());
        Viewport viewPort = ctrl.getViewport();
        Dimension viewSize = viewPort.getSize();
        int relX = viewSize.width / 2;
        int relY = viewSize.height / 2;
        Point nodeLoc = new Point(relX, relY);
        // make sure we have a free spot
        while (isNodeAtRel(nodeLoc)) {
            // move it a bit
            nodeLoc.x += 10;
            nodeLoc.y += 10;
        }
        return nodeLoc;


    }

    private Point getLocationRightOf(final NodeContainerEditPart refNode) {
        NodeUIInformation ui =
                refNode.getNodeContainer()
                .getUIInformation();
        int xOffset = 100;
        int yOffset = 120;
        // first try: right of reference node
        Point loc = new Point(ui.getBounds()[0] + (ui.getBounds()[2] / 2) + xOffset,
                ui.getBounds()[1]);

        // make sure we have a free spot
        while (isNodeAtAbs(loc)) {
            // move it down a bit
            loc.y += yOffset;
        }
        return loc;
    }

    private boolean isNodeAtAbs(final Point absoluteLoc) {
        return isNodeAtRel(toRelative(absoluteLoc));

    }

    private Point toRelative(final Point absLoc) {
        ScalableFreeformRootEditPart rootEditPart
            = (ScalableFreeformRootEditPart) getViewer().getRootEditPart();
        Viewport viewport = (Viewport) rootEditPart.getFigure();
        Rectangle area = viewport.getClientArea();
        Point loc = absLoc.getCopy();
        double z = getZoomfactor();
        loc.x = (int)Math.round((loc.x - area.x) * z);
        loc.y = (int)Math.round((loc.y - area.y) * z);
        return loc;
    }

    private Point toAbsolute(final Point relLoc) {
        ScalableFreeformRootEditPart rootEditPart
        = (ScalableFreeformRootEditPart) getViewer().getRootEditPart();
        Viewport viewport = (Viewport) rootEditPart.getFigure();
        Rectangle area = viewport.getClientArea();

        Point loc = relLoc.getCopy();
        loc.x += area.x;
        loc.y += area.y;
        return loc;
    }


    private double getZoomfactor() {
        ZoomManager zoomManager =
                (ZoomManager)(getViewer().getProperty(ZoomManager.class
                        .toString()));
        return zoomManager.getZoom();
    }

    private boolean isNodeAtRel(final Point relativeLoc) {
        EditPart ep = getViewer().findObjectAt(relativeLoc);
        if (ep == null) {
            return false;
        }
        while (!(ep instanceof RootEditPart)) {
            if (ep instanceof NodeContainerEditPart) {
                return true;
            }
            EditPart parent = ep.getParent();
            // avoid endless loops
            if (parent == null || ep == parent) {
                return false;
            }
            ep = parent;
        }
        return false;
    }

    /*
     * ---------- end of auto-placing and auto-connecting --------------
     */

    /**
     * Listener callback, listens to workflow events and triggers UI updates.
     *
     * {@inheritDoc}
     */
    @Override
    public void workflowChanged(final WorkflowEvent event) {
        LOGGER.debug("Workflow event triggered: " + event.toString());
        SyncExecQueueDispatcher.asyncExec(new Runnable() {
            @Override
            public void run() {
                markDirty();
                updateActions();
            }
        });

    }

    /** {@inheritDoc} */
    @Override
    public void nodePropertyChanged(final NodePropertyChangedEvent e) {
        switch (e.getProperty()) {
        case Name:
            updatePartName();
            break;
        case TemplateConnection:
            updateEditorBackgroundColor();
            break;
        default:
            // ignore
        }
    }

    /**
     * Marks this editor as dirty and notifies the registered listeners.
     */
    public void markDirty() {
        if (!m_isDirty) {
            m_isDirty = true;
            m_manager.setDirty();

            SyncExecQueueDispatcher.asyncExec(new Runnable() {
                @Override
                public void run() {
                    firePropertyChange(IEditorPart.PROP_DIRTY);
                }
            });
            if (m_parentEditor != null) {
                m_parentEditor.markDirty();
            }
        }
    }

    /**
     * we need to listen for resource changes to get informed if the currently
     * opened file in the navigator is renamed or deleted.
     *
     * {@inheritDoc}
     */
    @Override
    public void resourceChanged(final IResourceChangeEvent event) {
        try {
            if (event == null || event.getDelta() == null) {
                return;
            }
            event.getDelta().accept(new MyResourceDeltaVisitor());
        } catch (CoreException e) {
            LOGGER.error(e.getMessage(), e);
        }

    }

    /**
     * Simple visitor, checks whether the currently opened file has been renamed
     * and sets the new name in the editors' tab.
     */
    private class MyResourceDeltaVisitor implements IResourceDeltaVisitor {

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean visit(final IResourceDelta delta) throws CoreException {
            if (!m_fileResource.equals(delta.getResource().getLocationURI())) {
                // doesn't concern us
                return true;
            }
            if ((delta.getFlags() & IResourceDelta.MOVED_TO) != 0) {
                // remove workflow.knime from moved to path
                IPath newDirPath = delta.getMovedToPath();
                // directory name (without workflow groups)
                final String newName = newDirPath.lastSegment();
                WorkflowEditor.this.m_manager.renameWorkflowDirectory(newName);
                URI newDirURI =
                        ResourcesPlugin.getWorkspace().getRoot()
                                .findMember(newDirPath).getLocationURI();
                ProjectWorkflowMap.replace(newDirURI,
                        WorkflowEditor.this.m_manager, m_fileResource);
                Display.getDefault().syncExec(new Runnable() {
                    @Override
                    public void run() {
                        String newTitle =
                                m_manager.getID().getIDWithoutRoot() + ": "
                                        + newName;
                        setTitleToolTip(newTitle);
                        setPartName(newTitle);
                        m_fileResource =
                                ResourcesPlugin.getWorkspace().getRoot()
                                        .getFile(delta.getMovedToPath())
                                        .getLocationURI();
                        WorkflowEditor.super.setInput(new FileEditorInput(
                                ResourcesPlugin.getWorkspace().getRoot()
                                        .getFile(delta.getMovedToPath())));
                    }
                });
                /* false = don't visit children */
                return false;
            }
            if ((delta.getKind() == IResourceDelta.REMOVED)) {
                LOGGER.info(new Path(m_fileResource.getPath()).lastSegment()
                        + " resource has been removed.");

                // close the editor
                Display.getDefault().syncExec(new Runnable() {
                    @Override
                    public void run() {
                        getEditorSite().getPage().closeEditor(
                                WorkflowEditor.this, false);
                    }
                });
                /* false = don't visit children */
                return false;
            }

            return true;
        }
    }

    /**
     * Sets the underlying workflow manager for this editor.
     *
     * @param manager the workflow manager to set
     */
    void setWorkflowManager(final WorkflowManager manager) {
        m_manager = manager;
    }

    /**
     * @return returns true if this editor has been closed on the workbench
     */
    public boolean isClosed() {
        return m_closed;
    }

    /**
     * Transposes a point according to the given zoom manager.
     *
     * @param zoomManager the zoom manager providing the zoom levels
     * @param pointToAdapt the point to adapt
     */
    public static void transposeZoom(final ZoomManager zoomManager,
            final Point pointToAdapt, final boolean adaptViewPortLocation) {

        double zoomLevel = zoomManager.getZoom();

        // adapt the location accordint to the zoom level
        pointToAdapt.x = (int)(pointToAdapt.x * zoomLevel);
        pointToAdapt.y = (int)(pointToAdapt.y * zoomLevel);

        if (adaptViewPortLocation) {
            Point viewPortLocation =
                    zoomManager.getViewport().getViewLocation();
            pointToAdapt.x -= viewPortLocation.x;
            pointToAdapt.y -= viewPortLocation.y;
        }
    }

    /**
     * Adapts a point according to the given zoom manager.
     *
     * @param zoomManager the zoom manager providing the zoom levels
     * @param pointToAdapt the point to adapt
     */
    public static void adaptZoom(final ZoomManager zoomManager,
            final Point pointToAdapt, final boolean adaptViewPortLocation) {

        if (adaptViewPortLocation) {
            Point viewPortLocation =
                    zoomManager.getViewport().getViewLocation();
            pointToAdapt.x += viewPortLocation.x;
            pointToAdapt.y += viewPortLocation.y;
        }
        double zoomLevel = zoomManager.getZoom();

        // adapt the location according to the zoom level
        pointToAdapt.x = (int)(pointToAdapt.x * (1.0 / zoomLevel));
        pointToAdapt.y = (int)(pointToAdapt.y * (1.0 / zoomLevel));
    }

    /**
     * Adapts a precission point according to the given zoom manager.
     *
     * @param zoomManager the zoom manager providing the zoom levels
     * @param pointToAdapt the point to adapt
     */
    public static void adaptZoom(final ZoomManager zoomManager,
            final PrecisionPoint pointToAdapt,
            final boolean adaptViewPortLocation) {

        if (adaptViewPortLocation) {
            Point viewPortLocation =
                    zoomManager.getViewport().getViewLocation();
            pointToAdapt.x += viewPortLocation.x;
            pointToAdapt.y += viewPortLocation.y;
        }
        double zoomLevel = zoomManager.getZoom();

        // adapt the location accordint to the zoom level
        pointToAdapt.preciseX = (pointToAdapt.x * (1.0 / zoomLevel));
        pointToAdapt.preciseY = (pointToAdapt.y * (1.0 / zoomLevel));
    }

    /**
     * Set if the workflow loading process. Should only be invoked during
     * workflow loading.
     *
     * @param dirty whether the editor should be marked as dirty or not
     * @see LoadWorkflowRunnable
     */
    void setIsDirty(final boolean dirty) {
        m_isDirty = dirty;
    }

    public WorkflowSelectionTool getSelectionTool() {
        return m_selectionTool;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void stateChanged(final NodeStateEvent state) {
        markDirty();
    }

    /**
     * UI information listener only relevant if this editor is for a meta node
     * (update part name). {@inheritDoc}
     */
    @Override
    public void nodeUIInformationChanged(final NodeUIInformationEvent evt) {
        updatePartName();
    }

}
