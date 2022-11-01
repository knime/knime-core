export const createDefaultFilterConfig = (isMultiselect, possibleValues) => ({
    is: isMultiselect ? 'FilterMultiselect' : 'FilterInputField',
    ...isMultiselect && { possibleValues },
    value: isMultiselect ? [] : ''
});

export const arrayEquals = (a, b) => a.length === b.length && a.every((val, index) => val === b[index]);

export const isImage = contentType => contentType === 'img_path';
