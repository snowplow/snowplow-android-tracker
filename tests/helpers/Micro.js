
const request = require('request');

// URL of the Snowplow Micro to be interacted with
//export const COLLECTOR_ENDPOINT = '<%= subdomain %>' + '.ngrok.io';
export const COLLECTOR_ENDPOINT = '192.168.0.1';

// endpoints to interact with Snowplow Micro
const MICRO_GOOD  = "/micro/good";
const MICRO_BAD   = "/micro/bad";
const MICRO_ALL   = "/micro/all";
const MICRO_RESET = "/micro/reset";

export const uris = {
    APPLICATION_INSTALL_EVENT: "iglu:com.snowplowanalytics.mobile/application_install/jsonschema/1-0-0",
    SCREEN_VIEW_EVENT: "iglu:com.snowplowanalytics.mobile/screen_view/jsonschema/1-0-0",
    SCREEN_CONTEXT: "iglu:com.snowplowanalytics.mobile/screen/jsonschema/1-0-0",
    APPLICATION_CONTEXT: "iglu:com.snowplowanalytics.mobile/application/jsonschema/1-0-0"
};

export function resetMicro() {
    request.get(COLLECTOR_ENDPOINT + MICRO_RESET);
}

// schema is a string for Iglu URI
// contexts is an array of strings for Iglu URIs
export function getValidEvents(schema, contexts, custom) {
    let options = {
        json: {
            schema: schema,
            contexts: contexts
        }
    };
    if (custom) {
        options = custom;
    }
    return new Promise((resolve, reject) => {
        request.post(COLLECTOR_ENDPOINT + MICRO_GOOD, options, function (error, response, body) {
            if (error) reject(error);
            if (response.statusCode !== 200) {
                reject('Invalid status code <' + response.statusCode + '>');
            }
            const json = JSON.parse(body);
            if (json.hasOwnProperty('count') && json.count === 0) {
                reject('No valid matching events');
            }
            resolve(body['events']);
        });
    });
}

export function getCount() {
    return new Promise((resolve, reject) => {
        request.get(COLLECTOR_ENDPOINT + MICRO_ALL, function (error, response, body) {
            if (error) reject(error);
            if (response.statusCode !== 200) {
                reject('Invalid status code <' + response.statusCode + '>');
            }
            const json = JSON.parse(body);
            if (!json.hasOwnProperty('good')) {
                reject('Unexpected response from Micro: missing good count');
            } else if (!json.hasOwnProperty('bad')) {
                reject('Unexpected response from Micro: missing bad count');
            } else if (!json.hasOwnProperty('total')) {
                reject('Unexpected response from Micro: missing total count');
            } else {
                resolve(json);
            }
        });
    });
}