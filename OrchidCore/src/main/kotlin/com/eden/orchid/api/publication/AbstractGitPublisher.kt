package com.eden.orchid.api.publication

import com.eden.orchid.api.OrchidContext
import com.eden.orchid.api.options.annotations.Description
import com.eden.orchid.api.options.annotations.Option
import com.eden.orchid.api.options.annotations.StringDefault
import com.eden.orchid.api.util.GitFacade
import com.eden.orchid.api.util.copy
import com.eden.orchid.api.util.delete
import com.eden.orchid.api.util.makeSubDir
import javax.inject.Inject
import javax.inject.Named
import javax.validation.constraints.Email
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotNull

abstract class AbstractGitPublisher
@Inject
constructor(
    context: OrchidContext,
    private val git: GitFacade,

    @Named("dest")
    private val destinationDir: String,

    private val defaultBranch: String,

    type: String,
    priority: Int
) : OrchidPublisher(context, type, priority) {

    @Option
    @StringDefault("Orchid")
    @Description("The username on the commit.")
    @NotBlank
    lateinit var commitUsername: String

    @Option
    @StringDefault("orchid@orchid")
    @Description("The email on the commit.")
    @NotBlank
    @Email
    lateinit var commitEmail: String

    @Option
    @StringDefault("Deploy to GitHub Pages from Orchid.")
    @Description("The commit message to attach to this deploy.")
    @NotBlank
    lateinit var commitMessage: String

    @Option
    @Description("The branch to push to.")
    @NotBlank(message = "Must set the repository branch.")
    lateinit var branch: String

    @Option
    @StringDefault("CleanBranch")
    @Description("The type of publication to use for Github Pages.\n- `CleanBranch`: Create a new branch with no history and force-push to the remote. Overwrites existing branch completely.\n- `CleanBranchMaintainHistory`: Clone existing branch, remove all files, then push to the remote. Overwrites all files, but maintains history.\n- `VersionedBranch`: Clone existing branch, add current site to a versioned subfolder, then push to the remote. Maintains history and all prior versions\' content.\n- `VersionedBranchWithLatest`: Clone existing branch, add current site to a \'latest\' and a versioned subfolder, then force-push to the remote. Maintains history and all prior versions\' content.")
    @NotNull(message = "Must set a valid publish type.")
    lateinit var publishType: PublishType

    @Option
    @StringDefault("latest")
    @Description("The name of the \'latest\' directory used for the VersionedBranchWithLatest publish type.")
    @NotBlank
    lateinit var latestDirName: String

    protected abstract val displayedRemoteUrl: String
    protected abstract val remoteUrl: String

    override fun validate(): Boolean {
        if(branch.isBlank()) branch = defaultBranch
        return super.validate()
    }

    override fun publish() {
        when (publishType) {
            AbstractGitPublisher.PublishType.CleanBranch                -> doCleanBranch()
            AbstractGitPublisher.PublishType.CleanBranchMaintainHistory -> doCleanBranchMaintainHistory()
            AbstractGitPublisher.PublishType.VersionedBranch            -> doVersionedBranch()
            AbstractGitPublisher.PublishType.VersionedBranchWithLatest  -> doVersionedBranchWithLatest()
        }
    }

// Git publish types
//----------------------------------------------------------------------------------------------------------------------

    private fun doCleanBranch() {
        git.init(remoteUrl, displayedRemoteUrl, branch).apply {
            copy(from = destinationDir)

            commit(commitUsername, commitEmail, commitMessage)
            push(true)
        }
    }

    private fun doCleanBranchMaintainHistory() {
        git.clone(remoteUrl, displayedRemoteUrl, branch).apply {
            delete()
            copy(from = destinationDir)

            commit(commitUsername, commitEmail, commitMessage)
            push(false)
        }
    }

    private fun doVersionedBranch() {
        git.clone(remoteUrl, displayedRemoteUrl, branch).apply {
            makeSubDir(context.version).apply {
                delete(subdirectory = this)
                copy(subdirectory = this, from = destinationDir)
            }

            commit(commitUsername, commitEmail, commitMessage)
            push(false)
        }
    }

    private fun doVersionedBranchWithLatest() {
        git.clone(remoteUrl, displayedRemoteUrl, branch).apply {
            makeSubDir(context.version).apply {
                delete(subdirectory = this)
                copy(subdirectory = this, from = destinationDir)
            }

            makeSubDir(latestDirName).apply {
                delete(subdirectory = this)
                copy(subdirectory = this, from = destinationDir)
            }

            commit(commitUsername, commitEmail, commitMessage)
            push(true)
        }
    }

    enum class PublishType {
        CleanBranch, CleanBranchMaintainHistory, VersionedBranch, VersionedBranchWithLatest
    }

}
