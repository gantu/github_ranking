package kg.challange.ranking

import cats.effect._

import org.http4s.client.blaze.BlazeClientBuilder
import org.specs2.mutable.Specification
import org.specs2.specification.BeforeAfterAll
import scala.language.higherKinds

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.core.WireMockConfiguration._

import scala.concurrent.{ExecutionContext, ExecutionContextExecutorService}
import java.util.concurrent.Executors

import kg.challange.ranking.service.GithubRankingService
import kg.challange.ranking.models.{Repository, Contributor}
import kg.challange.ranking.config.AppConfig


class GithubRankingServiceSpec extends Specification with BeforeAfterAll {

  lazy val wireMockServer = new WireMockServer(wireMockConfig().port(port))

  def beforeAll: Unit = {
    wireMockServer.start()
    WireMock.configureFor(host, port)
  }

  def afterAll: Unit = wireMockServer.stop()

  implicit lazy val context: ContextShift[IO] =
    IO.contextShift(scala.concurrent.ExecutionContext.global)

  lazy val blockingEC: ExecutionContextExecutorService =
    ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(2))

  lazy val testConfig: AppConfig = AppConfig(
    githubApiUrl = s"http://$host:$port"
  )

  val port = 3000
  val host = "localhost"
  val hostName = s"$host:$port"

  "GithubRankingService" should {
    "return repos of given organization" in {

      val orgRepositories: String =
        s"""[
          |    {
          |        "id": 123456,
          |        "name": "scala"
          |    }
          |]""".stripMargin

      val repoContributors: String =
        s"""
          |[
          |    {
          |        "login": "martin",
          |        "contributions": 1000
          |    }
          |]
        """.stripMargin

      stubSingleRepoOrg("scala", "scala", orgRepositories, repoContributors)

      val repoList = fetchRepositories("scala")

      repoList.size must be_==(1)

      repoList.head.id must beEqualTo(123456)
      repoList.head.name must beEqualTo("scala")
    }
  }

  "return sorted contributors of all repositories within organization" in {

    val organization = "scala"
    val repository1 = "scala-lang"
    val repository2 = "scala-dev"
    val repository3 = "scala"
    val repository4 = "scala-xml"
    val contributor1Login = "martin"
    val contributor1Con = 100
    val contributor2Login = "adriaanm"
    val contributor2Con = 13
    val contributor3Login = "amirsh"
    val contributor3Con = 5545
    val contributor4Login = "axel22"
    val contributor4Con = 3
    val contributor5Login = "cvogt"
    val contributor5Con = 15

    val orgReposFirstPage = s"""[
                                 |    {
                                 |        "id": 123456789,
                                 |        "name": "$repository1",
                                 |        "full_name": "$organization/$repository4",
                                 |        "private": false
                                 |    }
                                 |]""".stripMargin

    stubFirstOrgReposPage(organization, orgReposFirstPage, 3)

    val orgReposSecondPage = s"""[
                                 |    {
                                 |        "id": 123456789,
                                 |        "node_id": "MDEwOlJlcG9zaXRvcnkxNjQxNTQ2MDc=",
                                 |        "name": "$repository2",
                                 |        "full_name": "$organization/$repository2",
                                 |        "private": false
                                 |    }
                                 |]""".stripMargin

    stubNotFirstOrgReposPage(organization, orgReposSecondPage, 2)

    val orgReposThirdPage = s"""[
                                  |    {
                                  |        "id": 123456789,
                                  |        "node_id": "MDEwOlJlcG9zaXRvcnkxNjQxNTQ2MDc=",
                                  |        "name": "$repository3",
                                  |        "full_name": "$organization/$repository3",
                                  |        "private": false
                                  |    },
                                  |        {
                                  |        "id": 123456789,
                                  |        "node_id": "MDEwOlJlcG9zaXRvcnkxNjQxNTQ2MDc=",
                                  |        "name": "$repository4",
                                  |        "full_name": "$organization/$repository4",
                                  |        "private": false
                                  |    }
                                  |]""".stripMargin

    stubNotFirstOrgReposPage(organization, orgReposThirdPage, 3)

    val firstRepoFirstContributorsPage = s"""
                                              |[
                                              |    {
                                              |        "login": "$contributor4Login",
                                              |        "contributions": 50
                                              |    },
                                              |    {
                                              |        "login": "$contributor3Login",
                                              |        "contributions": 100
                                              |    },
                                              |    {
                                              |        "login": "$contributor2Login",
                                              |        "contributions": 1000
                                              |    }
                                              |]
                                              |""".stripMargin

    stubForFirstContributorsPage(
      organization,
      repository1,
      firstRepoFirstContributorsPage,
      2
    )

    val firstRepoSecondContributorsPage = s"""
                                          |[
                                          |    {
                                          |        "login": "$contributor2Login",
                                          |        "contributions": 1
                                          |    }
                                          |]""".stripMargin
    stubForNotFirstContributorsPage(
      repository1,
      firstRepoSecondContributorsPage,
      2
    )

    val secondRepoContributorPage = s"""
                                    |[
                                    |    {
                                    |        "login": "$contributor4Login",
                                    |        "contributions": 25
                                    |    },
                                    |    {
                                    |        "login": "$contributor1Login",
                                    |        "contributions": 1
                                    |    },
                                    |    {
                                    |        "login": "$contributor2Login",
                                    |        "contributions": 1000
                                    |    }
                                    |]
                                    |""".stripMargin

    stubForFirstContributorsPage(
      organization,
      repository2,
      secondRepoContributorPage,
      1
    )

    val thirdRepoFirstContributorsPage = s"""
                                              |[
                                              |    {
                                              |        "login": "$contributor1Login",
                                              |        "contributions": 200
                                              |    },
                                              |    {
                                              |        "login": "$contributor5Login",
                                              |        "contributions": 23
                                              |    },
                                              |    {
                                              |        "login": "$contributor2Login",
                                              |        "contributions": 234
                                              |    }
                                              |]""".stripMargin

    stubForFirstContributorsPage(
      organization,
      repository3,
      thirdRepoFirstContributorsPage,
      2
    )

    val thirdRepoSecondContributorsPage = s"""
                                          |[
                                          |    {
                                          |        "login": "$contributor2Login",
                                          |        "contributions": 2121
                                          |    }
                                          |]
                                          |""".stripMargin

    stubForNotFirstContributorsPage(
      repository3,
      thirdRepoSecondContributorsPage,
      2
    )

    val fourthRepoContributorsPage = s"""
                                              |[
                                              |    {
                                              |        "login": "$contributor5Login",
                                              |        "contributions": 1000
                                              |    }
                                              |]
                                              |""".stripMargin

    stubForFirstContributorsPage(
      organization,
      repository4,
      fourthRepoContributorsPage,
      1
    )

    val contributorsList = fetchContributors(organization)
    contributorsList.size must be_==(5)
    contributorsList.head.login must be_==("martin")
    contributorsList must beEqualTo(
      List(
        Contributor(contributor2Login, contributor2Con),
        Contributor(contributor1Login, contributor1Con),
        Contributor(contributor5Login, contributor5Con),
        Contributor(contributor2Login, contributor2Con),
        Contributor(contributor4Login, contributor4Con)
      )
    )
  }

  def stubSingleRepoOrg(
      orgName: String,
      repoName: String,
      orgReposPage: String,
      repoContributorsPage: String
  ): Unit = {
    wireMockServer.stubFor(
      get(urlPathEqualTo(s"/orgs/$orgName/repos"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(orgReposPage)
            .withStatus(200)
        )
    )

    stubForOrgReposPage(orgName, repoName, repoContributorsPage)
  }

  def stubForOrgReposPage(
      orgName: String,
      repoName: String,
      repoPage: String
  ): Unit =
    wireMockServer.stubFor(
      get(urlPathEqualTo(s"/repos/$orgName/$repoName/contributors"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(repoPage)
            .withStatus(200)
        )
    )

  def stubForNotFirstContributorsPage(
      repoName: String,
      page: String,
      pageNum: Int
  ): Unit =
    wireMockServer.stubFor(
      get(urlPathEqualTo(s"/repositories/$repoName/contributors?page=$pageNum"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(page)
            .withStatus(200)
        )
    )

  def stubForFirstContributorsPage(
      orgName: String,
      repoName: String,
      page: String,
      lastPageNum: Int
  ): Unit =
    wireMockServer.stubFor(
      get(urlPathEqualTo(s"/repos/$orgName/$repoName/contributors"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withHeader(
              "Link",
              s"""<https://$hostName/repositories/$repoName/contributors?page=2>; rel="next", <http://$hostName/repositories/$repoName/contributors?page=$lastPageNum>; rel="last"""""
            )
            .withBody(page)
            .withStatus(200)
        )
    )

  def stubNotFirstOrgReposPage(
      orgName: String,
      page: String,
      pageNum: Int
  ): Unit =
    wireMockServer.stubFor(
      get(urlPathEqualTo(s"/organizations/$orgName/repos?page=$pageNum"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(page)
            .withStatus(200)
        )
    )

  def stubFirstOrgReposPage(
      orgName: String,
      page: String,
      lastPageNum: Int
  ): Unit =
    wireMockServer.stubFor(
      get(urlPathEqualTo(s"/orgs/$orgName/repos"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withHeader(
              "Link",
              s"""<http://$hostName/organizations/$orgName/repos?page=2>; rel="next", <http://$hostName/organizations/$orgName/repos?page=$lastPageNum>; rel="last""""
            )
            .withBody(page)
            .withStatus(200)
        )
    )

  def fetchContributors(organization: String): List[Contributor] = {
    BlazeClientBuilder[IO](blockingEC).resource.use { client =>
      GithubRankingService[IO](client, testConfig).getCompleteData(organization)
    }
  }.unsafeRunSync()

  def fetchRepositories(organization: String): List[Repository] = {
    BlazeClientBuilder[IO](blockingEC).resource.use { client =>
      GithubRankingService[IO](client, testConfig)
        .getRepositories(organization)
    }
  }.unsafeRunSync()
}
