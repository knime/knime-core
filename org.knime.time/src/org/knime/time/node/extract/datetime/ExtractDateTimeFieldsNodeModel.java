/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ---------------------------------------------------------------------
 *
 * History
 *   Apr 19, 2017 (marcel): created
 */
package org.knime.time.node.extract.datetime;

import java.time.format.TextStyle;
import java.time.temporal.ChronoField;
import java.time.temporal.WeekFields;
import java.util.Locale;

import org.apache.commons.lang3.LocaleUtils;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnDomainCreator;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.IntCell.IntCellFactory;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.def.StringCell.StringCellFactory;
import org.knime.core.data.time.localdate.LocalDateValue;
import org.knime.core.data.time.localdatetime.LocalDateTimeValue;
import org.knime.core.data.time.localtime.LocalTimeValue;
import org.knime.core.data.time.zoneddatetime.ZonedDateTimeValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.streamable.simple.SimpleStreamableFunctionNodeModel;
import org.knime.core.util.UniqueNameGenerator;

/**
 * @author Marcel Wiedenmann, KNIME.com, Konstanz, Germany
 */
final class ExtractDateTimeFieldsNodeModel extends SimpleStreamableFunctionNodeModel {

    static final String YEAR = "Year";

    static final String QUARTER = "Quarter";

    static final String MONTH_NUMBER = "Month (number)";

    static final String MONTH_NAME = "Month (name)";

    static final String WEEK = "Week";

    static final String DAY_OF_YEAR = "Day of year";

    static final String DAY_OF_MONTH = "Day of month";

    static final String DAY_OF_WEEK_NUMBER = "Day of week (number)";

    static final String DAY_OF_WEEK_NAME = "Day of week (name)";

    static final String HOUR = "Hour";

    static final String MINUTE = "Minute";

    static final String SECOND = "Second";

    static final String SUBSECOND = "Subsecond in";

    static final String SUBSECOND_COL = "Subsecond";

    static final String MILLISECOND = "milliseconds";

    static final String MICROSECOND = "microseconds";

    static final String NANOSECOND = "nanoseconds";

    static final String TIME_ZONE_NAME = "Time zone name";

    static final String TIME_ZONE_OFFSET = "Time zone offset";

    static SettingsModelString createColSelectModel() {
        return new SettingsModelString("col_select", null);
    }

    static SettingsModelBoolean createFieldBooleanModel(final String key) {
        return new SettingsModelBoolean(key, false);
    }

    static SettingsModelString createSubsecondUnitsModel(final SettingsModelBoolean subsecondsModelBoolean) {
        final SettingsModelString subsecondsModelString = new SettingsModelString("subsecond_units", MILLISECOND);
        subsecondsModelString.setEnabled(subsecondsModelBoolean.getBooleanValue());
        return subsecondsModelString;
    }

    static SettingsModelString createLocaleModel() {
        return new SettingsModelString("locale", Locale.getDefault().toString());
    }

    static boolean isDateType(final DataType type) {
        return !type.isCompatible(LocalTimeValue.class);
    }

    static boolean isTimeType(final DataType type) {
        return !type.isCompatible(LocalDateValue.class);
    }

    static boolean isZonedType(final DataType type) {
        return type.isCompatible(ZonedDateTimeValue.class);
    }

    private final SettingsModelString m_colSelectModel = createColSelectModel();

    private final SettingsModelBoolean m_yearModel = createFieldBooleanModel(YEAR);

    private final SettingsModelBoolean m_quarterModel = createFieldBooleanModel(QUARTER);

    private final SettingsModelBoolean m_monthNumberModel = createFieldBooleanModel(MONTH_NUMBER);

    private final SettingsModelBoolean m_monthNameModel = createFieldBooleanModel(MONTH_NAME);

    private final SettingsModelBoolean m_weekModel = createFieldBooleanModel(WEEK);

    private final SettingsModelBoolean m_dayYearModel = createFieldBooleanModel(DAY_OF_YEAR);

    private final SettingsModelBoolean m_dayMonthModel = createFieldBooleanModel(DAY_OF_MONTH);

    private final SettingsModelBoolean m_dayWeekNumberModel = createFieldBooleanModel(DAY_OF_WEEK_NUMBER);

    private final SettingsModelBoolean m_dayWeekNameModel = createFieldBooleanModel(DAY_OF_WEEK_NAME);

    private final SettingsModelBoolean m_hourModel = createFieldBooleanModel(HOUR);

    private final SettingsModelBoolean m_minuteModel = createFieldBooleanModel(MINUTE);

    private final SettingsModelBoolean m_secondModel = createFieldBooleanModel(SECOND);

    private final SettingsModelBoolean m_subsecondModel = createFieldBooleanModel(SUBSECOND);

    private final SettingsModelString m_subsecondUnitsModel = createSubsecondUnitsModel(m_subsecondModel);

    private final SettingsModelBoolean m_timeZoneNameModel = createFieldBooleanModel(TIME_ZONE_NAME);

    private final SettingsModelBoolean m_timeZoneOffsetModel = createFieldBooleanModel(TIME_ZONE_OFFSET);

    private final SettingsModelString m_localeModel = createLocaleModel();

    private final SettingsModelBoolean[] m_dateModels =
        new SettingsModelBoolean[]{m_yearModel, m_quarterModel, m_monthNumberModel, m_monthNameModel, m_weekModel,
            m_dayYearModel, m_dayMonthModel, m_dayWeekNumberModel, m_dayWeekNameModel};

    private final SettingsModelBoolean[] m_timeModels =
        new SettingsModelBoolean[]{m_hourModel, m_minuteModel, m_secondModel, m_subsecondModel};

    private final SettingsModelBoolean[] m_timeZoneModels =
        new SettingsModelBoolean[]{m_timeZoneNameModel, m_timeZoneOffsetModel};

    /**
     * {@inheritDoc}
     */
    @Override
    protected ColumnRearranger createColumnRearranger(final DataTableSpec spec) throws InvalidSettingsException {
        final String selectedCol = m_colSelectModel.getStringValue();
        if (selectedCol == null || selectedCol.isEmpty()) {
            throw new InvalidSettingsException("Node must be configured.");
        }
        final int selectedColIdx = spec.findColumnIndex(selectedCol);
        if (selectedColIdx < 0) {
            throw new InvalidSettingsException("Column " + selectedCol + " not found in the input table.");
        }
        final DataType selectedColType = spec.getColumnSpec(selectedCol).getType();
        final boolean isDate = isDateType(selectedColType);
        final boolean isTime = isTimeType(selectedColType);
        if (!isDate && !isTime) {
            throw new InvalidSettingsException("Column " + selectedCol + " does not contain a Date&Time type.");
        }
        final boolean isLocalDate = selectedColType.isCompatible(LocalDateValue.class);
        final boolean isLocalTime = selectedColType.isCompatible(LocalTimeValue.class);
        final boolean isLocalDateTime = selectedColType.isCompatible(LocalDateTimeValue.class);
        final boolean isZonedDateTime = selectedColType.isCompatible(ZonedDateTimeValue.class);

        final Locale locale = LocaleUtils.toLocale(m_localeModel.getStringValue());

        final UniqueNameGenerator nameGenerator = new UniqueNameGenerator(spec);
        final DataColumnDomainCreator domainCreator = new DataColumnDomainCreator();
        final ColumnRearranger rearranger = new ColumnRearranger(spec);

        if (isDate) {

            // extract date fields:

            // year:

            if (m_yearModel.getBooleanValue()) {
                final DataColumnSpec colSpec = nameGenerator.newColumn(YEAR, IntCell.TYPE);
                if (isLocalDate) {
                    rearranger.append(new AbstractLocalDateFieldCellFactory(selectedColIdx, colSpec) {

                        @Override
                        protected DataCell getCell(final LocalDateValue value) {
                            return IntCellFactory.create(value.getLocalDate().getYear());
                        }
                    });
                } else if (isLocalDateTime) {
                    rearranger.append(new AbstractLocalDateTimeFieldCellFactory(selectedColIdx, colSpec) {

                        @Override
                        protected DataCell getCell(final LocalDateTimeValue value) {
                            return IntCellFactory.create(value.getLocalDateTime().getYear());
                        }
                    });
                } else if (isZonedDateTime) {
                    rearranger.append(new AbstractZonedDateTimeFieldCellFactory(selectedColIdx, colSpec) {

                        @Override
                        protected DataCell getCell(final ZonedDateTimeValue value) {
                            return IntCellFactory.create(value.getZonedDateTime().getYear());
                        }
                    });
                }
            }

            // quarter:

            if (m_quarterModel.getBooleanValue()) {
                final DataColumnSpec colSpec = createBoundedIntColumn(domainCreator, nameGenerator, QUARTER, 1, 4);
                if (isLocalDate) {
                    rearranger.append(new AbstractLocalDateFieldCellFactory(selectedColIdx, colSpec) {

                        @Override
                        protected DataCell getCell(final LocalDateValue value) {
                            return IntCellFactory.create((value.getLocalDate().getMonthValue() + 2) / 3);
                        }
                    });
                } else if (isLocalDateTime) {
                    rearranger.append(new AbstractLocalDateTimeFieldCellFactory(selectedColIdx, colSpec) {

                        @Override
                        protected DataCell getCell(final LocalDateTimeValue value) {
                            return IntCellFactory.create((value.getLocalDateTime().getMonthValue() + 2) / 3);
                        }
                    });
                } else if (isZonedDateTime) {
                    rearranger.append(new AbstractZonedDateTimeFieldCellFactory(selectedColIdx, colSpec) {

                        @Override
                        protected DataCell getCell(final ZonedDateTimeValue value) {
                            return IntCellFactory.create((value.getZonedDateTime().getMonthValue() + 2) / 3);
                        }
                    });
                }
            }

            // month (number):

            if (m_monthNumberModel.getBooleanValue()) {
                final DataColumnSpec colSpec =
                    createBoundedIntColumn(domainCreator, nameGenerator, MONTH_NUMBER, 1, 12);
                if (isLocalDate) {
                    rearranger.append(new AbstractLocalDateFieldCellFactory(selectedColIdx, colSpec) {

                        @Override
                        protected DataCell getCell(final LocalDateValue value) {
                            return IntCellFactory.create(value.getLocalDate().getMonthValue());
                        }
                    });
                } else if (isLocalDateTime) {
                    rearranger.append(new AbstractLocalDateTimeFieldCellFactory(selectedColIdx, colSpec) {

                        @Override
                        protected DataCell getCell(final LocalDateTimeValue value) {
                            return IntCellFactory.create(value.getLocalDateTime().getMonthValue());
                        }
                    });
                } else if (isZonedDateTime) {
                    rearranger.append(new AbstractZonedDateTimeFieldCellFactory(selectedColIdx, colSpec) {

                        @Override
                        protected DataCell getCell(final ZonedDateTimeValue value) {
                            return IntCellFactory.create(value.getZonedDateTime().getMonthValue());
                        }
                    });
                }
            }

            // month (name):

            if (m_monthNameModel.getBooleanValue()) {
                final DataColumnSpec colSpec = nameGenerator.newColumn(MONTH_NAME, StringCell.TYPE);
                if (isLocalDate) {
                    rearranger.append(new AbstractLocalDateFieldCellFactory(selectedColIdx, colSpec) {

                        @Override
                        protected DataCell getCell(final LocalDateValue value) {
                            return StringCellFactory
                                .create(value.getLocalDate().getMonth().getDisplayName(TextStyle.FULL, locale));
                        }
                    });
                } else if (isLocalDateTime) {
                    rearranger.append(new AbstractLocalDateTimeFieldCellFactory(selectedColIdx, colSpec) {

                        @Override
                        protected DataCell getCell(final LocalDateTimeValue value) {
                            return StringCellFactory
                                .create(value.getLocalDateTime().getMonth().getDisplayName(TextStyle.FULL, locale));
                        }
                    });
                } else if (isZonedDateTime) {
                    rearranger.append(new AbstractZonedDateTimeFieldCellFactory(selectedColIdx, colSpec) {

                        @Override
                        protected DataCell getCell(final ZonedDateTimeValue value) {
                            return StringCellFactory
                                .create(value.getZonedDateTime().getMonth().getDisplayName(TextStyle.FULL, locale));
                        }
                    });
                }
            }

            // week:

            if (m_weekModel.getBooleanValue()) {
                final DataColumnSpec colSpec = createBoundedIntColumn(domainCreator, nameGenerator, WEEK, 1, 52);
                if (isLocalDate) {
                    rearranger.append(new AbstractLocalDateFieldCellFactory(selectedColIdx, colSpec) {

                        @Override
                        protected DataCell getCell(final LocalDateValue value) {
                            return IntCellFactory
                                .create(value.getLocalDate().get(WeekFields.of(locale).weekOfWeekBasedYear()));
                        }
                    });
                } else if (isLocalDateTime) {
                    rearranger.append(new AbstractLocalDateTimeFieldCellFactory(selectedColIdx, colSpec) {

                        @Override
                        protected DataCell getCell(final LocalDateTimeValue value) {
                            return IntCellFactory
                                .create(value.getLocalDateTime().get(WeekFields.of(locale).weekOfWeekBasedYear()));
                        }
                    });
                } else if (isZonedDateTime) {
                    rearranger.append(new AbstractZonedDateTimeFieldCellFactory(selectedColIdx, colSpec) {

                        @Override
                        protected DataCell getCell(final ZonedDateTimeValue value) {
                            return IntCellFactory
                                .create(value.getZonedDateTime().get(WeekFields.of(locale).weekOfWeekBasedYear()));
                        }
                    });
                }
            }

            // day of year:

            if (m_dayYearModel.getBooleanValue()) {
                final DataColumnSpec colSpec =
                    createBoundedIntColumn(domainCreator, nameGenerator, DAY_OF_YEAR, 1, 366);
                if (isLocalDate) {
                    rearranger.append(new AbstractLocalDateFieldCellFactory(selectedColIdx, colSpec) {

                        @Override
                        protected DataCell getCell(final LocalDateValue value) {
                            return IntCellFactory.create(value.getLocalDate().getDayOfYear());
                        }
                    });
                } else if (isLocalDateTime) {
                    rearranger.append(new AbstractLocalDateTimeFieldCellFactory(selectedColIdx, colSpec) {

                        @Override
                        protected DataCell getCell(final LocalDateTimeValue value) {
                            return IntCellFactory.create(value.getLocalDateTime().getDayOfYear());
                        }
                    });
                } else if (isZonedDateTime) {
                    rearranger.append(new AbstractZonedDateTimeFieldCellFactory(selectedColIdx, colSpec) {

                        @Override
                        protected DataCell getCell(final ZonedDateTimeValue value) {
                            return IntCellFactory.create(value.getZonedDateTime().getDayOfYear());
                        }
                    });
                }
            }

            // day of month:

            if (m_dayMonthModel.getBooleanValue()) {
                final DataColumnSpec colSpec =
                    createBoundedIntColumn(domainCreator, nameGenerator, DAY_OF_MONTH, 1, 31);
                if (isLocalDate) {
                    rearranger.append(new AbstractLocalDateFieldCellFactory(selectedColIdx, colSpec) {

                        @Override
                        protected DataCell getCell(final LocalDateValue value) {
                            return IntCellFactory.create(value.getLocalDate().getDayOfMonth());
                        }
                    });
                } else if (isLocalDateTime) {
                    rearranger.append(new AbstractLocalDateTimeFieldCellFactory(selectedColIdx, colSpec) {

                        @Override
                        protected DataCell getCell(final LocalDateTimeValue value) {
                            return IntCellFactory.create(value.getLocalDateTime().getDayOfMonth());
                        }
                    });
                } else if (isZonedDateTime) {
                    rearranger.append(new AbstractZonedDateTimeFieldCellFactory(selectedColIdx, colSpec) {

                        @Override
                        protected DataCell getCell(final ZonedDateTimeValue value) {
                            return IntCellFactory.create(value.getZonedDateTime().getDayOfMonth());
                        }
                    });
                }
            }

            // day of week (number):

            if (m_dayWeekNumberModel.getBooleanValue()) {
                final DataColumnSpec colSpec =
                    createBoundedIntColumn(domainCreator, nameGenerator, DAY_OF_WEEK_NUMBER, 1, 7);
                if (isLocalDate) {
                    rearranger.append(new AbstractLocalDateFieldCellFactory(selectedColIdx, colSpec) {

                        @Override
                        protected DataCell getCell(final LocalDateValue value) {
                            return IntCellFactory.create(value.getLocalDate().get(WeekFields.of(locale).dayOfWeek()));
                        }
                    });
                } else if (isLocalDateTime) {
                    rearranger.append(new AbstractLocalDateTimeFieldCellFactory(selectedColIdx, colSpec) {

                        @Override
                        protected DataCell getCell(final LocalDateTimeValue value) {
                            return IntCellFactory
                                .create(value.getLocalDateTime().get(WeekFields.of(locale).dayOfWeek()));
                        }
                    });
                } else if (isZonedDateTime) {
                    rearranger.append(new AbstractZonedDateTimeFieldCellFactory(selectedColIdx, colSpec) {

                        @Override
                        protected DataCell getCell(final ZonedDateTimeValue value) {
                            return IntCellFactory
                                .create(value.getZonedDateTime().get(WeekFields.of(locale).dayOfWeek()));
                        }
                    });
                }
            }

            // day of week (name):

            if (m_dayWeekNameModel.getBooleanValue()) {
                final DataColumnSpec colSpec = nameGenerator.newColumn(DAY_OF_WEEK_NAME, StringCell.TYPE);
                if (isLocalDate) {
                    rearranger.append(new AbstractLocalDateFieldCellFactory(selectedColIdx, colSpec) {

                        @Override
                        protected DataCell getCell(final LocalDateValue value) {
                            return StringCellFactory.create(
                                value.getLocalDate().getDayOfWeek().getDisplayName(TextStyle.FULL_STANDALONE, locale));
                        }
                    });
                } else if (isLocalDateTime) {
                    rearranger.append(new AbstractLocalDateTimeFieldCellFactory(selectedColIdx, colSpec) {

                        @Override
                        protected DataCell getCell(final LocalDateTimeValue value) {
                            return StringCellFactory.create(value.getLocalDateTime().getDayOfWeek()
                                .getDisplayName(TextStyle.FULL_STANDALONE, locale));
                        }
                    });
                } else if (isZonedDateTime) {
                    rearranger.append(new AbstractZonedDateTimeFieldCellFactory(selectedColIdx, colSpec) {

                        @Override
                        protected DataCell getCell(final ZonedDateTimeValue value) {
                            return StringCellFactory.create(value.getZonedDateTime().getDayOfWeek()
                                .getDisplayName(TextStyle.FULL_STANDALONE, locale));
                        }
                    });
                }
            }
        }
        if (isTime) {

            // extract time fields:

            // hour:

            if (m_hourModel.getBooleanValue()) {
                final DataColumnSpec colSpec = createBoundedIntColumn(domainCreator, nameGenerator, HOUR, 0, 23);
                if (isLocalTime) {
                    rearranger.append(new AbstractLocalTimeFieldCellFactory(selectedColIdx, colSpec) {

                        @Override
                        protected DataCell getCell(final LocalTimeValue value) {
                            return IntCellFactory.create(value.getLocalTime().getHour());
                        }
                    });
                } else if (isLocalDateTime) {
                    rearranger.append(new AbstractLocalDateTimeFieldCellFactory(selectedColIdx, colSpec) {

                        @Override
                        protected DataCell getCell(final LocalDateTimeValue value) {
                            return IntCellFactory.create(value.getLocalDateTime().getHour());
                        }
                    });
                } else if (isZonedDateTime) {
                    rearranger.append(new AbstractZonedDateTimeFieldCellFactory(selectedColIdx, colSpec) {

                        @Override
                        protected DataCell getCell(final ZonedDateTimeValue value) {
                            return IntCellFactory.create(value.getZonedDateTime().getHour());
                        }
                    });
                }
            }

            // minute:

            if (m_minuteModel.getBooleanValue()) {
                final DataColumnSpec colSpec = createBoundedIntColumn(domainCreator, nameGenerator, MINUTE, 0, 59);
                if (isLocalTime) {
                    rearranger.append(new AbstractLocalTimeFieldCellFactory(selectedColIdx, colSpec) {

                        @Override
                        protected DataCell getCell(final LocalTimeValue value) {
                            return IntCellFactory.create(value.getLocalTime().getMinute());
                        }
                    });
                } else if (isLocalDateTime) {
                    rearranger.append(new AbstractLocalDateTimeFieldCellFactory(selectedColIdx, colSpec) {

                        @Override
                        protected DataCell getCell(final LocalDateTimeValue value) {
                            return IntCellFactory.create(value.getLocalDateTime().getMinute());
                        }
                    });
                } else if (isZonedDateTime) {
                    rearranger.append(new AbstractZonedDateTimeFieldCellFactory(selectedColIdx, colSpec) {

                        @Override
                        protected DataCell getCell(final ZonedDateTimeValue value) {
                            return IntCellFactory.create(value.getZonedDateTime().getMinute());
                        }
                    });
                }
            }

            // second:

            if (m_secondModel.getBooleanValue()) {
                final DataColumnSpec colSpec = createBoundedIntColumn(domainCreator, nameGenerator, SECOND, 0, 59);
                if (isLocalTime) {
                    rearranger.append(new AbstractLocalTimeFieldCellFactory(selectedColIdx, colSpec) {

                        @Override
                        protected DataCell getCell(final LocalTimeValue value) {
                            return IntCellFactory.create(value.getLocalTime().getSecond());
                        }
                    });
                } else if (isLocalDateTime) {
                    rearranger.append(new AbstractLocalDateTimeFieldCellFactory(selectedColIdx, colSpec) {

                        @Override
                        protected DataCell getCell(final LocalDateTimeValue value) {
                            return IntCellFactory.create(value.getLocalDateTime().getSecond());
                        }
                    });
                } else if (isZonedDateTime) {
                    rearranger.append(new AbstractZonedDateTimeFieldCellFactory(selectedColIdx, colSpec) {

                        @Override
                        protected DataCell getCell(final ZonedDateTimeValue value) {
                            return IntCellFactory.create(value.getZonedDateTime().getSecond());
                        }
                    });
                }
            }

            // subsecond:

            if (m_subsecondModel.getBooleanValue()) {
                final String subsecondUnit = m_subsecondUnitsModel.getStringValue();
                if (subsecondUnit.equals(MILLISECOND)) {
                    // in milliseconds
                    final DataColumnSpec colSpec = createBoundedIntColumn(domainCreator, nameGenerator,
                        SUBSECOND_COL + " (in " + MILLISECOND + ")", 0, 999);
                    if (isLocalTime) {
                        rearranger.append(new AbstractLocalTimeFieldCellFactory(selectedColIdx, colSpec) {

                            @Override
                            protected DataCell getCell(final LocalTimeValue value) {
                                return IntCellFactory.create(value.getLocalTime().get(ChronoField.MILLI_OF_SECOND));
                            }
                        });
                    } else if (isLocalDateTime) {
                        rearranger.append(new AbstractLocalDateTimeFieldCellFactory(selectedColIdx, colSpec) {

                            @Override
                            protected DataCell getCell(final LocalDateTimeValue value) {
                                return IntCellFactory.create(value.getLocalDateTime().get(ChronoField.MILLI_OF_SECOND));
                            }
                        });
                    } else if (isZonedDateTime) {
                        rearranger.append(new AbstractZonedDateTimeFieldCellFactory(selectedColIdx, colSpec) {

                            @Override
                            protected DataCell getCell(final ZonedDateTimeValue value) {
                                return IntCellFactory.create(value.getZonedDateTime().get(ChronoField.MILLI_OF_SECOND));
                            }
                        });
                    }
                } else if (subsecondUnit.equals(MICROSECOND)) {
                    // in microseconds
                    final DataColumnSpec colSpec = createBoundedIntColumn(domainCreator, nameGenerator,
                        SUBSECOND_COL + " (in " + MICROSECOND + ")", 0, 999_999);
                    if (isLocalTime) {
                        rearranger.append(new AbstractLocalTimeFieldCellFactory(selectedColIdx, colSpec) {

                            @Override
                            protected DataCell getCell(final LocalTimeValue value) {
                                return IntCellFactory.create(value.getLocalTime().get(ChronoField.MICRO_OF_SECOND));
                            }
                        });
                    } else if (isLocalDateTime) {
                        rearranger.append(new AbstractLocalDateTimeFieldCellFactory(selectedColIdx, colSpec) {

                            @Override
                            protected DataCell getCell(final LocalDateTimeValue value) {
                                return IntCellFactory.create(value.getLocalDateTime().get(ChronoField.MICRO_OF_SECOND));
                            }
                        });
                    } else if (isZonedDateTime) {
                        rearranger.append(new AbstractZonedDateTimeFieldCellFactory(selectedColIdx, colSpec) {

                            @Override
                            protected DataCell getCell(final ZonedDateTimeValue value) {
                                return IntCellFactory.create(value.getZonedDateTime().get(ChronoField.MICRO_OF_SECOND));
                            }
                        });
                    }
                } else if (subsecondUnit.equals(NANOSECOND)) {
                    // in nanoseconds
                    final DataColumnSpec colSpec = createBoundedIntColumn(domainCreator, nameGenerator,
                        SUBSECOND_COL + " (in " + NANOSECOND + ")", 0, 999_999_999);
                    if (isLocalTime) {
                        rearranger.append(new AbstractLocalTimeFieldCellFactory(selectedColIdx, colSpec) {

                            @Override
                            protected DataCell getCell(final LocalTimeValue value) {
                                return IntCellFactory.create(value.getLocalTime().getNano());
                            }
                        });
                    } else if (isLocalDateTime) {
                        rearranger.append(new AbstractLocalDateTimeFieldCellFactory(selectedColIdx, colSpec) {

                            @Override
                            protected DataCell getCell(final LocalDateTimeValue value) {
                                return IntCellFactory.create(value.getLocalDateTime().getNano());
                            }
                        });
                    } else if (isZonedDateTime) {
                        rearranger.append(new AbstractZonedDateTimeFieldCellFactory(selectedColIdx, colSpec) {

                            @Override
                            protected DataCell getCell(final ZonedDateTimeValue value) {
                                return IntCellFactory.create(value.getZonedDateTime().getNano());
                            }
                        });
                    }
                }
            }
            if (isZonedDateTime) {

                // extract time zone fields:

                // time zone name:

                if (m_timeZoneNameModel.getBooleanValue()) {
                    final DataColumnSpec colSpec = nameGenerator.newColumn(TIME_ZONE_NAME, StringCell.TYPE);
                    rearranger.append(new AbstractZonedDateTimeFieldCellFactory(selectedColIdx, colSpec) {

                        @Override
                        protected DataCell getCell(final ZonedDateTimeValue value) {
                            return StringCellFactory.create(value.getZonedDateTime().getZone().getId().toString());
                        }
                    });
                }

                // time zone offset:

                if (m_timeZoneOffsetModel.getBooleanValue()) {
                    final DataColumnSpec colSpec = nameGenerator.newColumn(TIME_ZONE_OFFSET, StringCell.TYPE);
                    rearranger.append(new AbstractZonedDateTimeFieldCellFactory(selectedColIdx, colSpec) {

                        @Override
                        protected DataCell getCell(final ZonedDateTimeValue value) {
                            return StringCellFactory.create(
                                value.getZonedDateTime().getOffset().getDisplayName(TextStyle.FULL_STANDALONE, locale));
                        }
                    });
                }
            }
        }

        if (rearranger.getColumnCount() == spec.getNumColumns()) {
            getLogger().info("No fields will be extracted. Output table will equal input table.");
        }

        return rearranger;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_colSelectModel.saveSettingsTo(settings);
        for (final SettingsModelBoolean sm : m_dateModels) {
            sm.saveSettingsTo(settings);
        }
        for (final SettingsModelBoolean sm : m_timeModels) {
            sm.saveSettingsTo(settings);
        }
        m_subsecondUnitsModel.saveSettingsTo(settings);
        for (final SettingsModelBoolean sm : m_timeZoneModels) {
            sm.saveSettingsTo(settings);
        }
        m_localeModel.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_colSelectModel.validateSettings(settings);
        for (final SettingsModelBoolean sm : m_dateModels) {
            sm.validateSettings(settings);
        }
        for (final SettingsModelBoolean sm : m_timeModels) {
            sm.validateSettings(settings);
        }
        m_subsecondUnitsModel.validateSettings(settings);
        for (final SettingsModelBoolean sm : m_timeZoneModels) {
            sm.validateSettings(settings);
        }
        m_localeModel.validateSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_colSelectModel.loadSettingsFrom(settings);
        for (final SettingsModelBoolean sm : m_dateModels) {
            sm.loadSettingsFrom(settings);
        }
        for (final SettingsModelBoolean sm : m_timeModels) {
            sm.loadSettingsFrom(settings);
        }
        m_subsecondUnitsModel.loadSettingsFrom(settings);
        for (final SettingsModelBoolean sm : m_timeZoneModels) {
            sm.loadSettingsFrom(settings);
        }
        m_localeModel.loadSettingsFrom(settings);
    }

    private DataColumnSpec createBoundedIntColumn(final DataColumnDomainCreator domainCreator,
        final UniqueNameGenerator nameGenerator, final String suggestedName, final int lower, final int upper) {
        domainCreator.setLowerBound(IntCellFactory.create(lower));
        domainCreator.setUpperBound(IntCellFactory.create(upper));
        final DataColumnSpecCreator specCreator = nameGenerator.newCreator(suggestedName, IntCell.TYPE);
        specCreator.setDomain(domainCreator.createDomain());
        return specCreator.createSpec();
    }

    // cell factories:

    private abstract class AbstractLocalDateFieldCellFactory extends AbstractExtractFieldCellFactory<LocalDateValue> {
        AbstractLocalDateFieldCellFactory(final int colIdx, final DataColumnSpec newColSpec) {
            super(colIdx, newColSpec);
        }
    }

    private abstract class AbstractLocalTimeFieldCellFactory extends AbstractExtractFieldCellFactory<LocalTimeValue> {
        AbstractLocalTimeFieldCellFactory(final int colIdx, final DataColumnSpec newColSpec) {
            super(colIdx, newColSpec);
        }
    }

    private abstract class AbstractLocalDateTimeFieldCellFactory
        extends AbstractExtractFieldCellFactory<LocalDateTimeValue> {
        AbstractLocalDateTimeFieldCellFactory(final int colIdx, final DataColumnSpec newColSpec) {
            super(colIdx, newColSpec);
        }
    }

    private abstract class AbstractZonedDateTimeFieldCellFactory
        extends AbstractExtractFieldCellFactory<ZonedDateTimeValue> {
        AbstractZonedDateTimeFieldCellFactory(final int colIdx, final DataColumnSpec newColSpec) {
            super(colIdx, newColSpec);
        }
    }

    private abstract class AbstractExtractFieldCellFactory<V extends DataValue> extends SingleCellFactory {

        private final int m_colIdx;

        AbstractExtractFieldCellFactory(final int colIdx, final DataColumnSpec newColSpec) {
            super(true, newColSpec);
            m_colIdx = colIdx;
        }

        @Override
        public DataCell getCell(final DataRow row) {
            final DataCell cell = row.getCell(m_colIdx);
            if (cell.isMissing()) {
                return DataType.getMissingCell();
            }
            @SuppressWarnings("unchecked")
            final V value = (V)cell;
            return getCell(value);
        }

        protected abstract DataCell getCell(final V value);
    }
}
