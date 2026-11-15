package com.genvision.nova.core.nav

import com.genvision.nova.core.TestData
import com.genvision.nova.core.data.LoadResult
import com.genvision.nova.core.data.LoadedData
import com.genvision.nova.core.model.Edge
import com.genvision.nova.core.model.MapData
import com.genvision.nova.core.model.Marker
import com.genvision.nova.core.model.MarkersData
import com.genvision.nova.core.model.MarkersMeta
import com.genvision.nova.core.model.Node
import com.genvision.nova.core.nav.NavState.*
import com.genvision.nova.core.speech.Phrases
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NavigationMachineTest {
    private val clock = FakeClock()
    private val machine = NavigationMachine(clock)

    private val entrance = "You are at the entrance. Walk straight about 2 steps. Then climb 16 steps up."
    private val landing = "You are at the stairs landing. Walk straight about 2 steps. Then climb 10 steps up."

    private fun send(e: NavEvent) = machine.handle(e)
    private fun List<Action>.texts() = filterIsInstance<Action.Speak>().map { it.text }

    private fun loaded(m: NavigationMachine = machine) {
        m.handle(NavEvent.DataLoaded(LoadResult.Success(TestData.loaded)))
    }

    private fun choose(dest: String) =
        send(NavEvent.DestinationChosen(dest, cameraPermissionGranted = true))

    private fun sight(vararg ids: Int) = ids.map { send(NavEvent.Sighting(it)) }.last()

    // Row 1
    @Test fun row01_dataLoadedOk_speaksReadyAndGoesHome() {
        val a = send(NavEvent.DataLoaded(LoadResult.Success(TestData.loaded)))
        assertEquals(listOf(Phrases.READY), a.texts())
        assertEquals(HOME, machine.state)
    }

    // Row 2
    @Test fun row02_dataLoadedFailed_showsAndSpeaksError() {
        val a = send(NavEvent.DataLoaded(LoadResult.Failure(listOf("Bad file."))))
        assertTrue(a.any { it is Action.ShowError })
        assertEquals(listOf("Bad file."), a.texts())
        assertEquals(ERROR, machine.state)
    }

    // Row 3
    @Test fun row03_destinationChosenWithPermission_startsCamera() {
        loaded()
        val a = choose("D13")
        assertTrue(Action.StartCamera in a)
        assertEquals(listOf("Going to D13. Point the phone forward at a marker."), a.texts())
        assertEquals(WAITING, machine.state)
    }

    // Row 4
    @Test fun row04_destinationChosenWithoutPermission_asksForIt() {
        loaded()
        val a = send(NavEvent.DestinationChosen("D13", cameraPermissionGranted = false))
        assertTrue(Action.RequestPermission in a)
        assertEquals(PERMISSION, machine.state)
    }

    // Row 5
    @Test fun row05_permissionGranted_sameAsRow3() {
        loaded()
        send(NavEvent.DestinationChosen("D13", false))
        val a = send(NavEvent.PermissionResult(true))
        assertTrue(Action.StartCamera in a)
        assertEquals(listOf("Going to D13. Point the phone forward at a marker."), a.texts())
        assertEquals(WAITING, machine.state)
    }

    // Row 6
    @Test fun row06_permissionDenied_explainsAndGoesHome() {
        loaded()
        send(NavEvent.DestinationChosen("D13", false))
        val a = send(NavEvent.PermissionResult(false))
        assertEquals(listOf(Phrases.PERMISSION_DENIED), a.texts())
        assertTrue(Action.ShowSettingsButton in a)
        assertEquals(HOME, machine.state)
    }

    // Row 7
    @Test fun row07_waitingSightingAtDestination_arrives() {
        loaded(); choose("D13")
        val a = sight(10)
        assertEquals(listOf("You have arrived. D13 is on your right."), a.texts())
        assertTrue((a.first { it is Action.Speak && it.protected }) is Action.Speak)
        assertEquals(ARRIVED, machine.state)
    }

    // Row 8
    @Test fun row08_waitingSightingWithForwardPath_plansAndSpeaksFirstLeg() {
        loaded(); choose("D13")
        val a = sight(0)
        assertEquals(listOf(entrance), a.texts())
        assertEquals(GUIDING, machine.state)
    }

    // Row 9
    @Test fun row09_waitingSightingWithNoForwardPath_saysBehindAndKeepsWaiting() {
        loaded(); choose("D11")
        val a = sight(10)
        assertEquals(listOf(Phrases.destinationBehind("D11")), a.texts())
        assertEquals(WAITING, machine.state)
    }

    // Row 10
    @Test fun row10_guidingSightingAtDestination_arrives() {
        loaded(); choose("D13"); sight(0)
        val a = sight(10)
        assertEquals(listOf("You have arrived. D13 is on your right."), a.texts())
        assertEquals(ARRIVED, machine.state)
    }

    // Row 11
    @Test fun row11_guidingSightingAhead_speaksThatLeg() {
        loaded(); choose("D13"); sight(0)
        assertEquals(listOf(landing), sight(2).texts())
        assertEquals(GUIDING, machine.state)
    }

    @Test fun row11_skippingMarkersIsFine() {
        loaded(); choose("D12"); sight(0)
        val a = sight(6)
        assertEquals(listOf("You are at the walkway corner near the stairs. Turn left. Walk about 14 steps."), a.texts())
    }

    @Test fun demoWalkThroughMatchesGoldenTextEndToEnd() {
        loaded(); choose("D13")
        val spoken = listOf(0, 2, 4, 6, 7, 8, 10).flatMap { sight(it).texts() }
        assertEquals(
            listOf(
                entrance, landing,
                "You are at the top of the main stairs. Turn right. Walk about 8 steps.",
                "You are at the walkway corner near the stairs. Turn left. Walk about 14 steps.",
                "You are at the start of the D block path. Turn right. Walk about 11 steps.",
                "You are beside D11. Walk straight about 15 steps.",
                "You have arrived. D13 is on your right.",
            ),
            spoken,
        )
    }

    // Row 12
    @Test fun row12_guidingSightingAtEarlierPlace_saysBackAndPlansAgain() {
        loaded(); choose("D13"); sight(0, 2, 4)
        val a = sight(2)
        assertEquals(listOf("You are back at the stairs landing.", landing), a.texts())
        assertEquals(GUIDING, machine.state)
        // The second sentence queues behind the first instead of cutting it off.
        assertEquals(false, (a.filterIsInstance<Action.Speak>()[1]).interrupt)
        // After replanning from the landing, the next marker on the route is "ahead" again.
        assertEquals(
            listOf("You are at the top of the main stairs. Turn right. Walk about 8 steps."),
            sight(4).texts(),
        )
    }

    // Row 13 (no forward path)
    @Test fun row13_offRouteWithNoForwardPath_saysBehindInstead() {
        loaded(); choose("D12"); sight(0)
        val a = sight(10) // D13 is past D12, so D12 is now behind
        assertEquals(listOf(Phrases.destinationBehind("D12")), a.texts())
        assertEquals(GUIDING, machine.state)
    }

    // Row 13 (a path exists): needs a map with a side branch
    @Test fun row13_offRouteWithAPath_saysNewRouteThenInstruction() {
        val map = MapData(
            nodes = listOf(
                Node("A", "the start", "junction", x = 0.0, y = 0.0),
                Node("B", "place B", "junction", x = 0.0, y = -7.0),
                Node("C", "place C", "junction", x = 7.0, y = 0.0),
                Node("R", "R1", "room", x = 7.0, y = -7.0, doorSide = "left"),
            ),
            edges = listOf(
                Edge("A", "B", 7.0, "walk"), Edge("B", "R", 7.0, "walk"),
                Edge("A", "C", 20.0, "walk"), Edge("C", "R", 7.0, "walk"),
            ),
            destinations = listOf("R"),
        )
        val markers = MarkersData(
            MarkersMeta(dictionary = "DICT_4X4_50"),
            listOf(Marker(0, "A", 0.0), Marker(1, "B", 0.0), Marker(2, "C", 90.0)),
        )
        val m = NavigationMachine(clock)
        m.handle(NavEvent.DataLoaded(LoadResult.Success(LoadedData(map, markers))))
        m.handle(NavEvent.DestinationChosen("R", true))
        m.handle(NavEvent.Sighting(0)) // planned route is A, B, R
        val a = m.handle(NavEvent.Sighting(2)) // C is not on it
        assertEquals(
            listOf(
                Phrases.OFF_ROUTE,
                "You are at place C. Turn left. Walk about 10 steps.",
            ),
            a.texts(),
        )
        assertTrue(a.any { it is Action.Log && it.event == "off_route" })
        assertEquals(GUIDING, m.state)
    }

    // Rows 14 and 15
    @Test fun rows14and15_remindersAt12_27_42_thenHelpAt57_thenNothing() {
        loaded(); choose("D13"); sight(0)
        val heard = mutableListOf<Pair<Long, String>>()
        for (second in 1..120) {
            clock.advance(1000)
            send(NavEvent.Tick).texts().forEach { heard += second.toLong() to it }
        }
        assertEquals(
            listOf(
                12L to Phrases.NO_MARKER_REMINDER,
                27L to Phrases.NO_MARKER_REMINDER,
                42L to Phrases.NO_MARKER_REMINDER,
                57L to Phrases.HELP_AFTER_REMINDERS,
            ),
            heard,
        )
    }

    @Test fun reminderTimerStartsWhenWaitingBegins() {
        loaded(); choose("D13")
        clock.advance(11_999)
        assertEquals(emptyList(), send(NavEvent.Tick).texts())
        clock.advance(1)
        assertEquals(listOf(Phrases.NO_MARKER_REMINDER), send(NavEvent.Tick).texts())
    }

    @Test fun aSightingResetsTheReminderCount() {
        loaded(); choose("D13"); sight(0)
        clock.advance(12_000); send(NavEvent.Tick)
        clock.advance(15_000); send(NavEvent.Tick) // two reminders so far
        sight(2) // resets
        clock.advance(11_999)
        assertEquals(emptyList(), send(NavEvent.Tick).texts())
        clock.advance(1)
        assertEquals(listOf(Phrases.NO_MARKER_REMINDER), send(NavEvent.Tick).texts())
    }

    // Row 16
    @Test fun row16_repeatSpeaksTheLastInstruction() {
        loaded(); choose("D13")
        assertEquals(emptyList(), send(NavEvent.RepeatPressed).texts()) // nothing yet
        sight(0, 2)
        assertEquals(listOf(landing), send(NavEvent.RepeatPressed).texts())
    }

    @Test fun row16_repeatAfterOffRouteMessageRepeatsBothSentences() {
        loaded(); choose("D13"); sight(0, 2, 4, 2)
        assertEquals(listOf("You are back at the stairs landing. $landing"), send(NavEvent.RepeatPressed).texts())
    }

    // Row 17
    @Test fun row17_whereAmI() {
        loaded(); choose("D13")
        assertEquals(listOf("No marker seen yet."), send(NavEvent.WhereAmIPressed).texts())
        sight(0, 2)
        clock.advance(12_000)
        assertEquals(listOf("Last marker: the stairs landing, 12 seconds ago."), send(NavEvent.WhereAmIPressed).texts())
    }

    // Row 18
    @Test fun row18_stopStopsCameraAndGoesHome() {
        loaded(); choose("D13"); sight(0)
        val a = send(NavEvent.StopPressed)
        assertTrue(Action.StopCamera in a)
        assertEquals(listOf("Guidance stopped."), a.texts())
        assertEquals(HOME, machine.state)
    }

    // Row 19
    @Test fun row19_chooseAnotherReturnsHome() {
        loaded(); choose("D13"); sight(10)
        val a = send(NavEvent.ChooseAnotherPressed)
        assertTrue(Action.StopCamera in a)
        assertEquals(HOME, machine.state)
    }

    // Row 20 and 21
    @Test fun rows20and21_cameraErrorThenRetryReturnsToGuiding() {
        loaded(); choose("D13"); sight(0)
        val a = send(NavEvent.ErrorOccurred(ErrorKind.CAMERA, "The camera stopped."))
        assertTrue(a.any { it is Action.ShowError })
        assertEquals(listOf("The camera stopped."), a.texts())
        assertEquals(ERROR, machine.state)
        val r = send(NavEvent.RetryPressed)
        assertEquals(listOf(Action.RetryStep(ErrorKind.CAMERA)), r)
        assertEquals(GUIDING, machine.state)
    }

    @Test fun row21_dataErrorRetryGoesBackToLoading() {
        send(NavEvent.DataLoaded(LoadResult.Failure(listOf("Bad."))))
        val r = send(NavEvent.RetryPressed)
        assertEquals(listOf(Action.RetryStep(ErrorKind.DATA)), r)
        assertEquals(LOADING, machine.state)
        loaded()
        assertEquals(HOME, machine.state)
    }

    @Test fun errorFromHomeRetriesToHomeAndBackGoesHome() {
        loaded()
        send(NavEvent.ErrorOccurred(ErrorKind.SPEECH, "No English voice."))
        send(NavEvent.RetryPressed)
        assertEquals(HOME, machine.state)
        send(NavEvent.ErrorOccurred(ErrorKind.SPEECH, "No English voice."))
        send(NavEvent.BackPressed)
        assertEquals(HOME, machine.state)
    }

    // Row 22
    @Test fun row22_backgroundPausesCameraKeepsStateAndResumes() {
        loaded(); choose("D13"); sight(0)
        assertEquals(listOf(Action.PauseCamera), send(NavEvent.AppBackgrounded))
        assertEquals(GUIDING, machine.state)
        clock.advance(60_000)
        assertEquals(emptyList(), send(NavEvent.Tick).texts()) // no reminders while away
        assertEquals(emptyList(), send(NavEvent.Sighting(2)).texts()) // camera is off
        assertEquals(listOf(Action.ResumeCamera), send(NavEvent.AppForegrounded))
        assertEquals(GUIDING, machine.state)
        clock.advance(11_999)
        assertEquals(emptyList(), send(NavEvent.Tick).texts()) // timer restarted on return
    }

    // Rules
    @Test fun sightingsAreLogged() {
        loaded(); choose("D13")
        val a = send(NavEvent.Sighting(0))
        assertTrue(a.contains(Action.Log("sighting", mapOf("marker" to "0", "node" to "ENTRANCE_PLAZA"))))
    }

    @Test fun unknownMarkerIsLoggedAndIgnored() {
        loaded(); choose("D13")
        val a = send(NavEvent.Sighting(33))
        assertEquals(emptyList(), a.texts())
        assertEquals(WAITING, machine.state)
    }

    @Test fun stepLengthSettingAppliesToTheNextInstruction() {
        var step = 0.70
        val m = NavigationMachine(clock) { step }
        loaded(m)
        m.handle(NavEvent.DestinationChosen("D13", true))
        m.handle(NavEvent.Sighting(0))
        step = 0.60
        val a = m.handle(NavEvent.Sighting(4))
        assertEquals(listOf("You are at the top of the main stairs. Turn right. Walk about 9 steps."), a.texts())
    }
}
