const merge = require('webpack-merge');
const webpackDevConfig = require('./webpack.dev.js');
module.exports = merge(webpackDevConfig);