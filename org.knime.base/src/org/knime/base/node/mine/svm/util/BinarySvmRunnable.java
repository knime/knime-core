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
