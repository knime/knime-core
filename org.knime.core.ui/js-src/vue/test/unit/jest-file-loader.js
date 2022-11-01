const path = require('path');

module.exports = {
    process(content, filename) {
        let basename = path.basename(filename);
        return {
            code: `module.exports = "${basename}";`
        };
    }
};
