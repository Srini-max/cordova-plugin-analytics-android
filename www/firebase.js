var exec = require('cordova/exec');

var PLUGIN_NAME = 'FirebasePlugin';
//
// Analytics
//
exports.logEvent = function (name, params, success, error) {
  exec(success, error, PLUGIN_NAME, "logEvent", [name, params]);
};

exports.setScreenName = function (name, success, error) {
  exec(success, error, PLUGIN_NAME, "setScreenName", [name]);
};

exports.setUserId = function (id, success, error) {
  exec(success, error, PLUGIN_NAME, "setUserId", [id]);
};

exports.setUserProperty = function (name, value, success, error) {
  exec(success, error, PLUGIN_NAME, "setUserProperty", [name, value]);
};

exports.setAnalyticsCollectionEnabled = function (enabled, success, error) {
  exec(success, error, PLUGIN_NAME, "setAnalyticsCollectionEnabled", [enabled]);
};

//
// Crashlytics
//
exports.logError = function (message, success, error) {
  exec(success, error, PLUGIN_NAME, "logError", [message]);
};

exports.forceCrashlytics = function (message, success, error) {
  exec(success, error, PLUGIN_NAME, "forceCrashlytics", [message]);
};

exports.setCrashlyticsUserId = function (userId, success, error) {
  exec(success, error, PLUGIN_NAME, "setCrashlyticsUserId", [userId]);
};

//
// Performance
//
exports.startTrace = function (name, success, error) {
  exec(success, error, PLUGIN_NAME, "startTrace", [name]);
};

exports.incrementCounter = function (name, counterNamed, success, error) {
  exec(success, error, PLUGIN_NAME, "incrementCounter", [name, counterNamed]);
};

exports.stopTrace = function (name, success, error) {
  exec(success, error, PLUGIN_NAME, "stopTrace", [name]);
};

exports.addTraceAttribute = function (traceName, attribute, value, success, error) {
  exec(success, error, PLUGIN_NAME, "addTraceAttribute", [traceName, attribute, value]);
};

exports.setPerformanceCollectionEnabled = function (enabled, success, error) {
  exec(success, error, PLUGIN_NAME, "setPerformanceCollectionEnabled", [enabled]);
};
