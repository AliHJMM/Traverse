// Only needed on Windows -- karma-firefox-launcher can't find Firefox
// there without an explicit path. On Linux (e.g. the Jenkins CI container,
// which installs firefox-esr via apt) it auto-detects the binary correctly
// on its own, and this same hardcoded Windows path would break it.
if (process.platform === 'win32') {
  process.env.FIREFOX_BIN = process.env.FIREFOX_BIN || 'C:\\Program Files\\Mozilla Firefox\\firefox.exe';
}

module.exports = function (config) {
  config.set({
    basePath: '',
    frameworks: ['jasmine', '@angular-devkit/build-angular'],
    plugins: [
      require('karma-jasmine'),
      require('karma-firefox-launcher'),
      require('karma-coverage'),
      require('@angular-devkit/build-angular/plugins/karma'),
    ],
    client: {
      jasmine: {},
      clearContext: false,
    },
    reporters: ['progress', 'coverage'],
    coverageReporter: {
      dir: require('path').join(__dirname, 'coverage'),
      subdir: '.',
      reporters: [{ type: 'lcov' }, { type: 'text-summary' }],
    },
    port: 9876,
    colors: true,
    logLevel: config.LOG_INFO,
    autoWatch: false,
    browsers: ['FirefoxHeadless'],
    customLaunchers: {
      FirefoxHeadless: {
        base: 'Firefox',
        flags: ['-headless'],
      },
    },
    singleRun: true,
    restartOnFileChange: false,
  });
};
