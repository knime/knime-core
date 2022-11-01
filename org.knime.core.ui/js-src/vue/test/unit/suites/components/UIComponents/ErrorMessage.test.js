import { shallowMount } from '@vue/test-utils';
import ErrorMessage from '@/components/UIComponents/ErrorMessage.vue';

describe('ErrorMessage.vue', () => {
    const defaultPropsData = {
        errors: [
            'First error',
            'Second error'
        ]
    };

    it('renders', () => {
        const wrapper = shallowMount(ErrorMessage, {
            propsData: defaultPropsData
        });
        expect(wrapper.getComponent(ErrorMessage).exists()).toBe(true);
    });
});
