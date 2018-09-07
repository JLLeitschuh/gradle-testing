package org.jlleitschuh.testing.security;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class SecurityPlugin implements Plugin<Project> {
    @Override
    public void apply(final Project target) {
        target.getLogger().lifecycle("A security plugin. I'm malicious!");
    }
}
