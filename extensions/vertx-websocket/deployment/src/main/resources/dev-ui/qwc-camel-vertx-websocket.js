/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import {css} from 'lit';
import {html} from 'qwc-hot-reload-element';
import {QwcCamelCore} from "../camel-quarkus-core/qwc-camel-core.js";
import {columnBodyRenderer} from '@vaadin/grid/lit.js';
import '@vaadin/grid';
import '@vaadin/grid/vaadin-grid-sort-column.js';
import '@vaadin/vertical-layout';
import '@vaadin/grid';
import '@vaadin/text-field';
import '@vaadin/button';
import '@vaadin/tooltip';
import '@vaadin/message-input';
import '@vaadin/message-list';

export class QwcCamelVertxWebsocket extends QwcCamelCore {
    static styles = css`
        :host {
            display: flex;
            flex-direction: column;
            gap: 10px;
        }

        .endpoints-table {
            padding-bottom: 10px;
        }

        .top-bar {
            align-items: baseline;
            gap: 20px;
            padding-left: 20px;
            padding-right: 20px;
        }

        .top-bar h4 {
            color: var(--lumo-contrast-60pct);
        }

        vaadin-message.outgoing {
            background-color: hsla(214, 61%, 25%, 0.05);
            border: 2px solid rgb(255, 255, 255);
            border-radius: 9px;
        }

        .message-list {
            gap: 20px;
            padding-left: 20px;
            padding-right: 20px;
        }

        vaadin-message-input > vaadin-text-area > textarea {
            font-family: monospace;
        }
    `;

    static properties = {
        includeHeaders: {state: false},
        _selectedEndpoint: {state: true},
        _selectedConnection: {state: true},
        _endpointsAndConnections: {state: true},
        _textMessages: {state: true},
    }

    constructor() {
        super('vertx-websocket', {});
        this.includeHeaders = false;
        this._selectedEndpoint = null;
        this._selectedConnection = null;
        this._textMessages = [];
        this._endpointsAndConnections = [];
    }

    render() {
        let host = null;
        const hosts = super.consoleData()['hosts'];
        if (hosts && hosts.length > 0) {
            // There will only ever be one host - the Quarkus Vert.x HTTP server
            host = hosts[0];
        }

        if (host && this._endpointsAndConnections) {
            if (this._selectedConnection) {
                if (this._textMessages) {
                    return this._renderConnection();
                } else {
                    return html`<span>Loading messages...</span>`;
                }
            } else if (this._selectedEndpoint) {
                return this._renderConnections();
            } else {
                return this._renderEndpoints(host);
            }
        } else {
            return html`<span>Camel Vert.x WebSocket consumer endpoints are not configured</span>`;
        }
    }

    _renderConnection() {
        return html`
            ${this._renderTopBarConnection()}
            <vaadin-message-input @submit="${this._sendMessage}" style="font-family: monospace;"></vaadin-message-input>

            <vaadin-message-list class="message-list" .items="${this._textMessages}"></vaadin-message-list>
        `;
    }

    _renderConnections() {
        return html`
            <vaadin-horizontal-layout theme="spacing padding">
                <vaadin-checkbox label="Show headers"
                                 @change="${() => this._includeHeaders()}"
                                 .checked="${this.includeHeaders}"
                                 style="width: 20%">
                </vaadin-checkbox>
            </vaadin-horizontal-layout>
            <vaadin-grid .items="${this._selectedEndpoint.peers}" class="consoleData" theme="no-border row-stripes">
                <vaadin-grid-sort-column
                    path="id"
                    header="ID"
                    ${columnBodyRenderer((item) => super.codeStyleRenderer(item.id), [])}
                    width="380px"
                    flex-grow="0">
                </vaadin-grid-sort-column>
                <vaadin-grid-sort-column
                    auto-width
                    path="path"
                    header="Path"
                    resizable
                    ${columnBodyRenderer((item) => super.codeStyleRenderer(item.path), [])}>
                </vaadin-grid-sort-column>
                <vaadin-grid-sort-column
                    auto-width
                    path="hostAddress"
                    header="Host"
                    resizable
                    ${columnBodyRenderer((item) => super.codeStyleRenderer(item.hostAddress), [])}>
                </vaadin-grid-sort-column>
                <vaadin-grid-column
                    auto-width
                    header="Headers"
                    ${columnBodyRenderer((item) => this._renderHeaders(item.headers), [])}
                    resizable
                    ?hidden="${!this.includeHeaders}">
                </vaadin-grid-column>
                <span slot="empty-state">No data available</span>
            </vaadin-grid>
        `;
    }

    _renderEndpoints(host) {
        this._endpointsAndConnections = host.paths;
        return html`
            <vaadin-grid .items="${this._endpointsAndConnections}" class="endpoints-table" theme="no-border" all-rows-visible>
                <vaadin-grid-column auto-width
                    header="Path"
                    ${columnBodyRenderer(this._renderPath, [])}
                    resizable>
                </vaadin-grid-column>
                <vaadin-grid-column auto-width
                    header="Connections"
                    ${columnBodyRenderer(this._renderConnectionsButton, [])}
                    resizable>
                </vaadin-grid-column>
            </vaadin-grid>
        `;
    }

    _renderConnectionsButton(endpoint) {
        return html`
            <vaadin-button @click=${() => this._showConnections(endpoint)}>
                <vaadin-icon icon="font-awesome-solid:plug" style="padding: 0.25em" slot="prefix"></vaadin-icon>
                ${endpoint.peers.length}
            </vaadin-button>
        `;
    }

    _renderPath(endpoint) {
        const inputId = 'id-' + endpoint.path
            .replace(/[^a-zA-Z0-9_-]+/g, '-')
            .replace(/^-+|-+$/g, '')
            .toLowerCase();
        let inputPath;
        let resetButton;

        if (endpoint.path.indexOf('{') !== -1) {
            inputPath = html`
                <vaadin-text-field
                    id="${inputId}"
                    value="${endpoint.path}"
                    helper-text="Replace path parameters with real values"
                    style="font-family: monospace;width: 15em;">
            `
            resetButton = html`
                <vaadin-button @click="${() => this._resetPathInput(inputId, endpoint.path)}" label="Reset the original path">
                    <vaadin-icon icon="font-awesome-solid:rotate-right" style="padding: 0.25em"></vaadin-icon>
                    <vaadin-tooltip slot="tooltip" text="Reset the value to the original endpoint path"></vaadin-tooltip>
                </vaadin-button>
            `
        } else {
            inputPath = html`
                <vaadin-text-field
                    id="${inputId}"
                    value="${endpoint.path}"
                    readonly
                    style="font-family: monospace;width: 15em;">
            `
            resetButton = html``;
        }
        return html`
            ${inputPath}
            </vaadin-text-field>
            <vaadin-button @click="${() => this._openDevConnection(inputId)}" label="Open Dev UI connection">
                Connect
                <vaadin-icon icon="font-awesome-solid:plug" style="padding: 0.25em" slot="prefix"></vaadin-icon>
            </vaadin-button>
            ${resetButton}
        `;
    }

    _resetPathInput(endpointPathId, value) {
        const query = '#' + endpointPathId;
        const input = this.renderRoot?.querySelector(query) ?? null;
        if (input) {
            input.value = value;
        }
    }

    _renderHeaders(headers) {
        if (headers) {
            return html`
                <vaadin-horizontal-layout theme="wrap" style="gap: 10px">
                    ${Object.entries(headers).map(([key, value]) => html`
                        <qui-badge small pill><span>${key}=${value}</span></qui-badge>
                    `)}
                </vaadin-horizontal-layout>
            `;
        }
    }

    _renderTopBarConnection() {
        return html`
            <div class="top-bar">
                <vaadin-button @click="${() => this._closeDevConnection()}">
                    <vaadin-icon icon="font-awesome-solid:caret-left" slot="prefix"></vaadin-icon>
                    Back
                </vaadin-button>
                <vaadin-button @click="${this._clearMessages}">
                    <vaadin-icon icon="font-awesome-solid:trash" slot="prefix"></vaadin-icon>
                    Clear messages
                </vaadin-button>
                <h4>Endpoint: <code>${this._selectedConnection.url}</code></h4>
            </div>`;
    }

    _openDevConnection(inputPathId) {
        const query = '#' + inputPathId;
        const path = this.renderRoot?.querySelector(query).value ?? null;

        if (/[{}]/.test(path)) {
            return;
        }

        this._selectedConnection = new WebSocket(path);
        this._selectedConnection.onmessage = (event) => {
            const message = {
                text: '',
                time: this._formatDate(new Date()),
                userAbbr: 'IN',
                className: 'incoming'
            };

            if (typeof event.data === "string") {
                message.text = event.data
            } else {
                message.text = "Binary content";
            }

            this._textMessages.push(message);
            this._textMessages.sort((a, b) => new Date(b.time) - new Date(a.time));
            this._textMessages = [...this._textMessages];
        }
    }

    _closeDevConnection() {
        this._selectedConnection.close();
        this._selectedConnection = null;
        this._clearMessages();
        this._showConnections(this._selectedEndpoint)
    }

    _sendMessage(e) {
        if (this._selectedConnection) {
            const message = {
                text: e.detail.value,
                time: this._formatDate(new Date()),
                userAbbr: 'OUT',
                className: 'outgoing'
            };
            this._selectedConnection.send(e.detail.value);

            this._textMessages.push(message);
            this._textMessages.sort((a, b) => new Date(b.time) - new Date(a.time));
            this._textMessages = [...this._textMessages];
        }
    }

    _clearMessages() {
        this._textMessages = [];
    }

    _showConnections(endpoint) {
        this._selectedEndpoint = endpoint;
        this._selectedConnection = null;
    }

    _includeHeaders() {
        this.includeHeaders = !this.includeHeaders;
    }

    _formatDate(date) {
        const year = date.getFullYear();
        const month = String(date.getMonth() + 1).padStart(2, '0');
        const day = String(date.getDate()).padStart(2, '0');
        const hours = String(date.getHours()).padStart(2, '0');
        const minutes = String(date.getMinutes()).padStart(2, '0');
        const seconds = String(date.getSeconds()).padStart(2, '0');
        const milliseconds= String(date.getMilliseconds()).padStart(2, '0');
        return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}:${milliseconds}`;
    }
}

customElements.define('qwc-camel-vertx-websocket', QwcCamelVertxWebsocket);
