const test = require("node:test");
const assert = require("node:assert/strict");

const {
    calculateReconnectDelay,
    createReconnectingWebSocket,
} = require("../static/product-updates.js");

class FakeWebSocket {
    static CONNECTING = 0;
    static OPEN = 1;
    static CLOSING = 2;
    static CLOSED = 3;
    static instances = [];

    constructor(url) {
        this.url = url;
        this.readyState = FakeWebSocket.CONNECTING;
        this.closeCalls = [];
        FakeWebSocket.instances.push(this);
    }

    open() {
        this.readyState = FakeWebSocket.OPEN;
        this.onopen?.();
    }

    disconnect() {
        this.readyState = FakeWebSocket.CLOSED;
        this.onclose?.();
    }

    message(data) {
        this.onmessage?.({ data });
    }

    close(code, reason) {
        this.closeCalls.push({ code, reason });
        this.readyState = FakeWebSocket.CLOSING;
    }
}

function createFakeTimers() {
    const timers = [];
    return {
        timers,
        setTimeout(callback, delay) {
            const timer = { callback, delay, cancelled: false };
            timers.push(timer);
            return timer;
        },
        clearTimeout(timer) {
            timer.cancelled = true;
        },
        runNext() {
            const timer = timers.find(candidate => !candidate.cancelled && !candidate.executed);
            assert.ok(timer, "Expected a pending timer");
            timer.executed = true;
            timer.callback();
        },
        activeTimers() {
            return timers.filter(timer => !timer.cancelled && !timer.executed);
        },
    };
}

function createClient(overrides = {}) {
    FakeWebSocket.instances = [];
    const timers = createFakeTimers();
    const statuses = [];
    const messages = [];
    const client = createReconnectingWebSocket({
        url: "wss://shop.local/api/v1/product-updates/ws",
        WebSocketImpl: FakeWebSocket,
        setTimeoutImpl: timers.setTimeout,
        clearTimeoutImpl: timers.clearTimeout,
        random: () => 0.5,
        onStatus: status => statuses.push(status),
        onMessage: data => messages.push(data),
        ...overrides,
    });
    return { client, timers, statuses, messages };
}

test("reconnect delay grows exponentially with jitter and a maximum", () => {
    assert.equal(calculateReconnectDelay(0, () => 0.5), 1000);
    assert.equal(calculateReconnectDelay(1, () => 0.5), 2000);
    assert.equal(calculateReconnectDelay(0, () => 0), 750);
    assert.equal(calculateReconnectDelay(0, () => 1), 1250);
    assert.equal(calculateReconnectDelay(20, () => 1), 30000);
});

test("unexpected close schedules one reconnect", () => {
    const { client, timers } = createClient();
    client.start();
    const firstSocket = FakeWebSocket.instances[0];

    firstSocket.disconnect();

    assert.equal(timers.activeTimers().length, 1);
    assert.equal(timers.activeTimers()[0].delay, 1000);
    timers.runNext();
    assert.equal(FakeWebSocket.instances.length, 2);
});

test("successful connection resets the backoff attempt", () => {
    const { client, timers } = createClient();
    client.start();
    FakeWebSocket.instances[0].disconnect();
    timers.runNext();
    FakeWebSocket.instances[1].disconnect();
    assert.equal(timers.activeTimers()[0].delay, 2000);
    timers.runNext();

    FakeWebSocket.instances[2].open();
    FakeWebSocket.instances[2].disconnect();

    assert.equal(timers.activeTimers()[0].delay, 1000);
});

test("events from a stale socket cannot schedule another reconnect", () => {
    const { client, timers, statuses, messages } = createClient();
    client.start();
    const firstSocket = FakeWebSocket.instances[0];
    firstSocket.disconnect();
    timers.runNext();
    statuses.length = 0;

    firstSocket.open();
    firstSocket.message("stale update");
    firstSocket.onerror?.();
    firstSocket.disconnect();

    assert.deepEqual(statuses, []);
    assert.deepEqual(messages, []);
    assert.equal(timers.activeTimers().length, 0);
    assert.equal(FakeWebSocket.instances.length, 2);

    FakeWebSocket.instances[1].disconnect();
    assert.equal(timers.activeTimers()[0].delay, 2000);
});

test("stop cancels a pending reconnect and prevents future connections", () => {
    const { client, timers } = createClient();
    client.start();
    FakeWebSocket.instances[0].disconnect();
    const queuedCallback = timers.activeTimers()[0].callback;

    client.stop();
    queuedCallback();

    assert.equal(timers.activeTimers().length, 0);
    assert.equal(FakeWebSocket.instances.length, 1);
});

test("stop closes the active socket intentionally without reconnecting", () => {
    const { client, timers } = createClient();
    client.start();
    const socket = FakeWebSocket.instances[0];
    socket.open();

    client.stop();
    socket.disconnect();

    assert.deepEqual(socket.closeCalls, [{ code: 1000, reason: "Client shutdown" }]);
    assert.equal(timers.activeTimers().length, 0);
    assert.equal(FakeWebSocket.instances.length, 1);
});

test("start is idempotent while a socket is active", () => {
    const { client } = createClient();

    client.start();
    client.start();

    assert.equal(FakeWebSocket.instances.length, 1);
});

test("stop from connecting status closes the socket and remains terminal", () => {
    let client;
    ({ client } = createClient({
        onStatus(status) {
            if (status === "connecting") {
                client.stop();
            }
        },
    }));

    client.start();

    assert.equal(FakeWebSocket.instances.length, 1);
    assert.deepEqual(FakeWebSocket.instances[0].closeCalls, [
        { code: 1000, reason: "Client shutdown" },
    ]);
});

test("stop from reconnect status cancels the timer", () => {
    let client;
    let timers;
    ({ client, timers } = createClient({
        onStatus(status) {
            if (status.startsWith("reconnecting")) {
                client.stop();
            }
        },
    }));
    client.start();

    FakeWebSocket.instances[0].disconnect();

    assert.equal(timers.activeTimers().length, 0);
    assert.equal(FakeWebSocket.instances.length, 1);
});

test("start from connecting status cannot create a parallel socket", () => {
    let client;
    let reentered = false;
    ({ client } = createClient({
        onStatus(status) {
            if (status === "connecting" && !reentered) {
                reentered = true;
                client.start();
            }
        },
    }));

    client.start();

    assert.equal(FakeWebSocket.instances.length, 1);
});
