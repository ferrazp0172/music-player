package com.github.anrimian.filesync.models.repo

class RepoSetupTemplate(
    val repoType: Int,
    val credentials: RemoteRepoCredentials,
    var remoteRootPath: String,
    var localRootPath: String
) {
    lateinit var accountInfo: RepoAccountInfo
    lateinit var spaceUsage: RepoSpaceUsage
}