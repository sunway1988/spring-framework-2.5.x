/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.scheduling.commonj;

import java.util.Collection;

import javax.naming.NamingException;

import commonj.work.Work;
import commonj.work.WorkException;
import commonj.work.WorkItem;
import commonj.work.WorkListener;
import commonj.work.WorkManager;
import commonj.work.WorkRejectedException;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.jndi.JndiLocatorSupport;
import org.springframework.scheduling.SchedulingException;
import org.springframework.scheduling.SchedulingTaskExecutor;
import org.springframework.util.Assert;

/**
 * TaskExecutor implementation that delegates to a CommonJ WorkManager,
 * implementing the {@link commonj.work.WorkManager} interface,
 * which either needs to be specified as reference or through the JNDI name.
 *
 * <p><b>This is the central convenience class for setting up a
 * CommonJ WorkManager in a Spring context.</b>
 *
 * <p>Also implements the CommonJ WorkManager interface itself, delegating all
 * calls to the target WorkManager. Hence, a caller can choose whether it wants
 * to talk to this executor through the Spring TaskExecutor interface or the
 * CommonJ WorkManager interface.
 *
 * <p>The CommonJ WorkManager will usually be retrieved from the application
 * server's JNDI environment, as defined in the server's management console.
 *
 * <p><b>Note: At the time of this writing, the CommonJ WorkManager facility
 * is only supported on IBM WebSphere 6.0+ and BEA WebLogic 9.0+,
 * despite being such a crucial API for an application server.</b>
 * (There is a similar facility available on WebSphere 5.1 Enterprise,
 * though, which we will discuss below.)
 *
 * <p><b>On JBoss and GlassFish, a similar facility is available through
 * the JCA WorkManager.</b> See the
 * {@link org.springframework.jca.work.jboss.JBossWorkManagerTaskExecutor}
 * {@link org.springframework.jca.work.glassfish.GlassFishWorkManagerTaskExecutor}
 * classes which are the direct equivalent of this CommonJ adapter class.
 *
 * <p>A similar facility is available on WebSphere 5.1, under the name
 * "Asynch Beans". Its central interface is called WorkManager too and is
 * also obtained from JNDI, just like a standard CommonJ WorkManager.
 * However, this WorkManager variant is notably different: The central
 * execution method is called "startWork" instead of "schedule",
 * and takes a slightly different Work interface as parameter.
 *
 * <p>Support for this WebSphere 5.1 variant can be built with this class
 * and its helper DelegatingWork as template: Call the WorkManager's
 * <code>startWork(Work)</code> instead of <code>schedule(Work)</code>
 * in the <code>execute(Runnable)</code> implementation. Furthermore,
 * for simplicity's sake, drop the entire "Implementation of the CommonJ
 * WorkManager interface" section (and the corresponding
 * <code>implements WorkManager</code> clause at the class level).
 * Of course, you also need to change all <code>commonj.work</code> imports in
 * your WorkManagerTaskExecutor and DelegatingWork variants to the corresponding
 * WebSphere API imports (<code>com.ibm.websphere.asynchbeans.WorkManager</code>
 * and <code>com.ibm.websphere.asynchbeans.Work</code>, respectively).
 * This should be sufficient to get a TaskExecutor adapter for WebSphere 5.
 *
 * @author Juergen Hoeller
 * @since 2.0
 */
public class WorkManagerTaskExecutor extends JndiLocatorSupport
		implements SchedulingTaskExecutor, WorkManager, InitializingBean {

	private WorkManager workManager;

	private String workManagerName;

	private WorkListener workListener;


	/**
	 * Specify the CommonJ WorkManager to delegate to.
	 * <p>Alternatively, you can also specify the JNDI name
	 * of the target WorkManager.
	 * @see #setWorkManagerName
	 */
	public void setWorkManager(WorkManager workManager) {
		this.workManager = workManager;
	}

	/**
	 * Set the JNDI name of the CommonJ WorkManager.
	 * <p>This can either be a fully qualified JNDI name,
	 * or the JNDI name relative to the current environment
	 * naming context if "resourceRef" is set to "true".
	 * @see #setWorkManager
	 * @see #setResourceRef
	 */
	public void setWorkManagerName(String workManagerName) {
		this.workManagerName = workManagerName;
	}

	/**
	 * Specify a CommonJ WorkListener to apply, if any.
	 * <p>This shared WorkListener instance will be passed on to the
	 * WorkManager by all {@link #execute} calls on this TaskExecutor.
	 */
	public void setWorkListener(WorkListener workListener) {
		this.workListener = workListener;
	}

	public void afterPropertiesSet() throws NamingException {
		if (this.workManager == null) {
			if (this.workManagerName == null) {
				throw new IllegalArgumentException("Either 'workManager' or 'workManagerName' must be specified");
			}
			this.workManager = (WorkManager) lookup(this.workManagerName, WorkManager.class);
		}
	}


	//-------------------------------------------------------------------------
	// Implementation of the Spring SchedulingTaskExecutor interface
	//-------------------------------------------------------------------------

	public void execute(Runnable task) {
		Assert.state(this.workManager != null, "No WorkManager specified");
		Work work = new DelegatingWork(task);
		try {
			if (this.workListener != null) {
				this.workManager.schedule(work, this.workListener);
			}
			else {
				this.workManager.schedule(work);
			}
		}
		catch (WorkRejectedException ex) {
			throw new TaskRejectedException("CommonJ WorkManager did not accept task: " + task, ex);
		}
		catch (WorkException ex) {
			throw new SchedulingException("Could not schedule task on CommonJ WorkManager", ex);
		}
	}

	/**
	 * This task executor prefers short-lived work units.
	 */
	public boolean prefersShortLivedTasks() {
		return true;
	}


	//-------------------------------------------------------------------------
	// Implementation of the CommonJ WorkManager interface
	//-------------------------------------------------------------------------

	public WorkItem schedule(Work work)
			throws WorkException, IllegalArgumentException {

		return this.workManager.schedule(work);
	}

	public WorkItem schedule(Work work, WorkListener workListener)
			throws WorkException, IllegalArgumentException {

		return this.workManager.schedule(work, workListener);
	}

	public boolean waitForAll(Collection workItems, long timeout)
			throws InterruptedException, IllegalArgumentException {

		return this.workManager.waitForAll(workItems, timeout);
	}

	public Collection waitForAny(Collection workItems, long timeout)
			throws InterruptedException, IllegalArgumentException {

		return this.workManager.waitForAny(workItems, timeout);
	}

}
