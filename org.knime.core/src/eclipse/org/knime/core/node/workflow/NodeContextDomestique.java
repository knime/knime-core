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
 *   Jul 4, 2019 (loki): created
 */
package org.knime.core.node.workflow;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import org.knime.core.node.workflow.NodeContext.ContextObjectSupplier;

/**
 * This class exists to circumvent a domino-fall of classloading and instantiation that occurs when the
 * {@link NodeContext} class is used; see https://knime-com.atlassian.net/browse/AP-12159 for the history to
 * this.
 *
 * @author loki der quaeler
 * @since 4.0
 * @noreference This class is not intended to be referenced by clients.
 */
public class NodeContextDomestique {
    //   !!!!!!!!!!!!!!
    // DO NOT ADD A STATIC INSTANCE OF NodeLogger TO THIS CLASS.
    //   !!!!!!!!!!!!!!
    private static final AtomicBoolean APPLICATION_START_UP_HAS_SATISFACTORILY_CONCLUDED = new AtomicBoolean(false);
    private static final ArrayList<ContextObjectSupplier> PENDING_ADDITIONS = new ArrayList<>();

    /**
     * Registered a new context object suppliers to be used for object retrieval via
     * #{@link NodeContext#getContextObjectForClass(Class)}.
     *
     * @param supplier object to register
     */
    public static void addContextObjectSupplier(final ContextObjectSupplier supplier) {
        if (APPLICATION_START_UP_HAS_SATISFACTORILY_CONCLUDED.get()) {
            NodeContext.addContextObjectSupplier(supplier);
        } else {
            PENDING_ADDITIONS.add(supplier);
        }
    }

    /**
     * This method should be called from the <code>IApplication.start(IApplicationContext)</code> implementation at a
     * point at which references to defining the user's workspace have concluded.
     */
    public static void applicationStartupIsAbleToUseNodeLogger() {
        APPLICATION_START_UP_HAS_SATISFACTORILY_CONCLUDED.set(true);

        for (final ContextObjectSupplier supplier : PENDING_ADDITIONS) {
            NodeContext.addContextObjectSupplier(supplier);
        }

        PENDING_ADDITIONS.clear();
    }
}
