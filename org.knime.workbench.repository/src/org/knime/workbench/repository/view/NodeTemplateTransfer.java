/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
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
 *   12.01.2005 (Florian Georg): created
 */
package org.knime.workbench.repository.view;

import org.eclipse.swt.dnd.ByteArrayTransfer;
import org.eclipse.swt.dnd.TransferData;

import org.knime.workbench.repository.model.NodeTemplate;

/**
 * Implements a SWT transfer class for use in Drag&Drop operations.
 * 
 * @author Florian Georg, University of Konstanz
 */
public final class NodeTemplateTransfer extends ByteArrayTransfer {
    private static final NodeTemplateTransfer INSTANCE;
    static {
        INSTANCE = new NodeTemplateTransfer();
    }

    /**
     * This is the register transfer type name.
     */
    protected static final String TYPE_NAME = "repository-transfer-format";

    /**
     * This is the ID that is registered to the name.
     */
    protected static final int TYPE_ID = registerType(TYPE_NAME);

    /**
     * This records the time at which the transfer data was recorded.
     */
    private long m_startTime;

    /**
     * This records the data being transferred.
     */
    private NodeTemplate m_object;

    private NodeTemplateTransfer() {
        // singleton constructor
    }

    /**
     * @return The shared singleton instance
     */
    public static synchronized NodeTemplateTransfer getInstance() {
        return INSTANCE;
    }

    /**
     * @return This returns the transfer ids that this agent supports.
     */
    @Override
    protected int[] getTypeIds() {
        return new int[] {TYPE_ID};
    }

    /**
     * @return This returns the transfer names that this agent supports.
     */
    @Override
    public String[] getTypeNames() {
        return new String[] {TYPE_NAME};
    }

    /**
     * This records the object and current time and encodes only the current
     * time into the transfer data.
     * 
     * @param object The NodeTemplate object
     * @param transferData The transfer data
     */
    @Override
    public void javaToNative(final Object object,
            final TransferData transferData) {
        m_startTime = System.currentTimeMillis();
        this.m_object = (NodeTemplate) object;
        if (transferData != null) {
            super.javaToNative(String.valueOf(m_startTime).getBytes(),
                    transferData);
        }
    }

    /**
     * This decodes the time of the transfer and returns the recorded the object
     * if the recorded time and the decoded time match.
     * 
     * @param transferData The data to transfer
     * @return The object
     */
    @Override
    public Object nativeToJava(final TransferData transferData) {
        byte[] bytes = (byte[]) super.nativeToJava(transferData);
        if (bytes == null) {
            return null;
        }

        try {
            long startTime = Long.valueOf(new String(bytes)).longValue();
            return this.m_startTime == startTime ? m_object : null;
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}
