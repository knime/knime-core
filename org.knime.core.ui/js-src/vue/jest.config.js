module.exports = {
    moduleFileExtensions: [
        'js',
        'jsx',
        'json',
        'vue'
    ],
    transform: {
        '\\.js$': 'babel-jest',
        '\\.vue$': 'vue-jest',
        '\\.(css|styl|less|sass|scss|ttf|woff|woff2)(\\?|$)': 'jest-transform-stub',
        '\\.svg': '<rootDir>/test/unit/jest-transform-svgs',
        '\\.(jpg|webp)': '<rootDir>/test/unit/jest-file-loader'
    },
    transformIgnorePatterns: [
        '/node_modules/(?!(@knime/knime-ui-table/util|@knime/ui-extension-service))'
    ],
    moduleNameMapper: {
        '\\.(jpg|png)\\?(jpg|webp)': '<rootDir>/test/unit/assets/stub.$2',
        '^@/(.*\\.svg)\\?inline$': '<rootDir>/src/$1',
        '^~/(.*\\.svg)\\?inline$': '<rootDir>/$1',
        '^(.*\\.svg)\\?inline$': '$1',
        '\\.svg\\?data$': '<rootDir>/test/unit/assets/stub.data',
        '^vue$': 'vue/dist/vue.common.js',
        '^@/(.*)$': '<rootDir>/src/$1',
        '^~/(.*)$': '<rootDir>/$1'
    },
    reporters: ['default', ['jest-junit', { outputDirectory: './coverage' }]],
    coverageReporters: ['lcov', 'text'],
    // keep in sync with sonar-project.properties!
    collectCoverageFrom: [
        '<rootDir>/**/*.{js,vue}',
        '!config.js',
        '!**/*.config.js',
        '!.eslintrc*.js',
        '!**/.eslintrc*.js',
        '!.stylelintrc.js',
        '!<rootDir>/test/unit/test-util'
    ],
    coveragePathIgnorePatterns: [
        '^<rootDir>/(coverage|dist|test|target|node_modules|bin|webapps-common|src/dev)/',
        '^<rootDir>/src/(main.js|dev.js)'
    ],
    watchPathIgnorePatterns: [
        '^<rootDir>/(coverage|dist|target|node_modules|bin|webapps-common)/'
    ],
    testURL: 'http://test.example/',
    testMatch: [
        '<rootDir>/test/unit/suites/**/*.test.js'
    ],
    watchPlugins: [
        'jest-watch-typeahead/filename',
        'jest-watch-typeahead/testname'
    ],
    setupFiles: ['<rootDir>/test/unit/jest-setup']
};
