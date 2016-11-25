/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 *   25 Oct 2016 (albrecht): created
 */
package org.knime.core.node.util.dialog.field;

import org.knime.core.node.util.dialog.field.FieldList.InColumnList;
import org.knime.core.node.util.dialog.field.FieldList.InFlowVariableList;
import org.knime.core.node.util.dialog.field.FieldList.OutColumnList;
import org.knime.core.node.util.dialog.field.FieldList.OutFlowVariableList;

/**
 * This class is a collection of definitions of system fields.
 * A field can be an input or output column or input or output flow variables.
 *
 * <p>This class might change and is not meant as public API.
 *
 * @author Christian Albrecht, KNIME.com GmbH, Konstanz, Germany
 * @author Heiko Hofer
 * @since 3.3
 * @noextend This class is not intended to be subclassed by clients.
 * @noinstantiate This class is not intended to be instantiated by clients.
 * @noreference This class is not intended to be referenced by clients.
 */
public class FieldCollection {

    private InColumnList m_inColumnList;
    private InFlowVariableList m_inFlowVariableList;
    private OutColumnList m_outColumnList;
    private OutFlowVariableList m_outFlowVariableList;

    /**
     * Creates a new collection element with empty field lists.
     */
    public FieldCollection() {
        m_inColumnList = new InColumnList();
        m_inFlowVariableList = new InFlowVariableList();
        m_outColumnList = new OutColumnList();
        m_outFlowVariableList = new OutFlowVariableList();
    }

    /**
     * Creates a new collection element with the given field lists.
     *
     * @param inColumnList the input column list
     * @param inFlowVariableList the input flow variable list
     * @param outColumnList the output column list
     * @param outFlowVariableList the output flow variable list
     */
    public FieldCollection(final InColumnList inColumnList, final InFlowVariableList inFlowVariableList,
            final OutColumnList outColumnList, final OutFlowVariableList outFlowVariableList) {
        m_inColumnList = inColumnList;
        m_inFlowVariableList = inFlowVariableList;
        m_outColumnList = outColumnList;
        m_outFlowVariableList = outFlowVariableList;
    }

    /**
     * @return the inColumnList
     */
    public InColumnList getInColumnList() {
        return m_inColumnList;
    }

    /**
     * @return the inFlowVariableList
     */
    public InFlowVariableList getInFlowVariableList() {
        return m_inFlowVariableList;
    }

    /**
     * @return the outColumnList
     */
    public OutColumnList getOutColumnList() {
        return m_outColumnList;
    }

    /**
     * @return the outFlowVariableList
     */
    public OutFlowVariableList getOutFlowVariableList() {
        return m_outFlowVariableList;
    }

}
