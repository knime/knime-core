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
