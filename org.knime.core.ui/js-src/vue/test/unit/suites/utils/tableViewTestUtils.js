import TableView from '@/components/TableView.vue';
import { mount } from '@vue/test-utils';

export const asyncMountTableView = async (context) => {
    let wrapper = await mount(TableView, context);
    await wrapper.vm.$nextTick();
    await wrapper.vm.$nextTick();
    const getImageUrlSpy = jest.spyOn(wrapper.vm, 'getImageUrl');
    await wrapper.vm.$nextTick();
    const updateDataSpy = jest.spyOn(wrapper.vm, 'updateData');
    const refreshTableSpy = jest.spyOn(wrapper.vm, 'refreshTable');
    const requestTableSpy = jest.spyOn(wrapper.vm, 'requestTable');
    const dataSpy = jest.spyOn(wrapper.vm.jsonDataService, 'data');
    await wrapper.vm.$nextTick();
    await wrapper.vm.$nextTick();
    return { wrapper, dataSpy, updateDataSpy, refreshTableSpy, requestTableSpy, getImageUrlSpy };
};

export const changeViewSetting = async (wrapper, settingsKey, settingsValue) => {
    const settings = JSON.parse(JSON.stringify(wrapper.vm.$data.settings));
    settings[settingsKey] = settingsValue;
    await wrapper.vm.onViewSettingsChange({
        data: { data: { view: settings } }
    });
};
