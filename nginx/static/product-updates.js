(function (global) {
    "use strict";

    function calculateReconnectDelay(
        attempt,
        random = Math.random,
        initialDelay = 1000,
        maxDelay = 30000,
        jitterRatio = 0.25,
    ) {
        const exponentialDelay = Math.min(maxDelay, initialDelay * (2 ** attempt));
        const jitterMultiplier = 1 - jitterRatio + (2 * jitterRatio * random());
        return Math.min(maxDelay, Math.max(0, Math.round(exponentialDelay * jitterMultiplier)));
    }

    function createReconnectingWebSocket({
                                             url,
                                             WebSocketImpl = global.WebSocket,
                                             setTimeoutImpl = global.setTimeout.bind(global),
                                             clearTimeoutImpl = global.clearTimeout.bind(global),
                                             random = Math.random,
                                             onStatus = () => {},
                                             onMessage = () => {},
                                         }) {
        let socket = null;
        let reconnectTimer = null;
        let reconnectAttempt = 0;
        let stopped = false;

        const scheduleReconnect = () => {
            if (stopped || reconnectTimer !== null) {
                return;
            }

            const delay = calculateReconnectDelay(reconnectAttempt, random);
            reconnectAttempt += 1;
            const timer = setTimeoutImpl(() => {
                if (reconnectTimer !== timer) {
                    return;
                }
                reconnectTimer = null;
                connect();
            }, delay);
            reconnectTimer = timer;
            onStatus(`reconnecting in ${delay} ms`);
        };

        const connect = () => {
            if (stopped || socket !== null || reconnectTimer !== null) {
                return;
            }

            const currentSocket = new WebSocketImpl(url);
            socket = currentSocket;

            currentSocket.onopen = () => {
                if (currentSocket !== socket || stopped) {
                    return;
                }
                reconnectAttempt = 0;
                onStatus("connected");
            };
            currentSocket.onmessage = event => {
                if (currentSocket === socket && !stopped) {
                    onMessage(event.data);
                }
            };
            currentSocket.onerror = () => {
                if (currentSocket === socket && !stopped) {
                    onStatus("error");
                }
            };
            currentSocket.onclose = () => {
                if (currentSocket !== socket) {
                    return;
                }
                socket = null;
                if (!stopped) {
                    scheduleReconnect();
                }
            };
            onStatus("connecting");
        };

        const stop = () => {
            if (stopped) {
                return;
            }

            stopped = true;
            if (reconnectTimer !== null) {
                clearTimeoutImpl(reconnectTimer);
                reconnectTimer = null;
            }

            const currentSocket = socket;
            socket = null;
            if (currentSocket !== null &&
                currentSocket.readyState !== WebSocketImpl.CLOSING &&
                currentSocket.readyState !== WebSocketImpl.CLOSED
            ) {
                currentSocket.close(1000, "Client shutdown");
            }
            onStatus("closed");
        };

        return { start: connect, stop };
    }

    const api = { calculateReconnectDelay, createReconnectingWebSocket };
    if (typeof module !== "undefined" && module.exports) {
        module.exports = api;
        return;
    }

    const appendUpdate = (targetId, protocol, rawData) => {
        console.log(`[${protocol}] product update`, JSON.parse(rawData));
        const item = document.createElement("li");
        item.textContent = rawData;
        const eventList = document.getElementById(targetId);
        eventList.prepend(item);
        while (eventList.children.length > 50) {
            eventList.lastElementChild.remove();
        }
    };

    const setStatus = (id, text, connected) => {
        const status = document.getElementById(id);
        status.textContent = text;
        status.classList.toggle("connected", connected);
    };

    const eventSource = new EventSource("/api/v1/product-updates/sse");
    eventSource.onopen = () => setStatus("sse-status", "connected", true);
    eventSource.onerror = () => setStatus("sse-status", "reconnecting", false);
    eventSource.addEventListener("product-updated", event => {
        appendUpdate("sse-events", "SSE", event.data);
    });

    const websocketScheme = location.protocol === "https:" ? "wss" : "ws";
    const websocketClient = createReconnectingWebSocket({
        url: `${websocketScheme}://${location.host}/api/v1/product-updates/ws`,
        onStatus: status => setStatus("ws-status", status, status === "connected"),
        onMessage: data => appendUpdate("ws-events", "WebSocket", data),
    });
    websocketClient.start();

    global.stopProductWebSocket = () => websocketClient.stop();
    global.addEventListener("pagehide", () => {
        websocketClient.stop();
        eventSource.close();
    }, { once: true });
}(typeof window === "undefined" ? globalThis : window));
