/*
 * Copyright 2002-2004 the original author or authors.
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

package org.springframework.orm.hibernate.support;

import net.sf.hibernate.HibernateException;
import net.sf.hibernate.Session;
import net.sf.hibernate.SessionFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.orm.hibernate.HibernateTemplate;
import org.springframework.orm.hibernate.SessionFactoryUtils;

/**
 * Convenient super class for Hibernate data access objects.
 *
 * <p>Requires a SessionFactory to be set, providing a HibernateTemplate
 * based on it to subclasses. Can alternatively be initialized directly via
 * a HibernateTemplate, to reuse the latter's settings like SessionFactory,
 * exception translator, flush mode, etc.</p>
 *
 * <p>This base class is mainly intended for HibernateTemplate usage
 * but can also be used when working with SessionFactoryUtils directly,
 * e.g. in combination with HibernateInterceptor-managed Sessions.
 * Convenience <code>getSession</code> and <code>closeSessionIfNecessary</code>
 * methods are provided for that usage style.</p>
 * 
 * <p>This class will create its own HibernateTemplate if only a SessionFactory
 * is passed in. The allowCreate flag on that HibernateTemplate will true by
 * default. This default value may be overriden.</p>
 *
 * @author Juergen Hoeller
 * @since 28.07.2003
 * @see #setSessionFactory
 * @see #setHibernateTemplate
 * @see #getSession
 * @see #closeSessionIfNecessary
 * @see org.springframework.orm.hibernate.HibernateTemplate
 * @see org.springframework.orm.hibernate.HibernateInterceptor
 */
public abstract class HibernateDaoSupport implements InitializingBean {

	protected final Log logger = LogFactory.getLog(getClass());

	private HibernateTemplate hibernateTemplate;
	protected boolean defaultTemplateAllowCreateValue = true;
	protected boolean selfCreatedHibernateTemplate = false;
	

	/**
	 * Set the Hibernate SessionFactory to be used by this DAO.
	 */
	public void setSessionFactory(SessionFactory sessionFactory) {
	  this.hibernateTemplate = new HibernateTemplate(sessionFactory, defaultTemplateAllowCreateValue);
	  selfCreatedHibernateTemplate = true;
	}

	/**
	 * Return the Hibernate SessionFactory used by this DAO.
	 */
	public final SessionFactory getSessionFactory() {
		return (this.hibernateTemplate != null ? this.hibernateTemplate.getSessionFactory() : null);
	}

	/**
	 * Set the HibernateTemplate for this DAO explicitly,
	 * as an alternative to specifying a SessionFactory.
	 */
	public final void setHibernateTemplate(HibernateTemplate hibernateTemplate) {
		this.hibernateTemplate = hibernateTemplate;
		selfCreatedHibernateTemplate = false;
	}

	/**
	 * Return the HibernateTemplate for this DAO,
	 * pre-initialized with the SessionFactory or set explicitly.
	 */
	public final HibernateTemplate getHibernateTemplate() {
	  return hibernateTemplate;
	}

	
	/**
	 * Allow the default value for the allowCreate flag on any HibernateTemplate
	 * created by this class to be set. If this method is called after
	 * setSessionFactory, which will normally internally immediately created a
	 * HibernateTemplate, that existing template will be called with this value.
	 * This value has no bearing on the allowCreate flag of any externally passed-
	 * in HibernateTemplate
	 * 
	 * @param defaultAllowCreate The default allowCreate flga to set.
	 */
	public void setDefaultTemplateAllowCreateValue(boolean defaultAllowCreate) {
		this.defaultTemplateAllowCreateValue = defaultAllowCreate;
		if (selfCreatedHibernateTemplate)
			getHibernateTemplate().setAllowCreate(defaultAllowCreate);
	}
	
	/**
	 * @return Returns the defaultTemplateAllowCreateValue 
	 */
	public boolean getDefaultTemplateAllowCreateValue() {
		return defaultTemplateAllowCreateValue;
	}
	
	public final void afterPropertiesSet() throws Exception {
		if (this.hibernateTemplate == null) {
			throw new IllegalArgumentException("sessionFactory or hibernateTemplate is required");
		}
		initDao();
	}

	/**
	 * Subclasses can override this for custom initialization behavior.
	 * Gets called after population of this instance's bean properties.
	 * @throws Exception if initialization fails
	 */
	protected void initDao() throws Exception {
	}


	/**
	 * Get a Hibernate Session, either from the current transaction or
	 * a new one. The latter is only allowed if the "allowCreate" setting
	 * of this bean's HibernateTemplate is true.
	 * @return the Hibernate Session
	 * @throws DataAccessResourceFailureException if the Session couldn't be created
	 * @throws IllegalStateException if no thread-bound Session found and allowCreate false
	 * @see org.springframework.orm.hibernate.HibernateTemplate
	 */
	protected final Session getSession()
	    throws DataAccessResourceFailureException, IllegalStateException {
		return getSession(this.hibernateTemplate.isAllowCreate());
	}

	/**
	 * Get a Hibernate Session, either from the current transaction or
	 * a new one. The latter is only allowed if "allowCreate" is true.
	 * @param allowCreate if a new Session should be created if no thread-bound found
	 * @return the Hibernate Session
	 * @throws DataAccessResourceFailureException if the Session couldn't be created
	 * @throws IllegalStateException if no thread-bound Session found and allowCreate false
	 * @see org.springframework.orm.hibernate.SessionFactoryUtils#getSession(SessionFactory, boolean)
	 */
	protected final Session getSession(boolean allowCreate)
	    throws DataAccessResourceFailureException, IllegalStateException {
		return (!allowCreate ?
		    SessionFactoryUtils.getSession(getSessionFactory(), false) :
				SessionFactoryUtils.getSession(getSessionFactory(),
				                               this.hibernateTemplate.getEntityInterceptor(),
																			 this.hibernateTemplate.getJdbcExceptionTranslator()));
	}

	/**
	 * Convert the given HibernateException to an appropriate exception from
	 * the org.springframework.dao hierarchy. Will automatically detect
	 * wrapped SQLExceptions and convert them accordingly.
	 * <p>Delegates to the convertHibernateAccessException method of this
	 * DAO's HibernateTemplate.
	 * @param ex HibernateException that occured
	 * @return the corresponding DataAccessException instance
	 * @see #setHibernateTemplate
	 * @see org.springframework.orm.hibernate.HibernateTemplate#convertHibernateAccessException
	 */
	protected final DataAccessException convertHibernateAccessException(HibernateException ex) {
		return this.hibernateTemplate.convertHibernateAccessException(ex);
	}

	/**
	 * Close the given Hibernate Session if necessary, created via this bean's
	 * SessionFactory, if it isn't bound to the thread.
	 * @param session Session to close
	 * @see org.springframework.orm.hibernate.SessionFactoryUtils#closeSessionIfNecessary
	 */
	protected final void closeSessionIfNecessary(Session session) {
		SessionFactoryUtils.closeSessionIfNecessary(session, getSessionFactory());
	}

}
