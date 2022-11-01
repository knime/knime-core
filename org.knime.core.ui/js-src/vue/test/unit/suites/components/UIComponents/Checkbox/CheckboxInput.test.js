import { mountJsonFormsComponent, initializesJsonFormsControl, mountJsonFormsComponentWithStore } from
    '~/test/unit/suites/utils/jsonFormsTestUtils';
import CheckboxInput from '@/components/UIComponents/CheckboxInput.vue';
import ErrorMessage from '@/components/UIComponents/ErrorMessage.vue';
import FlowVariableIcon from '@/components/UIComponents/FlowVariableIcon.vue';
import ReexecutionIcon from '~/webapps-common/ui/assets/img/icons/reexecution.svg?inline';
import DescriptionPopover from '@/components/UIComponents/DescriptionPopover.vue';
import Checkbox from '~/webapps-common/ui/components/forms/Checkbox.vue';
import BothFlowVariables from '~/webapps-common/ui/assets/img/icons/both-flow-variables.svg?inline';
import OnlyFlowVariable from '~/webapps-common/ui/assets/img/icons/only-flow-variables.svg?inline';
import ExposeFlowVariable from '~/webapps-common/ui/assets/img/icons/expose-flow-variables.svg?inline';

describe('CheckboxInput.vue', () => {
    let wrapper, onChangeSpy, handleChangeSpy, defaultPropsData;

    beforeAll(() => {
        onChangeSpy = jest.spyOn(CheckboxInput.methods, 'onChange');
        handleChangeSpy = CheckboxInput.methods.handleChange = jest.fn();
    });
    
    beforeEach(async () => {
        defaultPropsData = {
            control: {
                path: 'test',
                enabled: true,
                visible: true,
                label: 'defaultLabel',
                data: true,
                schema: {
                    properties: {
                        showTooltip: {
                            type: 'boolean',
                            title: 'Show tooltip'
                        }
                    }
                },
                uischema: {
                    type: 'Control',
                    scope: '#/properties/showTooltip',
                    options: {
                        format: 'checkbox'
                    }
                },
                rootSchema: {
                    flowVariablesMap: {}
                }
            }
        };
        wrapper = await mountJsonFormsComponent(CheckboxInput, defaultPropsData);
    });

    afterEach(() => {
        jest.clearAllMocks();
    });
    
    it('renders', () => {
        expect(wrapper.getComponent(CheckboxInput).exists()).toBe(true);
        expect(wrapper.getComponent(Checkbox).exists()).toBe(true);
        expect(wrapper.getComponent(ErrorMessage).exists()).toBe(true);
        expect(wrapper.findComponent(ReexecutionIcon).exists()).toBe(false);
    });

    it('renders the description popover', async () => {
        expect(wrapper.findComponent(DescriptionPopover).exists()).toBe(false);
        wrapper.setProps({ control: { description: 'foo' } });
        await wrapper.vm.$nextTick(); // wait until pending promises are resolved
        expect(wrapper.findComponent(DescriptionPopover).exists()).toBe(true);
    });
    
    it('initializes jsonforms', () => {
        initializesJsonFormsControl(wrapper);
    });

    it('calls onChange when checkbox is changed', async () => {
        const dirtySettingsMock = jest.fn();
        const localWrapper = await mountJsonFormsComponentWithStore(CheckboxInput, defaultPropsData, {
            'pagebuilder/dialog': {
                actions: { dirtySettings: dirtySettingsMock },
                namespaced: true
            }
        });
        localWrapper.findComponent(Checkbox).vm.$emit('input', true);
        expect(onChangeSpy).toHaveBeenCalledWith(true);
        expect(handleChangeSpy).toHaveBeenCalledWith(defaultPropsData.control.path, true);
        expect(dirtySettingsMock).not.toHaveBeenCalled();
    });

    it('indicates model settings change when model setting is changed', async () => {
        const dirtySettingsMock = jest.fn();
        const localWrapper = await mountJsonFormsComponentWithStore(
            CheckboxInput,
            {
                ...defaultPropsData,
                control: {
                    ...defaultPropsData.control,
                    uischema: {
                        ...defaultPropsData.control.schema,
                        scope: '#/properties/model/filterMissingValues'
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
        localWrapper.findComponent(Checkbox).vm.$emit('input', true);
        expect(onChangeSpy).toHaveBeenCalledWith(true);
        expect(handleChangeSpy).toHaveBeenCalledWith(defaultPropsData.control.path, true);
        expect(dirtySettingsMock).toHaveBeenCalledWith(expect.anything(), true, expect.undefined);
    });

    it('checks that re-execution icon is present if it is a model setting', async () => {
        const localWrapper = await mountJsonFormsComponent(
            CheckboxInput,
            {
                ...defaultPropsData,
                control: {
                    ...defaultPropsData.control,
                    uischema: {
                        ...defaultPropsData.control.schema,
                        scope: '#/properties/model/filterMissingValues'
                    }
                }
            }
        );
        expect(localWrapper.findComponent(ReexecutionIcon).exists()).toBe(true);
    });

    it('sets correct initial value', () => {
        expect(wrapper.findComponent(Checkbox).vm.value).toBe(defaultPropsData.control.data);
    });

    it('sets correct label', () => {
        expect(wrapper.findComponent('label').text()).toBe(defaultPropsData.control.label);
    });

    it('disables input when controlled by a flow variable', () => {
        defaultPropsData.control.rootSchema
            .flowVariablesMap[defaultPropsData.control.path] = {
                controllingFlowVariableAvailable: true,
                controllingFlowVariableName: 'knime.test',
                exposedFlowVariableName: 'test',
                leaf: true
            };
        const localWrapper = mountJsonFormsComponent(CheckboxInput, defaultPropsData);
        expect(localWrapper.vm.disabled).toBeTruthy();
    });

    it('renders both icons when controlled and exposed by a flow variable', () => {
        defaultPropsData.control.rootSchema
            .flowVariablesMap[defaultPropsData.control.path] = {
                controllingFlowVariableAvailable: true,
                controllingFlowVariableName: 'knime.test',
                exposedFlowVariableName: 'test',
                leaf: true
            };
        
        const localWrapper = mountJsonFormsComponent(CheckboxInput, defaultPropsData);
        expect(Boolean(localWrapper.findComponent(FlowVariableIcon).vm.isControlledByFlowVariable)).toBe(true);
        expect(Boolean(localWrapper.findComponent(FlowVariableIcon).vm.isExposedFlowVariable)).toBe(true);
        
        const icon = localWrapper.findComponent(BothFlowVariables);
        expect(icon.exists()).toBe(true);
    });

    it('renders exposedFlowVariable icon when exposed flow variable exists', () => {
        defaultPropsData.control.rootSchema
            .flowVariablesMap[defaultPropsData.control.path] = {
                controllingFlowVariableAvailable: true,
                controllingFlowVariableName: null,
                exposedFlowVariableName: 'test',
                leaf: true
            };

        const localWrapper = mountJsonFormsComponent(CheckboxInput, defaultPropsData);
        expect(Boolean(localWrapper.findComponent(FlowVariableIcon).vm.isControlledByFlowVariable)).toBe(false);
        expect(Boolean(localWrapper.findComponent(FlowVariableIcon).vm.isExposedFlowVariable)).toBe(true);
        
        const icon = localWrapper.findComponent(ExposeFlowVariable);
        expect(icon.exists()).toBe(true);
    });

    it('renders onlyFlowVariable icon when controlled by a flow variable', () => {
        defaultPropsData.control.rootSchema
            .flowVariablesMap[defaultPropsData.control.path] = {
                controllingFlowVariableAvailable: true,
                controllingFlowVariableName: 'knime.test',
                exposedFlowVariableName: null,
                leaf: true
            };

        const localWrapper = mountJsonFormsComponent(CheckboxInput, defaultPropsData);
        expect(Boolean(localWrapper.findComponent(FlowVariableIcon).vm.isControlledByFlowVariable)).toBe(true);
        expect(Boolean(localWrapper.findComponent(FlowVariableIcon).vm.isExposedFlowVariable)).toBe(false);

        const icon = localWrapper.findComponent(OnlyFlowVariable);
        expect(icon.exists()).toBe(true);
    });
});
