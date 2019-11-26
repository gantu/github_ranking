package kg.challange.ranking.config

case class AppConfig(
    port: Int = 8080,
    host: String = "0.0.0.0",
    githubApiUrl: String = "https://api.github.com"
)