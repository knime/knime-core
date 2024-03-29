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
 *   Oct 8, 2019 (hornm): created
 */
package org.knime.core.node.workflow.changes;

import org.knime.core.node.workflow.NodeContainer;

/**
 * Tracks changes of a {@link NodeContainer} and their (potential) children.
 *
 * Pending API! Do not use!
 *
 * Logic will be obsolete once the eclipse-gef-based workflow editor is not supported anymore.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 * @since 4.1
 * @noreference This class is not intended to be referenced by clients.
 * @noextend This interface is not intended to be extended by clients.
 */
public final class ChangesTracker {

    private boolean m_hasNodeStateChanges;

    private boolean m_hasOtherChanges;

    /**
     *
     */
    public ChangesTracker() {
        clearChanges();
    }

    /**
     * Clears all the changes tracked so far.
     */
    public void clearChanges() {
        m_hasNodeStateChanges = false;
        m_hasOtherChanges = false;
    }

    /**
     * Indicates a node state change to be tracked.
     */
    public void nodeStateChange() {
        m_hasNodeStateChanges = true;
    }

    /**
     * Indicates another change that is not a node state change to be tracked.
     */
    public void otherChange() {
        m_hasOtherChanges = true;
    }

    /**
     * @return the tracked changes
     */
    public TrackedChanges getTrackedChanges() {
        return new TrackedChanges() {

            @Override
            public boolean hasNodeStateChanges() {
                return m_hasNodeStateChanges;
            }

            @Override
            public boolean hasOtherChanges() {
                return m_hasOtherChanges;
            }
        };
    }
}
