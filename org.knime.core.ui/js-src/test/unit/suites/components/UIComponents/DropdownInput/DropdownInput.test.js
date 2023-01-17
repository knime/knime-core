import { mountJsonFormsComponent, initializesJsonFormsControl, mountJsonFormsComponentWithStore }
    from '~/test/unit/suites/utils/jsonFormsTestUtils';
import DropdownInput from '@/components/UIComponents/DropdownInput.vue';
import LabeledInput from '@/components/UIComponents/LabeledInput.vue';
import Dropdown from '~/webapps-common/ui/components/forms/Dropdown.vue';

describe('DropdownInput.vue', () => {
    let wrapper, onChangeSpy, handleChangeSpy, defaultPropsData;

    beforeAll(() => {
        onChangeSpy = jest.spyOn(DropdownInput.methods, 'onChange');
        handleChangeSpy = DropdownInput.methods.handleChange = jest.fn();
    });
    
    beforeEach(async () => {
        defaultPropsData = {
            control: {
                path: 'test',
                enabled: true,
                data: 'Universe_0_0',
                label: 'defaultLabel',
                schema: {
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
                uischema: {
                    type: 'Control',
                    scope: '#/properties/view/properties/yAxisColumn',
                    options: {
                        format: 'columnSelection',
                        showRowKeys: false,
                        showNoneColumn: false
                    }
                },
                rootSchema: {
                    hasNodeView: true,
                    flowVariablesMap: {
                        test: {
                            controllingFlowVariableAvailable: true,
                            controllingFlowVariableName: 'knime.test',
                            exposedFlowVariableName: 'test',
                            leaf: true
                        }
                    }
                }
            }
        };
        wrapper = await mountJsonFormsComponent(DropdownInput, defaultPropsData);
    });

    afterEach(() => {
        jest.clearAllMocks();
    });

    it('renders', () => {
        expect(wrapper.getComponent(DropdownInput).exists()).toBe(true);
        expect(wrapper.findComponent(LabeledInput).exists()).toBe(true);
        expect(wrapper.findComponent(Dropdown).exists()).toBe(true);
    });
    
    it('initializes jsonforms', () => {
        initializesJsonFormsControl(wrapper);
    });

    it('calls onChange when input is changed', async () => {
        const dirtySettingsMock = jest.fn();
        const localWrapper = await mountJsonFormsComponentWithStore(DropdownInput, defaultPropsData, {
            'pagebuilder/dialog': {
                actions: { dirtySettings: dirtySettingsMock },
                namespaced: true
            }
        });
        const changedDropdownInput = 'Shaken not stirred';
        localWrapper.findComponent(Dropdown).vm.$emit('input', changedDropdownInput);
        expect(onChangeSpy).toHaveBeenCalledWith(changedDropdownInput);
        expect(handleChangeSpy).toHaveBeenCalledWith(defaultPropsData.control.path, changedDropdownInput);
        expect(dirtySettingsMock).not.toHaveBeenCalled();
    });

    it('indicates model settings change when model setting is changed', async () => {
        const dirtySettingsMock = jest.fn();
        const localWrapper = await mountJsonFormsComponentWithStore(
            DropdownInput,
            {
                ...defaultPropsData,
                control: {
                    ...defaultPropsData.control,
                    uischema: {
                        ...defaultPropsData.control.schema,
                        scope: '#/properties/model/properties/yAxisColumn'
                    }
                }
            },
            {
                'pagebuilder/dialog': {
                    actions: { dirtySettings: dirtySettingsMock },
                    namespaced: true
                }
            }
        );
        const changedDropdownInput = 'Shaken not stirred';
        localWrapper.findComponent(Dropdown).vm.$emit('input', changedDropdownInput);
        expect(dirtySettingsMock).toHaveBeenCalledWith(expect.anything(), true, expect.undefined);
        expect(handleChangeSpy).toHaveBeenCalledWith(defaultPropsData.control.path, changedDropdownInput);
    });

    it('sets correct initial value', () => {
        expect(wrapper.findComponent(Dropdown).vm.value).toBe(defaultPropsData.control.data);
    });

    it('sets correct label', () => {
        expect(wrapper.findComponent('label').text()).toBe(defaultPropsData.control.label);
    });

    it('disables dropdown when controlled by a flow variable', () => {
        expect(wrapper.vm.disabled).toBeTruthy();
        expect(wrapper.findComponent(Dropdown).vm.disabled).toBeTruthy();
    });

    it('does not disable dropdown when not controlled by a flow variable', async () => {
        delete defaultPropsData.control.rootSchema.flowVariablesMap;
        wrapper = await mountJsonFormsComponent(DropdownInput, defaultPropsData);
        expect(wrapper.vm.disabled).toBeFalsy();
        expect(wrapper.findComponent(Dropdown).vm.disabled).toBeFalsy();
    });
});
