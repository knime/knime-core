/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2009
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
 *   Sep 1, 2008 (wiswedel): created
 */
package org.knime.base.node.preproc.regexsplit;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.StringValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;

/**
 * Settings object for the regex split node model.
 * @author Bernd Wiswedel, University of Konstanz
 */
final class RegexSplitSettings {

    private String m_column;
    private String m_pattern;
    private boolean m_isUnixLines;
    private boolean m_isCaseInsensitive;
    private boolean m_isComments;
    private boolean m_isMultiLine;
    private boolean m_isLiteral;
    private boolean m_isDotAll;
    private boolean m_isUniCodeCase;
    private boolean m_isCanonEQ;
    
    /**
     * Load method for NodeModel.
     * @param settings To load from.
     * @throws InvalidSettingsException If settings invalid.
     */
    void loadSettingsInModel(final NodeSettingsRO settings) 
        throws InvalidSettingsException {
        m_column = settings.getString("column");
        m_pattern = settings.getString("pattern");
        m_isUnixLines = settings.getBoolean("isUnixLines");
        m_isCaseInsensitive = settings.getBoolean("isCaseInsensitive");
        m_isComments = settings.getBoolean("isComments");
        m_isMultiLine = settings.getBoolean("isMultiLine");
        m_isLiteral = settings.getBoolean("isLiteral");
        m_isDotAll = settings.getBoolean("isDotAll");
        m_isUniCodeCase = settings.getBoolean("isUniCodeCase");
        m_isCanonEQ = settings.getBoolean("isCanonEQ");
        compile();
    }

    /** Load model for dialog.
     * @param settings To load from.
     * @param spec Input spec
     * @throws NotConfigurableException If no matching col found
     */
    void loadSettingsInDialog(final NodeSettingsRO settings, 
            final DataTableSpec spec) throws NotConfigurableException {
        String defColumn = null;
        for (DataColumnSpec s : spec) {
            if (s.getType().isCompatible(StringValue.class)) {
                defColumn = s.getName();
            }
        }
        m_column = settings.getString("column", defColumn);
        DataColumnSpec col = spec.getColumnSpec(m_column);
        if (col == null || !col.getType().isCompatible(StringValue.class)) {
            m_column = defColumn;
        }
        m_pattern = settings.getString("pattern", "(.*)");
        m_isUnixLines = settings.getBoolean("isUnixLines", false);
        m_isCaseInsensitive = settings.getBoolean("isCaseInsensitive", false);
        m_isComments = settings.getBoolean("isComments", false);
        m_isMultiLine = settings.getBoolean("isMultiLine", false);
        m_isLiteral = settings.getBoolean("isLiteral", false);
        m_isDotAll = settings.getBoolean("isDotAll", false);
        m_isUniCodeCase = settings.getBoolean("isUniCodeCase", false);
        m_isCanonEQ = settings.getBoolean("isCanonEQ", false);
        if (m_column == null) {
            throw new NotConfigurableException(
                    "No string compatible column in input table");
        }
    }
    
    /**
     * Saves parameters to argument settings.
     * @param settings To save to.
     */
    void saveSettingsTo(final NodeSettingsWO settings) {
        settings.addString("column", m_column);
        settings.addString("pattern", m_pattern);
        settings.addBoolean("isUnixLines", m_isUnixLines);
        settings.addBoolean("isCaseInsensitive", m_isCaseInsensitive);
        settings.addBoolean("isComments", m_isComments);
        settings.addBoolean("isMultiLine", m_isMultiLine);
        settings.addBoolean("isLiteral", m_isLiteral);
        settings.addBoolean("isDotAll", m_isDotAll);
        settings.addBoolean("isUniCodeCase", m_isUniCodeCase);
        settings.addBoolean("isCanonEQ", m_isCanonEQ);
    }
    
    /** Compiles the pattern with the current settings.
     * @return The pattern object
     * @throws InvalidSettingsException If the pattern can't be compiled.
     */
    Pattern compile() throws InvalidSettingsException {
        int flags = getFlags();
        try {
            return Pattern.compile(m_pattern, flags);
        } catch (PatternSyntaxException e) {
            throw new InvalidSettingsException(
                    "Invalid pattern: " + e.getMessage(), e);
        } catch (IllegalArgumentException iae) {
            throw new IllegalArgumentException("Invalid flags in pattern " 
                    + "compilation: " + iae.getMessage(), iae);
        }
    }
    
    private int getFlags() {
        int flags = 0;
        if (m_isUnixLines) {
            flags |= Pattern.UNIX_LINES;
        }
        if (m_isCaseInsensitive) {
            flags |= Pattern.UNIX_LINES;
        }
        if (m_isComments) {
            flags |= Pattern.COMMENTS;
        }
        if (m_isMultiLine) {
            flags |= Pattern.MULTILINE;
        }
        if (m_isLiteral) {
            flags |= Pattern.LITERAL;
        }
        if (m_isDotAll) {
            flags |= Pattern.DOTALL;
        }
        if (m_isUniCodeCase) {
            flags |= Pattern.UNICODE_CASE;
        }
        if (m_isCanonEQ) {
            flags |= Pattern.CANON_EQ;
        }
        return flags;
    }
    
    /**
     * @return the column
     */
    String getColumn() {
        return m_column;
    }

    /**
     * @param column the column to set
     */
    void setColumn(final String column) {
        m_column = column;
    }

    /**
     * @return the pattern
     */
    String getPattern() {
        return m_pattern;
    }

    /**
     * @param pattern the pattern to set
     */
    void setPattern(final String pattern) {
        m_pattern = pattern;
    }

    /**
     * @return the isUnixLines
     */
    boolean isUnixLines() {
        return m_isUnixLines;
    }

    /**
     * @param isUnixLines the isUnixLines to set
     */
    void setUnixLines(final boolean isUnixLines) {
        m_isUnixLines = isUnixLines;
    }

    /**
     * @return the isCaseInsensitive
     */
    boolean isCaseInsensitive() {
        return m_isCaseInsensitive;
    }

    /**
     * @param isCaseInsensitive the isCaseInsensitive to set
     */
    void setCaseInsensitive(final boolean isCaseInsensitive) {
        m_isCaseInsensitive = isCaseInsensitive;
    }

    /**
     * @return the isComments
     */
    boolean isComments() {
        return m_isComments;
    }

    /**
     * @param isComments the isComments to set
     */
    void setComments(final boolean isComments) {
        m_isComments = isComments;
    }

    /**
     * @return the isMultiLine
     */
    boolean isMultiLine() {
        return m_isMultiLine;
    }

    /**
     * @param isMultiLine the isMultiLine to set
     */
    void setMultiLine(final boolean isMultiLine) {
        m_isMultiLine = isMultiLine;
    }

    /**
     * @return the isLiteral
     */
    boolean isLiteral() {
        return m_isLiteral;
    }

    /**
     * @param isLiteral the isLiteral to set
     */
    void setLiteral(final boolean isLiteral) {
        m_isLiteral = isLiteral;
    }

    /**
     * @return the isDotAll
     */
    boolean isDotAll() {
        return m_isDotAll;
    }

    /**
     * @param isDotAll the isDotAll to set
     */
    void setDotAll(final boolean isDotAll) {
        m_isDotAll = isDotAll;
    }

    /**
     * @return the isUniCodeCase
     */
    boolean isUniCodeCase() {
        return m_isUniCodeCase;
    }

    /**
     * @param isUniCodeCase the isUniCodeCase to set
     */
    void setUniCodeCase(final boolean isUniCodeCase) {
        m_isUniCodeCase = isUniCodeCase;
    }

    /**
     * @return the isCanonEQ
     */
    boolean isCanonEQ() {
        return m_isCanonEQ;
    }

    /**
     * @param isCanonEQ the isCanonEQ to set
     */
    void setCanonEQ(final boolean isCanonEQ) {
        m_isCanonEQ = isCanonEQ;
    }

}
