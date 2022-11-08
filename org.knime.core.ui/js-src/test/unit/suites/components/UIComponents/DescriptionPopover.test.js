import { mount } from '@vue/test-utils';

import DescriptionPopover from '@/components/UIComponents/DescriptionPopover.vue';

describe('DescriptionPopover.vue', () => {
    let toggleSpy, closeSpy;

    beforeAll(() => {
        toggleSpy = jest.spyOn(DescriptionPopover.methods, 'toggle');
        closeSpy = jest.spyOn(DescriptionPopover.methods, 'close');
    });

    afterEach(() => {
        jest.clearAllMocks();
    });

    it('renders the default state', () => {
        const wrapper = mount(DescriptionPopover);

        expect(wrapper.find('.popover').exists()).toBeTruthy();
        expect(wrapper.find('.button').exists()).toBeTruthy();
        expect(wrapper.find('svg').exists()).toBeTruthy();
        expect(wrapper.find('.box').exists()).toBeFalsy();
    });

    it('renders the content box when expanded', async () => {
        const wrapper = mount(DescriptionPopover, { propsData: { hover: false, html: 'foo' } });
        wrapper.setData({ expanded: true });
        await wrapper.vm.$nextTick(); // wait until pending promises are resolved

        expect(wrapper.find('.popover').exists()).toBeTruthy();
        expect(wrapper.find('.button').exists()).toBeTruthy();
        expect(wrapper.find('svg').exists()).toBeTruthy();
        expect(wrapper.find('.box').exists()).toBeTruthy();
        expect(wrapper.find('.content').exists()).toBeTruthy();
        expect(wrapper.find('.content').text()).toContain('foo');
    });

    it('shows description button when hovering', async () => {
        const wrapper = mount(DescriptionPopover, { propsData: { hover: false } });
        
        const descriptionIcon = wrapper.find('.button');
        expect(descriptionIcon.exists()).toBeTruthy();
        expect(descriptionIcon.isVisible()).toBeFalsy();
        
        wrapper.setProps({ hover: true }); // set hover to true
        await wrapper.vm.$nextTick(); // wait until pending promises are resolved
        expect(descriptionIcon.isVisible()).toBeTruthy();
    });

    it('expands and closes on mouseup', async () => {
        const wrapper = mount(DescriptionPopover);
        
        expect(toggleSpy).toHaveBeenCalledTimes(0);
        expect(wrapper.find('.content').exists()).toBeFalsy();

        // opens content
        wrapper.find('.button').trigger('mouseup');
        expect(toggleSpy).toHaveBeenCalledTimes(1);
        await wrapper.vm.$nextTick(); // wait until pending promises are resolved
        expect(wrapper.find('.content').exists()).toBeTruthy();

        // closes content
        wrapper.find('.button').trigger('mouseup');
        expect(toggleSpy).toHaveBeenCalledTimes(2);
        await wrapper.vm.$nextTick(); // wait until pending promises are resolved
        expect(wrapper.find('.content').exists()).toBeFalsy();
    });

    it('expands and closes via space', async () => {
        const wrapper = mount(DescriptionPopover);
        
        expect(toggleSpy).toHaveBeenCalledTimes(0);
        expect(wrapper.find('.content').exists()).toBeFalsy();

        // opens content
        wrapper.find('.button').trigger('keydown.space');
        expect(toggleSpy).toHaveBeenCalledTimes(1);
        await wrapper.vm.$nextTick(); // wait until pending promises are resolved
        expect(wrapper.find('.content').exists()).toBeTruthy();

        // closes content
        wrapper.find('.button').trigger('keydown.space');
        expect(toggleSpy).toHaveBeenCalledTimes(2);
        await wrapper.vm.$nextTick(); // wait until pending promises are resolved
        expect(wrapper.find('.content').exists()).toBeFalsy();
    });

    it('closes via escape', async () => {
        const wrapper = mount(DescriptionPopover);
        
        expect(toggleSpy).toHaveBeenCalledTimes(0);
        expect(wrapper.find('.content').exists()).toBeFalsy();

        // opens content
        wrapper.find('.button').trigger('keydown.space');
        expect(closeSpy).toHaveBeenCalledTimes(0);
        await wrapper.vm.$nextTick(); // wait until pending promises are resolved
        expect(wrapper.find('.content').exists()).toBeTruthy();

        // closes content
        wrapper.find('.button').trigger('keydown.esc');
        expect(closeSpy).toHaveBeenCalledTimes(1);
        await wrapper.vm.$nextTick(); // wait until pending promises are resolved
        expect(wrapper.find('.content').exists()).toBeFalsy();
    });

    it('closeUnlessHover closes only when not hovering', async () => {
        const wrapper = mount(DescriptionPopover, { propsData: { hover: true } });
        
        expect(toggleSpy).toHaveBeenCalledTimes(0);
        expect(wrapper.find('.content').exists()).toBeFalsy();

        // opens content
        wrapper.find('.button').trigger('keydown.space');
        await wrapper.vm.$nextTick(); // wait until pending promises are resolved
        expect(wrapper.find('.content').exists()).toBeTruthy();

        wrapper.vm.closeUnlessHover();

        // closes content
        expect(wrapper.find('.content').exists()).toBeTruthy();

        wrapper.setProps({ hover: false });
        await wrapper.vm.$nextTick(); // wait until pending promises are resolved
        wrapper.vm.closeUnlessHover();

        // closes content
        await wrapper.vm.$nextTick(); // wait until pending promises are resolved
        expect(wrapper.find('.content').exists()).toBeFalsy();
    });

    it('sets orientation to below if out of bounds', async () => {
        const getBoundingClientRectSpy = jest.spyOn(HTMLElement.prototype, 'getBoundingClientRect');
        getBoundingClientRectSpy.mockReturnValue({ top: -1 });
        const updateOrientationSpy = jest.spyOn(DescriptionPopover.methods, 'updateOrientation');
            
        const wrapper = mount(DescriptionPopover);
        wrapper.setData({ expanded: true });
        await wrapper.vm.$nextTick();
        expect(updateOrientationSpy).toHaveBeenCalledTimes(1);
        await wrapper.vm.$nextTick();
        expect(wrapper.vm.orientation).toBe('below');

        getBoundingClientRectSpy.mockReturnValue({ top: 1 });
        await wrapper.vm.updateOrientation();
        expect(wrapper.vm.orientation).toBe('above');

        getBoundingClientRectSpy.mockRestore();
    });
});
