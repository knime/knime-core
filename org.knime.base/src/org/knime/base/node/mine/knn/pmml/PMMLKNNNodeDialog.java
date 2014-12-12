package org.knime.base.node.mine.knn.pmml;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.data.StringValue;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnFilter2;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;

/**
 * <code>NodeDialog</code> for the "TransformationsMerger" Node.
 *
 *
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more
 * complex dialog please derive directly from
 * {@link org.knime.core.node.NodeDialogPane}.
 *
 * @author Alexander Fillbrunn
 */
public class PMMLKNNNodeDialog extends DefaultNodeSettingsPane {

    /**
     * New pane for configuring TransformationsMerger node dialog.
     * This is just a suggestion to demonstrate possible default dialog
     * components.
     */
    protected PMMLKNNNodeDialog() {
        DialogComponentColumnNameSelection predColSelect = new DialogComponentColumnNameSelection(
                                                                 PMMLKNNNodeModel.createPredColumnNameSettingsModel(),
                                                                 "Predicted column", 0, StringValue.class);

        final SettingsModelBoolean limitRowsModel = PMMLKNNNodeModel.createLimitRowsSettingsModel();
        DialogComponentBoolean limitRows = new DialogComponentBoolean(limitRowsModel, "Limit rows");

        final DialogComponentNumber maxNumRec = new DialogComponentNumber(PMMLKNNNodeModel.createMaxNumRowsSettingsModel(),
                                                                    "Maximum number of rows", 1);

        limitRowsModel.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent e) {
                maxNumRec.getModel().setEnabled(limitRowsModel.getBooleanValue());
            }
        });

        DialogComponentNumber numNeighbors = new DialogComponentNumber(
                                                                   PMMLKNNNodeModel.createNumNeighborsSettingsModel(),
                                                                   "Number of neighbors", 1);

        DialogComponentColumnFilter2 learningCols = new DialogComponentColumnFilter2(PMMLKNNNodeModel.createLearningColumnsSettingsModel(), 0);

        addDialogComponent(predColSelect);
        addDialogComponent(numNeighbors);
        createNewGroup("Row limit");
        setHorizontalPlacement(true);
        addDialogComponent(limitRows);
        addDialogComponent(maxNumRec);
        setHorizontalPlacement(false);
        closeCurrentGroup();
        addDialogComponent(learningCols);
    }
}

