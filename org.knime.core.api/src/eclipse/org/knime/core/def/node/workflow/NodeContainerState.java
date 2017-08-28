/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 * Created on Apr 24, 2013 by wiswedel
 */
package org.knime.core.def.node.workflow;

/**
 * Possible status values of a NodeContainer. The actual implementation is an enum but is hidden to client
 * (e.g. GUI) code so that new states can be added as needed.
 *
 * @author Bernd Wiswedel, Michael Berthold, KNIME.com, Zurich, Switzerland
 * @since 2.8
 */
public interface NodeContainerState {

    /**
     * @return true if node is idle (unconfigured, not queued, not marked).
     */
    public boolean isIdle();

    /**
     * @return true if node is configured (not queued, not marked).
     */
    public boolean isConfigured();

    /**
     * @return true if node is executed and not marked for re-execution.
     */
    public boolean isExecuted();

    /**
     * @return true if node is executing or waiting to be executed (marked, queued, executing)
     */
    public boolean isExecutionInProgress();

    /** @return true if node is currently executing (not queued, not marked but executing) and it's using a
     * non-default job manager. If so (SGE executor), the workflow can be saved without setting it dirty (SGE executor
     * can restore running jobs)
     */
    public boolean isExecutingRemotely();

    /**
     * @return true if node is waiting to be executed (marked or queued).
     */
    public boolean isWaitingToBeExecuted();

    /** @return false if node is executing or queued */
    public boolean isHalted();

}
