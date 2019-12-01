package kg.challange.ranking.config

case class AppConfig(
    port: Int = 8080,
    host: String = "0.0.0.0",
    githubApiUrl: String = "https://api.github.com",
    nThreadsBlocking: Int = 4,
    totalClientConnections: Int = 50,
    clientMaxWaitQueueLimit: Int = 512,
    ghToken : Option[String] = None
)