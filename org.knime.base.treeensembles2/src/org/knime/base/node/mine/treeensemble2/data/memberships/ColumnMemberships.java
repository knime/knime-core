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
 *   04.12.2015 (Adrian Nembach): created
 */
package org.knime.base.node.mine.treeensemble2.data.memberships;

/**
 *
 * @author Adrian Nembach
 */
public interface ColumnMemberships {

    /**
     * @return the number of instances indexed by this object
     */
    public int size();

    /**
     * Goes to next instance if there is another one and returns true, otherwise returns false
     * @return true if there is another index
     */
    public boolean next();

    /**
     * Jumps to the next index in the column that is larger or equal to <b>indexInColumn</b>
     * Note: For efficiency the starting point is the current position in the columnMemberships.
     * If you want to start from the beginning, call reset() first.
     *
     * @param indexInColumn
     * @return true if the columnMemberships object contains an index larger or equal <b>indexInColumn</b> else false.
     */
    public boolean nextIndexFrom(int indexInColumn);

    /**
     *
     * @return the weight of the current instance
     */
    public double getRowWeight();

    /**
     * @return the index in the original dataset of the current instance
     */
    public int getOriginalIndex();

    /**
     * @return the index in the column of the current instance
     */
    public int getIndexInColumn();

    /**
     * @return the index in the corresponding DataMembershipsObject
     */
    public int getIndexInDataMemberships();

    /**
     * reset internal index
     */
    public void reset();

    /**
     * Jumps to the last instance in the ColumnMemberships
     */
    public void goToLast();

    /**
     * Goes to the previous instance or sets the internal index to -1
     * if there is no previous instance (beginning of instances in column)
     *
     * @return true if there is a previous instance
     */
    public boolean previous();

}
