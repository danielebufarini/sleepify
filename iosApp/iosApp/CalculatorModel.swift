import SwiftUI
@preconcurrency import SharedApp

/// Swift owns presentation only; all inputs and calculations belong to the shared session.
@MainActor
final class CalculatorModel: ObservableObject {
    let session = IosSleepify.shared.createSession()
    @Published private(set) var state: CalculatorState

    init() { state = session.state.value }

    func observe() async {
        for await next in session.state {
            guard !Task.isCancelled else { return }
            // A synchronous action may already have superseded a queued flow emission.
            if next.isEqual(session.state.value) { state = next }
        }
    }

    func setMode(_ mode: SleepMode) { session.setMode(mode: mode); refresh() }
    func setLatency(_ minutes: Int) { session.setLatency(minutes: Int32(minutes)); refresh() }
    func setMinCycles(_ cycles: Int) { session.setMinCycles(cycles: Int32(cycles)); refresh() }
    func setMaxCycles(_ cycles: Int) { session.setMaxCycles(cycles: Int32(cycles)); refresh() }

    var referenceDate: Date {
        get {
            Calendar.current.date(from: DateComponents(year: 2001, month: 1, day: 1,
                hour: Int(state.settings.referenceMinutes) / 60, minute: Int(state.settings.referenceMinutes) % 60))!
        }
        set {
            let components = Calendar.current.dateComponents([.hour, .minute], from: newValue)
            session.setTime(hour: Int32(components.hour!), minute: Int32(components.minute!))
            refresh()
        }
    }

    private func refresh() { state = session.state.value }

    deinit {
        let owned = session
        if Thread.isMainThread { owned.close() }
        else { DispatchQueue.main.async { owned.close() } }
    }
}
