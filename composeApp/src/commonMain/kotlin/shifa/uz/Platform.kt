package shifa.uz

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
