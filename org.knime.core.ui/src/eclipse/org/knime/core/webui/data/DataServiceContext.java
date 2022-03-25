/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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
 *   22 Mar 2022 (marcbux): created
 */
package org.knime.core.webui.data;

import java.util.ArrayList;
import java.util.List;

import org.knime.core.webui.data.json.JsonInitialDataService;
import org.knime.core.webui.data.rpc.json.JsonRpcDataService;

/**
 * A {@link DataServiceContext} allows to report warning messages during a data service invocation or assembly of
 * initial data. These warning messages can then be obtained by the {@link JsonRpcDataService} or
 * {@link JsonInitialDataService} and passed to the frontend for display along a valid result.
 *
 * Marc Bux, KNIME GmbH, Berlin, Germany
 */
public final class DataServiceContext {

    private static final ThreadLocal<DataServiceContext> CONTEXTS = ThreadLocal.withInitial(DataServiceContext::new);

    /**
     * @return the {@link DataServiceContext} for the current thread., potentially creating a new one in the process
     */
    public static DataServiceContext getContext() {
        return CONTEXTS.get();
    }

    private final List<String> m_warningMessages = new ArrayList<>();

    /**
     * Adds another warning message to the list of warning messages.
     *
     * @param warningMessage a warning message
     */
    public void addWarningMessage(final String warningMessage) {
        m_warningMessages.add(warningMessage);
    }

    /**
     * @return a list of warnings that occurred while invoking the data service
     */
    public String[] getWarningMessages() {
        return m_warningMessages.toArray(new String[0]);
    }

    /**
     * Clears the current list of warning messages and resets the current threads context.
     */
    public void clear() {
        m_warningMessages.clear();
        CONTEXTS.remove();
    }

}
