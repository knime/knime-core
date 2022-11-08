/* The methods exported by this test utility file can be used for testing components that are supposed to be used
 * within a JSONForms context.
 * A component can be mounted using composition API and the correct initialization of JSONForms can be verified on a
 * given vue test utils wrapper. */
import { createLocalVue, mount } from '@vue/test-utils';
import CompositionApi from '@vue/composition-api';
import Vuex from 'vuex';

const jsonFormsMock = require('@jsonforms/vue2');

export const mountJsonFormsComponentWithStore = (component, propsData, modules) => {
    const localVue = createLocalVue();
    localVue.use(Vuex);
    localVue.use(CompositionApi);
    return mount(component, {
        localVue,
        propsData,
        stubs: {
            DispatchRenderer: true
        },
        mocks: {
            $store: new Vuex.Store({ modules })
        }
    });
};

// eslint-disable-next-line arrow-body-style
export const mountJsonFormsComponent = (component, propsData) => {
    return mountJsonFormsComponentWithStore(component, propsData, null);
};

const hasBasicProps = (props) => {
    expect(props.hasOwnProperty('rendererPropsInitialized')).toBe(true);
    expect(props.hasOwnProperty('schema')).toBe(true);
    expect(props.hasOwnProperty('uischema')).toBe(true);
    expect(props.hasOwnProperty('path')).toBe(true);
};

export const initializesJsonFormsControl = (wrapper) => {
    const props = wrapper.props();
    hasBasicProps(props);
    expect(props.hasOwnProperty('control')).toBe(true);
    expect(props.control.schema).toBeDefined();
    expect(props.control.uischema).toBeDefined();
    expect(jsonFormsMock.useJsonFormsControl).toHaveBeenCalled();
};

export const initializesJsonFormsLayout = (wrapper) => {
    const props = wrapper.props();
    hasBasicProps(props);
    expect(props.hasOwnProperty('layout')).toBe(true);
    expect(props.layout.schema).toBeDefined();
    expect(props.layout.uischema).toBeDefined();
    expect(jsonFormsMock.useJsonFormsLayout).toHaveBeenCalled();
};

export const initializesJsonFormsArrayControl = (wrapper) => {
    const props = wrapper.props();
    hasBasicProps(props);
    expect(props.hasOwnProperty('control')).toBe(true);
    expect(props.control.schema).toBeDefined();
    expect(props.control.uischema).toBeDefined();
    expect(jsonFormsMock.useJsonFormsArrayControl).toHaveBeenCalled();
};
