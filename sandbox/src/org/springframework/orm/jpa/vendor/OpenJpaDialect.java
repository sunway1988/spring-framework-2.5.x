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
package org.springframework.orm.jpa.vendor;

import java.sql.Connection;
import java.sql.SQLException;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceException;

import org.apache.openjpa.persistence.OpenJPAEntityManager;
import org.springframework.jdbc.datasource.ConnectionHandle;
import org.springframework.jdbc.datasource.SimpleConnectionHandle;
import org.springframework.orm.jpa.DefaultJpaDialect;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;

/**
 * OpenJPA specific dialect.
 * 
 * @author Costin Leau
 *
 */
public class OpenJpaDialect extends DefaultJpaDialect {

	@Override
	public ConnectionHandle getJdbcConnection(EntityManager entityManager, boolean readOnly) throws PersistenceException, SQLException {
		Connection connection = (Connection) getOpenJpaEntityManager(entityManager).getConnection();

		return new SimpleConnectionHandle(connection);
	}

	@Override
	public void releaseJdbcConnection(ConnectionHandle conHandle, EntityManager em) throws PersistenceException, SQLException {
		// be preventive
		if (conHandle != null && conHandle.getConnection() != null)
			conHandle.getConnection().close();
	}

	@Override
	public Object beginTransaction(EntityManager entityManager, TransactionDefinition definition) throws PersistenceException, SQLException, TransactionException {
		super.beginTransaction(entityManager, definition);
		if (!definition.isReadOnly()) {
			// like in Toplink/Kodo case, make sure to start the logic transaction early so that
			// other participants using the connection (like JdbcTemplate) run
			// in a transaction.
			OpenJPAEntityManager manager = getOpenJpaEntityManager(entityManager);
			// start tx
			manager.beginStore();
		}

		return null;
	}

	/**
	 * Return the OpenJPA specific interface of EntityManager.
	 * @param em
	 * @return
	 */
	protected OpenJPAEntityManager getOpenJpaEntityManager(EntityManager em) {
		return ((OpenJPAEntityManager) em);
	}
}
