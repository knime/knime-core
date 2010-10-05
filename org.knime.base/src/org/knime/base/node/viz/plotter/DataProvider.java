/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2010
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
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
 * -------------------------------------------------------------------
 * 
 * History
 *   31.08.2006 (Fabian Dill): created
 */
package org.knime.base.node.viz.plotter;

import org.knime.base.node.util.DataArray;

/**
 * The plotters rely on a <code>DataProvider</code> to get the data to 
 * visualize. It provides the data as a 
 * {@link org.knime.base.node.util.DataArray} with the 
 * {@link #getDataArray(int)}, where the index can be used, if a NodeModel has 
 * two inports and both data should be visualized. Then the index provides 
 * means to determine which {@link org.knime.base.node.util.DataArray} should 
 * be returned. 
 * 
 * @author Fabian Dill, University of Konstanz
 */
public interface DataProvider {
    
    /** Default start row for DataArray creation. */
    public static final int START = 1;
    
    /** Default end row for DataArray creation. */
    public static final int END = 2500;
    
    /**
     * Provides the data that should be visualized. The index can be used, if a 
     * NodeModel has two inports and both data should be visualized. Then the 
     * index provides means to determine which 
     * {@link org.knime.base.node.util.DataArray} should be returned.
     * 
     * @param index if the data of more than one data table should be 
     *  visualized.
     * @return the data as a data array.
     */
    public DataArray getDataArray(final int index);


}
