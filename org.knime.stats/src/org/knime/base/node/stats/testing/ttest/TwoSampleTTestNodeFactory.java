package org.knime.base.node.stats.testing.ttest;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "Two-Sample T-Test" Node.
 *
 *
 * @author Heiko Hofer
 */
public class TwoSampleTTestNodeFactory extends
    NodeFactory<TwoSampleTTestNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public TwoSampleTTestNodeModel createNodeModel() {
        return new TwoSampleTTestNodeModel();
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
    public NodeView<TwoSampleTTestNodeModel> createNodeView(final int viewIndex,
            final TwoSampleTTestNodeModel nodeModel) {
    	return new TwoSampleTTestNodeView(nodeModel);
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
        return new TwoSampleTTestNodeDialog();
    }

}

