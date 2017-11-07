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
 */
package org.knime.base.node.meta.looper.recursive;

import org.knime.core.data.DataRow;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;

/**
 * This is the model implementation of Recursive Loop End Node (2port).
 *
 *
 * @author Iris Adae, University of Konstanz, Germany
 */
public class RecursiveLoopEnd2NodeModel extends RecursiveLoopEndNodeModel {


    /**
     * Constructor for the node model.
     * @param inPorts the number of inports
     * @param outPorts the number of outports
     */
    protected RecursiveLoopEnd2NodeModel(final int inPorts, final int outPorts) {
        super(inPorts, outPorts);
    }

    private BufferedDataTable m_inData2;
    private static int resultingIn2 = 2;


    /**
     * Check if the loop end is connected to the correct loop start.
     */
    @Override
    protected void validateLoopStart() {
        if (!(this.getLoopStartNode() instanceof RecursiveLoopStart2NodeModel)) {
            throw new IllegalStateException("Loop End is not connected"
                    + " to matching/corresponding Recursive Loop Start node.");
        }
    }



    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean checkDataTableSize(final int minNrRows) {
        return (m_inData2.getRowCount() < minNrRows) || (super.checkDataTableSize(minNrRows));
    }
     /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
            throws Exception {


        if (!(this.getLoopStartNode() instanceof RecursiveLoopStart2NodeModel)) {
            throw new IllegalStateException("Loop End is not connected"
                    + " to matching/corresponding Recursive Loop Start (2 ports) node.");
        }

        // in port 2: is fed back to loop start node
        BufferedDataContainer loopData = exec.createDataContainer(inData[resultingIn2].getDataTableSpec());

        ExecutionContext exec1 = exec.createSubExecutionContext(0.3);

        int count = 0;
        for (DataRow row : inData[resultingIn2]) {
            exec1.checkCanceled();
            exec1.setProgress(1.0 * count / loopData.size(), "Copy input table 2");
            loopData.addRowToTable(createNewRow(row, row.getKey()));
        }
        loopData.close();
        m_inData2  = loopData.getTable();
        return super.execute(inData, exec.createSubExecutionContext(0.7));
    }

    /**Call to get the in data table of the previous iteration.
    *
    * @param port of the indata
    * @return the indata table of the last iteration.
    */
   public BufferedDataTable getInData(final int port) {
       if (port == resultingIn2) {
           return m_inData2;
       }
       return getInData();
   }
}
