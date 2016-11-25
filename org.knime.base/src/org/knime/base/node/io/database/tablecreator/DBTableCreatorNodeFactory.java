package org.knime.base.node.io.database.tablecreator;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "DBTableCreator" Node.
 *
 *
 * @author Budi Yanto, KNIME.com
 */
public class DBTableCreatorNodeFactory extends NodeFactory<DBTableCreatorNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public DBTableCreatorNodeModel createNodeModel() {
        return new DBTableCreatorNodeModel();
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
    public NodeView<DBTableCreatorNodeModel> createNodeView(final int viewIndex,
        final DBTableCreatorNodeModel nodeModel) {
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
        return new DBTableCreatorNodeDialog();
    }

}
