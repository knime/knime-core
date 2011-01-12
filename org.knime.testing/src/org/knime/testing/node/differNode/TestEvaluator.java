/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2011
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
 *   May 19, 2006 (ritmeier): created
 */
package org.knime.testing.node.differNode;

import org.knime.core.data.DataTable;
import org.knime.core.node.InvalidSettingsException;


/**
 * 
 * @author ritmeier, University of Konstanz
 */
public interface TestEvaluator {

    /**
     * Compares the result of the workflow with a golden DataTable.
     * 
     * @param goldenTable - what the result should be
     * @param workflowResult - what the result was
     * @return - true if the workflowresult matches the golden DataTable
     * @throws InvalidSettingsException
     */
    public void compare(DataTable goldenTable, DataTable workflowResult)
            throws TestEvaluationException;

}
