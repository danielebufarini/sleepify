import SwiftUI
import UIKit
@preconcurrency import SharedApp

private enum Palette {
    static let night = Color(hex: 0x101827)
    static let panel = Color(hex: 0x192437)
    static let raised = Color(hex: 0x222F45)
    static let lavender = Color(hex: 0xB6A8FF)
    static let muted = Color(hex: 0xACBAD0)
    static let outline = Color(hex: 0x35435A)
    static let text = Color(hex: 0xF3F6FC)
}

@MainActor
struct CalculatorView: View {
    @ObservedObject var model: CalculatorModel
    @State private var showingTime = false
    @State private var copiedCycles: Int32?
    @Environment(\.dynamicTypeSize) private var dynamicTypeSize
    @ScaledMetric(relativeTo: .largeTitle) private var clockSize: CGFloat = 44
    private var bedtime: Bool { model.state.settings.mode == .bedtime }

    var body: some View {
        ZStack {
            LinearGradient(colors: [Color(hex: 0x253052), Palette.night, Palette.night],
                startPoint: .top, endPoint: .bottom).ignoresSafeArea()
            ScrollView {
                VStack(spacing: 24) {
                    header
                    inputs
                    results
                    explanation
                }
                .frame(maxWidth: 560)
                .padding(.horizontal, 20)
                .padding(.vertical, 24)
                .frame(maxWidth: .infinity)
            }
        }
        .foregroundStyle(Palette.text)
        .tint(Palette.lavender)
        .task { await model.observe() }
        .onChange(of: model.state.settings) { _, _ in copiedCycles = nil }
        .sheet(isPresented: $showingTime) {
            NavigationStack {
                DatePicker(bedtime ? "wake_time" : "bed_time",
                    selection: Binding(get: { model.referenceDate }, set: { model.referenceDate = $0 }),
                    displayedComponents: .hourAndMinute)
                    .datePickerStyle(.wheel)
                    .labelsHidden()
                    .environment(\.locale, Locale(identifier:
                        Locale.current.language.languageCode?.identifier == "it" ? "it_IT" : "en_GB"))
                    .padding()
                    .navigationTitle(bedtime ? tr("wake_time") : tr("bed_time"))
                    .navigationBarTitleDisplayMode(.inline)
                    .toolbar {
                        ToolbarItem(placement: .confirmationAction) {
                            Button("close") { showingTime = false }
                        }
                    }
            }
            .presentationDetents([.medium])
        }
    }

    private var header: some View {
        VStack(spacing: 8) {
            Image(systemName: "moon.stars.fill")
                .font(.system(size: 42, weight: .light)).foregroundStyle(Palette.lavender)
                .padding(.bottom, 6).accessibilityHidden(true)
            Text("Sleepify").font(.largeTitle.bold()).tracking(-1)
            Text("calculator_title").font(.subheadline.weight(.medium))
            Text("subtitle").font(.subheadline).foregroundStyle(Palette.muted)
        }
        .multilineTextAlignment(.center)
    }

    private var inputs: some View {
        VStack(alignment: .leading, spacing: 18) {
            Text("mode_label").font(.subheadline.weight(.semibold))
            controlLayout {
                modeButton("bedtime", symbol: "bed.double", mode: .bedtime, identifier: "mode_bedtime")
                modeButton("wake_up", symbol: "alarm", mode: .wakeUp, identifier: "mode_wake")
            }
            VStack(alignment: .leading, spacing: 10) {
                Text(bedtime ? "wake_time" : "bed_time").font(.subheadline.weight(.semibold))
                    .foregroundStyle(Palette.muted)
                Button { showingTime = true } label: {
                    HStack(spacing: 16) {
                        Text(referenceTime).font(.system(size: clockSize, weight: .light, design: .rounded))
                            .monospacedDigit().tracking(2).lineLimit(1).minimumScaleFactor(0.4)
                        Image(systemName: "clock").font(.system(size: 20))
                    }
                    .frame(maxWidth: .infinity).padding(.vertical, 14)
                    .background(Palette.night, in: RoundedRectangle(cornerRadius: 16))
                    .overlay(RoundedRectangle(cornerRadius: 16).stroke(Palette.outline))
                }
                .buttonStyle(.plain)
                .accessibilityLabel(tr(bedtime ? "wake_time" : "bed_time"))
                .accessibilityValue(referenceTime)
                .accessibilityIdentifier("reference_time")
            }
            VStack(spacing: 8) {
                ViewThatFits(in: .horizontal) {
                    HStack {
                        Text("latency").font(.subheadline.weight(.semibold))
                        Spacer(minLength: 8)
                        latencyValue
                    }
                    VStack(alignment: .leading, spacing: 6) {
                        Text("latency").font(.subheadline.weight(.semibold))
                        latencyValue
                    }.frame(maxWidth: .infinity, alignment: .leading)
                }
                Slider(value: Binding(get: { Double(model.state.settings.latencyMinutes) },
                    set: { model.setLatency(Int(($0 / 5).rounded()) * 5) }), in: 0...45, step: 5)
                    .accessibilityLabel(tr("latency"))
                    .accessibilityValue(String(format: tr("minutes"), Int(model.state.settings.latencyMinutes)))
                    .accessibilityIdentifier("latency")
                HStack {
                    ForEach([0, 15, 30, 45], id: \.self) { value in
                        if value != 0 { Spacer() }
                        Text("\(value) min").font(.caption).foregroundStyle(Palette.muted)
                    }
                }.accessibilityHidden(true)
            }
            controlLayout {
                cyclePicker("minimum_cycles", value: model.state.settings.minCycles, range: 2...5,
                    identifier: "min_cycles", action: model.setMinCycles)
                cyclePicker("maximum_cycles", value: model.state.settings.maxCycles, range: 4...8,
                    identifier: "max_cycles", action: model.setMaxCycles)
            }
        }
        .padding(20)
        .background(Palette.panel, in: RoundedRectangle(cornerRadius: 24))
        .overlay(RoundedRectangle(cornerRadius: 24).stroke(Palette.outline))
    }

    private var latencyValue: some View {
        Text(String(format: tr("minutes"), Int(model.state.settings.latencyMinutes)))
            .font(.subheadline.weight(.semibold)).foregroundStyle(Palette.lavender)
            .accessibilityIdentifier("latency_value")
    }

    private func modeButton(_ key: String, symbol: String, mode: SleepMode, identifier: String) -> some View {
        let selected = model.state.settings.mode == mode
        return Button { model.setMode(mode) } label: {
            VStack(spacing: 6) {
                Image(systemName: symbol).font(.body)
                Text(tr(key)).font(.subheadline.weight(.semibold)).multilineTextAlignment(.center)
            }
            .frame(maxWidth: .infinity, minHeight: 44).padding(10)
            .foregroundStyle(selected ? Color(hex: 0x20183C) : Palette.muted)
            .background(selected ? Palette.lavender : Palette.raised, in: RoundedRectangle(cornerRadius: 14))
        }
        .buttonStyle(.plain)
        .accessibilityAddTraits(selected ? [.isSelected] : [])
        .accessibilityIdentifier(identifier)
    }

    private func cyclePicker(_ key: String, value: Int32, range: ClosedRange<Int>, identifier: String,
        action: @escaping (Int) -> Void) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(tr(key)).font(.caption.weight(.semibold)).foregroundStyle(Palette.muted)
            Menu {
                ForEach(Array(range), id: \.self) { count in
                    Button { action(count) } label: {
                        if count == value {
                            Label(String(format: tr("cycles"), count), systemImage: "checkmark")
                        } else {
                            Text(String(format: tr("cycles"), count))
                        }
                    }
                }
            } label: {
                HStack {
                    Text(String(format: tr("cycles"), Int(value)))
                    Spacer(minLength: 4)
                    Image(systemName: "chevron.down").font(.caption)
                }
                .font(.subheadline).padding(12)
                .background(Palette.night, in: RoundedRectangle(cornerRadius: 12))
                .overlay(RoundedRectangle(cornerRadius: 12).stroke(Palette.outline))
            }
            .accessibilityLabel(tr(key)).accessibilityValue(String(value))
            .accessibilityIdentifier(identifier)
        }.frame(maxWidth: .infinity, alignment: .leading)
    }

    private var results: some View {
        VStack(spacing: 10) {
            controlLayout {
                Text(bedtime ? "bedtime_results" : "wake_results").font(.headline)
                    .accessibilityAddTraits(.isHeader)
                if !dynamicTypeSize.isAccessibilitySize { Spacer(minLength: 8) }
                Text(model.state.results.count == 1 ? tr("count_one") :
                    String(format: tr("count_many"), model.state.results.count))
                    .font(.caption).foregroundStyle(Palette.muted)
            }.padding(.bottom, 2)
            ForEach(model.state.results, id: \.cycles) { result in
                HStack(spacing: 12) {
                    VStack(alignment: .leading, spacing: 4) {
                        Text(result.timeText).font(.title.weight(.semibold)).monospacedDigit()
                            .lineLimit(1).minimumScaleFactor(0.6)
                        Text(String(format: tr("result_detail"), Int(result.cycles), result.durationText))
                            .font(.caption).foregroundStyle(Palette.muted)
                        Text(tr(result.dayOffset < 0 ? "previous_day" : result.dayOffset > 0 ? "next_day" : "same_day"))
                            .font(.caption2.weight(.medium)).foregroundStyle(Palette.lavender)
                    }.frame(maxWidth: .infinity, alignment: .leading)
                    Button {
                        UIPasteboard.general.string = result.timeText
                        copiedCycles = result.cycles
                        UIAccessibility.post(notification: .announcement,
                            argument: String(format: tr("copied"), result.timeText))
                    } label: {
                        Image(systemName: copiedCycles == result.cycles ? "checkmark" : "doc.on.doc")
                            .font(.body).frame(width: 44, height: 44)
                    }
                    .buttonStyle(.plain).foregroundStyle(Palette.lavender)
                    .accessibilityLabel(String(format: tr(copiedCycles == result.cycles ? "copied" : "copy_time"), result.timeText))
                    .accessibilityIdentifier("copy_\(result.cycles)")
                }
                .padding(.leading, 18).padding(.trailing, 8).padding(.vertical, 14)
                .background(Palette.raised, in: RoundedRectangle(cornerRadius: 18))
                .accessibilityElement(children: .contain)
                .accessibilityIdentifier("result_\(result.cycles)")
            }
        }
    }

    private var explanation: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("estimate_title").font(.subheadline.weight(.semibold)).foregroundStyle(Palette.lavender)
            Text("estimate_note").font(.caption).foregroundStyle(Palette.muted)
            Rectangle().fill(Palette.outline).frame(height: 1).padding(.vertical, 4)
            Text("disclaimer").font(.caption).foregroundStyle(Palette.muted)
        }.frame(maxWidth: .infinity, alignment: .leading).padding(.horizontal, 4)
    }

    private var referenceTime: String {
        let minutes = Int(model.state.settings.referenceMinutes)
        return String(format: "%02d:%02d", minutes / 60, minutes % 60)
    }

    private var controlLayout: AnyLayout {
        dynamicTypeSize.isAccessibilitySize
            ? AnyLayout(VStackLayout(alignment: .leading, spacing: 12))
            : AnyLayout(HStackLayout(alignment: .top, spacing: 12))
    }
}

private func tr(_ key: String) -> String { String(localized: String.LocalizationValue(key)) }

private extension Color {
    init(hex: UInt32) {
        self.init(.sRGB, red: Double((hex >> 16) & 255) / 255,
            green: Double((hex >> 8) & 255) / 255, blue: Double(hex & 255) / 255, opacity: 1)
    }
}
