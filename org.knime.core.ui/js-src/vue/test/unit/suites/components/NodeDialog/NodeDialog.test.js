import Vue from 'vue';
import Vuex from 'vuex';
import { createLocalVue, shallowMount } from '@vue/test-utils';
import { JsonForms } from '@jsonforms/vue2';
import { JsonDataService } from '@knime/ui-extension-service';
import { dialogInitialData } from '~/test/unit/mocks/dialogInitialData';

import NodeDialog from '@/components/NodeDialog.vue';

window.closeCEFWindow = () => {};

describe('NodeDialog.vue', () => {
    let localVue;

    const getOptions = ({ setApplySettingsMock, dirtySettingsMock, cleanSettingsMock } = {}) => ({
        provide: {
            getKnimeService: () => ({
                extensionConfig: {},
                callService: jest.fn().mockResolvedValue({}),
                registerDataGetter: jest.fn(),
                addNotificationCallback: jest.fn()
            })
        },
        propsData: {
            dialogSettings: {
                nodeId: 'test'
            }
        },
        mocks: {
            $store: new Vuex.Store({
                modules: {
                    'pagebuilder/dialog': {
                        actions: {
                            setApplySettings: setApplySettingsMock || jest.fn(),
                            dirtySettings: dirtySettingsMock || jest.fn(),
                            cleanSettings: cleanSettingsMock || jest.fn()
                        },
                        namespaced: true
                    }
                }
            })
        }
    });

    beforeAll(() => {
        localVue = createLocalVue();
        localVue.use(Vuex);
    });

    beforeEach(() => {
        jest.clearAllMocks();
        jest.spyOn(JsonDataService.prototype, 'initialData').mockResolvedValue({ ...dialogInitialData });
        jest.spyOn(JsonDataService.prototype, 'applyData').mockResolvedValue();
        jest.spyOn(JsonDataService.prototype, 'publishData').mockResolvedValue();
    });

    it('renders empty wrapper', async () => {
        const setApplySettingsMock = jest.fn();
        const wrapper = await shallowMount(NodeDialog, getOptions({ setApplySettingsMock }));
        await Vue.nextTick();
        await Vue.nextTick();

        expect(wrapper.getComponent(NodeDialog).exists()).toBe(true);
        expect(setApplySettingsMock).toHaveBeenCalled();
    });

    it('passes props to jsonform', async () => {
        const wrapper = await shallowMount(NodeDialog, getOptions());
        await Vue.nextTick();
        await Vue.nextTick(); // needed twice

        const jsonformsStub = wrapper.getComponent(JsonForms);

        expect(jsonformsStub.props('data')).toStrictEqual(dialogInitialData.data);
        expect(jsonformsStub.props('schema')).toStrictEqual(dialogInitialData.schema);
        expect(jsonformsStub.props('uischema')).toStrictEqual(dialogInitialData.ui_schema);
    });

    it('returns current values on getData', async () => {
        const wrapper = await shallowMount(NodeDialog, getOptions());
        await Vue.nextTick();
        await Vue.nextTick(); // needed twice

        expect(wrapper.vm.getData()).toStrictEqual(dialogInitialData.data);
    });

    describe('onSettingsChanged', () => {
        let wrapper, onSettingsChangedSpy, publishDataSpy, jsonformsStub, dirtySettingsMock, cleanSettingsMock;

        beforeEach(async () => {
            dirtySettingsMock = jest.fn();
            cleanSettingsMock = jest.fn();
            wrapper = await shallowMount(NodeDialog, getOptions({ dirtySettingsMock, cleanSettingsMock }));
            onSettingsChangedSpy = jest.spyOn(wrapper.vm, 'onSettingsChanged');
            publishDataSpy = jest.spyOn(wrapper.vm.jsonDataService, 'publishData');

            await Vue.nextTick();
            await Vue.nextTick(); // needed twice

            jsonformsStub = wrapper.getComponent(JsonForms);
        });

        it('sets new values', () => {
            jsonformsStub.vm.$emit('change', {
                data: { ...dialogInitialData.data, yAxisScale: 'NEW_VALUE' }
            });

            expect(onSettingsChangedSpy).toHaveBeenCalledWith({
                data: { ...dialogInitialData.data, yAxisScale: 'NEW_VALUE' }
            });

            const expectedData = {
                ...dialogInitialData.data,
                yAxisScale: 'NEW_VALUE'
            };

            expect(wrapper.vm.settings.data).toStrictEqual(expectedData);
            expect(publishDataSpy).toHaveBeenCalledWith({ ...dialogInitialData, data: expectedData });
            expect(dirtySettingsMock).toHaveBeenCalledTimes(1);
        });

        it('cleans settings if new data match original data', () => {
            const payload = { data: dialogInitialData.data };
            jsonformsStub.vm.$emit('change', payload);

            expect(onSettingsChangedSpy).toHaveBeenCalledWith(payload);

            expect(wrapper.vm.settings.data).toStrictEqual(dialogInitialData.data);
            expect(wrapper.vm.originalSettingsData).toStrictEqual(JSON.stringify(dialogInitialData.data));
            expect(publishDataSpy).toHaveBeenCalledWith(wrapper.vm.settings);
            expect(cleanSettingsMock).toHaveBeenCalledTimes(1);
            expect(dirtySettingsMock).toHaveBeenCalledTimes(0);
        });

        it('does not set new value if data is not provided', () => {
            jsonformsStub.vm.$emit('change', {});

            expect(wrapper.vm.settings.data).toStrictEqual({
                ...dialogInitialData.data
            });
            expect(publishDataSpy).not.toHaveBeenCalled();
            expect(dirtySettingsMock).toHaveBeenCalledTimes(0);
        });
    });

    describe('applySettings', () => {
        it('calls apply data and closes window', async () => {
            const wrapper = await shallowMount(NodeDialog, getOptions());
            const closeDialogSpy = jest.spyOn(wrapper.vm, 'closeDialog');
            const applyDataSpy = jest.spyOn(wrapper.vm.jsonDataService, 'applyData');
            // Needed twice to make sure that the async mounted method is resolved first
            await Vue.nextTick();
            await Vue.nextTick();

            await wrapper.vm.applySettingsCloseDialog();

            expect(applyDataSpy).toHaveBeenCalled();
            expect(closeDialogSpy).toHaveBeenCalled();
        });

        it('logs error that apply data been thrown', async () => {
            jest.spyOn(JsonDataService.prototype, 'applyData').mockRejectedValue(new Error());
            const wrapper = await shallowMount(NodeDialog, getOptions());
            await Vue.nextTick();

            expect(wrapper.vm.applySettingsCloseDialog()).rejects.toThrowError();
        });
    });

    it('calls window.closeCEFWindow in closeDialog', () => {
        const wrapper = shallowMount(NodeDialog, getOptions());
        const spy = jest.spyOn(window, 'closeCEFWindow');

        wrapper.vm.closeDialog();

        expect(spy).toHaveBeenCalledWith();
    });
});
