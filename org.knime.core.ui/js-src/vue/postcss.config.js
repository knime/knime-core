const { preset, plugins, order } = require('webapps-common/webpack/webpack.postcss.config');

module.exports = {

    plugins: Object.assign({}, plugins, {
        'postcss-preset-env': preset
    }),

    order
};
