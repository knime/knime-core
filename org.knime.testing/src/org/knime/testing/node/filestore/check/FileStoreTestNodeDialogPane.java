package org.knime.testing.node.filestore.check;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;

final class FileStoreTestNodeDialogPane extends DefaultNodeSettingsPane {

    FileStoreTestNodeDialogPane() {
        addDialogComponent(new DialogComponentBoolean(
            FileStoreTestNodeModel.createAllowMissingModel(), "Allow Missing Values"));
    }

}
