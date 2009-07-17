/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
 * University of Konstanz, Germany
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
 *   14.07.2009 (wiswedel): created
 */
package org.knime.core.node.config;

import java.util.EventObject;

/**
 * Event that is fired when the settings associated with the nodes in a
 * {@link ConfigEditTreeModel} change.
 * @author Bernd Wiswedel, University of Konstanz
 */
public final class ConfigEditTreeEvent extends EventObject {

    private final String m_exposeVariableName;
    private final String m_useVariable;
    private final String[] m_keyPath;

    /** Creates new event. The source and the keyPath arguments must not be
     * null.
     * @param eventSource The source of the event (the tree model).
     * @param keyPath The path of the node that has changed.
     * @param useVariable The new variable used to overwrite the setting.
     * @param exposeVariableName The newly defined variable to expose the value.
     *
     */
    ConfigEditTreeEvent(final Object eventSource, final String[] keyPath,
            final String useVariable, final String exposeVariableName) {
        super(eventSource);
        if (keyPath == null) {
            throw new NullPointerException();
        }
        m_keyPath = keyPath;
        m_useVariable = useVariable;
        m_exposeVariableName = exposeVariableName;
    }

    /**
     * @return The newly defined variable to expose the value (or null).
     */
    public String getExposeVariableName() {
        return m_exposeVariableName;
    }

    /**
     * @return The new variable used to overwrite the setting (or null).
     */
    public String getUseVariable() {
        return m_useVariable;
    }

    /**
     * @return The path of the node that has changed.
     */
    public String[] getKeyPath() {
        return m_keyPath;
    }

}
