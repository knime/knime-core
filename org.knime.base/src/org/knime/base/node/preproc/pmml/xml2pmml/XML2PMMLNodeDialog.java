package org.knime.base.node.preproc.pmml.xml2pmml;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.data.xml.XMLValue;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;

/**
 * <code>NodeDialog</code> for the "XML2PMML" Node.
 *
 *
 * @author Alexander Fillbrunn
 */
public class XML2PMMLNodeDialog extends DefaultNodeSettingsPane {

    /**
     * New pane for configuring the XML2PMML node.
     */
    @SuppressWarnings("unchecked")
    protected XML2PMMLNodeDialog() {

        final DialogComponentBoolean replaceColumn =
                new DialogComponentBoolean(XML2PMMLNodeModel.createReplaceXMLColumnSettingsMode(),
                        "Replace existing column");

        final DialogComponentString newColumnName =
                new DialogComponentString(XML2PMMLNodeModel.createNewColumnNameSettingsMode(),
                                          "New PMML column", true, 15);

        replaceColumn.getModel().addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent e) {
                SettingsModelBoolean model = (SettingsModelBoolean)replaceColumn.getModel();
                newColumnName.getModel().setEnabled(!model.getBooleanValue());
            }
        });

        addDialogComponent(new DialogComponentColumnNameSelection(
                               XML2PMMLNodeModel.createXMLColumnNameSettingsMode(),  "XML column", 0, XMLValue.class));
        addDialogComponent(replaceColumn);
        addDialogComponent(newColumnName);
        addDialogComponent(new DialogComponentBoolean(
                                XML2PMMLNodeModel.createFailOnInvalidSettingsMode(), "Fail on invalid PMML"));
    }
}
