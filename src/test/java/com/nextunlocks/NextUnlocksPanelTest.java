package com.nextunlocks;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class NextUnlocksPanelTest
{
	@Test
	public void displayTitleUsesTheSubquestOrSectionName()
	{
		assertEquals("Barbarian Herblore", NextUnlocksPanel.displayTitle("Barbarian Training#Barbarian Herblore"));
		assertEquals("Another Cook's Quest", NextUnlocksPanel.displayTitle("Recipe for Disaster/Another Cook's Quest"));
		assertEquals("Easy Ardougne Diary", NextUnlocksPanel.displayTitle("Easy Ardougne Diary"));
		assertEquals("Odd/", NextUnlocksPanel.displayTitle("Odd/"));
	}
}
