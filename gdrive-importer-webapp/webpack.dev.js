const path = require('path');
const merge = require('webpack-merge');
const webpackCommonConfig = require('./webpack.common.js');

// the display name of the war
const app = 'gdrive-importer-webapp';

// add the server path to your server location path
const exoServerPath = "/exo-server";

let config = merge(webpackCommonConfig, {
  output: {
    path: path.resolve(`${exoServerPath}/webapps/${app}/`),
    filename: 'js/[name].bundle.js'
  },
  devtool: 'inline-source-map'
});

module.exports = config;
