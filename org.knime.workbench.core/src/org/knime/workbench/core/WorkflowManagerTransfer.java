/* ------------------------------------------------------------------
  * This source code, its documentation and all appendant files
  * are protected by copyright law. All rights reserved.
  *
  * Copyright, 2008 - 2011
  * KNIME.com, Zurich, Switzerland
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
  * History
  *   May 20, 2011 (morent): created
  */

package org.knime.workbench.core;

import java.util.List;

import org.eclipse.gef.dnd.SimpleObjectTransfer;
import org.knime.core.node.workflow.WorkflowManager;

/**
 * Is used to transfer meta node templates between the workflow editor and
 * KNIME spaces.
 *
 * @author Dominik Morent, KNIME.com, Zurich, Switzerland
 */
public final class WorkflowManagerTransfer extends SimpleObjectTransfer {
    private static final String TYPE_NAME
            = "workflow-manager-transfer-format"
                + (new Long(System.currentTimeMillis())).toString();

    private static final WorkflowManagerTransfer INSTANCE
            = new WorkflowManagerTransfer();

    private static final int TYPEID = registerType(TYPE_NAME);

    /**
     * Only the singleton instance of this class may be used.
     */
    private WorkflowManagerTransfer() {
        // do nothing
    }

    /**
     * Returns the singleton.
     *
     * @return the singleton
     */
    public static WorkflowManagerTransfer getTransfer() {
        return INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected int[] getTypeIds() {
        return new int[] {TYPEID};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String[] getTypeNames() {
        return new String[] {TYPE_NAME};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("rawtypes")
    public void setObject(final Object obj) {
        if (obj instanceof List) {
           for (Object item : (List)obj) {
               if (!(item instanceof WorkflowManager)) {
                   throw new IllegalArgumentException("Only a List of "
                           + "WorkflowManager can be set as object.");
               }
           }
           super.setObject(obj);
        } else {
            throw new IllegalArgumentException("Only a List of "
                    + "WorkflowManager can be set as object.");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public List<WorkflowManager> getObject() {
        return (List<WorkflowManager>)super.getObject();
    }
}
