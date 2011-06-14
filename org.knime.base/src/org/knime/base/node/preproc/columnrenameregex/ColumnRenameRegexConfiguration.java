/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
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
 *  propagated with or for interoperation with KNIME. The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 *
 */
package org.knime.base.node.preproc.columnrenameregex;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/** Settings proxy for the node.
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
final class ColumnRenameRegexConfiguration {

    private String m_searchString;
    private String m_replaceString;
    private boolean m_isCaseInsensitive;
    private boolean m_isLiteral;

    /** @return the searchString */
    String getSearchString() {
        return m_searchString;
    }
    /** @param searchString the searchString to set */
    void setSearchString(final String searchString) {
        m_searchString = searchString;
    }
    /** @return the replaceString */
    String getReplaceString() {
        return m_replaceString;
    }
    /** @param replaceString the replaceString to set */
    void setReplaceString(final String replaceString) {
        m_replaceString = replaceString;
    }

    /** @return the isCaseInsensitive */
    boolean isCaseInsensitive() {
        return m_isCaseInsensitive;
    }
    /** @param isCaseInsensitive the isCaseInsensitive to set */
    void setCaseInsensitive(final boolean isCaseInsensitive) {
        m_isCaseInsensitive = isCaseInsensitive;
    }
    /** @return the isLiteral */
    boolean isLiteral() {
        return m_isLiteral;
    }
    /** @param isLiteral the isLiteral to set */
    void setLiteral(final boolean isLiteral) {
        m_isLiteral = isLiteral;
    }
    /** Save config to argument.
     * @param settings To save to. */
    void saveConfiguration(final NodeSettingsWO settings) {
        if (m_searchString != null) {
            settings.addString("searchString", m_searchString);
            settings.addString("replaceString", m_replaceString);
            settings.addBoolean("isCaseInsensitive", m_isCaseInsensitive);
            settings.addBoolean("isLiteral", m_isLiteral);
        }
    }

    /** Load config in model.
     * @param settings To load from.
     * @throws InvalidSettingsException If that fails.
     */
    void loadSettingsInModel(final NodeSettingsRO settings)
        throws InvalidSettingsException {
        m_searchString = settings.getString("searchString");
        m_replaceString = settings.getString("replaceString");
        m_isCaseInsensitive = settings.getBoolean("isCaseInsensitive");
        m_isLiteral = settings.getBoolean("isLiteral");
        toSearchPattern();
    }

    /** Load config in dialog.
            return Pattern.compile(
     * @param settings To load from.
     */
    void loadSettingsInDialog(final NodeSettingsRO settings) {
        m_searchString = settings.getString("searchString", "(.+)");
        m_replaceString = settings.getString("replaceString", "prefix_$1");
        m_isCaseInsensitive = settings.getBoolean("isCaseInsensitive", false);
        m_isLiteral = settings.getBoolean("isLiteral", false);
    }

    /** Creates a pattern from the current settings.
     * @return A new pattern.
     * @throws InvalidSettingsException
     *          If that fails due to a {@link PatternSyntaxException}. */
    Pattern toSearchPattern() throws InvalidSettingsException {
        try {
            int flags = 0;
            if (m_isCaseInsensitive) {
                flags = flags | Pattern.CASE_INSENSITIVE;
            }
            if (m_isLiteral) {
                flags = flags | Pattern.LITERAL;
            }
            return Pattern.compile(m_searchString, flags);
        } catch (PatternSyntaxException e) {
            throw new InvalidSettingsException(
                    "Invalid search pattern: " + e.getMessage(), e);
        }

    }

}
