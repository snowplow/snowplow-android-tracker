import { COLLECTOR_ENDPOINT, uris, getCount, getValidEvents, resetMicro } from "../helpers/Micro";
import Gestures from "../helpers/Gestures";

describe('The Snowplow Demo app, when first loading,', () => {
    beforeEach(() => {
        resetMicro();
    });

    it('Sends standard events', async () => {
        // click the first demo screen button
        let selector = 'new UiSelector().resourceId(\"com.snowplowanalytics.snowplowtrackerdemo:id/btn_lite\")';
        let Button = $(`android=${selector}`);
        Button.click();
        // click the URI field
        selector = 'new UiSelector().resourceId(\"com.snowplowanalytics.snowplowtrackerdemo:id/emitter_uri_field\")';
        const URIField = $(`android=${selector}`);
        URIField.click();
        // enter text into field
        URIField.addValue('192.168.0.1');
        driver.hideKeyboard();
        // click HTTPS
        selector = 'new UiSelector().resourceId(\"com.snowplowanalytics.snowplowtrackerdemo:id/radio_https\")';
        Button = $(`android=${selector}`);
        Button.click();
        // click POST
        selector = 'new UiSelector().resourceId(\"com.snowplowanalytics.snowplowtrackerdemo:id/radio_post\")';
        Button = $(`android=${selector}`);
        Button.click();
        // click track
        selector = 'new UiSelector().resourceId(\"com.snowplowanalytics.snowplowtrackerdemo:id/btn_lite_start\")';
        Button = $(`android=${selector}`);
        Button.click();

        try {
            let count = await getCount();
            expect(count.good).toEqual(15);
            expect(count.bad).toEqual(0);
            expect(count.total).toEqual(15);
        } catch (error) {

        }
    });

    it('Sends screenviews events', async () => {
        Gestures.swipeLeft(1);
        try {
            let count = await getCount();
            expect(count.good).toEqual(1);
            expect(count.bad).toEqual(0);
            expect(count.total).toEqual(1);

            let events = await getValidEvents(uris.SCREEN_VIEW_EVENT);
            expect(events).toEqual(jasmine.any(Array));
            expect(events.length).toEqual(1);
        } catch (error) {

        }
    });
});
