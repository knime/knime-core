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
 *   02.04.2012 (hofer): created
 */
package org.knime.base.node.jsnippet.template;

import java.awt.Component;
import java.util.Map;

import org.knime.base.node.jsnippet.util.JSnippetTemplate;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.workflow.FlowVariable;

/**
 * The default implementation of TemplateController. It provides methods to get
 * a preview and to replace the setting of a java snippet by the settings of a
 * template.
 * <p>This class might change and is not meant as public API.
 *
 * @author Heiko Hofer
 * @param <T> the {@link JSnippetTemplate} implementation
 * @since 2.12
 * @noextend This class is not intended to be subclassed by clients.
 * @noinstantiate This class is not intended to be instantiated by clients.
 * @noreference This class is not intended to be referenced by clients.
 */
public class DefaultTemplateController<T extends JSnippetTemplate> implements TemplateController<T> {
    private TemplateNodeDialog<T> m_model;
    private TemplateNodeDialog<T> m_preview;
    private DataTableSpec m_spec;
    private Map<String, FlowVariable> m_flowVariables;


    /**
     * Create a new instance.
     * @param model the dialog that serves as a model in the MVC principle
     * @param preview the dialog used for preview of the template
     */
    public DefaultTemplateController(final TemplateNodeDialog<T> model,
            final TemplateNodeDialog<T> preview) {
        super();
        m_model = model;
        m_preview = preview;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Component getPreview() {
        return m_preview.getPanel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setPreviewSettings(final T template) {
        m_preview.applyTemplate(template, m_spec, m_flowVariables);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setSettings(final T template) {
        m_model.applyTemplate(template, m_spec, m_flowVariables);
    }

    /**
     * Set the spec used for for the preview and applying a template to
     * the model.
     * @param spec the spec of the input
     */
    public void setDataTableSpec(final DataTableSpec spec) {
        m_spec = spec;

    }

    /**
     * Set the flow variables used for for the preview and applying a
     * template to the model.
     * @param flowVariables the flow variables
     */
    public void setFlowVariables(
            final Map<String, FlowVariable> flowVariables) {
        m_flowVariables = flowVariables;
    }

}
