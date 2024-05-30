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
 *   Feb 26, 2019 (Mark Ortmann, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.core.data.container;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.Buffer.MemorizeIfSmallLifecycle;
import org.knime.core.data.container.Buffer.SoftRefLRULifecycle;
import org.knime.core.data.container.storage.TableStoreFormat;
import org.knime.core.data.container.storage.TableStoreFormatRegistry;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.util.CheckUtils;

/**
 * The buffer settings. Solely used for benchmarking.
 *
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 * @noreference This class is not intended to be referenced by clients.
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
public final class BufferSettings {

    /** The node logger for this class. */
    private static final NodeLogger LOGGER = NodeLogger.getLogger(BufferSettings.class);

    /**
     * The default for whether to use the {@link SoftRefLRULifecycle} as opposed to the
     * {@link MemorizeIfSmallLifecycle}.
     */
    static final String DEF_TABLE_CACHE = "LRU";

    /** The default number of tables that can be kept in the soft-references LRU cache before being weak-referenced. */
    static final int DEF_LRU_CACHE_SIZE = 32;

    /** The enable LRU caching flag. */
    private final boolean m_enableLRU;

    /** The LRU cache size. */
    private final int m_lruCacheSize;

    /** The output table store format. */
    private final TableStoreFormat m_outputFormat;

    /**
     * Default constructor.
     */
    BufferSettings(final BufferSettingsBuilder bufferSettingsBuilder) {
        m_enableLRU = bufferSettingsBuilder.m_enableLRU;
        m_lruCacheSize = bufferSettingsBuilder.m_lruCacheSize;
        m_outputFormat = bufferSettingsBuilder.m_outputFormat;
    }

    /**
     * Initializes the LRU caching flag w.r.t. the defined properties.
     *
     * @return the LRU caching flag
     */
    private static boolean initLRU() {
        final String valTableCache = System.getProperty(KNIMEConstants.PROPERTY_TABLE_CACHE);
        if (valTableCache != null) {
            switch (valTableCache.trim().toUpperCase()) {
                case "LRU":
                    return true;
                case "SMALL":
                    return false;
                default:
                    LOGGER.warn("Unknown setting for table caching: " + valTableCache + ". Using default: "
                        + DEF_TABLE_CACHE + ".");
            }
        }
        return DEF_TABLE_CACHE.equals("LRU");
    }

    /**
     * Returns whether to use LRU caching or not.
     *
     * @return flag indicating whether to use LRU caching or not
     */
    public boolean useLRU() {
        return m_enableLRU;
    }

    /**
     * Returns the the LRU cache size.
     *
     * @return the LRU cache size
     */
    int getLRUCacheSize() {
        return m_lruCacheSize;
    }

    /**
     * Returns the {@link TableStoreFormat} used to read and write the {@link Buffer Buffer's} content.
     *
     * @param spec the spec of the table to read/write
     * @return the {@link TableStoreFormat}
     */
    TableStoreFormat getOutputFormat(final DataTableSpec spec) {
        if (m_outputFormat.accepts(spec)) {
            LOGGER.debugWithFormat("Using table format %s", m_outputFormat.getClass().getName());
            return m_outputFormat;
        }
        final TableStoreFormat storeFormat = TableStoreFormatRegistry.getInstance().getFormatFor(spec);
        LOGGER.debugWithFormat(
            "Cannot use table format '%s' as it does not support the table schema, " + "using '%s' instead",
            m_outputFormat.getClass().getName(), storeFormat.getClass().getName());
        return storeFormat;
    }

    /**
     * Returns the default {@link BufferSettings}.
     *
     * @return the default {@code BufferSettings}.
     */
    public static BufferSettings getDefault() {
        return builder().build();
    }

    static BufferSettingsBuilder builder() {
        return new BufferSettingsBuilder();
    }

    /**
     * @since 5.3
     * @noreference This class is not intended to be referenced by clients.
     */
    public static final class BufferSettingsBuilder {

        /** The enable LRU caching flag. */
        private boolean m_enableLRU;

        /** The LRU cache size. */
        private int m_lruCacheSize;

        /** The output table store format. */
        private TableStoreFormat m_outputFormat;

        private BufferSettingsBuilder() {
            m_enableLRU = initLRU();
            m_lruCacheSize = DEF_LRU_CACHE_SIZE;
            m_outputFormat = TableStoreFormatRegistry.getInstance().getInstanceTableStoreFormat();
        }

        /**
         * Changes the enable LRU caching flag.
         *
         * @param enableLRU the new enable LRU caching flag
         * @return this
         */
        public BufferSettingsBuilder withLRU(final boolean enableLRU) {
            m_enableLRU = enableLRU;
            return this;
        }

        /**
         * Changes the LRU cache size.
         *
         * @param lruCacheSize the new LRU cache size
         * @return this
         */
        public BufferSettingsBuilder withLRUCacheSize(final int lruCacheSize) {
            m_lruCacheSize = lruCacheSize;
            return this;
        }

        /**
         * Changes the table store format.
         *
         * @param outputFormat the new table store format
         * @return this
         */
        public BufferSettingsBuilder withOutputFormat(final TableStoreFormat outputFormat) {
            m_outputFormat = CheckUtils.checkArgumentNotNull(outputFormat);
            return this;
        }

        BufferSettings build() {
            return new BufferSettings(this);
        }

    }

}
