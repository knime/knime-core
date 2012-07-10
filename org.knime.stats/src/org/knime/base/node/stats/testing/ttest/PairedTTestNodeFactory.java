package org.knime.base.node.stats.testing.ttest;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "Paired T-Test" Node.
 *
 *
 * @author Heiko Hofer
 */
public class PairedTTestNodeFactory extends
    NodeFactory<PairedTTestNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public PairedTTestNodeModel createNodeModel() {
        return new PairedTTestNodeModel();
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
    public NodeView<PairedTTestNodeModel> createNodeView(final int viewIndex,
            final PairedTTestNodeModel nodeModel) {
    	return new PairedTTestNodeView(nodeModel);
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
        return new PairedTTestNodeDialog();
    }

}

