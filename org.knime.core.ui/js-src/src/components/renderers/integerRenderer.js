import { rankWith, isIntegerControl } from '@jsonforms/core';
import IntegerInput from '@/components/UIComponents/IntegerInput.vue';
import { priorityRanks, inputFormats } from '@/constants';

export const integerTester = (uischema, schema) => {
    const isInteger = isIntegerControl(uischema, schema);
    return isInteger && uischema.options?.format === inputFormats.integer;
};

export const integerRenderer = {
    renderer: IntegerInput,
    tester: rankWith(priorityRanks.default, integerTester)
};
