/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2006
 * University of Konstanz, Germany.
 * Chair for Bioinformatics and Information Mining
 * Prof. Dr. Michael R. Berthold
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any quesions please contact the copyright holder:
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
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.property.hilite.DefaultHiLiteHandler;
import org.knime.core.node.property.hilite.HiLiteHandler;


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
public abstract class NodeModel {

    /**
     * The node logger for this class; do not make static to make sure the right
     * class name is printed in messages.
     */
    private final NodeLogger m_logger;

    /** The number of input data tables. */
    private final int m_nrDataIns;

    /** The number of output data tables produced by the derived model. */
    private final int m_nrDataOuts;

    /** The number of input predictive models. */
    private final int m_nrModelIns;

    /** The number of predictive models. */
    private final int m_nrModelOuts;

    /** Holds the input hilite handler for each input. */
    private final HiLiteHandler[] m_inHiLiteHdls;

    /** Keeps a list of registered views. */
    private final Set<NodeView> m_views;

    /** Stores the result of the last call of <code>#configure()</code>. */
    private boolean m_configured;

    /** Flag for the isExecuted state. */
    private boolean m_executed;

    /**
     * Flag to indicate that the node should be immediately executed when all
     * predecessors are green, interesting for instance in the table view.
     */
    private boolean m_isAutoExecutable;

    /**
     * Optional warning message to be set during / after execution. Enables
     * hiher levels to display the given message.
     */
    private String m_warningMessage = null;

    /**
     * Creates a new model with the given number of data, and predictor in- and
     * outputs.
     * 
     * @param nrDataIns The number of <code>DataTable</code> elements expected
     *            as inputs.
     * @param nrDataOuts The number of <code>DataTable</code> objects expected
     *            at the output.
     * @param nrPredParamsIns The number of <code>ModelContent</code>
     *            elements available as inputs.
     * @param nrPredParamsOuts The number of <code>ModelContent</code>
     *            objects available at the output.
     * @throws NegativeArraySizeException If the number of in- or outputs is
     *             smaller than zero.
     */
    protected NodeModel(final int nrDataIns, final int nrDataOuts,
            final int nrPredParamsIns, final int nrPredParamsOuts) {
        // create logger
        m_logger = NodeLogger.getLogger(this.getClass());
        // inits number of inputs
        m_nrDataIns = nrDataIns;
        // inits number of outputs
        m_nrDataOuts = nrDataOuts;
        // inits number of inputs
        m_nrModelIns = nrPredParamsIns;
        // inits number of outputs
        m_nrModelOuts = nrPredParamsOuts;

        // model is not configured and not executed
        m_configured = false;
        m_executed = false;

        // set intial array of HiLiteHandlers
        if (getNrIns() == 0) {
            // init a new one if no input exists
            m_inHiLiteHdls = new HiLiteHandler[1];
        } else {
            // otherwise create a spot handlers - one for each input.
            m_inHiLiteHdls = new HiLiteHandler[getNrDataIns()];
        }

        // keeps set of registered views in the order they are added
        m_views = Collections.synchronizedSet(new LinkedHashSet<NodeView>());
    }

    /**
     * Creates a new model with the given number of in- and outputs.
     * 
     * @param nrDataIns Number of data inputs.
     * @param nrDataOuts Number of data outputs.
     * 
     * @see NodeModel#NodeModel(int, int, int, int)
     */
    protected NodeModel(final int nrDataIns, final int nrDataOuts) {
        this(nrDataIns, nrDataOuts, 0, 0);
    }
    
    /**
     * Load internals into the derived <code>NodeModel</code>. This method is 
     * only called if the <code>Node</code> was executed. Read all your
     * internal structures from the given file directory to create your internal
     * data structure which is necessary to provide all node functionalities
     * after the workflow is loaded, e.g. view content and/or hilite mapping.  
     * @param nodeInternDir The directory to read from.
     * @param exec Used to report progress and to cancel the load process.
     * @throws IOException If an error occurs during reading from this dir.
     * @throws CanceledExecutionException If the loading has been canceled.
     * @see #saveInternals(File,ExecutionMonitor)
     */
    protected abstract void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException;
    
    /**
     * Save internals of the derived <code>NodeModel</code>. This method is 
     * only called if the <code>Node</code> is executed. Write all your
     * internal structures into the given file directory which are necessary to 
     * recreate this model when the workflow is loaded, e.g. view content and/or
     * hilite mapping.  
     * @param nodeInternDir The directory to write into.
     * @param exec Used to report progress and to cancel the save process.
     * @throws IOException If an error occurs during writting to this dir.
     * @throws CanceledExecutionException If the saving has been canceled.
     * @see #loadInternals(File,ExecutionMonitor)
     */
    protected abstract void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException;
    
    /**
     * Override this methode if <code>ModelContent</code> input(s) have
     * been set. This methode is then called for each ModelContent input to
     * load the <code>ModelContent</code> after the previous node has been
     * executed successfully or is reset.
     * 
     * <p>This implementation throws a InvalidSettingsException as it should 
     * not have been called: If a derived NodeModel defines a model input, it
     * must override this method.
     * 
     * @param index The input index, starting from 0.
     * @param predParams The ModelContent to load, which can be null to
     *            indicate that no ModelContent model is available.
     * @throws InvalidSettingsException If the predictive parameters could not
     *             be loaded.
     */
    protected void loadModelContent(final int index,
            final ModelContentRO predParams) throws InvalidSettingsException {
        assert predParams == predParams;
        throw new InvalidSettingsException(
                "loadModelContent() not overridden: " + index);
    }

    /**
     * Override this methode if <code>ModelContent</code> output(s) have
     * been set. This methode is then called for each
     * <code>ModelContent</code> output to save the
     * <code>ModelContent</code> after this node has been successfully
     * executed.
     * 
     * <p>This implementation throws a InvalidSettingsException as it should 
     * not have been called: If a derived NodeModel defines a model output, it
     * must override this method.

     * @param index The output index, starting from 0.
     * @param predParams The ModelContent to save to.
     * @throws InvalidSettingsException If the model could not be saved.
     */
    protected void saveModelContent(final int index,
            final ModelContentWO predParams) throws InvalidSettingsException {
        assert predParams == predParams;
        throw new InvalidSettingsException(
                "saveModelContent() not overridden: " + index);
    }

    /**
     * Registers the given view at the model to receive change events of the
     * underlying model. Note that no change event is fired.
     * 
     * @param view The view to register.
     */
    final void registerView(final NodeView view) {
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
    final void unregisterView(final NodeView view) {
        assert view != null;
        boolean success = m_views.remove(view);
        if (success) {
            m_logger.debug("Unregistering view from model (" + m_views.size()
                    + " remaining).");
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
    final Set<NodeView> getViews() {
        return Collections.unmodifiableSet(m_views);
    }

    /**
     * Returns the number of data inputs.
     * 
     * @return Number of inputs.
     */
    protected final int getNrDataIns() {
        return m_nrDataIns;
    }

    /**
     * Returns the number of data outputs.
     * 
     * @return Number of outputs.
     */
    protected final int getNrDataOuts() {
        return m_nrDataOuts;
    }

    /**
     * Returns the number of model inputs.
     * 
     * @return Number of inputs.
     */
    protected final int getNrModelIns() {
        return m_nrModelIns;
    }

    /**
     * Returns the number of model outputs.
     * 
     * @return Number of outputs.
     */
    protected final int getNrModelOuts() {
        return m_nrModelOuts;
    }

    /**
     * Returns the overall number of inputs.
     * 
     * @return Number of inputs.
     */
    protected final int getNrIns() {
        return m_nrDataIns + m_nrModelIns;
    }

    /**
     * Returns the overall number of outputs.
     * 
     * @return Number of outputs.
     */
    protected final int getNrOuts() {
        return m_nrDataOuts + m_nrModelOuts;
    }

    /**
     * Returns <code>true</code> if the model has been configured, i.e. all
     * user settings set.
     * 
     * @return <code>true</code> if the model has been configured, otherwise
     *         <code>false</code>.
     */
    final boolean isConfigured() {
        return m_configured;
    }

    /**
     * Validates the specified settings in the model and then loads them into
     * it.
     * 
     * @param settings The settings to read.
     * @throws InvalidSettingsException If the load iof the validated settings
     *             fails.
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
     * after startup, no valid settings are available this mehtod can write
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
     * consistencty. It must be possible to load a settings object validated
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
     * @throws Exception any exception that is fired in the derived model will
     *             be just forwarded. This method throws an
     *             CanceledExecutionException if the user pressed cancel during
     *             execution. Even if the derived model doesn't check, the
     *             result will be discarded and the exception thrown.
     * @throws IllegalStateException If the number of <code>DataTable</code>
     *             objects returned by the derived <code>NodeModel</code> does
     *             not match the number of outputs. Or if any of them is null.
     * @see #execute(BufferedDataTable[],ExecutionContext)
     */
    protected final BufferedDataTable[] executeModel(
            final BufferedDataTable[] data, final ExecutionContext exec) 
            throws Exception {
        assert (data != null && data.length == m_nrDataIns);
        assert (exec != null);

        // temp. storage for result of derived model.
        // EXECUTE DERIVED MODEL
        BufferedDataTable[] outData = execute(data, exec);

        // if execution was canceled without exception flying return false
        if (exec.isCanceled()) {
            throw new CanceledExecutionException(
                    "Result discarded due to user " + "cancel");
        }

        // if number of out tables does not match: fail
        if (outData == null || outData.length != m_nrDataOuts) {
            throw new IllegalStateException(
                    "Invalid result. Execution failed, "
                            + "reason: data is null or number "
                            + "of outputs wrong.");
        }

        // check the result, data tables must not be null
        for (int i = 0; i < outData.length; i++) {
            if (outData[i] == null) {
                m_logger.error("execution failed: null data at port: " + i);
                throw new IllegalStateException("Invalid result. "
                        + "Execution failed, reason: data at output " + i
                        + " is null.");
            }
        }
        setExecuted(true);
        return outData;
    } // executeModel(DataTable[],ExecutionMonitor)
    
    /**
     * Sets the isExecuted flag and fires a state change event.
     * @param isExecuted Flag if this node is configured be executed or not.
     */
    final void setExecuted(final boolean isExecuted) {
        assert !(isExecuted && !m_configured);
        if (isExecuted != isExecuted()) {
            // set the state flag to "executed"
            m_executed = isExecuted;
            // and inform all views about the new model
            stateChanged();
        }
    }

    /**
     * Sets the isConfigured flag and fires a state change event.
     * @param isConfigured Flag if this node is configured or not.
     */
    final void setConfigured(final boolean isConfigured) {
        assert !(m_executed && !isConfigured);
        if (isConfigured != isConfigured()) {
            // set the state flag to "configured"
            m_configured = isConfigured;
            stateChanged();
        }
    }

    /**
     * Returns <code>true</code> if this model has been executed otherwise
     * <code>false</code>.
     * 
     * @return <code>true</code> if the node was execute otherwise
     *         <code>false</code>.
     * 
     * @see #executeModel(BufferedDataTable[],ExecutionContext)
     * @see #resetModel()
     */
    final boolean isExecuted() {
        return m_executed;
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
     *            should be interrupted and throws an exeption then. This
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
    protected abstract BufferedDataTable[] execute(
            final BufferedDataTable[] inData, final ExecutionContext exec) 
            throws Exception;

    /**
     * Invokes the abstract <code>#reset()</code> method of the derived model.
     * In additon, this method resets all hilite handlers, and notifies all
     * views about the changes.
     */
    final void resetModel() {
        // reset in derived model
        reset();
        // set state to not executed and not configured
        m_executed = false;
        m_configured = false;
        // reset these property handlers
        resetHiLiteHandlers();
        // and notify all views
        stateChanged();
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
    protected synchronized void stateChanged() {
        for (NodeView view : m_views) {
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
     * <code>NodeView#updateModel(Object)</code> is called and needs to be
     * overriden.
     * 
     * @param arg The argument you want to pass.
     */
    protected final void notifyViews(final Object arg) {
        for (NodeView view : m_views) {
            view.updateModel(arg);
        }
    }

    /**
     * This method is called by the corresponding <code>Node</code> in order
     * to set the <code>HiLiteHandler</code> for the given input port.
     * 
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
    protected void setInHiLiteHandler(final int inIndex,
            final HiLiteHandler hiLiteHdl) {
        assert inIndex >= 0;
        assert hiLiteHdl == hiLiteHdl;
    }

    /**
     * Sets a new <code>HiLiteHandler</code> for the given input.
     * 
     * @param hdl The new <code>HiLiteHandler</code>.
     * @param in The input index.
     * 
     * @see #setInHiLiteHandler(int, HiLiteHandler)
     */
    final void setInHiLiteHandler(final HiLiteHandler hdl, final int in) {
        m_inHiLiteHdls[in] = hdl;
        setInHiLiteHandler(in, hdl);
        stateChanged();
    }

    /**
     * Returns the <code>HiLiteHandler</code> for the given input index.
     * 
     * @param inIndex The input index.
     * @return <code>HiLiteHandler</code> for the given input index (could be
     *         null).
     * @throws IndexOutOfBoundsException If the <code>inIndex</code> is not in
     *             the range of inputs.
     */
    public final HiLiteHandler getInHiLiteHandler(final int inIndex) {
        return m_inHiLiteHdls[inIndex];
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
        if (outIndex < 0 || outIndex >= getNrOuts()) {
            throw new IndexOutOfBoundsException("index=" + outIndex);
        }
        // if we have no inputs we create a new instance (but only one...)
        if (getNrIns() == 0) {
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
        if (getNrDataIns() == 0) {
            for (int i = 0; i < m_inHiLiteHdls.length; i++) {
                if (m_inHiLiteHdls[i] != null) {
                    m_inHiLiteHdls[i].unHiLiteAll();
                }
            }
        }
    }

    /**
     * Is this method "autoexecutable", i.e. should this node be immediately
     * being executed if all predecessors are executed (without any user
     * interaction). By default this value will be <code>false</code>. Change
     * it in the set method if your node is a simple view that doesn't require
     * expensive computation in the execute method.
     * 
     * @return The autoexecutable flag, by default <code>false</code>.
     */
    protected boolean isAutoExecutable() {
        return m_isAutoExecutable;
    }

    /**
     * Set if the node should execute immediately after all predecessors are
     * executed. Some view implementations, such as a plain table view, do not
     * require much computation during the execute, hence, can be conveniently
     * executed right after all predecessors are executed. Set this flag here to
     * indicate that your derived node is of that kind. This method should be
     * called in the constructor of the derived node model.
     * 
     * @param isAutoExecutable <code>true</code> if the node should be
     *            immediately executed when possible, <code>false</code>
     *            otherwise.
     */
    protected final void setAutoExecutable(final boolean isAutoExecutable) {
        m_isAutoExecutable = isAutoExecutable;
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
    final DataTableSpec[] configureModel(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {

        assert inSpecs.length == getNrDataIns();

        DataTableSpec[] copyInSpecs = new DataTableSpec[getNrDataIns()];
        DataTableSpec[] newOutSpecs;

        if (inSpecs != null) {
            System.arraycopy(inSpecs, 0, copyInSpecs, 0, inSpecs.length);
        }
        // make sure we conveniently have TableSpecs.
        // Rather empty ones than null
        for (int i = 0; i < copyInSpecs.length; i++) {
            if (copyInSpecs[i] == null) {
                // replace with an empty data table spec
                copyInSpecs[i] = new DataTableSpec();
            }
        }
        try {
            // CALL CONFIGURE
            newOutSpecs = configure(copyInSpecs);
            // if successful (without exception) set configured flag
            m_configured = true;
            // check if null
            if (newOutSpecs == null) {
                newOutSpecs = new DataTableSpec[m_nrDataOuts];
            }
            // check output data table specs length
            if (newOutSpecs.length != m_nrDataOuts) {
                m_logger.error("Output spec-array length invalid: "
                        + newOutSpecs.length + " <> " + m_nrDataOuts);
                newOutSpecs = new DataTableSpec[m_nrDataOuts];
            }
        } catch (InvalidSettingsException ise) {
            m_configured = false;
            setWarningMessage("Configure failed: " + ise.getMessage());
            throw ise;
        }
        // return the resulting DataTableSpecs from the configure call
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
    protected abstract DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException;

    /**
     * @return the warning message set during execution, null if none.
     */
    protected final String getWarningMessage() {
        return m_warningMessage;
    }

    /**
     * Sets an optional warning message by the implementing node model.
     * 
     * @param warningMessage the warning message to set
     */
    protected final void setWarningMessage(final String warningMessage) {
        m_warningMessage = warningMessage;
    }
}
