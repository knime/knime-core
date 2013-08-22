/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
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
 * ---------------------------------------------------------------------
 *
 * History
 *   18.05.2012 (meinl): created
 */
package org.knime.testing.core.ng;

import junit.framework.Test;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;

/**
 * Abstract base class for workflow tests.
 *
 * @author Thorsten Meinl, University of Konstanz
 * @since 2.9
 */
public abstract class WorkflowTest implements Test {
    /**
     * The workflow's name.
     */
    protected final String m_workflowName;

    /**
     * The progress monitor, never <code>null</code>.
     */
    protected final IProgressMonitor m_progressMonitor;

    /**
     * The test context, never <code>null</code>.
     */
    protected final WorkflowTestContext m_context;

    /**
     * Creates a new workflow test for the given workflow.
     *
     * @param workflowName the workflow's name
     * @param monitor progress monitor, may be <code>null</code>
     * @param context the test context, must not be <code>null</code>
     */
    protected WorkflowTest(final String workflowName, final IProgressMonitor monitor, final WorkflowTestContext context) {
        m_workflowName = workflowName;
        if (monitor == null) {
            m_progressMonitor = new NullProgressMonitor();
        } else {
            m_progressMonitor = monitor;
        }
        if (context == null) {
            throw new IllegalArgumentException("Test context must not be null");
        }
        m_context = context;
    }

    /**
     * Returns the name of the workflow that is being tested (including the full path).
     *
     * @return the workflow's name
     */
    public final String getWorkflowName() {
        return m_workflowName;
    }

    /**
     * Returns a name for the specific test.
     *
     * @return a name
     */
    public abstract String getName();

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return getWorkflowName() + " - " + getName();
    }

    /**
     * This methods is called when the whole test suite starts but before the first test is run. Subclassed may override
     * this method in order to initialize things.
     */
    public void aboutToStart() {
        // do nothing by default
    }
}
