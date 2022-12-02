import { mountJsonFormsComponent, initializesJsonFormsControl, mountJsonFormsComponentWithStore }
    from '~/test/unit/suites/utils/jsonFormsTestUtils';
import RadioInputBase from '@/components/UIComponents/RadioInputBase.vue';
import LabeledInput from '@/components/UIComponents/LabeledInput.vue';
import RadioButtons from '~/webapps-common/ui/components/forms/RadioButtons.vue';
import ValueSwitch from '~/webapps-common/ui/components/forms/ValueSwitch.vue';
import BaseRadioButtons from '~/webapps-common/ui/components/forms/BaseRadioButtons.vue';
import OnlyFlowVariable from '~/webapps-common/ui/assets/img/icons/only-flow-variables.svg?inline';
import BothFlowVariables from '~/webapps-common/ui/assets/img/icons/both-flow-variables.svg?inline';
import ExposeFlowVariable from '~/webapps-common/ui/assets/img/icons/expose-flow-variables.svg?inline';

describe('RadioInputBase.vue', () => {
    const defaultPropsData = {
        type: 'radio',
        control: {
            path: 'test',
            enabled: true,
            visible: true,
            label: 'defaultLabel',
            data: 'LOG',
            schema: {
                oneOf: [
                    {
                        const: 'LOG',
                        title: 'Logarithmic'
                    },
                    {
                        const: 'VALUE',
                        title: 'Linear'
                    }
                ]
            },
            uischema: {
                type: 'Control',
                scope: '#/properties/testScale',
                options: {
                    format: 'radio',
                    radioLayout: 'horizontal'
                }
            },
            rootSchema: {
                hasNodeView: true,
                flowVariablesMap: {}
            }
        }
    };

    let wrapper, onChangeSpy, handleChangeSpy;

    beforeAll(() => {
        onChangeSpy = jest.spyOn(RadioInputBase.methods, 'onChange');
        handleChangeSpy = RadioInputBase.methods.handleChange = jest.fn();
    });

    beforeEach(async () => {
        wrapper = await mountJsonFormsComponent(RadioInputBase, defaultPropsData);
    });

    afterEach(() => {
        jest.clearAllMocks();
    });

    it('renders', () => {
        expect(wrapper.getComponent(RadioInputBase).exists()).toBe(true);
        expect(wrapper.findComponent(LabeledInput).exists()).toBe(true);
        expect(wrapper.findComponent(RadioButtons).exists()).toBe(true);
        expect(wrapper.findComponent(BaseRadioButtons).exists()).toBe(true);
    });

    it('initializes jsonforms', () => {
        initializesJsonFormsControl(wrapper);
    });

    const createTypedWrapper = async (type) => {
        const wrapper = await mountJsonFormsComponentWithStore(
            RadioInputBase,
            {
                ...defaultPropsData,
                type,
                control: {
                    ...defaultPropsData.control,
                    uischema: {
                        ...defaultPropsData.control.schema,
                        scope: '#/properties/model/properties/testColumn'
                    }
                }
            }
        );
        return wrapper;
    };

    const testTypes = [
        ['radio', RadioButtons],
        ['valueSwitch', ValueSwitch],
        ['unknown', RadioButtons]
    ];

    it.each(testTypes)('renders explicit type %s', async (type, component) => {
        const localWrapper = await createTypedWrapper(type);
        expect(localWrapper.getComponent(RadioInputBase).exists()).toBe(true);
        expect(localWrapper.findComponent(LabeledInput).exists()).toBe(true);
        expect(localWrapper.findComponent(BaseRadioButtons).exists()).toBe(true);
        expect(localWrapper.getComponent(component).exists()).toBe(true);
    });

    it.each(testTypes)('initializes jsonforms for type %s', async (type, component) => {
        const localWrapper = await createTypedWrapper(type);
        initializesJsonFormsControl(localWrapper);
    });

    it('calls onChange when radio button is changed', async () => {
        const dirtySettingsMock = jest.fn();
        const localWrapper = await mountJsonFormsComponentWithStore(RadioInputBase, defaultPropsData, {
            'pagebuilder/dialog': {
                actions: { dirtySettings: dirtySettingsMock },
                namespaced: true
            }
        });
        const changedRadioInputBase = 'Shaken not stirred';
        localWrapper.findComponent(RadioButtons).vm.$emit('input', changedRadioInputBase);
        expect(onChangeSpy).toHaveBeenCalledWith(changedRadioInputBase);
        expect(handleChangeSpy).toHaveBeenCalledWith(defaultPropsData.control.path, changedRadioInputBase);
        expect(dirtySettingsMock).not.toHaveBeenCalled();
    });

    it('indicates model settings change when model setting is changed', async () => {
        const dirtySettingsMock = jest.fn();
        const localWrapper = await mountJsonFormsComponentWithStore(
            RadioInputBase,
            {
                ...defaultPropsData,
                control: {
                    ...defaultPropsData.control,
                    uischema: {
                        ...defaultPropsData.control.schema,
                        scope: '#/properties/model/properties/testColumn'
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
        const changedRadioInputBase = 'Shaken not stirred';
        localWrapper.findComponent(RadioButtons).vm.$emit('input', changedRadioInputBase);
        expect(dirtySettingsMock).toHaveBeenCalledWith(expect.anything(), true, expect.undefined);
        expect(handleChangeSpy).toHaveBeenCalledWith(defaultPropsData.control.path, changedRadioInputBase);
    });

    it('sets correct initial value', () => {
        expect(wrapper.findComponent(BaseRadioButtons).vm.value).toBe(defaultPropsData.control.data);
    });

    it('sets correct label', () => {
        expect(wrapper.findComponent('label').text()).toBe(defaultPropsData.control.label);
    });

    it('sets correct possible values', () => {
        const possibleValues = [{ id: 'LOG', text: 'Logarithmic' }, { id: 'VALUE', text: 'Linear' }];
        expect(wrapper.findComponent(BaseRadioButtons).props().possibleValues).toStrictEqual(possibleValues);
    });

    it('disables radioInput when controlled by a flow variable', () => {
        const localDefaultPropsData = JSON.parse(JSON.stringify(defaultPropsData));
        localDefaultPropsData.control.rootSchema
            .flowVariablesMap[defaultPropsData.control.path] = {
                controllingFlowVariableAvailable: true,
                controllingFlowVariableName: 'knime.test',
                exposedFlowVariableName: 'test',
                leaf: true
            };

        const localWrapper = mountJsonFormsComponent(RadioInputBase, localDefaultPropsData);
        expect(localWrapper.vm.disabled).toBeTruthy();
    });

    it('does not disable radioInput when not controlled by a flow variable', () => {
        defaultPropsData.control.rootSchema
            .flowVariablesMap[defaultPropsData.control.path] = {};
        expect(wrapper.vm.disabled).toBeFalsy();
    });

    it('renders both icons when controlled and exposed by a flow variable', () => {
        defaultPropsData.control.rootSchema
            .flowVariablesMap[defaultPropsData.control.path] = {
                controllingFlowVariableAvailable: true,
                controllingFlowVariableName: 'knime.test',
                exposedFlowVariableName: 'test',
                leaf: true
            };

        const localWrapper = mountJsonFormsComponent(RadioInputBase, defaultPropsData);
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

        const localWrapper = mountJsonFormsComponent(RadioInputBase, defaultPropsData);
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

        const localWrapper = mountJsonFormsComponent(RadioInputBase, defaultPropsData);
        const icon = localWrapper.findComponent(OnlyFlowVariable);
        expect(icon.exists()).toBe(true);
    });
});
