require('consola');
jest.mock('@knime/ui-extension-service');
jest.mock('@jsonforms/vue2', () => {
    const original = jest.requireActual('@jsonforms/vue2');
    return {
        ...original,
        rendererProps: jest.fn().mockReturnValue({
            schema: {},
            uischema: {},
            path: '',
            control: null,
            layout: null,
            rendererPropsInitialized: true
        }),
        useJsonFormsControl: jest.fn(),
        useJsonFormsLayout: jest.fn(),
        useJsonFormsArrayControl: jest.fn()
    };
}, { virtual: false });
jest.mock('@jsonforms/core', () => {
    const original = jest.requireActual('@jsonforms/core');
    return {
        ...original,
        createDefaultValue: jest.fn(),
        composePaths: jest.fn()
    };
}, { virtual: false });
