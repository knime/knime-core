package org.knime.base.node.stats.testing.ttest;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "one-way ANOVA" Node.
 *
 *
 * @author Heiko Hofer
 */
public class OneWayANOVANodeFactory extends
    NodeFactory<OneWayANOVANodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public OneWayANOVANodeModel createNodeModel() {
        return new OneWayANOVANodeModel();
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
    public NodeView<OneWayANOVANodeModel> createNodeView(final int viewIndex,
            final OneWayANOVANodeModel nodeModel) {
    	return new OneWayANOVANodeView(nodeModel);
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
        return new OneWayANOVANodeDialog();
    }

}

