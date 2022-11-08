import { mount } from '@vue/test-utils';

import FlowVariableIcon from '@/components/UIComponents/FlowVariableIcon.vue';
import OnlyFlowVariable from '~/webapps-common/ui/assets/img/icons/only-flow-variables.svg?inline';
import ExposeFlowVariable from '~/webapps-common/ui/assets/img/icons/expose-flow-variables.svg?inline';
import BothFlowVariables from '~/webapps-common/ui/assets/img/icons/both-flow-variables.svg?inline';

describe('FlowVariableIcon.vue', () => {
    it('renders', () => {
        const wrapper = mount(FlowVariableIcon);
        expect(wrapper.getComponent(FlowVariableIcon).exists()).toBe(true);
    });

    it('renders onlyFlowVariable icon when controlled by flow variable', () => {
        const wrapper = mount(FlowVariableIcon, {
            propsData: {
                flowSettings: {
                    controllingFlowVariableAvailable: true,
                    controllingFlowVariableName: 'knime.test',
                    exposedFlowVariableName: null,
                    leaf: true
                }
            }
        });
        expect(Boolean(wrapper.getComponent(FlowVariableIcon).vm.isControlledByFlowVariable)).toBe(true);
        expect(Boolean(wrapper.getComponent(FlowVariableIcon).vm.isExposedFlowVariable)).toBe(false);
        const icon = wrapper.findComponent(OnlyFlowVariable);
        expect(icon.exists()).toBe(true);
    });

    it('renders both icons when controlled and exposed by a flow variable', () => {
        const wrapper = mount(FlowVariableIcon, {
            propsData: {
                flowSettings: {
                    controllingFlowVariableAvailable: true,
                    controllingFlowVariableName: 'knime.test',
                    exposedFlowVariableName: 'test',
                    leaf: true
                }
            }
        });
        expect(Boolean(wrapper.getComponent(FlowVariableIcon).vm.isControlledByFlowVariable)).toBe(true);
        expect(Boolean(wrapper.getComponent(FlowVariableIcon).vm.isExposedFlowVariable)).toBe(true);
        const icon = wrapper.findComponent(BothFlowVariables);
        expect(icon.exists()).toBe(true);
    });

    it('renders exposedFlowVariable icon when exposed flow variable exists', () => {
        const wrapper = mount(FlowVariableIcon, {
            propsData: {
                flowSettings: {
                    controllingFlowVariableAvailable: true,
                    controllingFlowVariableName: null,
                    exposedFlowVariableName: 'test',
                    leaf: true
                }
            }
        });
        expect(Boolean(wrapper.getComponent(FlowVariableIcon).vm.isControlledByFlowVariable)).toBe(false);
        expect(Boolean(wrapper.getComponent(FlowVariableIcon).vm.isExposedFlowVariable)).toBe(true);
        const icon = wrapper.findComponent(ExposeFlowVariable);
        expect(icon.exists()).toBe(true);
    });
});
