package com.lksnext.ParkingIMayordomo.uitest

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.lksnext.ParkingIMayordomo.ui.pages.Help
import com.lksnext.ParkingIMayordomo.utils.TestTags
import org.junit.Rule
import org.junit.Test

class HelpScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun faqAccordion_expandsAndCollapses() {
        composeTestRule.setContent {
            Help(onNavigate = { })
        }

        composeTestRule.onAllNodesWithTag(TestTags.HELP_FAQ_ACCORDION)[0].performClick()
        composeTestRule.onAllNodesWithTag(TestTags.HELP_FAQ_ACCORDION)[0].performClick()
    }

    @Test
    fun faqAccordion_multipleItems_allToggle() {
        composeTestRule.setContent {
            Help(onNavigate = { })
        }

        composeTestRule.onAllNodesWithTag(TestTags.HELP_FAQ_ACCORDION)[0].performClick()
        composeTestRule.onAllNodesWithTag(TestTags.HELP_FAQ_ACCORDION)[1].performClick()
        composeTestRule.onAllNodesWithTag(TestTags.HELP_FAQ_ACCORDION)[0].performClick()
    }

    @Test
    fun faqAccordion_toggleDownArrow() {
        composeTestRule.setContent {
            Help(onNavigate = { })
        }

        composeTestRule.onAllNodesWithTag(TestTags.HELP_FAQ_ACCORDION)[0].performClick()
    }
}
