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
 *   May 25, 2023 (Leon Wenzler, KNIME AG, Konstanz, Germany): created
 */
package org.knime.core.node;

import java.io.Writer;
import java.util.NoSuchElementException;

import org.apache.log4j.Layout;
import org.knime.core.node.NodeLogger.LEVEL;
import org.knime.core.node.logging.KNIMELogger;
import org.knime.core.util.Pair;

/**
 * All configuration-relevant NodeLogger functionality. Includes adding and removing writers,
 * getting and setting log level ranges, and translating between KNIME and Log4J Level enums.
 *
 * @author Leon Wenzler, KNIME AG, Konstanz, Germany
 * @since 5.1
 */
public final class NodeLoggerConfig {


    /**
     * Adds a new {@link java.io.Writer} with the given level to this logger.
     *
     * @param writer The writer to add.
     * @param minLevel The minimum level to output.
     * @param maxLevel The maximum level to output.
     */
    public static void addKNIMEConsoleWriter(final Writer writer, final LEVEL minLevel, final LEVEL maxLevel) {
        KNIMELogger.addKNIMEConsoleWriter(writer, minLevel, maxLevel);
    }

    /**
     * Adds a new {@link java.io.Writer} with the given level to this logger.
     *
     * @param writer The writer to add.
     * @param layout the log file layout to use
     * @param minLevel The minimum level to output.
     * @param maxLevel The maximum level to output.
     */
    public static void addWriter(final Writer writer, final Layout layout, final LEVEL minLevel, final LEVEL maxLevel) {
        KNIMELogger.addWriter(writer, layout, minLevel, maxLevel);
    }

    /**
     * Removes the previously added {@link java.io.Writer} from the logger.
     *
     * @param writer The Writer to remove.
     */
    public static void removeWriter(final Writer writer) {
        KNIMELogger.removeWriter(writer);
    }

    // -- GLOBAL LOG LEVEL GETTER AND SETTER --

    /**
     * Sets an new minimum logging level for all internal appenders, that are, log file, and <code>System.out</code> and
     * <code>System.err</code> appender. The maximum logging level stays <code>LEVEL.ALL</code> for all appenders.
     *
     * @param level new minimum logging level
     * @deprecated use {@link #setAppenderLevelRange(String, LEVEL, LEVEL)} instead for more fine-grained control
     */
    @Deprecated
    public static void setLevel(final LEVEL level) {
        KNIMELogger.setLevel(level);
    }

    /**
     * Returns the minimum and maximum log level for a given appender name.
     * If the log level range has not been specified, returns null.
     *
     * @param appenderName Name of the appender.
     * @return Pair of (minLogLevel, maxLogLevel).
     * @deprecated Use {@link #getAppenderLevelRange(String)}
     */
    @Deprecated
    public static Pair<LEVEL, LEVEL> getAppenderLevelRange(final String appenderName) {
        return KNIMELogger.getAppenderLevelRange(appenderName);
    }

    /**
     * Sets a level range filter on the given appender.
     *
     * @param appenderName the name of the appender
     * @param min the minimum logging level
     * @param max the maximum logging level
     * @throws NoSuchElementException if the given appender does not exist
     * @deprecated use {@link NodeLogger#setAppenderLevelRange(String, LEVEL, LEVEL)}
     */
    @Deprecated
    public static void setAppenderLevelRange(final String appenderName, final LEVEL min, final LEVEL max)
        throws NoSuchElementException {
        KNIMELogger.setAppenderLevelRange(appenderName, min, max);
    }

    /**
     * Hides the constructor.
     */
    private NodeLoggerConfig() {
    }
}
