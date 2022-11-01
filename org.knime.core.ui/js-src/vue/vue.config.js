const path = require('path');
const svgConfig = require('webapps-common/webpack/webpack.svg.config');

module.exports = {
    css: {
        extract: false
    },
    chainWebpack: config => {
        // allow tilde imports
        config.resolve.alias.set('~', path.resolve(__dirname));

        config.resolve.alias.set('webapps-common', path.resolve(__dirname, 'webapps-common'));

        // apply SVG loader config
        config.module.rule('svg').uses.clear();
        config.merge({ module: { rule: { svg: svgConfig } } });
    }
};
