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
 *   Oct 3, 2010 (wiswedel): created
 */
package org.knime.ext.sun.nodes.script.settings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 *
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 */
public class JavaScriptingCustomizer {

    private boolean m_showColumnList = true;
    private boolean m_showGlobalDeclarationList = true;
    private boolean m_showOutputPanel = true;
    private boolean m_showOutputTypePanel = true;
    private Collection<JavaSnippetType<?, ?, ?>> m_returnTypes =
        Arrays.asList(JavaSnippetType.TYPES);
    private boolean m_outputIsVariable = false;
    private boolean m_showArrayReturn = true;
    private boolean m_showInsertMissingAsNull = true;

    /** @return the showColumnList */
    public boolean getShowColumnList() {
        return m_showColumnList;
    }
    /** @param showColumnList the showColumnList to set */
    public void setShowColumnList(final boolean showColumnList) {
        m_showColumnList = showColumnList;
    }
    /** @return the showGlobalDeclarationList */
    public boolean getShowGlobalDeclarationList() {
        return m_showGlobalDeclarationList;
    }
    /** @param showGlobalDeclarationList the showGlobalDeclarationList to set */
    public void setShowGlobalDeclarationList(
            final boolean showGlobalDeclarationList) {
        m_showGlobalDeclarationList = showGlobalDeclarationList;
    }
    /** @return the showOutputPanel */
    public boolean isShowOutputPanel() {
        return m_showOutputPanel;
    }
    /** @param showOutputPanel the showOutputPanel to set */
    public void setShowOutputPanel(final boolean showOutputPanel) {
        m_showOutputPanel = showOutputPanel;
    }
    /** @return the showOutputTypePanel */
    public boolean getShowOutputTypePanel() {
        return m_showOutputTypePanel;
    }
    /** @param showOutputTypePanel the showOutputTypePanel to set */
    public void setShowOutputTypePanel(final boolean showOutputTypePanel) {
        m_showOutputTypePanel = showOutputTypePanel;
    }
    /** @return the returnTypes */
    public Collection<JavaSnippetType<?, ?, ?>> getReturnTypes() {
        return m_returnTypes;
    }
    /** @param returnTypes the returnTypes to set */
    public void setReturnTypes(
            final JavaSnippetType<?, ?, ?>... returnTypes) {
        List<JavaSnippetType<?, ?, ?>> asList = Arrays.asList(returnTypes);
        if (returnTypes == null || asList.contains(null)) {
            throw new NullPointerException("Argument must not be null.");
        }
        Set<JavaSnippetType<?, ?, ?>> unique =
            new HashSet<JavaSnippetType<?, ?, ?>>(asList);
        if (unique.size() < returnTypes.length) {
            throw new IllegalArgumentException("No duplicates allowed");
        }
        m_returnTypes = new ArrayList<JavaSnippetType<?, ?, ?>>(asList);
    }
    /** @return the outputIsVariable */
    public boolean getOutputIsVariable() {
        return m_outputIsVariable;
    }
    /** @param outputIsVariable the outputIsVariable to set */
    public void setOutputIsVariable(final boolean outputIsVariable) {
        m_outputIsVariable = outputIsVariable;
    }
    /** @return the showArrayReturn */
    public boolean getShowArrayReturn() {
        return m_showArrayReturn;
    }
    /** @param showArrayReturn the showArrayReturn to set */
    public void setShowArrayReturn(final boolean showArrayReturn) {
        m_showArrayReturn = showArrayReturn;
    }

    /** @return the showInsertMissingAsNull */
    public boolean getShowInsertMissingAsNull() {
        return m_showInsertMissingAsNull;
    }

    /** @param showInsertMissingAsNull the showInsertMissingAsNull to set */
    public void setShowInsertMissingAsNull(
            final boolean showInsertMissingAsNull) {
        m_showInsertMissingAsNull = showInsertMissingAsNull;
    }
    /** Factory method for settings. Sub-classes can hard-code settings here.
     * @return A new settings object.
     */
    public JavaScriptingSettings createSettings() {
        return new JavaScriptingSettings(this);
    }

}
