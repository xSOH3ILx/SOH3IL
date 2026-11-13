package com.soh3il.core.updater

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateUrlPolicyTest {

    @Test
    fun acceptsOnlyHttpsGitHubHosts() {
        assertTrue(UpdateUrlPolicy.isAllowed("https://api.github.com/repos/xSOH3ILx/SOH3IL/releases/latest"))
        assertTrue(UpdateUrlPolicy.isAllowed("https://github.com/xSOH3ILx/SOH3IL/releases/latest"))
        assertFalse(UpdateUrlPolicy.isAllowed("http://api.github.com/repos/xSOH3ILx/SOH3IL/releases/latest"))
        assertFalse(UpdateUrlPolicy.isAllowed("https://example.com/release.apk"))
        assertFalse(UpdateUrlPolicy.isAllowed("https://objects.githubusercontent.com/release.apk"))
    }

    @Test
    fun rejectsLookalikeGitHubHosts() {
        assertFalse(UpdateUrlPolicy.isAllowed("https://githubusercontent.com.evil.example/release.apk"))
        assertFalse(UpdateUrlPolicy.isAllowed("https://github.com.evil.example/release.apk"))
    }
}
