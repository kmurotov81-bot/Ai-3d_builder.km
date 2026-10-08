package com.example.socratictutor

import org.junit.Assert.*
import org.junit.Test

class NavStateTest {
    @Test fun goChangesScreenAndLeavesTest() {
        val s = NavState().openTest().go(Screen.PROGRESS)
        assertEquals(Screen.PROGRESS, s.screen)
        assertFalse(s.inTest)
    }

    @Test fun backFromTestThenScreenThenExit() {
        var s = NavState().go(Screen.SETTINGS).openTest()
        s = s.back()!!
        assertFalse(s.inTest); assertEquals(Screen.SETTINGS, s.screen)
        s = s.back()!!
        assertEquals(Screen.HOME, s.screen)
        assertNull(s.back())
    }

    @Test fun practiceOnSetsTopicOnce() {
        val s = NavState().practiceOn(Topic.QUADRATICS)
        assertEquals(Screen.PRACTICE, s.screen)
        assertEquals(Topic.QUADRATICS, s.practiceTopic)
        assertNull(s.consumePracticeTopic().practiceTopic)
    }
}
