package org.knime.ext.sun.nodes.script.multitable;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataTableSpecCreator;

/**
 * Handles the creation of a joint {@link DataTableSpec} for two tables.
 *
 * @author Stefano Woerner
 */
public final class MultiSpecHandler {

    /**
     * Prefix for indicating columns of the left table.
     */
    public static final String LEFT_PREFIX = "left";

    /**
     * Prefix for indicating columns of the right table.
     */
    public static final String RIGHT_PREFIX = "right";

    /**
     * Separates the prefix from the column name.
     */
    public static final String PREFIX_SEPARATOR = ".";

    private MultiSpecHandler() {
        // This class should not be instantiated
    }

    /**
     * Creates a joint {@link DataTableSpec} for two tables.
     *
     * @param leftSpec spec of the left table
     * @param rightSpec spec of the right table
     * @return the joint {@link DataTableSpec}
     */
    public static DataTableSpec createJointSpec(final DataTableSpec leftSpec, final DataTableSpec rightSpec) {
        DataTableSpecCreator specCreator = new DataTableSpecCreator();

        for (DataColumnSpec dataColumnSpec : leftSpec) {
            DataColumnSpecCreator dataColumnSpecCreator = new DataColumnSpecCreator(
                LEFT_PREFIX + PREFIX_SEPARATOR + dataColumnSpec.getName(), dataColumnSpec.getType());
            specCreator.addColumns(dataColumnSpecCreator.createSpec());
        }
        for (DataColumnSpec dataColumnSpec : rightSpec) {
            DataColumnSpecCreator dataColumnSpecCreator = new DataColumnSpecCreator(
                RIGHT_PREFIX + PREFIX_SEPARATOR + dataColumnSpec.getName(), dataColumnSpec.getType());
            specCreator.addColumns(dataColumnSpecCreator.createSpec());
        }

        return specCreator.createSpec();
    }

    /**
     * Creates an equivalent {@link DataTableSpec} to {@link #createJointSpec} but uses the same column
     * nomenclature as the Joiner Node.
     *
     * @param leftSpec spec of the left table
     * @param rightSpec spec of the right table
     * @return the joint {@link DataTableSpec}
     */
    public static DataTableSpec createJointOutputSpec(final DataTableSpec leftSpec, final DataTableSpec rightSpec) {
        DataTableSpecCreator specCreator = new DataTableSpecCreator(leftSpec);
        for (DataColumnSpec dataColumnSpec : rightSpec) {
            String columnName = dataColumnSpec.getName();
            if (leftSpec.containsName(columnName)) {
                columnName += " (#1)";
            }
            DataColumnSpecCreator dataColumnSpecCreator =
                new DataColumnSpecCreator(columnName, dataColumnSpec.getType());
            specCreator.addColumns(dataColumnSpecCreator.createSpec());
        }

        return specCreator.createSpec();
    }

}
