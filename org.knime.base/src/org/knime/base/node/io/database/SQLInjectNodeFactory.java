package org.knime.base.node.io.database;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "SQLInject" Node.
 *
 *
 * @author Alexander Fillbrunn
 * @since 2.10
 */
public class SQLInjectNodeFactory
        extends NodeFactory<SQLInjectNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public SQLInjectNodeModel createNodeModel() {
        return new SQLInjectNodeModel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNrNodeViews() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeView<SQLInjectNodeModel> createNodeView(final int viewIndex,
            final SQLInjectNodeModel nodeModel) {
        return null;
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
        return new SQLInjectNodeDialog();
    }

}

