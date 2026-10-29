import XCTest

@MainActor
final class SleepifyUITests: XCTestCase {
    private func launch() -> XCUIApplication {
        let app = XCUIApplication()
        app.launchArguments += ["-AppleLanguages", "(it)", "-AppleLocale", "it_IT"]
        app.launch()
        return app
    }

    func testModeSwitchAndCopy() {
        let app = launch()
        XCTAssertTrue(app.buttons["reference_time"].waitForExistence(timeout: 10))
        XCTAssertEqual(app.buttons["reference_time"].value as? String, "07:00")
        app.swipeUp()
        XCTAssertTrue(app.staticTexts["21:45"].waitForExistence(timeout: 5))
        app.buttons["copy_6"].tap()
        XCTAssertEqual(app.buttons["copy_6"].label, "Orario 21:45 copiato")
        app.swipeDown()
        app.buttons["mode_wake"].tap()
        XCTAssertEqual(app.buttons["reference_time"].value as? String, "23:00")
        app.swipeUp()
        XCTAssertTrue(app.staticTexts["03:45"].waitForExistence(timeout: 5))
    }

    func testNativeTimePickerAndCycleNormalization() {
        let app = launch()
        app.buttons["reference_time"].tap()
        let wheel = app.pickerWheels.element(boundBy: 0)
        XCTAssertTrue(wheel.waitForExistence(timeout: 5))
        // The Italian wheel value includes a spoken suffix ("07 esatte").
        // Select the actual two-digit option, independently of that suffix.
        wheel.adjust(toPickerWheelValue: "08")
        app.buttons["Chiudi"].tap()
        XCTAssertEqual(app.buttons["reference_time"].value as? String, "08:00")
        app.swipeUp()
        app.buttons["min_cycles"].tap()
        app.buttons["5 cicli"].tap()
        app.buttons["max_cycles"].tap()
        app.buttons["4 cicli"].tap()
        XCTAssertEqual(app.buttons["max_cycles"].value as? String, "5")
        XCTAssertTrue(app.staticTexts["1 orario"].exists)
        XCTAssertTrue(app.staticTexts["00:15"].exists)
    }
}
