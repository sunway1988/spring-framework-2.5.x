package org.springframework.orm.hibernate.support;

import net.sf.hibernate.SessionFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.orm.hibernate.HibernateTemplate;

/**
 * Convenient super class for Hibernate data access objects.
 * Requires a SessionFactory to be set, providing a
 * HibernateTemplate based on it to subclasses.
 *
 * <p>This base class is mainly intended for HibernateTemplate usage
 * but can also be used when working with SessionFactoryUtils directly,
 * e.g. in combination with HibernateInterceptor-managed Sessions.
 *
 * @author Juergen Hoeller
 * @since 28.07.2003
 * @see #setSessionFactory
 * @see org.springframework.orm.hibernate.HibernateTemplate
 * @see org.springframework.orm.hibernate.HibernateInterceptor
 */
public abstract class HibernateDaoSupport implements InitializingBean {

	protected final Log logger = LogFactory.getLog(getClass());

	private HibernateTemplate hibernateTemplate;

	/**
	 * Set the Hibernate SessionFactory to be used by this DAO.
	 */
	public final void setSessionFactory(SessionFactory sessionFactory) {
	  this.hibernateTemplate = new HibernateTemplate(sessionFactory);
	}

	/**
	 * Return the Hibernate SessionFactory used by this DAO.
	 */
	protected final SessionFactory getSessionFactory() {
		return hibernateTemplate.getSessionFactory();
	}

	/**
	 * Set the HibernateTemplate for this DAO explicitly,
	 * as an alternative to specifying a SessionFactory.
	 */
	public final void setHibernateTemplate(HibernateTemplate hibernateTemplate) {
		this.hibernateTemplate = hibernateTemplate;
	}

	/**
	 * Return the HibernateTemplate for this DAO,
	 * pre-initialized with the SessionFactory or set explicitly.
	 */
	protected final HibernateTemplate getHibernateTemplate() {
	  return hibernateTemplate;
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

}
