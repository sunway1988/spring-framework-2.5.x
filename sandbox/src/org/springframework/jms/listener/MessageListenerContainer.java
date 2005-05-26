/*
 * Copyright 2002-2005 the original author or authors.
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

package org.springframework.jms.listener;

import javax.jms.Connection;
import javax.jms.ConnectionConsumer;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ServerSession;
import javax.jms.ServerSessionPool;
import javax.jms.Session;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.jms.support.JmsAccessor;
import org.springframework.jms.support.JmsUtils;

/**
 * @author Juergen Hoeller
 * @since 25.05.2005
 */
public class MessageListenerContainer extends JmsAccessor implements DisposableBean {

	private Destination destination;

	private String messageSelector;

	private Object messageListener;

	private ServerSessionFactory serverSessionFactory = new SimpleServerSessionFactory();

	private int maxMessages = 1;

	private Connection connection;

	private ConnectionConsumer consumer;

	private ListenerSessionManager sessionManager;


	public void setDestination(Destination destination) {
		this.destination = destination;
	}

	protected Destination getDestination() {
		return destination;
	}

	public void setMessageSelector(String messageSelector) {
		this.messageSelector = messageSelector;
	}

	protected String getMessageSelector() {
		return messageSelector;
	}

	public void setMessageListener(Object messageListener) {
		if (!(messageListener instanceof MessageListener ||
				messageListener instanceof SessionAwareMessageListener)) {
			throw new IllegalArgumentException(
					"messageListener needs to be of type [javax.jms.MessageListener] or " +
					"[org.springframework.jmx.listener.SessionAwareMessageListener]");
		}
		this.messageListener = messageListener;
	}

	protected MessageListener getMessageListener() {
		return (this.messageListener instanceof MessageListener ? (MessageListener) this.messageListener : null);
	}

	protected SessionAwareMessageListener getSessionAwareMessageListener() {
		return (this.messageListener instanceof SessionAwareMessageListener ?
				(SessionAwareMessageListener) this.messageListener : null);
	}

	public void setServerSessionFactory(ServerSessionFactory serverSessionFactory) {
		this.serverSessionFactory = serverSessionFactory;
	}

	protected ServerSessionFactory getServerSessionFactory() {
		return serverSessionFactory;
	}

	public void setMaxMessages(int maxMessages) {
		this.maxMessages = maxMessages;
	}

	protected int getMaxMessages() {
		return maxMessages;
	}


	public void afterPropertiesSet() {
		super.afterPropertiesSet();

		if (getDestination() == null) {
			throw new IllegalArgumentException("destination is required");
		}
		if (getMessageListener() == null && getSessionAwareMessageListener() == null) {
			throw new IllegalArgumentException("messageListener is required");
		}

		// Create JMS Connection and ConnectionConsumer.
		try {
			this.connection = createConnection();
			ServerSessionPool pool = createServerSessionPool(this.connection);
			this.consumer = createConnectionConsumer(this.connection, pool);
			this.connection.start();
		}
		catch (JMSException ex) {
			JmsUtils.closeConnection(this.connection);
			throw convertJmsAccessException(ex);
		}
	}


	/**
	 * Create a JMS Connection via this template's ConnectionFactory.
	 * <p>This implementation uses JMS 1.1 API.
	 * @return the new JMS Connection
	 * @throws JMSException if thrown by JMS API methods
	 */
	protected Connection createConnection() throws JMSException {
		return getConnectionFactory().createConnection();
	}

	/**
	 * Create a JMS ConnectionConsumer for the given Connection.
	 * <p>This implementation uses JMS 1.1 API.
	 * @param con the JMS Connection to create a Session for
	 * @return the new JMS Session
	 * @throws JMSException if thrown by JMS API methods
	 */
	protected ConnectionConsumer createConnectionConsumer(Connection con, ServerSessionPool pool)
			throws JMSException {

		return con.createConnectionConsumer(getDestination(), getMessageSelector(), pool, getMaxMessages());
	}

	/**
	 * Create a JMS Session for the given Connection.
	 * <p>This implementation uses JMS 1.1 API.
	 * @param con the JMS Connection to create a Session for
	 * @return the new JMS Session
	 * @throws JMSException if thrown by JMS API methods
	 */
	protected Session createSession(Connection con) throws JMSException {
		return con.createSession(isSessionTransacted(), getSessionAcknowledgeMode());
	}


	protected ServerSessionPool createServerSessionPool(final Connection con) throws JMSException {
		this.sessionManager = new ListenerSessionManager() {
			public Session createListenerSession() throws JMSException {
				return MessageListenerContainer.this.createListenerSession(con);
			}
			public void executeListenerSession(Session session) {
				MessageListenerContainer.this.executeListenerSession(session);
			}
		};
		return new ServerSessionPool() {
			public ServerSession getServerSession() throws JMSException {
				logger.debug("JMS ConnectionConsumer requests ServerSession");
				return serverSessionFactory.getServerSession(sessionManager);
			}
		};
	}

	protected Session createListenerSession(Connection con) throws JMSException {
		final Session session = createSession(con);
		if (getMessageListener() != null) {
			session.setMessageListener(getMessageListener());
		}
		else {
			// It's a SessionAwareMessageListener.
			session.setMessageListener(new MessageListener() {
				public void onMessage(Message message) {
					try {
						getSessionAwareMessageListener().onMessage(message, session);
					}
					catch (JMSException ex) {
						convertJmsAccessException(ex);
					}
				}
			});
		}
		return session;
	}

	protected void executeListenerSession(Session session) {
		session.run();
	}


	public void start() throws JMSException {
		this.connection.start();
	}

	public void stop() throws JMSException {
		this.connection.stop();
	}


	public void destroy() throws JMSException {
		logger.debug("Closing ServerSessionFactory");
		this.serverSessionFactory.close(this.sessionManager);
		logger.debug("Closing JMS ConnectionConsumer and Connection");
		this.consumer.close();
		this.connection.close();
	}

}
