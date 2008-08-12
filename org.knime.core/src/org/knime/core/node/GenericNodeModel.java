/*
 * ------------------------------------------------------------------
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
 * --------------------------------------------------------------------- *
 *
 * History
 *   17.01.2006(sieb, ohl): reviewed
 */
package org.knime.core.node;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.NoSuchElementException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.property.hilite.DefaultHiLiteHandler;
import org.knime.core.node.property.hilite.HiLiteHandler;
import org.knime.core.node.property.hilite.HiLiteHandlerAdapter;
import org.knime.core.node.workflow.ScopeLoopContext;
import org.knime.core.node.workflow.ScopeObjectStack;
import org.knime.core.node.workflow.ScopeVariable;


/**
 * Class implements the general model of a node which gives access to the
 * <code>DataTable</code>,<code>HiLiteHandler</code>, and
 * <code>DataTableSpec</code> of all outputs.
 * <p>
 * The <code>NodeModel</code> should contain the node's "model", i.e., what
 * ever is stored, contained, done in this node - it's the "meat" of this node.
 *
 * @author Thomas Gabriel, University of Konstanz
 */
public abstract class GenericNodeModel {

    /**
     * The node logger for this class; do not make static to make sure the right
     * class name is printed in messages.
     */
    private final NodeLogger m_logger;

    /** Hold in and output port types. */
    private final PortType[] m_inPortTypes;
    private final PortType[] m_outPortTypes;

    /** Holds the input hilite handler for each input. */
    private final HiLiteHandler[] m_inHiLiteHdls;
    
    /** Hilite adapter returned in 
     * {@link GenericNodeModel#getInHiLiteHandler(int)} when the current in-port
     * hilite handler is <code>null</code>, e.g. the node is not fully 
     * connected.
     */
    private static final HiLiteHandlerAdapter HILITE_ADAPTER 
        = new HiLiteHandlerAdapter();

    /** Keeps a list of registered views. */
    private final CopyOnWriteArrayList<GenericNodeView<?>> m_views;

    /** Flag for the hasContent state. */
    private boolean m_hasContent;

    /**
     * Optional warning message to be set during / after execution. Enables
     * higher levels to display the given message.
     */
    private String m_warningMessage = null;

    /** The listeners that are interested in changes of the model warning. */
    private final CopyOnWriteArraySet<NodeModelWarningListener>
                                                         m_warningListeners;

    /**
     * Creates a new model with the given number (and types!) of input and
     * output types.
     * @param inPortTypes an array of non-null in-port types
     * @param outPortTypes an array of non-null out-port types
     * @throws NegativeArraySizeException If the number of in- or outputs is
     *             smaller than zero.
     */
    protected GenericNodeModel(final PortType[] inPortTypes,
            final PortType[] outPortTypes) {
        // create logger
        m_logger = NodeLogger.getLogger(this.getClass());

        // init message listener array
        m_warningListeners =
                       new CopyOnWriteArraySet<NodeModelWarningListener>();

        // check port types of validity and store them
        if ((inPortTypes == null) || (outPortTypes == null)) {
            throw new NullPointerException("Args must not be null!");
        }
        m_inPortTypes = new PortType[inPortTypes.length];
        for (int i = 0; i < inPortTypes.length; i++) {
            if (inPortTypes[i] == null) {
                throw new NullPointerException("InPortType[" + i
                        + "] must not be null!");
            }
            m_inPortTypes[i] = inPortTypes[i];
        }
        m_outPortTypes = new PortType[outPortTypes.length];
        for (int i = 0; i < outPortTypes.length; i++) {
            if (outPortTypes[i] == null) {
                throw new NullPointerException("OutPortType[" + i
                        + "] must not be null!");
            }
            m_outPortTypes[i] = outPortTypes[i];
        }

        m_hasContent = false;

        // set initial array of HiLiteHandlers
        if (getNrInPorts() == 0) {
            // initialize a new one if no input exists
            m_inHiLiteHdls = new HiLiteHandler[1];
        } else {
            // otherwise create a spot handlers - one for each input.
            m_inHiLiteHdls = new HiLiteHandler[getNrInPorts()];
        }

        // keeps set of registered views in the order they are added
        m_views = new CopyOnWriteArrayList<GenericNodeView<?>>();
    }

    /**
     * Load internals into the derived <code>NodeModel</code>. This method is
     * only called if the <code>Node</code> was executed. Read all your
     * internal structures from the given file directory to create your internal
     * data structure which is necessary to provide all node functionalities
     * after the workflow is loaded, e.g. view content and/or hilite mapping.
     * <br>
     * This method is not called if the model is autoexecutable. The node will
     * be executed instead.
     *
     * @param nodeInternDir The directory to read from.
     * @param exec Used to report progress and to cancel the load process.
     * @throws IOException If an error occurs during reading from this dir.
     * @throws CanceledExecutionException If the loading has been canceled.
     * @see #saveInternals(File,ExecutionMonitor)
     * @see #setAutoExecutable(boolean)
     */
    protected abstract void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException;

    /**
     * Save internals of the derived <code>NodeModel</code>. This method is
     * only called if the <code>Node</code> is executed. Write all your
     * internal structures into the given file directory which are necessary to
     * recreate this model when the workflow is loaded, e.g. view content and/or
     * hilite mapping.<br>
     * This method is not called, if the model is autoexecutable.
     *
     * @param nodeInternDir The directory to write into.
     * @param exec Used to report progress and to cancel the save process.
     * @throws IOException If an error occurs during writing to this dir.
     * @throws CanceledExecutionException If the saving has been canceled.
     * @see #loadInternals(File,ExecutionMonitor)
     * @see #setAutoExecutable(boolean)
     */
    protected abstract void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException;


    /**
     * Registers the given view at the model to receive change events of the
     * underlying model. Note that no change event is fired.
     *
     * @param view The view to register.
     */
    final void registerView(final GenericNodeView<?> view) {
        assert view != null;
        m_views.add(view);
        m_logger.debug("Registering view at  model (total count "
                + m_views.size() + ")");
    }

    /**
     * Unregisters the given view.
     *
     * @param view The view to unregister.
     */
    final void unregisterView(final GenericNodeView<?> view) {
        assert view != null;
        boolean success = m_views.remove(view);
        if (success) {
            m_logger.debug("Unregistering view from model ("
                    + m_views.size() + " remaining).");
        } else {
            m_logger.debug("Can't remove view from model, not registered.");
        }
    }

    /**
     * Unregisters all views from the model.
     */
    final void unregisterAllViews() {
        m_logger.debug("Removing all (" + m_views.size()
                + ") views from model.");
        m_views.clear();
    }

    /**
     * @return All registered views.
     */
    final Collection<GenericNodeView<?>> getViews() {
        return Collections.unmodifiableCollection(m_views);
    }

    /**
     * Returns the overall number of inputs.
     *
     * @return Number of inputs.
     */
    protected final int getNrInPorts() {
        return m_inPortTypes.length;
    }

    /**
     * Returns the overall number of outputs.
     *
     * @return Number of outputs.
     */
    protected final int getNrOutPorts() {
        return m_outPortTypes.length;
    }

    /**
     * @param index
     * @return Type of the specified input port
     */
    final PortType getInPortType(final int index) {
        return m_inPortTypes[index];
    }

    /**
     * @param index
     * @return Type of the specified output port
     */
    final PortType getOutPortType(final int index) {
        return m_outPortTypes[index];
    }

    /**
     * Validates the specified settings in the model and then loads them into
     * it.
     *
     * @param settings the settings to read
     * @throws InvalidSettingsException if the settings are not valid or cannot
     *      be loaded into the model
     */
    final void loadSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        // validate the settings before loading them
        validateSettings(settings);
        // load settings into the model
        loadValidatedSettingsFrom(settings);
    }

    /**
     * Adds to the given <code>NodeSettings</code> the model specific
     * settings. The settings don't need to be complete or consistent. If, right
     * after startup, no valid settings are available this method can write
     * either nothing or invalid settings.
     * <p>
     * Method is called by the <code>Node</code> if the current settings need
     * to be saved or transfered to the node's dialog.
     *
     * @param settings The object to write settings into.
     *
     * @see #loadValidatedSettingsFrom(NodeSettingsRO)
     * @see #validateSettings(NodeSettingsRO)
     */
    protected abstract void saveSettingsTo(final NodeSettingsWO settings);

    /**
     * Validates the settings in the passed <code>NodeSettings</code> object.
     * The specified settings should be checked for completeness and
     * consistency. It must be possible to load a settings object validated
     * here without any exception in the
     * <code>#loadValidatedSettings(NodeSettings)</code> method. The method
     * must not change the current settings in the model - it is supposed to
     * just check them. If some settings are missing, invalid, inconsistent, or
     * just not right throw an exception with a message useful to the user.
     *
     * @param settings The settings to validate.
     * @throws InvalidSettingsException If the validation of the settings
     *             failed.
     * @see #saveSettingsTo(NodeSettingsWO)
     * @see #loadValidatedSettingsFrom(NodeSettingsRO)
     */
    protected abstract void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException;

    /**
     * Sets new settings from the passed object in the model. You can safely
     * assume that the object passed has been successfully validated by the
     * <code>#validateSettings(NodeSettings)</code> method. The model must set
     * its internal configuration according to the settings object passed.
     *
     * @param settings The settings to read.
     *
     * @throws InvalidSettingsException If a property is not available.
     *
     * @see #saveSettingsTo(NodeSettingsWO)
     * @see #validateSettings(NodeSettingsRO)
     */
    protected abstract void loadValidatedSettingsFrom(
            final NodeSettingsRO settings) throws InvalidSettingsException;

    /**
     * Invokes the abstract <code>#execute()</code> method of this model. In
     * addition, this method notifies all assigned views of the model about the
     * changes.
     * 
     * @param data An array of <code>DataTable</code> objects holding the data
     *            from the inputs.
     * @param exec The execution monitor which is passed to the execute method
     *            of this model.
     * @return The result of the execution in form of an array with
     *         <code>DataTable</code> elements, as many as the node has
     *         outputs.
     * @throws Exception any exception or error that is fired in the derived
     *             model will be just forwarded. It may throw an 
     *             CanceledExecutionException if the user pressed cancel during
     *             execution. Even if the derived model doesn't check, the
     *             result will be discarded and the exception thrown. 
     * @throws IllegalStateException If the number of <code>PortObject</code>
     *             objects returned by the derived <code>GenericNodeModel</code>
     *             does not match the number of outputs. Or if any of them is 
     *             null.
     * @see #execute(PortObject[],ExecutionContext)
     */
    protected final PortObject[] executeModel(final PortObject[] data, 
            final ExecutionContext exec) 
        throws Exception {
        assert (data != null && data.length == getNrInPorts());
        assert (exec != null);

        // TODO: check ingoing types! (in Node!)

        // temporary storage for result of derived model.
        // EXECUTE DERIVED MODEL
        PortObject[] outData = execute(data, exec);

        // if execution was canceled without exception flying return false
        if (exec.isCanceled()) {
            throw new CanceledExecutionException(
                    "Result discarded due to user cancel");
        }
        
        /* Cleanup operation for nodes that just pass on their input 
         * data table. We need to wrap those here so that the framework 
         * explicitly references them (instead of copying) */
        for (int i = 0; i < outData.length; i++) {
            if (outData[i] instanceof BufferedDataTable) {
                for (int j = 0; j < data.length; j++) {
                    if (outData[i] == data[j]) {
                        outData[i] = exec.createWrappedTable(
                                (BufferedDataTable)data[j]);
                    }
                }
            }
        }

        // TODO: check outgoing types! (inNode!)

        if (outData == null) {
            outData = new PortObject[getNrOutPorts()];
        }
        // if number of out tables does not match: fail
        if (outData.length != getNrOutPorts()) {
            throw new IllegalStateException(
                    "Invalid result. Execution failed. "
                            + "Reason: Incorrect implementation; the execute"
                            + " method in " + this.getClass().getSimpleName()
                            + " returned null or an incorrect number of output"
                            + " tables.");
        }

        // check the result, data tables must not be null
        for (int i = 0; i < outData.length; i++) {
            // do not check for null output tables if this is the end node
            // of a loop and another loop iteration is requested
            if ((getLoopStatus() == null) && (outData[i] == null)) {
                m_logger.error("Execution failed: Incorrect implementation;"
                        + " the execute method in "
                        + this.getClass().getSimpleName()
                        + "returned a null data table at port: " + i);
                throw new IllegalStateException("Invalid result. "
                        + "Execution failed, reason: data at output " + i
                        + " is null.");
            }
        }
        // check meaningfulness of result and warn,
        // - only if the execute didn't issue a warning already
        if ((m_warningMessage == null)
                || (m_warningMessage.length() == 0)) {
            boolean hasData = false;
            boolean hasDataPorts = false;
            for (int i = 0; i < outData.length; i++) {
                if (outData[i] instanceof BufferedDataTable) {
                    // do some sanity checks on PortObjects holding data tables
                    hasDataPorts = true;
                    BufferedDataTable outDataTable =
                        (BufferedDataTable)outData[i];
                    if (outDataTable.getDataTableSpec().getNumColumns() < 1) {
                        m_logger.info("The result table at port " + i
                                + " has no columns");
                    }
                    if (outDataTable.getRowCount() < 1) {
                        m_logger.info("The result table at port " + i
                                + " contains no rows");
                    }
                    if (outDataTable.getDataTableSpec().getNumColumns() > 0
                            && outDataTable.getRowCount() > 0) {
                        hasData = true;
                    }
                }
            }
            if (hasDataPorts && !hasData) {
                setWarningMessage(
                        "Node created empty data tables on all out-ports.");
            }
        }

        setHasContent(true);
        return outData;
    } // executeModel(PortObject[],ExecutionMonitor)

    /**
     * Sets the hasContent flag and fires a state change event.
     * @param hasContent Flag if this node is configured be executed or not.
     */
    final void setHasContent(final boolean hasContent) {
        if (hasContent != hasContent()) {
            m_hasContent = hasContent;
            // and inform all views about the new model
            stateChanged();
        }
    }

    /**
     * @return <code>true</code> if this model has been executed and therefore
     * possibly has content that can be displayed in a view, 
     * <code>false</code> otherwise.
     */
    final boolean hasContent() {
        return m_hasContent;
    }

    /**
     * This function is invoked by the <code>Node#executeNode()</code> method
     * of the node (through the
     * <code>#executeModel(BufferedDataTable[],ExecutionMonitor)</code>
     * method)only after all predecessor nodes have been successfully executed
     * and all data is therefore available at the input ports. Implement this
     * function with your task in the derived model.
     * <p>
     * The input data is available in the given array argument
     * <code>inData</code> and is ensured to be neither <code>null</code>
     * nor contain <code>null</code> elements.
     *
     * <p>
     * In order to create output data, you need to create objects of class
     * <code>BufferedDataTable</code>. Use the execution context argument to
     * create <code>BufferedDataTable</code>.
     *
     * @param inData An array holding <code>DataTable</code> elements, one for
     *            each input.
     * @param exec The execution monitor for this execute method. It provides us
     *            with means to create new <code>BufferedDataTable</code>.
     *            Additionally, it should be asked frequently if the execution
     *            should be interrupted and throws an exception then. This
     *            exception might me caught, and then after closing all data
     *            streams, been thrown again. Also, if you can tell the progress
     *            of your task, just set it in this monitor.
     * @return An array of non- <code>null</code> DataTable elements with the
     *         size of the number of outputs. The result of this execution.
     * @throws Exception If you must fail the execution. Try to provide a
     *             meaningful error message in the exception as it will be
     *             displayed to the user.<STRONG>Please</STRONG> be advised to
     *             check frequently the canceled status by invoking
     *             <code>ExecutionMonitor#checkCanceled</code> which will
     *             throw an <code>CanceledExcecutionException</code> and abort
     *             the execution.
     */
    protected abstract PortObject[] execute(
            final PortObject[] inData, final ExecutionContext exec)
            throws Exception;

    /**
     * Invokes the abstract <code>#reset()</code> method of the derived model.
     * In addition, this method resets all hilite handlers, and notifies all
     * views about the changes.
     */
    final void resetModel() {
        try {
            // reset in derived model
            reset();
        } catch (Throwable t) {
            String name = t.getClass().getSimpleName();
            m_logger.coding("Reset failed due to a " + name, t);
        } finally {
            // set state to not executed and not configured
            m_hasContent = false;
            // reset these property handlers
            resetHiLiteHandlers();
            // and notify all views
            stateChanged();
        }
    }

    /**
     * Override this function in the derived model and reset your
     * <code>NodeModel</code>. All components should unregister themselves
     * from any observables (at least from the hilite handler right now). All
     * internally stored data structures should be released. User settings
     * should not be deleted/reset though.
     */
    protected abstract void reset();

    /**
     * Notifies all registered views of a change of the underlying model. It is
     * called by functions of the abstract class that modify the model (like
     * <code>#executeModel()</code> and <code>#resetModel()</code> ).
     */
    protected final void stateChanged() {
        for (GenericNodeView<?> view : m_views) {
            try {
                view.callModelChanged();
            } catch (Exception e) {
                setWarningMessage("View [" + view.getViewName()
                        + "] could not be open, reason: " + e.getMessage());
                m_logger.debug("View [" + view.getViewName()
                        + "] could not be open", e);
            }
        }
    }

    /**
     * This method can be called from the derived model in order to inform the
     * views about changes of the settings or during execution, if you want the
     * views to show the progress, and if they can display models half way
     * through the execution. In the view
     * <code>GenericNodeView#updateModel(Object)</code> is called and needs to
     * be overridden.
     *
     * @param arg The argument you want to pass.
     */
    protected final void notifyViews(final Object arg) {
        for (GenericNodeView<?> view : m_views) {
            view.updateModel(arg);
        }
    }

    /**
     * <p>This implementation is empty. Subclasses may override this method
     * in order to be informed when the hilite handler changes at the inport,
     * e.g. when the node (or an preceding node) is newly connected.
     *
     * @param inIndex The index of the input.
     * @param hiLiteHdl The <code>HiLiteHandler</code> at input index.
     *         May be <code>null</code> when not available, i.e. not properly
     *         connected.
     * @throws IndexOutOfBoundsException If the <code>inIndex</code> is not in
     *          the range of inputs.
     */
    //TODO refactor - terrible name! This is a notification really!
    protected void setInHiLiteHandler(final int inIndex,
            final HiLiteHandler hiLiteHdl) {
        assert inIndex >= 0;
        assert hiLiteHdl == hiLiteHdl;
    }

    /**
     * Sets a new <code>HiLiteHandler</code> for the given input.
     *
     * This method is called by the corresponding <code>Node</code> in order
     * to set the <code>HiLiteHandler</code> for the given input port.
     *
     * @param in The input index.
     * @param hdl The new <code>HiLiteHandler</code>.
     *
     * @see #setInHiLiteHandler(int, HiLiteHandler)
     */
    final void setNewInHiLiteHandler(final int in, final HiLiteHandler hdl) {
        if (m_inHiLiteHdls[in] == hdl) {
            // only do something (calls notification and state change!)
            // if there really is a new HiLiteHandler.
            return;
        }
        m_inHiLiteHdls[in] = hdl;
        int simulatedPortIndex = getSimulatedHiliteHandlerPortIndex(in);
        if (simulatedPortIndex >= 0) {
            setInHiLiteHandler(simulatedPortIndex, hdl);
        }
        stateChanged();
    }

    /**
     * Returns the <code>HiLiteHandler</code> for the given input index, if the
     * current in-port hilite handler is <code>null</code> an 
     * <code>HiLiteHandlerAdapter</code> is created and returned.
     *
     * @param inIndex in-port index
     * @return <code>HiLiteHandler</code> for the given input index
     * @throws IndexOutOfBoundsException if the <code>inIndex</code> is not in
     *             the range of inputs
     */
    public final HiLiteHandler getInHiLiteHandler(final int inIndex) {
        int correctIndex = getTrueHiliteHandlerPortIndex(inIndex);
        if (m_inHiLiteHdls[correctIndex] == null) {
            return HILITE_ADAPTER;
        }
        return m_inHiLiteHdls[correctIndex];
    }
    
    /** Returns the argument. This method is overridden in class 
     * {@link NodeModel} to handle incoming model ports appropriately 
     * (no hilite handlers at deprecated model ports).
     * @param portIndex The simulated port index
     * @return The true port index
     */  
    int getTrueHiliteHandlerPortIndex(final int portIndex) {
        return portIndex;
    }
    
    /** Returns the argument. This method is overridden in class 
     * {@link NodeModel} to handle incoming model ports appropriately 
     * (no hilite handlers at deprecated model ports).
     * @param portIndex The true port index
     * @return The simulated port index
     */  
    int getSimulatedHiliteHandlerPortIndex(final int portIndex) {
        return portIndex;
    }

    /**
     * Returns the <code>HiLiteHandler</code> for the given output index. This
     * default implementation simply passes on the handler of input port 0 or
     * generates a new one if this node has no inputs. <br>
     * <br>
     * This method is intended to be overridden
     *
     * @param outIndex The output index.
     * @return <code>HiLiteHandler</code> for the given output port.
     * @throws IndexOutOfBoundsException If the index is not in output's range.
     */
    protected HiLiteHandler getOutHiLiteHandler(final int outIndex) {
        if (outIndex < 0 || outIndex >= getNrOutPorts()) {
            throw new IndexOutOfBoundsException("index=" + outIndex);
        }
        // if we have no inputs we create a new instance (but only one...)
        if (getNrInPorts() == 0) {
            if (m_inHiLiteHdls[0] == null) {
                m_inHiLiteHdls[0] = new DefaultHiLiteHandler();
            }
            return m_inHiLiteHdls[0];
        }
        return getInHiLiteHandler(0);
    }

    /**
     * Resets all internal <code>HiLiteHandler</code> objects if the number of
     * inputs is zero.
     */
    private void resetHiLiteHandlers() {
        // if we have no inputs we have created a new instance.
        // we need to reset it here then.
        if (getNrInPorts() == 0) {
            for (int i = 0; i < m_inHiLiteHdls.length; i++) {
                if (m_inHiLiteHdls[i] != null) {
                    m_inHiLiteHdls[i].fireClearHiLiteEvent();
                }
            }
        }
    }

    /**
     * This function is called when something changes that could affect the
     * output <code>DataTableSpec</code> elements. E.g. after a reset,
     * execute, (dis-)connect, and object creation (model instantiation).
     * <p>
     * The function calls <code>#configure()</code> to receive the updated
     * output DataTableSpecs, if the model is not executed yet. After execution
     * the DataTableSpecs are simply taken from the output DataTables.
     *
     * @param inSpecs An array of input <code>DataTableSpec</code> elements,
     *            either the array or each of its elements can be
     *            <code>null</code>.
     * @return An array where each element indicates if the outport has changed.
     * @throws InvalidSettingsException if the current settings don't go along
     *             with the table specs
     */
    final PortObjectSpec[] configureModel(final PortObjectSpec[] inSpecs)
            throws InvalidSettingsException {

        assert inSpecs.length == getNrInPorts();

        PortObjectSpec[] copyInSpecs = new PortObjectSpec[getNrInPorts()];
        PortObjectSpec[] newOutSpecs;

        if (inSpecs != null) {
            System.arraycopy(inSpecs, 0, copyInSpecs, 0, inSpecs.length);
        }
        // make sure we conveniently have TableSpecs.
        // Rather empty ones than null
        for (int i = 0; i < copyInSpecs.length; i++) {
            if (copyInSpecs[i] == null) {
                if (m_inPortTypes[i].getPortObjectSpecClass().isAssignableFrom(
                        DataTableSpec.class)) {
                    // replace with an empty data table spec
                    copyInSpecs[i] = new DataTableSpec();
                } else {
                    // TODO: is this really what we want?
                    copyInSpecs[i] = null;
                }
            }
        }
        // CALL CONFIGURE
        newOutSpecs = configure(copyInSpecs);
        if (newOutSpecs == null) {
            newOutSpecs = new PortObjectSpec[getNrOutPorts()];
        }
        // check output object spec length
        if (newOutSpecs.length != getNrOutPorts()) {
            m_logger.error("Output spec-array length invalid: "
                    + newOutSpecs.length + " <> " + getNrOutPorts());
            newOutSpecs = new PortObjectSpec[getNrOutPorts()];
        }
        return newOutSpecs;
    }

    /**
     * This function is called whenever the derived model should re-configure
     * its output DataTableSpecs. Based on the given input data table spec(s)
     * and the current model's settings, the derived model has to calculate the
     * output data table spec and return them.
     * <p>
     * The passed DataTableSpec elements are never <code>null</code> but can
     * be empty. The model may return <code>null</code> data table spec(s) for
     * the outputs. But still, the model may be in an executable state. Note,
     * after the model has been executed this function will not be called
     * anymore, as the output DataTableSpecs are then being pulled from the
     * output DataTables. A derived <code>NodeModel</code> that doesn't want
     * to provide any DataTableSpecs at its outputs before execution can return
     * an array containing just <code>null</code> elements.
     *
     * @param inSpecs An array of DataTableSpecs (as many as this model has
     *            inputs). Do NOT modify the contents of this array. None of the
     *            DataTableSpecs in the array can be <code>null</code> but
     *            empty. If the predecessor node is not yet connected, or
     *            doesn't provide a DataTableSpecs at its output port.
     * @return An array of DataTableSpecs (as many as this model has outputs)
     *         They will be propagated to connected successor nodes.
     *         <code>null</code> DataTableSpec elements are changed to empty
     *         once.
     *
     * @throws InvalidSettingsException if the <code>#configure()</code>
     *             failed, that is, the settings are inconsistent with given
     *             DataTableSpec elements.
     */
    protected abstract PortObjectSpec[] configure(
            final PortObjectSpec[] inSpecs)
            throws InvalidSettingsException;

    /////////////////////////
    // Warning handling
    /////////////////////////
    
    /**
     * Sets an optional warning message by the implementing node model.
     *
     * @param warningMessage the warning message to set
     */
    protected final void setWarningMessage(final String warningMessage) {
        // message changed, set new one and notify listeners.
        m_warningMessage = warningMessage;
        notifyWarningListeners(m_warningMessage);
    }

    /**
     * Adds a warning listener to this node. Ignored if the listener is already
     * registered.
     * 
     * @param listener The listener to add.
     */
    public void addWarningListener(final NodeModelWarningListener listener) {
        if (listener == null) {
            throw new NullPointerException(
                    "NodeModel message listener must not be null!");
        }
        m_warningListeners.add(listener);
    }

    /**
     * Removes a warning listener from this node. Ignored if the listener is
     * not registered.
     * 
     * @param listener The listener to remove.
     */
    public void removeWarningListener(
            final NodeModelWarningListener listener) {
        if (!m_warningListeners.remove(listener)) {
            m_logger.debug("listener was not registered: " + listener);
        }
    }

    /**
     * Notifies all listeners that the warning of this node has changed.
     * 
     * @param message The warning message.
     */
    public void notifyWarningListeners(final String warning) {
        for (NodeModelWarningListener listener : m_warningListeners) {
            try {
                listener.warningChanged(warning);
            } catch (Throwable t) {
                m_logger.error("Exception while notifying NodeModel listeners",
                        t);
            }
        }
    }
    
    /** Holds the ScopeContext Stack of this node. */
    private ScopeObjectStack m_scopeContextStackContainer;

    /** Get the value of the String variable with the given name leaving the
     * variable stack unmodified. 
     * @param name Name of the variable
     * @return The value of the string variable
     * @throws NullPointerException If the argument is null
     * @throws NoSuchElementException If no such variable with the correct
     * type is available.
     */
    protected final String peekScopeVariableString(final String name) {
        return m_scopeContextStackContainer.peekScopeVariable(
                name, ScopeVariable.Type.STRING).getStringValue();
    }
    
    /** Put a new variable of type double onto the stack. If such variable
     * already exists, its value will be (virtually) overwritten.
     * @param name The name of the variable.
     * @param value The assignment value for the variable
     * @throws NullPointerException If the name argument is null.
     */
    protected final void pushScopeVariableDouble(
            final String name, final double value) {
        m_scopeContextStackContainer.push(new ScopeVariable(name, value));
    }
    
    /** Get the value of the double variable with the given name leaving the
     * variable stack unmodified. 
     * @param name Name of the variable
     * @return The assignment value of the variable
     * @throws NullPointerException If the argument is null
     * @throws NoSuchElementException If no such variable with the correct
     * type is available.
     */
    protected final double peekScopeVariableDouble(final String name) {
        return m_scopeContextStackContainer.peekScopeVariable(
                name, ScopeVariable.Type.DOUBLE).getDoubleValue();
    }
    
    /** Put a new variable of type integer onto the stack. If such variable
     * already exists, its value will be (virtually) overwritten.
     * @param name The name of the variable.
     * @param value The assignment value for the variable
     * @throws NullPointerException If the name argument is null.
     */
    protected final void pushScopeVariableInt(
            final String name, final int value) {
        m_scopeContextStackContainer.push(new ScopeVariable(name, value));
    }
    
    /** Get the value of the integer variable with the given name leaving the
     * variable stack unmodified. 
     * @param name Name of the variable
     * @return The value of the integer variable
     * @throws NullPointerException If the argument is null
     * @throws NoSuchElementException If no such variable with the correct
     * type is available.
     */
    protected final int peekScopeVariableInt(final String name) {
        return m_scopeContextStackContainer.peekScopeVariable(
                name, ScopeVariable.Type.INTEGER).getIntValue();
    }
    
    /** Put a new variable of type String onto the stack. If such variable
     * already exists, its value will be (virtually) overwritten.
     * @param name The name of the variable.
     * @param value The assignment value for the variable
     * @throws NullPointerException If the name argument is null.
     */
    protected final void pushScopeVariableString(
            final String name, final String value) {
        m_scopeContextStackContainer.push(new ScopeVariable(name, value));
    }
    
    /** Informs WorkflowManager after execute to continue the loop.
     * Call by the tail of the loop! This will result in both
     * this Node as well as the creator of the ScopeContext to be
     * queued for execution once again. In this case the node can return
     * an empty table after execution.
     */
    protected final void continueLoop() {
        ScopeLoopContext slc = m_scopeContextStackContainer.peek(
                ScopeLoopContext.class);
        if (slc == null) {
            // wrong wiring of the pipeline: head seems to be missing!
            throw new IllegalStateException(
                    "Missing Loop Head in Pipeline!");
        }
        m_loopStatus = slc;
        // note that the WFM will set the tail ID so we can retrieve it
        // in the head node!
    }

    private ScopeLoopContext m_loopStatus;
    
    protected final ScopeLoopContext getLoopStatus() {
        return m_loopStatus;
    }
    
    protected final void clearLoopStatus() {
        m_loopStatus = null;
    }
    
    private GenericNodeModel m_loopEndNode = null;
    
    protected final GenericNodeModel getLoopEndNode() {
        return m_loopEndNode;
    }
    
    public void setLoopEndNode(final GenericNodeModel end) {
        m_loopEndNode = end;
    }

    private GenericNodeModel m_loopStartNode = null;
    
    protected final GenericNodeModel getLoopStartNode() {
        return m_loopStartNode;
    }
    
    public void setLoopStartNode(final GenericNodeModel start) {
        m_loopStartNode = start;
    }

    ScopeObjectStack getScopeContextStackContainer() {
        return m_scopeContextStackContainer;
    }

    void setScopeContextStackContainer(final ScopeObjectStack scsc) {
        m_scopeContextStackContainer = scsc;
    }

}

