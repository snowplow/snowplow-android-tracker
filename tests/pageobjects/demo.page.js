export default class DemoPage {
    static waitForDemoViewShown () {
        $('~demoIdentifier').waitForDisplayed(5000);
    }
}