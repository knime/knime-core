import { rankWith, isOneOfControl } from '@jsonforms/core';
import DropdownInput from '@/components/UIComponents/DropdownInput.vue';
import { priorityRanks, inputFormats } from '@/constants';

export const dropDownTester = (uischema, schema) => {
    const isOfControl = isOneOfControl(uischema, schema);
    return isOfControl && uischema.options?.format === inputFormats.oneOfDropdown;
};

export const dropdownRenderer = {
    renderer: DropdownInput,
    tester: rankWith(priorityRanks.default, dropDownTester)
};
