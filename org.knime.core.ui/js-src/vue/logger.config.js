// Development app logger config. Not included in production build.

const { _types } = require('consola');
let level = process.env.VUE_APP_LOG_LEVEL || 'info';

module.exports = {
    level: _types[level].level,

    // browser only
    logToConsole: String(process.env.VUE_APP_LOG_TO_CONSOLE) !== 'false'
};
