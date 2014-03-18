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