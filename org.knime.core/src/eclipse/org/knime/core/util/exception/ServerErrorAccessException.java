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
 *   6 Dec 2022 (leon.wenzler): created
 */
package org.knime.core.util.exception;

import org.knime.core.node.util.CheckUtils;

/**
 * Exception that is thrown when a HTTP request receives a server error response: 5XX
 *
 * @author Leon Wenzler, KNIME AG, Konstanz, Germany
 * @noreference This class is not intended to be referenced by clients.
 * @since 5.0
 */
public class ServerErrorAccessException extends HttpResourceAccessException { // NOSONAR
    private static final long serialVersionUID = -4967431437259925173L;

    /**
     * @param message Message detailing the cause for access failure.
     * @param statusCode Status code of the response.
     */
    public ServerErrorAccessException(final String message, final int statusCode) {
        super(message, statusCode);
        validateStatus(statusCode);
    }

    /**
     * @param message Further explanation.
     * @param cause Cause for access failure.
     * @param statusCode Status code of the response.
     */
    public ServerErrorAccessException(final String message, final Throwable cause, final int statusCode) {
        super(message != null ? message : cause.getMessage(), cause, statusCode);
        validateStatus(statusCode);
    }

    private static void validateStatus(final int statusCode) {
        CheckUtils.checkArgument(500 <= statusCode && statusCode <= 599,
            "Status codes for server error exceptions must be from the 5XX family, got: %d", statusCode);
    }
}
