import { rankWith, isOneOfControl } from '@jsonforms/core';
import { priorityRanks, inputFormats } from '@/constants';
import RadioInput from '@/components/UIComponents/RadioInput.vue';

export const radioTester = (uischema, schema) => {
    const isOneOf = isOneOfControl(uischema, schema);
    return isOneOf && uischema.options?.format === inputFormats.oneOfRadio;
};

export const radioRenderer = {
    renderer: RadioInput,
    tester: rankWith(priorityRanks.default, radioTester)
};
