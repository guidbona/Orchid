package com.eden.orchid.api.resources.resource;

import com.eden.orchid.api.resources.OrchidReference;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * A Resource type that provides a content to a template from a resource file contained with a jarfile. When used with
 * renderTemplate() or renderString(), this resource will supply the `page.content` variable to the template renderer as
 * the file contents after having the embedded data removed, and any embedded data will be available in the renderer
 * through the `page` variable. When used with renderRaw(), the raw contents (after having the embedded data removed)
 * will be written directly instead.
 */
public final class JarResource extends FreeableResource {

    private final JarEntry jarEntry;
    private final JarFile jarFile;

    public JarResource(JarFile jarFile, JarEntry jarEntry) {
        super(new OrchidReference(jarEntry.getName()));
        this.jarEntry = jarEntry;
        this.jarFile = jarFile;
    }

    public JarResource(JarFile jarFile, JarEntry jarEntry, OrchidReference reference) {
        super(reference);
        this.jarEntry = jarEntry;
        this.jarFile = jarFile;
    }

    @Override
    protected void loadContent() {
        if(rawContent == null) {
            try {
                if(jarFile != null && jarEntry != null) {
                    rawContent = IOUtils.toString(jarFile.getInputStream(jarEntry));
                }
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }

        super.loadContent();
    }
}