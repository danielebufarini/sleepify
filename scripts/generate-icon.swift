import Foundation
import CoreGraphics
import ImageIO

// Reproducible geometric icon, rendered without external assets or fonts.
let size = 1024
let context = CGContext(data: nil, width: size, height: size, bitsPerComponent: 8,
    bytesPerRow: size * 4, space: CGColorSpaceCreateDeviceRGB(),
    bitmapInfo: CGImageAlphaInfo.noneSkipLast.rawValue)!
let background = CGColor(red: 16/255, green: 24/255, blue: 39/255, alpha: 1)
let lavender = CGColor(red: 195/255, green: 185/255, blue: 1, alpha: 1)
context.setFillColor(background)
context.fill(CGRect(x: 0, y: 0, width: size, height: size))
context.setFillColor(lavender)
context.fillEllipse(in: CGRect(x: 210, y: 230, width: 570, height: 570))
context.setFillColor(background)
context.fillEllipse(in: CGRect(x: 380, y: 360, width: 520, height: 520))
context.setFillColor(lavender)
context.move(to: CGPoint(x: 735, y: 740))
for point in [(759,675), (824,651), (759,627), (735,562), (711,627), (646,651), (711,675)] {
    context.addLine(to: CGPoint(x: point.0, y: point.1))
}
context.closePath()
context.fillPath()
let output = URL(fileURLWithPath: "iosApp/iosApp/Assets.xcassets/AppIcon.appiconset/AppIcon.png")
let destination = CGImageDestinationCreateWithURL(output as CFURL, "public.png" as CFString, 1, nil)!
CGImageDestinationAddImage(destination, context.makeImage()!, nil)
precondition(CGImageDestinationFinalize(destination))
