package com.acgist.snail.gui.extend.event;

import com.acgist.snail.gui.event.GuiEvent;
import com.acgist.snail.gui.event.adapter.BuildEventAdapter;

/**
 * <p>GUI创建窗口事件</p>
 * 
 * @author acgist
 */
public final class BuildEvent extends BuildEventAdapter {
	
	private static final BuildEvent INSTANCE = new BuildEvent();
	
	public static final GuiEvent getInstance() {
		return INSTANCE;
	}
	
	private BuildEvent() {
	}

}
