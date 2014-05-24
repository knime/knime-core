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
 */
package org.knime.testing.node.filestore.createloopend;

import org.knime.core.data.DataRow;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.workflow.LoopEndNode;
import org.knime.core.node.workflow.LoopStartNode;
import org.knime.core.node.workflow.LoopStartNodeTerminator;
import org.knime.testing.node.filestore.create.FileStoreCreateNodeModel;

/**
 * @author Christian Dietz, Bernd Wiswedel
 * 
 * Added {@link LoopEndNode} interface to {@link FileStoreCreateNodeModel}
 */
public class FileStoreCreateLoopEndNodeModel extends FileStoreCreateNodeModel implements LoopEndNode {
    
    private BufferedDataContainer m_container;
    
    @Override
    protected BufferedDataTable[] execute(BufferedDataTable[] inData, ExecutionContext exec) throws Exception {
        LoopStartNode loopStartNode = getLoopStartNode();
        CheckUtils.checkSetting(loopStartNode instanceof LoopStartNodeTerminator, 
            "No loop start or not of the expected interface");
        BufferedDataTable superResult = super.execute(inData, exec)[0];
        if (m_container == null) {
            m_container = exec.createDataContainer(superResult.getDataTableSpec());
        }
        int i = 0;
        for (DataRow r : superResult) {
            exec.setProgress(i / (double)superResult.getRowCount(), String.format("%d/%d - Row \"%s\"", 
                i, superResult.getRowCount(), r.getKey()));
            exec.checkCanceled();
            m_container.addRowToTable(r);
        }
        LoopStartNodeTerminator startTerminate = (LoopStartNodeTerminator)loopStartNode;
        if (startTerminate.terminateLoop()) {
            m_container.close();
            return new BufferedDataTable[] {m_container.getTable()};
        } else {
            super.continueLoop();
            return null;
        }
    }
    
    @Override
    protected void reset() {
        super.reset();
        m_container = null;
    }
}