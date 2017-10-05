/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 *   12.02.2016 (Adrian Nembach): created
 */
package org.knime.base.node.mine.treeensemble2.model;

import java.io.DataOutputStream;
import java.io.IOException;

import org.knime.base.node.mine.treeensemble2.data.TreeMetaData;

/**
 * This class represents the frequent case that there is no good surrogate and only the default response (direction) is
 * used as Surrogate
 *
 * @author Adrian Nembach
 */
public class TreeNodeSurrogateOnlyDefDirCondition extends AbstractTreeNodeSurrogateCondition {

    private final TreeNodeColumnCondition m_condition;

    /**
     * Standard constructor to create a TreeNodeSurrogateOnlyDefDirCondition
     *
     * @param condition
     * @param defaultResponse
     */
    public TreeNodeSurrogateOnlyDefDirCondition(final TreeNodeColumnCondition condition,
        final boolean defaultResponse) {
        super(defaultResponse);
        m_condition = condition;
    }

    /**
     * Constructor for serialization purposes. Not to be used in any other way.
     *
     * @throws IOException
     *
     */
    TreeNodeSurrogateOnlyDefDirCondition(final TreeModelDataInputStream input, final TreeMetaData metaData, final TreeBuildingInterner treeBuildingInterner)
        throws IOException {
        super(input, metaData);
        m_condition = (TreeNodeColumnCondition)TreeNodeCondition.load(input, metaData, treeBuildingInterner);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected TreeNodeColumnCondition getColumnCondition(final int index) {
        if (index != 0) {
            throw new IndexOutOfBoundsException("This kind of surrogate condition has only 1 column condition.");
        }
        return m_condition;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNumSurrogates() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void save(final DataOutputStream dataOutput) throws IOException {
        super.save(dataOutput);
        m_condition.save(dataOutput);
    }

}
