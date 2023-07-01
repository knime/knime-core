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
 *   Jul 2, 2023 (hornm): created
 */
package org.knime.core.node.port;

import java.io.Closeable;

import org.knime.core.data.model.PortObjectCell;

/**
 * Provides contextual information which might be required to serialize a {@link PortObject} (and/or the
 * {@link PortObjectSpec}).
 *
 * When setting the context it must be unset again as soon as the serialization is done, ideally in an
 * try-resource-block:
 *
 * <pre>
 * try (var psc = PortSerializerContext.set(INTENT.WRAP_INTO_TABLE_CELL)) {
 *     PortUtil.writeObjectToStream(cell.m_content, (OutputStream)output, new ExecutionMonitor());
 * }
 * </pre>
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 * @since 5.1
 */
public final class PortSerializerContext {

    private static final ThreadLocal<PortSerializerContext> CONTEXT = new ThreadLocal<>();

    /**
     * The intention of why a port object (spec) is being serialized.
     */
    public enum INTENT {

            /**
             * If the port (spec) is to be serialized in order to be transferred to another JVM (e.g. in order to call
             * another workflow).
             */
            COPY_TO_OTHER_JVM,

            /**
             * If the port (spec) is to be serialized in order to be wrapped into a {@link PortObjectCell} which is then
             * put into a data table (usually in order to aggregate multiple port-objects into a table column)
             */
            WRAP_INTO_TABLE_CELL,

            /** If the intent for the serialization is not known. */
            UNKNOWN

    }

    private final INTENT m_intent;

    private PortSerializerContext(final INTENT intent) {
        m_intent = intent;
    }

    /**
     * Sets the context for the current thread initialized with the given intent.
     *
     * @param intent
     * @return a closable to be used to unset the context again (i.e. equivalent to calling {@link #unset()}).
     * @throws IllegalStateException if there is already a context set
     */
    public static Closeable set(final INTENT intent) {
        if (CONTEXT.get() != null) {
            throw new IllegalStateException("PortSerializerContext already set");
        }
        CONTEXT.set(new PortSerializerContext(intent));
        return () -> unset();
    }

    /**
     * Unsets the currently set context for this thread.
     */
    public static void unset() {
        CONTEXT.remove();
    }

    /**
     * @return the {@link INTENT} for the port serialization
     */
    public static INTENT getIntent() {
        var c = CONTEXT.get();
        if (c != null && c.m_intent != null) {
            return c.m_intent;
        }
        return INTENT.UNKNOWN;
    }

}
