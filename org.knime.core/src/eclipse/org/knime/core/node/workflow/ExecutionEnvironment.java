/*
 * ------------------------------------------------------------------------
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
 * Created on May 13, 2013 by Berthold
 */
package org.knime.core.node.workflow;

import org.knime.core.node.interactive.ViewContent;

/** Interface for objects defining the environment nodes will be executed in.
 * Provides information about re-execution, ViewContents to be loaded (and
 * in the future also FileStoreHandlers and other environment information.)
 *
 * @author M. Berthold
 * @since 2.8
 */
public final class ExecutionEnvironment {

    /** @since 2.12 */
    public static final ExecutionEnvironment DEFAULT = new ExecutionEnvironment();

    private final boolean m_reExecute;
    private final Object m_preReExecData;
    private final boolean m_useAsNewDefault;

    /** Default constructor: no re-execution, don't preload ViewContent.
     */
    public ExecutionEnvironment() {
        m_reExecute = false;
        m_preReExecData = null;
        m_useAsNewDefault = false;
    }

    /** Setup default environment with new parameters.
     *
     * @param reExecute flag indicating if nodes is to be re-executed.
     * @param preExecVC view content to be loaded into node before execution
     * @since 2.10
     * @deprecated use the more generic {@link #ExecutionEnvironment(boolean, Object, boolean)} instead
     */
    @Deprecated(since = "4.5")
    public ExecutionEnvironment(final boolean reExecute, final ViewContent preExecVC, final boolean useAsNewDefault) {
        this(reExecute, (Object) preExecVC, useAsNewDefault);
    }

    /**
     * Setup default environment with new parameters.
     *
     * @param reExecute flag indicating if node is to be re-executed
     * @param preReExecData data to be provided to the node prior re-execution
     * @param useAsNewDefault whether the provided data shall be used a new default
     * @since 4.5
     */
    public ExecutionEnvironment(final boolean reExecute, final Object preReExecData, final boolean useAsNewDefault) {
        m_reExecute = reExecute;
        m_preReExecData = preReExecData;
        m_useAsNewDefault = useAsNewDefault;
    }

    /**
     * @return true if this is a re-execution.
     */
    public boolean reExecute() {
        return m_reExecute;
    }

    /**
     * @return {@link ViewContent} to be loaded before (re)execution.
     * @throws IllegalStateException if the pre-reexecution data is not of type {@link ViewContent}
     * @deprecated use the more generic {@link #getPreReExecuteData()} instead
     */
    @Deprecated(since = "4.5")
    public ViewContent getPreExecuteViewContent() {
        if (m_preReExecData instanceof ViewContent) {
            return (ViewContent)m_preReExecData;
        } else {
            throw new IllegalStateException("Data provided for the node for re-execution is not of type ViewContent");
        }
    }

    /**
     * @return data that is provided to a node prior its re-execution
     * @since 4.5
     */
    public Object getPreReExecuteData() {
        return m_preReExecData;
    }

    /**
     * @return the useAsNewDefault
     * @since 2.10
     */
    public boolean getUseAsDefault() {
        return m_useAsNewDefault;
    }

}
