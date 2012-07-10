package org.knime.base.node.stats.testing.ttest;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "One-Sample T-Test" Node.
 *
 *
 * @author Heiko Hofer
 */
public class OneSampleTTestNodeFactory extends
    NodeFactory<OneSampleTTestNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public OneSampleTTestNodeModel createNodeModel() {
        return new OneSampleTTestNodeModel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNrNodeViews() {
        return 1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeView<OneSampleTTestNodeModel> createNodeView(final int viewIndex,
            final OneSampleTTestNodeModel nodeModel) {
    	return new OneSampleTTestNodeView(nodeModel);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasDialog() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeDialogPane createNodeDialogPane() {
        return new OneSampleTTestNodeDialog();
    }

}

