/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright (c) KNIME.com, Zurich, Switzerland
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.com
 * email: contact@knime.com
 * ---------------------------------------------------------------------
 *
 * Created: May 17, 2011
 * Author: ohl
 */
package org.knime.workbench.ui.navigator;

import org.knime.core.node.workflow.WorkflowManager;

/**
 * Hackaround to avoid cyclic dependencies. The navigator needs to ask the
 * editor for its workflow manager. It does it through this adapter.
 *
 * @author ohl, University of Konstanz
 */
public class WorkflowEditorAdapter {
    private final WorkflowManager m_wfm;

    public WorkflowEditorAdapter(final WorkflowManager wfm) {
        m_wfm = wfm;
    }

    /**
     *
     */
    public WorkflowManager getWorkflowManager() {
        return m_wfm;
    }
}
