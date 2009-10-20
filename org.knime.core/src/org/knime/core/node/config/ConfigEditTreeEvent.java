/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2009
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
