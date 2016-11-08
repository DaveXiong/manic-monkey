/**
 * 
 */
package com.netflix.simianarmy.resources.socket;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.netflix.simianarmy.manic.EventListener;
import com.netflix.simianarmy.manic.ManicEvent;
import com.netflix.simianarmy.manic.MonkeyEventDispatcher;

@ServerEndpoint("/manic")
public class ManicWebSocket {

	private static final Logger LOGGER = LoggerFactory.getLogger(ManicWebSocket.class);
	@Inject
	private static Map<Session, EventListener> clients = Collections
			.synchronizedMap(new HashMap<Session, EventListener>());

	public ManicWebSocket() {
	}

	@OnMessage
	public void onMessage(String message, Session session) throws IOException, InterruptedException {
		LOGGER.info("receive {} from {}", message, session);
	}

	@OnOpen
	public void onOpen(final Session session) {
		LOGGER.info("new connection from {}", session);

		EventListener listener = new EventListener() {

			@Override
			public void onEvent(ManicEvent evt) {

				if (evt.getType() == ManicEvent.Type.MONKEY || evt.getType() == ManicEvent.Type.INSTANCE) {
					if (session.isOpen()) {
						try {
							session.getBasicRemote().sendText(new Gson().toJson(evt));
						} catch (IOException e) {
							e.printStackTrace();
							onClose(session);
						}
					}
				}

			}
		};

		clients.put(session, listener);
		MonkeyEventDispatcher.INSTANCE.subscribe(listener);
	}

	@OnClose
	public void onClose(Session session) {
		LOGGER.info("close connection from {}", session);
		MonkeyEventDispatcher.INSTANCE.unsubscribe(clients.get(session));
		clients.remove(session);
	}
}
