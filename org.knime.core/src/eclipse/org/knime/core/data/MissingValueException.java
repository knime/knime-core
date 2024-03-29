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
 *   Apr 9, 2019 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.core.data;

import java.util.Optional;

import org.knime.core.node.util.CheckUtils;

/**
 * This exception is thrown if a missing value is encountered and can't be dealt with. It can be caught on a higher
 * level to better indicate to the user where the missing value occurred.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @since 4.0
 */
public class MissingValueException extends RuntimeException {

    /**
     * Message for the convenience constructor that only receives the missing value as argument.
     */
    private static final String DEFAULT_MSG = "Encountered unsupported missing value.";

    private final String m_error;

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    /**
     * Constructs a <code>MissingvalueException</code> for the provided
     * {@link MissingValue}. </br>
     * The exception message defaults to "Encountered unsupported missing value.".
     * @param missingValue the missing value that caused this exception
     */
    public MissingValueException(final MissingValue missingValue) {
        this(missingValue, DEFAULT_MSG);
    }

    /**
     * Constructs a <code>MissingvalueException</code> for the provided
     * {@link MissingValue} with the specified detail message.
     * Use a helpful message here as it might be displayed to the user.
     *
     * @param missingValue the missing value that caused this exception
     * @param msg the detail message
     */
    public MissingValueException(final MissingValue missingValue, final String msg) {
        super(msg);
        CheckUtils.checkNotNull(missingValue);
        m_error = missingValue.getError();
    }

    /**
     * Constructs a <code>MissingvalueException</code> with the specified
     * detail message in the case where a missing value can't be accessed.
     * Use a helpful message here as it might be displayed to the user.
     *
     * @param msg the reason for the missing value
     * @since 5.2
     */
    public MissingValueException(final String msg) {
        super(msg);
        m_error = msg;
    }

    /**
     * Constructs a <code>MissingvalueException</code> for the provided
     * {@link MissingValue} with the specified cause.
     *
     * @param missingValue the missing value that caused this exception
     * @param cause the original cause of the exception
     */
    public MissingValueException(final MissingValue missingValue, final Throwable cause) {
        super(cause);
        CheckUtils.checkNotNull(missingValue);
        m_error = missingValue.getError();
    }

    /**
     * Constructs a <code>MissingvalueException</code> for the provided
     * {@link MissingValue} with the specified cause and detail message.
     * Use a helpful message here as it might be displayed to the user.
     *
     * @param missingValue the missing value that caused this exception
     * @param msg the detail message
     * @param cause the original cause of the exception
     */
    public MissingValueException(final MissingValue missingValue, final String msg, final Throwable cause) {
        super(msg, cause);
        CheckUtils.checkNotNull(missingValue);
        m_error = missingValue.getError();
    }

    /**
     * @return the error message explaining why the cell is missing or an empty {@link Optional} if no error message is
     *         available
     */
    public Optional<String> getError() {
        return Optional.ofNullable(m_error);
    }

}
