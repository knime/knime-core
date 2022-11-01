import { rankWith, isOneOfControl } from '@jsonforms/core';
import { priorityRanks, inputFormats } from '@/constants';
import ValueSwitchInput from '@/components/UIComponents/ValueSwitchInput.vue';

export const valueSwitchTester = (uischema, schema) => {
    const isOneOf = isOneOfControl(uischema, schema);
    return isOneOf && uischema.options?.format === inputFormats.oneOfValueSwitch;
};

export const valueSwitchRenderer = {
    renderer: ValueSwitchInput,
    tester: rankWith(priorityRanks.default, valueSwitchTester)
};
