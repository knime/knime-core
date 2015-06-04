package org.knime.base.node.io.database.columnrename;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "DBColumnRename" Node.
 *
 *
 * @author Budi Yanto, KNIME.com
 */
public class DBColumnRenameNodeFactory
        extends NodeFactory<DBColumnRenameNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public DBColumnRenameNodeModel createNodeModel() {
        return new DBColumnRenameNodeModel();
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
    public NodeView<DBColumnRenameNodeModel> createNodeView(final int viewIndex,
            final DBColumnRenameNodeModel nodeModel) {
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
        return new DBRenameNodeDialogPane();
    }

}

