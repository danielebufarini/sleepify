import XCTest

@MainActor
final class SleepifyUITests: XCTestCase {
    private func launch() -> XCUIApplication {
        let app = XCUIApplication()
        app.launchArguments += ["-AppleLanguages", "(it)", "-AppleLocale", "it_IT"]
        app.launch()
        return app
    }

    func testHomeHasNoAppNameInNavigationBar() {
        let app = launch()
        XCTAssertTrue(app.buttons["reference_time"].waitForExistence(timeout: 10))
        capture("Home navigation")
        XCTAssertFalse(app.navigationBars.staticTexts["Sleepify"].exists)
        XCTAssertTrue(app.buttons["app_menu"].isHittable)
    }

    func testResultsNavigationStaysAccessibleWhileScrolling() {
        let app = launch()
        scrollTo(app.buttons["max_cycles"], in: app)
        app.buttons["max_cycles"].tap()
        app.buttons["8 cicli"].tap()
        openResults(app)
        let navigationBar = app.navigationBars["Orari consigliati"]
        let initialBarFrame = navigationBar.frame
        let firstResult = app.staticTexts["18:45"]
        XCTAssertTrue(firstResult.exists)
        let initialResultY = firstResult.frame.minY
        capture("Results before scrolling")
        app.scrollViews.firstMatch.swipeUp()
        XCTAssertLessThan(firstResult.frame.minY, initialResultY)
        XCTAssertEqual(navigationBar.frame, initialBarFrame)
        XCTAssertTrue(app.buttons["app_menu"].isHittable)
        XCTAssertTrue(navigationBar.buttons.element(boundBy: 0).isHittable)
        capture("Results beneath navigation")
        assertAbout(app)
        backToInputs(app)
        XCTAssertEqual(app.buttons["max_cycles"].value as? String, "8")
    }

    func testSeparateResultsScreenAndBackPreserveInputs() {
        let app = launch()
        XCTAssertTrue(app.buttons["reference_time"].waitForExistence(timeout: 10))
        XCTAssertEqual(app.buttons["reference_time"].value as? String, "07:00")
        XCTAssertFalse(app.staticTexts["21:45"].exists)
        capture("Calculator")
        openResults(app)
        XCTAssertFalse(app.buttons["reference_time"].exists)
        XCTAssertTrue(app.staticTexts["21:45"].waitForExistence(timeout: 5))
        for cycle in 3...6 { XCTAssertFalse(app.buttons["copy_\(cycle)"].exists) }
        capture("Bedtime results")
        backToInputs(app)
        XCTAssertEqual(app.buttons["reference_time"].value as? String, "07:00")
        app.buttons["mode_wake"].tap()
        XCTAssertEqual(app.buttons["reference_time"].value as? String, "23:00")
        openResults(app)
        XCTAssertTrue(app.staticTexts["03:45"].waitForExistence(timeout: 5))
        for cycle in 3...6 { XCTAssertFalse(app.buttons["copy_\(cycle)"].exists) }
        capture("Wake results")
        backToInputs(app)
        XCTAssertEqual(app.buttons["reference_time"].value as? String, "23:00")
    }

    func testAboutFromBothScreensPreservesCalculation() {
        let app = launch()
        assertAbout(app)
        XCTAssertEqual(app.buttons["reference_time"].value as? String, "07:00")
        app.buttons["mode_wake"].tap()
        openResults(app)
        assertAbout(app)
        XCTAssertTrue(app.staticTexts["03:45"].exists)
        backToInputs(app)
        XCTAssertEqual(app.buttons["reference_time"].value as? String, "23:00")
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
        openResults(app)
        XCTAssertTrue(app.staticTexts["1 orario"].exists)
        XCTAssertTrue(app.staticTexts["00:15"].exists)
    }

    func testSystemAlarmSchedulingPersistenceAndCancellation() {
        let app = launch()
        setWakeTime(app, hour: "08", minute: "10")
        app.buttons["set_reference_alarm"].tap()
        let springboard = XCUIApplication(bundleIdentifier: "com.apple.springboard")
        let allow = springboard.alerts.buttons.matching(NSPredicate(format: "label IN %@", ["Consenti", "Allow"]))
            .firstMatch
        if allow.waitForExistence(timeout: 3) { allow.tap() }
        assertScheduled(app, time: "08:10")
        scrollTo(app.otherElements["scheduled_alarm_490"], in: app)
        XCTAssertTrue(app.otherElements["scheduled_alarm_490"].exists)

        app.terminate()
        app.launch()
        scrollTo(app.otherElements["scheduled_alarm_490"], in: app)
        XCTAssertTrue(app.otherElements["scheduled_alarm_490"].exists, "AlarmKit restores alarms after relaunch")
        app.swipeDown()
        app.swipeDown()
        setWakeTime(app, hour: "08", minute: "10")
        app.buttons["set_reference_alarm"].tap()
        assertScheduled(app, time: "08:10")
        scrollTo(app.buttons["cancel_alarm_490"], in: app)
        XCTAssertEqual(app.buttons.matching(identifier: "cancel_alarm_490").count, 1)
        app.buttons["cancel_alarm_490"].tap()
        XCTAssertFalse(app.otherElements["scheduled_alarm_490"].exists)

        app.swipeDown()
        app.swipeDown()
        app.buttons["mode_wake"].tap()
        XCTAssertFalse(app.buttons["set_reference_alarm"].exists)
        openResults(app)
        scrollTo(app.buttons["alarm_3"], in: app)
        app.buttons["alarm_3"].tap()
        assertScheduled(app, time: "03:45")
        scrollTo(app.buttons["cancel_alarm_225"], in: app)
        app.buttons["cancel_alarm_225"].tap()
        XCTAssertFalse(app.otherElements["scheduled_alarm_225"].exists)
    }

    private func openResults(_ app: XCUIApplication) {
        scrollTo(app.buttons["show_results"], in: app)
        app.buttons["show_results"].tap()
        XCTAssertTrue(app.navigationBars["Orari consigliati"].waitForExistence(timeout: 5))
    }

    private func backToInputs(_ app: XCUIApplication) {
        app.navigationBars.buttons.element(boundBy: 0).tap()
        XCTAssertTrue(app.buttons["reference_time"].waitForExistence(timeout: 5))
    }

    private func assertAbout(_ app: XCUIApplication) {
        app.buttons["app_menu"].tap()
        app.buttons["menu_about"].tap()
        XCTAssertTrue(app.staticTexts["about_version"].waitForExistence(timeout: 5))
        XCTAssertEqual(app.staticTexts["about_version"].label, "Versione 1.0 (1)")
        capture("About")
        app.buttons["close_about"].tap()
        XCTAssertFalse(app.staticTexts["about_version"].exists)
    }

    private func capture(_ name: String) {
        let attachment = XCTAttachment(screenshot: XCUIScreen.main.screenshot())
        attachment.name = name
        attachment.lifetime = .keepAlways
        add(attachment)
    }

    private func setWakeTime(_ app: XCUIApplication, hour: String, minute: String) {
        app.buttons["reference_time"].tap()
        XCTAssertTrue(app.pickerWheels.element(boundBy: 0).waitForExistence(timeout: 5))
        app.pickerWheels.element(boundBy: 0).adjust(toPickerWheelValue: hour)
        app.pickerWheels.element(boundBy: 1).adjust(toPickerWheelValue: minute)
        app.buttons["Chiudi"].tap()
    }

    private func assertScheduled(_ app: XCUIApplication, time: String) {
        let alert = app.alerts["Sveglia di sistema"]
        XCTAssertTrue(alert.waitForExistence(timeout: 10))
        XCTAssertTrue(alert.staticTexts["Sveglia impostata alle \(time), alla prossima occorrenza dell’orario."].exists,
            app.debugDescription)
        alert.buttons["Chiudi"].tap()
    }

    private func scrollTo(_ element: XCUIElement, in app: XCUIApplication) {
        for _ in 0..<6 {
            if element.exists && element.isHittable { return }
            app.swipeUp()
        }
        XCTAssertTrue(element.exists && element.isHittable)
    }
}
