package org.knime.core.data.convert.map;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataValue;
import org.knime.core.data.convert.map.MappingFramework.CellValueConsumer;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * Interface for the destination for extracting and writing {@link DataCell}/{@link DataValue} contents.
 *
 * May be implemented to write the values to an SQL Database, file, H2O frame and more.
 *
 * Note: extend {@link AbstractSink} instead of implementing this interface.
 *
 * @author Jonathan Hale
 * @param <ThisType> Class which is implementing this interface.
 */
public interface Sink<ThisType extends Sink<ThisType>> {
    /**
     * Called after filling cells of a row was completed.
     */
    public void finishRow();

    /**
     * Set value of cell in current row at given index.
     *
     * @param index Index of the cell in the current row.
     * @param value Value to set
     */
    public void setCellValue(int index, DataCell value);

    /**
     * Parameters passed to a {@link CellValueConsumer}, further specializing it to a specific input and output.
     *
     * @author Jonathan Hale
     * @param <S> Type of {@link Sink} these parameters work with
     */
    public static interface ConsumerParameters<S extends Sink<S>> {

        /**
         * Save these parameters to node settings.
         *
         * @param settings Settings to save to
         */
        void saveSettingsTo(NodeSettingsWO settings);

        /**
         * Load parameters from node settings.
         *
         * @param settings Settings to load from
         * @throws InvalidSettingsException If invalid settings are encountered.
         */
        void loadSettingsFrom(NodeSettingsRO settings) throws InvalidSettingsException;
    }
}
