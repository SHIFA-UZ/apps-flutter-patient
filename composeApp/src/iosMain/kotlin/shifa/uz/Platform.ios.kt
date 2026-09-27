package shifa.uz

import platform.UIKit.UIDevice

class IOSPlatform : Platform {
    override val name: String = UIDevice.currentDevice.systemName() as String
}

actual fun getPlatform(): Platform = IOSPlatform()
