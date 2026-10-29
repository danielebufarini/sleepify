import AlarmKit
import SwiftUI

struct ScheduledWakeAlarm: Identifiable {
    let id: UUID
    let minutes: Int

    var timeText: String { String(format: "%02d:%02d", minutes / 60, minutes % 60) }
}

struct AlarmNotice: Identifiable {
    let id = UUID()
    let message: String
    var needsSettings = false
}

private struct WakeAlarmMetadata: AlarmMetadata {}

/// AlarmKit owns persistence and delivery, including when Sleepify isn't running.
@MainActor
final class SystemAlarms: ObservableObject {
    @Published private(set) var alarms: [ScheduledWakeAlarm] = []
    @Published private(set) var isScheduling = false
    @Published private(set) var notice: AlarmNotice?

    func observe() async {
        refresh()
        for await alarms in AlarmManager.shared.alarmUpdates {
            guard !Task.isCancelled else { return }
            update(alarms)
        }
    }

    func refresh() {
        guard AlarmManager.shared.authorizationState == .authorized else {
            alarms = []
            return
        }
        do { update(try AlarmManager.shared.alarms) }
        catch { /* Keep the last confirmed state until the next system update. */ }
    }

    func schedule(minutes: Int) async {
        guard !isScheduling else { return }
        precondition((0..<1440).contains(minutes))
        isScheduling = true
        defer { isScheduling = false }

        do {
            let manager = AlarmManager.shared
            let authorization = manager.authorizationState == .notDetermined
                ? try await manager.requestAuthorization() : manager.authorizationState
            guard authorization == .authorized else {
                notice = AlarmNotice(message: localized("alarm_permission_denied"), needsSettings: true)
                return
            }

            let existing = try manager.alarms
            let schedule = Alarm.Schedule.relative(.init(time: .init(hour: minutes / 60, minute: minutes % 60),
                repeats: .never))
            // Repeated taps on the same time keep the existing alarm.
            if existing.contains(where: { $0.schedule == schedule }) {
                update(existing)
            } else {
                let attributes = AlarmAttributes<WakeAlarmMetadata>(
                    presentation: AlarmPresentation(alert: .init(title: "alarm_title")),
                    metadata: WakeAlarmMetadata(),
                    tintColor: Color(red: 182 / 255, green: 168 / 255, blue: 1))
                let alarm = try await manager.schedule(id: UUID(),
                    configuration: .alarm(schedule: schedule, attributes: attributes))
                update((try? manager.alarms) ?? (existing + [alarm]))
            }
            let time = String(format: "%02d:%02d", minutes / 60, minutes % 60)
            notice = AlarmNotice(message: String(format: localized("alarm_scheduled"), time))
        } catch {
            notice = AlarmNotice(message: localized("alarm_failed"))
        }
    }

    func cancel(_ alarm: ScheduledWakeAlarm) {
        do {
            try AlarmManager.shared.cancel(id: alarm.id)
            alarms.removeAll { $0.id == alarm.id }
        } catch {
            notice = AlarmNotice(message: localized("alarm_cancel_failed"))
        }
    }

    private func update(_ incoming: [Alarm]) {
        alarms = incoming.compactMap { alarm in
            guard case let .relative(relative) = alarm.schedule else { return nil }
            return ScheduledWakeAlarm(id: alarm.id, minutes: relative.time.hour * 60 + relative.time.minute)
        }.sorted { $0.minutes < $1.minutes }
    }

    private func localized(_ key: String) -> String {
        String(localized: String.LocalizationValue(key))
    }
}
