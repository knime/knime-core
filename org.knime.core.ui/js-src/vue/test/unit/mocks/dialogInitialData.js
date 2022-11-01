export const dialogInitialData = {
    data: {
        view: {
            title: 'Scatter Plot',
            yAxisScale: 'VALUE',
            xAxisColumn: 'Universe_1_1',
            yAxisColumn: 'Universe_0_1',
            xAxisLabel: '',
            yAxisLabel: '',
            frequencyColumns: ['Universe_0_0', 'Universe_0_1', 'Universe_1_0', 'Universe_1_1'],
            referenceLines: [
                {
                    borderStyle: null,
                    color: '',
                    label: '',
                    size: 0,
                    value: 0
                }
            ]
        }
    },
    schema: {
        type: 'object',
        properties: {
            view: {
                type: 'object',
                properties: {
                    title: {
                        type: 'string',
                        title: 'Title',
                        description: 'some description'
                    },
                    xAxisColumn: {
                        oneOf: [
                            {
                                const: 'Universe_0_0',
                                title: 'Universe_0_0'
                            },
                            {
                                const: 'Universe_0_1',
                                title: 'Universe_0_1'
                            },
                            {
                                const: 'Universe_1_0',
                                title: 'Universe_1_0'
                            },
                            {
                                const: 'Universe_1_1',
                                title: 'Universe_1_1'
                            }
                        ],
                        title: 'X Axis Column'
                    },
                    xAxisLabel: {
                        type: 'string',
                        title: 'X Axis Label'
                    },
                    yAxisColumn: {
                        oneOf: [
                            {
                                const: 'Universe_0_0',
                                title: 'Universe_0_0'
                            },
                            {
                                const: 'Universe_0_1',
                                title: 'Universe_0_1'
                            },
                            {
                                const: 'Universe_1_0',
                                title: 'Universe_1_0'
                            },
                            {
                                const: 'Universe_1_1',
                                title: 'Universe_1_1'
                            }
                        ],
                        title: 'Y Axis Column'
                    },
                    yAxisLabel: {
                        type: 'string',
                        title: 'Y Axis Label'
                    },
                    yAxisScale: {
                        oneOf: [
                            {
                                const: 'LOG',
                                title: 'Logarithmic'
                            },
                            {
                                const: 'VALUE',
                                title: 'Linear'
                            }
                        ],
                        title: 'Y Axis Scale'
                    },
                    showTooltip: {
                        type: 'boolean',
                        title: 'Show tooltip'
                    },
                    maxRows: {
                        type: 'integer',
                        title: 'Show tooltip'
                    },
                    fraction: {
                        type: 'number',
                        title: 'Show tooltip'
                    },
                    frequencyColumns: {
                        anyOf: [
                            {
                                const: 'Universe_0_0',
                                title: 'Universe_0_0'
                            },
                            {
                                const: 'Universe_0_1',
                                title: 'Universe_0_1'
                            },
                            {
                                const: 'Universe_1_0',
                                title: 'Universe_1_0'
                            },
                            {
                                const: 'Universe_1_1',
                                title: 'Universe_1_1'
                            }
                        ],
                        title: 'Frequency column selection'
                    },
                    referenceLines: {
                        type: 'array',
                        items: {
                            type: 'object',
                            properties: {
                                borderStyle: {
                                    oneOf: [
                                        {
                                            const: 'DASHED',
                                            title: 'Dashed'
                                        },
                                        {
                                            const: 'DOTTED',
                                            title: 'Dotted'
                                        },
                                        {
                                            const: 'SOLID',
                                            title: 'Solid'
                                        }
                                    ],
                                    title: 'Borderstyle'
                                },
                                color: {
                                    type: 'string',
                                    title: 'Color'
                                },
                                label: {
                                    type: 'string',
                                    title: 'Label'
                                },
                                size: {
                                    type: 'integer',
                                    title: 'Size',
                                    minimum: 0,
                                    maximum: 10
                                },
                                value: {
                                    type: 'string',
                                    title: 'Value'
                                }
                            }
                        }
                    }
                }
            }
        }
    },
    ui_schema: { // eslint-disable-line camelcase
        elements: [
            {
                type: 'Section',
                label: 'Style',
                elements: [
                    {
                        type: 'Control',
                        scope: '#/properties/view/properties/title'
                    },
                    {
                        type: 'Control',
                        scope: '#/properties/view/properties/yAxisScale',
                        options: {
                            format: 'radio',
                            radioLayout: 'horizontal'
                        }
                    },
                    {
                        type: 'Control',
                        scope: '#/properties/view/properties/maxRows',
                        options: {
                            format: 'integer'
                        }
                    },
                    {
                        type: 'Control',
                        scope: '#/properties/view/properties/fraction',
                        options: {
                            format: 'number'
                        }
                    },
                    {
                        type: 'Control',
                        scope: '#/properties/view/properties/showTooltip',
                        options: {
                            format: 'checkbox'
                        }
                    }
                ]
            },
            {
                type: 'Section',
                label: 'Data',
                elements: [
                    {
                        type: 'Control',
                        scope: '#/properties/view/properties/xAxisColumn',
                        options: {
                            format: 'columnSelection',
                            showRowKeys: false,
                            showNoneColumn: false
                        }
                    },
                    {
                        type: 'Control',
                        scope: '#/properties/view/properties/yAxisColumn',
                        options: {
                            format: 'columnSelection',
                            showRowKeys: false,
                            showNoneColumn: false
                        }
                    },
                    {
                        type: 'Control',
                        scope: '#/properties/view/properties/xAxisLabel'
                    },
                    {
                        type: 'Control',
                        scope: '#/properties/view/properties/yAxisLabel'
                    },
                    {
                        type: 'Control',
                        scope: '#/properties/model/properties/frequencyColumns',
                        options: {
                            format: 'columnFilter',
                            allowEmptyInclude: true,
                            twinListSize: 7,
                            twinListLabelLeft: 'Excluded Columns',
                            twinListLabelRight: 'Included Columns'
                        },
                        rule: {
                            effect: 'DISABLE',
                            condition: {
                                scope: '#/properties/model/properties/aggregationMethod',
                                schema: { oneOf: [{ const: ['COUNT'] }] }
                            }
                        }
                    }
                ]
            },
            {
                type: 'Section',
                label: 'Reference Lines',
                elements: [
                    {
                        type: 'Control',
                        scope: '#/properties/view/properties/referenceLines',
                        options: {
                            detail: {
                                value: {
                                    type: 'Control',
                                    scope: '#/properties/value'
                                },
                                label: {
                                    type: 'Control',
                                    scope: '#/properties/label'
                                },
                                borderStyle: {
                                    type: 'Control',
                                    scope: '#/properties/borderStyle',
                                    options: {
                                        format: 'radio',
                                        radioLayout: 'horizontal'
                                    }
                                },
                                horizontalLayout: {
                                    type: 'HorizontalLayout',
                                    elements: [
                                        {
                                            type: 'Control',
                                            scope: '#/properties/size'
                                        },
                                        {
                                            type: 'Control',
                                            scope: '#/properties/color'
                                        }
                                    ]
                                }
                            }
                        }
                    }
                ]
            }
        ]
    }
};

export const expectedRenderers = [
    { scope: '#/properties/view/properties/title', component: 'TextInput' },
    { scope: '#/properties/view/properties/yAxisScale', component: 'RadioInput' },
    { scope: '#/properties/view/properties/maxRows', component: 'IntegerInput' },
    { scope: '#/properties/view/properties/fraction', component: 'IntegerInput' },
    { scope: '#/properties/view/properties/showTooltip', component: 'CheckboxInput' },
    { scope: '#/properties/view/properties/xAxisColumn', component: 'ColumnSelect' },
    { scope: '#/properties/view/properties/yAxisColumn', component: 'ColumnSelect' },
    { scope: '#/properties/view/properties/xAxisLabel', component: 'TextInput' },
    { scope: '#/properties/view/properties/yAxisLabel', component: 'TextInput' },
    { scope: '#/properties/model/properties/frequencyColumns', component: '' },
    { scope: '#/properties/view/properties/referenceLines', component: 'ArrayLayout' }
];
