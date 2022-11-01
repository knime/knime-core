import { rankWith,
    isOneOfControl,
    isAnyOfControl,
    isNumberControl,
    isBooleanControl,
    isIntegerControl,
    isStringControl } from '@jsonforms/core';
import { priorityRanks } from '~/src/constants';
import { numberRenderer } from './numberRenderer';
import { checkboxRenderer } from './checkboxRenderer';
import { dropdownRenderer } from './dropdownRenderer';
import { textRenderer } from './textRenderer';
import { twinlistRenderer } from './twinlistRenderer';
import { integerRenderer } from './integerRenderer';

export const fallbackRenderers = [
    { ...dropdownRenderer, tester: rankWith(priorityRanks.fallback, isOneOfControl) },
    { ...twinlistRenderer, tester: rankWith(priorityRanks.fallback, isAnyOfControl) },
    { ...numberRenderer, tester: rankWith(priorityRanks.fallback, isNumberControl) },
    { ...checkboxRenderer, tester: rankWith(priorityRanks.fallback, isBooleanControl) },
    { ...integerRenderer, tester: rankWith(priorityRanks.fallback, isIntegerControl) },
    { ...textRenderer, tester: rankWith(priorityRanks.fallback, isStringControl) }
];
