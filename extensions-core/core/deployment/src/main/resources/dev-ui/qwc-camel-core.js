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
import { css, html } from 'qwc-hot-reload-element';
import { QwcHotReloadElement } from 'qwc-hot-reload-element';
import { JsonRpc } from 'jsonrpc';

export class QwcCamelCore extends QwcHotReloadElement {
    jsonRpc = new JsonRpc(this);

    static styles = css`
        :host {
            display: flex;
            flex-direction: column;
            height: 100%;
        }

        .consoleData {
            height: 100%;
            padding-bottom: 10px;
        }

        code {
            font-size: 85%;
        }

        .httpMethod {
            text-transform: uppercase;
        }
        
        .capitialized {
            text-transform: capitalize;
        }

        vaadin-horizontal-layout.empty-container {
            justify-content: center;
            align-items: center;
            text-align: center;
            color: gray;
            font-style: italic;
            font-size: 1.2em;
        }
    `;

    static properties = {
        _consoleData: {state: true},
    };

    constructor(consoleId, consoleOptions = {}) {
        super();
        this._consoleId = consoleId;
        this._consoleOptions = consoleOptions;
        this._streamGeneration = 0;
    }

    connectedCallback() {
        super.connectedCallback();
        // Leaving the page cancels its stream, so reopen it on every attach. Any data already shown stays until
        // the new fetch replaces it
        this.hotReload();
    }

    disconnectedCallback() {
        this._cancelObserver();
        super.disconnectedCallback();
    }

    hotReload(){
        this._cancelObserver();
        this.enableConsoleUpdates();
    }

    enableConsoleUpdates() {
        // If we're being inherited from outside camel-quarkus-core this allows JsonRpc calls to work
        if (this.jsonRpc.getExtensionName() !== 'camel-quarkus-core') {
            this.jsonRpc._setExtensionName('camel-quarkus-core');
        }

        // Each stream is bound to the options it was opened with, so take a copy
        const options = {...this._consoleOptions};
        const generation = this._streamGeneration;

        // Fetch initial data
        this.jsonRpc.getConsoleJSON({'id': this._consoleId, 'options': options}).then((consoleData) => {
            // Skip if the page was disconnected, or the options changed, while this fetch was in flight
            if (generation !== this._streamGeneration || !this.isConnected) {
                return;
            }
            this._consoleData = JSON.parse(consoleData.result);

            // Set up live data streaming
            this._observer = this.jsonRpc.streamConsole({'id': this._consoleId, 'options': options}).onNext(consoleData => {
                this._consoleData = JSON.parse(consoleData.result);
            });
        });
    }

    putOption(key, value) {
        if (value) {
            this._consoleOptions[key] = value;
        } else {
            delete this._consoleOptions[key];
        }
        // Reopen the stream with the new options
        this._cancelObserver();
        this.enableConsoleUpdates();
    }

    consoleData() {
        if (this._consoleData) {
            return this._consoleData;
        }
        return [];
    }

    toDate(value) {
        if (value) {
            return new Date(value).toLocaleString().replace(',', '');
        }
        return '';
    }

    formatUptime(uptimeMilliseconds) {
        if (uptimeMilliseconds) {
            const totalSeconds = Math.floor(uptimeMilliseconds / 1000);
            const d = Math.floor(totalSeconds / 86400);
            const h = Math.floor((totalSeconds % 86400) / 3600);
            const m = Math.floor((totalSeconds % 3600) / 60);
            const s = totalSeconds % 60;
            return `${d ? d + 'd ' : ''}${h ? h + 'h ' : ''}${m ? m + 'm ' : ''}${s}s`.trim();
        }
        return '';
    }

    render() {
        return html`<h1>Loading...</h1>`;
    }

    redenderNoDataAvailableMessage() {
        return html`
            <vaadin-horizontal-layout class="empty-container">
                No data available
            </vaadin-horizontal-layout>`;
    }

    codeStyleRenderer(value, elemClass){
        return html`
            <code class="${elemClass?? ''}">${value?? '' }</code>
        `;
    }

    numericPillStyleRenderer(value){
        return html`
            <qui-badge small pill><span>${value?? ''}</span></qui-badge>
        `;
    }

    _cancelObserver(){
        this._streamGeneration++;
        if(this._observer){
            this._observer.cancel();
            this._observer = null;
        }
    }
}

customElements.define('qwc-camel-core', QwcCamelCore);
