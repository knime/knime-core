/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
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
 *   11.10.2005 (Fabian Dill): created
 */
package org.knime.core.node.interrupt;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.knime.core.data.DataTable;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;

/**
 * This class provides a generic implementation of a node that can be stopped
 * and resumed during execution. Therefore a derived class has to provide a
 * method which realizes one iteration of the algorithm that should be processed
 * interruptible.
 * 
 * @author Fabian Dill, University of Konstanz
 */
public abstract class InterruptibleNodeModel extends NodeModel {

    /**
     * A constant to define the initial delay, so the algorithm and the slider
     * have the same initial values.
     */
    public static final int INITIAL_DELAY = 10;

    /**
     * A constant defining how long the execute thread should sleep, while
     * waiting to resume again.
     */
    public static final int SLEEPING_MILLIS = 500;

    private static final NodeLogger LOGGER =
            NodeLogger.getLogger(InterruptibleNodeModel.class);

    private volatile boolean m_pause;

    private volatile boolean m_finished;

    private int m_iterationCounter = 0;

    private int m_iterationsToDo = 0;

    private boolean m_runAnyway = false;

    private BufferedDataTable[] m_inData;

    private int m_delay = INITIAL_DELAY;

    private final Object m_lock = new Object();

    private static final String FILE_NAME = "interruptibleInput";

    private static final String INTERN_CFG_KEY = "interruptibleInternSettings";

    private static final String INTERN_CFG_ITERATION = "iteration";

    private static final String INTERN_CFG_FINIS = "finished";

    /**
     * Constructs a NodeModel with the desired in- and out-ports, setting the
     * state to paused, that is waiting for an initial event to start executing.
     * 
     * @param nrInPorts - the desired number of in-ports.
     * @param nrOutPorts - the desired number of out-ports.
     */
    public InterruptibleNodeModel(final int nrInPorts, final int nrOutPorts) {
        super(nrInPorts, nrOutPorts);
        m_pause = true;
        m_finished = false;
    }

    /**
     * Returns the number of processed iterations so far.
     * 
     * @return - the number of processed iterations so far.
     */
    public int getNumberOfIterations() {
        return m_iterationCounter;
    }
    
        
    /**
     * Sets the delay, that is the number of iterations until the view is
     * refreshed. Setting the delay to n, every n-th iteration the view is
     * refreshed.
     * 
     * @param delay - the number of iterations until the view is refreshed.
     */
    public void setDelay(final int delay) {
        this.m_delay = delay;
    }

    /**
     * Returns the current delay, that is the number of iterations until the
     * view is refreshed.
     * 
     * @return - the current delay.
     */
    public int getDelay() {
        return m_delay;
    }

    /**
     * Indicates whether the execution of the algorithm is paused or not.
     * 
     * @return - true, if the execution pauses, false otherwise.
     */
    public boolean isPaused() {
        return m_pause;
    }

    /**
     * Causes the execution to pause until either {@link #next(int)} or
     * {@link #run()} is called.
     * 
     */
    public void pause() {
        synchronized (m_lock) {
            m_pause = true;
            m_runAnyway = false;
            m_iterationsToDo = 0;
            if (!m_pause) {
                m_lock.notify();
            }
        }
    }

    /**
     * Indicates whether the state of the algorithm is finished or not.
     * 
     * @return - true, if the algorithm is finished, false otherwise.
     */
    public boolean isFinished() {
        return m_finished;
    }

    /**
     * Forces the algorithm to finish its execution. If the algorithm is
     * currently running, the current iteration will be finished gracefully.
     */
    public void finish() {
        synchronized (m_lock) {
            m_finished = true;
            m_lock.notify();
        }
    }

    /**
     * Causes the execution of the next (n) iteration(s).
     * 
     * @param numberOfIterations number of iterations to perform
     */
    public void next(final int numberOfIterations) {
        synchronized (m_lock) {
            m_iterationsToDo = numberOfIterations;
            m_pause = false;
            m_runAnyway = false;
            m_lock.notify();
        }
    }

    /**
     * Causes the node to run for an unlimited number of iterations.
     * 
     */
    public void run() {
        synchronized (m_lock) {
            m_runAnyway = true;
            m_pause = false;
            m_iterationsToDo = 0;
            m_lock.notify();
        }
    }

    /**
     * Provides access to the input data the node is passed to at the beginning
     * of the execute method.
     * 
     * @param portNr - the referring inport number
     * @return - the DataTable that was connected to port portNr at the
     *         beginning of the execute method, null if no input data is
     *         available.
     */
    public DataTable getInputData(final int portNr) {
        if (m_inData != null) {
            return m_inData[portNr];
        }
        return null;
    }

    /**
     * Returns the input data that was connected to the node at the beginning of
     * the execute method as a whole.
     * 
     * @return - the input data as a whole.
     */
    public BufferedDataTable[] getInputData() {
        return m_inData;
    }

    /**
     * Initialises the NodeModel, starts the execution, pauses it, resumes it
     * and finishes it. At the end the from the derived NodeModel provided
     * output data is set to the output.
     * 
     * {@inheritDoc}
     */
    @Override
    public final BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        if (isFinished()) {
            m_finished = false;
        }
        m_inData = inData;
        init(inData, exec);
        notifyViews(this);
        try {
            // perform iterations until the node is finished
            while (!isFinished()) {
                // in case the the node is paused or there are not iterations
                // to do and the node should not run anyway, wait until
                // the state changes
                synchronized (m_lock) {
                    while (!isFinished()
                            && (isPaused() || m_iterationsToDo <= 0
                                    && !m_runAnyway)) {

                        m_pause = true;
                        // if the status was changed we have to update the views
                        notifyViews(this);
                        // don't catch InterruptedException
                        // if thrown it will be caught in the catch block
                        // below where the finish() method is called
                        m_lock.wait();
                    }
                }
                // check whether the previous loop has been left
                // due to a "finish" event
                // if not perform next iteration, where the finish status is
                // checked anyway
                if (!isFinished()) {
                    executeOneIteration(exec);
                    m_iterationCounter++;
                    m_iterationsToDo--;
                    if (m_delay == 0 || m_iterationCounter % m_delay == 0) {
                        notifyViews(this);
                    }
                }
            }
        } catch (Exception e) {
            finish();
            throw new Exception(e);
        }
        LOGGER.debug(this.getClass().getSimpleName()
                + " says: 'I'm down and out...'");
        return getOutput(exec);
    }

    /**
     * Resets the status of the Node to paused and not finished so it can be
     * triggered to start again.
     * 
     * @see org.knime.core.node.NodeModel#reset()
     */
    @Override
    protected void reset() {
        m_pause = true;
        m_finished = false;
        m_runAnyway = false;
        m_iterationsToDo = 0;
        m_iterationCounter = 0;
        notifyViews(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        File f = new File(nodeInternDir, FILE_NAME);
        FileInputStream fis = new FileInputStream(f);
        NodeSettingsRO internalSettings = NodeSettings.loadFromXML(fis);
        try {
            m_finished = internalSettings.getBoolean(INTERN_CFG_FINIS);
            m_iterationCounter = internalSettings.getInt(INTERN_CFG_ITERATION);
        } catch (InvalidSettingsException ise) {
            LOGGER.warn(ise.getMessage());
            throw new IOException(ise.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        NodeSettings internalSettings = new NodeSettings(INTERN_CFG_KEY);
        internalSettings.addInt(INTERN_CFG_ITERATION, m_iterationCounter);
        internalSettings.addBoolean(INTERN_CFG_FINIS, m_finished);
        File f = new File(nodeInternDir, FILE_NAME);
        FileOutputStream fos = new FileOutputStream(f);
        internalSettings.saveToXML(fos);
    }


    /**
     * This method is assumed to implement one iteration of an interruptible
     * algorithm. Most of the data mining algorithm typically run in several
     * for- or while-clauses until they meet any stopping criteria. Here the
     * control is different, since here the user is able to start, resume and
     * stop the algorithm. (If there is any natural stopping criteria for even
     * the interruptible algorithm call the finish()-method when it is met). In
     * any case implement in this method the content of the for- or
     * while-clause.
     * 
     * @param exec the ExecutionContext to cancel the operation or show the
     *            progress.
     * @throws CanceledExecutionException if the operation is canceled by the
     *             user.
     */
    public abstract void executeOneIteration(final ExecutionContext exec)
            throws CanceledExecutionException;

    /**
     * Do here the initialisation of the model. This method is called before
     * starting the execute method.
     * 
     * @param inData - the incoming DataTables at the moment the execute method
     *            starts.
     * @param exec to show the progress of the initialization or to cancel it.
     * @throws CanceledExecutionException if the operation is canceled by the
     *             user.
     * @throws InvalidSettingsException - if the inData doesn't fit the expected
     *             configuration.
     */
    public abstract void init(final BufferedDataTable[] inData,
            ExecutionContext exec) throws CanceledExecutionException,
            InvalidSettingsException;

    /**
     * Is called at the end of the execute method when it is finished and the
     * data should be available. Note that the returned DataTable[] is directly
     * returned from the execute method, so mind the restrictions on the
     * DataTable[] as for the execute method.
     * 
     * @param exec The execution monitor to show the progress.
     * @return - an BufferedDataTable[] as should be returned from the
     *         NodeModel's execute method.
     * @throws CanceledExecutionException If writing output tables has been
     *             canceled.
     * 
     */
    public abstract BufferedDataTable[] getOutput(final ExecutionContext exec)
            throws CanceledExecutionException;

}
