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
 */
package org.knime.base.node.mine.svm.util;

import org.knime.base.node.mine.svm.Svm;
import org.knime.base.node.mine.svm.kernel.Kernel;
import org.knime.base.node.mine.svm.learner.SvmAlgorithm;
import org.knime.core.node.ExecutionMonitor;

/**
 * Utility class to run a Binary SVM learning process.
 * 
 * @author cebron, University of Konstanz
 */
public class BinarySvmRunnable implements Runnable {
    
    private SvmAlgorithm m_svmAlgo;
    
    private Exception m_exception;
    
    private ExecutionMonitor m_exec;
    
    private Svm m_svm;
    
    /**
     * @param inputData the input data to train with
     * @param positiveClass the positive class value
     * @param kernel the kernel to use
     * @param paramC overlapping penalty to use
     * @param exec the execution process to report to
     */
    public BinarySvmRunnable(final DoubleVector[] inputData,
            final String positiveClass, 
            final Kernel kernel, final double paramC,
            final ExecutionMonitor exec) {
        m_svmAlgo = new SvmAlgorithm(inputData, positiveClass, kernel, paramC);
        m_exception = null;
        m_exec = exec;
    }
    
    /**
     * {@inheritDoc}
     */
    public void run() {
        try {
            m_svm = m_svmAlgo.run(m_exec);
        } catch (Exception e) {
            m_exception = e;
        }
    }
    
    /**
     * @throws Exception if something went wrong.
     */
    public void ok() throws Exception {
        if (m_exception != null) {
            throw m_exception;
        }
    }
    
    /**
     * @return the trained Support Vector Machine.
     */
    public Svm getSvm() {
        return m_svm;
    }
}
