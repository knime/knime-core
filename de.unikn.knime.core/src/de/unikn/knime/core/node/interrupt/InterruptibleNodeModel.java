/* @(#)$RCSfile$ 
 * $Revision: 2187 $ $Date: 2006-05-26 13:47:51 +0200 (Fr, 26 Mai 2006) $ 
 * $Author: dill $
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
 * History
 *   11.10.2005 (Fabian Dill): created
 */
package de.unikn.knime.core.node.interrupt;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import de.unikn.knime.core.data.DataTable;
import de.unikn.knime.core.data.container.DataContainer;
import de.unikn.knime.core.node.BufferedDataTable;
import de.unikn.knime.core.node.CanceledExecutionException;
import de.unikn.knime.core.node.ExecutionMonitor;
import de.unikn.knime.core.node.InvalidSettingsException;
import de.unikn.knime.core.node.NodeLogger;
import de.unikn.knime.core.node.NodeModel;
import de.unikn.knime.core.node.NodeSettings;

/**
 * This class provides a generic implementation of a node that can be stopped
 * and resumed during execution. Therefore a derived class has to provide a
 * method which realises one iteration of the algorithm that should be processed
 * interruptible.
 * 
 * @author Fabian Dill, University of Konstanz
 */
public abstract class InterruptibleNodeModel extends NodeModel {
    
    /**
     * A constant to define the inital delay, so the algorithm and the slider
     * have the same inital values.
     */
    public static final int INITIAL_DELAY = 10;
    
    /**
     * A constant defining how long the execute thread should sleep, while
     * waiting to resume again.
     */
    public static final int SLEEPING_MILLIS = 500;
    
    private static final NodeLogger LOGGER = NodeLogger
    .getLogger(InterruptibleNodeModel.class);
    
    private volatile boolean m_pause;
    
    private volatile boolean m_finished;
    
    private int m_iterationCounter = 0;
    
    private DataTable[] m_inData;
    
    private int m_delay = INITIAL_DELAY;
    
    private static final String FILE_NAME = "interruptibleInput";
    
    private static final String INTERN_CFG_KEY = "interruptibleInternSettings";
    private static final String INTERN_CFG_ITERATION = "iteration";
    private static final String INTERN_CFG_FINIS = "finished";
    
    
    /**
     * Constructs a NodeModel with the desired in- and outports, setting the
     * state to paused, that is waiting for an intial event to start executing.
     * 
     * @param nrInPorts - the desired number of inports.
     * @param nrOutPorts - the desired number of outports.
     */
    public InterruptibleNodeModel(final int nrInPorts, final int nrOutPorts) {
        super(nrInPorts, nrOutPorts);
        m_pause = true;
        m_finished = false;
    }
    
    /**
     * Constructs a NodeModel with the desired in- and outports, and model
     * in- and outports. State is paused, waiting for an intial event to start
     * executing. Be sure to override load/save ModelContent in your 
     * derived NodeModel.
     * 
     * @param nrInPorts - the desired number of inports.
     * @param nrOutPorts - the desired number of outports.
     * @param nrPredParamsIns The number of <code>ModelContent</code>
     *            elements available as inputs.
     * @param nrPredParamsOuts The number of <code>ModelContent</code>
     *            objects available at the output.
     */
    public InterruptibleNodeModel(final int nrInPorts, final int nrOutPorts,
            final int nrPredParamsIns, final int nrPredParamsOuts) {
        super(nrInPorts, nrOutPorts, nrPredParamsIns, nrPredParamsOuts);
        m_pause = true;
        m_finished = false;
    }
    
    /**
     * Returns the number of proessed iterations so far.
     * 
     * @return - the number of processed iterations so far.
     */
    public synchronized int getNumberOfIterations() {
        return m_iterationCounter;
    }
    
    /**
     * Increments the iteration counter.
     */
    public synchronized void incrementIterationCounter() {
        m_iterationCounter++;
    }
    
    /**
     * Resets the iteration counter to zero again.
     * 
     */
    public void resetIterationCounter() {
        m_iterationCounter = 0;
    }
    
    /**
     * Sets the delay, that is the number of iterations until the view is
     * refreshed. Setting the delay to n, every n-th iteration the view is
     * refreshed.
     * 
     * @param delay - the number of iterations until the view is refreshed.
     */
    public synchronized void setDelay(final int delay) {
        this.m_delay = delay;
    }
    
    /**
     * Returns the current delay, that is the number of iterations until the
     * view is refreshed.
     * 
     * @return - the current delay.
     */
    public synchronized int getDelay() {
        return m_delay;
    }
    
    /**
     * Indicates whether the execution of the algorithm is paused or not.
     * 
     * @return - true, if the execution pauses, false otherwise.
     */
    public synchronized boolean isPaused() {
        return m_pause;
    }
    
    /**
     * Sets the status of the execution to paused or not. By setting it to 
     * pause = true the execution will be paused after finishing the current 
     * iteration, setting it to pause = false causes the execution to resume 
     * after at most waiting SLEEPING_MILLIS milliseconds.
     * 
     * @param b - true, if the execition should pause, false if the execution
     *            should be resumed.
     */
    public synchronized void pause(final boolean b) {
        m_pause = b;
        if (!m_pause) {
            notify();
        }
    }
    
    /**
     * Indicates whether the state of the algorithm is finished or not.
     * 
     * @return - true, if the algorithm is finished, false otherwise.
     */
    public synchronized boolean isFinished() {
        return m_finished;
    }
    
    /**
     * Forces the algorithm to finish its execution. If the algorithm is
     * currently running, the current iteration will be finished gracefully.
     */
    public synchronized void finish() {
        m_finished = true;
        notify();
    }
    
    /**
     * Sets the status of the execution to be finished or not.
     * 
     * @param finish - true, if the execution is finished, false otherwise.
     */
    public synchronized void setFinish(final boolean finish) {
        m_finished = finish;
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
    public DataTable[] getInputData() {
        return m_inData;
    }
    
    /**
     * Initialises the NodeModel, starts the execution, pauses it, resumes it
     * and finishes it. At the end the from the derived NodeModel provided
     * output data is set to the output.
     * 
     * @see de.unikn.knime.core.node.NodeModel#execute(
     *  BufferedDataTable[],
     *  de.unikn.knime.core.node.ExecutionMonitor)
     */
    @Override
    public final BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionMonitor exec) throws Exception {
        m_inData = inData;
        init(inData);
        try {
            while (!isFinished()) {
                exec.checkCanceled();
                if (isFinished()) {
                    LOGGER.debug(Thread.currentThread().getName()
                            + " says: 'bye, bye...'");
                    break;
                }
                if (isPaused()) {
                    while (isPaused() && !isFinished()) {
                        exec.checkCanceled();
                        try {
                            synchronized (this) {
                                while (isPaused() && !isFinished()) {
                                    wait();
                                }
                            }
                            // Thread.sleep(SLEEPING_MILLIS);
                        } catch (InterruptedException ie) {
                            ie.printStackTrace();
                            LOGGER.debug("interrupted while sleeping");
                            break;
                        }
                    }
                } else if (!isPaused()) {
                    synchronized (this) {
                        // LOGGER.debug("execute one iteration");
                        executeOneIteration();
                        incrementIterationCounter();
                        if (getNumberOfIterations() % m_delay == 0) {
                            // LOGGER.debug("notify views at iteration nr.: "
                            // + getNumberOfIterations());
                            notifyViews(this);
                        }
                    }
                }
            }
        } catch (Exception e) {
            finish();
            e.printStackTrace();
            throw new Exception(e);
        }
        LOGGER.debug(this.getClass().getSimpleName()
                + " says: 'I'm down and out...'");
        return getOutput();
    }
    
    /**
     * Resets the status of the Node to paused and not finished so it can be
     * triggered to start again.
     * 
     * @see de.unikn.knime.core.node.NodeModel#reset()
     */
    @Override
    protected void reset() {
        pause(true);
        setFinish(false);
        resetIterationCounter();
    }
    
    
    /**
     * @see de.unikn.knime.core.node.NodeModel#loadInternals(java.io.File, 
     * de.unikn.knime.core.node.ExecutionMonitor)
     */
    @Override
    protected void loadInternals(final File nodeInternDir, 
            final ExecutionMonitor exec) 
    throws IOException, CanceledExecutionException {
        m_inData = new DataTable[getNrDataIns()];
        for (int i = 0; i < getNrDataIns(); i++) {
            File f = new File(nodeInternDir, FILE_NAME + i);
            m_inData[i] = DataContainer.readFromZip(f);
        }  
        File f = new File(nodeInternDir, FILE_NAME);
        FileInputStream fis = new FileInputStream(f);
        NodeSettings internalSettings = NodeSettings.loadFromXML(fis);
        try {
        m_finished = internalSettings.getBoolean(INTERN_CFG_FINIS);
        m_iterationCounter = internalSettings.getInt(INTERN_CFG_ITERATION);
        } catch (InvalidSettingsException ise) {
            LOGGER.warn(ise.getMessage());
            throw new IOException(ise.getMessage());
        }
    }

    /**
     * @see de.unikn.knime.core.node.NodeModel#saveInternals(
     * java.io.File, de.unikn.knime.core.node.ExecutionMonitor)
     */
    @Override
    protected void saveInternals(final File nodeInternDir, 
            final ExecutionMonitor exec) 
        throws IOException, CanceledExecutionException {
        for (int i = 0; i < m_inData.length; i++) {
            File f = new File(nodeInternDir, FILE_NAME + i);
            DataContainer.writeToZip(m_inData[i], f, exec);
        }
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
     */
    public abstract void executeOneIteration();
    
    /**
     * Do here the initialisation of the model. This method is called before
     * starting the execute method.
     * 
     * @param inData - the incoming DataTables at the moment the execute method
     *            starts.
     * @throws InvalidSettingsException - if the inData doesn't fit the expected
     *             configuration.
     */
    public abstract void init(final DataTable[] inData)
    throws InvalidSettingsException;
    
    /**
     * Is called at the end of the execute method when it is finished and the
     * data should be available. Note that the returned DataTable[] is directly
     * returned from the execute method, so mind the restrictions on the
     * DataTable[] as for the execute method.
     * 
     * @see de.unikn.knime.core.node.NodeModel#execute(BufferedDataTable[],
     *      ExecutionMonitor)
     * @return - an BufferedDataTable[] as should be returned from the 
     *      NodeModel's execute method.
     */
    public abstract BufferedDataTable[] getOutput();
    
}
