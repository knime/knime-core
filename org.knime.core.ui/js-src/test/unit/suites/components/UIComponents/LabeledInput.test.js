import { mount } from '@vue/test-utils';

import LabeledInput from '@/components/UIComponents/LabeledInput.vue';
import ErrorMessage from '@/components/UIComponents/ErrorMessage.vue';
import FlowVariableIcon from '@/components/UIComponents/FlowVariableIcon.vue';
import DescriptionPopover from '@/components/UIComponents/DescriptionPopover.vue';
import ReexecutionIcon from '~/webapps-common/ui/assets/img/icons/reexecution.svg?inline';
import BothFlowVariables from '~/webapps-common/ui/assets/img/icons/both-flow-variables.svg?inline';
import OnlyFlowVariable from '~/webapps-common/ui/assets/img/icons/only-flow-variables.svg?inline';
import ExposeFlowVariable from '~/webapps-common/ui/assets/img/icons/expose-flow-variables.svg?inline';

describe('LabeledInput.vue', () => {
    it('renders', () => {
        const wrapper = mount(LabeledInput);
        expect(wrapper.getComponent(LabeledInput).exists()).toBe(true);
        expect(wrapper.findComponent(ErrorMessage).exists()).toBe(true);
        const icon = wrapper.findComponent(ReexecutionIcon);
        expect(icon.exists()).toBe(false);
    });

    it('visually displays model settings', () => {
        const wrapper = mount(LabeledInput, { propsData: { showReexecutionIcon: true } });
        expect(wrapper.vm.showReexecutionIcon).toBe(true);
        const icon = wrapper.findComponent(ReexecutionIcon);
        expect(icon.exists()).toBe(true);
    });

    it('renders the description popover', async () => {
        const wrapper = mount(LabeledInput);
        expect(wrapper.findComponent(DescriptionPopover).exists()).toBe(false);
        wrapper.setProps({ description: 'foo' });
        await wrapper.vm.$nextTick(); // wait until pending promises are resolved
        expect(wrapper.findComponent(DescriptionPopover).exists()).toBe(true);
    });

    // FIXME: UIEXT-253 - this needs to be added again once errors are properly passed and displayed
    /* it('renders error message on error', () => {
        const wrapper = mount(LabeledInput, { propsData: { errors: ['test error'] } });
        expect(wrapper.getComponent(ErrorMessage).props().errors).toStrictEqual(['test error']);
    }); */
    it('renders both icons rendered when controlled and exposed by a flow variable', () => {
        const wrapper = mount(LabeledInput, {
            propsData: {
                flowSettings: {
                    controllingFlowVariableAvailable: true,
                    controllingFlowVariableName: 'knime.test',
                    exposedFlowVariableName: 'test',
                    leaf: true
                }
            }
        });
        expect(Boolean(wrapper.findComponent(FlowVariableIcon).vm.isControlledByFlowVariable)).toBe(true);
        expect(Boolean(wrapper.findComponent(FlowVariableIcon).vm.isExposedFlowVariable)).toBe(true);
        const icon = wrapper.findComponent(BothFlowVariables);
        expect(icon.exists()).toBe(true);
    });

    it('renders exposedFlowVariable icon when exposed flow variable exists', () => {
        const wrapper = mount(LabeledInput, {
            propsData: {
                flowSettings: {
                    controllingFlowVariableAvailable: true,
                    controllingFlowVariableName: null,
                    exposedFlowVariableName: 'test',
                    leaf: true
                }
            }
        });
        expect(Boolean(wrapper.findComponent(FlowVariableIcon).vm.isControlledByFlowVariable)).toBe(false);
        expect(Boolean(wrapper.findComponent(FlowVariableIcon).vm.isExposedFlowVariable)).toBe(true);
        const icon = wrapper.findComponent(ExposeFlowVariable);
        expect(icon.exists()).toBe(true);
    });

    it('renders onlyFlowVariable icon when controlled by flow variable', () => {
        const wrapper = mount(LabeledInput, {
            propsData: {
                flowSettings: {
                    controllingFlowVariableAvailable: true,
                    controllingFlowVariableName: 'knime.test',
                    exposedFlowVariableName: null,
                    leaf: true
                }
            }
        });
        expect(Boolean(wrapper.findComponent(FlowVariableIcon).vm.isControlledByFlowVariable)).toBe(true);
        expect(Boolean(wrapper.findComponent(FlowVariableIcon).vm.isExposedFlowVariable)).toBe(false);
        const icon = wrapper.findComponent(OnlyFlowVariable);
        expect(icon.exists()).toBe(true);
    });
});
