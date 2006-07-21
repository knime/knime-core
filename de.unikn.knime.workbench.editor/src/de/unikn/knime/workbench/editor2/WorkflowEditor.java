/* @(#)$RCSfile$ 
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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.PrecisionPoint;
import org.eclipse.gef.DefaultEditDomain;
import org.eclipse.gef.EditDomain;
import org.eclipse.gef.EditPartViewer;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.KeyHandler;
import org.eclipse.gef.KeyStroke;
import org.eclipse.gef.RootEditPart;
import org.eclipse.gef.SnapToGeometry;
import org.eclipse.gef.SnapToGrid;
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
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.internal.help.WorkbenchHelpSystem;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.progress.IProgressService;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import org.eclipse.ui.views.properties.IPropertySheetPage;
import org.eclipse.ui.views.properties.PropertySheetPage;

import de.unikn.knime.core.node.CanceledExecutionException;
import de.unikn.knime.core.node.DefaultNodeProgressMonitor;
import de.unikn.knime.core.node.ExecutionMonitor;
import de.unikn.knime.core.node.InvalidSettingsException;
import de.unikn.knime.core.node.KNIMEConstants;
import de.unikn.knime.core.node.NodeLogger;
import de.unikn.knime.core.node.NodeProgressListener;
import de.unikn.knime.core.node.NodeProgressMonitor;
import de.unikn.knime.core.node.NodeLogger.LEVEL;
import de.unikn.knime.core.node.meta.MetaInputModel;
import de.unikn.knime.core.node.meta.MetaOutputModel;
import de.unikn.knime.core.node.workflow.NodeContainer;
import de.unikn.knime.core.node.workflow.WorkflowEvent;
import de.unikn.knime.core.node.workflow.WorkflowInExecutionException;
import de.unikn.knime.core.node.workflow.WorkflowListener;
import de.unikn.knime.core.node.workflow.WorkflowManager;
import de.unikn.knime.core.util.MutableInteger;
import de.unikn.knime.workbench.editor2.actions.AbstractNodeAction;
import de.unikn.knime.workbench.editor2.actions.CancelAllAction;
import de.unikn.knime.workbench.editor2.actions.CopyAction;
import de.unikn.knime.workbench.editor2.actions.CutAction;
import de.unikn.knime.workbench.editor2.actions.ExecuteAction;
import de.unikn.knime.workbench.editor2.actions.ExecuteAllAction;
import de.unikn.knime.workbench.editor2.actions.ExecuteAndOpenViewAction;
import de.unikn.knime.workbench.editor2.actions.NodeConnectionContainerDeleteAction;
import de.unikn.knime.workbench.editor2.actions.OpenDialogAction;
import de.unikn.knime.workbench.editor2.actions.PasteAction;
import de.unikn.knime.workbench.editor2.actions.ResetAction;
import de.unikn.knime.workbench.editor2.actions.SetNameAndDescriptionAction;
import de.unikn.knime.workbench.editor2.actions.job.ProgressMonitorJob;
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

    public static final String CLIPBOARD_ROOT_NAME = "clipboard";

    /**
     * The static clipboard for copy/cut/paste.
     */
    private static ClipboardObject clipboard;

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
    private static final ArrayList<ConsoleViewAppender> APPENDERS = new ArrayList<ConsoleViewAppender>();

    static {
        IPreferenceStore pStore = KNIMEUIPlugin.getDefault()
                .getPreferenceStore();
        String logLevelConsole = pStore
                .getString(PreferenceConstants.P_LOGLEVEL_CONSOLE);
        setLogLevel(logLevelConsole);
        String logLevelFile = pStore
                .getString(PreferenceConstants.P_LOGLEVEL_LOG_FILE);
        LEVEL l = LEVEL.WARN;
        try {
            l = LEVEL.valueOf(logLevelFile);
        } catch (NullPointerException ne) {
            LOGGER.warn("Null is an invalid log level, using WARN");
        } catch (IllegalArgumentException iae) {
            LOGGER.warn("Invalid log level " + logLevelFile + ", using WARN");
        }
        NodeLogger.setLevelIntern(l);
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
        KNIMEConstants.GLOBAL_THREAD_POOL.setMaxThreads(pStore
                .getInt(PreferenceConstants.P_MAXIMUM_THREADS));
        System.setProperty("java.io.tmpdir", pStore
                .getString(PreferenceConstants.P_TEMP_DIR));
        pStore.addPropertyChangeListener(new IPropertyChangeListener() {
            public void propertyChange(final PropertyChangeEvent event) {
                if (event.getProperty().equals(
                        PreferenceConstants.P_LOGLEVEL_CONSOLE)) {
                    String newName = event.getNewValue().toString();
                    setLogLevel(newName);
                } else if (event.getProperty().equals(
                        PreferenceConstants.P_LOGLEVEL_LOG_FILE)) {
                    String newName = event.getNewValue().toString();
                    LEVEL level = LEVEL.WARN;
                    try {
                        level = LEVEL.valueOf(newName);
                    } catch (NullPointerException ne) {
                        LOGGER.warn("Null is an invalid log level, using WARN");
                    } catch (IllegalArgumentException iae) {
                        LOGGER.warn("Invalid log level " + newName
                                + ", using WARN");
                    }
                    NodeLogger.setLevelIntern(level);
                } else if (event.getProperty().equals(
                        PreferenceConstants.P_MAXIMUM_THREADS)) {
                    int count;
                    try {
                        count = (Integer)event.getNewValue();
                        KNIMEConstants.GLOBAL_THREAD_POOL.setMaxThreads(count);
                    } catch (Exception e) {
                        LOGGER.warn("Unable to get maximum thread count "
                                + " from preference page.", e);
                    }
                } else if (event.getProperty().equals(
                        PreferenceConstants.P_TEMP_DIR)) {
                    System.setProperty("java.io.tmpdir", (String)event
                            .getNewValue());
                }
            }
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
     * 
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

        return clipboard;
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

        clipboard = content;
    }

    /**
     * @see org.eclipse.ui.IEditorPart #init(org.eclipse.ui.IEditorSite,
     *      org.eclipse.ui.IEditorInput)
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
    @Override
    public void dispose() {

        // remember that this editor has been closed
        m_closed = true;

        // first of all close all child editors
        for (MetaWorkflowEditor metaWorkflowEditor : m_childEditors) {

            IWorkbenchPage page = PlatformUI.getWorkbench()
                    .getActiveWorkbenchWindow().getActivePage();
            if (page != null) {
                page.closeEditor(metaWorkflowEditor, false);
            }
        }

        m_manager.shutdown();

        // remove appender listener from "our" NodeLogger
        NodeLogger.getLogger(WorkflowEditor.class).debug("Disposing editor...");
        // remove appender listener from "our" NodeLogger
        for (int i = 0; i < APPENDERS.size(); i++) {
            removeAppender(APPENDERS.get(i));
        }

        m_manager.removeListener(this);
        getSite().getWorkbenchWindow().getSelectionService()
                .removeSelectionListener(this);

        // register resource listener..
        if (m_fileResource != null) {
            m_fileResource.getWorkspace().removeResourceChangeListener(this);
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

        // super already does someting for us...
        super.createActions();

        // Stack actions

        StackAction undo = new UndoAction(this);
        StackAction redo = new RedoAction(this);

        // Editor Actions
        WorkbenchPartAction delete = new NodeConnectionContainerDeleteAction(
                this);
        WorkbenchPartAction save = new SaveAction(this);
        WorkbenchPartAction print = new PrintAction(this);

        // node actions
        //
        AbstractNodeAction openDialog = new OpenDialogAction(this);

        AbstractNodeAction execute = new ExecuteAction(this);
        AbstractNodeAction executeAll = new ExecuteAllAction(this);
        AbstractNodeAction cancelAll = new CancelAllAction(this);
        AbstractNodeAction executeAndView = new ExecuteAndOpenViewAction(this);
        AbstractNodeAction reset = new ResetAction(this);
        AbstractNodeAction setNameAndDescription = new SetNameAndDescriptionAction(
                this);

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
        m_actionRegistry.registerAction(executeAll);
        m_actionRegistry.registerAction(cancelAll);
        m_actionRegistry.registerAction(executeAndView);
        m_actionRegistry.registerAction(reset);
        m_actionRegistry.registerAction(setNameAndDescription);

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
        m_editorActions.add(executeAll.getId());
        m_editorActions.add(cancelAll.getId());
        m_editorActions.add(executeAndView.getId());
        m_editorActions.add(reset.getId());
        m_editorActions.add(setNameAndDescription.getId());

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

        loadProperties();

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
     * Configurs the graphical viewer.
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
     * @see org.eclipse.ui.part.EditorPart#setInput(org.eclipse.ui.IEditorInput)
     */
    @Override
    protected void setInput(final IEditorInput input) {
        LOGGER.debug("Setting input into editor...");

        setDefaultInput(input);
        // we only support file inputs
        m_fileResource = ((IFileEditorInput)input).getFile();

        // TODO try to load a WFM-config from the file, create an empty one if
        // this fails
        // LOGGER.debug("Created new WFM object as input");

        LOGGER.debug("Resource File's project: " + m_fileResource.getProject());
        final File file = m_fileResource.getLocation().toFile();
        try {
            // FIXME:
            // setInput is called before the entire repository is loaded,
            // need to figure out how to do it the other way around
            // the static block needs to be executed, access
            // RepositoryManager.INSTANCE
            if (RepositoryManager.INSTANCE == null) {
                LOGGER.fatal("Dummy line, never printed");
            }
            assert m_manager == null;

            // If something fails an empty workflow is created
            // except when cancalation occured
            final MutableInteger createEmptyWorkflow = new MutableInteger(0);

            IWorkbench wb = PlatformUI.getWorkbench();
            IProgressService ps = wb.getProgressService();
            ps.busyCursorWhile(new IRunnableWithProgress() {
                public void run(final IProgressMonitor pm) {
                    try {
                        // create progress monitor
                        ProgressHandler progressHandler = new ProgressHandler(
                                pm, 1);
                        final DefaultNodeProgressMonitor progressMonitor = new DefaultNodeProgressMonitor();
                        progressMonitor.addProgressListener(progressHandler);

                        CheckThread checkThread = new CheckThread(pm,
                                progressMonitor);

                        checkThread.start();

                        pm.beginTask("Load workflow...", 10);
                        pm.subTask("Load nodes...");
                        m_manager = new WorkflowManager(file, progressMonitor);
                        pm.subTask("Finished.");
                        pm.done();
                        checkThread.finished();
                    } catch (FileNotFoundException fnfe) {
                        LOGGER.fatal("File not found", fnfe);
                    } catch (IOException ioe) {
                        if (file.length() == 0) {
                            LOGGER.info("New workflow created.");
                        } else {
                            LOGGER.error("Could not load workflow from: "
                                    + file.getName(), ioe);
                        }
                    } catch (InvalidSettingsException ise) {
                        LOGGER.error("Could not load workflow from: "
                                + file.getName(), ise);
                    } catch (CanceledExecutionException cee) {
                        LOGGER.info("Canceled loading worflow: "
                                + file.getName());
                        m_manager = null;
                        createEmptyWorkflow.setValue(1);
                    } catch (Exception e) {
                        LOGGER.info("Workflow could not be loaded. "
                                + file.getName());
                        m_manager = null;
                    } finally {
                        // create empty WFM if loading failed

                        if (m_manager == null) {
                            // && createEmptyWorkflow.intValue() == 0) {
                            m_manager = new WorkflowManager();
                            m_isDirty = false;
                        }
                    }
                }
            });

            if (createEmptyWorkflow.intValue() == 1) {
                throw new RuntimeException("Opening workflow canceled.");
            }
            
            

            m_manager.addListener(this);
        } catch (InterruptedException ie) {
            LOGGER.fatal("Workflow loading thread interrupted", ie);
        } catch (InvocationTargetException e) {
            LOGGER.fatal("Workflow could not be loaded.", e);
        }

        // Editor name (title)
        setPartName(file.getParentFile().getName());

        if (getGraphicalViewer() != null) {
            loadProperties();
        }

        // update Actions, as now there's everything available
        updateActions();
    }

    private class CheckThread extends Thread {

        private boolean m_finished = false;

        private IProgressMonitor m_pm;

        private DefaultNodeProgressMonitor m_progressMonitor;

        /**
         * Creates a new cancel execution checker.
         * 
         * @param pm the eclipse progress monitor
         * @param progressMonitor the knime progress monitor
         */
        public CheckThread(final IProgressMonitor pm,
                final DefaultNodeProgressMonitor progressMonitor) {
            m_pm = pm;
            m_progressMonitor = progressMonitor;
        }

        /**
         * Sets the finished flag.
         * 
         */
        public void finished() {
            m_finished = true;
        }

        public void run() {

            while (!m_finished) {

                if (m_pm.isCanceled()) {
                    m_progressMonitor.setExecuteCanceled();
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    // nothing to do here
                }
            }
        }
    };

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

        // the super implementation handles the rest
        return super.getAdapter(adapter);
    }

    /**
     * Sets the snap functionality.
     */
    protected void loadProperties() {

        // Ruler properties
        // LogicRuler ruler =
        // getLogicDiagram().getRuler(PositionConstants.WEST);
        // RulerProvider provider = null;
        // if (ruler != null) {
        // provider = new LogicRulerProvider(ruler);
        // }
        // getGraphicalViewer().setProperty(RulerProvider.PROPERTY_VERTICAL_RULER,
        // provider);
        // ruler = getLogicDiagram().getRuler(PositionConstants.NORTH);
        // provider = null;
        // if (ruler != null) {
        // provider = new LogicRulerProvider(ruler);
        // }
        // getGraphicalViewer().setProperty(
        // RulerProvider.PROPERTY_HORIZONTAL_RULER, provider);
        // getGraphicalViewer().setProperty(
        // RulerProvider.PROPERTY_RULER_VISIBILITY,
        // new Boolean(getLogicDiagram().getRulerVisibility()));

        // Snap to Geometry property
        getGraphicalViewer().setProperty(SnapToGeometry.PROPERTY_SNAP_ENABLED,
                new Boolean(true));

        // Grid properties
        getGraphicalViewer().setProperty(SnapToGrid.PROPERTY_GRID_ENABLED,
                new Boolean(false));
        // Grid properties
        getGraphicalViewer().setProperty(SnapToGrid.PROPERTY_GRID_SPACING,
                new Dimension(6, 6));
        // We keep grid visibility and enablement in sync
        getGraphicalViewer().setProperty(SnapToGrid.PROPERTY_GRID_VISIBLE,
                new Boolean(false));

    }

    private class ProgressHandler implements NodeProgressListener {

        private IProgressMonitor m_progressMonitor;

        private int m_totalWork;

        /**
         * Handles progress changes during saving the workflow.
         * 
         * @param monitor the eclipse progressmonitor
         */
        public ProgressHandler(final IProgressMonitor monitor,
                final int totalWork) {
            monitor.beginTask("Saving workflow...", totalWork);
            m_progressMonitor = monitor;
            m_totalWork = totalWork;
        }

        /**
         * @see de.unikn.knime.core.node.NodeProgressListener#
         *      progressChanged(double, java.lang.String)
         */
        public void progressChanged(final double progress, final String message) {

            m_progressMonitor.worked((int)(progress * m_totalWork));
            if (message != null) {
                m_progressMonitor.subTask(message);
            }
        }
    }

    /**
     * @see org.eclipse.ui.part.EditorPart
     *      #doSave(org.eclipse.core.runtime.IProgressMonitor)
     */
    @Override
    public void doSave(final IProgressMonitor monitor) {
        LOGGER.debug("Saving workflow ....");

        // create progress monitor
        ProgressHandler progressHandler = new ProgressHandler(monitor,
                m_manager.getNodes().size());
        DefaultNodeProgressMonitor progressMonitor = new DefaultNodeProgressMonitor();
        progressMonitor.addProgressListener(progressHandler);
        ExecutionMonitor exec = new ExecutionMonitor(progressMonitor);

        try {
            // make sure the resource is "fresh" before saving...
            // m_fileResource.refreshLocal(IResource.DEPTH_ONE, null);
            File file = m_fileResource.getLocation().toFile();
            m_manager.save(file, exec);
            monitor.worked(8);
            // mark command stack (no undo beyond this point)
            getCommandStack().markSaveLocation();

        } catch (CanceledExecutionException cee) {
            LOGGER.debug("Saving of workflow canceled");
        } catch (WorkflowInExecutionException e) {

            // inform the user
            MessageBox mb = new MessageBox(Display.getDefault()
                    .getActiveShell(), SWT.ICON_INFORMATION | SWT.OK);
            mb.setText("Workflow could not be saved ...");
            mb.setMessage("Execution in progress! The workflow could not be "
                    + "saved.");
            mb.open();

            LOGGER.warn("Could not save workflow");
            monitor.setCanceled(true);

        } catch (Exception e) {
            LOGGER.debug("Could not save workflow");

            // inform the user
            MessageBox mb = new MessageBox(Display.getDefault()
                    .getActiveShell(), SWT.ICON_INFORMATION | SWT.OK);
            mb.setText("Workflow could not be saved ...");
            mb.setMessage("The workflow could not be saved. "
                    + "Possibly the file was removed fromt the file "
                    + "system, or the file is set read-only.");
            mb.open();
        }

        try {

            m_fileResource.getProject().refreshLocal(IResource.DEPTH_INFINITE,
                    monitor);

        } catch (Exception e) {
            e.printStackTrace();
        }
        // try {
        // // try to refresh project
        // // m_fileResource.getProject().refreshLocal(IResource.DEPTH_INFINITE,
        // // monitor);
        //            
        // //archive attribute aendern
        // } catch (CoreException e) {
        // // TODO Auto-generated catch block
        // LOGGER.debug("", e);
        // }

        monitor.done();
    }

    /**
     * @see org.eclipse.ui.part.EditorPart#doSaveAs()
     */
    @Override
    public void doSaveAs() {
        throw new UnsupportedOperationException("saveAs not implemented");
    }

    /**
     * @see org.eclipse.ui.part.EditorPart#isDirty()
     */
    @Override
    public boolean isDirty() {
        return m_isDirty;
    }

    /**
     * @see org.eclipse.ui.part.EditorPart#isSaveAsAllowed()
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
    @Override
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

    private final Map<Integer, ProgressMonitorJob> m_dummyNodeJobs = new HashMap<Integer, ProgressMonitorJob>();

    /**
     * Listener callback, listens to workflow events and triggers UI updates.
     * 
     * @see de.unikn.knime.core.node.workflow.WorkflowListener
     *      #workflowChanged(de.unikn.knime.core.node.workflow.WorkflowEvent)
     */
    public void workflowChanged(final WorkflowEvent event) {
        LOGGER.debug("Workflow event triggered: " + event.toString());

        markDirty();

        if (event instanceof WorkflowEvent.NodeWaiting) {
            NodeContainer nc = (NodeContainer)event.getOldValue();
            if (!(MetaOutputModel.class.isAssignableFrom(nc.getModelClass()) || MetaInputModel.class
                    .isAssignableFrom(nc.getModelClass()))) {
                NodeProgressMonitor pm = (NodeProgressMonitor)event
                        .getNewValue();

                ProgressMonitorJob job = new ProgressMonitorJob(nc
                        .getCustomName()
                        + " (" + nc.getName() + ")", pm, m_manager, nc,
                        "Queued for execution...");
                // Reverted as not properly ordered yet. Improve in next version
                // job.schedule();

                m_dummyNodeJobs.put(event.getID(), job);
            }
        } else if (event instanceof WorkflowEvent.NodeStarted) {
            ProgressMonitorJob j = m_dummyNodeJobs.get(event.getID());
            if (j != null) {
                j.setStateMessage("Executing");
                j.schedule();
            }
        } else if (event instanceof WorkflowEvent.NodeFinished) {
            ProgressMonitorJob j = m_dummyNodeJobs.get(event.getID());
            if (j != null) {
                j.finish();
            }
        } else if (event instanceof WorkflowEvent.NodeRemoved) {

            // if a node removed node was a meta node
            // a possible open meta editor must be closed
            MetaWorkflowEditor childEditor = getEditor((NodeContainer)event
                    .getOldValue());
            if (childEditor != null && !childEditor.isClosed()) {
                PlatformUI.getWorkbench().getActiveWorkbenchWindow()
                        .getActivePage().closeEditor(childEditor, false);
            }
        }

    }

    /**
     * Marks this editor as diry and notifies the registered listeners.
     */
    public void markDirty() {
        if (!m_isDirty) {
            m_isDirty = true;

            Display.getDefault().asyncExec(new Runnable() {
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
        private String getTypeString(final IResourceDelta delta) {
            StringBuffer buffer = new StringBuffer();

            if ((delta.getKind() & IResourceDelta.ADDED) != 0) {
                buffer.append("ADDED|");
            }
            if ((delta.getKind() & IResourceDelta.ADDED_PHANTOM) != 0) {
                buffer.append("ADDED_PHANTOM|");
            }
            if ((delta.getKind() & IResourceDelta.ALL_WITH_PHANTOMS) != 0) {
                buffer.append("ALL_WITH_PHANTOMS|");
            }
            if ((delta.getKind() & IResourceDelta.CHANGED) != 0) {
                buffer.append("CHANGED|");
            }
            if ((delta.getKind() & IResourceDelta.CONTENT) != 0) {
                buffer.append("CONTENT|");
            }
            if ((delta.getFlags() & IResourceDelta.DESCRIPTION) != 0) {
                buffer.append("DESCRIPTION|");
            }
            if ((delta.getKind() & IResourceDelta.ENCODING) != 0) {
                buffer.append("ENCODING|");
            }
            if ((delta.getKind() & IResourceDelta.MARKERS) != 0) {
                buffer.append("MARKERS|");
            }
            if ((delta.getFlags() & IResourceDelta.MOVED_FROM) != 0) {
                buffer.append("MOVED_FROM|");
            }
            if ((delta.getFlags() & IResourceDelta.MOVED_TO) != 0) {
                buffer.append("MOVED_TO|");
            }
            if ((delta.getKind() & IResourceDelta.NO_CHANGE) != 0) {
                buffer.append("NO_CHANGE|");
            }
            if ((delta.getKind() & IResourceDelta.OPEN) != 0) {
                buffer.append("OPEN|");
            }
            if ((delta.getKind() & IResourceDelta.REMOVED) != 0) {
                buffer.append("REMOVED|");
            }
            if ((delta.getKind() & IResourceDelta.REMOVED_PHANTOM) != 0) {
                buffer.append("REMOVED_PHANTOM|");
            }
            if ((delta.getKind() & IResourceDelta.REPLACED) != 0) {
                buffer.append("REPLACED|");
            }
            if ((delta.getKind() & IResourceDelta.SYNC) != 0) {
                buffer.append("SYNC|");
            }
            if ((delta.getKind() & IResourceDelta.TYPE) != 0) {
                buffer.append("TYPE|");
            }
            return buffer.toString();
        }

        /**
         * @see org.eclipse.core.resources.IResourceDeltaVisitor
         *      #visit(org.eclipse.core.resources.IResourceDelta)
         */
        public boolean visit(final IResourceDelta delta) throws CoreException {

            LOGGER.debug("Path: " + delta.getResource().getName() + "Parent: "
                    + m_fileResource.getProject() + " Deltat type: "
                    + getTypeString(delta));
            // Parent project removed? close this editor....
            if (m_fileResource.getProject().equals(delta.getResource())) {

                if ((delta.getFlags() & IResourceDelta.MOVED_TO) != 0) {

                    Display.getDefault().syncExec(new Runnable() {
                        public void run() {

                            setPartName(delta.getMovedToPath().segment(0));

                        }
                    });

                } else if ((delta.getKind() & IResourceDelta.REMOVED) != 0
                        && (delta.getFlags() & IResourceDelta.MOVED_TO) == 0) {
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

                        // // get the new name
                        // String newName =
                        // delta.getMovedToPath().lastSegment();
                        // // set the part name
                        // setPartName(newName);

                        // we need to update the file resource that we have
                        // currently opened in our editor, so that we now have
                        // the "new" file
                        m_fileResource = ResourcesPlugin.getWorkspace()
                                .getRoot().getFile(delta.getMovedToPath());
                        setDefaultInput(new FileEditorInput(m_fileResource));

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
    // void createWorkflowManager(final NodeSettings settings) {
    // m_manager = RepositoryManager.INSTANCE.loadWorkflowFromConfig(settings);
    // m_manager.addListener(this);
    // }
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
    public MetaWorkflowEditor getEditor(final NodeContainer metaNodeContainer) {

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

    /**
     * Adapts a point according to the given zoom manager.
     * 
     * @param zoomManager the zoom manager providing the zoom levels
     * @param pointToAdapt the point to adapt
     */
    public static void adaptZoom(final ZoomManager zoomManager,
            final Point pointToAdapt, final boolean adaptViewPortLocation) {

        if (adaptViewPortLocation) {
            Point viewPortLocation = zoomManager.getViewport()
                    .getViewLocation();
            pointToAdapt.x += viewPortLocation.x;
            pointToAdapt.y += viewPortLocation.y;
        }
        double zoomLevel = zoomManager.getZoom();

        // adapt the location accordint to the zoom level
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
            Point viewPortLocation = zoomManager.getViewport()
                    .getViewLocation();
            pointToAdapt.x += viewPortLocation.x;
            pointToAdapt.y += viewPortLocation.y;
        }
        double zoomLevel = zoomManager.getZoom();

        // adapt the location accordint to the zoom level
        pointToAdapt.preciseX = (pointToAdapt.x * (1.0 / zoomLevel));
        pointToAdapt.preciseY = (pointToAdapt.y * (1.0 / zoomLevel));
    }
}
