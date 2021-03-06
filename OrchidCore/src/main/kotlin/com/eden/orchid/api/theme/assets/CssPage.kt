package com.eden.orchid.api.theme.assets

import com.eden.orchid.api.options.OptionsHolder
import com.eden.orchid.api.options.annotations.BooleanDefault
import com.eden.orchid.api.options.annotations.Description
import com.eden.orchid.api.options.annotations.Option
import com.eden.orchid.api.resources.resource.ExternalResource
import com.eden.orchid.api.resources.resource.InlineResource
import com.eden.orchid.api.resources.resource.OrchidResource
import com.eden.orchid.api.theme.pages.OrchidReference
import com.eden.orchid.utilities.OrchidUtils

private const val ASSET_DESCRIPTION = "The resource to load as an extra stylesheet"
private const val ATTRS_DESCRIPTION = "Arbitrary attributes to apply to this element when rendered to page"
private const val INLINED_DESCRIPTION = "Inlines the contents of this stylesheet directly into the page instead of being referenced from a URL."
private const val DOWNLOAD_DESCRIPTION = "If the resource is external, download it and serve it from the built site so the site doesn't depend on other servers being available."

interface CssPageAttributes {
    var attrs: MutableMap<String, String>
    var inlined: Boolean
    var download: Boolean
}

@Description(value = "A CSS static asset.", name = "CSS Asset")
class CssPage(
    origin: AssetManagerDelegate,
    resource: OrchidResource,
    key: String,
    title: String
) : AssetPage(origin, resource, key, title), CssPageAttributes {

    @Option
    @Description(ATTRS_DESCRIPTION)
    override lateinit var attrs: MutableMap<String, String>

    @Option
    @Description(INLINED_DESCRIPTION)
    override var inlined: Boolean = false

    @Option
    @BooleanDefault(true)
    @Description(DOWNLOAD_DESCRIPTION)
    override var download: Boolean = true

    fun applyAttributes(config: CssPageAttributes) {
        this.attrs = config.attrs
        this.inlined = config.inlined
        this.download = config.download
    }

    override fun configureReferences() {
        reference = OrchidReference(resource.reference)// copy reference so we can update it
        reference.isUsePrettyUrl = false

        if (resource is ExternalResource) {
            // mark the user's request to download the resource offline
            resource.download = download

            if (resource.shouldDownload) {
                // if the resource should actually should be downloaded offline, then update our reference to point to
                // the offline file, and apply the prefix as needed
                reference.baseUrl = context.baseUrl
                if (origin.prefix != null) {
                    reference.path = OrchidUtils.normalizePath(origin.prefix) + "/" + reference.path
                }
            } else {
                // keep it referencing the external location, don't change anything
            }
        } else {
            // it's just a local file, apply the prefix as needed
            if (origin.prefix != null) {
                reference.path = OrchidUtils.normalizePath(origin.prefix) + "/" + reference.path
            }
        }
    }

    override val shouldInline: Boolean get() {
        return when {
            resource is InlineResource -> true
            resource is ExternalResource && resource.shouldDownload && inlined -> true
            resource !is ExternalResource && inlined -> true
            else -> false
        }
    }

    override fun renderAssetToPage(): String {
        return if (shouldInline) {
            "<style ${renderAttrs(attrs)}>\n${resource.compileContent(context, this)}\n</style>"
        } else {
            """<link rel="stylesheet" type="text/css" href="${this.link}" ${renderAttrs(attrs)}/>"""
        }
    }
}

class ExtraCss : OptionsHolder, CssPageAttributes {

    @Option
    @Description(ASSET_DESCRIPTION)
    lateinit var asset: String

    @Option
    @Description(ATTRS_DESCRIPTION)
    override lateinit var attrs: MutableMap<String, String>

    @Option
    @Description(INLINED_DESCRIPTION)
    override var inlined: Boolean = false

    @Option
    @BooleanDefault(true)
    @Description(DOWNLOAD_DESCRIPTION)
    override var download: Boolean = true

    override fun toString(): String {
        return "ExtraCss(asset='$asset', inlined=$inlined, download=$download)"
    }
}
