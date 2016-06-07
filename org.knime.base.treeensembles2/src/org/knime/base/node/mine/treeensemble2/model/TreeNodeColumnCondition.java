/*
 * ------------------------------------------------------------------------
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
 * ------------------------------------------------------------------------
 *
 * History
 *   Jan 6, 2012 (wiswedel): created
 */
package org.knime.base.node.mine.treeensemble2.model;

import java.io.DataOutputStream;
import java.io.IOException;

import org.knime.base.node.mine.treeensemble2.data.TreeAttributeColumnMetaData;
import org.knime.base.node.mine.treeensemble2.data.TreeColumnMetaData;
import org.knime.base.node.mine.treeensemble2.data.TreeMetaData;

/**
 *
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
public abstract class TreeNodeColumnCondition extends TreeNodeCondition {

    private final TreeAttributeColumnMetaData m_columnMetaData;

    private final boolean m_acceptsMissings;

    /**
     *  */
    TreeNodeColumnCondition(final TreeAttributeColumnMetaData columnMetaData, final boolean acceptsMissings) {
        m_columnMetaData = columnMetaData;
        m_acceptsMissings = acceptsMissings;
    }

    /**
     *  */
    public TreeNodeColumnCondition(final TreeModelDataInputStream input, final TreeMetaData treeMetaData)
        throws IOException {
        int index = input.readInt();
        if (index < 0 || index >= treeMetaData.getNrAttributes()) {
            throw new IOException("Invalid attribute index " + index);
        }
        m_columnMetaData = treeMetaData.getAttributeMetaData(index);
        assert m_columnMetaData.getAttributeIndex() == index;
        m_acceptsMissings = input.readBoolean();
    }

    protected final void checkTypeCorrectness(final TreeColumnMetaData instance,
        final Class<? extends TreeColumnMetaData> expectedClass) throws IOException {
        if (!expectedClass.isInstance(instance)) {
            throw new IOException("Column meta information associated with " + "condition \""
                + getClass().getSimpleName() + "\" is not " + "of expected type \"" + expectedClass.getSimpleName()
                + "\" but \"" + instance.getClass().getSimpleName() + "\"");
        }
    }

    /** @return the columnMetaData */
    public TreeAttributeColumnMetaData getColumnMetaData() {
        return m_columnMetaData;
    }

    public String getAttributeName() {
        return getColumnMetaData().getAttributeName();
    }

    public boolean acceptsMissings() {
        return m_acceptsMissings;
    }

    /** {@inheritDoc} */
    @Override
    public void save(final DataOutputStream dataOutput) throws IOException {
        super.save(dataOutput);
        dataOutput.writeInt(m_columnMetaData.getAttributeIndex());
        dataOutput.writeBoolean(m_acceptsMissings);
        saveContent(dataOutput);
    }

    protected abstract void saveContent(final DataOutputStream model) throws IOException;

//    /**
//     * {@inheritDoc}
//     */
//    @Override
//    public abstract PMMLCompoundPredicate toPMMLPredicate();

}
