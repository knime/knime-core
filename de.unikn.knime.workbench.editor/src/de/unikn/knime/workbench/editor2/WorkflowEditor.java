/* @(#)$RCSfile$ 
 * $Revision$ $Date$ $Author$
 * 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 * 
 * Copyright, 2003 - 2006
 * Universitaet Konstanz, Germany.
 * Lehrstuhl fuer Angewandte Informatik
 * Prof. Dr. Michael R. Berthold
 * 
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner.
 * -------------------------------------------------------------------
 * 
 */
package de.unikn.knime.workbench.editor2;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.HashSet;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.gef.DefaultEditDomain;
import org.eclipse.gef.EditDomain;
import org.eclipse.gef.EditPartViewer;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.KeyHandler;
import org.eclipse.gef.KeyStroke;
import org.eclipse.gef.RootEditPart;
import org.eclipse.gef.commands.CommandStack;
import org.eclipse.gef.commands.CommandStackListener;
import org.eclipse.gef.editparts.ScalableFreeformRootEditPart;
import org.eclipse.gef.editparts.ZoomManager;
import org.eclipse.gef.tools.SelectionTool;
import org.eclipse.gef.ui.actions.ActionRegistry;
import org.eclipse.gef.ui.actions.GEFActionConstants;
import org.eclipse.gef.ui.actions.PrintAction;
import org.eclipse.gef.ui.actions.RedoAction;
import org.eclipse.gef.ui.actions.SaveAction;
import org.eclipse.gef.ui.actions.StackAction;
import org.eclipse.gef.ui.actions.UndoAction;
import org.eclipse.gef.ui.actions.WorkbenchPartAction;
import org.eclipse.gef.ui.parts.GraphicalEditor;
import org.eclipse.gef.ui.parts.GraphicalViewerKeyHandler;
import org.eclipse.gef.ui.properties.UndoablePropertySheetEntry;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.internal.help.WorkbenchHelpSystem;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import org.eclipse.ui.views.properties.IPropertySheetPage;
import org.eclipse.ui.views.properties.PropertySheetPage;

import de.unikn.knime.core.node.KNIMEConstants;
import de.unikn.knime.core.node.NodeLogger;
import de.unikn.knime.core.node.NodeSettings;
import de.unikn.knime.core.node.NodeLogger.LEVEL;
import de.unikn.knime.core.node.meta.MetaNodeContainer;
import de.unikn.knime.core.node.workflow.WorkflowEvent;
import de.unikn.knime.core.node.workflow.WorkflowListener;
import de.unikn.knime.core.node.workflow.WorkflowManager;
import de.unikn.knime.workbench.editor2.actions.AbstractNodeAction;
import de.unikn.knime.workbench.editor2.actions.CopyAction;
import de.unikn.knime.workbench.editor2.actions.CutAction;
import de.unikn.knime.workbench.editor2.actions.ExecuteAction;
import de.unikn.knime.workbench.editor2.actions.ExecuteAndOpenViewAction;
import de.unikn.knime.workbench.editor2.actions.NodeConnectionContainerDeleteAction;
import de.unikn.knime.workbench.editor2.actions.OpenDialogAction;
import de.unikn.knime.workbench.editor2.actions.PasteAction;
import de.unikn.knime.workbench.editor2.actions.ResetAction;
import de.unikn.knime.workbench.repository.RepositoryManager;
import de.unikn.knime.workbench.ui.KNIMEUIPlugin;
import de.unikn.knime.workbench.ui.preferences.PreferenceConstants;

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
        IResourceChangeListener {

    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(WorkflowEditor.class);

    private static final String EDITOR_ROOT_NAME = "knime";

    public static final String CLIPBOARD_ROOT_NAME = "clipboard";

    /**
     * The static clipboard for copy/cut/paste.
     */
    private static ClipboardObject m_clipboard;

    /** root model object (=editor input) that is handled by the editor. * */
    private WorkflowManager m_manager;

    /** The main graphical viewer embeded in this editor * */
    // private GraphicalViewer m_graphicalViewer;
    /** the editor's action registry. */
    private ActionRegistry m_actionRegistry;

    /** the <code>EditDomain</code>. */
    private DefaultEditDomain m_editDomain;

    /** the dirty state. */
    private boolean m_isDirty;

    /** List with the action ids that are associated to this editor. * */
    private List<String> m_editorActions;

    private GraphicalViewer m_graphicalViewer;

    // private File m_file;
    private IFile m_fileResource;

    private NewOverviewOutlinePage m_overviewOutlinePage;

    private PropertySheetPage m_undoablePropertySheetPage;

    /**
     * Keeps all meta workflow editors which were opend from this editor.
     */
    private HashSet<MetaWorkflowEditor> m_childEditors;

    /**
     * Indicates if this editor has been closed.
     */
    private boolean m_closed;

    /**
     * Keeps list of <code>ConsoleViewAppender</code>. TODO FIXME remove
     * static if you want to have a console for each Workbench
     */
    private static final ArrayList<ConsoleViewAppender> APPENDERS = 
        new ArrayList<ConsoleViewAppender>();

    static {
        IPreferenceStore pStore = KNIMEUIPlugin.getDefault()
                .getPreferenceStore();
        String logLevel = pStore
                .getString(PreferenceConstants.P_LOGLEVEL_CONSOLE);
        setLogLevel(logLevel);
        // Level: warn
        try {
            ConsoleViewAppender.WARN_APPENDER
                    .write(KNIMEConstants.WELCOME_MESSAGE);
            ConsoleViewAppender.WARN_APPENDER.write("Log file is located at: "
                    + KNIMEConstants.KNIME_HOME_DIR + File.separator
                    + NodeLogger.LOG_FILE + "\n");
        } catch (IOException ioe) {
            LOGGER.error("Could not print welcome message: ", ioe);
        }
        pStore.addPropertyChangeListener(new IPropertyChangeListener() {
            public void propertyChange(final PropertyChangeEvent event) {
                if (event.getProperty().equals(
                        PreferenceConstants.P_LOGLEVEL_CONSOLE)) {
                    String newName = event.getNewValue().toString();
                    setLogLevel(newName);
                } else if (event.getProperty().equals(
                        PreferenceConstants.P_LOGLEVEL_LOG_FILE)) {
                    String newName = event.getNewValue().toString();
                    LEVEL l = LEVEL.WARN;
                    try {
                        l = LEVEL.valueOf(newName);
                    } catch (NullPointerException ne) {
                        LOGGER.warn("Null is an invalid log level, using WARN");
                    } catch (IllegalArgumentException iae) {
                        LOGGER.warn("Invalid log level " + newName
                                + ", using WARN");
                    }
                    NodeLogger.setLevelIntern(l);
                }
            };
        });
    }

    /**
     * Register the appenders according to logLevel, i.e.
     * PreferenceConstants.P_LOGLEVEL_DEBUG,
     * PreferenceConstants.P_LOGLEVEL_INFO, etc.
     * 
     * @param logLevel The new log level.
     */
    private static void setLogLevel(final String logLevel) {
        boolean changed = false;
        if (logLevel.equals(PreferenceConstants.P_LOGLEVEL_DEBUG)) {
            changed |= addAppender(ConsoleViewAppender.DEBUG_APPENDER);
            changed |= addAppender(ConsoleViewAppender.INFO_APPENDER);
            changed |= addAppender(ConsoleViewAppender.WARN_APPENDER);
            changed |= addAppender(ConsoleViewAppender.ERROR_APPENDER);
            changed |= addAppender(ConsoleViewAppender.FATAL_ERROR_APPENDER);
        } else if (logLevel.equals(PreferenceConstants.P_LOGLEVEL_INFO)) {
            changed |= removeAppender(ConsoleViewAppender.DEBUG_APPENDER);
            changed |= addAppender(ConsoleViewAppender.INFO_APPENDER);
            changed |= addAppender(ConsoleViewAppender.WARN_APPENDER);
            changed |= addAppender(ConsoleViewAppender.ERROR_APPENDER);
            changed |= addAppender(ConsoleViewAppender.FATAL_ERROR_APPENDER);
        } else if (logLevel.equals(PreferenceConstants.P_LOGLEVEL_WARN)) {
            changed |= removeAppender(ConsoleViewAppender.DEBUG_APPENDER);
            changed |= removeAppender(ConsoleViewAppender.INFO_APPENDER);
            changed |= addAppender(ConsoleViewAppender.WARN_APPENDER);
            changed |= addAppender(ConsoleViewAppender.ERROR_APPENDER);
            changed |= addAppender(ConsoleViewAppender.FATAL_ERROR_APPENDER);
        } else if (logLevel.equals(PreferenceConstants.P_LOGLEVEL_ERROR)) {
            changed |= removeAppender(ConsoleViewAppender.DEBUG_APPENDER);
            changed |= removeAppender(ConsoleViewAppender.INFO_APPENDER);
            changed |= removeAppender(ConsoleViewAppender.WARN_APPENDER);
            changed |= addAppender(ConsoleViewAppender.ERROR_APPENDER);
            changed |= addAppender(ConsoleViewAppender.FATAL_ERROR_APPENDER);
        } else {
            LOGGER.warn("Invalid log level " + logLevel + "; setting to "
                    + PreferenceConstants.P_LOGLEVEL_WARN);
            setLogLevel(PreferenceConstants.P_LOGLEVEL_WARN);
        }
        if (changed) {
            LOGGER.info("Setting console log level to " + logLevel);
        }
    }

    /**
     * No arg constructor, creates the edit domain for this editor.
     */
    public WorkflowEditor() {
        super();

        LOGGER.debug("Creating WorkflowEditor...");

        m_childEditors = new HashSet<MetaWorkflowEditor>();
        m_closed = false;

        // create an edit domain for this editor (handles the command stack)
        m_editDomain = new DefaultEditDomain(this);
        m_editDomain.setDefaultTool(new SelectionTool());

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
     * Add the given Appender to the NodeLogger.
     * @param app Appender to add.
     * @return If the given appender was not previously registered.
     */
    static boolean addAppender(final ConsoleViewAppender app) {
        if (!APPENDERS.contains(app)) {
            NodeLogger.addWriter(app, app.getLevel(), app.getLevel());
            APPENDERS.add(app);
            return true;
        }
        return false;
    }

    /**
     * Removes the given Appender from the NodeLogger.
     * 
     * @param app Appender to remove.
     * @return If the given appended was previously registered.
     */
    static boolean removeAppender(final ConsoleViewAppender app) {
        if (APPENDERS.contains(app)) {
            NodeLogger.removeWriter(app);
            APPENDERS.remove(app);
            return true;
        }
        return false;
    }

    /**
     * Returns the clipboard content for this editor.
     * 
     * @return the clipboard for this editor
     */
    public ClipboardObject getClipboardContent() {

        return m_clipboard;
        // if (m_clipboard == null) {
        // m_clipboard = new Clipboard(getSite().getShell().getDisplay());
        // }
        //
        // return m_clipboard;
    }

    /**
     * Sets the clipboard content for this editor.
     * 
     * @param content the content to set into the clipboard
     * 
     */
    public void setClipboardContent(final ClipboardObject content) {

        m_clipboard = content;
    }

    /**
     * @see org.eclipse.ui.IEditorPart #init(org.eclipse.ui.IEditorSite,
     *      org.eclipse.ui.IEditorInput)
     */
    public void init(final IEditorSite site, final IEditorInput input)
            throws PartInitException {

        LOGGER.debug("Initializing editor UI...");

        // TODO FIXME: Colors need to be assigned to different debugging levels
        NodeLogger.getLogger(WorkflowEditor.class).debug(
                "Opening workflow Editor on " + input.getName());

        // store site and input
        setSite(site);
        setInput(input);

        // register listener to check wether the underlying knime file
        // has been deleted or renamed
        ResourcesPlugin.getWorkspace().addResourceChangeListener(this,
                IResourceChangeEvent.POST_CHANGE);

        // add this as a CommandStackListener
        getCommandStack().addCommandStackListener(this);

        // add this as a selection change listener
        getSite().getWorkbenchWindow().getSelectionService()
                .addSelectionListener(this);

        // add this editor as a listener to WorkflowEvents
        m_manager.addListener(this);
    }

    /**
     * Deregisters all listeners when the editor is disposed.
     * 
     * @see org.eclipse.ui.IWorkbenchPart#dispose()
     */
    public void dispose() {

        // remember that this editor has been closed
        m_closed = true;

        // first of all close all child editors
        for (MetaWorkflowEditor metaWorkflowEditor : m_childEditors) {
            PlatformUI.getWorkbench().getActiveWorkbenchWindow()
                    .getActivePage().closeEditor(metaWorkflowEditor, false);
        }

        // remove appender listener from "our" NodeLogger
        NodeLogger.getLogger(WorkflowEditor.class).debug("Disposing editor...");
        // remove appender listener from "our" NodeLogger
        for (int i = 0; i < APPENDERS.size(); i++) {
            removeAppender((ConsoleViewAppender)APPENDERS.get(i));
        }

        m_manager.removeListener(this);
        getSite().getWorkbenchWindow().getSelectionService()
                .removeSelectionListener(this);

        // register resource listener..
        m_fileResource.getWorkspace().removeResourceChangeListener(this);

        getCommandStack().removeCommandStackListener(this);

        super.dispose();
    }

    /**
     * Creates the editor actions.
     * 
     * @see org.eclipse.gef.ui.parts.GraphicalEditor#createActions()
     */
    protected void createActions() {
        LOGGER.debug("creating editor actions...");

        // super already does someting for us...
        super.createActions();

        // Stack actions

        StackAction undo = new UndoAction(this);
        StackAction redo = new RedoAction(this);

        // Editor Actions
        WorkbenchPartAction delete = new NodeConnectionContainerDeleteAction(
                (IWorkbenchPart)this);
        WorkbenchPartAction save = new SaveAction(this);
        WorkbenchPartAction print = new PrintAction(this);

        // node actions
        //
        AbstractNodeAction openDialog = new OpenDialogAction(this);

        AbstractNodeAction execute = new ExecuteAction(this);
        AbstractNodeAction executeAndView = new ExecuteAndOpenViewAction(this);
        AbstractNodeAction reset = new ResetAction(this);

        // copy / cut / paste action
        CopyAction copy = new CopyAction(this);
        CutAction cut = new CutAction(this);
        PasteAction paste = new PasteAction(this);

        // register the actions
        m_actionRegistry.registerAction(undo);
        m_actionRegistry.registerAction(redo);
        m_actionRegistry.registerAction(delete);
        m_actionRegistry.registerAction(save);
        m_actionRegistry.registerAction(print);

        m_actionRegistry.registerAction(openDialog);
        m_actionRegistry.registerAction(execute);
        m_actionRegistry.registerAction(executeAndView);
        m_actionRegistry.registerAction(reset);

        m_actionRegistry.registerAction(copy);
        m_actionRegistry.registerAction(cut);
        m_actionRegistry.registerAction(paste);

        // remember ids for later updates via 'updateActions'
        m_editorActions = new ArrayList<String>();
        m_editorActions.add(undo.getId());
        m_editorActions.add(redo.getId());
        m_editorActions.add(delete.getId());
        m_editorActions.add(save.getId());
        m_editorActions.add(print.getId());

        m_editorActions.add(openDialog.getId());
        m_editorActions.add(execute.getId());
        m_editorActions.add(executeAndView.getId());
        m_editorActions.add(reset.getId());

        m_editorActions.add(copy.getId());
        m_editorActions.add(cut.getId());
        m_editorActions.add(paste.getId());

    }

    /**
     * This hooks keys like F2 for editing, delete etc. inside the editor...
     * 
     * @return The common (shared) key handler.
     */
    protected KeyHandler getCommonKeyHandler() {

        KeyHandler sharedKeyHandler = new KeyHandler();

        // Delete
        sharedKeyHandler.put(KeyStroke.getPressed(SWT.DEL, 0),
                getActionRegistry().getAction(ActionFactory.DELETE.getId()));

        // Edit
        sharedKeyHandler.put(KeyStroke.getPressed(SWT.F2, 0),
                getActionRegistry().getAction(GEFActionConstants.DIRECT_EDIT));

        // Open Dialog on CR
        sharedKeyHandler.put(KeyStroke.getPressed(SWT.CR, 0),
                getActionRegistry().getAction(OpenDialogAction.ID));

        return sharedKeyHandler;
    }

    /**
     * Returns the action registry for this editor. It is "lazy" created on
     * first invocation.
     * 
     * @see org.eclipse.gef.ui.parts.GraphicalEditor#getActionRegistry()
     */
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
    protected void createGraphicalViewer(final Composite parent) {
        IEditorSite editorSite = getEditorSite();
        GraphicalViewer viewer = null;
        viewer = new WorkflowGraphicalViewerCreator(editorSite, this
                .getActionRegistry()).createViewer(parent);

        // Configure the key handler
        GraphicalViewerKeyHandler keyHandler = new GraphicalViewerKeyHandler(
                viewer);

        KeyHandler parentKeyHandler = keyHandler
                .setParent(getCommonKeyHandler());
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
                "de.unikn.knime.workbench.help.flow_editor_context");

    }

    /**
     * This does nothing by now, as all is handled by
     * <code>createGraphicalViewer</code>.
     * 
     * @see org.eclipse.gef.ui.parts.GraphicalEditor
     *      #initializeGraphicalViewer()
     */
    protected void initializeGraphicalViewer() {
        // nothing
    }

    /**
     * Configurs the graphical viewer.
     * 
     * @see org.eclipse.gef.ui.parts.GraphicalEditor#configureGraphicalViewer()
     */
    protected void configureGraphicalViewer() {
        super.configureGraphicalViewer();

    }

    /**
     * @return The graphical viewer in this editor
     * @see org.eclipse.gef.ui.parts.GraphicalEditor#getGraphicalViewer()
     */
    protected GraphicalViewer getGraphicalViewer() {
        return m_graphicalViewer;
    }

    /**
     * Sets the editor input, that is, the file that contains the serialized
     * workflow manager.
     * 
     * @see org.eclipse.ui.part.EditorPart#setInput(org.eclipse.ui.IEditorInput)
     */
    protected void setInput(final IEditorInput input) {
        LOGGER.debug("Setting input into editor...");

        setDefaultInput(input);
        // we only support file inputs
        m_fileResource = ((IFileEditorInput)input).getFile();

        // TODO try to load a WFM-config from the file, create an empty one if
        // this fails
        // m_manager = new WorkflowManager(file.getName());
        // LOGGER.debug("Created new WFM object as input");

        // ClassLoader oldLoader =
        // Thread.currentThread().getContextClassLoader();
        File file = m_fileResource.getLocation().toFile();
        try {
            NodeSettings settings = NodeSettings
                    .loadFromXML(new FileInputStream(file));
            // NodeSettings cfg = Config.readFromFile(
            // new ObjectInputStream(new FileInputStream(file)));
            createWorkflowManager(settings);
        } catch (FileNotFoundException fnfe) {
            LOGGER.fatal("File not found", fnfe);
        } catch (IOException ioe) {
            if (file.length() > 0) {
                LOGGER.error("Could not load workflow from: " + file.getName());
            } else {
                LOGGER.debug("File length: " + file.length()
                        + " maybe a new workflow has been created.");
            }
        } finally {
            // create empty WFM if loading failed
            if (m_manager == null) {
                m_manager = new WorkflowManager();
                m_isDirty = false;
            }
        }

        // Editor name (title)
        setPartName(file.getName());

        // update Actions, as now there's everything available
        updateActions();
    }

    /**
     * Sets the input in the super class for defaults.
     * 
     * @param input the editor input object
     */
    void setDefaultInput(final IEditorInput input) {

        super.setInput(input);
    }

    /**
     * Updates the actions of this workflow editor. Can be used from subclassing
     * objects to update the actions.
     */
    public void updateActions() {
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
                m_overviewOutlinePage = new NewOverviewOutlinePage(
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

        // the super implementation handles the rest
        return super.getAdapter(adapter);
    }

    /**
     * @see org.eclipse.ui.part.EditorPart
     *      #doSave(org.eclipse.core.runtime.IProgressMonitor)
     */
    public void doSave(final IProgressMonitor monitor) {

        LOGGER.debug("Saving workflow ....");
        NodeSettings cfg = new NodeSettings(EDITOR_ROOT_NAME);
        m_manager.save(cfg);

        try {

            // make sure the resource is "fresh" before saving...
            //m_fileResource.refreshLocal(IResource.DEPTH_ONE, null);
            File file = m_fileResource.getLocation().toFile();
            FileOutputStream os = new FileOutputStream(file);
            cfg.saveToXML(os);
            // cfg.writeToFile(new ObjectOutputStream(os));

            // mark command stack (no undo beyond this point)
            getCommandStack().markSaveLocation();

        } catch (Exception e) {
            LOGGER.warn("Could not save workflow");
        }

//        try {
//            // try to refresh project
////            m_fileResource.getProject().refreshLocal(IResource.DEPTH_INFINITE,
////                    monitor);
//            
//            //archive attribute aendern
//        } catch (CoreException e) {
//            // TODO Auto-generated catch block
//            LOGGER.debug("", e);
//        }

    }

    /**
     * @see org.eclipse.ui.part.EditorPart#doSaveAs()
     */
    public void doSaveAs() {
        throw new UnsupportedOperationException("saveAs not implemented");
    }

    /**
     * @see org.eclipse.ui.part.EditorPart#isDirty()
     */
    public boolean isDirty() {
        return m_isDirty;
    }

    /**
     * @see org.eclipse.ui.part.EditorPart#isSaveAsAllowed()
     */
    public boolean isSaveAsAllowed() {
        return false;
    }

    /**
     * Notifies property listeners on the editor about changes (e.g. dirty state
     * has changed). Updates the available actions afterwards
     * 
     * @see org.eclipse.ui.part.WorkbenchPart#firePropertyChange(int)
     */
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
    public void selectionChanged(final IWorkbenchPart part,
            final ISelection selection) {

        // update available actions
        updateActions(m_editorActions);
    }

    /**
     * Called when the command stack has changed, that is, a GEF command was
     * executed (Add,Remove,....). This keeps track of the dirty state of the
     * editor.
     * 
     * @see org.eclipse.gef.commands.CommandStackListener
     *      #commandStackChanged(java.util.EventObject)
     */
    public void commandStackChanged(final EventObject event) {

        // update the actions (should enable undo/redo accordingly)
        updateActions(m_editorActions);

        // track the dirty state of the edit domain
        boolean b = m_editDomain.getCommandStack().isDirty();
        if (b != m_isDirty) {
            // If state has changed, notify listeners
            m_isDirty = b;
            firePropertyChange(IEditorPart.PROP_DIRTY);
        }

    }

    /**
     * Listener callback, listens to workflow events and triggers UI updates.
     * 
     * @see de.unikn.knime.core.node.workflow.WorkflowListener
     *      #workflowChanged(de.unikn.knime.core.node.workflow.WorkflowEvent)
     */
    public void workflowChanged(final WorkflowEvent event) {
        LOGGER.debug("Workflow event triggered: " + event.toString());
        if (!m_isDirty) {
            m_isDirty = true;

            Display.getDefault().syncExec(new Runnable() {
                public void run() {
                    firePropertyChange(IEditorPart.PROP_DIRTY);
                }
            });
        }

    }

    /**
     * we need to listen for resource changes to get informed if the currently
     * opened file in the navigator is renamed or deleted.
     * 
     * @see org.eclipse.core.resources.IResourceChangeListener
     *      #resourceChanged(org.eclipse.core.resources.IResourceChangeEvent)
     */
    public void resourceChanged(final IResourceChangeEvent event) {
        try {
            if (event == null || event.getDelta() == null) {
                return;
            }
            event.getDelta().accept(new MyResourceDeltaVisitor());
        } catch (CoreException e) {
            // should never happen, I think...
            e.printStackTrace();
        }

    }

    /**
     * Simple visitor, checks wheter the currently opened file has been renamed
     * and sets the new name in the editors' tab.
     * 
     * @author Florian Georg, University of Konstanz
     */
    private class MyResourceDeltaVisitor implements IResourceDeltaVisitor {

        /**
         * @see org.eclipse.core.resources.IResourceDeltaVisitor
         *      #visit(org.eclipse.core.resources.IResourceDelta)
         */
        public boolean visit(final IResourceDelta delta) throws CoreException {

            // Parent project removed? close this editor....
            if (m_fileResource.getProject().equals(delta.getResource())) {
                if ((delta.getKind() & IResourceDelta.REMOVED) != 0) {
                    // We can't save the workflow here, so unsaved changes are
                    // definitly lost. Well, people deleting projects really
                    // should know what they're doing ;-)
                    Display.getDefault().asyncExec(new Runnable() {
                        public void run() {
                            getEditorSite().getPage().closeEditor(
                                    WorkflowEditor.this, false);
                        }
                    });
                }
            }
            // we're only interested in deltas that are about "our" resource
            if (!m_fileResource.equals(delta.getResource())) {
                return true;
            }

            // listen for "MOVED_TO" deltas...
            if ((delta.getFlags() & IResourceDelta.MOVED_TO) != 0) {

                // This updates UI, so we need the code to be executed in the
                // SWT UI thread.
                Display.getDefault().syncExec(new Runnable() {
                    public void run() {

                        // get the new name
                        String newName = delta.getMovedToPath().lastSegment();
                        // set the part name
                        setPartName(newName);

                        // we need to update the file resource that we have
                        // currently opened in our editor, so that we now have
                        // the "new" file
                        m_fileResource = ResourcesPlugin.getWorkspace()
                                .getRoot().getFile(delta.getMovedToPath());

                    }
                });

                return false;
            } else if ((delta.getKind() == IResourceDelta.REMOVED)) {
                LOGGER.info(m_fileResource.getName()
                        + " resource has been removed.");

                // close the editor
                Display.getDefault().asyncExec(new Runnable() {
                    public void run() {
                        getEditorSite().getPage().closeEditor(
                                WorkflowEditor.this, false);
                    }
                });
            }

            return true;
        }

    }

    /**
     * Creates the underlying <code>WorkflowManager</code> for this editor.
     * Therefore the settings are loaded and the editor registeres itself as
     * listener to get workflow events.
     * 
     * @param settings the settings representing this workflow
     */
    void createWorkflowManager(final NodeSettings settings) {
        m_manager = RepositoryManager.INSTANCE.loadWorkflowFromConfig(settings);
        m_manager.addListener(this);
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
     * Tries to add the given editor.
     * 
     * @param editor the edior to add as a child
     * 
     * @return true if the given editor was not added already
     */
    public boolean addEditor(final MetaWorkflowEditor editor) {
        return m_childEditors.add(editor);
    }

    /**
     * Returns the editor for the given meta node container.
     * 
     * @param metaNodeContainer the meta node container to look up
     * 
     * @return the editor for this meta node container, null if container was
     *         not found
     */
    public MetaWorkflowEditor getEditor(
            final MetaNodeContainer metaNodeContainer) {

        for (MetaWorkflowEditor metaWorkflowEditor : m_childEditors) {

            if (metaWorkflowEditor.representsNodeContainer(metaNodeContainer)) {
                return metaWorkflowEditor;
            }
        }

        // if there was no editor found representing the given meta node
        // container return null
        return null;
    }

    /**
     * @return returns true if this editor has been closed on the workbench
     */
    public boolean isClosed() {
        return m_closed;
    }

    /**
     * Removes the given editor from the child editor set.
     * 
     * @param editor the editor to remove
     */
    public void removeEditor(final IEditorPart editor) {
        m_childEditors.remove(editor);
    }
}
