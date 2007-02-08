package org.knime.timeseries;

import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeView;

/**
 * <code>NodeView</code> for the "DummyNode" Node.
 * Dummy Node
 *
 * @author KNIME GmbH
 */
public class DummyNodeNodeView extends NodeView {

    /**
     * Creates a new view.
     * 
     * @param nodeModel The model (class: {@link DummyNodeNodeModel})
     */
    protected DummyNodeNodeView(final NodeModel nodeModel) {
        super(nodeModel);

        // TODO instantiate the components of the view here.

    }

    /**
     * @see org.knime.core.node.NodeView#modelChanged()
     */
    @Override
    protected void modelChanged() {

		// TODO retrieve the new model from your nodemodel and 
		// update the view.
		DummyNodeNodeModel nodeModel = 
			(DummyNodeNodeModel)getNodeModel();
		assert nodeModel != null;
		
		// be aware of a possibly not executed nodeModel! The data you retrieve
		// from your nodemodel could be null, emtpy, or invalid in any kind.
		
    }

    /**
     * @see org.knime.core.node.NodeView#onClose()
     */
    @Override
    protected void onClose() {
    
    	// TODO things to do when closing the view
    }

    /**
     * @see org.knime.core.node.NodeView#onOpen()
     */
    @Override
    protected void onOpen() {

    	// TODO things to do when opening the view
    }

}
