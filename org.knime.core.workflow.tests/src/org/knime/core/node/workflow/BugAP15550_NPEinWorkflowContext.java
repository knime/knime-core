MISSINGpackage org.knime.core.node.workflow;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.workflow.node.adapter.AdapterNodeFactory;
import org.knime.core.node.workflow.node.adapter.AdapterNodeModel;
import org.knime.core.util.FileUtil;

import java.io.File;

import static org.junit.jupiter.api.Assertions.fail;

public class BugAP15550_NPEinWorkflowContext extends WorkflowTestCase {

    private NodeID m_metaNode;

    private static boolean m_accessContextFailed = false;

    @BeforeEach
    public void setUp() throws Exception {
        NodeID baseID = loadAndSetWorkflow(getDefaultWorkflowDirectory());
        m_metaNode = new NodeID(baseID, 2);
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
        m_accessContextFailed = false;
    }

    /**
     * Test saving a metanode as template, like via rightclick > metanode > share.
     * @throws Exception
     */
    @Test
    public void testSaveAsTemplate() throws Exception {
        WorkflowManager mnMgr = getManager().getNodeContainer(m_metaNode, WorkflowManager.class, true);
        assert mnMgr != null;
        mnMgr.createAndAddNode(new CheckAccessContextNodeFactory());
        File tmpDir = FileUtil.createTempDir(this.getClass().getName());
        try {
            mnMgr.saveAsTemplate(tmpDir, new ExecutionMonitor());
            if (m_accessContextFailed) {
                fail();
            }
        } finally {
            FileUtil.deleteRecursively(tmpDir);
        }
    }

    public static final class CheckAccessContextNodeFactory extends AdapterNodeFactory {

        private void checkAccessContext() {
            WorkflowContext wfCtx = NodeContext.getContext().getWorkflowManager().getContext();
            if (wfCtx == null) {
                m_accessContextFailed = true;
            }
        }

        @Override
        public AdapterNodeModel createNodeModel() {
            return new AdapterNodeModel(0,0) {
                @Override
                protected PortObjectSpec[] configure(PortObjectSpec[] inSpecs) throws InvalidSettingsException {
                    checkAccessContext();
                    return super.configure(inSpecs);
                }

                @Override
                protected void onDispose() {
                    super.onDispose();
                    checkAccessContext();
                }
            };
        }
    }

