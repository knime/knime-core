package org.knime.base.node.io.database;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "SQL Extract" Node.
 *
 * @author Alexander Fillbrunn
 * @since 2.10
 */
public class SQLExtractNodeFactory
        extends NodeFactory<SQLExtractNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public SQLExtractNodeModel createNodeModel() {
        return new SQLExtractNodeModel();
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
    public NodeView<SQLExtractNodeModel> createNodeView(final int viewIndex,
            final SQLExtractNodeModel nodeModel) {
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
        return new SQLExtractNodeDialog();
    }

}

