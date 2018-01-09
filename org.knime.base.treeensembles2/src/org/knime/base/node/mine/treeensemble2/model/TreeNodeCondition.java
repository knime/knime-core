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
 *   Jan 6, 2012 (wiswedel): created
 */
package org.knime.base.node.mine.treeensemble2.model;

import java.io.DataOutputStream;
import java.io.IOException;

import org.knime.base.node.mine.decisiontree2.PMMLPredicate;
import org.knime.base.node.mine.treeensemble2.data.PredictorRecord;
import org.knime.base.node.mine.treeensemble2.data.TreeMetaData;

/**
 *
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 */
public abstract class TreeNodeCondition {

    public abstract boolean testCondition(final PredictorRecord record);

    /**
     * @param dataOutput
     * @throws IOException
     */
    public void save(final DataOutputStream dataOutput) throws IOException {
        byte type;
        if (this instanceof TreeNodeTrueCondition) {
            type = 't';
        } else if (this instanceof TreeNodeNominalCondition) {
            type = 'c'; // categorical
        } else if (this instanceof TreeNodeNominalBinaryCondition) {
            type = 'z';
        } else if (this instanceof TreeNodeNumericCondition) {
            type = 'f'; // floating number
        } else if (this instanceof TreeNodeBitCondition) {
            type = 'b';
        } else if (this instanceof TreeNodeSurrogateCondition) {
            type = 's';
        } else if (this instanceof TreeNodeSurrogateOnlyDefDirCondition) {
            type = 'o';
        } else {
            throw new IllegalStateException("Unknown condition type " + this.getClass().getName()
                + " (not implemented)");
        }
        dataOutput.writeByte(type);
    }

    public static TreeNodeCondition load(final TreeModelDataInputStream input, final TreeMetaData metaData, final TreeBuildingInterner treeBuildingInterner)
        throws IOException {
        byte type = input.readByte();
        switch (type) {
            case 't':
                return TreeNodeTrueCondition.INSTANCE;
            case 'c':
                return new TreeNodeNominalCondition(input, metaData);
            case 'z':
                return new TreeNodeNominalBinaryCondition(input, metaData, treeBuildingInterner);
            case 'f':
                return new TreeNodeNumericCondition(input, metaData);
            case 'b':
                return new TreeNodeBitCondition(input, metaData);
            case 's':
                return new TreeNodeSurrogateCondition(input, metaData, treeBuildingInterner);
            case 'o':
                return new TreeNodeSurrogateOnlyDefDirCondition(input, metaData, treeBuildingInterner);
        }
        throw new IOException("Unknown tree node condition type identifier: '" + (char)type + "'");
    }

    public abstract PMMLPredicate toPMMLPredicate();

}
