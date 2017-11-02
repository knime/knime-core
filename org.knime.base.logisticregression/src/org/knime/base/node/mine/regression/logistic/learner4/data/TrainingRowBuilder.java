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
 *   24.05.2017 (Adrian Nembach): created
 */
package org.knime.base.node.mine.regression.logistic.learner4.data;

import java.util.List;
import java.util.Map;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.node.BufferedDataTable;

/**
 * Builds a training row.
 *
 * @author Adrian Nembach, KNIME.com
 * @param <T> the kind of {@link TrainingRow} this builder produces
 */
public interface TrainingRowBuilder <T extends TrainingRow> {

    /**
     * Creates a {@link TrainingRow} from a {@link DataRow}
     *
     * @param row from a {@link BufferedDataTable}
     * @param id the created {@link TrainingRow} should have
     * @return a {@link TrainingRow} with the data from <b>row</b>
     */
    public T build(DataRow row, int id);

    /**
     * Getter for the number of features.
     *
     * @return the number of features
     */
    public int getFeatureCount();

    /**
     * Getter for the dimension of the target.
     * In case of a classification problem with K classes, this would be K-1.
     *
     * @return the target dimension
     */
    public int getTargetDimension();

    /**
     * The key of the map corresponds to the column index in the input table.
     *
     * @return a map of the possible values of all nominal columns
     */
    public Map<Integer, List<DataCell>> getNominalDomainValues();

    /**
     * The indices are with respect to the original input table.
     *
     * @return a list containing the indices of the columns used for learning
     */
    public List<Integer> getLearningColumns();

    /**
     * The key of the map corresponds to the column index in the input table.
     *
     * @return a map of the lenghts of all vector columns
     */
    public Map<Integer, Integer> getVectorLengths();

}
