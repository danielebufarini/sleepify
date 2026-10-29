import SwiftUI

@main
@MainActor
struct SleepifyApp: App {
    @StateObject private var model = CalculatorModel()

    var body: some Scene {
        WindowGroup {
            CalculatorView(model: model)
                .preferredColorScheme(.dark)
        }
    }
}
