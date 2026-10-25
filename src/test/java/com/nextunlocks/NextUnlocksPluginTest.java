package com.nextunlocks;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class NextUnlocksPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(NextUnlocksPlugin.class);
		RuneLite.main(args);
	}
}
