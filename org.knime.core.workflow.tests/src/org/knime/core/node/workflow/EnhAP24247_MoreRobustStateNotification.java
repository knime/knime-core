package org.knime.core.node.workflow;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

final class EnhAP24247_MoreRobustStateNotification extends WorkflowTestCase {
    
    // a single "Empty Table Creator" node
    private NodeID m_nodeID;
    
    @BeforeEach
    void beforeEach() throws Exception {
        m_nodeID = loadAndSetWorkflow().createChild(1);
    }
    
    @Test
    void testWorkflowStateChangeWithError() {
        final var wfm = getManager();
        
        // two error throwing listeners for testing
        final var listener1Called = new AtomicBoolean();
        final var listener1 = new WorkflowListener() {
            
            @Override
            public void workflowChanged(final WorkflowEvent event) {
                listener1Called.set(true);
                throw new RuntimeException(event.toString());
            }
        };
        wfm.addListener(listener1, false);
        final var listener2Called = new AtomicBoolean();
        final var listener2 = new WorkflowListener() {
            
            @Override
            public void workflowChanged(final WorkflowEvent event) {
                listener2Called.set(true);
                throw new RuntimeException(event.toString());
            }
        };
        wfm.addListener(listener2, false);
        
        // invoke some action that triggers a workflow state change
        wfm.removeNode(m_nodeID);
        
        // check both listeners to make sure that the exception was caught,
        // and thus all registered listeners notified
        assertTrue(listener1Called::get, "First workflow state listener was not called");
        assertTrue(listener2Called::get, "Second workflow state listener was not called");
    }
    
    @Test
    void testNodeStateChangeWithError() {
        final var node = getManager().getNodeContainer(m_nodeID);
        
        // two error throwing listeners for testing
        final var listener1Called = new AtomicBoolean();
        final var listener1 = new NodeStateChangeListener() {
            
            @Override
            public void stateChanged(final NodeStateEvent state) {
                listener1Called.set(true);
                throw new RuntimeException(state.toString());
            }
        };
        node.addNodeStateChangeListener(listener1);
        final var listener2Called = new AtomicBoolean();
        final var listener2 = new NodeStateChangeListener() {
            
            @Override
            public void stateChanged(final NodeStateEvent state) {
                listener2Called.set(true);
                throw new RuntimeException(state.toString());
            }
        };
        node.addNodeStateChangeListener(listener2);
        
        // invoke some action that triggers a node state change
        node.setInternalState(InternalNodeContainerState.IDLE, false);
        
        // check both listeners to make sure that the exception was caught,
        // and thus all registered listeners notified
        assertTrue(listener1Called::get, "First node state listener was not called");
        assertTrue(listener2Called::get, "Second node state listener was not called");
    }
    
}
