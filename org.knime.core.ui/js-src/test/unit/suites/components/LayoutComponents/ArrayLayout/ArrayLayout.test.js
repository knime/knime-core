import { mountJsonFormsComponent, initializesJsonFormsArrayControl } from '~/test/unit/suites/utils/jsonFormsTestUtils';
import ArrayLayout from '@/components/LayoutComponents/ArrayLayout';

describe('ArrayLayout.vue', () => {
    const defaultPropsData = {
        control: {
            cells: [],
            data: [{
                borderStyle: 'DASHED',
                color: 'blue',
                label: undefined,
                size: 1,
                value: '0'
            }],
            path: 'view/referenceLines',
            schema: {
                type: 'object',
                properties: {
                    borderStyle: {
                        oneOf: [{
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
                        }],
                        title: 'Borderstyle',
                        default: 'DASHED'
                    },
                    color: {
                        type: 'string',
                        title: 'Color',
                        default: 'blue'
                    },
                    label: {
                        type: 'string',
                        title: 'Label'
                    },
                    size: {
                        type: 'integer',
                        format: 'int32',
                        title: 'Size',
                        default: 1,
                        minimum: 0,
                        maximum: 10
                    },
                    value: {
                        type: 'string',
                        title: 'Value',
                        default: '0'
                    }
                    
                }
            },
            uischema: {
                type: 'Control',
                scope: '#/properties/view/properties/referenceLines',
                options: {
                    details: {
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
                                { type: 'Control', scope: '#/properties/size' },
                                { type: 'Control', scope: '#/properties/color' }
                            ]
                        }
                    }
                }
            }
        }
    };

    let wrapper;

    beforeEach(async () => {
        wrapper = await mountJsonFormsComponent(ArrayLayout, defaultPropsData);
    });

    afterEach(() => {
        jest.clearAllMocks();
    });

    it('renders', () => {
        expect(wrapper.getComponent(ArrayLayout).exists()).toBe(true);
    });
    
    it('initializes jsonforms', () => {
        initializesJsonFormsArrayControl(wrapper);
    });

    it('creates a default value', () => {
        const expectedDefaultValue = {
            borderStyle: 'DASHED',
            color: 'blue',
            label: undefined,
            size: 1,
            value: '0'
        };
        expect(wrapper.getComponent(ArrayLayout).vm.createDefaultValue(defaultPropsData.control.schema))
            .toStrictEqual(expectedDefaultValue);
    });

    it('adds default item', async () => {
        const addItemSpy = ArrayLayout.methods.addItem = jest.fn().mockReturnValue(() => false);
        wrapper = await mountJsonFormsComponent(ArrayLayout, defaultPropsData);
        wrapper.vm.addDefaultItem();
        expect(addItemSpy).toHaveBeenCalled();
    });


    it('deletes item', async () => {
        const deleteItemSpy = ArrayLayout.methods.deleteItem = jest.fn().mockReturnValue(() => false);
        wrapper = await mountJsonFormsComponent(ArrayLayout, defaultPropsData);
        wrapper.vm.deleteItem();
        expect(deleteItemSpy).toHaveBeenCalled();
    });
});
