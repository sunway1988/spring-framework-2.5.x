/*
 * Copyright 2002-2006 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.aop.target.dynamic;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.aop.TargetSource;

/**
 * Abstract TargetSource implementation that wraps a refreshable target object.
 * Subclasses can determine whether a refresh is required, and need to provide
 * fresh target objects.
 *
 * <p>Implements the Refreshable interface for explicit control.
 *
 * @author Rod Johnson
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 2.0
 */
public abstract class AbstractRefreshableTargetSource implements TargetSource, Refreshable {

	/** Logger available to subclasses */
	protected Log logger = LogFactory.getLog(getClass());

	protected Object targetObject;

	private long refreshCheckDelay = -1;

	private long lastRefreshCheck = -1;

	private long lastRefreshTime = -1;

	private long refreshCount = 0;


	/**
	 * Set the delay between refresh checks, in milliseconds.
	 * Default is -1, indicating no refresh checks at all.
	 * <p>Note that an actual refresh will only happen when
	 * <code>requiresRefresh()</code> returns <code>true</code>.
	 * @see #requiresRefresh()
	 */
	public void setRefreshCheckDelay(long refreshCheckDelay) {
		this.refreshCheckDelay = refreshCheckDelay;
	}


	public Class getTargetClass() {
		if (this.targetObject == null) {
			refresh();
		}
		return this.targetObject.getClass();
	}

	/**
	 * Not static.
	 */
	public boolean isStatic() {
		return false;
	}

	public final synchronized Object getTarget() {
		if ((refreshCheckDelayElapsed() && requiresRefresh()) || this.targetObject == null) {
			refresh();
		}
		return this.targetObject;
	}

	/**
	 * No need to release target.
	 */
	public void releaseTarget(Object object) {
	}


	public final synchronized void refresh() {
		logger.debug("Attempting to refresh target");

		this.targetObject = freshTarget();
		this.refreshCount++;
		this.lastRefreshTime = System.currentTimeMillis();

		logger.debug("Target refreshed successfully");
	}

	public long getRefreshCount() {
		return this.refreshCount;
	}

	public long getLastRefreshTime() {
		return this.lastRefreshTime;
	}


	private boolean refreshCheckDelayElapsed() {
		if (this.refreshCheckDelay < 0) {
			return false;
		}

		long currentTimeMillis = System.currentTimeMillis();

		if (this.lastRefreshCheck < 0 || currentTimeMillis - this.lastRefreshCheck > this.refreshCheckDelay) {
			// Going to perform a refresh check - update the time.
			this.lastRefreshCheck = currentTimeMillis;
			logger.debug("Refresh check delay elapsed - checking whether refresh is required");
			return true;
		}

		return false;
	}


	/**
	 * Determine whether a refresh is required.
	 * Invoked for each refresh check, after the refresh check delay has elapsed.
	 * <p>Default implementation always returns <code>true</code>, trigger
	 * a refresh every time the delay has elapsed. To be overridden by subclasses
	 * with an appropriate check of the underlying target resource.
	 */
	protected boolean requiresRefresh() {
		return true;
	}

	/**
	 * Obtain a fresh target object.
	 * Only invoked if a refresh check has found that a refresh is required
	 * (that is, <code>requiresRefresh</code> has returned <code>true</code>).
	 * @see #requiresRefresh()
	 */
	protected abstract Object freshTarget();

}