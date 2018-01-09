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
 * ------------------------------------------------------------------------
 *
 * History
 *   30.03.2012 (hofer): created
 */
package org.knime.base.node.jsnippet.util;

/**
 * Simple container to hold errors and warnings retrieved by validating a
 * JavaSnippet.
 * <p>This class might change and is not meant as public API.
 *
 * @author Heiko Hofer
 * @since 2.12
 * @noextend This class is not intended to be subclassed by clients.
 * @noinstantiate This class is not intended to be instantiated by clients.
 * @noreference This class is not intended to be referenced by clients.
 */
public class ValidationReport {
    private String[] m_errors;
    private String[] m_warnings;

    /**
     * Creates a new report object.
     *
     * @param errors the errors
     * @param warnings the warnings
     */
    public ValidationReport(final String[] errors, final String[] warnings) {
        super();
        m_errors = errors;
        m_warnings = warnings;
    }

    /**
     * @return the errors
     */
    public String[] getErrors() {
        return m_errors;
    }

    /**
     * @param errors the errors to set
     */
    public void setErrors(final String[] errors) {
        m_errors = errors;
    }

    /**
     * @return the warnings
     */
    public String[] getWarnings() {
        return m_warnings;
    }

    /**
     * @param warnings the warnings to set
     */
    public void setWarnings(final String[] warnings) {
        m_warnings = warnings;
    }

    /**
     * @return true when there are one or more errors.
     */
    public boolean hasErrors() {
        return m_errors.length > 0;
    }

    /**
     * @return true when there are one or more warnings.
     */
    public boolean hasWarnings() {
        return m_warnings.length > 0;
    }
}
