/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
 * University of Konstanz, Germany
 * KNIME.com, Zurich, Switzerland
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
 * ---------------------------------------------------------------------
 *
 * History
 *   02.04.2009 (ohl): created
 */
package org.knime.core.node.workflow;

import javax.swing.JPanel;

/**
 * Implements the content of a view to the {@link NodeExecutionJob}. The
 * corresponding {@link NodeExecutionJobManager} creates an instance of this.
 *
 * @author ohl, KNIME.com, Zurich, Switzerland
 */
public abstract class NodeExecutionJobManagerViewPanel extends JPanel {

    /**
     * Called when the view is about to open.
     */
    public abstract void onOpen();

    /**
     * Called when the view is about to close and shut down. The panel should
     * be unregistered with all lists so it can be disposed of after this call.
     */
    public abstract void onClose();

    /**
     * Called when the underlying node is reset.
     */
    public abstract void reset();

}
