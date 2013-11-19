/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
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
 *   12.08.2013 (thor): created
 */
package org.knime.testing.core;

import java.util.List;

import org.knime.core.data.DataColumnProperties;
import org.knime.core.data.DataValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.DialogComponent;

/**
 * Interface used by the difference checker node. Subclasses can implement more relaxed difference checks than pure 1:1
 * equality.
 *
 * @param <T> value type that this checker handles
 * @author Thorsten Meinl, KNIME.com, Zurich, Switzerland
 * @since 2.9
 */
public interface DifferenceChecker<T extends DataValue> {
    /**
     * Interface for the result of a difference check.
     */
    final class Result {
        private final String m_message;

        /**
         * Creates a positive result, i.e. {@link #ok()} will return <code>true</code>.
         */
        public Result() {
            m_message = null;
        }

        /**
         * Creates a negative result i.e. {@link #ok()} will return <code>false</code>. A message explaining the
         * negative result must be supplied.
         *
         * @param message an explanation for the negative result
         */
        public Result(final String message) {
            if (message == null) {
                throw new IllegalAccessError("Message must not be null");
            }
            m_message = message;
        }

        /**
         * Returns whether the check is OK, i.e. the two value are equal (in the sense of the checker).
         *
         * @return <code>true</code> if the check is OK, false otherwise
         */
        public boolean ok() {
            return m_message == null;
        }

        /**
         * Returns a message explaining the differences in case {@link #ok()} returns <code>false</code>. In case
         * {@link #ok()} is <code>true</code>, null may be returned.
         *
         * @return an optional message or <code>null</code>
         */
        public String getMessage() {
            return m_message;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return ok() ? "OK" : "Not OK: " + m_message;
        }
    }

    /**
     * Constant result for successful checks, i.e. {@link Result#ok()} is <code>true</code>.
     */
    Result OK = new Result();

    /**
     * Checks whether the two values are "equal" in the sense of this checker.
     *
     * @param expected the expected value
     * @param got the actual value
     * @return the result of the comparison
     */
    Result check(T expected, T got);

    /**
     * Loads the settings for this checker from the given node settings.
     *
     * @param settings a node settings object
     * @throws InvalidSettingsException if an expected setting is missing
     */
    void loadSettings(NodeSettingsRO settings) throws InvalidSettingsException;

    /**
     * Loads the settings for this checker from the given node settings, using default values for missing settings.
     *
     * @param settings a node settings object
     */
    void loadSettingsForDialog(NodeSettingsRO settings);

    /**
     * Saves the settings for this checker into the given node settings.
     *
     * @param settings a node settings object
     */
    void saveSettings(NodeSettingsWO settings);

    /**
     * Validates the settings for this checker.
     *
     * @param settings a node settings object
     * @throws InvalidSettingsException if an expected setting is missing
     */
    void validateSettings(NodeSettingsRO settings) throws InvalidSettingsException;


    /**
     * Returns a (possibly empty) list of dialog components for this checker's options.
     *
     * @return a list with {@link DialogComponent}s, never <code>null</code>
     */
    List<? extends DialogComponent> getDialogComponents();

    /**
     * Returns a short description for this checker.
     *
     * @return a description, never <code>null</code>
     */
    String getDescription();

    /**
     * Returns whether the data column spec properties (see {@link DataColumnProperties}) should be ignored while
     * comparing a column from two tables.
     *
     * @return <code>true</code> if the properties should be ignored, <code>false</code> otherwise
     */
    boolean ignoreColumnProperties();
}
