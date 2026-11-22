package com.nextunlocks;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.Value;

/** What the panel's Guides page shows, handed from the client thread to the EDT. */
@Value
class GuidesView
{
	static final GuidesView EMPTY = new GuidesView(false, false, Collections.emptyList(), Collections.emptyList(), Collections.emptyMap());

	boolean loggedIn;
	boolean iron;
	List<Guide> guides;
	/** Ids of the followed guides, in follow order. */
	List<String> followed;
	/** Each guide's progress by id; empty when logged out. */
	Map<String, GuideProgress> progress;

	boolean isFollowed(Guide guide)
	{
		return followed.contains(guide.getId());
	}
}
