# GithubRankinService

This is a service which helps you to fetch information from github API

## Building, running and testing

Please export your GithubToken into the OS Enviroment by ```export GH_TOKEN={gtuhub token}```

Use ```sbt run``` to build and run it
Use ```sbt test``` to invoke specs

## Usage and sample response

```curl -i 'http://localhost:8080/org/{organizationName}'``` fetches all repositories of given organization and returns json document in json format as  ```[{"id":841941,"name":"legacy-svn-scala"}]```

```curl -i 'http://localhost:8080/org/{organizationName}/contributors``` fetches all contributors of given organization and returns sorted in json format as ``` [{"login":"contributor1","contributions":4867},{"login":"contributor2","contributions":1778}]```
