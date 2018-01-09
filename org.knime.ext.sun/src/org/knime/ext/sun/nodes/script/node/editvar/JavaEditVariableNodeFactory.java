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
package org.knime.ext.sun.nodes.script.node.editvar;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;
import org.knime.ext.sun.nodes.script.settings.JavaScriptingCustomizer;
import org.knime.ext.sun.nodes.script.settings.JavaScriptingSettings;
import org.knime.ext.sun.nodes.script.settings.JavaSnippetType.JavaSnippetDoubleType;
import org.knime.ext.sun.nodes.script.settings.JavaSnippetType.JavaSnippetIntType;
import org.knime.ext.sun.nodes.script.settings.JavaSnippetType.JavaSnippetStringType;

/**
 *
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 */
public final class JavaEditVariableNodeFactory extends
        NodeFactory<JavaEditVariableNodeModel> {

    private final JavaScriptingCustomizer m_customizer;

    /**
     *
     */
    public JavaEditVariableNodeFactory() {
        m_customizer = new JavaScriptingCustomizer() {
            /** {@inheritDoc} */
            @Override
            public JavaScriptingSettings createSettings() {
                JavaScriptingSettings s = super.createSettings();
                s.setArrayReturn(false);
                // not applicable anyway
                s.setInsertMissingAsNull(false);
                return s;
            }
        };
        m_customizer.setShowColumnList(false);
        m_customizer.setShowGlobalDeclarationList(false);
        m_customizer.setOutputIsVariable(true);
        m_customizer.setShowArrayReturn(false);
        m_customizer.setShowInsertMissingAsNull(false);
        m_customizer.setReturnTypes(JavaSnippetIntType.INSTANCE,
                JavaSnippetDoubleType.INSTANCE, JavaSnippetStringType.INSTANCE);
    }

    /** {@inheritDoc} */
    @Override
    public JavaEditVariableNodeModel createNodeModel() {
        return new JavaEditVariableNodeModel(m_customizer);
    }

    /** {@inheritDoc} */
    @Override
    protected int getNrNodeViews() {
        return 0;
    }

    /** {@inheritDoc} */
    @Override
    public NodeView<JavaEditVariableNodeModel> createNodeView(final int viewIndex,
            final JavaEditVariableNodeModel nodeModel) {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    protected boolean hasDialog() {
        return true;
    }

    /** {@inheritDoc} */
    @Override
    protected NodeDialogPane createNodeDialogPane() {
        return new JavaEditVariableNodeDialogPane(m_customizer);
    }

}
