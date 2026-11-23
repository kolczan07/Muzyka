package pl.mdinstal.muzyka

enum class Screen { Home, YtMusic, Radio, Player, About, Theme }

data class Station(val name: String, val url: String, val logo: String = "")
