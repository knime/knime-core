/*
 * ------------------------------------------------------------------------
 *
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
 *   04.12.2015 (Adrian Nembach): created
 */
package org.knime.base.node.mine.treeensemble2.data.memberships;

import java.util.BitSet;

/**
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public interface DataMemberships {

    /**
     * @return Array containing the weights of all instances indexed by this DataMemberships
     */
    public double[] getRowWeights();

    /**
     * @param index
     * @return ColumnMemberships for the column with index <b>index</b>
     */
    public ColumnMemberships getColumnMemberships(int index);

    /**
     * @return Array containing the original indices of the instances indexed by this DataMemberships
     */
    public int[] getOriginalIndices();

    /**
     * @param inChild BitSet that marks all instances with a true bit that should be in the child DataMemberships
     * @return A child DataMemberships object that contains all instances marked by <b>inChild</b>
     */
    public DataMemberships createChildMemberships(BitSet inChild);

    /**
     * @param index
     * @return The weight for the row at index <b>index</b>
     */
    double getRowWeight(int index);

    /**
     * @param index
     * @return The original index for the row at index <b>index</b>
     */
    int getOriginalIndex(int index);

    /**
     * @return number of rows in this dataMemberships object
     */
    int getRowCount();

    /**
     * @return number of rows in the dataMemberships object of the tree root node
     */
    int getRowCountInRoot();

}
