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
 *   05.09.2017 (Adrian Nembach): created
 */
package org.knime.base.node.mine.treeensemble2.model.pmml;

import org.knime.base.node.mine.treeensemble2.data.TreeAttributeColumnMetaData;
import org.knime.base.node.mine.treeensemble2.data.TreeMetaData;
import org.knime.base.node.mine.treeensemble2.data.TreeTargetColumnMetaData;
import org.knime.base.node.mine.treeensemble2.model.TreeNodeCondition;
import org.knime.core.data.DataTableSpec;

/**
 * Provides a mapping from field names to {@link TreeAttributeColumnMetaData}.
 * For example necessary for creation of {@link TreeNodeCondition} objects.
 *
 * @author Adrian Nembach, KNIME
 */
interface MetaDataMapper <T extends TreeTargetColumnMetaData> {

    /**
     * Returns the {@link TreeAttributeColumnMetaData} for a given field name.
     * @param field for which to retrieve the respective meta data
     * @return the meta data object for <b>field</b>
     */
    public ColumnHelper<?> getColumnHelper(final String field);

    /**
     * Returns true if <b>field</b> is nominal.
     * @param field to be tested
     * @return true if <b>field</b> corresponds to a nominal column
     */
    public boolean isNominal(final String field);

    /**
     * Returns the meta data information for nominal columns.
     * It is recommended to only call this method if the isNominal method
     * returned true.
     * @param field identifier of a nominal column
     * @return the meta data information
     * @throws IllegalArgumentException if <b>field</b> does not correspond to a nominal column
     */
    public NominalAttributeColumnHelper getNominalColumnHelper(final String field);

    /**
     * Returns the meta data information for numeric columns.
     * It is recommended to only call this method if the isNominal method
     * returned true.
     * @param field identifier of a numeric column
     * @return the meta data information
     * @throws IllegalArgumentException if <b>field</b> does not correspond to a numeric column
     */
    public NumericAttributeColumnHelper getNumericColumnHelper(final String field);

    /**
     * Returns the {@link DataTableSpec} on which the model was learned.
     * In case of a model that is learned on a bit/byte/double vector, the returned spec contains
     * the correct name and type of the vector column.
     *
     * @return the {@link DataTableSpec} of the table the model was originally learned on
     */
    public DataTableSpec getLearnSpec();

    /**
     * @return the meta data information for the target column
     */
    public TargetColumnHelper<T> getTargetColumnHelper();

    /**
     *
     * @return the meta data information for the tree (learning and target columns)
     */
    public TreeMetaData getTreeMetaData();
}
