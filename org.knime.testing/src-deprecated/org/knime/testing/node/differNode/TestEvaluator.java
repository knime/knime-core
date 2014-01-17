/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright by 
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
import org.knime.testing.internal.nodes.image.ImageDifferNodeFactory;


/**
 *
 * @author ritmeier, University of Konstanz
 * @deprecated use the new image comparator {@link ImageDifferNodeFactory} and the extension point for difference
 *             checker instead
 */
@Deprecated
public interface TestEvaluator {

    /**
     * Compares the result of the workflow with a golden DataTable.
     *
     * @param goldenTable - what the result should be
     * @param workflowResult - what the result was
     */
    void compare(DataTable goldenTable, DataTable workflowResult)
            throws TestEvaluationException;
}
