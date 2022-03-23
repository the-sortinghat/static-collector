package com.thesortinghat.staticcollector.application.services

import com.thesortinghat.staticcollector.application.utils.HttpAbstraction
import com.thesortinghat.staticcollector.domain.exceptions.UnableToFetchDataException
import com.thesortinghat.staticcollector.domain.services.DataFetcher
import com.thesortinghat.staticcollector.domain.vo.FetchResponse
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class FetchDockerCompose(@Autowired private val httpAbstraction: HttpAbstraction<String>) : DataFetcher {

    override fun execute(url: String, filename: String): FetchResponse {
        val info = getRemoteRepoInfo(url, filename)
        val response = httpAbstraction.get(info.rawContentUrl)

        if (response.status != 200) {
            throw UnableToFetchDataException("status ${response.status}: error while fetching")
        }

        return FetchResponse("${info.userOrOrgName}/${info.repoName}", response.data)
    }

    private fun getRemoteRepoInfo(url: String, filename: String): RemoteRepoInfo {
        val matchResult = listOf("github", "gitlab")
                .map { "(?:https?://)?(?:www\\.)?($it)\\.com/(.+)/(.+)/?".toRegex().matchEntire(url) }
                .find { it != null && it.groupValues.size >= 4 }
            ?: throw UnableToFetchDataException("given url is invalid")

        val repoType = matchResult.groupValues[1]
        val userOrOrgName = matchResult.groupValues[2]
        val repoName = matchResult.groupValues[3]
        val rawContentUrl = hashMapOf(
                "github" to "https://raw.githubusercontent.com/$userOrOrgName/$repoName/main/$filename",
                "gitlab" to "https://gitlab.com/$userOrOrgName/$repoName/raw/master/$filename"
        )[repoType]!!

        return RemoteRepoInfo(
            userOrOrgName,
            repoName,
            rawContentUrl
        )
    }

    private class RemoteRepoInfo(val userOrOrgName: String, val repoName: String, val rawContentUrl: String)
}
